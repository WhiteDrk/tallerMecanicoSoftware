package mx.taller.workshop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/workshops")
public class WorkshopController {
  private final WorkshopFacade facade;
  private final WorkshopAccessService access;
  public WorkshopController(WorkshopFacade facade, WorkshopAccessService access) { this.facade = facade; this.access = access; }
  @GetMapping("/available") @PreAuthorize("hasAnyRole('OWNER','MANAGER','CUSTOMER_SERVICE')")
  public List<WorkshopFacade.WorkshopOption> available(Authentication authentication) { return facade.accessible(authentication.getName(), access); }
  @GetMapping @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
  public List<WorkshopFacade.WorkshopDetail> list(Authentication authentication) { return facade.list(authentication.getName()); }
  @GetMapping("/{workshopId}") @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
  public WorkshopFacade.WorkshopDetail get(@PathVariable java.util.UUID workshopId, Authentication authentication) { return facade.get(workshopId, authentication.getName()); }
  @GetMapping("/managers") @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
  public List<WorkshopFacade.ManagerOption> managers(Authentication authentication) { return facade.managers(authentication.getName()); }
  @PostMapping(consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
  public Map<String, Object> register(@Valid @RequestPart("data") WorkshopRegistrationRequest data, @RequestPart(value = "photo", required = false) MultipartFile photo, Authentication authentication, HttpServletRequest request) {
    WorkshopFacade.WorkshopResult result = facade.register(data, photo, authentication.getName(), request.getRemoteAddr());
    return Map.of("id", result.id(), "name", result.name(), "message", "Taller registrado exitosamente");
  }
  @PutMapping(value = "/{workshopId}", consumes = "multipart/form-data") @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
  public Map<String, Object> update(@PathVariable java.util.UUID workshopId, @Valid @RequestPart("data") WorkshopRegistrationRequest data, @RequestPart(value = "photo", required = false) MultipartFile photo, Authentication authentication, HttpServletRequest request) {
    WorkshopFacade.WorkshopResult result = facade.update(workshopId, data, photo, authentication.getName(), request.getRemoteAddr());
    return Map.of("id", result.id(), "name", result.name(), "message", "Taller actualizado exitosamente");
  }
  @DeleteMapping("/{workshopId}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('OWNER')")
  public void deactivate(@PathVariable java.util.UUID workshopId, Authentication authentication, HttpServletRequest request) { facade.deactivate(workshopId, authentication.getName(), request.getRemoteAddr()); }
}
