TFI Bases de Datos I - Food Store
=================================
Alumno: Facundo Exequiel López
Motor: MySQL 8.0.46 (InnoDB) - requiere MySQL 8.0.18 o superior
       (índice funcional: 8.0.13; EXPLAIN ANALYZE: 8.0.18)
Versión de los scripts: 1.0 - 07/11/2026

ORDEN DE EJECUCIÓN
------------------
Ejecutar como root, en este orden:

  01_esquema.sql              Etapa 1  Base FoodStore, tablas categoria y producto
                                       (PK, FK, UNIQUE condicional, CHECK)
  02_catalogos.sql            Etapa 2  Tabla semilla: 5 categorías
  03_carga_masiva.sql         Etapa 2  10.000 productos con WITH RECURSIVE
                                       + verificaciones de consistencia
  04_indices.sql              Etapa 3  idx_producto_precio (el índice de la FK
                                       categoria_id lo crea InnoDB)
  05_consultas.sql            Etapa 3  4 consultas avanzadas
  05_explain.sql              Etapa 3  EXPLAIN ANALYZE con/sin índice
                                       (igualdad, rango, JOIN)
  06_vistas.sql               Etapas 3 y 4  vista útil y vistas de seguridad
  07_seguridad.sql            Etapa 4  usuario con privilegios mínimos,
                                       anti SQL Injection, pruebas PK/FK/UNIQUE/CHECK
  08_transacciones.sql        Etapa 5  COMMIT / ROLLBACK y referencia al retry (Java)
  09_concurrencia_guiada.sql  Etapa 5  GUION MANUAL para dos sesiones (deadlock y
                                       niveles de aislamiento). No ejecutar de corrido.

Ejemplo desde la terminal (scripts 01 a 08):
  for f in 0[1-8]_*.sql; do mysql -u root -p FoodStore < "$f"; done
(01_esquema.sql crea la base, así que puede ejecutarse sin base seleccionada:
  mysql -u root -p < 01_esquema.sql)

IDEMPOTENCIA
------------
- 01 usa DROP ... IF EXISTS y vuelve a crear base, tablas, vistas y procedimientos.
- 02 y 03 vacían los datos que cargan antes de volver a cargarlos.
- 04 crea el índice solo si no existe (MySQL 8 no admite CREATE INDEX IF NOT EXISTS).
- 06 usa CREATE OR REPLACE VIEW; 07 usa DROP USER/PROCEDURE IF EXISTS y deshace
  sus pruebas con ROLLBACK; 08 compensa los cambios de stock.
- Ejecutar 01 borra todos los datos y los vuelve a crear desde cero.

RETRY ANTE DEADLOCK (Java)
--------------------------
Etapa5-Java/src/RetryDeadlockDemo.java (JDBC puro, driver en lib/):
  javac -d out src/RetryDeadlockDemo.java
  java -cp "out:lib/mysql-connector-j-8.3.0.jar" RetryDeadlockDemo 2 3
