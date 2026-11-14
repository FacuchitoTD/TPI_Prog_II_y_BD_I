-- =====================================================================
-- 05_explain.sql - Etapa 3: mediciones con y sin índice
-- Igualdad, rango y JOIN. Ejecutar cada sentencia 3 veces y tomar la
-- mediana del "actual time" del nodo raíz (ms). Sin índice: IGNORE INDEX.
-- Con índice: FORCE INDEX. Requiere 04_indices.sql. Solo lectura.
-- Resultados registrados en el PDF (sección de evidencias, Etapa 3).
-- =====================================================================
USE FoodStore;

-- Igualdad
EXPLAIN ANALYZE SELECT * FROM producto p IGNORE INDEX (categoria_id)
WHERE p.categoria_id = 2 AND p.eliminado = false;
EXPLAIN ANALYZE SELECT * FROM producto p FORCE INDEX (categoria_id)
WHERE p.categoria_id = 2 AND p.eliminado = false;

-- Rango
EXPLAIN ANALYZE SELECT p.id, p.nombre, p.precio FROM producto p IGNORE INDEX (idx_producto_precio)
WHERE p.precio BETWEEN 1000 AND 5000 AND p.eliminado = false;
EXPLAIN ANALYZE SELECT p.id, p.nombre, p.precio FROM producto p FORCE INDEX (idx_producto_precio)
WHERE p.precio BETWEEN 1000 AND 5000 AND p.eliminado = false;

-- JOIN
EXPLAIN ANALYZE
SELECT c.nombre AS categoria, COUNT(p.id) AS total_productos, ROUND(AVG(p.precio), 2) AS precio_promedio
FROM categoria c
LEFT JOIN producto p IGNORE INDEX (categoria_id) ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre;

EXPLAIN ANALYZE
SELECT c.nombre AS categoria, COUNT(p.id) AS total_productos, ROUND(AVG(p.precio), 2) AS precio_promedio
FROM categoria c
LEFT JOIN producto p FORCE INDEX (categoria_id) ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre;
