package mx.taller.report;

import java.time.Instant;
import java.util.UUID;

/** Closed projection: listing reports must not load ZIP blobs. */
public interface ReportCard {
  UUID getId(); String getTitle(); String getTool(); String getOutcome();
  Integer getTotal(); Integer getPassed(); Integer getFailed();
  Instant getExecutedAt(); Instant getCreatedAt(); int getArtifactSize();
}
