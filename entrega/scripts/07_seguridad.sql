-- =====================================================================
-- 07_seguridad.sql - Etapa 4: seguridad e integridad
-- Usuario con privilegios mínimos, consulta parametrizada anti SQL
-- Injection y pruebas de restricciones PK, FK, UNIQUE y CHECK.
-- Idempotente: DROP ... IF EXISTS. Ejecutar como root. Requiere 06_vistas.sql.
-- =====================================================================
USE FoodStore;

-- ---------------------------------------------------------------------
-- 1. Usuario de aplicación con privilegios mínimos
-- NOTA DE SEGURIDAD: contraseña de ejemplo, solo para el entorno de prueba
-- de este TP. En producción nunca se escribiría en un script ni en el
-- repositorio: se gestionaría fuera del código (variables de entorno o un
-- gestor de secretos) y se usaría una contraseña robusta y única.
-- ---------------------------------------------------------------------
DROP USER IF EXISTS 'foodstore_app'@'%';
CREATE USER 'foodstore_app'@'%' IDENTIFIED BY 'PasswordSeguro123!';
-- Sin DELETE: el sistema usa baja lógica (UPDATE ... SET eliminado = true)
GRANT SELECT, INSERT, UPDATE ON FoodStore.categoria TO 'foodstore_app'@'%';
GRANT SELECT, INSERT, UPDATE ON FoodStore.producto  TO 'foodstore_app'@'%';
GRANT SELECT ON FoodStore.vista_catalogo_publico   TO 'foodstore_app'@'%';
GRANT SELECT ON FoodStore.vista_resumen_categorias TO 'foodstore_app'@'%';
SHOW GRANTS FOR 'foodstore_app'@'%';

-- ---------------------------------------------------------------------
-- 2. Consulta parametrizada y prueba anti SQL Injection
-- El parámetro se trata como DATO, nunca como código SQL.
-- (En Java, el equivalente es PreparedStatement con parámetros "?").
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_buscar_producto_por_nombre;
DELIMITER //
CREATE PROCEDURE sp_buscar_producto_por_nombre(IN p_nombre VARCHAR(255))
BEGIN
    SELECT p.id, p.nombre, p.precio, p.stock, c.nombre AS categoria
    FROM producto p
    JOIN categoria c ON p.categoria_id = c.id
    WHERE p.nombre = p_nombre AND p.eliminado = false;
END //
DELIMITER ;

-- Búsqueda legítima: devuelve 1 fila
CALL sp_buscar_producto_por_nombre('Producto 1');
-- Intento de inyección: se busca literalmente "' OR '1'='1" -> 0 filas
CALL sp_buscar_producto_por_nombre("' OR '1'='1");

-- ---------------------------------------------------------------------
-- 3. Pruebas de restricciones (también validación práctica de la Etapa 1)
-- sp_probar_restriccion ejecuta una sentencia y, si falla, muestra el
-- código y el mensaje de error sin cortar la ejecución del script.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_probar_restriccion;
DELIMITER //
CREATE PROCEDURE sp_probar_restriccion(IN p_prueba VARCHAR(100), IN p_sql TEXT)
BEGIN
    DECLARE v_codigo INT DEFAULT 0;
    DECLARE v_mensaje TEXT DEFAULT 'OK (sin error)';
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
        GET DIAGNOSTICS CONDITION 1 v_codigo = MYSQL_ERRNO, v_mensaje = MESSAGE_TEXT;
    SET @sql_prueba = p_sql;
    PREPARE s FROM @sql_prueba;
    EXECUTE s;
    DEALLOCATE PREPARE s;
    SELECT p_prueba AS prueba, v_codigo AS codigo_error, v_mensaje AS resultado;
END //
DELIMITER ;

-- Todas las pruebas se hacen dentro de una transacción que se deshace al
-- final, para no alterar los datos (el script queda idempotente).
START TRANSACTION;

-- Inserción válida
CALL sp_probar_restriccion('Válida: categoría nueva',
  "INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES ('Panadería', 'Pan y facturas', false, NOW())");
-- CHECK: precio negativo -> error 3819
CALL sp_probar_restriccion('CHECK precio >= 0',
  "INSERT INTO producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) VALUES ('Pan', 'comida', -2000, 25, 'pan.jpg', true, false, NOW(), 1)");
-- FK: categoría inexistente -> error 1452
CALL sp_probar_restriccion('FK categoria_id inexistente',
  "INSERT INTO producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) VALUES ('Producto huérfano', 'Prueba de FK', 1500, 10, 'test.jpg', true, false, NOW(), 999)");
-- PK: id duplicado -> error 1062
CALL sp_probar_restriccion('PK id duplicado',
  "INSERT INTO categoria (id, nombre, descripcion, eliminado, created_at) VALUES (1, 'Otra', 'PK repetida', false, NOW())");
-- UNIQUE condicional: categoría ACTIVA duplicada -> error 1062 (uk_nombre_activo)
CALL sp_probar_restriccion('UNIQUE: activa duplicada',
  "INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES ('Bebidas', 'Intento duplicado activo', false, NOW())");
-- UNIQUE condicional: baja lógica y re-creación con el mismo nombre -> OK
UPDATE categoria SET eliminado = true WHERE nombre = 'Bebidas' AND eliminado = false;
CALL sp_probar_restriccion('UNIQUE: re-crear tras baja lógica',
  "INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES ('Bebidas', 'Nombre reciclado', false, NOW())");
-- UNIQUE condicional: dos eliminadas con el mismo nombre conviven -> OK
CALL sp_probar_restriccion('UNIQUE: dos eliminadas iguales',
  "UPDATE categoria SET eliminado = true WHERE nombre = 'Bebidas' AND eliminado = false");
SELECT id, nombre, eliminado FROM categoria WHERE nombre = 'Bebidas';

ROLLBACK;   -- deshace todas las pruebas
