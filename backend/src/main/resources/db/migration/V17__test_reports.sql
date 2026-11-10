CREATE TABLE test_reports (
  id BINARY(16) PRIMARY KEY,
  title VARCHAR(140) NOT NULL,
  tool VARCHAR(20) NOT NULL,
  notes VARCHAR(2000) NOT NULL,
  outcome VARCHAR(20) NOT NULL,
  total INT NULL,
  passed INT NULL,
  failed INT NULL,
  executed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  uploaded_by BINARY(16) NOT NULL,
  summary_json MEDIUMTEXT NOT NULL,
  artifact MEDIUMBLOB NOT NULL,
  artifact_size INT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  CONSTRAINT fk_test_reports_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
  INDEX idx_test_reports_created (created_at)
);
