ALTER TABLE workshops
  ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN deactivated_at TIMESTAMP NULL,
  ADD INDEX idx_workshops_active_name (active, name);
