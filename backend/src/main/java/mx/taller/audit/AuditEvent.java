package mx.taller.audit;

import jakarta.persistence.*;
import java.time.Instant;
import mx.taller.identity.AppUser;

@Entity @Table(name = "audit_events")
public class AuditEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();
  @ManyToOne @JoinColumn(name = "actor_id") private AppUser actor;
  @Column(nullable = false, length = 100) private String action;
  @Column(name = "entity_type", nullable = false, length = 60) private String entityType;
  @Column(name = "entity_id", length = 80) private String entityId;
  @Column(name = "ip_hash", length = 64, columnDefinition = "char(64)") private String ipHash;
  @Column(columnDefinition = "json") private String details;
  protected AuditEvent() { }
  public AuditEvent(AppUser actor, String action, String entityType, String entityId, String ipHash, String details) { this.actor=actor; this.action=action; this.entityType=entityType; this.entityId=entityId; this.ipHash=ipHash; this.details=details; }
}
