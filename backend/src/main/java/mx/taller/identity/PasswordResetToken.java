package mx.taller.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @ManyToOne(optional = false) @JoinColumn(name = "user_id") private AppUser user;
  @Column(name = "token_hash", nullable = false, length = 64, unique = true, columnDefinition = "char(64)") private String tokenHash;
  @Column(name = "expires_at", nullable = false) private Instant expiresAt;
  @Column(name = "used_at") private Instant usedAt;
  protected PasswordResetToken() { }
  public PasswordResetToken(AppUser user, String tokenHash, Instant expiresAt) { this.user=user; this.tokenHash=tokenHash; this.expiresAt=expiresAt; }
  public boolean isUsable(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
  public void use() { usedAt = Instant.now(); } public AppUser getUser() { return user; }
}
