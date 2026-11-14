-- =====================================================================
-- Modificación de la tabla categoria (tabla ya existente con datos)
-- Regla: el nombre debe ser único solo entre las categorías activas.
-- Índice único funcional: si la categoría está activa, la clave es el
-- nombre; si está eliminada, la clave es NULL. Un índice UNIQUE admite
-- varios NULL, así que las categorías eliminadas no compiten entre sí.
-- Requiere MySQL 8.0.13 o superior.
-- =====================================================================


-- Paso 1 - Verificar que NO haya nombres repetidos entre categorías activas
-- (si esta consulta devuelve filas, hay que corregir esos datos antes del
-- paso 2; si no, el ALTER TABLE falla con ERROR 1062 Duplicate entry)
SELECT nombre, COUNT(*) AS cantidad
FROM categoria
WHERE eliminado = FALSE
GROUP BY nombre
HAVING COUNT(*) > 1;


-- Paso 2 - Agregar el índice único condicional
ALTER TABLE categoria
    ADD UNIQUE KEY uk_nombre_activo ((IF(eliminado = 0, nombre, NULL)));


-- Paso 3 - Verificar que el índice quedó creado
SHOW INDEX FROM categoria;
