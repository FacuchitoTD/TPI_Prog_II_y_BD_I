-- =====================================================================
-- Etapa 5 - Concurrencia y transacciones
-- Simulación de deadlock entre dos sesiones
-- Fecha de ejecución: 07/11/2026
-- =====================================================================
-- Sesión A: pestaña de consultas del editor (VS Code, extensión de BD)
-- Sesión B: terminal con el cliente mysql (mysql -u root -p FoodStore)
--
-- IMPORTANTE: ejecutar sentencia por sentencia, respetando el número
-- de paso e intercalando entre las dos sesiones. No ejecutar el
-- archivo completo de una sola vez: en una única sesión no se produce
-- el deadlock.
-- =====================================================================


-- ---------------------------------------------------------------------
-- Paso 0 - Ambas sesiones: verificar que son conexiones distintas
-- (CONNECTION_ID() debe devolver un número diferente en cada una)
-- ---------------------------------------------------------------------
SELECT CONNECTION_ID();


-- ---------------------------------------------------------------------
-- Paso 1 - Sesión A: inicia la transacción
-- ---------------------------------------------------------------------
START TRANSACTION;

-- ---------------------------------------------------------------------
-- Paso 2 - Sesión A: bloquea (bloqueo exclusivo) el producto id = 1
-- ---------------------------------------------------------------------
SELECT * FROM producto WHERE id = 1 FOR UPDATE;


-- ---------------------------------------------------------------------
-- Paso 3 - Sesión B: inicia la transacción
-- ---------------------------------------------------------------------
START TRANSACTION;

-- ---------------------------------------------------------------------
-- Paso 4 - Sesión B: bloquea (bloqueo exclusivo) el producto id = 2
-- ---------------------------------------------------------------------
SELECT * FROM producto WHERE id = 2 FOR UPDATE;


-- ---------------------------------------------------------------------
-- Paso 5 - Sesión A: pide el producto id = 2, que tiene la Sesión B
-- Resultado: la sesión A queda EN ESPERA (espera por bloqueo)
-- ---------------------------------------------------------------------
SELECT * FROM producto WHERE id = 2 FOR UPDATE;


-- ---------------------------------------------------------------------
-- Paso 6 - Sesión B: pide el producto id = 1, que tiene la Sesión A
-- Se forma el ciclo (A espera a B y B espera a A) -> DEADLOCK
-- Ejecutar antes de que pasen 50 s desde el paso 5
-- (innodb_lock_wait_timeout); si no, A recibe el error 1205.
--
-- Resultado obtenido en la Sesión B (víctima):
--   ERROR 1213 (40001): Deadlock found when trying to get lock;
--   try restarting transaction
-- InnoDB hace ROLLBACK de la transacción de B y libera la fila id = 2.
-- La Sesión A se destraba y obtiene la fila id = 2 (espera: 47.4 s).
-- ---------------------------------------------------------------------
SELECT * FROM producto WHERE id = 1 FOR UPDATE;


-- ---------------------------------------------------------------------
-- Paso 7 - Sesión A: finaliza la transacción y libera los bloqueos
-- (ROLLBACK porque es solo una prueba y no se modificaron datos)
-- ---------------------------------------------------------------------
ROLLBACK;


-- ---------------------------------------------------------------------
-- Paso 8 - Cualquier sesión: evidencia del deadlock
-- Ver la sección "LATEST DETECTED DEADLOCK" (guarda solo el último).
-- En la terminal, \G muestra el resultado en formato vertical legible.
-- ---------------------------------------------------------------------
SHOW ENGINE INNODB STATUS;


