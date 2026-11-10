package mx.taller.report;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import mx.taller.identity.AppUser;
import org.hibernate.annotations.UuidGenerator;

@Entity @Table(name = "test_reports")
public class TestReport {
  @Id @UuidGenerator @Column(columnDefinition = "BINARY(16)") private UUID id;
  @Column(nullable=false, length=140) private String title;
  @Column(nullable=false, length=20) private String tool;
  @Column(nullable=false, length=2000) private String notes;
  @Column(nullable=false, length=20) private String outcome;
  private Integer total;
  private Integer passed;
  private Integer failed;
  @Column(name="executed_at") private Instant executedAt;
  @Column(name="created_at", nullable=false) private Instant createdAt;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="uploaded_by") private AppUser uploadedBy;
  @Lob @Column(name="summary_json", nullable=false, columnDefinition="MEDIUMTEXT") private String summaryJson;
  @Lob @Column(nullable=false, columnDefinition="MEDIUMBLOB") private byte[] artifact;
  @Column(name="artifact_size", nullable=false) private int artifactSize;
  @Column(nullable=false, length=64) private String sha256;
  protected TestReport() { }
  public TestReport(String title, String tool, String notes, ReportArchive.Parsed parsed, byte[] artifact, String sha256, AppUser actor) {
    this.title=title; this.tool=tool; this.notes=notes; this.outcome=parsed.outcome();
    this.total=parsed.total(); this.passed=parsed.passed(); this.failed=parsed.failed();
    this.executedAt=parsed.executedAt(); this.summaryJson=parsed.summary().toString();
    this.artifact=artifact; this.artifactSize=artifact.length; this.sha256=sha256;
    this.uploadedBy=actor; this.createdAt=Instant.now();
  }
  public UUID getId(){return id;} public String getTitle(){return title;} public String getTool(){return tool;}
  public String getNotes(){return notes;} public String getOutcome(){return outcome;}
  public Integer getTotal(){return total;} public Integer getPassed(){return passed;} public Integer getFailed(){return failed;}
  public Instant getExecutedAt(){return executedAt;} public Instant getCreatedAt(){return createdAt;}
  public String getSummaryJson(){return summaryJson;} public byte[] getArtifact(){return artifact;}
  public int getArtifactSize(){return artifactSize;} public String getSha256(){return sha256;}
}
