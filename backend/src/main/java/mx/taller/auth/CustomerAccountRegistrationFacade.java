package mx.taller.auth;

import mx.taller.audit.AuditService;
import mx.taller.client.Client;
import mx.taller.client.ClientRepository;
import mx.taller.identity.AppUser;
import mx.taller.identity.AppUserRepository;
import mx.taller.identity.Role;
import mx.taller.identity.RoleCode;
import mx.taller.identity.RoleRepository;
import mx.taller.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates one CUSTOMER identity for an existing, active client; it never creates client data. */
@Service
public class CustomerAccountRegistrationFacade {
  private final AppUserRepository users;
  private final ClientRepository clients;
  private final RoleRepository roles;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final AuditService audit;

  public CustomerAccountRegistrationFacade(AppUserRepository users, ClientRepository clients, RoleRepository roles, PasswordEncoder passwords, JwtService jwt, AuditService audit) {
    this.users = users; this.clients = clients; this.roles = roles; this.passwords = passwords; this.jwt = jwt; this.audit = audit;
  }

  @Transactional
  public AuthController.TokenResponse register(AuthController.Register request, String ip) {
    String email = request.email().trim().toLowerCase();
    if (!request.password().equals(request.confirmPassword())) throw new ConflictException("Las contraseñas no coinciden");
    if (users.findByEmailIgnoreCase(email).isPresent()) throw new ConflictException("Ya existe una cuenta para este correo");

    Client client = clients.findByEmailNormalized(email).orElseThrow(() -> new ConflictException("No existe un cliente activo con los datos proporcionados"));
    if (!client.hasStatus("ACTIVO") || !sameName(client.getFullName(), request.fullName())) {
      throw new ConflictException("No existe un cliente activo con los datos proporcionados");
    }

    Role customer = roles.findByCode(RoleCode.CUSTOMER).orElseThrow(() -> new IllegalStateException("Default role is missing"));
    AppUser user = new AppUser(email, client.getFullName(), passwords.encode(request.password()), customer, client);
    user.enable();
    users.saveAndFlush(user);
    audit.record(user, "CUSTOMER_ACCOUNT_REGISTERED", "CLIENT", client.getId().toString(), ip, "{\"role\":\"CUSTOMER\"}");
    return new AuthController.TokenResponse(jwt.issue(user), "Bearer", jwt.expiresInSeconds());
  }

  private boolean sameName(String stored, String supplied) {
    return stored.trim().replaceAll("\\s+", " ").equalsIgnoreCase(supplied.trim().replaceAll("\\s+", " "));
  }
}