-- =====================================================================
-- EVIDENCIA - Salida de SHOW ENGINE INNODB STATUS (07/11/2026)
-- Sección LATEST DETECTED DEADLOCK
--
-- Correspondencia con las sesiones del guion:
--   TRANSACTION (1) = 27095, thread id 31 (127.0.0.1) -> Sesión A (editor)
--     tiene: id = 1 | espera: id = 2
--   TRANSACTION (2) = 27096, thread id 30 (localhost) -> Sesión B (terminal)
--     tiene: id = 2 | espera: id = 1
--   Víctima: TRANSACTION (2) -> Sesión B (recibió el ERROR 1213)
--
-- Capturas asociadas (carpeta capturas/ del TPI):
--   deadlock.png   -> Sesión A en espera por bloqueo (paso 5)
--   Deadlock_2.png -> ERROR 1213 en la Sesión B (paso 6)
-- =====================================================================
/*
------------------------
LATEST DETECTED DEADLOCK
------------------------
2026-11-07 19:45:03 131112102876864
*** (1) TRANSACTION:
TRANSACTION 27095, ACTIVE 88 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 3 lock struct(s), heap size 1128, 2 row lock(s)
MySQL thread id 31, OS thread handle 131112069306048, query id 219 localhost 127.0.0.1 root statistics
SELECT * FROM producto WHERE id = 2 FOR UPDATE

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 22 page no 6 n bits 192 index PRIMARY of table `FoodStore`.`producto` trx id 27095 lock_mode X locks rec but not gap
Record lock, heap no 2 PHYSICAL RECORD: n_fields 12; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;
 1: len 6; hex 0000000069bb; asc     i ;;
 2: len 7; hex 02000001480151; asc     H Q;;
 3: len 14; hex 636f636120636f6c6120312e356c; asc coca cola 1.5l;;
 4: len 18; hex 676173656f7361207265746f726e61626c65; asc gaseosa retornable;;
 5: len 8; hex 000000000088a340; asc        @;;
 6: len 4; hex 80000032; asc    2;;
 7: len 10; hex 636f636131352e6a7067; asc coca15.jpg;;
 8: len 1; hex 81; asc  ;;
 9: len 1; hex 81; asc  ;;
 10: len 5; hex 99bb4ed1f1; asc   N  ;;
 11: len 8; hex 8000000000000001; asc         ;;


*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 22 page no 6 n bits 192 index PRIMARY of table `FoodStore`.`producto` trx id 27095 lock_mode X locks rec but not gap waiting
Record lock, heap no 3 PHYSICAL RECORD: n_fields 12; compact format; info bits 0
 0: len 8; hex 8000000000000002; asc         ;;
 1: len 6; hex 0000000069c6; asc     i ;;
 2: len 7; hex 82000000bc0110; asc        ;;
 3: len 10; hex 50726f647563746f2031; asc Producto 1;;
 4: len 30; hex 446573637269706369c3b36e2064656c2070726f647563746f2067656ec3; asc Descripci  n del producto gen ; (total 45 bytes);
 5: len 8; hex 52b81e85ab2cb240; asc R    , @;;
 6: len 4; hex 80000048; asc    H;;
 7: len 9; hex 696d675f312e6a7067; asc img_1.jpg;;
 8: len 1; hex 81; asc  ;;
 9: len 1; hex 80; asc  ;;
 10: len 5; hex 99bb4ed3c5; asc   N  ;;
 11: len 8; hex 8000000000000002; asc         ;;


*** (2) TRANSACTION:
TRANSACTION 27096, ACTIVE 63 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 3 lock struct(s), heap size 1128, 2 row lock(s)
MySQL thread id 30, OS thread handle 131112401565376, query id 220 localhost root statistics
SELECT * FROM producto WHERE id = 1 FOR UPDATE

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 22 page no 6 n bits 192 index PRIMARY of table `FoodStore`.`producto` trx id 27096 lock_mode X locks rec but not gap
Record lock, heap no 3 PHYSICAL RECORD: n_fields 12; compact format; info bits 0
 0: len 8; hex 8000000000000002; asc         ;;
 1: len 6; hex 0000000069c6; asc     i ;;
 2: len 7; hex 82000000bc0110; asc        ;;
 3: len 10; hex 50726f647563746f2031; asc Producto 1;;
 4: len 30; hex 446573637269706369c3b36e2064656c2070726f647563746f2067656ec3; asc Descripci  n del producto gen ; (total 45 bytes);
 5: len 8; hex 52b81e85ab2cb240; asc R    , @;;
 6: len 4; hex 80000048; asc    H;;
 7: len 9; hex 696d675f312e6a7067; asc img_1.jpg;;
 8: len 1; hex 81; asc  ;;
 9: len 1; hex 80; asc  ;;
 10: len 5; hex 99bb4ed3c5; asc   N  ;;
 11: len 8; hex 8000000000000002; asc         ;;


*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 22 page no 6 n bits 192 index PRIMARY of table `FoodStore`.`producto` trx id 27096 lock_mode X locks rec but not gap waiting
Record lock, heap no 2 PHYSICAL RECORD: n_fields 12; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;
 1: len 6; hex 0000000069bb; asc     i ;;
 2: len 7; hex 02000001480151; asc     H Q;;
 3: len 14; hex 636f636120636f6c6120312e356c; asc coca cola 1.5l;;
 4: len 18; hex 676173656f7361207265746f726e61626c65; asc gaseosa retornable;;
 5: len 8; hex 000000000088a340; asc        @;;
 6: len 4; hex 80000032; asc    2;;
 7: len 10; hex 636f636131352e6a7067; asc coca15.jpg;;
 8: len 1; hex 81; asc  ;;
 9: len 1; hex 81; asc  ;;
 10: len 5; hex 99bb4ed1f1; asc   N  ;;
 11: len 8; hex 8000000000000001; asc         ;;

*** WE ROLL BACK TRANSACTION (2)
*/


