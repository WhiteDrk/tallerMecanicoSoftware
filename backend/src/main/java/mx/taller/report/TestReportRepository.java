package mx.taller.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestReportRepository extends JpaRepository<TestReport, UUID> {
  List<ReportCard> findTop50ByOrderByCreatedAtDesc();
}
