package mx.taller.vehicle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.auth.ConflictException;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.identity.RoleCode;
import mx.taller.status.StatusCatalogFacade;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import mx.taller.workshop.WorkshopAuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleManagementFacade {
  private final VehicleRepository vehicles;
  private final WorkshopAccessService access;
  private final VehicleCatalogFacade catalog;
  private final StatusCatalogFacade statuses;
  private final AppUserRepository users;
  private final AuditService audit;
  private final ObjectMapper json;

  public VehicleManagementFacade(VehicleRepository vehicles, WorkshopAccessService access,
      VehicleCatalogFacade catalog, StatusCatalogFacade statuses, AppUserRepository users,
      AuditService audit, ObjectMapper json) {
    this.vehicles = vehicles; this.access = access; this.catalog = catalog;
    this.statuses = statuses; this.users = users; this.audit = audit; this.json = json;
  }

  /** Vehicle update and before/after audit commit or roll back as a single transaction. */
  @Transactional
  public VehicleQueryFacade.VehicleDetail update(UUID id, VehicleUpdateRequest request, String actorId, String ip) {
    Vehicle v = scoped(id, actorId);
    requireActiveRevision(v, request.revision());
    if (!v.getVin().equals(VehicleRegistrationFacade.normalizeVin(request.vin())))
      throw new VehicleValidationException("El VIN es inmutable y no se puede modificar");
    String plate = VehicleRegistrationFacade.normalizePlate(request.plate());
    String serial = VehicleRegistrationFacade.normalizeSerial(request.serialNumber());
    VehicleRegistrationFacade.validateYear(request.year());
    if (vehicles.existsByPlateNormalizedAndIdNot(plate, id) || vehicles.existsBySerialNumberAndIdNot(serial, id))
      throw new ConflictException("La placa o el número de serie no está disponible");
    Snapshot before = snapshot(v);
    boolean manual = hasText(request.manualBrand()) || hasText(request.manualModel());
    // Preserve an unchanged manual/pending model without editing shared catalog records.
    VehicleModel model = !manual && v.getModel().getId().equals(request.modelId())
        && v.getModel().getBrand().getId().equals(request.brandId()) ? v.getModel()
        : catalog.resolveModel(request.brandId(), request.modelId(), request.manualBrand(), request.manualModel());
    v.updateDetails(model, request, plate, serial);
    vehicles.saveAndFlush(v);
    record(v, before, "VEHICLE_UPDATED", actorId, ip);
    return VehicleQueryFacade.detail(v);
  }

  @Transactional
  public VehicleQueryFacade.VehicleDetail deactivate(UUID id, VehicleDeactivationRequest request, String actorId, String ip) {
    Vehicle v = scoped(id, actorId);
    requireActiveRevision(v, request.revision());
    if (!Boolean.TRUE.equals(request.confirmed()) || request.confirmationVin() == null
        || !v.getVin().equals(request.confirmationVin().trim().toUpperCase(java.util.Locale.ROOT)))
      throw new VehicleValidationException("Confirma la desactivación escribiendo el VIN del vehículo");
    Snapshot before = snapshot(v);
    v.deactivate(statuses.requiredActive("INACTIVO"));
    vehicles.saveAndFlush(v);
    record(v, before, "VEHICLE_DEACTIVATED", actorId, ip);
    return VehicleQueryFacade.detail(v);
  }

  private Vehicle scoped(UUID id, String actorId) {
    AppUser actor = users.findById(UUID.fromString(actorId)).orElseThrow(() ->
      new WorkshopAuthorizationException("El usuario autenticado ya no existe"));
    if (!actor.isEnabled() || actor.getRoles().stream().noneMatch(role ->
        role.getCode() == RoleCode.ADMINISTRADOR || role.getCode() == RoleCode.RECEPCIONISTA))
      throw new WorkshopAuthorizationException("No tiene permisos para modificar vehículos");
    List<UUID> scope = access.accessible(actorId).stream().map(Workshop::getId).toList();
    if (scope.isEmpty()) throw new VehicleNotFoundException();
    return vehicles.findByIdAndClientWorkshopIdIn(id, scope).orElseThrow(VehicleNotFoundException::new);
  }

  private void requireActiveRevision(Vehicle v, Long revision) {
    if (!"ACTIVO".equals(v.getStatus().getValue()))
      throw new ConflictException("El vehículo no está ACTIVO; consulta nuevamente su ficha");
    if (revision == null || v.getRevision() != revision)
      throw new ConflictException("La ficha cambió; vuelve a consultarla antes de continuar");
  }

  private void record(Vehicle v, Snapshot before, String action, String actorId, String ip) {
    AppUser actor = users.findById(UUID.fromString(actorId)).orElseThrow(() ->
      new WorkshopAuthorizationException("El usuario autenticado ya no existe"));
    try {
      audit.record(actor, action, "VEHICLE", v.getId().toString(), ip,
        json.writeValueAsString(Map.of("before", before, "after", snapshot(v))));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("No se pudo preparar la auditoría del vehículo", exception);
    }
  }

  private boolean hasText(String value) { return value != null && !value.isBlank(); }
  private Snapshot snapshot(Vehicle v) {
    return new Snapshot(VehicleQueryFacade.detail(v), v.getClient().getId(), v.getClient().getWorkshop().getId());
  }
  private record Snapshot(VehicleQueryFacade.VehicleDetail vehicle, UUID clientId, UUID workshopId) { }
}
