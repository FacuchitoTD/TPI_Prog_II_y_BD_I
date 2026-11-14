package integrado.prog2.dao;

import integrado.prog2.config.ConfiguracionPersistencia;
import integrado.prog2.exception.PersistenciaException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abre conexiones JDBC con los datos de persistence.xml y traduce las SQLException
 * a PersistenciaException con mensajes comprensibles.
 * Está en dao (y no en config) porque es la única capa autorizada a importar java.sql.
 */
public final class ConexionDB {

    private static final String SALTO = System.lineSeparator();

    private ConexionDB() {
    }

    /**
     * Abre una conexión nueva. Quien la pide es responsable de cerrarla (try-with-resources).
     *
     * @return conexión abierta con autocommit activado
     * @throws PersistenciaException si falta el driver o la base no responde
     */
    public static Connection getConexion() throws PersistenciaException {
        ConfiguracionPersistencia config = ConfiguracionPersistencia.getInstancia();
        try {
            Class.forName(config.getDriver());
            return DriverManager.getConnection(config.getUrl(), config.getUsuario(), config.getPassword());
        } catch (ClassNotFoundException e) {
            throw new PersistenciaException(armarDiagnostico(
                    "no se encontró el driver JDBC " + config.getDriver() + ".",
                    "Verificá que el .jar del driver JDBC (para MySQL: mysql-connector-j-8.3.0.jar) esté en la carpeta lib/.",
                    "Ejecutá el programa con el classpath completo: java -cp \"bin:lib/*\" integrado.prog2.Main"), e);
        } catch (SQLException e) {
            throw new PersistenciaException(diagnosticarConexion(e), e);
        }
    }

    /**
     * Abre y cierra una conexión para comprobar que la base responde.
     *
     * @throws PersistenciaException con la causa probable y los pasos a revisar
     */
    public static void probarConexion() throws PersistenciaException {
        try (Connection conexion = getConexion()) {
            if (!conexion.isValid(2)) {
                throw new PersistenciaException(armarDiagnostico(
                        "la conexión se abrió pero la base no responde.",
                        "Revisá que el servidor MySQL no esté sobrecargado o reiniciándose."));
            }
        } catch (SQLException e) {
            throw new PersistenciaException(diagnosticarConexion(e), e);
        }
    }

    /**
     * Convierte una SQLException ocurrida durante una operación en un mensaje para el usuario.
     *
     * @param e         excepción original del driver
     * @param operacion descripción breve de lo que se intentaba hacer, por ejemplo "crear la categoría"
     * @return excepción propia lista para lanzar
     */
    static PersistenciaException traducir(SQLException e, String operacion) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String mensaje;
        if (estado.startsWith("08")) {
            mensaje = diagnosticarConexion(e);
        } else if (e.getErrorCode() == 1062) {
            mensaje = "No se pudo " + operacion + ": ya existe un registro activo con ese valor único.";
        } else if (e.getErrorCode() == 1452) {
            mensaje = "No se pudo " + operacion + ": la categoría indicada no existe en la base.";
        } else if (e.getErrorCode() == 3819) {
            mensaje = "No se pudo " + operacion + ": la base rechazó un valor negativo (precio o stock).";
        } else if (e.getErrorCode() == 1142) {
            mensaje = "No se pudo " + operacion + ": el usuario de la base no tiene permiso para esta operación.";
        } else {
            mensaje = "No se pudo " + operacion + ". Detalle de la base: " + e.getMessage();
        }
        return new PersistenciaException(mensaje, e);
    }

    private static String diagnosticarConexion(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        if (estado.startsWith("08")) {
            return armarDiagnostico("no se pudo contactar al servidor de base de datos.",
                    "Verificá que MySQL esté iniciado (por ejemplo: systemctl status mysql).",
                    "Revisá host y puerto de jakarta.persistence.jdbc.url en src/META-INF/persistence.xml.");
        }
        if (e.getErrorCode() == 1045) {
            return armarDiagnostico("usuario o contraseña incorrectos.",
                    "Revisá jakarta.persistence.jdbc.user y jakarta.persistence.jdbc.password en persistence.xml.",
                    "Si la base es nueva, ejecutá db/01_esquema.sql: crea el usuario de la aplicación.");
        }
        if (e.getErrorCode() == 1049 || e.getErrorCode() == 1044) {
            return armarDiagnostico("la base de datos de la URL no existe o el usuario no tiene acceso.",
                    "Ejecutá db/01_esquema.sql como root para crear la base, las tablas y los permisos.");
        }
        return armarDiagnostico("error de la base de datos (" + e.getMessage() + ").",
                "Revisá los datos de conexión en src/META-INF/persistence.xml.");
    }

    private static String armarDiagnostico(String causa, String... pasos) {
        StringBuilder texto = new StringBuilder("Causa probable: ").append(causa);
        texto.append(SALTO).append("Pasos a revisar:");
        for (String paso : pasos) {
            texto.append(SALTO).append("  - ").append(paso);
        }
        return texto.toString();
    }
}
