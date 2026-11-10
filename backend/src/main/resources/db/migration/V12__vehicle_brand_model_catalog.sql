CREATE TABLE vehicle_brands (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_vehicle_brands_name (name)
);

CREATE TABLE vehicle_models (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  brand_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_vehicle_models_brand FOREIGN KEY (brand_id) REFERENCES vehicle_brands(id),
  UNIQUE KEY uq_vehicle_models_brand_name (brand_id, name)
);

INSERT INTO vehicle_brands(name) VALUES ('KIA'), ('MAZDA'), ('NISSAN'), ('TOYOTA');
INSERT INTO vehicle_models(brand_id, name) SELECT id, 'RIO' FROM vehicle_brands WHERE name = 'KIA';
INSERT INTO vehicle_models(brand_id, name) SELECT id, 'CX-5' FROM vehicle_brands WHERE name = 'MAZDA';
INSERT INTO vehicle_models(brand_id, name) SELECT id, 'VERSA' FROM vehicle_brands WHERE name = 'NISSAN';
INSERT INTO vehicle_models(brand_id, name) SELECT id, 'COROLLA' FROM vehicle_brands WHERE name = 'TOYOTA';
