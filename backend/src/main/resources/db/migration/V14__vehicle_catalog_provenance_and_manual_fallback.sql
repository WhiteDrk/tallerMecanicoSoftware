CREATE TABLE vehicle_catalog_imports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_code VARCHAR(50) NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  source_url VARCHAR(500) NOT NULL,
  catalog_year SMALLINT NOT NULL,
  imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_vehicle_catalog_imports_source_year (source_code, catalog_year)
);

INSERT INTO vehicle_catalog_imports(source_code, source_name, source_url, catalog_year)
VALUES ('CONUEE', 'Catálogo de rendimiento de combustible en vehículos ligeros de venta en México',
        'https://www.gob.mx/cms/uploads/attachment/file/1081919/catalogo_de_rendimientos_29_05_26.pdf', 2026);

ALTER TABLE vehicle_brands
  ADD COLUMN source_import_id BIGINT NULL,
  ADD COLUMN source_origin VARCHAR(30) NOT NULL DEFAULT 'LEGACY_SEED',
  ADD COLUMN pending_review BOOLEAN NOT NULL DEFAULT FALSE,
  ADD CONSTRAINT fk_vehicle_brands_source_import FOREIGN KEY (source_import_id) REFERENCES vehicle_catalog_imports(id);

ALTER TABLE vehicle_models
  ADD COLUMN source_import_id BIGINT NULL,
  ADD COLUMN source_origin VARCHAR(30) NOT NULL DEFAULT 'LEGACY_SEED',
  ADD COLUMN pending_review BOOLEAN NOT NULL DEFAULT FALSE,
  ADD CONSTRAINT fk_vehicle_models_source_import FOREIGN KEY (source_import_id) REFERENCES vehicle_catalog_imports(id);

INSERT INTO vehicle_brands(name, enabled, source_import_id, source_origin, pending_review)
SELECT source.name, TRUE, imports.id, 'CONUEE_2026', FALSE
FROM vehicle_catalog_imports imports
JOIN (
  SELECT 'ACURA' AS name UNION ALL SELECT 'AUDI' UNION ALL SELECT 'AUTECO' UNION ALL SELECT 'AVATR'
  UNION ALL SELECT 'BAIC' UNION ALL SELECT 'BENTLEY' UNION ALL SELECT 'BUICK' UNION ALL SELECT 'BUILD YOUR DREAMS'
  UNION ALL SELECT 'CADILLAC' UNION ALL SELECT 'CHANGAN' UNION ALL SELECT 'CHEVROLET' UNION ALL SELECT 'DEEPAL'
  UNION ALL SELECT 'FORD' UNION ALL SELECT 'GEELY' UNION ALL SELECT 'GMC' UNION ALL SELECT 'HONDA'
  UNION ALL SELECT 'INFINITI' UNION ALL SELECT 'JETOUR' UNION ALL SELECT 'JMC' UNION ALL SELECT 'LINCOLN'
  UNION ALL SELECT 'MITSUBISHI' UNION ALL SELECT 'NISSAN' UNION ALL SELECT 'PORSCHE' UNION ALL SELECT 'SEAT'
  UNION ALL SELECT 'SOUEAST' UNION ALL SELECT 'SUZUKI' UNION ALL SELECT 'VOLKSWAGEN'
  UNION ALL SELECT 'VOLKSWAGEN COMERCIALES' UNION ALL SELECT 'VOLVO' UNION ALL SELECT 'XPENG'
) source
WHERE imports.source_code = 'CONUEE' AND imports.catalog_year = 2026
  AND NOT EXISTS (SELECT 1 FROM vehicle_brands existing_brand WHERE existing_brand.name = source.name);
