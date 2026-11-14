package integrado.prog2.exception;

/**
 * Se lanza cuando una operación es válida en formato pero la impide una regla de negocio,
 * por ejemplo dar de baja una categoría con productos activos o modificar un pedido terminado.
 */
public class OperacionNoPermitidaException extends FoodStoreException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
