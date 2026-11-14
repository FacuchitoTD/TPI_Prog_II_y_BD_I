-- =====================================================================
-- Etapa 3 - Consultas avanzadas, vista y mediciones con/sin índices
-- Base: FoodStore (MySQL 8.0, InnoDB) - 10.001 productos, 7 categorías
-- =====================================================================


-- ---------------------------------------------------------------------
-- Consulta 1 - JOIN: catálogo legible de productos activos
-- Utilidad: listado para mostrar al operador o en un reporte, con el
--   nombre de la categoría en lugar de su id.
-- Lógica: une producto con categoria por la FK (p.categoria_id = c.id)
--   y filtra las bajas lógicas de ambas tablas (eliminado = false).
-- ---------------------------------------------------------------------
SELECT
    p.id AS producto_id,
    p.nombre AS producto,
    p.precio,
    p.stock,
    c.nombre AS categoria
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false
LIMIT 10;


-- ---------------------------------------------------------------------
-- Consulta 2 - JOIN + GROUP BY + HAVING: estadísticas por categoría
-- Utilidad: reporte de gestión (cantidad de productos, stock total y
--   precio promedio por categoría), mostrando solo las categorías con
--   gran volumen (más de 1000 productos activos).
-- Lógica: LEFT JOIN para contemplar categorías sin productos; la
--   condición p.eliminado = false va en el ON para no descartarlas.
--   GROUP BY agrupa por categoría y HAVING filtra sobre el agregado
--   (WHERE no puede filtrar sobre COUNT).
-- ---------------------------------------------------------------------
SELECT
    c.nombre AS categoria,
    COUNT(p.id) AS total_productos,
    SUM(p.stock) AS stock_total,
    ROUND(AVG(p.precio), 2) AS precio_promedio
FROM categoria c
LEFT JOIN producto p ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre
HAVING COUNT(p.id) > 1000;


-- ---------------------------------------------------------------------
-- Consulta 3 - Subconsulta en WHERE: productos más caros que el promedio
-- Utilidad: identificar los productos de precio alto para análisis de
--   precios o promociones.
-- Lógica: la subconsulta (no correlacionada) calcula una sola vez el
--   precio promedio de los productos activos, y la consulta externa
--   devuelve los que lo superan.
-- ---------------------------------------------------------------------
SELECT
    id,
    nombre,
    precio,
    stock
FROM producto
WHERE precio > (SELECT AVG(precio) FROM producto WHERE eliminado = false)
  AND eliminado = false
LIMIT 10;


-- ---------------------------------------------------------------------
-- Consulta 4 - Subconsulta correlacionada: producto más caro por categoría
-- Utilidad: conocer el producto "premium" de cada categoría.
-- Lógica: la subconsulta depende de la fila externa (sub.categoria_id =
--   p.categoria_id): para cada producto calcula el precio máximo de SU
--   categoría y lo devuelve si coincide. Si hay empate, aparecen todos.
-- ---------------------------------------------------------------------
SELECT
    p.id,
    p.nombre,
    p.precio,
    p.categoria_id
FROM producto p
WHERE p.precio = (
    SELECT MAX(sub.precio)
    FROM producto sub
    WHERE sub.categoria_id = p.categoria_id AND sub.eliminado = false
)
AND p.eliminado = false;


-- ---------------------------------------------------------------------
-- Vista - vista_productos_activos
-- Utilidad: encapsula el JOIN y los filtros de baja lógica para que la
--   aplicación y los reportes consulten el catálogo activo con un
--   simple SELECT, sin repetir la lógica en cada consulta.
-- ---------------------------------------------------------------------
CREATE VIEW vista_productos_activos AS
SELECT
    p.id,
    p.nombre AS producto,
    p.descripcion,
    p.precio,
    p.stock,
    p.imagen,
    c.nombre AS categoria,
    p.created_at
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false;

SELECT * FROM vista_productos_activos LIMIT 10;


-- =====================================================================
-- ÍNDICES
-- =====================================================================
-- categoria_id: NO hace falta crear idx_producto_categoria. InnoDB crea
--   automáticamente un índice para la clave foránea (se llama
--   "categoria_id") y cumple la misma función. Crear otro sería redundante.
-- CREATE INDEX idx_producto_categoria ON producto(categoria_id);

-- precio: índice para búsquedas por rango de precio (creado el 07/11/2026;
--   si ya existe, esta sentencia da "Duplicate key name").
CREATE INDEX idx_producto_precio ON producto(precio);

-- Verificar los índices de la tabla
SHOW INDEX FROM producto;


-- =====================================================================
-- MEDICIONES CON Y SIN ÍNDICE (igualdad, rango y JOIN)
-- Cada variante se ejecutó 3 veces con EXPLAIN ANALYZE y se tomó la
-- mediana. Sin índice: IGNORE INDEX. Con índice: FORCE INDEX.
-- Ejecución automatizada: Etapa3-mediciones/medir.sh (carpeta del TPI)
-- Resultados completos:   Etapa3-mediciones/resultados-etapa3.md
-- =====================================================================

-- Igualdad (2000 filas cumplen el filtro)
EXPLAIN ANALYZE SELECT * FROM producto p IGNORE INDEX (categoria_id)
WHERE p.categoria_id = 2 AND p.eliminado = false;
EXPLAIN ANALYZE SELECT * FROM producto p FORCE INDEX (categoria_id)
WHERE p.categoria_id = 2 AND p.eliminado = false;

-- Rango (3997 filas cumplen el filtro)
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

/*
RESULTADOS (07/11/2026 22:48) - tiempos en ms

| Consulta | Variante   | type / key                   | C1   | C2   | C3   | Mediana |
|----------|------------|------------------------------|------|------|------|---------|
| Igualdad | Sin índice | ALL / NULL                   | 14.9 | 12.2 | 11.5 | 12.2    |
| Igualdad | Con índice | ref / categoria_id           | 4.99 | 4.43 | 4.53 | 4.53    |
| Rango    | Sin índice | ALL / NULL                   | 11.1 | 7.15 | 7.43 | 7.43    |
| Rango    | Con índice | range / idx_producto_precio  | 18.2 | 7.88 | 9.14 | 9.14    |
| JOIN     | Sin índice | ALL / NULL (hash join)       | 32.8 | 19.8 | 19.3 | 19.8    |
| JOIN     | Con índice | ref / categoria_id           | 18.9 | 19.2 | 16   | 18.9    |

INTERPRETACIÓN
- Igualdad: el índice redujo la mediana de 12.2 a 4.53 ms (~63%). Sin índice
  se recorren las 10.001 filas (ALL); con índice se accede solo a las 2000
  de la categoría (ref).
- Rango: con índice fue MÁS LENTO (9.14 vs 7.43 ms). El rango abarca ~40% de
  la tabla (3997 filas): por cada entrada del índice secundario hay que ir a
  buscar la fila completa al índice agrupado (accesos dispersos), y eso cuesta
  más que un escaneo secuencial. Un índice conviene cuando el filtro es
  selectivo (pocas filas). Por eso, sin FORCE INDEX, el optimizador puede
  preferir el escaneo completo.
- JOIN: mejora leve (19.8 vs 18.9 ms; en una corrida previa, 30.4 vs 20.9).
  La consulta agrega TODOS los productos de las categorías activas, así que
  igual se leen casi todas las filas; el índice evita el hash join, pero
  la ganancia es chica.
- Con 10.000 filas los tiempos son de pocos milisegundos y hay variación
  entre corridas (de ahí la mediana). La consigna sugiere 200.000-500.000
  registros para diferencias más estables.
*/
