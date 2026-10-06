package mx.taller.address;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import mx.taller.address.AddressCatalogFacade.LocalityResult;
import mx.taller.address.AddressCatalogFacade.Option;
import mx.taller.address.AddressCatalogFacade.PostalCodeResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/address-catalog")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','CUSTOMER_SERVICE')")
@Validated
public class AddressCatalogController {
  private final AddressCatalogFacade catalog;
  public AddressCatalogController(AddressCatalogFacade catalog) { this.catalog = catalog; }
  @GetMapping("/states") public List<Option> states() { return catalog.states(); }
  @GetMapping("/states/{stateId}/municipalities") public List<Option> municipalities(@PathVariable Long stateId) { return catalog.municipalities(stateId); }
  @GetMapping("/municipalities/{municipalityId}/localities") public List<Option> localities(@PathVariable Long municipalityId) { return catalog.localities(municipalityId); }
  @GetMapping("/postal-codes/{postalCode}") public PostalCodeResult postalCode(@PathVariable @Pattern(regexp = "[0-9]{5}") String postalCode) { return catalog.postalCode(postalCode); }
  @GetMapping("/localities/{localityId}") public LocalityResult locality(@PathVariable Long localityId) { return catalog.locality(localityId); }
}
