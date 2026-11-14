insert into categoria (nombre, descripcion, eliminado, created_at) values ('bebidas', 'bebidas sin y con alcohol', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('comida', 'comida rápida', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('snacks', 'snacks', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('golosinas', 'golosinas', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('bebidas', 'bebidas sin y con alcohol', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('comida', 'comida rápida', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('snacks', 'snacks', false, current_timestamp());
insert into categoria (nombre, descripcion, eliminado, created_at) values ('golosinas', 'golosinas', false, current_timestamp());

insert into producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) values ('coca cola 1.5l', 'gaseosa retornable', 2500.0, 50, 'coca15.jpg', true, false, current_timestamp(), 1);
insert into producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) values ('Pan', 'comida', -2000, 25, 'pan.jpg', true, false, current_timestamp(), 1);

insert into producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) values ('Producto huérfano', 'Prueba de violación de FK', 1500.0, 10, 'test.jpg', true, false, current_timestamp(), 999);


-- Esto debe arrojar el ERROR 1062 (Duplicate entry... for key 'categoria.uk_nombre_activo')
INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES ('Bebidas', 'Intento duplicado activo', 0, NOW());

-- 1. Damos de baja lógica la categoría actual (reemplaza el ID por el de tu categoría existente)
UPDATE categoria 
SET eliminado = 1 
WHERE id = 1;

-- 2. Insertamos una nueva categoría activa con el mismo nombre (Debe dejar pasar sin error)
INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES ('Bebidas', 'Nueva categoría con nombre reciclado', 0, NOW());

-- Damos de baja la última categoría activa creada
UPDATE categoria 
SET eliminado = 1 
WHERE nombre = 'Bebidas' AND eliminado = 0;

-- Verificamos con un SELECT que conviven ambas eliminadas
SELECT id, nombre, eliminado FROM categoria WHERE nombre = 'Bebidas';
