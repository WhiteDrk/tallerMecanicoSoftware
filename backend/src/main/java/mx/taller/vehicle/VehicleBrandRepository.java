package mx.taller.vehicle;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Long> {
  List<VehicleBrand> findByEnabledTrueOrderByNameAsc();
  Optional<VehicleBrand> findByIdAndEnabledTrue(Long id);
  Optional<VehicleBrand> findByNameIgnoreCase(String name);
}
