package mx.taller.status;

import java.util.List;
import mx.taller.audit.AuditService;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusCatalogFacade {
  private final StatusCatalogRepository statuses;
  private final AppUserRepository users;
  private final AuditService audit;
  public StatusCatalogFacade(StatusCatalogRepository statuses, AppUserRepository users, AuditService audit) { this.statuses = statuses; this.users = users; this.audit = audit; }

  @Transactional(readOnly = true) public List<StatusResult> list() { return statuses.findAllByOrderByValueAsc().stream().map(this::result).toList(); }
  @Transactional(readOnly = true) public StatusCatalog requiredActive(String value) {
    StatusCatalog status = required(value);
    if (!status.isEnabled()) throw new StatusCatalogException("El estatus " + value + " está inhabilitado");
    return status;
  }
  @Transactional public StatusResult create(StatusRequest request, String actorId, String ip) {
    String value = normalizeValue(request.value());
    if (statuses.existsByValue(value)) throw new StatusCatalogException("Ya existe un estatus con ese valor");
    StatusCatalog status = statuses.save(new StatusCatalog(value, normalizeDescription(request.description())));
    audit.record(actor(actorId), "STATUS_CREATED", "STATUS_CATALOG", status.getId().toString(), ip, "{\"value\":\"" + value + "\"}");
    return result(status);
  }
  @Transactional public StatusResult update(Long id, StatusRequest request, String actorId, String ip) {
    StatusCatalog status = required(id);
    status.updateDescription(normalizeDescription(request.description()));
    status.changeEnabled(request.enabled());
    audit.record(actor(actorId), "STATUS_UPDATED", "STATUS_CATALOG", status.getId().toString(), ip, "{\"enabled\":" + status.isEnabled() + "}");
    return result(status);
  }
  private StatusCatalog required(Long id) { return statuses.findById(id).orElseThrow(() -> new StatusCatalogException("El estatus no existe")); }
  private StatusCatalog required(String value) { return statuses.findByValue(value).orElseThrow(() -> new StatusCatalogException("No existe el estatus requerido: " + value)); }
  private AppUser actor(String id) { return users.findById(java.util.UUID.fromString(id)).orElseThrow(() -> new StatusCatalogException("El usuario autenticado ya no existe")); }
  private String normalizeValue(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", "_");
    if (!normalized.matches("[A-Z0-9_]{3,40}")) throw new StatusCatalogException("El valor debe usar 3 a 40 caracteres: letras, números o guion bajo");
    return normalized;
  }
  private String normalizeDescription(String description) {
    String normalized = description == null ? "" : description.trim();
    if (normalized.isBlank() || normalized.length() > 255) throw new StatusCatalogException("La descripción es obligatoria y no debe exceder 255 caracteres");
    return normalized;
  }
  private StatusResult result(StatusCatalog status) { return new StatusResult(status.getId(), status.getValue(), status.getDescription(), status.isEnabled(), status.isSystemBase()); }
  public record StatusRequest(String value, String description, boolean enabled) { }
  public record StatusResult(Long id, String value, String description, boolean enabled, boolean systemBase) { }
}
