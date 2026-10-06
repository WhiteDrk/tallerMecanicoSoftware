package mx.taller.identity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true, length = 40) private RoleCode code;
  @Column(nullable = false, length = 150) private String description;
  protected Role() { }
  public RoleCode getCode() { return code; }
}
