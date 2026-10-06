package mx.taller.address;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressMunicipalityRepository extends JpaRepository<AddressMunicipality, Long> {
  List<AddressMunicipality> findByStateIdOrderByNameAsc(Long stateId);
}
