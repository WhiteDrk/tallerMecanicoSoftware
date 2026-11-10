package mx.taller.vehicle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.auth.ConflictException;
import mx.taller.client.Client;
import mx.taller.identity.*;
import mx.taller.status.*;
import mx.taller.workshop.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

class VehicleManagementTest {
  final VehicleRepository repo = mock(VehicleRepository.class);
  final WorkshopAccessService access = mock(WorkshopAccessService.class);
  final VehicleCatalogFacade catalog = mock(VehicleCatalogFacade.class);
  final StatusCatalogFacade statuses = mock(StatusCatalogFacade.class);
  final AppUserRepository users = mock(AppUserRepository.class);
  final AuditService audit = mock(AuditService.class);
  final ObjectMapper json = new ObjectMapper();
  final VehicleManagementFacade facade = new VehicleManagementFacade(repo, access, catalog, statuses, users, audit, json);
  final UUID id = UUID.randomUUID(), workshopId = UUID.randomUUID();
  final String actorId = UUID.randomUUID().toString();
  final AppUser actor = mock(AppUser.class);
  Vehicle vehicle;

  @BeforeEach void setup() {
    Role role = mock(Role.class);
    when(role.getCode()).thenReturn(RoleCode.RECEPCIONISTA);
    when(actor.isEnabled()).thenReturn(true); when(actor.getRoles()).thenReturn(Set.of(role));
    when(users.findById(UUID.fromString(actorId))).thenReturn(Optional.of(actor));
    Workshop workshop = mock(Workshop.class); when(workshop.getId()).thenReturn(workshopId); when(workshop.getName()).thenReturn("Taller QA");
    Client client = mock(Client.class); when(client.getWorkshop()).thenReturn(workshop); when(client.getId()).thenReturn(UUID.randomUUID());
    VehicleModel model = mock(VehicleModel.class, RETURNS_DEEP_STUBS);
    when(model.getId()).thenReturn(2L); when(model.getName()).thenReturn("RIO");
    when(model.getBrand().getId()).thenReturn(1L); when(model.getBrand().getName()).thenReturn("KIA");
    vehicle = new Vehicle(client, model, new VehicleRegistrationRequest("3KPF24AD3R2610131", "UI-2613", 1L, 2L, null, null, 2024, "EX", 0L, "SERIE-2613"),
      "3KPF24AD3R2610131", "UI-2613", "SERIE-2613", new StatusCatalog("ACTIVO", "Activo"));
    ReflectionTestUtils.setField(vehicle, "id", id);
    when(access.accessible(actorId)).thenReturn(List.of(workshop));
    when(repo.findByIdAndClientWorkshopIdIn(id, List.of(workshopId))).thenReturn(Optional.of(vehicle));
    when(statuses.requiredActive("INACTIVO")).thenReturn(new StatusCatalog("INACTIVO", "Inactivo"));
  }
  VehicleUpdateRequest request(String vin, long revision) {
    return new VehicleUpdateRequest(vin, " qa - 1234 ", 1L, 2L, null, null, 2025, "EX \"PLUS\"", 100L, "NEW-2613", revision);
  }

  @Test void updateNormalizesAndAuditsBeforeAndAfterWithoutChangingIdentity() throws Exception {
    Client owner = vehicle.getClient();
    var result = facade.update(id, request(vehicle.getVin(), 0), actorId, "127.0.0.1");
    assertEquals("QA-1234", result.plate()); assertEquals("3KPF24AD3R2610131", result.vin());
    assertSame(owner, vehicle.getClient()); assertEquals(100L, result.mileage()); assertEquals("EX \"PLUS\"", result.version());
    ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
    verify(audit).record(eq(actor), eq("VEHICLE_UPDATED"), eq("VEHICLE"), eq(id.toString()), eq("127.0.0.1"), details.capture());
    var event = json.readTree(details.getValue());
    assertEquals("UI-2613", event.at("/before/vehicle/plate").asText());
    assertEquals("QA-1234", event.at("/after/vehicle/plate").asText());
    assertEquals(0L, event.at("/before/vehicle/mileage").asLong());
    verifyNoInteractions(catalog); verify(repo, never()).delete(any());
  }

