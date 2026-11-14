# Guion del video — TPI Programación 2: Food Store (Consola + JDBC)

Duración objetivo: **unos 14:45** (la pauta pide de 10 a 15; si te pasás de 15 baja la nota).
Margen: si en el ensayo te pasás, salteá el paso 9 del bloque 7 (gestionar detalles), que ahorra cerca de un minuto.
Formato de cada bloque: **Pantalla** = qué mostrar · **Decir** = texto sugerido (adaptalo a tus palabras).

La pauta evalúa cuatro cosas del video: recorrer el flujo completo, explicar el diseño (capas, DAO,
direccionalidad del UML), la prueba de persistencia cerrando y reabriendo, y que usuarios y pedidos se pierden.

---

## Antes de grabar (no va en el video)

1. Cargar los datos de ejemplo (una sola vez, como root):
   `mysql -u root -p FoodStore < db/02_datos_ejemplo.sql`
2. Compilar en limpio desde `Prog2-FoodStore/`, con estos tres comandos en orden:
    - `rm -rf bin`
    - `javac -encoding UTF-8 -d bin $(find src -name "*.java")`
    - `cp -r src/META-INF bin/META-INF`
3. Tener abiertos en el IDE: `Producto.java`, `Pedido.java`, `GenericDAO.java`, `ProductoDAO.java`
   (método `descontarStock`), `persistence.xml` y el árbol de paquetes.
4. Anotar los ids de los productos de ejemplo: en el programa, **Productos → 2) Listar los productos de una
   categoría** con Pizzas y con Hamburguesas. Como la base tiene los 10.000 productos de BD I, los nuevos
   quedan al final del listado general: en la demo cortá el listado con `0` y escribí el id directamente.
5. Terminal con letra grande y una sola ventana a la vista.

---

## 1. Presentación — 0:00 a 0:45

**Pantalla:** tu cámara o la carátula del informe.

**Decir:**
"Hola, soy Facundo López, de la comisión 5. Les presento Food Store, el TPI de Programación 2: un sistema de
consola en Java para gestionar categorías, productos, usuarios y pedidos. La clave del trabajo es el
almacenamiento mixto: categorías y productos se guardan en MySQL usando JDBC puro, mientras que usuarios,
pedidos y detalles viven en memoria, en colecciones, y se pierden al cerrar el programa. Primero les muestro
cómo está organizado el código y después recorro el sistema funcionando."

---

## 2. Arquitectura y modelo — 0:45 a 3:30

**Pantalla:** árbol de paquetes de `src/integrado/prog2`.

**Decir:**
"El proyecto está dividido en capas. `entities` tiene las clases del UML; `dao` es la única capa que tiene
SQL e importa `java.sql`; `repository` maneja las colecciones en memoria; `service` tiene las reglas de
negocio y decide si un dato va a la base o a memoria; y `ui` sólo interactúa con el usuario. El `Main` no
tiene SQL ni reglas: arma los objetos de cada capa, los conecta y arranca el menú."

**Pantalla:** `Base.java` y después `Producto.java`.

**Decir:**
"Todas las entidades heredan de la clase abstracta `Base`, que tiene id, eliminado y createdAt. Ahí también
sobrescribí `equals` y `hashCode`: dos entidades son iguales si son de la misma clase y tienen el mismo id.
Respeté la direccionalidad del UML: `Producto` no tiene ningún atributo de categoría. La categoría conoce sus
productos, pero el producto no conoce su categoría. La columna `categoria_id` existe sólo en la base, y el id
de la categoría viaja como parámetro en `insert(Producto, Long idCategoria)` y en `reasignarCategoria`. Lo
mismo pasa con `Pedido`: no tiene un atributo usuario; es el usuario el que tiene la lista de pedidos."

**Pantalla:** `Pedido.java`, desplazándote por los tres métodos y `calcularTotal`.

**Decir:**
"`Pedido` implementa la interfaz `Calculable`: `calcularTotal()` suma los subtotales de las líneas activas y
guarda el total. Además tiene los tres métodos obligatorios: `addDetallePedido`, que si el producto ya está en
el pedido le suma la cantidad a esa línea; `findeDetallePedidoByProducto`, que busca usando el `equals` por id;
y `deleteDetallePedidoByProducto`, que hace una baja lógica de la línea. Es composición: el pedido crea y
administra sus detalles."

**Pantalla:** `GenericDAO.java`.

**Decir:**
"Los DAO implementan una interfaz genérica, `GenericDAO<T extends Base>`, con listar, buscar por id,
actualizar y baja lógica. El alta no está en la interfaz porque no es igual para las dos entidades: el producto
necesita además el id de su categoría. Todos los DAO usan `PreparedStatement` con parámetros y
try-with-resources para cerrar conexión, sentencia y resultados, y traducen las `SQLException` a excepciones
propias."

