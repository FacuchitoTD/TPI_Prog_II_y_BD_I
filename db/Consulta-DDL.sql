-- creación de la tabla categoria según modelo relacional mínimo
create table categoria (
    id bigint auto_increment primary key,
    nombre varchar(255) not null,
    descripcion varchar(255) not null,
    eliminado boolean not null,
    created_at datetime not null,
    UNIQUE KEY uk_nombre_activo ((IF(eliminado = 0, nombre, NULL)))
);

-- creación de la tabla producto según modelo relacional mínimo
create table producto (
    id bigint auto_increment primary key,
    nombre varchar(255) not null,
    descripcion varchar(255) not null,
    precio double not null check (precio >= 0),
    stock int not null check (stock >= 0),
    imagen varchar(255) not null,
    disponible boolean not null,
    eliminado boolean not null,
    created_at datetime not null,
    categoria_id bigint not null,
    foreign key (categoria_id) references categoria(id)
);