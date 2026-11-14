-- Food Store - datos de ejemplo para pruebas y para el video (opcional).
-- Ejecutar después de 01_esquema.sql:  mysql -u root -p FoodStore < db/02_datos_ejemplo.sql
-- Sólo inserta lo que no exista: si una categoría o producto activo con ese nombre ya está, lo saltea.

USE FoodStore;

INSERT INTO categoria (nombre, descripcion, eliminado, created_at)
SELECT 'Pizzas', 'Pizzas a la piedra', FALSE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Pizzas' AND eliminado = FALSE);

INSERT INTO categoria (nombre, descripcion, eliminado, created_at)
SELECT 'Hamburguesas', 'Hamburguesas caseras con papas', FALSE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Hamburguesas' AND eliminado = FALSE);

INSERT INTO categoria (nombre, descripcion, eliminado, created_at)
SELECT 'Postres', 'Postres y helados', FALSE, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM categoria WHERE nombre = 'Postres' AND eliminado = FALSE);

INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id)
SELECT 'Muzzarella', 9500, 'Pizza grande de muzzarella', 20, 'muzzarella.jpg', TRUE, FALSE, NOW(), c.id
FROM categoria c
WHERE c.nombre = 'Pizzas' AND c.eliminado = FALSE
  AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre = 'Muzzarella' AND p.eliminado = FALSE);

INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id)
SELECT 'Napolitana', 11000, 'Muzzarella, tomate y ajo', 15, 'napolitana.jpg', TRUE, FALSE, NOW(), c.id
FROM categoria c
WHERE c.nombre = 'Pizzas' AND c.eliminado = FALSE
  AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre = 'Napolitana' AND p.eliminado = FALSE);

INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id)
SELECT 'Hamburguesa clasica', 8500, 'Carne, lechuga, tomate y queso', 25, 'clasica.jpg', TRUE, FALSE, NOW(), c.id
FROM categoria c
WHERE c.nombre = 'Hamburguesas' AND c.eliminado = FALSE
  AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre = 'Hamburguesa clasica' AND p.eliminado = FALSE);

INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id)
SELECT 'Hamburguesa doble', 11500, 'Doble carne, cheddar y panceta', 3, 'doble.jpg', TRUE, FALSE, NOW(), c.id
FROM categoria c
WHERE c.nombre = 'Hamburguesas' AND c.eliminado = FALSE
  AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre = 'Hamburguesa doble' AND p.eliminado = FALSE);

INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id)
SELECT 'Flan casero', 3500, 'Con dulce de leche', 10, 'flan.jpg', FALSE, FALSE, NOW(), c.id
FROM categoria c
WHERE c.nombre = 'Postres' AND c.eliminado = FALSE
  AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.nombre = 'Flan casero' AND p.eliminado = FALSE);
