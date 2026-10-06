package mx.taller.address;

import jakarta.persistence.*;

@Entity
@Table(name = "address_postal_codes")
public class AddressPostalCode {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, length = 5, unique = true) private String code;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "state_id") private AddressState state;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "municipality_id") private AddressMunicipality municipality;
  @Column(length = 120) private String city;
  protected AddressPostalCode() { }
  public Long getId() { return id; }
  public String getCode() { return code; }
  public AddressState getState() { return state; }
  public AddressMunicipality getMunicipality() { return municipality; }
}
