package mx.taller.client;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClientRepository extends JpaRepository<Client, UUID> {
  boolean existsByEmailNormalizedOrPhoneNormalized(String emailNormalized, String phoneNormalized);
  boolean existsByEmailNormalizedAndIdNot(String emailNormalized, UUID id);
  boolean existsByPhoneNormalizedAndIdNot(String phoneNormalized, UUID id);
  Optional<Client> findByEmailNormalized(String emailNormalized);
  Page<Client> findByWorkshopId(UUID workshopId, Pageable pageable);
}
