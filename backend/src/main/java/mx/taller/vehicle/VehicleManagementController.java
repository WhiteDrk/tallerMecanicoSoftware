package mx.taller.vehicle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles/{id}")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
public class VehicleManagementController {
  private final VehicleManagementFacade facade;
  public VehicleManagementController(VehicleManagementFacade facade) { this.facade = facade; }

  @PutMapping
  public VehicleQueryFacade.VehicleDetail update(@PathVariable UUID id, @Valid @RequestBody VehicleUpdateRequest request,
      Authentication auth, HttpServletRequest http) {
    return facade.update(id, request, auth.getName(), http.getRemoteAddr());
  }

  @PostMapping("/deactivation")
  public VehicleQueryFacade.VehicleDetail deactivate(@PathVariable UUID id, @Valid @RequestBody VehicleDeactivationRequest request,
      Authentication auth, HttpServletRequest http) {
    return facade.deactivate(id, request, auth.getName(), http.getRemoteAddr());
  }
}
