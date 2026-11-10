package mx.taller.vehicle;

import java.util.UUID;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
  @EntityGraph(attributePaths = {"client.workshop", "model.brand", "status"})
  Optional<Vehicle> findByVinAndClientWorkshopIdIn(String vin, Collection<UUID> workshopIds);
  @EntityGraph(attributePaths = {"client.workshop", "model.brand", "status"})
  Optional<Vehicle> findByPlateNormalizedAndClientWorkshopIdIn(String plate, Collection<UUID> workshopIds);
  @EntityGraph(attributePaths = {"client.workshop", "model.brand", "status"})
  Optional<Vehicle> findByIdAndClientWorkshopIdIn(UUID id, Collection<UUID> workshopIds);
  boolean existsByVin(String vin);
  boolean existsByPlateNormalized(String plateNormalized);
  boolean existsBySerialNumber(String serialNumber);
  boolean existsByPlateNormalizedAndIdNot(String plateNormalized, UUID id);
  boolean existsBySerialNumberAndIdNot(String serialNumber, UUID id);
}
