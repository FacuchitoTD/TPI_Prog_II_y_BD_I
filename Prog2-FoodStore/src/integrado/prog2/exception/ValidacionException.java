package integrado.prog2.exception;

/**
 * Se lanza cuando un dato ingresado no cumple el formato o las reglas mínimas
 * (campo vacío, precio negativo, mail mal formado o repetido, nombre de categoría duplicado).
 */
public class ValidacionException extends FoodStoreException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
