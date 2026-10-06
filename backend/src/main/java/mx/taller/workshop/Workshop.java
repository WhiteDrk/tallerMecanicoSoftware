package mx.taller.workshop;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import mx.taller.identity.AppUser;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "workshops")
public class Workshop {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @Column(nullable = false, length = 150) private String name;
  @Column(name = "legal_name", nullable = false, length = 200) private String legalName;
  @Column(nullable = false, length = 20) private String phone;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "manager_user_id", nullable = false) private AppUser manager;
  @Column(nullable = false, length = 13, unique = true) private String rfc;
  @Column(nullable = false, length = 254) private String email;
  @Column(nullable = false, length = 160) private String street;
  @Column(nullable = false, length = 120) private String neighborhood;
  @Column(nullable = false, length = 120) private String municipality;
  @Column(nullable = false, length = 120) private String state;
  @Column(name = "postal_code", nullable = false, length = 5) private String postalCode;
  @Lob @Basic(fetch = FetchType.LAZY) @Column(name = "photo_data", columnDefinition = "MEDIUMBLOB") private byte[] photoData;
  @Column(name = "photo_content_type", length = 50) private String photoContentType;
  @Column(nullable = false) private boolean active = true;
  @Column(name = "deactivated_at") private Instant deactivatedAt;

  protected Workshop() { }
  public Workshop(WorkshopRegistrationRequest request, AppUser manager, byte[] photoData, String photoContentType) {
    this.name = request.name().trim(); this.legalName = request.legalName().trim(); this.phone = request.phone().trim(); this.manager = manager;
    this.rfc = request.rfc().trim().toUpperCase(Locale.ROOT); this.email = request.email().trim().toLowerCase(Locale.ROOT);
    this.street = request.street().trim(); this.neighborhood = request.neighborhood().trim(); this.municipality = request.municipality().trim(); this.state = request.state().trim(); this.postalCode = request.postalCode().trim();
    this.photoData = photoData; this.photoContentType = photoContentType;
  }
  public UUID getId() { return id; }
  public String getName() { return name; }
  public String getRfc() { return rfc; }
  public String getLegalName() { return legalName; }
  public String getPhone() { return phone; }
  public AppUser getManager() { return manager; }
  public String getEmail() { return email; }
  public String getStreet() { return street; }
  public String getNeighborhood() { return neighborhood; }
  public String getMunicipality() { return municipality; }
  public String getState() { return state; }
  public String getPostalCode() { return postalCode; }
  public boolean isActive() { return active; }
  public void update(WorkshopRegistrationRequest request, AppUser manager, byte[] photoData, String photoContentType) {
    this.name = request.name().trim(); this.legalName = request.legalName().trim(); this.phone = request.phone().trim(); this.manager = manager;
    this.rfc = request.rfc().trim().toUpperCase(Locale.ROOT); this.email = request.email().trim().toLowerCase(Locale.ROOT);
    this.street = request.street().trim(); this.neighborhood = request.neighborhood().trim(); this.municipality = request.municipality().trim(); this.state = request.state().trim(); this.postalCode = request.postalCode().trim();
    if (photoData != null) { this.photoData = photoData; this.photoContentType = photoContentType; }
  }
  public void deactivate() { active = false; deactivatedAt = Instant.now(); }
}
