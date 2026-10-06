package mx.taller.workshop;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {
  boolean existsByRfcIgnoreCase(String rfc);
  boolean existsByRfcIgnoreCaseAndIdNot(String rfc, UUID id);
  List<Workshop> findAllByOrderByNameAsc();
  List<Workshop> findByManagerIdOrderByNameAsc(UUID managerId);
  @Query(value = "SELECT w.* FROM workshops w JOIN user_workshops uw ON uw.workshop_id = w.id WHERE uw.user_id = :userId AND w.active = TRUE ORDER BY w.name", nativeQuery = true)
  List<Workshop> findActiveAccessibleByUserId(@Param("userId") UUID userId);
}
