package mx.taller.status;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/status-catalog")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER')")
public class StatusCatalogController {
  private final StatusCatalogFacade facade;
  public StatusCatalogController(StatusCatalogFacade facade) { this.facade = facade; }
  @GetMapping public List<StatusCatalogFacade.StatusResult> list() { return facade.list(); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public StatusCatalogFacade.StatusResult create(@Valid @RequestBody StatusRequest request, Authentication authentication, HttpServletRequest servletRequest) { return facade.create(new StatusCatalogFacade.StatusRequest(request.value(), request.description(), true), authentication.getName(), servletRequest.getRemoteAddr()); }
  @PutMapping("/{statusId}")
  public StatusCatalogFacade.StatusResult update(@PathVariable Long statusId, @Valid @RequestBody StatusUpdateRequest request, Authentication authentication, HttpServletRequest servletRequest) { return facade.update(statusId, new StatusCatalogFacade.StatusRequest(null, request.description(), request.enabled()), authentication.getName(), servletRequest.getRemoteAddr()); }
  public record StatusRequest(@NotBlank @Size(max = 40) String value, @NotBlank @Size(max = 255) String description) { }
  public record StatusUpdateRequest(@NotBlank @Size(max = 255) String description, boolean enabled) { }
}
