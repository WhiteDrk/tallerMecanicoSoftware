package mx.taller.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ExecutiveReportFindings {
  static final Reference KARATE = new Reference("Karate Labs - Reportes de ejecución",
      "https://docs.karatelabs.io/running-tests/test-reports/");
  static final Reference BURP = new Reference("PortSwigger - Burp Repeater",
      "https://portswigger.net/burp/documentation/desktop/tools/repeater");
  static final Reference HTTP = new Reference("RFC 9110 - Significado de los códigos HTTP",
      "https://www.rfc-editor.org/rfc/rfc9110.html#name-status-codes");
  static final Reference SEPOMEX = new Reference("Correos de México - Catálogo de códigos postales",
      "https://www.correosdemexico.gob.mx/SSLServicios/ConsultaCP/CodigoPostal_Exportar.aspx");
  static final Reference ACCESS = new Reference("OWASP - Control de acceso en el servidor",
      "https://top10.owasp.org/2025/A01_2025-Broken_Access_Control/");

  private ExecutiveReportFindings() {}

  static List<Finding> from(TestReportService.Detail report) {
    var findings = new ArrayList<Finding>();
    JsonNode checks = report.summary().path("verificaciones");
    int limit = report.summary().path("umbral_ms").asInt(-1);
    if (checks.isArray() && !checks.isEmpty()) {
      for (JsonNode row : checks) {
        switch (row.path("resultado").asText()) {
          case "FALLIDO" -> findings.add(checkFailure(row, limit));
          case "INCOMPLETO" -> findings.add(incomplete(row.path("nombre").asText("Caso sin resultado")));
          default -> { }
        }
      }
      return findings;
    }
    for (JsonNode stage : report.summary().path("etapas")) {
      String label = stage.path("nombre").asText("Etapa de pruebas");
      if (stage.has("error")) {
        findings.add(incomplete(label));
      } else if (stage.path("fallidos").asInt() > 0) {
        String failed = stage.path("fallidos").asText();
        findings.add(new Finding(label, failed + " de " + stage.path("escenarios").asText()
            + " verificaciones incumplieron un criterio.",
            "Esta etapa requiere revisión antes de dar por aprobada la operación evaluada.",
            "Abrir el detalle de Karate en la evidencia técnica, identificar qué condición falló y repetir la etapa.",
            KARATE));
      }
    }
    return findings;
  }

  private static Finding checkFailure(JsonNode row, int limit) {
    String name = row.path("nombre").asText("Caso con fallo");
    int actual = row.path("http_obtenido").asInt(-1);
    int expected = row.path("http_esperado").asInt(-1);
    long elapsed = row.path("tiempo_ms").asLong(-1);
    String request = row.path("peticion").asText();
    if (actual < 0) {
      return new Finding(name, "La prueba terminó sin recibir respuesta.",
          "La operación quedó sin respuesta durante esta comprobación.",
          "Verificar la disponibilidad del servicio y revisar sus registros antes de repetir el caso.", null);
    }
    if ((expected == 401 || expected == 403) && actual >= 200 && actual < 300) {
      return new Finding(name, "El servidor aceptó una solicitud que debía rechazar.",
          "Puede haber acceso a información fuera de los permisos previstos si se devolvieron datos protegidos.",
          "Confirmar qué datos entregó la respuesta y revisar los permisos de esa operación en el servidor.", ACCESS);
    }
    if (actual != expected) {
      return new Finding(name, "El servicio devolvió HTTP " + actual + " cuando se esperaba " + expected + ".",
          "La operación se comportó de forma distinta a la esperada en el caso probado.",
          "Revisar la respuesta, los datos de prueba y los registros del servicio para identificar la causa.", HTTP);
    }
    if (limit > 0 && elapsed > limit) {
      return new Finding(name, "La respuesta tardó " + elapsed + " ms y el objetivo era " + limit + " ms.",
          "La persona podría esperar más de lo previsto para completar esta operación.",
          "Revisar tiempos de la consulta y recursos del servicio. Repetir la prueba bajo las mismas condiciones.", KARATE);
    }
    if (request.equals("GET /api/v1/address-catalog/states") || row.path("id").asText().equals("catalogo")
        || name.toLowerCase(Locale.ROOT).equals("catálogo de estados")) {
      return new Finding(name, "El servicio respondió, pero el catálogo no cumplió el requisito de cantidad o formato.",
          "El formulario de clientes puede mostrar una lista de estados vacía, incompleta o inválida.",
          "Revisar el catálogo SEPOMEX en la base de datos, completar o corregir sus datos y volver a probar el formulario.",
          SEPOMEX);
    }
    return new Finding(name, "El servicio respondió, pero los datos incumplieron la validación del caso.",
        "El usuario puede recibir información incompleta o distinta a la esperada en la operación probada.",
        "Comparar el dato devuelto con el dato esperado en Karate y repetir la prueba tras corregir la causa.", KARATE);
  }

  private static Finding incomplete(String name) {
    return new Finding(name, "La ejecución quedó sin un resultado completo.",
        "Esta comprobación sigue pendiente de verificación.",
        "Revisar el entorno y los registros de la ejecución y repetir el caso.", KARATE);
  }

  record Reference(String title, String url) {}
  record Finding(String name, String observed, String impact, String action, Reference reference) {}
}
