import java.io.Console;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Etapa 5 (BD I) - Transacción con retry ante deadlock (JDBC puro).
 *
 * Escenario: dos "pedidos" que se confirman al mismo tiempo y descuentan
 * stock de los mismos dos productos (P1 y P2), pero en ORDEN CRUZADO:
 *   - Pedido A: descuenta P1 y después P2.
 *   - Pedido B: descuenta P2 y después P1.
 * Es el mismo cruce que la simulación manual del deadlock, pero hecho desde
 * Java con dos hilos y dos conexiones distintas.
 *
 * MySQL elige una víctima y le devuelve ERROR 1213 (SQLSTATE 40001). La
 * víctima hace ROLLBACK, espera un tiempo breve (backoff) y reintenta la
 * transacción completa, hasta 2 reintentos. Cada intento queda registrado
 * en consola y en el archivo etapa5-retry.log (logging).
 *
 * Compilar y ejecutar desde la carpeta Etapa5-Java:
 *   javac -d out src/RetryDeadlockDemo.java
 *   java -cp "out:lib/mysql-connector-j-8.3.0.jar" RetryDeadlockDemo <idP1> <idP2>
 * (en Windows, separar el classpath con ';' en lugar de ':')
 * Los dos ids deben ser productos activos (eliminado = FALSE) con stock.
 *
 * La contraseña de MySQL se toma de la variable de entorno DB_PASSWORD
 * o se pide por consola (no queda escrita en el código).
 */
public class RetryDeadlockDemo {

    // ---------------- Configuración ----------------
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/FoodStore";
    private static final String USUARIO = "root";

    /** Reintentos permitidos DESPUÉS del primer intento (consigna: hasta 2). */
    private static final int MAX_REINTENTOS = 2;
    /** Backoff base en ms: 1er reintento espera 100 ms, 2do espera 200 ms. */
    private static final long BACKOFF_BASE_MS = 100;
    /** Pausa entre el primer y el segundo UPDATE, para forzar el cruce. */
    private static final long PAUSA_ENTRE_UPDATES_MS = 500;

    /** Códigos que identifican un deadlock en MySQL. */
    private static final int ERROR_DEADLOCK = 1213;
    private static final String SQLSTATE_DEADLOCK = "40001";

    private static final Logger LOG = Logger.getLogger("Etapa5");

    private static String password;

    // ---------------- Programa principal ----------------
    public static void main(String[] args) throws Exception {
        // Ids de dos productos ACTIVOS (eliminado = FALSE) con stock suficiente.
        // Se pueden pasar por parámetro: java ... RetryDeadlockDemo 2 3
        if (args.length != 2) {
            System.out.println("Uso: java -cp \"out:lib/mysql-connector-j-8.3.0.jar\" "
                    + "RetryDeadlockDemo <idProducto1> <idProducto2>");
            System.out.println("Usar dos productos activos (eliminado = FALSE) con stock.");
            return;
        }
        long idProducto1 = Long.parseLong(args[0]);
        long idProducto2 = Long.parseLong(args[1]);
        int cantidad = 1;

        configurarLogging();
        password = obtenerPassword();

        mostrarStock("Stock ANTES", idProducto1, idProducto2);

        // El latch hace que los dos pedidos arranquen exactamente a la vez.
        CountDownLatch largada = new CountDownLatch(1);

        Thread pedidoA = new Thread(() ->
                confirmarPedidoConRetry("Pedido-A", largada, idProducto1, idProducto2, cantidad));
        Thread pedidoB = new Thread(() ->
                confirmarPedidoConRetry("Pedido-B", largada, idProducto2, idProducto1, cantidad));

        pedidoA.start();
        pedidoB.start();
        largada.countDown();   // ¡largan los dos!
        pedidoA.join();
        pedidoB.join();

        mostrarStock("Stock DESPUÉS", idProducto1, idProducto2);
        LOG.info("Fin de la prueba. Ver detalle en etapa5-retry.log");
    }

    // ---------------- Lógica de retry ----------------

