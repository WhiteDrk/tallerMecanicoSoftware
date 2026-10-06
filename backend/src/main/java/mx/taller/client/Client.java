package mx.taller.client;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import mx.taller.workshop.Workshop;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "clients")
public class Client {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @Column(name="full_name", nullable=false, length=150) private String fullName;
  @Column(name="alternate_contact_name", nullable=false, length=150) private String alternateContactName;
  @Column(name="birth_date", nullable=false) private LocalDate birthDate;
  @Column(name="phone", nullable=false, length=20) private String phone;
  @Column(name="phone_normalized", nullable=false, length=20, unique=true) private String phoneNormalized;
  @Column(name="work_phone", nullable=false, length=20) private String workPhone;
  @Column(nullable=false, length=254) private String email;
  @Column(name="email_normalized", nullable=false, length=254, unique=true) private String emailNormalized;
  @Column(name="work_email", length=254) private String workEmail;
  @Column(nullable=false, length=160) private String street;
  @Column(nullable=false, length=120) private String neighborhood;
  @Column(nullable=false, length=120) private String municipality;
  @Column(nullable=false, length=120) private String state;
  @Column(name="postal_code", nullable=false, length=10) private String postalCode;
  @Lob @Basic(fetch=FetchType.LAZY) @Column(name="photo_data", columnDefinition="MEDIUMBLOB") private byte[] photoData;
  @Column(name="photo_content_type", length=50) private String photoContentType;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="workshop_id", nullable=false) private Workshop workshop;
  @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ClientStatus status = ClientStatus.ACTIVE;
  protected Client() { }
  public Client(ClientRegistrationRequest data, Workshop workshop, byte[] photoData, String photoContentType) {
    this.fullName=data.fullName().trim(); this.alternateContactName=data.alternateContactName().trim(); this.birthDate=data.birthDate(); this.phone=data.phone().trim(); this.phoneNormalized=normalizePhone(data.phone()); this.workPhone=data.workPhone().trim(); this.email=data.email().trim(); this.emailNormalized=data.email().trim().toLowerCase(); this.workEmail=blankToNull(data.workEmail()); this.street=data.street().trim(); this.neighborhood=data.neighborhood().trim(); this.municipality=data.municipality().trim(); this.state=data.state().trim(); this.postalCode=data.postalCode().trim(); this.workshop=workshop; this.photoData=photoData; this.photoContentType=photoContentType;
  }
  public UUID getId() { return id; } public String getFullName() { return fullName; }
  public String getEmail() { return email; } public Workshop getWorkshop() { return workshop; } public ClientStatus getStatus() { return status; }
  public String getAlternateContactName() { return alternateContactName; } public LocalDate getBirthDate() { return birthDate; } public String getPhone() { return phone; } public String getWorkPhone() { return workPhone; } public String getWorkEmail() { return workEmail; } public String getStreet() { return street; } public String getNeighborhood() { return neighborhood; } public String getMunicipality() { return municipality; } public String getState() { return state; } public String getPostalCode() { return postalCode; }
  public void update(ClientRegistrationRequest data, Workshop workshop, byte[] photoData, String photoContentType) {
    this.fullName=data.fullName().trim(); this.alternateContactName=data.alternateContactName().trim(); this.birthDate=data.birthDate(); this.phone=data.phone().trim(); this.phoneNormalized=normalizePhone(data.phone()); this.workPhone=data.workPhone().trim(); this.email=data.email().trim(); this.emailNormalized=data.email().trim().toLowerCase(); this.workEmail=blankToNull(data.workEmail()); this.street=data.street().trim(); this.neighborhood=data.neighborhood().trim(); this.municipality=data.municipality().trim(); this.state=data.state().trim(); this.postalCode=data.postalCode().trim(); this.workshop=workshop;
    if (photoData != null) { this.photoData = photoData; this.photoContentType = photoContentType; }
  }
  public void suspend() { status = ClientStatus.SUSPENDED; }
  public static String normalizePhone(String value) { return value.replaceAll("[^0-9]", ""); }
  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
