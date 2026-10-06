package mx.taller.client;

import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.identity.RoleCode;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import mx.taller.workshop.WorkshopAuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClientManagementFacade {
  private final ClientRepository clients; private final AppUserRepository users; private final WorkshopAccessService workshopAccess; private final ClientAddressValidator addressValidator; private final ClientPhotoValidator photoValidator; private final AuditService audit;
  public ClientManagementFacade(ClientRepository clients, AppUserRepository users, WorkshopAccessService workshopAccess, ClientAddressValidator addressValidator, ClientPhotoValidator photoValidator, AuditService audit) { this.clients = clients; this.users = users; this.workshopAccess = workshopAccess; this.addressValidator = addressValidator; this.photoValidator = photoValidator; this.audit = audit; }
  @Transactional(readOnly = true) public ClientDetail get(UUID clientId, String actorId) { AppUser actor = actor(actorId); Client client = client(clientId); requireSourceAccess(actor, client); return detail(client); }
  @Transactional public ClientDetail update(UUID clientId, ClientRegistrationRequest request, MultipartFile photo, String actorId, String ip) {
    AppUser actor = actor(actorId); Client client = client(clientId); requireSourceAccess(actor, client);
    Workshop destination = workshopAccess.requireAccess(actorId, request.workshopId());
    boolean moving = !client.getWorkshop().getId().equals(destination.getId());
    if (moving && !canMove(actor)) throw new WorkshopAuthorizationException("No tiene permisos para cambiar el cliente de taller");
    String email = request.email().trim().toLowerCase(); String phone = Client.normalizePhone(request.phone());
    if (clients.existsByEmailNormalizedAndIdNot(email, clientId) || clients.existsByPhoneNormalizedAndIdNot(phone, clientId)) throw new DuplicateClientException();
    addressValidator.validate(request);
    ClientPhotoValidator.ValidatedPhoto validatedPhoto = photoValidator.validate(photo);
    client.update(request, destination, validatedPhoto.bytes(), validatedPhoto.contentType());
    audit.record(actor, moving ? "CLIENT_WORKSHOP_CHANGED" : "CLIENT_UPDATED", "CLIENT", client.getId().toString(), ip, "{\"workshopId\":\"" + destination.getId() + "\"}");
    return detail(client);
  }
  @Transactional public void suspend(UUID clientId, String actorId, String ip) {
    AppUser actor = actor(actorId); if (!hasRole(actor, RoleCode.OWNER)) throw new WorkshopAuthorizationException("Sólo Administración puede suspender clientes");
    Client client = client(clientId); if (client.getStatus() == ClientStatus.SUSPENDED) throw new WorkshopAuthorizationException("El cliente ya está suspendido");
    client.suspend(); audit.record(actor, "CLIENT_SUSPENDED", "CLIENT", client.getId().toString(), ip, "{}");
  }
  private void requireSourceAccess(AppUser actor, Client client) { workshopAccess.requireAccess(actor.getId().toString(), client.getWorkshop().getId()); }
  private boolean canMove(AppUser actor) { return hasRole(actor, RoleCode.OWNER) || hasRole(actor, RoleCode.MANAGER); }
  private AppUser actor(String actorId) { return users.findById(UUID.fromString(actorId)).orElseThrow(() -> new WorkshopAuthorizationException("El usuario autenticado ya no existe")); }
  private Client client(UUID id) { return clients.findById(id).orElseThrow(() -> new WorkshopAuthorizationException("El cliente no existe")); }
  private boolean hasRole(AppUser user, RoleCode role) { return user.getRoles().stream().anyMatch(item -> item.getCode() == role); }
  private ClientDetail detail(Client client) { return new ClientDetail(client.getId(), client.getFullName(), client.getAlternateContactName(), client.getBirthDate().toString(), client.getPhone(), client.getWorkPhone(), client.getEmail(), client.getWorkEmail(), client.getStreet(), client.getNeighborhood(), client.getMunicipality(), client.getState(), client.getPostalCode(), client.getWorkshop().getId(), client.getWorkshop().getName(), client.getStatus().name()); }
  public record ClientDetail(UUID id, String fullName, String alternateContactName, String birthDate, String phone, String workPhone, String email, String workEmail, String street, String neighborhood, String municipality, String state, String postalCode, UUID workshopId, String workshopName, String status) { }
}
