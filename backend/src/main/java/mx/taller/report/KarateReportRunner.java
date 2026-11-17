package mx.taller.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.karatelabs.common.Resource;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.Runner;
import io.karatelabs.core.ScenarioRunEvent;
import io.karatelabs.gherkin.Feature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KarateReportRunner {
  private static final String FEATURE_NAME = "verificaciones.feature";
  static final List<Check> CHECKS = List.of(
      new Check("disponibilidad", "Disponibilidad de la API", "La API responde con estado UP.", 200, false, false, "GET /actuator/health", false),
      new Check("reportes-privados", "Listado de reportes sin sesión", "El listado requiere iniciar sesión.", 401, false, false, "GET /api/v1/test-reports", false),
      new Check("descarga-privada", "Descarga privada de un reporte", "El ZIP de un reporte existente requiere iniciar sesión.", 401, false, true, "GET /api/v1/test-reports/{id}/archive", false),
      new Check("catalogo", "Catálogo de estados", "El catálogo contiene al menos un estado.", 200, false, false, "GET /api/v1/address-catalog/states", true),
      new Check("vehiculo", "Consulta de vehículo", "El VIN devuelto coincide con el VIN de prueba.", 200, true, false, "GET /api/v1/vehicles?criterion=VIN&value={VIN}", true));

  private final TestReportService reports;
  private final ObjectMapper json;
  private final ServletWebServerApplicationContext server;
  private final String featureSource;
  private final KarateFeaturePlan designer;
  private final Semaphore running = new Semaphore(1);

  public KarateReportRunner(TestReportService reports, ObjectMapper json, ServletWebServerApplicationContext server)
      throws IOException {
    this.reports = reports;
    this.json = json;
    this.server = server;
    this.featureSource = new ClassPathResource("motordesk-checks/" + FEATURE_NAME).getContentAsString(StandardCharsets.UTF_8);
    this.designer = new KarateFeaturePlan(Feature.read(Resource.text(featureSource, Path.of(FEATURE_NAME))));
  }

  public TestReportService.Detail run(RunRequest spec, String session, String actorId, String ip) {
    List<Check> checks = selectChecks(spec);
    String vin = spec.vin() == null ? "" : spec.vin().strip().toUpperCase(Locale.ROOT);
    if (checks.stream().anyMatch(Check::requiresVin)) requireVin(vin);
    acquire();
    try {
      UUID reportId = null;
      if (checks.stream().anyMatch(Check::requiresReport)) {
        reportId = spec.reportId();
        if (reportId == null) {
          throw new ReportException("Selecciona el reporte cuya descarga quieres verificar");
        }
        reports.requireExisting(reportId);
      }
      Instant startedAt = Instant.now();
      String reportParameter = reportId == null ? "" : reportId.toString();
      Map<String, Observation> results = execute(featureSource, checks, builder -> builder
          .global("portalApi", baseUrl())
          .global("limiteMs", spec.maxMs())
          .global("vinPrueba", vin)
          .global("reportePrueba", reportParameter)
          .global("sesion", session == null ? "" : session)
          .global("minimoEstados", 1)
          .global("estadoVehiculo", 200));
      ReportArchive.Parsed report = summarize(checks, results, spec.maxMs(), startedAt);
      if (reportId != null) {
        report.summary().put("reporte_verificado", reportId.toString());
      }
      String title = checks.size() == 1 ? "Karate · " + checks.getFirst().name() : "Karate · Verificaciones de MotorDesk";
      return reports.saveRun(title, report, artifact(report.summary(), featureSource), actorId, ip);
    } finally {
      running.release();
    }
  }

  public String preview(KarateFeaturePlan.Request spec) {
    return designer.build(spec).source();
  }

  public TestReportService.Detail runFeature(KarateFeaturePlan.Request spec, String session, String actorId, String ip) {
    var plan = designer.build(spec);
    acquire();
    try {
      plan.cases().stream().map(KarateFeaturePlan.ScenarioCase::reportId).filter(java.util.Objects::nonNull)
          .distinct().forEach(reports::requireExisting);
      Instant startedAt = Instant.now();
      List<Check> checks = plan.cases().stream().map(KarateFeaturePlan.ScenarioCase::check).toList();
      Map<String, Observation> results = execute(plan.source(), checks, builder -> plan.configure(builder, baseUrl(), session));
      ReportArchive.Parsed report = summarize(checks, results, plan.maxMs(), startedAt);
      var rows = report.summary().withArray("verificaciones");
      for (int index = 0; index < plan.cases().size(); index++) {
        UUID reportId = plan.cases().get(index).reportId();
        if (reportId != null) {
          ((ObjectNode) rows.get(index)).put("reporte_verificado", reportId.toString());
        }
      }
      return reports.saveRun("Karate · " + plan.title(), report, artifact(report.summary(), plan.source()), actorId, ip);
    } finally {
      running.release();
    }
  }

  private void acquire() {
    if (!running.tryAcquire()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Hay una ejecución en curso. Intenta de nuevo en unos segundos.");
    }
  }

  private String baseUrl() {
    return "http://localhost:" + server.getWebServer().getPort();
  }

  static void requireVin(String vin) {
    if (!vin.matches("[A-HJ-NPR-Z0-9]{17}")) {
      throw new ReportException("Indica un VIN de prueba de 17 caracteres sin I, O ni Q");
    }
  }

  static int responseLimit(Integer maxMs) {
    if (maxMs == null || maxMs < 1 || maxMs > 5000) {
      throw new ReportException("El límite debe estar entre 1 y 5000 ms");
    }
    return maxMs;
  }

  private List<Check> selectChecks(RunRequest spec) {
    if (spec.checks() == null || spec.checks().isEmpty() || spec.checks().size() > CHECKS.size()) {
      throw new ReportException("Selecciona al menos una prueba del catálogo");
    }
    responseLimit(spec.maxMs());
    var ids = new HashSet<>(spec.checks());
    List<Check> selected = CHECKS.stream().filter(check -> ids.contains(check.id())).toList();
    if (selected.size() != spec.checks().size()) {
      throw new ReportException("Selecciona pruebas del catálogo sin repetirlas");
    }
    return selected;
  }

  private Map<String, Observation> execute(String source, List<Check> checks, Consumer<Runner.Builder> configure) {
    var observations = new ConcurrentHashMap<String, Observation>();
    var feature = Feature.read(Resource.text(source, Path.of(FEATURE_NAME)));
    String tags = String.join(",", checks.stream().map(check -> "@" + check.id()).toList());
    var runner = Runner.builder()
        .features(feature)
        .tags(tags)
        .consoleLevel("error")
        .captureStepLogs(false)
        .outputConsoleSummary(false)
        .outputHtmlReport(false)
        .outputJsonLines(false)
        .outputJunitXml(false)
        .outputCucumberJson(false)
        .backupOutputDir(false)
        .listener(event -> {
          if (event instanceof ScenarioRunEvent scenario && event.getType() == RunEventType.SCENARIO_EXIT) {
            capture(checks, observations, scenario);
          }
          return true;
        });
    configure.accept(runner);
    runner.parallel(1);
    return observations;
  }

  // Persist only the verdict, status and duration for each case.
  private void capture(List<Check> checks, Map<String, Observation> observations, ScenarioRunEvent scenario) {
    if (scenario.result().isSkipped() || scenario.result().isAborted()) {
      return;
    }
    var runtime = scenario.source();
    for (Check check : checks) {
      if (runtime.getScenario().getTags().stream().anyMatch(tag -> tag.getName().equals(check.id()))) {
        Integer status = runtime.getVariable("responseStatus") instanceof Number value ? value.intValue() : null;
        Long elapsed = runtime.getVariable("responseTime") instanceof Number value ? value.longValue() : null;
        observations.put(check.id(), new Observation(scenario.result().isPassed(), status, elapsed));
        return;
      }
    }
  }

  private ReportArchive.Parsed summarize(List<Check> checks, Map<String, Observation> results, int maxMs, Instant startedAt) {
    ObjectNode summary = json.createObjectNode();
    summary.put("herramienta", "Karate");
    summary.put("origen", "API de esta instancia de MotorDesk");
    summary.put("fecha_local", startedAt.toString());
    summary.put("umbral_ms", maxMs);
    var details = summary.putArray("verificaciones");
    int passed = 0;
    int failed = 0;
    for (Check check : checks) {
      ObjectNode row = details.addObject();
      row.put("id", check.id());
      row.put("nombre", check.name());
      row.put("esperado", check.description());
      row.put("peticion", check.request());
      row.put("con_sesion", check.authenticated());
      row.put("http_esperado", check.expectedStatus());
      Observation observed = results.get(check.id());
      if (observed == null) {
        row.put("resultado", "INCOMPLETO");
        row.put("detalle", "Karate no completó este escenario.");
        continue;
      }
      row.put("http_obtenido", observed.status());
      row.put("tiempo_ms", observed.elapsed());
      if (observed.passed()) {
        passed++;
        row.put("resultado", "APROBADO");
      } else {
        failed++;
        row.put("resultado", "FALLIDO");
        row.put("detalle", failureDetail(check, observed, maxMs));
      }
    }
    int total = passed + failed;
    boolean incomplete = total != checks.size();
    ObjectNode stage = summary.putArray("etapas").addObject();
    stage.put("nombre", "Verificaciones desde Reportes");
    stage.put("escenarios", total);
    stage.put("aprobados", passed);
    stage.put("fallidos", failed);
    stage.put("paralelismo", 1);
    if (incomplete) {
      summary.withArray("etapas").addObject().put("error", "Quedaron casos sin ejecutar.");
    }
    String outcome = incomplete ? "INCOMPLETO" : failed > 0 ? "FALLIDO" : "APROBADO";
    return new ReportArchive.Parsed(summary, outcome, total, passed, failed, startedAt);
  }

  private String failureDetail(Check check, Observation observed, int maxMs) {
    if (observed.status() == null) {
      return "Karate no registró una respuesta HTTP.";
    }
    if (observed.status() != check.expectedStatus()) {
      return "La API respondió con un código HTTP distinto al esperado.";
    }
    if (observed.elapsed() != null && observed.elapsed() > maxMs) {
      return "La respuesta superó el límite de " + maxMs + " ms.";
    }
    return "El contenido devuelto no cumplió la validación del caso.";
  }

  private byte[] artifact(ObjectNode summary, String source) {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry("resultados.json"));
      zip.write(json.writerWithDefaultPrettyPrinter().writeValueAsBytes(summary));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry(FEATURE_NAME));
      zip.write(source.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    } catch (IOException error) {
      throw new IllegalStateException("No se pudo generar el paquete de resultados", error);
    }
    return bytes.toByteArray();
  }

  public record RunRequest(List<String> checks, Integer maxMs, String vin, UUID reportId) {}
  public record Check(String id, String name, String description, int expectedStatus, boolean requiresVin,
                      boolean requiresReport, String request, boolean authenticated) {}
  private record Observation(boolean passed, Integer status, Long elapsed) {}
}
