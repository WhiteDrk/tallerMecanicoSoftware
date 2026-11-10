package mx.taller.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import mx.taller.audit.AuditService;
import mx.taller.identity.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TestReportService {
  private final TestReportRepository reports;
  private final AppUserRepository users;
  private final ReportArchive archives;
  private final ObjectMapper json;
  private final AuditService audit;
  public TestReportService(TestReportRepository reports,AppUserRepository users,ReportArchive archives,ObjectMapper json,AuditService audit) {
    this.reports=reports;this.users=users;this.archives=archives;this.json=json;this.audit=audit;
  }
  @Transactional(readOnly=true) public List<ReportCard> list(){return reports.findTop50ByOrderByCreatedAtDesc();}
  @Transactional(readOnly=true) public Detail detail(UUID id) {
    TestReport r=required(id);
    try { return new Detail(r.getId(),r.getTitle(),r.getTool(),r.getNotes(),r.getOutcome(),r.getTotal(),r.getPassed(),r.getFailed(),r.getCreatedAt(),r.getExecutedAt(),r.getArtifactSize(),r.getSha256(),json.readTree(r.getSummaryJson())); }
    catch(IOException error){throw new IllegalStateException("Stored report summary is invalid",error);}
  }
  @Transactional(readOnly=true) public byte[] artifact(UUID id){return required(id).getArtifact();}
  @Transactional public Detail upload(String title,String tool,String notes,MultipartFile file,String actorId,String ip) {
    if(title==null||title.isBlank()||title.length()>140||notes==null||notes.length()>2000)
      throw new ReportException("Indica un título de hasta 140 caracteres y notas de hasta 2000");
    tool=tool==null?"":tool.toUpperCase(Locale.ROOT);
    if(!List.of("KARATE","BURP").contains(tool)) throw new ReportException("Selecciona Karate o Burp");
    if(file==null||file.isEmpty()||file.getSize()>ReportArchive.MAX_BYTES) throw new ReportException("Selecciona un paquete ZIP de hasta 8 MB");
    try {
      byte[] bytes=file.getBytes();
      ReportArchive.Parsed parsed=archives.parse(bytes,tool);
      String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      var actor=users.findById(UUID.fromString(actorId)).filter(user->user.isEnabled()).orElseThrow(()->new ReportException("La cuenta ya no está habilitada"));
      TestReport report=reports.saveAndFlush(new TestReport(title.trim(),tool,notes.trim(),parsed,bytes,hash,actor));
      audit.record(actor,"TEST_REPORT_UPLOADED","TEST_REPORT",report.getId().toString(),ip,"{\"tool\":\""+tool+"\"}");
      return detail(report.getId());
    } catch(IOException error){throw new ReportException("No se pudo leer el archivo");}
      catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
  }
  private TestReport required(UUID id){return reports.findById(id).orElseThrow(ReportNotFoundException::new);}
  public record Detail(UUID id,String title,String tool,String notes,String outcome,Integer total,Integer passed,Integer failed,
    java.time.Instant createdAt,java.time.Instant executedAt,int artifactSize,String sha256,JsonNode summary) { }
}
