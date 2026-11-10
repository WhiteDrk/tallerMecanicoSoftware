ALTER TABLE users ADD COLUMN client_id BINARY(16) NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_client UNIQUE (client_id);
ALTER TABLE users ADD CONSTRAINT fk_users_client FOREIGN KEY (client_id) REFERENCES clients(id);
