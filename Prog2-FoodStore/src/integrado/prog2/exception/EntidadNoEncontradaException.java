package integrado.prog2.exception;

/**
 * Se lanza cuando se busca por id una entidad que no existe o que fue dada de baja lógica.
 */
public class EntidadNoEncontradaException extends FoodStoreException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
