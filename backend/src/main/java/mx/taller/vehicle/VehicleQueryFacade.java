package mx.taller.vehicle;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleQueryFacade {
  private final VehicleRepository vehicles;
  private final WorkshopAccessService access;
  public VehicleQueryFacade(VehicleRepository vehicles, WorkshopAccessService access) {
    this.vehicles = vehicles; this.access = access;
  }

  /** Resolves only within workshops obtained from the authenticated actor, never from input. */
  @Transactional(readOnly = true)
  public VehicleDetail find(String actorId, String criterion, String value) {
    if (criterion == null || value == null || value.isBlank() || value.length() > 64)
      throw new VehicleValidationException("Indica un criterio y un valor válido de hasta 64 caracteres");
    String type = criterion.trim().toUpperCase(Locale.ROOT);
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    UUID id = null;
    switch (type) {
      case "VIN" -> {
        if (!normalized.matches("[A-HJ-NPR-Z0-9]{17}"))
          throw new VehicleValidationException("El VIN debe tener 17 caracteres y no incluir I, O o Q");
      }
      case "PLATE" -> {
        normalized = normalized.replaceAll("\\s+", "");
        if (normalized.length() > 10 || !normalized.replace("-", "").matches("[A-Z0-9]{5,8}")
            || !normalized.matches("[A-Z0-9]+(?:-[A-Z0-9]+){0,2}"))
          throw new VehicleValidationException("La placa debe tener de 5 a 8 caracteres alfanuméricos y hasta dos guiones");
      }
      case "ID" -> {
        if (!normalized.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}"))
          throw new VehicleValidationException("El ID debe ser el UUID del vehículo");
        id = UUID.fromString(normalized);
      }
      default -> throw new VehicleValidationException("Selecciona VIN, placa o ID");
    }
    List<UUID> workshops = access.accessible(actorId).stream().map(Workshop::getId).toList();
    if (workshops.isEmpty()) throw new VehicleNotFoundException();
    Optional<Vehicle> result = switch (type) {
      case "VIN" -> vehicles.findByVinAndClientWorkshopIdIn(normalized, workshops);
      case "PLATE" -> vehicles.findByPlateNormalizedAndClientWorkshopIdIn(normalized, workshops);
      default -> vehicles.findByIdAndClientWorkshopIdIn(id, workshops);
    };
    Vehicle v = result.orElseThrow(VehicleNotFoundException::new);
    return detail(v);
  }

  static VehicleDetail detail(Vehicle v) {
    return new VehicleDetail(v.getId(), v.getVin(), v.getPlate(), v.getModel().getBrand().getName(),
      v.getModel().getName(), v.getYear(), v.getVersion(), v.getMileage(), v.getSerialNumber(),
      v.getStatus().getValue(), v.getClient().getWorkshop().getName(), v.getModel().getBrand().getId(),
      v.getModel().getId(), v.getRevision());
  }

  public record VehicleDetail(UUID id, String vin, String plate, String brand, String model, int year,
    String version, Long mileage, String serialNumber, String status, String workshopName,
    Long brandId, Long modelId, long revision) { }
}
