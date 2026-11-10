package mx.taller.status;

import jakarta.persistence.*;

@Entity
@Table(name = "status_catalog")
public class StatusCatalog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "str_valor", nullable = false, unique = true, length = 40) private String value;
  @Column(name = "str_descripcion", nullable = false, length = 255) private String description;
  @Column(nullable = false) private boolean enabled = true;
  @Column(name = "system_base", nullable = false) private boolean systemBase;
  protected StatusCatalog() { }
  public StatusCatalog(String value, String description) { this.value = value; this.description = description; }
  public Long getId() { return id; } public String getValue() { return value; } public String getDescription() { return description; } public boolean isEnabled() { return enabled; } public boolean isSystemBase() { return systemBase; }
  public void updateDescription(String value) { description = value; }
  public void changeEnabled(boolean value) { if (systemBase && !value) throw new StatusCatalogException("Los estatus base no pueden inhabilitarse"); enabled = value; }
}
