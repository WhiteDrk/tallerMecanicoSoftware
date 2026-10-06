CREATE TABLE address_states (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sepomex_code CHAR(2) NOT NULL,
  name VARCHAR(120) NOT NULL,
  CONSTRAINT uq_address_states_code UNIQUE (sepomex_code)
);

CREATE TABLE address_municipalities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  state_id BIGINT NOT NULL,
  sepomex_code CHAR(3) NOT NULL,
  name VARCHAR(120) NOT NULL,
  CONSTRAINT uq_address_municipalities_code UNIQUE (state_id, sepomex_code),
  CONSTRAINT fk_address_municipalities_state FOREIGN KEY (state_id) REFERENCES address_states(id),
  INDEX idx_address_municipalities_state_name (state_id, name)
);

CREATE TABLE address_postal_codes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code CHAR(5) NOT NULL,
  state_id BIGINT NOT NULL,
  municipality_id BIGINT NOT NULL,
  city VARCHAR(120) NULL,
  CONSTRAINT uq_address_postal_codes_code UNIQUE (code),
  CONSTRAINT fk_address_postal_codes_state FOREIGN KEY (state_id) REFERENCES address_states(id),
  CONSTRAINT fk_address_postal_codes_municipality FOREIGN KEY (municipality_id) REFERENCES address_municipalities(id),
  INDEX idx_address_postal_codes_municipality (municipality_id)
);

CREATE TABLE address_localities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  postal_code_id BIGINT NOT NULL,
  sepomex_code CHAR(4) NOT NULL,
  name VARCHAR(120) NOT NULL,
  settlement_type VARCHAR(80) NULL,
  CONSTRAINT uq_address_localities_source UNIQUE (postal_code_id, sepomex_code),
  CONSTRAINT fk_address_localities_postal_code FOREIGN KEY (postal_code_id) REFERENCES address_postal_codes(id),
  INDEX idx_address_localities_postal_name (postal_code_id, name)
);
