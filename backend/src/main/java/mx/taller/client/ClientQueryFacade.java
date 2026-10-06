package mx.taller.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.taller.workshop.Workshop;
import mx.taller.workshop.WorkshopAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientQueryFacade {
  private static final Set<String> SORTABLE = Set.of("fullName", "email", "status");
  private final ClientRepository clients;
  private final WorkshopAccessService workshopAccess;
  public ClientQueryFacade(ClientRepository clients, WorkshopAccessService workshopAccess) { this.clients = clients; this.workshopAccess = workshopAccess; }
  @Transactional(readOnly = true) public ClientPage list(String actorId, UUID workshopId, int page, int size, String sort, String direction) {
    Workshop workshop = workshopAccess.requireAccess(actorId, workshopId);
    String safeSort = SORTABLE.contains(sort) ? sort : "fullName";
    Sort.Direction safeDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    Page<Client> result = clients.findByWorkshopId(workshop.getId(), PageRequest.of(page, Math.min(size, 10), Sort.by(safeDirection, safeSort)));
    List<ClientSummary> content = result.getContent().stream().map(client -> new ClientSummary(client.getId(), client.getFullName(), client.getEmail(), client.getStatus().name())).toList();
    return new ClientPage(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
  }
  public record ClientSummary(UUID id, String fullName, String email, String status) { }
  public record ClientPage(List<ClientSummary> content, int page, int size, long totalElements, int totalPages) { }
}
