package mx.taller.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import mx.taller.identity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final Duration accessLifetime;
  public JwtService(@Value("${app.jwt.secret}") String encodedSecret, @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
    if (encodedSecret == null || encodedSecret.isBlank()) throw new IllegalStateException("APP_JWT_SECRET is required");
    byte[] secret;
    try { secret = Decoders.BASE64.decode(encodedSecret); }
    catch (IllegalArgumentException error) { throw new IllegalStateException("APP_JWT_SECRET must be Base64", error); }
    if (secret.length < 32) throw new IllegalStateException("APP_JWT_SECRET must contain at least 256 bits");
    this.key = Keys.hmacShaKeyFor(secret);
    this.accessLifetime = Duration.ofMinutes(accessTokenMinutes);
  }
  public String issue(AppUser user) {
    Instant now = Instant.now();
    List<String> roles = user.getRoles().stream().map(role -> "ROLE_" + role.getCode().name()).sorted().toList();
    return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail()).claim("roles", roles)
      .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessLifetime))).signWith(key).compact();
  }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
  public long expiresInSeconds() { return accessLifetime.toSeconds(); }
}
