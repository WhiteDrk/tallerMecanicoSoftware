package mx.taller.client;

import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClientRegistrationFacade {
  private final ClientRepository clients; private final AppUserRepository users; private final ClientPhotoValidator photoValidator; private final ClientAddressValidator addressValidator; private final WorkshopAccessService workshopAccess; private final AuditService audit;
  public ClientRegistrationFacade(ClientRepository clients, AppUserRepository users, ClientPhotoValidator photoValidator, ClientAddressValidator addressValidator, WorkshopAccessService workshopAccess, AuditService audit) { this.clients=clients; this.users=users; this.photoValidator=photoValidator; this.addressValidator=addressValidator; this.workshopAccess=workshopAccess; this.audit=audit; }
  @Transactional public ClientRegistrationResult register(ClientRegistrationRequest request, MultipartFile photo, String actorId, String ip) {
    String email = request.email().trim().toLowerCase(); String phone = Client.normalizePhone(request.phone());
    if (clients.existsByEmailNormalizedOrPhoneNormalized(email, phone)) throw new DuplicateClientException();
    Workshop workshop = workshopAccess.requireAccess(actorId, request.workshopId());
    addressValidator.validate(request);
    ClientPhotoValidator.ValidatedPhoto validatedPhoto = photoValidator.validate(photo);
    Client client;
    try { client = clients.saveAndFlush(new Client(request, workshop, validatedPhoto.bytes(), validatedPhoto.contentType())); }
    catch (DataIntegrityViolationException exception) { throw new DuplicateClientException(); }
    AppUser actor = users.findById(UUID.fromString(actorId)).orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    audit.record(actor, "CLIENT_REGISTERED", "CLIENT", client.getId().toString(), ip, "{\"workshopId\":\"" + workshop.getId() + "\",\"photoAttached\":" + (validatedPhoto.bytes() != null) + "}");
    return new ClientRegistrationResult(client.getId(), client.getFullName());
  }
  public record ClientRegistrationResult(UUID id, String fullName) { }
}
