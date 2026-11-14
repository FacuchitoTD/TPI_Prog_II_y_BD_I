# Food Store — Sistema de gestión de pedidos (Consola + JDBC)

Trabajo Práctico Integrador de **Programación 2** — Tecnicatura Universitaria en Programación a Distancia (UTN).
Alumno: **Facundo Exequiel López** — Comisión 5.

Aplicación de consola en Java que gestiona categorías, productos, usuarios y pedidos con **almacenamiento mixto**:

| Entidad        | Dónde se guarda | Cómo |
|----------------|-----------------|------|
| Categoria      | BASE DE DATOS   | Tabla `categoria`, CRUD por JDBC (`CategoriaDAO`) |
| Producto       | BASE DE DATOS   | Tabla `producto` con FK `categoria_id`, CRUD por JDBC (`ProductoDAO`) |
| Usuario        | MEMORIA         | `ArrayList` en `UsuarioRepository` (se pierde al cerrar) |
| Pedido         | MEMORIA         | Dentro de cada `Usuario` (asociación unidireccional) |
| DetallePedido  | MEMORIA         | Composición dentro de `Pedido` |

El stock de los productos vive en la base: al confirmar un pedido se descuenta en **una única transacción**
(`setAutoCommit(false)` + `commit`, o `rollback` si algún producto no alcanza).

## Enlaces

- Video demostrativo: **(completar con el enlace público)**
- Documentación PDF: **(completar con el enlace o subir el PDF a la raíz del repositorio)**

## Requisitos

- Java 21 o superior (`javac` y `java` en el PATH). No se usa Maven ni Gradle.
- MySQL 8.0.16 o superior.
- Driver JDBC **MySQL Connector/J 8.3.0** (`mysql-connector-j-8.3.0.jar`) en la carpeta `lib/`.
  Ya está incluido en el repositorio; si falta, se descarga de <https://dev.mysql.com/downloads/connector/j/>
  (opción *Platform Independent*) y se copia el `.jar` a `lib/`.

## 1. Crear la base de datos

Desde la carpeta del proyecto, con un usuario administrador de MySQL:

```bash
mysql -u root -p < db/01_esquema.sql
mysql -u root -p FoodStore < db/02_datos_ejemplo.sql
```

- `01_esquema.sql` crea la base `FoodStore`, las tablas `categoria` y `producto` (PK, FK, CHECK de precio y stock,
  nombre de categoría único entre las activas) y el usuario `foodstore_app` con permisos `SELECT, INSERT, UPDATE`.
  No se le otorga `DELETE` porque todas las bajas son lógicas.
- `02_datos_ejemplo.sql` es opcional: carga categorías y productos para probar el sistema.

## 2. Configurar la conexión

Los datos de conexión están en `src/META-INF/persistence.xml` (propiedades estándar `jakarta.persistence.jdbc.*`).
El programa los lee con el parser DOM del JDK; ninguna clase tiene la cadena de conexión escrita en el código.

```xml
<persistence-unit name="foodstorePU-mysql">
    <properties>
        <property name="jakarta.persistence.jdbc.driver" value="com.mysql.cj.jdbc.Driver"/>
        <property name="jakarta.persistence.jdbc.url" value="jdbc:mysql://127.0.0.1:3306/FoodStore"/>
        <property name="jakarta.persistence.jdbc.user" value="foodstore_app"/>
        <property name="jakarta.persistence.jdbc.password" value="PasswordSeguro123!"/>
    </properties>
</persistence-unit>
```

## 3. Compilar y ejecutar

Linux / macOS:

```bash
javac -encoding UTF-8 -d bin $(find src -name "*.java")
cp -r src/META-INF bin/META-INF
java -cp "bin:lib/*" integrado.prog2.Main
```

Windows (PowerShell): el separador del classpath es `;`

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
Copy-Item -Recurse src\META-INF bin\META-INF
java -cp "bin;lib/*" integrado.prog2.Main
```

Al arrancar, el programa prueba la conexión e informa el resultado. Si falla (driver ausente, MySQL apagado,
base inexistente, credenciales incorrectas) muestra la causa probable y los pasos a revisar, y sigue funcionando
con las opciones en memoria.

## Estructura del proyecto

```
db/                       scripts SQL (esquema y datos de ejemplo)
lib/                      driver JDBC
src/
 ├── META-INF/persistence.xml
 └── integrado/prog2/
     ├── Main.java        arma las capas y arranca el menú
     ├── config/          lectura de persistence.xml (DOM)
     ├── entities/        Base, Categoria, Producto, Usuario, Pedido, DetallePedido
     ├── enums/           Rol, Estado, FormaPago
     ├── interfaces/      Calculable
     ├── exception/       FoodStoreException y sus subclases
     ├── dao/             ConexionDB, GenericDAO, CategoriaDAO, ProductoDAO (único paquete con java.sql)
     ├── repository/      UsuarioRepository, PedidoRepository (colecciones en memoria)
     ├── service/         reglas de negocio y casos de uso
     └── ui/              Consola, Submenu y menús
```

## Decisiones de diseño principales

- **Relaciones unidireccionales del UML**: `Producto` no tiene atributo de categoría y `Pedido` no tiene atributo de
  usuario. El id de la categoría viaja como parámetro (`insert(Producto, Long idCategoria)`,
  `reasignarCategoria(idProducto, idCategoria)`) y la navegación es siempre Categoría → Productos
  (`SELECT ... WHERE categoria_id = ?`). Los pedidos se recorren desde Usuario → Pedidos.
- **Baja lógica en todo el sistema**: `UPDATE ... SET eliminado = TRUE` en la base y `eliminado = true` en memoria.
- **Unicidad**: el nombre de categoría se valida con una consulta SQL; el mail de usuario, recorriendo la colección.
- **Excepciones propias**: `PersistenciaException`, `EntidadNoEncontradaException`, `StockInvalidoException`,
  `ValidacionException` y `OperacionNoPermitidaException`, todas hijas de `FoodStoreException`.
  Toda `SQLException` se traduce dentro de la capa `dao`.
- **Consistencia memoria ↔ base**: cada operación de pedido primero mueve el stock en la base (transacción) y recién
  si salió bien modifica la memoria. Cancelar o eliminar un pedido vigente repone el stock.
