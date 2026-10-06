CREATE TABLE user_workshops (
  user_id BINARY(16) NOT NULL,
  workshop_id BINARY(16) NOT NULL,
  PRIMARY KEY (user_id, workshop_id),
  CONSTRAINT fk_user_workshops_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_workshops_workshop FOREIGN KEY (workshop_id) REFERENCES workshops(id)
);

INSERT IGNORE INTO user_workshops (user_id, workshop_id)
SELECT manager_user_id, id FROM workshops WHERE active = TRUE;

ALTER TABLE clients
  ADD COLUMN workshop_id BINARY(16) NULL,
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE clients
SET workshop_id = (SELECT id FROM workshops WHERE name = 'Motor Centro' AND active = TRUE LIMIT 1)
WHERE workshop_id IS NULL;

ALTER TABLE clients
  MODIFY workshop_id BINARY(16) NOT NULL,
  ADD CONSTRAINT fk_clients_workshop FOREIGN KEY (workshop_id) REFERENCES workshops(id),
  ADD INDEX idx_clients_workshop_name (workshop_id, full_name),
  ADD INDEX idx_clients_workshop_status (workshop_id, status);
