-- =====================================================================
-- 03_carga_masiva.sql - Etapa 2: carga masiva con SQL puro (10.000 productos)
-- Mecanismo: CTE recursiva (WITH RECURSIVE) que genera la serie 1..10000
-- y alimenta un único INSERT ... SELECT. Requiere 02_catalogos.sql.
-- Idempotente: borra los productos generados antes de volver a cargarlos.
-- =====================================================================
USE FoodStore;

DELETE FROM producto WHERE nombre LIKE 'Producto %';

-- MySQL limita las CTE recursivas a 1000 iteraciones por defecto
SET SESSION cte_max_recursion_depth = 15000;

INSERT INTO producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id)
WITH RECURSIVE n(seq) AS (
    SELECT 1
    UNION ALL
    SELECT seq + 1 FROM n WHERE seq < 10000          -- cantidad de filas a generar
)
SELECT
    CONCAT('Producto ', seq)                                     AS nombre,
    CONCAT('Descripción del producto genérico número ', seq)     AS descripcion,
    ROUND(100 + (RAND() * 9900), 2)                              AS precio,       -- 100 a 10000
    FLOOR(1 + (RAND() * 100))                                    AS stock,        -- 1 a 100
    CONCAT('img_', seq, '.jpg')                                  AS imagen,
    true                                                         AS disponible,
    false                                                        AS eliminado,
    CURRENT_TIMESTAMP()                                          AS created_at,
    (SELECT MIN(id) FROM categoria) + FLOOR(RAND() * 5)          AS categoria_id  -- una de las 5 categorías
FROM n;

-- ---------------------------------------------------------------------
-- Verificaciones de consistencia (cuadro de la Etapa 2)
-- ---------------------------------------------------------------------
-- 1. Conteo de filas generadas (esperado: 10000)
SELECT COUNT(*) AS filas_generadas FROM producto WHERE nombre LIKE 'Producto %';
-- 2. FK huérfanas (esperado: 0)
SELECT COUNT(*) AS fk_huerfanas
FROM producto p LEFT JOIN categoria c ON p.categoria_id = c.id
WHERE c.id IS NULL;
-- 3. Dominio de precio fuera de 100..10000 (esperado: 0)
SELECT COUNT(*) AS precio_fuera_de_dominio FROM producto
WHERE nombre LIKE 'Producto %' AND (precio < 100 OR precio > 10000);
-- 4. Dominio de stock fuera de 1..100 (esperado: 0)
SELECT COUNT(*) AS stock_fuera_de_dominio FROM producto
WHERE nombre LIKE 'Producto %' AND (stock < 1 OR stock > 100);
-- 5. Distribución por categoría (cardinalidad 1:N, ~20 % cada una)
SELECT categoria_id, COUNT(*) AS productos,
       ROUND(100 * COUNT(*) / (SELECT COUNT(*) FROM producto WHERE nombre LIKE 'Producto %'), 1) AS porcentaje
FROM producto WHERE nombre LIKE 'Producto %'
GROUP BY categoria_id ORDER BY categoria_id;
