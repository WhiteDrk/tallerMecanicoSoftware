#!/usr/bin/env bash
set -Eeuo pipefail

archive_path=${1:?"Uso: scripts/import-sepomex-catalog.sh /ruta/CPdescargatxt.zip"}
workspace=$(mktemp -d)
cleanup() { rm -rf "$workspace"; }
trap cleanup EXIT

# Se puede indicar un contexto Docker, por ejemplo DOCKER_CONTEXT=default.
docker_args=()
if [ -n "${DOCKER_CONTEXT:-}" ]; then
  docker_args=(--context "$DOCKER_CONTEXT")
fi

unzip -p "$archive_path" CPdescarga.txt > "$workspace/CPdescarga.txt"
docker "${docker_args[@]}" compose cp "$workspace/CPdescarga.txt" mysql:/var/lib/mysql-files/CPdescarga.txt

docker "${docker_args[@]}" compose exec -T mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" taller_db' <<'SQL'
CREATE TABLE IF NOT EXISTS sepomex_import_stage (
  postal_code CHAR(5), locality_name VARCHAR(120), settlement_type VARCHAR(80), municipality_name VARCHAR(120), state_name VARCHAR(120), city VARCHAR(120),
  postal_office_code VARCHAR(10), state_code CHAR(2), office_code VARCHAR(10), administrative_postal_code VARCHAR(10), settlement_type_code VARCHAR(10), municipality_code CHAR(3), locality_code CHAR(4), zone VARCHAR(30), city_code VARCHAR(10)
);
TRUNCATE TABLE sepomex_import_stage;
LOAD DATA INFILE '/var/lib/mysql-files/CPdescarga.txt'
INTO TABLE sepomex_import_stage
CHARACTER SET latin1
FIELDS TERMINATED BY '|'
LINES TERMINATED BY '\n'
IGNORE 2 LINES;

DELETE FROM address_localities;
DELETE FROM address_postal_codes;
DELETE FROM address_municipalities;
DELETE FROM address_states;

INSERT INTO address_states (sepomex_code, name)
SELECT state_code, TRIM(state_name) FROM sepomex_import_stage
WHERE state_code IS NOT NULL AND state_code <> ''
GROUP BY state_code, TRIM(state_name);

INSERT INTO address_municipalities (state_id, sepomex_code, name)
SELECT state.id, stage.municipality_code, TRIM(stage.municipality_name)
FROM sepomex_import_stage stage
JOIN address_states state ON state.sepomex_code = stage.state_code
WHERE stage.municipality_code IS NOT NULL AND stage.municipality_code <> ''
GROUP BY state.id, stage.municipality_code, TRIM(stage.municipality_name);

INSERT INTO address_postal_codes (code, state_id, municipality_id, city)
SELECT stage.postal_code, MIN(state.id), MIN(municipality.id), NULLIF(MAX(TRIM(stage.city)), '')
FROM sepomex_import_stage stage
JOIN address_states state ON state.sepomex_code = stage.state_code
JOIN address_municipalities municipality ON municipality.state_id = state.id AND municipality.sepomex_code = stage.municipality_code
WHERE stage.postal_code REGEXP '^[0-9]{5}$'
GROUP BY stage.postal_code;

INSERT INTO address_localities (postal_code_id, sepomex_code, name, settlement_type)
SELECT postal.id, stage.locality_code, TRIM(stage.locality_name), NULLIF(TRIM(stage.settlement_type), '')
FROM sepomex_import_stage stage
JOIN address_postal_codes postal ON postal.code = stage.postal_code
WHERE stage.locality_code IS NOT NULL AND stage.locality_code <> '';
SQL

echo 'Catálogo SEPOMEX importado correctamente.'
