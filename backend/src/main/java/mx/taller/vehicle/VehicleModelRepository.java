package mx.taller.vehicle;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
  List<VehicleModel> findByBrandIdAndEnabledTrueOrderByNameAsc(Long brandId);
  Optional<VehicleModel> findByIdAndEnabledTrue(Long id);
  Optional<VehicleModel> findByBrandIdAndNameIgnoreCase(Long brandId, String name);
}
