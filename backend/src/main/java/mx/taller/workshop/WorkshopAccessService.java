package mx.taller.workshop;

import java.util.List;
import java.util.UUID;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.identity.RoleCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkshopAccessService {
  private final WorkshopRepository workshops;
  private final AppUserRepository users;
  public WorkshopAccessService(WorkshopRepository workshops, AppUserRepository users) { this.workshops = workshops; this.users = users; }
  @Transactional(readOnly = true) public List<Workshop> accessible(String actorId) {
    AppUser actor = actor(actorId);
    if (hasRole(actor, RoleCode.OWNER)) return workshops.findAllByOrderByNameAsc().stream().filter(Workshop::isActive).toList();
    if (hasRole(actor, RoleCode.MANAGER) || hasRole(actor, RoleCode.CUSTOMER_SERVICE)) return workshops.findActiveAccessibleByUserId(actor.getId());
    throw new WorkshopAuthorizationException("No tiene acceso a talleres");
  }
  @Transactional(readOnly = true) public Workshop requireAccess(String actorId, UUID workshopId) {
    Workshop workshop = workshops.findById(workshopId).orElseThrow(() -> new WorkshopAuthorizationException("El taller no existe"));
    if (!workshop.isActive()) throw new WorkshopAuthorizationException("El taller está inactivo");
    if (accessible(actorId).stream().anyMatch(item -> item.getId().equals(workshopId))) return workshop;
    throw new WorkshopAuthorizationException("No tiene acceso a este taller");
  }
  private AppUser actor(String actorId) { return users.findById(UUID.fromString(actorId)).orElseThrow(() -> new WorkshopAuthorizationException("El usuario autenticado ya no existe")); }
  private boolean hasRole(AppUser user, RoleCode role) { return user.getRoles().stream().anyMatch(item -> item.getCode() == role); }
}
