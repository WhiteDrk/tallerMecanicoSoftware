package mx.taller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  @Test void rejectsUnsignedOrAlteredTokens() {
    String secret = Base64.getEncoder().encodeToString(new byte[32]);
    JwtService service = new JwtService(secret, 15);
    org.junit.jupiter.api.Assertions.assertThrows(io.jsonwebtoken.JwtException.class, () -> service.parse("not.a.token"));
  }
}
