package mx.taller.address;

import jakarta.persistence.*;

@Entity
@Table(name = "address_states")
public class AddressState {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "sepomex_code", nullable = false, length = 2, unique = true) private String sepomexCode;
  @Column(nullable = false, length = 120) private String name;
  protected AddressState() { }
  public Long getId() { return id; }
  public String getName() { return name; }
}
