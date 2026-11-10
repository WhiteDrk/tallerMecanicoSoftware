CREATE TABLE status_catalog (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  str_valor VARCHAR(40) NOT NULL,
  str_descripcion VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  system_base BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uq_status_catalog_valor (str_valor)
);

INSERT INTO status_catalog(str_valor, str_descripcion, enabled, system_base) VALUES
  ('ACTIVO', 'Registro habilitado para operar.', TRUE, TRUE),
  ('SUSPENDIDO', 'Registro suspendido; conserva su información.', TRUE, TRUE),
  ('INACTIVO', 'Registro inactivo sin eliminación física.', TRUE, TRUE),
  ('CANCELADO', 'Registro cancelado sin eliminación física.', TRUE, TRUE);

ALTER TABLE clients ADD COLUMN status_id BIGINT NULL;
UPDATE clients c JOIN status_catalog s ON s.str_valor = CASE c.status WHEN 'ACTIVE' THEN 'ACTIVO' WHEN 'SUSPENDED' THEN 'SUSPENDIDO' ELSE 'ACTIVO' END SET c.status_id = s.id;
ALTER TABLE clients MODIFY status_id BIGINT NOT NULL, ADD CONSTRAINT fk_clients_status FOREIGN KEY (status_id) REFERENCES status_catalog(id), ADD INDEX idx_clients_workshop_status_catalog (workshop_id, status_id);
ALTER TABLE clients DROP INDEX idx_clients_workshop_status;
ALTER TABLE clients DROP COLUMN status;
