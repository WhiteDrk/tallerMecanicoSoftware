package mx.taller.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

final class ExecutiveReportPdf {
  private static final Color INK = new Color(21, 39, 54);
  private static final Color MUTED = new Color(70, 88, 102);
  private static final Color TEAL = new Color(15, 110, 100);
  private static final Color RED = new Color(157, 32, 58);
  private static final Color LINE = new Color(219, 229, 234);
  private static final Color PALE = new Color(241, 247, 248);
  private static final Color ROSE = new Color(253, 240, 243);
  private enum Weight { REGULAR, BOLD }
  private static final Weight REGULAR = Weight.REGULAR;
  private static final Weight BOLD = Weight.BOLD;
  private static final float LEFT = 42;
  private static final float WIDTH = PDRectangle.A4.getWidth() - LEFT * 2;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm",
      Locale.forLanguageTag("es-MX")).withZone(ZoneId.of("America/Mexico_City"));

  private ExecutiveReportPdf() {}

  static byte[] render(TestReportService.Detail report) {
    String tool = "BURP".equals(report.tool()) ? "Burp Community" : "Karate";
    List<ExecutiveReportFindings.Finding> findings = ExecutiveReportFindings.from(report);
    Set<ExecutiveReportFindings.Reference> references = new LinkedHashSet<>();
    references.add("BURP".equals(report.tool()) ? ExecutiveReportFindings.BURP : ExecutiveReportFindings.KARATE);
    try (var document = new PDDocument(); var layout = new Layout(document, report)) {
      document.getDocumentInformation().setTitle(clean("MotorDesk - " + report.title()));
      document.getDocumentInformation().setSubject("Informe ejecutivo de pruebas");
      layout.newPage();
      layout.paragraph("Informe ejecutivo de pruebas", BOLD, 25, INK);
      layout.paragraph(report.title(), BOLD, 15, INK);
      layout.paragraph(tool + "  |  " + DATE.format(report.executedAt() == null ? report.createdAt() : report.executedAt())
          + " (hora de la Ciudad de México)", REGULAR, 10, MUTED);
      String scope = report.summary().path("origen").asText();
      if (!scope.isBlank()) layout.paragraph("Entorno evaluado: " + scope, REGULAR, 10, MUTED);
      layout.gap(12);
      layout.section("Resumen para la dirección");
      layout.paragraph(verdict(report.outcome()), BOLD, 14, "FALLIDO".equals(report.outcome()) ? RED : TEAL);
      layout.paragraph(decision(report), REGULAR, 11, INK);
      if (report.total() != null) layout.distribution(report.total(), report.passed(), report.failed());

      if (!findings.isEmpty()) {
        layout.section("Qué se encontró y qué hacer");
        for (var finding : findings) {
          layout.finding(finding);
          if (finding.reference() != null) references.add(finding.reference());
        }
      }
      if (report.notes() != null && !report.notes().isBlank()) {
        layout.section("Observaciones del equipo");
        layout.paragraph(report.notes(), REGULAR, 11, INK);
      }

      JsonNode checks = report.summary().path("verificaciones");
      JsonNode stages = report.summary().path("etapas");
      if (checks.isArray() && !checks.isEmpty()) {
        if (checks.size() > 3) layout.ensure(116 + checks.size() * 36);
        layout.section("Resultados por caso");
        layout.paragraph("El código HTTP describe la respuesta del servidor. Cada caso también comprueba su contenido y su tiempo.",
            REGULAR, 10, MUTED);
        for (JsonNode check : checks) {
          layout.row(check.path("nombre").asText("Caso"),
              "HTTP esperado " + number(check, "http_esperado") + "  |  HTTP recibido " + number(check, "http_obtenido")
                  + "  |  " + time(check, "tiempo_ms"),
              verdict(check.path("resultado").asText()));
        }
        references.add(ExecutiveReportFindings.HTTP);
      } else if (stages.isArray() && !stages.isEmpty()) {
        layout.stages(stages);
      }
      if (report.summary().hasNonNull("umbral_ms")) {
        layout.paragraph("Límite elegido para esta prueba: " + time(report.summary(), "umbral_ms") + " por respuesta.",
            REGULAR, 10, MUTED);
      }
      layout.section("Referencias y evidencia");
      layout.paragraph("El reporte en MotorDesk conserva el paquete técnico y los archivos originales de esta ejecución.",
          REGULAR, 10, MUTED);
      for (var reference : references) layout.reference(reference);
      return layout.finish();
    } catch (IOException error) {
      throw new IllegalStateException("No fue posible generar el informe PDF", error);
    }
  }

  private static String verdict(String outcome) {
    return switch (outcome) {
      case "APROBADO" -> "Aprobado";
      case "FALLIDO" -> "Requiere revisión";
      case "INCOMPLETO" -> "Verificación pendiente";
      case "DOCUMENTADO" -> "Evidencia manual";
      default -> "Resultado por revisar";
    };
  }

  private static String decision(TestReportService.Detail report) {
    return switch (report.outcome()) {
      case "APROBADO" -> "Los casos ejecutados cumplieron los criterios de esta prueba.";
      case "FALLIDO" -> Integer.valueOf(1).equals(report.failed())
          ? "Un caso requiere revisión. El hallazgo explica el impacto y el siguiente paso."
          : "Hay casos que requieren revisión. Los hallazgos explican sus impactos y las acciones sugeridas.";
      case "INCOMPLETO" -> "Parte de la ejecución quedó sin verificar. Revisa los casos pendientes y repite la prueba.";
      case "DOCUMENTADO" -> "El equipo adjuntó evidencia manual. Consulta sus observaciones y las capturas de la prueba.";
      default -> "Consulta el detalle disponible de la ejecución.";
    };
  }

  private static String number(JsonNode row, String key) {
    JsonNode value = row.path(key);
    return value.isIntegralNumber() ? value.asText() : "Sin dato";
  }

  private static String counted(JsonNode row, String key, String singular, String plural) {
    return number(row, key) + " " + (row.path(key).asInt(-1) == 1 ? singular : plural);
  }

  private static String time(JsonNode row, String key) {
    JsonNode value = row.path(key);
    return value.isIntegralNumber() ? value.asText() + " ms" : "Sin dato";
  }

  private static String clean(String value) {
    String str = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
        .replace('–', '-').replace('—', '-').replace('−', '-').replace('’', '\'').replace('“', '"').replace('”', '"')
        .replaceAll("(?i)\\bBearer[ \\t]+[A-Za-z0-9._~+/=-]{8,}", "Bearer [oculto]")
        .replaceAll("eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", "[token oculto]");
    var safe = new StringBuilder();
    str.codePoints().forEach(cp -> {
      if (Character.isWhitespace(cp) || cp == 160) safe.append(' ');
      else if ((cp >= 32 && cp <= 126) || (cp >= 161 && cp <= 255 && cp != 173)) safe.appendCodePoint(cp);
      else if (!Character.isISOControl(cp)) safe.append('?');
    });
    return safe.toString().replaceAll(" +", " ").trim();
  }

  private static final class Layout implements AutoCloseable {
    private final PDDocument doc;
    private final TestReportService.Detail report;
    private final PDFont font;
    private PDPage page;
    private PDPageContentStream stream;
    private float y;

    Layout(PDDocument doc, TestReportService.Detail report) throws IOException {
      this.doc = doc;
      this.report = report;
      try (var fontFile = ExecutiveReportPdf.class.getResourceAsStream(
          "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
        if (fontFile == null) throw new IOException("No se encontró la tipografía del informe");
        font = PDType0Font.load(doc, fontFile);
      }
    }

    void newPage() throws IOException {
      close();
      page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      stream = new PDPageContentStream(doc, page);
      float top = PDRectangle.A4.getHeight();
      block(0, top - 6, PDRectangle.A4.getWidth(), 6, TEAL);
      at(LEFT, top - 32, "MOTORDESK", BOLD, 13, TEAL);
      at(LEFT + 105, top - 32, "CONTROL DE CALIDAD", REGULAR, 9, MUTED);
      block(LEFT, top - 43, WIDTH, 0.7f, LINE);
      y = top - 71;
    }

    void section(String title) throws IOException {
      ensure(54);
      block(LEFT, y + 7, WIDTH, 0.7f, LINE);
      y -= 12;
      paragraph(title, BOLD, 14, INK);
    }

    void paragraph(String input, Weight weight, float size, Color color) throws IOException {
      for (String line : wrap(input, font, size, WIDTH)) {
        ensure(size + 4);
        at(LEFT, y, line, weight, size, color);
        y -= size + 4.5f;
      }
      y -= 3;
    }

    void gap(int distance) {
      y -= distance;
    }

    void distribution(int total, Integer approved, Integer failed) throws IOException {
      if (approved == null || failed == null) return;
      ensure(83);
      float card = (WIDTH - 20) / 3;
      metric(LEFT, card, total == 1 ? "Caso ejecutado" : "Casos ejecutados", total, INK, PALE);
      metric(LEFT + card + 10, card, approved == 1 ? "Aprobado" : "Aprobados", approved, TEAL, PALE);
      metric(LEFT + 2 * (card + 10), card, failed == 1 ? "Fallido" : "Fallidos", failed, RED, ROSE);
      y -= 55;
      if (total > 0) {
        float green = (float) ((double) approved / total * WIDTH);
        block(LEFT, y - 3, WIDTH, 7, RED);
        if (green > 0) block(LEFT, y - 3, Math.min(green, WIDTH), 7, TEAL);
      }
      y -= 25;
    }

    private void metric(float x, float width, String label, int value, Color color, Color surface) throws IOException {
      block(x, y - 50, width, 52, surface);
      at(x + 12, y - 22, Integer.toString(value), BOLD, 22, color);
      at(x + 12, y - 39, label, REGULAR, 10, MUTED);
    }

    void finding(ExecutiveReportFindings.Finding finding) throws IOException {
      var labels = List.of(finding.name(), "Observado: " + finding.observed(),
          "Impacto: " + finding.impact(), "Acción sugerida: " + finding.action());
      var lines = new ArrayList<List<String>>();
      for (int i = 0; i < labels.size(); i++) {
        lines.add(wrap(labels.get(i), font, i == 0 ? 12 : 10.5f, WIDTH - 27));
      }
      float height = 18 + lines.stream().mapToInt(List::size).sum() * 14 + 8;
      ensure(height + 11);
      block(LEFT, y - height + 7, WIDTH, height, ROSE);
      block(LEFT, y - height + 7, 3, height, RED);
      y -= 12;
      for (int i = 0; i < lines.size(); i++) {
        for (String line : lines.get(i)) {
          at(LEFT + 14, y, line, i == 0 ? BOLD : REGULAR, i == 0 ? 12 : 10.5f, i == 0 ? RED : INK);
          y -= 14;
        }
        y -= 2;
      }
      y -= 12;
    }

    void row(String title, String facts, String result) throws IOException {
      List<String> names = wrap(title, font, 11, WIDTH - 128);
      List<String> details = wrap(facts, font, 10, WIDTH - 14);
      ensure(13 * (names.size() + details.size()) + 11);
      if (!result.isBlank()) {
        boolean approved = "Aprobado".equals(result);
        at(LEFT + WIDTH - width(font, 9, result), y, result, BOLD, 9, approved ? TEAL : RED);
      }
      for (String name : names) {
        at(LEFT, y, name, BOLD, 11, INK);
        y -= 13;
      }
      for (String detail : details) {
        at(LEFT, y, detail, REGULAR, 10, MUTED);
        y -= 13;
      }
      block(LEFT, y + 2, WIDTH, 0.6f, LINE);
      y -= 10;
    }

    void stages(JsonNode stages) throws IOException {
      long maximum = 0;
      boolean measured = false;
      for (JsonNode stage : stages) {
        if (!stage.path("http_p95_ms").isIntegralNumber()) continue;
        measured = true;
        maximum = Math.max(maximum, stage.path("http_p95_ms").asLong());
      }
      section("Resultados por etapa");
      if (measured) {
        paragraph("La barra muestra el percentil 95 (p95). En cada etapa, cerca del 95 % de las consultas respondió en ese tiempo o menos.",
            REGULAR, 10, MUTED);
      }
      for (JsonNode stage : stages) {
        String name = stage.path("nombre").asText("Etapa");
        if (stage.has("error")) {
          row(name, "La etapa quedó pendiente de verificación.", "Incompleta");
          continue;
        }
        String facts = counted(stage, "escenarios", "caso", "casos") + "  |  "
            + counted(stage, "fallidos", "fallido", "fallidos")
            + "  |  Máximo " + time(stage, "http_maximo_ms");
        if (!stage.path("http_p95_ms").isIntegralNumber()) {
          row(name, facts, "");
          continue;
        }
        long p95 = stage.path("http_p95_ms").asLong();
        List<String> names = wrap(name, font, 11, WIDTH - 100);
        ensure(13 * names.size() + 34);
        String label = "p95 " + p95 + " ms";
        at(LEFT + WIDTH - width(font, 10, label), y, label, BOLD, 10, TEAL);
        for (String line : names) {
          at(LEFT, y, line, BOLD, 11, INK);
          y -= 13;
        }
        at(LEFT, y, facts, REGULAR, 10, MUTED);
        y -= 11;
        block(LEFT, y, WIDTH, 6, PALE);
        if (p95 > 0) block(LEFT, y, (float) ((double) p95 / maximum * WIDTH), 6, TEAL);
        y -= 19;
      }
    }

    void reference(ExecutiveReportFindings.Reference reference) throws IOException {
      for (String line : wrap(reference.title(), font, 10, WIDTH)) {
        ensure(15);
        at(LEFT, y, line, REGULAR, 10, TEAL);
        var link = new PDAnnotationLink();
        link.setRectangle(new PDRectangle(LEFT, y - 2, width(font, 10, line), 12));
        var border = new PDBorderStyleDictionary();
        border.setWidth(0);
        link.setBorderStyle(border);
        var action = new PDActionURI();
        action.setURI(reference.url());
        link.setAction(action);
        page.getAnnotations().add(link);
        y -= 15;
      }
    }

    private void ensure(float height) throws IOException {
      if (y - height < 53) newPage();
    }

    private void block(float x, float bottom, float width, float height, Color color) throws IOException {
      stream.setNonStrokingColor(color);
      stream.addRect(x, bottom, width, height);
      stream.fill();
    }

    private void at(float x, float baseline, String text, Weight weight, float size, Color color) throws IOException {
      drawText(stream, x, baseline, text, font, weight, size, color);
    }

    byte[] finish() throws IOException {
      close();
      int index = 0;
      for (PDPage item : doc.getPages()) {
        try (var footer = new PDPageContentStream(doc, item, AppendMode.APPEND, true, true)) {
          drawText(footer, LEFT, 29, "MotorDesk  |  " + report.id(), font, REGULAR, 8, MUTED);
          drawText(footer, LEFT + WIDTH - 55, 29, "Página " + (++index) + "/" + doc.getNumberOfPages(),
              font, REGULAR, 8, MUTED);
        }
      }
      var output = new ByteArrayOutputStream();
      doc.save(output);
      return output.toByteArray();
    }

    @Override
    public void close() throws IOException {
      if (stream != null) {
        stream.close();
        stream = null;
      }
    }
  }

  private static void drawText(PDPageContentStream stream, float x, float y, String text, PDFont font,
      Weight weight, float size, Color color) throws IOException {
    stream.saveGraphicsState();
    stream.setNonStrokingColor(color);
    if (weight == BOLD) {
      stream.setStrokingColor(color);
      stream.setLineWidth(Math.min(0.3f, size * 0.018f));
    }
    stream.beginText();
    if (weight == BOLD) stream.setRenderingMode(RenderingMode.FILL_STROKE);
    stream.setFont(font, size);
    stream.newLineAtOffset(x, y);
    stream.showText(clean(text));
    stream.endText();
    stream.restoreGraphicsState();
  }

  private static List<String> wrap(String input, PDFont font, float size, float maxWidth) throws IOException {
    var lines = new ArrayList<String>();
    String pending = "";
    for (String word : clean(input).split(" ")) {
      if (word.isEmpty()) continue;
      String combined = pending.isEmpty() ? word : pending + " " + word;
      if (width(font, size, combined) <= maxWidth) {
        pending = combined;
        continue;
      }
      if (!pending.isEmpty()) lines.add(pending);
      pending = "";
      for (int index = 0; index < word.length(); index++) {
        char character = word.charAt(index);
        if (width(font, size, pending + character) > maxWidth) {
          lines.add(pending);
          pending = "";
        }
        pending += character;
      }
    }
    if (!pending.isEmpty()) lines.add(pending);
    return lines;
  }

  private static float width(PDFont font, float size, String text) throws IOException {
    return font.getStringWidth(clean(text)) * size / 1000;
  }
}
