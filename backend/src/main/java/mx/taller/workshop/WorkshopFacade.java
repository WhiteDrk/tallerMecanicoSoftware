package mx.taller.workshop;

import java.util.List;
import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.identity.RoleCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkshopFacade {
  private final WorkshopRepository workshops; private final AppUserRepository users; private final WorkshopPhotoValidator photoValidator; private final WorkshopAddressValidator addressValidator; private final AuditService audit;
  public WorkshopFacade(WorkshopRepository workshops, AppUserRepository users, WorkshopPhotoValidator photoValidator, WorkshopAddressValidator addressValidator, AuditService audit) { this.workshops = workshops; this.users = users; this.photoValidator = photoValidator; this.addressValidator = addressValidator; this.audit = audit; }

  @Transactional(readOnly = true) public List<ManagerOption> managers(String actorId) {
    AppUser actor = actor(actorId);
    if (hasRole(actor, RoleCode.OWNER)) return users.findAll().stream().filter(user -> user.isEnabled() && hasRole(user, RoleCode.MANAGER)).map(this::option).toList();
    if (hasRole(actor, RoleCode.MANAGER)) return List.of(option(actor));
    throw new WorkshopAuthorizationException("No tiene permisos para consultar gerentes");
  }

  @Transactional(readOnly = true) public List<WorkshopDetail> list(String actorId) {
    AppUser actor = actor(actorId);
    List<Workshop> result = hasRole(actor, RoleCode.OWNER) ? workshops.findAllByOrderByNameAsc() : workshops.findByManagerIdOrderByNameAsc(actor.getId());
    if (!hasRole(actor, RoleCode.OWNER) && !hasRole(actor, RoleCode.MANAGER)) throw new WorkshopAuthorizationException("No tiene permisos para consultar talleres");
    return result.stream().map(workshop -> detail(workshop, hasRole(actor, RoleCode.OWNER))).toList();
  }
  @Transactional(readOnly = true) public List<WorkshopOption> accessible(String actorId, WorkshopAccessService access) { return access.accessible(actorId).stream().map(workshop -> new WorkshopOption(workshop.getId(), workshop.getName())).toList(); }

  @Transactional(readOnly = true) public WorkshopDetail get(UUID workshopId, String actorId) { AppUser actor = actor(actorId); return detail(authorize(workshopId, actor), hasRole(actor, RoleCode.OWNER)); }

  @Transactional public WorkshopResult register(WorkshopRegistrationRequest request, MultipartFile photo, String actorId, String ip) {
    AppUser actor = actor(actorId);
    AppUser manager = manager(request.managerId());
    authorizeAssignment(actor, manager);
    String rfc = request.rfc().trim();
    if (workshops.existsByRfcIgnoreCase(rfc)) throw new DuplicateWorkshopException();
    addressValidator.validate(request);
    WorkshopPhotoValidator.ValidatedPhoto validatedPhoto = photoValidator.validate(photo);
    Workshop workshop;
    try { workshop = workshops.saveAndFlush(new Workshop(request, manager, validatedPhoto.bytes(), validatedPhoto.contentType())); }
    catch (DataIntegrityViolationException error) { throw new DuplicateWorkshopException(); }
    audit.record(actor, "WORKSHOP_REGISTERED", "WORKSHOP", workshop.getId().toString(), ip, "{\"rfc\":\"" + workshop.getRfc() + "\",\"photoAttached\":" + (validatedPhoto.bytes() != null) + "}");
    return new WorkshopResult(workshop.getId(), workshop.getName());
  }
  @Transactional public WorkshopResult update(UUID workshopId, WorkshopRegistrationRequest request, MultipartFile photo, String actorId, String ip) {
    AppUser actor = actor(actorId); Workshop workshop = authorize(workshopId, actor);
    if (!workshop.isActive()) throw new WorkshopAuthorizationException("No se puede editar un taller desactivado");
    AppUser manager = manager(request.managerId()); authorizeAssignment(actor, manager);
    if (workshops.existsByRfcIgnoreCaseAndIdNot(request.rfc().trim(), workshopId)) throw new DuplicateWorkshopException();
    addressValidator.validate(request);
    WorkshopPhotoValidator.ValidatedPhoto validatedPhoto = photoValidator.validate(photo);
    workshop.update(request, manager, validatedPhoto.bytes(), validatedPhoto.contentType());
    audit.record(actor, "WORKSHOP_UPDATED", "WORKSHOP", workshop.getId().toString(), ip, "{\"rfc\":\"" + workshop.getRfc() + "\",\"photoUpdated\":" + (validatedPhoto.bytes() != null) + "}");
    return new WorkshopResult(workshop.getId(), workshop.getName());
  }
  @Transactional public void deactivate(UUID workshopId, String actorId, String ip) {
    AppUser actor = actor(actorId);
    if (!hasRole(actor, RoleCode.OWNER)) throw new WorkshopAuthorizationException("Sólo Administración puede desactivar talleres");
    Workshop workshop = workshops.findById(workshopId).orElseThrow(() -> new WorkshopAuthorizationException("El taller no existe"));
    if (!workshop.isActive()) throw new WorkshopAuthorizationException("El taller ya está desactivado");
    workshop.deactivate();
    audit.record(actor, "WORKSHOP_DEACTIVATED", "WORKSHOP", workshop.getId().toString(), ip, "{}");
  }
  private AppUser actor(String actorId) { return users.findById(UUID.fromString(actorId)).orElseThrow(() -> new WorkshopAuthorizationException("El usuario autenticado ya no existe")); }
  private AppUser manager(UUID managerId) {
    AppUser manager = users.findById(managerId).orElseThrow(() -> new WorkshopAuthorizationException("El gerente seleccionado no existe"));
    if (!manager.isEnabled() || !hasRole(manager, RoleCode.MANAGER)) throw new WorkshopAuthorizationException("El usuario seleccionado no es un gerente activo");
    return manager;
  }
  private void authorizeAssignment(AppUser actor, AppUser manager) {
    if (!hasRole(actor, RoleCode.OWNER) && (!hasRole(actor, RoleCode.MANAGER) || !actor.getId().equals(manager.getId()))) throw new WorkshopAuthorizationException("Un gerente sólo puede asignarse a sí mismo");
  }
  private Workshop authorize(UUID workshopId, AppUser actor) {
    Workshop workshop = workshops.findById(workshopId).orElseThrow(() -> new WorkshopAuthorizationException("El taller no existe"));
    if (hasRole(actor, RoleCode.OWNER) || hasRole(actor, RoleCode.MANAGER) && workshop.getManager().getId().equals(actor.getId())) return workshop;
    throw new WorkshopAuthorizationException("No tiene acceso a este taller");
  }
  private boolean hasRole(AppUser user, RoleCode role) { return user.getRoles().stream().anyMatch(item -> item.getCode() == role); }
  private ManagerOption option(AppUser user) { return new ManagerOption(user.getId(), user.getFullName(), user.getEmail()); }
  private WorkshopDetail detail(Workshop workshop, boolean canDeactivate) { return new WorkshopDetail(workshop.getId(), workshop.getName(), workshop.getLegalName(), workshop.getPhone(), workshop.getManager().getId(), workshop.getManager().getFullName(), workshop.getRfc(), workshop.getEmail(), workshop.getStreet(), workshop.getNeighborhood(), workshop.getMunicipality(), workshop.getState(), workshop.getPostalCode(), workshop.isActive(), canDeactivate); }
  public record ManagerOption(UUID id, String fullName, String email) { }
  public record WorkshopResult(UUID id, String name) { }
  public record WorkshopOption(UUID id, String name) { }
  public record WorkshopDetail(UUID id, String name, String legalName, String phone, UUID managerId, String managerName, String rfc, String email, String street, String neighborhood, String municipality, String state, String postalCode, boolean active, boolean canDeactivate) { }
}
