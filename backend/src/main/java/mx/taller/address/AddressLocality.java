package mx.taller.address;

import jakarta.persistence.*;

@Entity
@Table(name = "address_localities")
public class AddressLocality {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "postal_code_id") private AddressPostalCode postalCode;
  @Column(name = "sepomex_code", nullable = false, length = 4) private String sepomexCode;
  @Column(nullable = false, length = 120) private String name;
  @Column(name = "settlement_type", length = 80) private String settlementType;
  protected AddressLocality() { }
  public Long getId() { return id; }
  public String getName() { return name; }
  public AddressPostalCode getPostalCode() { return postalCode; }
}
