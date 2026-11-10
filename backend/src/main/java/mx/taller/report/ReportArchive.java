package mx.taller.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

/** Reads a bounded archive without extracting or executing any uploaded HTML. */
@Component
public class ReportArchive {
  public static final int MAX_BYTES = 8 * 1024 * 1024;
  private final ObjectMapper json;
  public ReportArchive(ObjectMapper json) { this.json=json; }
  public record Parsed(ObjectNode summary, String outcome, Integer total, Integer passed, Integer failed, Instant executedAt) { }

  public Parsed parse(byte[] bytes, String tool) {
    if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new ReportException("El ZIP debe pesar entre 1 byte y 8 MB");
    byte[] summary=null;
    long expanded=0;
    int entries=0;
    try (ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
      for (ZipEntry entry; (entry=zip.getNextEntry())!=null;) {
        if (++entries>1000) throw new ReportException("El ZIP contiene demasiados archivos");
        String name=entry.getName();
        if (name.startsWith("/") || name.contains("\\") || name.contains(":") || java.util.Arrays.asList(name.split("/")).contains(".."))
          throw new ReportException("El ZIP contiene rutas no permitidas");
        boolean isSummary=!entry.isDirectory() && (name.equals("resultados.json") || name.endsWith("/resultados.json"));
        ByteArrayOutputStream content=isSummary ? new ByteArrayOutputStream() : null;
        byte[] buffer=new byte[8192];
        for (int size; (size=zip.read(buffer))!=-1;) {
          expanded+=size;
          if (expanded>64L*1024*1024) throw new ReportException("El contenido descomprimido supera 64 MB");
          if (isSummary) {
            if (content.size()+size>512*1024) throw new ReportException("El resumen JSON supera 512 KB");
            content.write(buffer,0,size);
          }
        }
        if (isSummary) {
          if (summary!=null) throw new ReportException("El ZIP debe contener un único resultados.json");
          summary=content.toByteArray();
        }
      }
    } catch (IOException error) { throw new ReportException("No se pudo leer el ZIP"); }
    if (entries==0) throw new ReportException("El archivo no es un ZIP válido o está vacío");
    if (summary==null) {
      if (tool.equals("KARATE")) throw new ReportException("El paquete Karate debe incluir resultados.json generado por ejecutar-nube.py");
      return new Parsed(json.createObjectNode(),"DOCUMENTADO",null,null,null,null);
    }
    try {
      JsonNode input=json.readTree(summary);
      if (input==null || !input.isObject() || !input.path("etapas").isArray() || input.path("etapas").isEmpty() || input.path("etapas").size()>20)
        throw new ReportException("El resumen debe contener entre 1 y 20 etapas");
      ObjectNode clean=json.createObjectNode();
      copyText(input,clean,"origen",500); copyText(input,clean,"herramienta",100);
      copyText(input,clean,"fecha_local",100);
      if (input.has("umbral_ms")) clean.put("umbral_ms",number(input,"umbral_ms",1000000));
      ArrayNode stages=clean.putArray("etapas");
      int total=0,passed=0,failed=0;
      boolean incomplete=false;
      for (JsonNode stage:input.path("etapas")) {
        if (!stage.isObject()) throw new ReportException("Una etapa del resumen no es válida");
        ObjectNode out=stages.addObject();
        copyText(stage,out,"nombre",100);
        if (stage.has("error")) { copyText(stage,out,"error",1000); incomplete=true; continue; }
        int count=number(stage,"escenarios",1000000), ok=number(stage,"aprobados",1000000), bad=number(stage,"fallidos",1000000);
        if (ok+bad!=count) throw new ReportException("Los totales de una etapa no coinciden");
        total+=count; passed+=ok; failed+=bad;
        out.put("escenarios",count);out.put("aprobados",ok);out.put("fallidos",bad);
        if (stage.has("estados_http")) {
          JsonNode codes=stage.get("estados_http");
          if (!codes.isObject() || codes.size()>20) throw new ReportException("Los estados HTTP no son válidos");
          ObjectNode cleanCodes=out.putObject("estados_http");
          var names=codes.fieldNames();
          while(names.hasNext()) {
            String code=names.next();
            if(!code.matches("[1-5][0-9]{2}")) throw new ReportException("Un código HTTP no es válido");
            cleanCodes.put(code,number(codes,code,1000000));
          }
        }
        for (String key:new String[]{"paralelismo","casos_configurados","http_p95_ms","http_maximo_ms","duracion_suite_ms"})
          if (stage.has(key) && !stage.get(key).isNull()) out.put(key,number(stage,key,86400000));
        if (stage.has("http_promedio_ms") && !stage.get("http_promedio_ms").isNull()) {
          double mean=stage.get("http_promedio_ms").asDouble(-1);
          if (!Double.isFinite(mean)||mean<0||mean>86400000) throw new ReportException("El tiempo promedio no es válido");
          out.put("http_promedio_ms",mean);
        }
      }
      Instant executed=null;
      if (clean.has("fecha_local")) {
        try { executed=OffsetDateTime.parse(clean.get("fecha_local").asText()).toInstant(); }
        catch (RuntimeException error) { throw new ReportException("La fecha de ejecución no es válida"); }
      }
      return new Parsed(clean,incomplete?"INCOMPLETO":failed>0?"FALLIDO":"APROBADO",total,passed,failed,executed);
    } catch (IOException error) { throw new ReportException("El resumen JSON no es válido"); }
  }

  private static int number(JsonNode node,String key,int max) {
    JsonNode value=node.get(key);
    if (value==null||!value.isIntegralNumber()||!value.canConvertToInt()||value.intValue()<0||value.intValue()>max)
      throw new ReportException("Valor numérico no válido en "+key);
    return value.intValue();
  }
  private static void copyText(JsonNode input,ObjectNode output,String key,int max) {
    if (!input.has(key)) return;
    JsonNode value=input.get(key);
    if (!value.isTextual() || value.asText().length()>max) throw new ReportException("Texto no válido en "+key);
    output.put(key,value.asText());
  }
}
