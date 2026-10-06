package mx.taller.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class AppUser {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @Column(nullable = false, unique = true, length = 254) private String email;
  @Column(name = "full_name", nullable = false, length = 150) private String fullName;
  @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
  @Column(nullable = false) private boolean enabled;
  @Column(name = "mfa_enabled", nullable = false) private boolean mfaEnabled;
  @ManyToMany(fetch = FetchType.EAGER) @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id")) private Set<Role> roles = new HashSet<>();
  protected AppUser() { }
  public AppUser(String email, String fullName, String passwordHash, Role role) { this.email = email.toLowerCase(); this.fullName = fullName; this.passwordHash = passwordHash; this.roles.add(role); }
  public UUID getId() { return id; } public String getEmail() { return email; } public String getFullName() { return fullName; } public String getPasswordHash() { return passwordHash; } public boolean isEnabled() { return enabled; } public Set<Role> getRoles() { return Set.copyOf(roles); }
  public void enable() { enabled = true; } public void changePassword(String hash) { passwordHash = hash; }
}
