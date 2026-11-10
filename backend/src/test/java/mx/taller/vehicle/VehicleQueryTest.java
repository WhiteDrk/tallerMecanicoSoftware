package mx.taller.vehicle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleQueryTest {
  @ParameterizedTest @ValueSource(strings = {"VIN", "PLATE", "ID"})
  void retrievesExactMatchOnlyThroughScopedRepository(String criterion) {
    VehicleRepository repo = mock(VehicleRepository.class);
    WorkshopAccessService access = mock(WorkshopAccessService.class);
    Workshop workshop = mock(Workshop.class);
    UUID workshopId = UUID.randomUUID(), vehicleId = UUID.randomUUID();
    when(workshop.getId()).thenReturn(workshopId);
    when(access.accessible("actor")).thenReturn(List.of(workshop));
    Vehicle vehicle = mock(Vehicle.class, RETURNS_DEEP_STUBS);
    when(vehicle.getId()).thenReturn(vehicleId);
    when(vehicle.getVin()).thenReturn("3KPF24AD3R2610131");
    when(vehicle.getModel().getBrand().getName()).thenReturn("KIA");
    when(vehicle.getModel().getName()).thenReturn("RIO");
    List<UUID> scope = List.of(workshopId);
    String input;
    if (criterion.equals("VIN")) {
      input = " 3kpf24ad3r2610131 ";
      when(repo.findByVinAndClientWorkshopIdIn("3KPF24AD3R2610131", scope)).thenReturn(Optional.of(vehicle));
    } else if (criterion.equals("PLATE")) {
      input = " ui - 2613 ";
      when(repo.findByPlateNormalizedAndClientWorkshopIdIn("UI-2613", scope)).thenReturn(Optional.of(vehicle));
    } else {
      input = vehicleId.toString();
      when(repo.findByIdAndClientWorkshopIdIn(vehicleId, scope)).thenReturn(Optional.of(vehicle));
    }
    var result = new VehicleQueryFacade(repo, access).find("actor", criterion, input);
    assertEquals(vehicleId, result.id());
    assertEquals("KIA", result.brand());
    verify(repo, never()).findById(any());
    verify(repo, never()).findAll();
  }

  @ParameterizedTest @ValueSource(strings = {"VIN", "PLATE", "ID"})
  void absenceOrForeignVehicleHasSameNotFound(String criterion) {
    VehicleRepository repo = mock(VehicleRepository.class);
    WorkshopAccessService access = mock(WorkshopAccessService.class);
    Workshop workshop = mock(Workshop.class);
    when(workshop.getId()).thenReturn(UUID.randomUUID());
    when(access.accessible("actor")).thenReturn(List.of(workshop));
    String value = criterion.equals("VIN") ? "3KPF24AD3R2610131" : criterion.equals("PLATE") ? "UI-2613" : UUID.randomUUID().toString();
    assertThrows(VehicleNotFoundException.class, () -> new VehicleQueryFacade(repo, access).find("actor", criterion, value));
    verify(repo, never()).findById(any());
  }

  @Test void noWorkshopsNeverQueriesVehicles() {
    VehicleRepository repo = mock(VehicleRepository.class);
    WorkshopAccessService access = mock(WorkshopAccessService.class);
    when(access.accessible("actor")).thenReturn(List.of());
    assertThrows(VehicleNotFoundException.class, () -> new VehicleQueryFacade(repo, access).find("actor", "PLATE", "UI-2613"));
    verifyNoInteractions(repo);
  }

  @Test void invalidValuesNeverReachRepository() {
    VehicleRepository repo = mock(VehicleRepository.class);
    WorkshopAccessService access = mock(WorkshopAccessService.class);
    VehicleQueryFacade facade = new VehicleQueryFacade(repo, access);
    for (String criterion : List.of("VIN", "PLATE", "ID", "OTHER"))
      assertThrows(VehicleValidationException.class, () -> facade.find("actor", criterion, "??"));
    verifyNoInteractions(repo, access);
  }
}
