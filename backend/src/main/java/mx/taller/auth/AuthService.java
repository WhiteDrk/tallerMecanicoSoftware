package mx.taller.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import mx.taller.audit.AuditService;
import mx.taller.identity.*;
import mx.taller.security.JwtService;
import mx.taller.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final AppUserRepository users; private final RoleRepository roles; private final PasswordResetTokenRepository resetTokens; private final PasswordEncoder passwords; private final JwtService jwt; private final LoginRateLimiter rateLimiter; private final AuditService audit;
  public AuthService(AppUserRepository users, RoleRepository roles, PasswordResetTokenRepository resetTokens, PasswordEncoder passwords, JwtService jwt, LoginRateLimiter rateLimiter, AuditService audit) { this.users=users; this.roles=roles; this.resetTokens=resetTokens; this.passwords=passwords; this.jwt=jwt; this.rateLimiter=rateLimiter; this.audit=audit; }
  @Transactional public void register(AuthController.Register request, String ip) {
    if (users.findByEmailIgnoreCase(request.email()).isPresent()) throw new ConflictException("No se pudo registrar la solicitud");
    Role customer = roles.findByCode(RoleCode.CUSTOMER).orElseThrow(() -> new IllegalStateException("Default role is missing"));
    AppUser user = users.save(new AppUser(request.email(), request.fullName().trim(), passwords.encode(request.password()), customer));
    audit.record(user, "USER_REGISTERED", "USER", user.getId().toString(), ip, "{\"approvalRequired\":true}");
  }
  @Transactional public AuthController.TokenResponse login(AuthController.Login request, String ip) {
    String key = request.email().toLowerCase() + "|" + ip;
    if (!rateLimiter.allowed(key)) throw new AuthenticationFailedException();
    AppUser user = users.findByEmailIgnoreCase(request.email()).orElse(null);
    if (user == null || !user.isEnabled() || !passwords.matches(request.password(), user.getPasswordHash())) { rateLimiter.failure(key); if (user != null) audit.record(user, "LOGIN_FAILED", "USER", user.getId().toString(), ip, "{}"); throw new AuthenticationFailedException(); }
    rateLimiter.success(key); audit.record(user, "LOGIN_SUCCEEDED", "USER", user.getId().toString(), ip, "{}");
    return new AuthController.TokenResponse(jwt.issue(user), "Bearer", jwt.expiresInSeconds());
  }
  @Transactional public void requestRecovery(String email, String ip) {
    users.findByEmailIgnoreCase(email).filter(AppUser::isEnabled).ifPresent(user -> { byte[] raw = new byte[32]; new SecureRandom().nextBytes(raw); String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw); resetTokens.save(new PasswordResetToken(user, sha256(token), Instant.now().plusSeconds(900))); audit.record(user, "PASSWORD_RECOVERY_REQUESTED", "USER", user.getId().toString(), ip, "{}"); /* Integrate approved transactional email; never log or return token. */ });
  }
  @Transactional public void resetPassword(AuthController.ResetPassword request, String ip) {
    PasswordResetToken token = resetTokens.findByTokenHash(sha256(request.token())).filter(item -> item.isUsable(Instant.now())).orElseThrow(InvalidResetTokenException::new);
    token.getUser().changePassword(passwords.encode(request.password())); token.use(); audit.record(token.getUser(), "PASSWORD_RESET", "USER", token.getUser().getId().toString(), ip, "{}");
  }
  private String sha256(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception error) { throw new IllegalStateException(error); } }
}