-- =====================================================================
-- Etapa 5 - Comparación de niveles de aislamiento
-- READ COMMITTED vs REPEATABLE READ (fenómeno: lectura no repetible)
-- Fecha de ejecución: 07/11/2026
-- =====================================================================
-- Sesión A: pestaña de consultas del editor (cambia su nivel de aislamiento)
-- Sesión B: terminal mysql (autocommit = 1, nivel por defecto)
-- Dato observado: precio del producto id = 1 (coca cola 1.5l), inicial 2500
--
-- Ejecutar sentencia por sentencia, intercalando entre las sesiones.
-- Las lecturas de la Sesión A usan SELECT común (sin FOR UPDATE): una
-- lectura con FOR UPDATE siempre lee el dato actual y no usa la "foto".
-- =====================================================================


-- =====================================================================
-- CORRIDA 1: READ COMMITTED
-- =====================================================================

-- Paso 0 - Sesión A: configurar el nivel de la sesión y verificar
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT @@transaction_isolation;            -- Resultado: READ-COMMITTED

-- Paso 1 - Sesión A: abrir la transacción
START TRANSACTION;

-- Paso 2 - Sesión A: primera lectura
SELECT id, nombre, precio FROM producto WHERE id = 1;
-- Resultado: 2500

-- Paso 3 - Sesión B: cambiar el precio (con autocommit se confirma al instante)
UPDATE producto SET precio = 3000 WHERE id = 1;
-- Resultado: Query OK, 1 row affected

-- Paso 4 - Sesión A: segunda lectura, DENTRO de la misma transacción
SELECT @@transaction_isolation, p.id, p.nombre, p.precio
FROM producto p WHERE p.id = 1;
-- Resultado: READ-COMMITTED | 1 | coca cola 1.5l | 3000
-- -> Ve el cambio confirmado por B: LECTURA NO REPETIBLE
--    (misma consulta, misma transacción, valor distinto)

-- Paso 5 - Sesión A: cerrar la transacción
COMMIT;

-- Paso 6 - Sesión A: tercera lectura, fuera de la transacción
SELECT id, nombre, precio FROM producto WHERE id = 1;
-- Resultado: 3000

-- Paso 7 - Sesión B: restaurar el precio para la siguiente corrida
UPDATE producto SET precio = 2500 WHERE id = 1;


-- =====================================================================
-- CORRIDA 2: REPEATABLE READ (nivel por defecto de MySQL/InnoDB)
-- =====================================================================

-- Paso 0 - Sesión A: configurar el nivel de la sesión y verificar
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT @@transaction_isolation;            -- Resultado: REPEATABLE-READ

-- Paso 1 - Sesión A: abrir la transacción
START TRANSACTION;

-- Paso 2 - Sesión A: primera lectura
-- (en MySQL la "foto" se toma en la PRIMERA LECTURA, no en el START TRANSACTION)
SELECT id, nombre, precio FROM producto WHERE id = 1;
-- Resultado: 2500

-- Paso 3 - Sesión B: cambiar el precio (con autocommit se confirma al instante)
UPDATE producto SET precio = 3000 WHERE id = 1;

-- Paso 4 - Sesión A: segunda lectura, DENTRO de la misma transacción
SELECT @@transaction_isolation, p.id, p.nombre, p.precio
FROM producto p WHERE p.id = 1;
-- Resultado: REPEATABLE-READ | 1 | coca cola 1.5l | 2500
-- -> Sigue viendo la "foto" tomada en el paso 2, aunque B ya confirmó 3000:
--    LECTURA REPETIBLE (lectura consistente con MVCC)

