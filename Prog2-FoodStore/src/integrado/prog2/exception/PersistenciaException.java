package integrado.prog2.exception;

/**
 * Error al acceder a la configuración o a la base de datos.
 * Envuelve las SQLException para que ninguna capa fuera de dao dependa de java.sql.
 */
public class PersistenciaException extends FoodStoreException {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
