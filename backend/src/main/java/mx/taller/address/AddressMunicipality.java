package mx.taller.address;

import jakarta.persistence.*;

@Entity
@Table(name = "address_municipalities")
public class AddressMunicipality {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "state_id") private AddressState state;
  @Column(name = "sepomex_code", nullable = false, length = 3) private String sepomexCode;
  @Column(nullable = false, length = 120) private String name;
  protected AddressMunicipality() { }
  public Long getId() { return id; }
  public String getName() { return name; }
}
