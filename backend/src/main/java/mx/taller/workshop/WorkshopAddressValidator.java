package mx.taller.workshop;

import mx.taller.address.AddressCatalogNotFoundException;
import mx.taller.address.AddressLocalityRepository;
import mx.taller.address.AddressPostalCode;
import mx.taller.address.AddressPostalCodeRepository;
import org.springframework.stereotype.Component;

@Component
public class WorkshopAddressValidator {
  private final AddressPostalCodeRepository postalCodes;
  private final AddressLocalityRepository localities;
  public WorkshopAddressValidator(AddressPostalCodeRepository postalCodes, AddressLocalityRepository localities) { this.postalCodes = postalCodes; this.localities = localities; }
  public void validate(WorkshopRegistrationRequest request) {
    AddressPostalCode postalCode = postalCodes.findByCode(request.postalCode()).orElseThrow(() -> new AddressCatalogNotFoundException("El código postal no existe en el catálogo SEPOMEX"));
    boolean valid = postalCode.getState().getName().equalsIgnoreCase(request.state().trim())
      && postalCode.getMunicipality().getName().equalsIgnoreCase(request.municipality().trim())
      && localities.existsByPostalCodeIdAndNameIgnoreCase(postalCode.getId(), request.neighborhood().trim());
    if (!valid) throw new AddressCatalogNotFoundException("La colonia, municipio, estado y código postal no corresponden entre sí");
  }
}
