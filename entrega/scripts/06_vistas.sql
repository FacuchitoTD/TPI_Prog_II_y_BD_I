-- =====================================================================
-- 06_vistas.sql - Etapas 3 y 4: vistas
-- Idempotente: CREATE OR REPLACE VIEW.
-- =====================================================================
USE FoodStore;

-- Vista útil (Etapa 3): catálogo activo con su categoría, para que la
-- aplicación y los reportes no repitan el JOIN y los filtros de baja lógica.
CREATE OR REPLACE VIEW vista_productos_activos AS
SELECT p.id, p.nombre AS producto, p.descripcion, p.precio, p.stock, p.imagen,
       c.nombre AS categoria, p.created_at
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false;

-- Vista de seguridad 1 (Etapa 4): catálogo público. Oculta datos internos
-- (eliminado, stock, imagen, auditoría created_at).
CREATE OR REPLACE VIEW vista_catalogo_publico AS
SELECT p.id AS codigo_producto, p.nombre AS producto, p.descripcion, p.precio,
       p.disponible, c.nombre AS categoria
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false AND p.disponible = true;

-- Vista de seguridad 2 (Etapa 4): resumen gerencial. Expone solo totales
-- agregados por categoría, ocultando precios unitarios.
CREATE OR REPLACE VIEW vista_resumen_categorias AS
SELECT c.id AS categoria_id, c.nombre AS categoria,
       COUNT(p.id) AS cantidad_productos, SUM(p.stock) AS stock_acumulado
FROM categoria c
LEFT JOIN producto p ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre;

SELECT * FROM vista_productos_activos LIMIT 10;
SELECT * FROM vista_catalogo_publico LIMIT 10;
SELECT * FROM vista_resumen_categorias;
