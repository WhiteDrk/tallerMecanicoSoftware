UPDATE roles SET code = 'ADMINISTRADOR', description = 'Administrador' WHERE code = 'OWNER';
UPDATE roles SET code = 'RECEPCIONISTA', description = 'Recepcionista' WHERE code = 'CUSTOMER_SERVICE';
INSERT INTO roles(code, description) SELECT 'SECRETARIA', 'Secretaria' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SECRETARIA');
