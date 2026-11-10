package mx.taller.vehicle;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleQueryController {
  private final VehicleQueryFacade facade;
  public VehicleQueryController(VehicleQueryFacade facade) { this.facade = facade; }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
  public VehicleQueryFacade.VehicleDetail find(@RequestParam MultiValueMap<String, String> params, Authentication auth) {
    if (params.size() != 2 || !params.containsKey("criterion") || !params.containsKey("value")
        || params.get("criterion").size() != 1 || params.get("value").size() != 1)
      throw new VehicleValidationException("Envía únicamente un criterio y un valor");
    return facade.find(auth.getName(), params.getFirst("criterion"), params.getFirst("value"));
  }
}
