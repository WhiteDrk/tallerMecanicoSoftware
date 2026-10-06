ALTER TABLE address_states MODIFY sepomex_code VARCHAR(2) NOT NULL;
ALTER TABLE address_municipalities MODIFY sepomex_code VARCHAR(3) NOT NULL;
ALTER TABLE address_postal_codes MODIFY code VARCHAR(5) NOT NULL;
ALTER TABLE address_localities MODIFY sepomex_code VARCHAR(4) NOT NULL;
