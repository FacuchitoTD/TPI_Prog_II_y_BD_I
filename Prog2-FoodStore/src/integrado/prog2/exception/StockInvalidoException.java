package integrado.prog2.exception;

/**
 * Se lanza ante un stock negativo o cuando la cantidad pedida supera el stock disponible.
 */
public class StockInvalidoException extends FoodStoreException {

    public StockInvalidoException(String mensaje) {
        super(mensaje);
    }
}
