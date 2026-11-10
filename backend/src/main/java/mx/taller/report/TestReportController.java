package mx.taller.report;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/v1/test-reports")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','MANAGER','AUDITOR')")
public class TestReportController {
  private final TestReportService service;
  public TestReportController(TestReportService service){this.service=service;}
  @GetMapping public ResponseEntity<List<ReportCard>> list(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list());}
  @GetMapping("/{id}") public ResponseEntity<TestReportService.Detail> detail(@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(id));}
  @GetMapping("/{id}/archive") public ResponseEntity<byte[]> archive(@PathVariable UUID id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType("application/zip"))
      .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename("motordesk-reporte-"+id+".zip").build().toString())
      .header("X-Content-Type-Options","nosniff").body(service.artifact(id));
  }
  @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<TestReportService.Detail> upload(@RequestParam String title,@RequestParam String tool,
      @RequestParam(defaultValue="") String notes,@RequestPart MultipartFile file,Authentication auth,HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.upload(title,tool,notes,file,auth.getName(),request.getRemoteAddr()));
  }
}
