package mx.taller.vehicle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/vehicles")
public class VehicleController {
  private final VehicleRegistrationFacade facade;
  public VehicleController(VehicleRegistrationFacade facade) { this.facade = facade; }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
  public VehicleRegistrationFacade.VehicleResult register(@PathVariable UUID clientId, @Valid @RequestBody VehicleRegistrationRequest request, Authentication authentication, HttpServletRequest servletRequest) { return facade.register(clientId, request, authentication.getName(), servletRequest.getRemoteAddr()); }
}