**Pantalla:** `persistence.xml` y un vistazo a `ConfiguracionPersistencia.java`.

**Decir:**
"Los datos de conexión no están en el código: están en `META-INF/persistence.xml`, con las propiedades estándar
de Jakarta Persistence, y los leo con el parser DOM que trae el JDK, sin librerías externas."

---

## 3. Compilación, arranque y configuración — 3:30 a 4:30

**Pantalla:** terminal. Ejecutar:
`java -cp "bin:lib/*" integrado.prog2.Main`

**Decir:**
"Se compila con `javac` y se ejecuta con `java`, sin Maven ni Gradle; el driver de MySQL está en `lib`. Al
arrancar, el programa prueba la conexión y lo informa. Si faltara el driver o la base estuviera apagada,
mostraría la causa probable y qué revisar, sin cortarse."

**Pantalla:** menú principal → opción **5**.

**Decir:**
"Cada opción del menú indica dónde se guardan los datos: base de datos o memoria. La opción 5 muestra la
unidad de persistencia activa y la URL JDBC."

*(Opcional, si te sobra tiempo al final: ejecutá `java -cp bin integrado.prog2.Main`, sin el driver, para
mostrar el mensaje de diagnóstico.)*

---

## 4. Categorías [BASE DE DATOS] — 4:30 a 6:00

**Pantalla y acciones:**
1. `1` Categorías → `1) Listar`.
2. `2) Crear`: nombre **Empanadas**, descripción **Empanadas al horno** → muestra el id generado.
3. `2) Crear` otra vez con **Empanadas** → error de nombre duplicado.
4. `3) Editar`: lista primero, elegir Empanadas, dejar el nombre vacío (Enter) y cambiar la descripción.
5. `4) Eliminar`: elegir **Pizzas** → confirmar S → error porque tiene productos activos.

**Decir (mientras lo hacés):**
"Al crear, el id lo genera la base y lo recupero con `getGeneratedKeys`. Antes de insertar, verifico con una
consulta SQL que no haya otra categoría activa con el mismo nombre. Al editar, los campos vacíos conservan su
valor. Y la baja es lógica, un UPDATE de eliminado a TRUE; además no me deja dar de baja una categoría que
todavía tiene productos activos, y me dice cuántos."

---

## 5. Productos [BASE DE DATOS] — 6:00 a 8:00

**Pantalla y acciones:**
1. `2` Productos → `3) Crear`: **Empanada de carne**, precio **-100** … categoría Empanadas → error de precio
   negativo.
2. `3) Crear` de nuevo, bien: **Empanada de carne**, descripción, **1200**, stock **50**, imagen, disponible S,
   categoría Empanadas → id generado.
3. `2) Listar los productos de una categoría` → Empanadas.
4. `4) Editar`: cortar el listado con `0`, escribir el id de la empanada y cambiar sólo el precio.
5. `5) Mover a otra categoría`: no hace falta mostrarlo entero, alcanza con nombrarlo.

**Decir:**
"Las validaciones están en el servicio: precio o stock negativos lanzan una excepción propia y no se inserta
nada. El listado general no muestra la categoría, porque el producto no la conoce; para verlos agrupados,
listo los productos de una categoría: hago un SELECT filtrando por `categoria_id` y el resultado se carga en
la lista de la categoría, siempre en el sentido Categoría → Productos. El UPDATE de edición no toca la clave
foránea: mover un producto de categoría es una operación aparte que sólo cambia `categoria_id`. Como la base
tiene diez mil productos de la materia Bases de Datos, los listados se muestran de a veinte."

---

## 6. Usuarios [MEMORIA] — 8:00 a 9:00

**Pantalla y acciones:**
1. `3` Usuarios → `2) Crear` con mail **ana@** → error de formato.
2. `2) Crear` bien: **Ana Gómez, ana@mail.com**, rol USUARIO → id 1.
3. `2) Crear` otro usuario con **ANA@mail.com** → error de mail repetido.
4. `1) Listar`.

**Decir:**
"Los usuarios no van a la base: están en un ArrayList dentro del repositorio, y el id lo genera el propio
programa con un contador. La unicidad del mail se valida recorriendo la colección; al editar, se excluye al
propio usuario. La baja también es lógica: el usuario queda en la lista marcado como eliminado, para conservar
el historial de sus pedidos."

---

## 7. Pedidos y detalles [MEMORIA + stock en BASE] — 9:00 a 12:30