    /**
     * Ejecuta la transacción del pedido y la reintenta si MySQL la elige
     * como víctima de un deadlock. Los errores que no son deadlock (por
     * ejemplo, stock insuficiente) NO se reintentan.
     */
    private static void confirmarPedidoConRetry(String nombre, CountDownLatch largada,long primerProducto, long segundoProducto, int cantidad) {
        try {
            largada.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        int intento = 1;
        while (true) {
            try {
                LOG.info(nombre + " | intento " + intento + " | inicia la transacción");
                descontarStockEnTransaccion(nombre, primerProducto, segundoProducto, cantidad);
                LOG.info(nombre + " | intento " + intento + " | COMMIT exitoso");
                return;

            } catch (SQLException e) {
                if (esDeadlock(e) && intento <= MAX_REINTENTOS) {
                    long espera = BACKOFF_BASE_MS * (1L << (intento - 1)); // 100, 200 ms
                    LOG.warning(nombre + " | intento " + intento + " | DEADLOCK (error "
                            + e.getErrorCode() + ", SQLSTATE " + e.getSQLState()
                            + "): se hizo ROLLBACK, reintenta en " + espera + " ms");
                    dormir(espera);
                    intento++;
                } else if (esDeadlock(e)) {
                    LOG.severe(nombre + " | DEADLOCK y se agotaron los " + MAX_REINTENTOS
                            + " reintentos. El pedido NO se confirmó.");
                    return;
                } else {
                    LOG.log(Level.SEVERE, nombre + " | error no recuperable (no se reintenta): "
                            + e.getMessage(), e);
                    return;
                }
            }
        }
    }

    /** Un deadlock se reconoce por el código de MySQL o por el SQLSTATE estándar. */
    private static boolean esDeadlock(SQLException e) {
        return e.getErrorCode() == ERROR_DEADLOCK || SQLSTATE_DEADLOCK.equals(e.getSQLState());
    }

    // ---------------- Transacción ----------------

    /**
     * Una transacción completa: descuenta stock de dos productos.
     * Si algo falla, hace ROLLBACK y relanza la excepción para que el
     * método de retry decida qué hacer.
     */
    private static void descontarStockEnTransaccion(String nombre, long primerProducto,
                                                    long segundoProducto, int cantidad)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(URL, USUARIO, password)) {
            conn.setAutoCommit(false);   // equivale a START TRANSACTION
            try {
                descontar(conn, nombre, primerProducto, cantidad);
                dormir(PAUSA_ENTRE_UPDATES_MS);  // da tiempo a que el otro pedido tome su fila
                descontar(conn, nombre, segundoProducto, cantidad);

                conn.commit();           // COMMIT
            } catch (SQLException e) {
                // ROLLBACK siempre: si MySQL ya deshizo la transacción víctima,
                // igual dejamos la conexión en un estado limpio.
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOG.warning(nombre + " | falló el rollback: " + ex.getMessage());
                }
                throw e;
            }
        }
    }

    /**
     * UPDATE parametrizado (PreparedStatement, sin concatenar valores).
     * La condición stock >= ? evita stock negativo; si no se actualiza
     * ninguna fila, es un error de negocio y NO se reintenta.
     */
    private static void descontar(Connection conn, String nombre, long idProducto, int cantidad)
            throws SQLException {
        String sql = "UPDATE producto SET stock = stock - ? "
                   + "WHERE id = ? AND eliminado = FALSE AND stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setLong(2, idProducto);
            ps.setInt(3, cantidad);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException("Stock insuficiente o producto inexistente (id "
                        + idProducto + ")", "45000");
            }
            LOG.info(nombre + " | bloqueó y descontó " + cantidad + " del producto id " + idProducto);
        }
    }

    // ---------------- Utilidades ----------------

    private static void mostrarStock(String titulo, long id1, long id2) throws SQLException {
        String sql = "SELECT id, nombre, stock FROM producto WHERE id IN (?, ?) ORDER BY id";
        try (Connection conn = DriverManager.getConnection(URL, USUARIO, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id1);
            ps.setLong(2, id2);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder(titulo + ":");
                while (rs.next()) {
                    sb.append(" [id ").append(rs.getLong("id"))
                      .append(" - ").append(rs.getString("nombre"))
                      .append(" - stock ").append(rs.getInt("stock")).append("]");
                }
                LOG.info(sb.toString());
            }
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void configurarLogging() throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT.%1$tL [%4$s] %5$s%6$s%n");
        FileHandler archivo = new FileHandler("etapa5-retry.log", true); // true = agrega al final
        archivo.setFormatter(new SimpleFormatter());
        LOG.addHandler(archivo);
    }

    private static String obtenerPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null) {
            return env;
        }
        Console consola = System.console();
        if (consola != null) {
            return new String(consola.readPassword("Contraseña de MySQL (%s): ", USUARIO));
        }
        System.out.print("Contraseña de MySQL (" + USUARIO + "): ");
        return new Scanner(System.in).nextLine();
    }
}
