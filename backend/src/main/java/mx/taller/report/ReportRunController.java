package mx.taller.report;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-reports")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReportRunController {
  private final KarateReportRunner runner;

  public ReportRunController(KarateReportRunner runner) {
    this.runner = runner;
  }

  @GetMapping("/checks")
  public ResponseEntity<List<KarateReportRunner.Check>> checks() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(KarateReportRunner.CHECKS);
  }

  @PostMapping(value = "/runs", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TestReportService.Detail> run(
      @RequestBody KarateReportRunner.RunRequest spec, Authentication actor, HttpServletRequest request) {
    var report = runner.run(spec, request.getHeader(HttpHeaders.AUTHORIZATION), actor.getName(), request.getRemoteAddr());
    return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(report);
  }

  @PostMapping(value = "/features/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FeaturePreview> preview(@RequestBody KarateFeaturePlan.Request spec) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new FeaturePreview(runner.preview(spec)));
  }

  @PostMapping(value = "/features", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TestReportService.Detail> runFeature(
      @RequestBody KarateFeaturePlan.Request spec, Authentication actor, HttpServletRequest request) {
    var report = runner.runFeature(spec, request.getHeader(HttpHeaders.AUTHORIZATION), actor.getName(), request.getRemoteAddr());
    return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(report);
  }

  record FeaturePreview(String source) {}
}