  @Test void vinIsImmutableEvenForWellFormedReplacement() {
    assertThrows(VehicleValidationException.class, () -> facade.update(id, request("3KPF24AD3R2610132", 0), actorId, "ip"));
    assertEquals("UI-2613", vehicle.getPlate()); verify(repo, never()).saveAndFlush(any()); verifyNoInteractions(audit);
  }
  @Test void staleCardRejectedBeforeMutation() {
    assertThrows(ConflictException.class, () -> facade.update(id, request(vehicle.getVin(), 8), actorId, "ip"));
    assertThrows(ConflictException.class, () -> facade.deactivate(id, new VehicleDeactivationRequest(vehicle.getVin(), true, 8L), actorId, "ip"));
    verify(repo, never()).saveAndFlush(any()); verifyNoInteractions(audit);
  }
  @ParameterizedTest @ValueSource(booleans = {true, false})
  void unavailablePlateOrSerialRejected(boolean duplicatePlate) {
    if (duplicatePlate) when(repo.existsByPlateNormalizedAndIdNot("QA-1234", id)).thenReturn(true);
    else when(repo.existsBySerialNumberAndIdNot("NEW-2613", id)).thenReturn(true);
    assertThrows(ConflictException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    assertEquals("UI-2613", vehicle.getPlate()); verifyNoInteractions(audit);
  }
  @Test void confirmationMustMatchAndBeExplicit() {
    for (var request : List.of(new VehicleDeactivationRequest("3KPF24AD3R2610132", true, 0L), new VehicleDeactivationRequest(vehicle.getVin(), false, 0L)))
      assertThrows(VehicleValidationException.class, () -> facade.deactivate(id, request, actorId, "ip"));
    assertEquals("ACTIVO", vehicle.getStatus().getValue()); verifyNoInteractions(audit);
  }
  @Test void deactivationPreservesRecordAndAuditsStatus() throws Exception {
    var result = facade.deactivate(id, new VehicleDeactivationRequest(vehicle.getVin(), true, 0L), actorId, "ip");
    assertEquals("INACTIVO", result.status()); assertEquals("UI-2613", result.plate()); assertEquals("SERIE-2613", result.serialNumber());
    ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
    verify(audit).record(eq(actor), eq("VEHICLE_DEACTIVATED"), eq("VEHICLE"), eq(id.toString()), eq("ip"), details.capture());
    assertEquals("ACTIVO", json.readTree(details.getValue()).at("/before/vehicle/status").asText());
    assertEquals("INACTIVO", json.readTree(details.getValue()).at("/after/vehicle/status").asText());
    assertThrows(ConflictException.class, () -> facade.deactivate(id, new VehicleDeactivationRequest(vehicle.getVin(), true, 0L), actorId, "ip"));
    assertThrows(ConflictException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    verify(repo, never()).delete(any()); verify(repo, times(1)).saveAndFlush(vehicle);
  }
  @Test void foreignVehicleAndMissingVehicleDoNotWrite() {
    when(repo.findByIdAndClientWorkshopIdIn(id, List.of(workshopId))).thenReturn(Optional.empty());
    assertThrows(VehicleNotFoundException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    assertThrows(VehicleNotFoundException.class, () -> facade.deactivate(id, new VehicleDeactivationRequest(vehicle.getVin(), true, 0L), actorId, "ip"));
    verify(repo, never()).findById(any()); verify(repo, never()).saveAndFlush(any()); verifyNoInteractions(audit);
  }
  @Test void emptyWorkshopScopeDoesNotReadOrWrite() {
    when(access.accessible(actorId)).thenReturn(List.of());
    assertThrows(VehicleNotFoundException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    verifyNoInteractions(repo, audit);
  }
  @Test void disabledUserIsRejectedEvenWithOldToken() {
    when(actor.isEnabled()).thenReturn(false);
    assertThrows(WorkshopAuthorizationException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    verifyNoInteractions(access, repo, audit);
  }
  @Test void flushConflictDoesNotAuditSuccess() {
    when(repo.saveAndFlush(vehicle)).thenThrow(new ObjectOptimisticLockingFailureException(Vehicle.class, id));
    assertThrows(ObjectOptimisticLockingFailureException.class, () -> facade.update(id, request(vehicle.getVin(), 0), actorId, "ip"));
    verifyNoInteractions(audit);
  }
}
