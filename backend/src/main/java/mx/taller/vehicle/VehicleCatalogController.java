package mx.taller.vehicle;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicle-catalog")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
public class VehicleCatalogController {
  private final VehicleCatalogFacade facade;
  public VehicleCatalogController(VehicleCatalogFacade facade) { this.facade = facade; }
  @GetMapping("/brands") public List<VehicleCatalogFacade.BrandOption> brands() { return facade.brands(); }
  @GetMapping("/brands/{brandId}/models") public List<VehicleCatalogFacade.ModelOption> models(@PathVariable Long brandId) { return facade.models(brandId); }
}
