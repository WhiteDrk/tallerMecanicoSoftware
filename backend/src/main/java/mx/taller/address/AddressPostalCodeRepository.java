package mx.taller.address;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressPostalCodeRepository extends JpaRepository<AddressPostalCode, Long> {
  @EntityGraph(attributePaths = { "state", "municipality" })
  Optional<AddressPostalCode> findByCode(String code);
}