**Pantalla y acciones (crear):**
1. `4` Pedidos → `3) Crear pedido con detalles` → usuario Ana → forma de pago TRANSFERENCIA.
2. `1) Agregar producto`: **Flan casero** → error: no disponible.
3. `1) Agregar producto`: **Hamburguesa doble**, cantidad **5** → error: stock insuficiente (hay 3).
4. `1) Agregar producto`: Hamburguesa doble, cantidad **2**.
5. `1) Agregar producto`: Empanada de carne, cantidad **6**.
6. `3) Ver resumen` → `4) Confirmar` → S.

**Decir:**
"El pedido se arma en memoria. Cada línea se agrega con `addDetallePedido` y el subtotal es cantidad por
precio. Antes de agregar, leo el producto de la base para validar que esté disponible y que alcance el stock.
El total lo calcula `calcularTotal()` de la interfaz `Calculable`."

**Pantalla:** `ProductoDAO.java`, método `descontarStock`.

**Decir:**
"Este es el punto de contacto entre memoria y base. Al confirmar, el stock de todas las líneas se descuenta en
una única transacción: `setAutoCommit(false)`, un UPDATE por línea y `commit`. Cada UPDATE sólo descuenta si
el producto todavía tiene stock suficiente; si alguno no alcanza, se hace `rollback` de todo, se lanza
`StockInvalidoException` y el pedido no se registra en memoria. El orden es siempre primero la base y después
la memoria, así ningún error deja los dos lugares desincronizados."

**Pantalla y acciones (verificar y modificar):**
7. Volver al menú principal → `2` Productos → listar Hamburguesas → el stock bajó de 3 a 1.
8. `4` Pedidos → `1) Listar pedidos` (se ve agrupado por usuario).
9. `5) Gestionar detalles` → pedido 1:
   - `2) Buscar línea por producto` → id de la empanada (usa `findeDetallePedidoByProducto`).
   - `4) Modificar cantidad` de empanadas a **4**.
   - `5) Quitar producto` → hamburguesa → S (usa `deleteDetallePedidoByProducto` y repone stock).
10. `4) Actualizar estado` → **CANCELADO** → S → se repone el stock.
11. `4) Actualizar estado` otra vez → error: el pedido cancelado no admite cambios.

**Decir:**
"Los pedidos se listan recorriendo los usuarios, porque la relación Usuario → Pedido es unidireccional. Desde
gestionar detalles se completa el ABM de las líneas, y cada cambio ajusta el stock en la base. Al cancelar,
se repone el stock de todo el pedido; y un pedido terminado o cancelado ya no se puede modificar."

*(Si vas justo de tiempo, salteá el paso 9 y quedate con el 10 y el 11.)*

---

## 8. Prueba de persistencia — 12:30 a 13:45 (obligatoria)

**Pantalla y acciones:**
1. Menú principal → `0) Salir`. Se ve el mensaje de que la memoria se descarta.
2. Volver a ejecutar `java -cp "bin:lib/*" integrado.prog2.Main`.
3. `1` Categorías → Listar → **Empanadas** sigue ahí.
4. `2` Productos → listar los productos de Empanadas → **Empanada de carne** sigue ahí.
5. `3` Usuarios → Listar → "No hay usuarios cargados en memoria".
6. `4` Pedidos → Listar → "No hay pedidos cargados en memoria".

**Decir:**
"Cerré el programa y lo volví a abrir. La categoría y el producto que cargué siguen estando, porque se guardan
en MySQL. En cambio, Ana y su pedido desaparecieron, porque vivían en memoria. Esa es justamente la
diferencia entre los dos almacenamientos que pedía el trabajo."

---

## 9. Tres decisiones de diseño — 13:45 a 14:15

**Pantalla:** el árbol de paquetes, con `dao/ConexionDB.java` y `exception/` a la vista; después `db/01_esquema.sql`
(las líneas `GRANT`).

**Decir:**
"Para terminar, tres decisiones que tomé. Primero, `ConexionDB` está en el paquete `dao` y no en `config`,
porque `dao` es la única capa que puede importar `java.sql`; `config` sólo lee el XML. Segundo, todas mis
excepciones heredan de `FoodStoreException`, así cada menú las captura con un solo catch y muestra el mensaje
sin que el programa se corte. Y tercero, el usuario de la base que usa la aplicación tiene permisos de SELECT,
INSERT y UPDATE, pero no de DELETE: como todas las bajas son lógicas, no hace falta, y así ni por error se
puede borrar un registro físicamente."

---

## 10. Cierre — 14:15 a 14:45

**Decir:**
"Resumiendo: el sistema respeta el UML y su direccionalidad, usa JDBC con consultas parametrizadas y baja
lógica, maneja todos los errores con excepciones propias sin cortarse, y resuelve el descuento de stock con
una transacción. El código, el script SQL y el informe están en el repositorio cuyo enlace figura en el README.
Muchas gracias."
