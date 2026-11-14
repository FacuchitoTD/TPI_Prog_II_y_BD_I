-- =====================================================================
-- 08_transacciones.sql - Etapa 5: transacciones (START TRANSACTION /
-- COMMIT / ROLLBACK) y referencia a la implementación del retry.
-- Idempotente: las operaciones de prueba se deshacen o se compensan.
-- =====================================================================
USE FoodStore;

-- ---------------------------------------------------------------------
-- 1. Transacción exitosa: confirmar un pedido descuenta stock de dos
--    productos como una sola unidad (atomicidad). Se compensa al final.
-- ---------------------------------------------------------------------
SELECT id, nombre, stock FROM producto WHERE id IN (2, 3);

START TRANSACTION;
UPDATE producto SET stock = stock - 1 WHERE id = 2 AND eliminado = false AND stock >= 1;
UPDATE producto SET stock = stock - 1 WHERE id = 3 AND eliminado = false AND stock >= 1;
COMMIT;

SELECT id, nombre, stock FROM producto WHERE id IN (2, 3);   -- 1 unidad menos cada uno

-- Compensación (para que el script sea idempotente)
UPDATE producto SET stock = stock + 1 WHERE id IN (2, 3);

-- ---------------------------------------------------------------------
-- 2. Transacción con ROLLBACK: si una parte falla, se deshace todo.
-- ---------------------------------------------------------------------
START TRANSACTION;
UPDATE producto SET stock = stock - 1 WHERE id = 2;
-- ... error de negocio detectado (por ejemplo, el segundo producto no
-- tiene stock o fue dado de baja) ...
ROLLBACK;

SELECT id, nombre, stock FROM producto WHERE id = 2;   -- stock sin cambios

-- ---------------------------------------------------------------------
-- 3. Retry ante deadlock (ERROR 1213 / SQLSTATE 40001)
-- Implementado en Java (JDBC puro), opción permitida por la consigna:
--   Etapa5-Java/src/RetryDeadlockDemo.java
-- Dos hilos con conexiones distintas descuentan stock en orden cruzado:
--   UPDATE producto SET stock = stock - ? WHERE id = ? AND eliminado = FALSE AND stock >= ?
-- Ante 1213/40001: ROLLBACK + logging + backoff (100 ms, 200 ms) y
-- reintento de la transacción completa, hasta 2 reintentos. Los errores
-- de negocio no se reintentan.
-- Ejecución: java -cp "out:lib/mysql-connector-j-8.3.0.jar" RetryDeadlockDemo 2 3
-- Evidencia: Etapa5-Java/etapa5-retry.log (resultados en el PDF).
-- ---------------------------------------------------------------------
