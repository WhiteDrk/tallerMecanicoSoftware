package mx.taller.vehicle;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import mx.taller.client.Client;
import mx.taller.status.StatusCatalog;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "vehicles")
public class Vehicle {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "client_id", nullable = false) private Client client;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "model_id", nullable = false) private VehicleModel model;
  @Column(nullable = false, unique = true, updatable = false, length = 17) private String vin;
  @Column(nullable = false, length = 10) private String plate;
  @Column(name = "plate_normalized", nullable = false, unique = true, length = 10) private String plateNormalized;
  @Column(name = "vehicle_year", nullable = false) private int year;
  @Column(nullable = false, length = 100) private String version;
  private Long mileage;
  @Column(name = "serial_number", nullable = false, unique = true, length = 40) private String serialNumber;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "status_id", nullable = false) private StatusCatalog status;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
  @Version @Column(nullable = false) private long revision;
  protected Vehicle() { }
  public Vehicle(Client client, VehicleModel model, VehicleRegistrationRequest request, String vin, String plate, String serialNumber, StatusCatalog status) {
    this.client = client; this.model = model; this.vin = vin; this.plate = plate; this.plateNormalized = plate; this.year = request.year(); this.version = request.version().trim(); this.mileage = request.mileage(); this.serialNumber = serialNumber; this.status = status;
  }
  public UUID getId() { return id; }
  public Client getClient() { return client; }
  public VehicleModel getModel() { return model; }
  public String getVin() { return vin; }
  public String getPlate() { return plate; }
  public int getYear() { return year; }
  public String getVersion() { return version; }
  public Long getMileage() { return mileage; }
  public String getSerialNumber() { return serialNumber; }
  public StatusCatalog getStatus() { return status; }
  public long getRevision() { return revision; }

  /** Updates only the current record; identity, owner and VIN are immutable here. */
  public void updateDetails(VehicleModel model, VehicleUpdateRequest request, String plate, String serialNumber) {
    this.model = model; this.plate = plate; this.plateNormalized = plate;
    this.year = request.year(); this.version = request.version().trim();
    this.mileage = request.mileage(); this.serialNumber = serialNumber;
    this.updatedAt = Instant.now();
  }

  public void deactivate(StatusCatalog inactive) {
    this.status = inactive;
    this.updatedAt = Instant.now();
  }
}
