package integrado.prog2.exception;

/**
 * Raíz de las excepciones propias del sistema.
 * La UI captura este tipo para mostrar cualquier error de negocio o de persistencia
 * sin conocer cada subclase en particular.
 */
public abstract class FoodStoreException extends Exception {

    protected FoodStoreException(String mensaje) {
        super(mensaje);
    }

    protected FoodStoreException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
