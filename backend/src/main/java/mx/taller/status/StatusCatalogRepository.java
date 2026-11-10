package mx.taller.status;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusCatalogRepository extends JpaRepository<StatusCatalog, Long> {
  Optional<StatusCatalog> findByValue(String value);
  boolean existsByValue(String value);
  List<StatusCatalog> findAllByOrderByValueAsc();
}