-- Paso 5 - Sesión A: cerrar la transacción
COMMIT;

-- Paso 6 - Sesión A: tercera lectura, fuera de la transacción
SELECT id, nombre, precio FROM producto WHERE id = 1;
-- Resultado: 3000 (al cerrar la transacción se descarta la "foto")

-- Paso 7 - Sesión B: restaurar el precio original
UPDATE producto SET precio = 2500 WHERE id = 1;


-- =====================================================================
-- RESUMEN DE RESULTADOS (Sesión A)
-- ---------------------------------------------------------------------
--                               READ COMMITTED   REPEATABLE READ
-- Paso 2 (primera lectura)           2500             2500
-- Paso 4 (tras COMMIT de B)          3000             2500   <- diferencia
-- Paso 6 (fuera de la transacción)   3000             3000
--
-- Capturas asociadas (carpeta capturas/ del TPI):
--   committed.png                              -> paso 0, READ COMMITTED
--   primera lectura(etapa5 aislamiento).png    -> paso 2, READ COMMITTED
--   sesion B Aislamiento etapa 5.png           -> paso 3, UPDATE de la Sesión B
--   captura corregida paso 4 etapa 5.png       -> paso 4, READ COMMITTED (3000)
--   paso 6 etapa 5.png                         -> paso 6, READ COMMITTED (3000)
--   tercera lectura seccion b etapa 5.png      -> paso 7, precio restaurado (2500)
--   repeatable.png                             -> paso 0, REPEATABLE READ
--   repeatable CORREGIDO.png                   -> paso 2, REPEATABLE READ (2500)
--   paso 4 etapa 5 corregido.png               -> paso 4, REPEATABLE READ (2500)
--   paso 4 etapa 5 corregido 2.png             -> paso 6, REPEATABLE READ (3000)
-- =====================================================================


-- =====================================================================
-- Etapa 5 - Transacción con retry ante deadlock (implementación en Java)
-- Fecha de ejecución: 07/11/2026
-- =====================================================================
-- La consigna permite implementar el retry en Java o en SQL. Se eligió
-- Java (JDBC puro), reutilizable en el TPI de Programación II.
--
-- Código:    Etapa5-Java/src/RetryDeadlockDemo.java (carpeta del TPI)
-- Driver:    Etapa5-Java/lib/mysql-connector-j-8.3.0.jar
-- Ejecución: java -cp "out:lib/mysql-connector-j-8.3.0.jar" RetryDeadlockDemo <idP1> <idP2>
-- Log:       Etapa5-Java/etapa5-retry.log
--
-- Escenario: dos hilos con conexiones distintas simulan dos pedidos que
-- descuentan stock en orden cruzado. Sentencias que ejecuta cada transacción
-- (setAutoCommit(false) equivale a START TRANSACTION):
--
--   UPDATE producto SET stock = stock - ?
--   WHERE id = ? AND eliminado = FALSE AND stock >= ?;   -- primer producto
--   UPDATE producto SET stock = stock - ?
--   WHERE id = ? AND eliminado = FALSE AND stock >= ?;   -- segundo producto
--   COMMIT;   -- o ROLLBACK si hay error
--
-- Ante ERROR 1213 / SQLSTATE 40001: ROLLBACK + log + backoff (100 ms, 200 ms)
-- y reintento de la transacción completa, hasta 2 reintentos.
-- Los errores de negocio (stock insuficiente, producto eliminado) NO se reintentan.
--
-- Resultados:
--   Ejecución 1 (productos 1 y 2): el producto 1 está eliminado (baja lógica).
--     Error de negocio, sin reintento. El Pedido B ya había descontado
--     el producto 2, pero el ROLLBACK lo deshizo: stock 72 -> 72 (atomicidad).
--   Ejecución 2 (productos 2 y 3): deadlock; víctima el Pedido B.
--     22:13:38.845 [ADVERTENCIA] Pedido-B | intento 1 | DEADLOCK (error 1213,
--                  SQLSTATE 40001): se hizo ROLLBACK, reintenta en 100 ms
--     22:13:39.545 [INFORMACIÓN] Pedido-B | intento 2 | COMMIT exitoso
--     Stock: id 2 72 -> 70, id 3 87 -> 85 (se aplicaron los dos pedidos).
-- =====================================================================
