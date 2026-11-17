package mx.taller.report;

import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Step;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class KarateFeaturePlan {
  static final int MAX_CASES = 6;
  private static final String URL_PROPERTY = "motordesk.feature.url";
  private static final String SESSION_PROPERTY = "motordesk.feature.authorization";

  private final List<Step> background;
  private final Map<String, List<Step>> templates;

  KarateFeaturePlan(Feature feature) {
    background = feature.getBackground().getSteps();
    templates = feature.getSections().stream()
        .map(section -> section.getScenario())
        .collect(Collectors.toUnmodifiableMap(scenario -> scenario.getTags().getFirst().getName(),
            scenario -> scenario.getSteps()));
  }

  Plan build(Request request) {
    String title = label(request.title(), 120, "Indica un nombre para el feature");
    int maxMs = KarateReportRunner.responseLimit(request.maxMs());
    if (request.cases() == null || request.cases().isEmpty() || request.cases().size() > MAX_CASES
        || request.cases().stream().anyMatch(item -> item == null)) {
      throw new ReportException("Agrega entre 1 y " + MAX_CASES + " escenarios");
    }

    var source = new StringBuilder("Feature: ").append(title).append("\n\nBackground:\n");
    variable(source, "portalApi", propertyOrEnv(URL_PROPERTY, "MOTORDESK_BASE_URL"));
    variable(source, "sesion", propertyOrEnv(SESSION_PROPERTY, "MOTORDESK_AUTHORIZATION"));
    variable(source, "limiteMs", Integer.toString(maxMs));
    appendSteps(source, background);

    var cases = new ArrayList<ScenarioCase>();
    var properties = new LinkedHashMap<String, String>();
    for (CaseRequest item : request.cases()) {
      int index = cases.size() + 1;
      var template = KarateReportRunner.CHECKS.stream().filter(check -> check.id().equals(item.checkId()))
          .findFirst().orElseThrow(() -> new ReportException("Selecciona una consulta de MotorDesk para el caso " + index));
      String name = label(item.name(), 90, "Indica el nombre del caso " + index);
      String caseId = "caso-" + index;
      String description = template.description();
      int status = template.expectedStatus();
      UUID reportId = null;
      source.append("\n@").append(caseId).append("\nScenario: ").append(name).append('\n');
      if (template.requiresVin()) {
        String vin = item.vin() == null ? "" : item.vin().strip().toUpperCase(Locale.ROOT);
        KarateReportRunner.requireVin(vin);
        status = item.expectedStatus() == null ? 200 : item.expectedStatus();
        if (status != 200 && status != 404) {
          throw new ReportException("Para la consulta de vehículo elige HTTP 200 o 404");
        }
        description = status == 200 ? template.description() : "La API indica que el vehículo no se encontró.";
        input(source, properties, index, "vin", "vinPrueba", "MOTORDESK_VIN_", vin);
        variable(source, "estadoVehiculo", Integer.toString(status));
      }
      if (template.requiresReport()) {
        reportId = item.reportId();
        if (reportId == null) {
          throw new ReportException("Selecciona un reporte existente para el caso " + index);
        }
        input(source, properties, index, "report", "reportePrueba", "MOTORDESK_REPORTE_", reportId.toString());
      }
      if ("catalogo".equals(template.id())) {
        int minimum = item.minStates() == null ? 1 : item.minStates();
        if (minimum < 1 || minimum > 32) {
          throw new ReportException("El mínimo de estados debe estar entre 1 y 32");
        }
        variable(source, "minimoEstados", Integer.toString(minimum));
        description = minimum == 1 ? "El catálogo contiene al menos un estado."
            : "El catálogo contiene al menos " + minimum + " estados.";
      }
      appendSteps(source, templates.get(template.id()));
      var check = new KarateReportRunner.Check(caseId, name, description, status,
          template.requiresVin(), template.requiresReport(), template.request(), template.authenticated());
      cases.add(new ScenarioCase(check, reportId));
    }
    return new Plan(title, maxMs, List.copyOf(cases), source.toString(), Map.copyOf(properties));
  }

  private static String label(String value, int limit, String error) {
    String text = value == null ? "" : value.strip();
    if (text.isEmpty() || text.length() > limit || text.codePoints().anyMatch(codePoint ->
        Character.isISOControl(codePoint) || Character.getType(codePoint) == Character.LINE_SEPARATOR
            || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR)) {
      throw new ReportException(error + " en una línea, de hasta " + limit + " caracteres");
    }
    return text;
  }

  private static void input(StringBuilder source, Map<String, String> properties, int index,
      String key, String variable, String envPrefix, String value) {
    String property = "motordesk.feature." + key + "." + index;
    properties.put(property, value);
    variable(source, variable, propertyOrEnv(property, envPrefix + index));
  }

  private static String propertyOrEnv(String property, String env) {
    return "karate.properties['" + property + "'] || java.lang.System.getenv('" + env + "')";
  }

  private static void variable(StringBuilder source, String name, String expression) {
    source.append("  * def ").append(name).append(" = ").append(expression).append('\n');
  }

  private static void appendSteps(StringBuilder source, List<Step> steps) {
    for (Step step : steps) {
      source.append("  ").append(step.getPrefix()).append(' ').append(step.getKeyword())
          .append(' ').append(step.getText()).append('\n');
    }
  }

  public record Request(String title, Integer maxMs, List<CaseRequest> cases) {}
  public record CaseRequest(String checkId, String name, String vin, UUID reportId, Integer expectedStatus,
                            Integer minStates) {}
  record ScenarioCase(KarateReportRunner.Check check, UUID reportId) {}
  record Plan(String title, int maxMs, List<ScenarioCase> cases, String source, Map<String, String> properties) {
    void configure(io.karatelabs.core.Runner.Builder runner, String baseUrl, String session) {
      runner.systemProperty(URL_PROPERTY, baseUrl).systemProperty(SESSION_PROPERTY, session == null ? "" : session);
      properties.forEach(runner::systemProperty);
    }
  }
}
