package mx.taller.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import mx.taller.identity.AppUser;
import org.springframework.stereotype.Service;

@Service public class AuditService {
  private final AuditEventRepository repository;
  public AuditService(AuditEventRepository repository) { this.repository = repository; }
  public void record(AppUser actor, String action, String entityType, String entityId, String ip, String details) { repository.save(new AuditEvent(actor, action, entityType, entityId, hash(ip), details)); }
  private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
}
