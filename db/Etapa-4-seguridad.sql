-- 1. Crear usuario de aplicación con privilegios acotados
-- NOTA DE SEGURIDAD: contraseña de ejemplo, solo para el entorno de prueba
-- de este TP. En producción nunca se escribiría en un script ni en el
-- repositorio: se gestionaría fuera del código (variables de entorno o un
-- gestor de secretos) y se usaría una contraseña robusta y única.
CREATE USER 'foodstore_app'@'%' IDENTIFIED BY 'PasswordSeguro123!';

-- Otorgar solo los permisos estrictamente necesarios (Principio de Mínimos Privilegios)
GRANT SELECT, INSERT, UPDATE ON FoodStore.categoria TO 'foodstore_app'@'%';
GRANT SELECT, INSERT, UPDATE ON FoodStore.producto TO 'foodstore_app'@'%';

-- Aplicar cambios de privilegios
FLUSH PRIVILEGES;

-- 2. Vistas para ocultar columnas/información sensible
-- Vista 1: Catálogo Público (Oculta datos internos como eliminado o campos de auditoría)
CREATE VIEW vista_catalogo_publico AS
SELECT 
    p.id AS codigo_producto,
    p.nombre AS producto,
    p.descripcion,
    p.precio,
    p.disponible,
    c.nombre AS categoria
FROM producto p
JOIN categoria c ON p.categoria_id = c.id
WHERE p.eliminado = false AND c.eliminado = false AND p.disponible = true;

-- Vista 2: Resumen Gerencial por Categoría (Expone solo totales agregados, ocultando precios unitarios)
CREATE VIEW vista_resumen_categorias AS
SELECT 
    c.id AS categoria_id,
    c.nombre AS categoria,
    COUNT(p.id) AS cantidad_productos,
    SUM(p.stock) AS stock_acumulado
FROM categoria c
LEFT JOIN producto p ON c.id = p.categoria_id AND p.eliminado = false
WHERE c.eliminado = false
GROUP BY c.id, c.nombre;

-- Probar acceso desde las vistas
SELECT * FROM vista_catalogo_publico LIMIT 10;
SELECT * FROM vista_resumen_categorias;

-- 3. Consulta segura y prueba anti-inyección SQL
DELIMITER //
CREATE PROCEDURE sp_buscar_producto_por_nombre(IN p_nombre VARCHAR(255))
BEGIN
    SELECT p.id, p.nombre, p.precio, p.stock, c.nombre AS categoria
    FROM producto p
    JOIN categoria c ON p.categoria_id = c.id
    WHERE p.nombre = p_nombre AND p.eliminado = false;
END //
DELIMITER ;

-- Prueba: Intento de inyección neutralizado (devuelve 0 filas)
CALL sp_buscar_producto_por_nombre("' OR '1'='1");