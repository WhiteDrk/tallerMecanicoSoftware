package mx.taller.vehicle;

import java.time.Year;
import java.util.Locale;
import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.client.Client;
import mx.taller.client.ClientRepository;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.status.StatusCatalogFacade;
import mx.taller.workshop.WorkshopAccessService;
import mx.taller.workshop.WorkshopAuthorizationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleRegistrationFacade {
  private final VehicleRepository vehicles;
  private final ClientRepository clients;
  private final VehicleCatalogFacade catalog;
  private final StatusCatalogFacade statuses;
  private final WorkshopAccessService workshopAccess;
  private final AppUserRepository users;
  private final AuditService audit;
  public VehicleRegistrationFacade(VehicleRepository vehicles, ClientRepository clients, VehicleCatalogFacade catalog, StatusCatalogFacade statuses, WorkshopAccessService workshopAccess, AppUserRepository users, AuditService audit) { this.vehicles = vehicles; this.clients = clients; this.catalog = catalog; this.statuses = statuses; this.workshopAccess = workshopAccess; this.users = users; this.audit = audit; }
  @Transactional public VehicleResult register(UUID clientId, VehicleRegistrationRequest request, String actorId, String ip) {
    Client client = clients.findById(clientId).orElseThrow(() -> new VehicleValidationException("El cliente no existe"));
    workshopAccess.requireAccess(actorId, client.getWorkshop().getId());
    if (!client.hasStatus("ACTIVO")) throw new WorkshopAuthorizationException("El cliente debe estar ACTIVO para registrar vehículos");
    String vin = normalizeVin(request.vin()); String plate = normalizePlate(request.plate()); String serialNumber = normalizeSerial(request.serialNumber());
    validateYear(request.year());
    if (vehicles.existsByVin(vin)) throw new VehicleValidationException("Ya existe un vehículo con ese VIN");
    if (vehicles.existsByPlateNormalized(plate)) throw new VehicleValidationException("Ya existe un vehículo con esa placa");
    if (vehicles.existsBySerialNumber(serialNumber)) throw new VehicleValidationException("Ya existe un vehículo con ese número de serie");
    VehicleModel model = catalog.resolveModel(request.brandId(), request.modelId(), request.manualBrand(), request.manualModel());
    Vehicle vehicle;
    try { vehicle = vehicles.saveAndFlush(new Vehicle(client, model, request, vin, plate, serialNumber, statuses.requiredActive("ACTIVO"))); }
    catch (DataIntegrityViolationException exception) { throw new VehicleValidationException("No se pudo registrar el vehículo porque VIN, placa o número de serie ya existe"); }
    AppUser actor = users.findById(UUID.fromString(actorId)).orElseThrow(() -> new WorkshopAuthorizationException("El usuario autenticado ya no existe"));
    boolean manualCatalog = request.manualBrand() != null && !request.manualBrand().isBlank();
    audit.record(actor, "VEHICLE_REGISTERED", "VEHICLE", vehicle.getId().toString(), ip, "{\"clientId\":\"" + client.getId() + "\",\"workshopId\":\"" + client.getWorkshop().getId() + "\",\"manualCatalog\":" + manualCatalog + "}");
    return new VehicleResult(vehicle.getId(), "Vehículo registrado exitosamente");
  }
  static String normalizeVin(String value) {
    String vin = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (!vin.matches("[A-HJ-NPR-Z0-9]{17}")) throw new VehicleValidationException("El VIN debe tener 17 caracteres válidos y no incluir I, O o Q");
    return vin;
  }
  static String normalizePlate(String value) {
    String plate = value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    String compact = plate.replace("-", "");
    if (!compact.matches("[A-Z0-9]{5,8}") || !plate.matches("[A-Z0-9]+(?:-[A-Z0-9]+){0,2}")) throw new VehicleValidationException("La placa debe contener de 5 a 8 caracteres alfanuméricos; se permiten hasta dos guiones");
    return plate;
  }
  static String normalizeSerial(String value) {
    String serial = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    if (!serial.matches("[A-Z0-9-]{4,40}")) throw new VehicleValidationException("El número de serie debe tener de 4 a 40 caracteres alfanuméricos o guiones");
    return serial;
  }
  static void validateYear(int value) { if (value < 1886 || value > Year.now().getValue() + 1) throw new VehicleValidationException("El año del vehículo no es válido"); }
  public record VehicleResult(UUID id, String message) { }
}
