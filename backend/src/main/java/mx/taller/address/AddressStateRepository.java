package mx.taller.address;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressStateRepository extends JpaRepository<AddressState, Long> {
  List<AddressState> findAllByOrderByNameAsc();
}
