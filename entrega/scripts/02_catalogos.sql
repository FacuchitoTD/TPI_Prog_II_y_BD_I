-- =====================================================================
-- 02_catalogos.sql - Etapa 2: tabla semilla / maestra (categorías)
-- Idempotente: vacía las tablas antes de cargar (producto primero por la FK).
-- =====================================================================
USE FoodStore;

DELETE FROM producto;
DELETE FROM categoria;
ALTER TABLE producto  AUTO_INCREMENT = 1;
ALTER TABLE categoria AUTO_INCREMENT = 1;

INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES
('Bebidas',    'Bebidas sin y con alcohol',           false, CURRENT_TIMESTAMP()),
('Almacen',    'Productos de almacén y despensa',     false, CURRENT_TIMESTAMP()),
('Limpieza',   'Artículos de limpieza para el hogar', false, CURRENT_TIMESTAMP()),
('Lácteos',    'Leche, quesos y derivados',           false, CURRENT_TIMESTAMP()),
('Fiambrería', 'Fiambres y embutidos',                false, CURRENT_TIMESTAMP());

SELECT * FROM categoria;
