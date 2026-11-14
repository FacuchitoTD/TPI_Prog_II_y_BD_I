-- =====================================================================
-- 04_indices.sql - Etapa 3: índices
-- categoria_id: NO se crea un índice adicional. InnoDB crea uno
--   automáticamente para la FK (llamado "categoria_id"), que cumple la
--   función de idx_producto_categoria. Crear otro sería redundante.
-- precio: idx_producto_precio para búsquedas por rango.
-- Idempotente: MySQL 8 no admite CREATE/DROP INDEX IF [NOT] EXISTS, así
--   que se verifica en information_schema y se usa SQL dinámico.
-- =====================================================================
USE FoodStore;

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'producto'
                  AND INDEX_NAME = 'idx_producto_precio');
SET @sql := IF(@existe = 0,
               'CREATE INDEX idx_producto_precio ON producto(precio)',
               'SELECT ''idx_producto_precio ya existe'' AS aviso');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SHOW INDEX FROM producto;
