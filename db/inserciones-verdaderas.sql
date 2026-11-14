insert into categoria (nombre, descripcion, eliminado, created_at) values 
('Bebidas', 'Bebidas sin y con alcohol', false, current_timestamp()),
('Almacen', 'Productos de almacén y despensa', false, current_timestamp()),
('Limpieza', 'Artículos de limpieza para el hogar', false, current_timestamp()),
('Lácteos', 'Leche, quesos y derivados', false, current_timestamp()),
('Fiambrería', 'Fiambres y embutidos', false, current_timestamp());

SET cte_max_recursion_depth = 15000;

insert into producto (nombre, descripcion, precio, stock, imagen, disponible, eliminado, created_at, categoria_id)
with recursive n(seq) as (
    select 1
    union all
    select seq + 1 from n where seq < 10000 -- Cantidad de filas a generar (10 mil)
)
select 
    concat('Producto ', seq) as nombre,
    concat('Descripción del producto genérico número ', seq) as descripcion,
    round(100 + (rand() * 9900), 2) as precio, -- Precio aleatorio entre 100 y 10000
    floor(1 + (rand() * 100)) as stock,       -- Stock aleatorio entre 1 y 100
    concat('img_', seq, '.jpg') as imagen,
    true as disponible,
    false as eliminado,
    current_timestamp() as created_at,
    1 + floor(rand() * 5) as categoria_id     -- Asigna aleatoriamente una categoría entre 1 y 5
from n;