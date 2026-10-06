package mx.taller.address;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressLocalityRepository extends JpaRepository<AddressLocality, Long> {
  List<AddressLocality> findByPostalCodeIdOrderByNameAsc(Long postalCodeId);
  List<AddressLocality> findByPostalCodeMunicipalityIdOrderByNameAsc(Long municipalityId);
  boolean existsByPostalCodeIdAndNameIgnoreCase(Long postalCodeId, String name);
}
