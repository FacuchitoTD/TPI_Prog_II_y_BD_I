-- Food Store - Programación 2 (Consola + JDBC)
-- Crea la base, las dos únicas tablas del modelo relacional (categoria y producto)
-- y el usuario de la aplicación con permisos mínimos (sin DELETE: las bajas son lógicas).
-- Ejecutar como root:  mysql -u root -p < db/01_esquema.sql
-- Requiere MySQL 8.0.16 o superior (CHECK) y 8.0.13 o superior (índice funcional).
-- Es seguro ejecutarlo más de una vez: no borra datos existentes.

CREATE DATABASE IF NOT EXISTS FoodStore
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE FoodStore;

CREATE TABLE IF NOT EXISTS categoria (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    eliminado   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_nombre_activo ((IF(eliminado = FALSE, nombre, NULL)))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS producto (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255) NOT NULL,
    precio       DOUBLE       NOT NULL,
    stock        INT          NOT NULL,
    imagen       VARCHAR(255) NOT NULL,
    disponible   BOOLEAN      NOT NULL DEFAULT TRUE,
    eliminado    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    categoria_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY categoria_id (categoria_id),
    CONSTRAINT producto_ibfk_1 FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT producto_chk_1 CHECK (precio >= 0),
    CONSTRAINT producto_chk_2 CHECK (stock >= 0)
) ENGINE = InnoDB;

CREATE USER IF NOT EXISTS 'foodstore_app'@'%' IDENTIFIED BY 'PasswordSeguro123!';
GRANT SELECT, INSERT, UPDATE ON FoodStore.categoria TO 'foodstore_app'@'%';
GRANT SELECT, INSERT, UPDATE ON FoodStore.producto  TO 'foodstore_app'@'%';
FLUSH PRIVILEGES;
