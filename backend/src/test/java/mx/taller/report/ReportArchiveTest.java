package mx.taller.report;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class ReportArchiveTest {
  ReportArchive reader=new ReportArchive(new ObjectMapper());
  static final String SUMMARY="""
    {"fecha_local":"2026-11-10T09:43:03-06:00","origen":"https://example.invalid","umbral_ms":2000,
     "etapas":[{"nombre":"Carga","escenarios":10,"aprobados":9,"fallidos":1,"paralelismo":5,"http_p95_ms":2300,"estados_http":{"200":9,"500":1}}],"accessToken":"do-not-expose"}
    """;
  static byte[] zip(String name,String content) throws Exception {
    var bytes=new ByteArrayOutputStream();
    try(var zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry(name));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    return bytes.toByteArray();
  }
  @Test void readsGeneratedSummaryAndKeepsOnlySupportedFields() throws Exception {
    var parsed=reader.parse(zip("run/resultados.json",SUMMARY),"KARATE");
    assertEquals(10,parsed.total());assertEquals(9,parsed.passed());assertEquals(1,parsed.failed());
    assertEquals("FALLIDO",parsed.outcome());assertFalse(parsed.summary().has("accessToken"));
    assertEquals(9,parsed.summary().path("etapas").get(0).path("estados_http").path("200").intValue());
    assertEquals("2026-11-10T15:43:03Z",parsed.executedAt().toString());
  }
  @Test void preservesIncompleteExecutionInsteadOfCallingItSuccessful() throws Exception {
    var parsed=reader.parse(zip("resultados.json","{\"etapas\":[{\"nombre\":\"Carga\",\"error\":\"Timeout\"}]}"),"KARATE");
    assertEquals("INCOMPLETO",parsed.outcome());
  }
  @Test void rejectsInconsistentCounts() throws Exception {
    var bytes=zip("resultados.json",SUMMARY.replace("\"aprobados\":9","\"aprobados\":10"));
    assertThrows(ReportException.class,()->reader.parse(bytes,"KARATE"));
  }
  @Test void rejectsTraversalAndNonZipContent() throws Exception {
    var bytes=zip("../resultados.json",SUMMARY);
    assertThrows(ReportException.class,()->reader.parse(bytes,"KARATE"));
    assertThrows(ReportException.class,()->reader.parse("not a ZIP".getBytes(),"BURP"));
  }
  @Test void burpEvidenceWithoutMetricsIsDocumented() throws Exception {
    var parsed=reader.parse(zip("notas.txt","Prueba manual"),"BURP");
    assertEquals("DOCUMENTADO",parsed.outcome());assertNull(parsed.total());assertNull(parsed.passed());
  }
  @Test void karateRequiresItsSummary() throws Exception {
    var bytes=zip("index.html","Report");
    assertThrows(ReportException.class,()->reader.parse(bytes,"KARATE"));
  }
  @Test void rejectsUnboundedSummaryAndNegativeTimes() throws Exception {
    var huge=zip("resultados.json"," ".repeat(512*1024+1));
    var negative=zip("resultados.json",SUMMARY.replace("\"http_p95_ms\":2300","\"http_p95_ms\":-1"));
    assertThrows(ReportException.class,()->reader.parse(huge,"KARATE"));
    assertThrows(ReportException.class,()->reader.parse(negative,"KARATE"));
  }
}
