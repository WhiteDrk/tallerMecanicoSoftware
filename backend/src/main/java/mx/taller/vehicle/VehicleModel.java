package mx.taller.vehicle;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_models", uniqueConstraints = @UniqueConstraint(name = "uq_vehicle_models_brand_name", columnNames = {"brand_id", "name"}))
public class VehicleModel {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "brand_id", nullable = false) private VehicleBrand brand;
  @Column(nullable = false, length = 100) private String name;
  @Column(nullable = false) private boolean enabled = true;
  @Column(name = "source_origin", nullable = false, length = 30) private String sourceOrigin = "LEGACY_SEED";
  @Column(name = "pending_review", nullable = false) private boolean pendingReview;
  protected VehicleModel() { }
  private VehicleModel(VehicleBrand brand, String name, boolean enabled, String sourceOrigin, boolean pendingReview) { this.brand = brand; this.name = name; this.enabled = enabled; this.sourceOrigin = sourceOrigin; this.pendingReview = pendingReview; }
  public static VehicleModel manualPending(VehicleBrand brand, String name) { return new VehicleModel(brand, name, false, "MANUAL_PENDING", true); }
  public Long getId() { return id; } public VehicleBrand getBrand() { return brand; } public String getName() { return name; } public boolean isEnabled() { return enabled; } public boolean isPendingReview() { return pendingReview; }
}
