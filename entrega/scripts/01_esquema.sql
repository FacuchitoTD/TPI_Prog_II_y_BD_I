-- =====================================================================
-- 01_esquema.sql - Etapa 1: esquema físico (PK, FK, UNIQUE, CHECK)
-- TFI Bases de Datos I - Food Store - MySQL 8.0 (InnoDB)
-- Idempotente: elimina y vuelve a crear los objetos.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS FoodStore
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE FoodStore;

-- Objetos dependientes primero (vistas y procedimientos de scripts posteriores)
DROP VIEW IF EXISTS vista_productos_activos;
DROP VIEW IF EXISTS vista_catalogo_publico;
DROP VIEW IF EXISTS vista_resumen_categorias;
DROP PROCEDURE IF EXISTS sp_buscar_producto_por_nombre;
DROP PROCEDURE IF EXISTS sp_probar_restriccion;

-- Tablas: primero la de detalle (producto) y después la maestra (categoria)
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;

-- ---------------------------------------------------------------------
-- categoria (tabla maestra)
-- UNIQUE condicional: el nombre es único solo entre las categorías
-- ACTIVAS. Índice funcional: si eliminado = 0 la clave es el nombre;
-- si está eliminada la clave es NULL (un UNIQUE admite varios NULL).
-- Permite dar de baja lógica y volver a crear una categoría con el
-- mismo nombre. Requiere MySQL 8.0.13 o superior.
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    eliminado   BOOLEAN      NOT NULL,
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_nombre_activo ((IF(eliminado = 0, nombre, NULL)))
);

-- ---------------------------------------------------------------------
-- producto (tabla de detalle, N:1 con categoria)
-- CHECK de dominio: precio y stock no negativos.
-- La FK crea automáticamente un índice sobre categoria_id.
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255) NOT NULL,
    precio       DOUBLE       NOT NULL CHECK (precio >= 0),
    stock        INT          NOT NULL CHECK (stock >= 0),
    imagen       VARCHAR(255) NOT NULL,
    disponible   BOOLEAN      NOT NULL,
    eliminado    BOOLEAN      NOT NULL,
    created_at   DATETIME     NOT NULL,
    categoria_id BIGINT       NOT NULL,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

SHOW INDEX FROM categoria;
SHOW INDEX FROM producto;
