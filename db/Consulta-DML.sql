-- dml: consultas ejecutables en sql puro 

-- 1. validar unicidad entre categorías activas 
select count(*) from categoria where nombre = 'bebidas' and eliminado = false;

-- 2. listar solo categorías activas
select id, nombre, descripcion, created_at from categoria where eliminado = false;

-- 3. insertar una nueva categoría de prueba
insert into categoria (nombre, descripcion, eliminado, created_at) 
values ('bebidas', 'bebidas sin y con alcohol', false, current_timestamp());

-- 4. baja lógica de una categoría específica por su id
update categoria set eliminado = true where id = 1;

-- 5. listar solo productos activos
select id, nombre, precio, stock, disponible, created_at from producto where eliminado = false;

-- 6. listar productos de una categoría específica 
select id, nombre, precio, stock, disponible, created_at from producto where categoria_id = 1 and eliminado = false;

-- 7. insertar un nuevo producto de prueba
insert into producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id) 
values ('coca cola 1.5l', 'gaseosa retornable', 2500.0, 50, 'coca15.jpg', true, false, current_timestamp(), 1);

-- 8. baja lógica de un producto por su id
update producto set eliminado = true where id = 1;

select * from categoria;
select * from producto;