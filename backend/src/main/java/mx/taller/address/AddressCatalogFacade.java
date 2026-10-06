package mx.taller.address;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressCatalogFacade {
  private final AddressStateRepository states;
  private final AddressMunicipalityRepository municipalities;
  private final AddressPostalCodeRepository postalCodes;
  private final AddressLocalityRepository localities;

  public AddressCatalogFacade(AddressStateRepository states, AddressMunicipalityRepository municipalities, AddressPostalCodeRepository postalCodes, AddressLocalityRepository localities) {
    this.states = states; this.municipalities = municipalities; this.postalCodes = postalCodes; this.localities = localities;
  }

  @Transactional(readOnly = true) public List<Option> states() { return states.findAllByOrderByNameAsc().stream().map(item -> new Option(item.getId(), item.getName())).toList(); }
  @Transactional(readOnly = true) public List<Option> municipalities(Long stateId) { return municipalities.findByStateIdOrderByNameAsc(stateId).stream().map(item -> new Option(item.getId(), item.getName())).toList(); }
  @Transactional(readOnly = true) public List<Option> localities(Long municipalityId) { return localities.findByPostalCodeMunicipalityIdOrderByNameAsc(municipalityId).stream().map(item -> new Option(item.getId(), item.getName())).toList(); }

  @Transactional(readOnly = true) public PostalCodeResult postalCode(String code) {
    AddressPostalCode postalCode = postalCodes.findByCode(code).orElseThrow(() -> new AddressCatalogNotFoundException("El código postal no existe en el catálogo SEPOMEX"));
    return new PostalCodeResult(postalCode.getCode(), new Option(postalCode.getState().getId(), postalCode.getState().getName()), new Option(postalCode.getMunicipality().getId(), postalCode.getMunicipality().getName()), localities.findByPostalCodeIdOrderByNameAsc(postalCode.getId()).stream().map(item -> new Option(item.getId(), item.getName())).toList());
  }

  @Transactional(readOnly = true) public LocalityResult locality(Long localityId) {
    AddressLocality locality = localities.findById(localityId).orElseThrow(() -> new AddressCatalogNotFoundException("La localidad no existe en el catálogo SEPOMEX"));
    return new LocalityResult(locality.getId(), locality.getName(), locality.getPostalCode().getCode());
  }

  public record Option(Long id, String name) { }
  public record PostalCodeResult(String postalCode, Option state, Option municipality, List<Option> localities) { }
  public record LocalityResult(Long id, String name, String postalCode) { }
}
