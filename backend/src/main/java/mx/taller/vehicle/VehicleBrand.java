package mx.taller.vehicle;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_brands")
public class VehicleBrand {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true, length = 100) private String name;
  @Column(nullable = false) private boolean enabled = true;
  @Column(name = "source_origin", nullable = false, length = 30) private String sourceOrigin = "LEGACY_SEED";
  @Column(name = "pending_review", nullable = false) private boolean pendingReview;
  protected VehicleBrand() { }
  private VehicleBrand(String name, boolean enabled, String sourceOrigin, boolean pendingReview) { this.name = name; this.enabled = enabled; this.sourceOrigin = sourceOrigin; this.pendingReview = pendingReview; }
  public static VehicleBrand manualPending(String name) { return new VehicleBrand(name, false, "MANUAL_PENDING", true); }
  public Long getId() { return id; } public String getName() { return name; } public boolean isEnabled() { return enabled; } public boolean isPendingReview() { return pendingReview; }
}
