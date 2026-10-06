package mx.taller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  public AuthController(AuthService authService) { this.authService = authService; }
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> register(@Valid @RequestBody Register request, HttpServletRequest servletRequest) { authService.register(request, servletRequest.getRemoteAddr()); return Map.of("message", "Solicitud registrada. Requiere aprobación de Gerencia."); }
  @PostMapping("/login") public TokenResponse login(@Valid @RequestBody Login request, HttpServletRequest servletRequest) { return authService.login(request, servletRequest.getRemoteAddr()); }
  @PostMapping("/password-recovery") @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> recovery(@Valid @RequestBody Recovery request, HttpServletRequest servletRequest) { authService.requestRecovery(request.email(), servletRequest.getRemoteAddr()); return Map.of("message", "Si la cuenta existe, enviamos instrucciones de recuperación."); }
  @PostMapping("/password-reset") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reset(@Valid @RequestBody ResetPassword request, HttpServletRequest servletRequest) { authService.resetPassword(request, servletRequest.getRemoteAddr()); }
  public record Register(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(min=12,max=72) String password, @NotBlank @Size(max=150) String fullName) {}
  public record Login(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
  public record Recovery(@Email @NotBlank @Size(max=254) String email) {}
  public record ResetPassword(@NotBlank @Size(max=512) String token, @NotBlank @Size(min=12,max=72) String password) {}
  public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
