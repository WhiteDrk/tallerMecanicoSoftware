package mx.taller.client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

@RestController @RequestMapping("/api/v1/clients") @Validated
public class ClientController {
  private final ClientRegistrationFacade registrationFacade;
  private final ClientQueryFacade queryFacade;
  private final ClientManagementFacade managementFacade;
  public ClientController(ClientRegistrationFacade registrationFacade, ClientQueryFacade queryFacade, ClientManagementFacade managementFacade) { this.registrationFacade = registrationFacade; this.queryFacade = queryFacade; this.managementFacade = managementFacade; }
  @GetMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER','RECEPCIONISTA','SECRETARIA')")
  public ClientQueryFacade.ClientPage list(@RequestParam UUID workshopId, @RequestParam(defaultValue="0") @jakarta.validation.constraints.Min(0) int page, @RequestParam(defaultValue="10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10) int size, @RequestParam(defaultValue="fullName") String sort, @RequestParam(defaultValue="asc") String direction, Authentication authentication) {
    return queryFacade.list(authentication.getName(), workshopId, page, size, sort, direction);
  }
  @GetMapping("/{clientId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER','RECEPCIONISTA','SECRETARIA')")
  public ClientManagementFacade.ClientDetail get(@PathVariable UUID clientId, Authentication authentication) { return managementFacade.get(clientId, authentication.getName()); }
  @GetMapping("/me") @PreAuthorize("hasRole('CUSTOMER')")
  public ClientManagementFacade.ClientDetail own(Authentication authentication) { return managementFacade.own(authentication.getName()); }
  @PostMapping(consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER','RECEPCIONISTA','SECRETARIA')")
  public Map<String,Object> register(@Valid @RequestPart("data") ClientRegistrationRequest data, @RequestPart(value="photo", required=false) MultipartFile photo, Authentication authentication, HttpServletRequest request) {
    var created = registrationFacade.register(data, photo, authentication.getName(), request.getRemoteAddr());
    return Map.of("id", created.id(), "fullName", created.fullName(), "message", "Cliente registrado exitosamente");
  }
  @PutMapping(value="/{clientId}", consumes="multipart/form-data") @PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER','RECEPCIONISTA','SECRETARIA')")
  public ClientManagementFacade.ClientDetail update(@PathVariable UUID clientId, @Valid @RequestPart("data") ClientRegistrationRequest data, @RequestPart(value="photo", required=false) MultipartFile photo, Authentication authentication, HttpServletRequest request) { return managementFacade.update(clientId, data, photo, authentication.getName(), request.getRemoteAddr()); }
  @PostMapping("/{clientId}/suspension") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','SECRETARIA')")
  public void suspend(@PathVariable UUID clientId, Authentication authentication, HttpServletRequest request) { managementFacade.suspend(clientId, authentication.getName(), request.getRemoteAddr()); }
}
