-- =====================================================================
-- 05_consultas.sql - Etapa 3: consultas avanzadas y reportes
-- Solo lectura (idempotente).
-- =====================================================================
USE FoodStore;

-- Consulta 1 - JOIN: catálogo legible de productos activos
-- Utilidad: listado con el nombre de la categoría en lugar de su id.
-- Lógica: une producto con categoria por la FK y filtra las bajas lógicas.
SELECT p.id AS producto_id, p.nombre AS producto, p.precio, p.stock, c.nombre AS categoria
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false
LIMIT 10;

-- Consulta 2 - JOIN + GROUP BY + HAVING: estadísticas por categoría
-- Utilidad: reporte de gestión (cantidad, stock total y precio promedio),
--   solo para categorías con más de 1000 productos activos.
-- Lógica: LEFT JOIN con la condición de baja lógica en el ON (no descarta
--   categorías sin productos); HAVING filtra sobre el agregado.
SELECT c.nombre AS categoria,
       COUNT(p.id) AS total_productos,
       SUM(p.stock) AS stock_total,
       ROUND(AVG(p.precio), 2) AS precio_promedio
FROM categoria c
LEFT JOIN producto p ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre
HAVING COUNT(p.id) > 1000;

-- Consulta 3 - Subconsulta en WHERE: productos más caros que el promedio
-- Utilidad: identificar productos de precio alto.
-- Lógica: la subconsulta (no correlacionada) calcula una vez el promedio.
SELECT id, nombre, precio, stock
FROM producto
WHERE precio > (SELECT AVG(precio) FROM producto WHERE eliminado = false)
  AND eliminado = false
LIMIT 10;

-- Consulta 4 - Subconsulta correlacionada: producto más caro de cada categoría
-- Utilidad: conocer el producto "premium" de cada categoría.
-- Lógica: para cada fila, la subconsulta calcula el máximo de SU categoría.
SELECT p.id, p.nombre, p.precio, p.categoria_id
FROM producto p
WHERE p.precio = (
    SELECT MAX(sub.precio)
    FROM producto sub
    WHERE sub.categoria_id = p.categoria_id AND sub.eliminado = false
)
AND p.eliminado = false;
