package integrado.prog2.service;

import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.exception.ValidacionException;

/**
 * Validaciones de datos compartidas por los servicios.
 */
final class Validaciones {

    private static final String FORMATO_MAIL = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private Validaciones() {
    }

    static String textoObligatorio(String valor, String campo) throws ValidacionException {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("El campo '" + campo + "' no puede estar vacío.");
        }
        return valor.trim();
    }

    static String textoOpcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    static boolean seIngreso(String valor) {
        return valor != null && !valor.isBlank();
    }

    static void precioValido(Double precio) throws ValidacionException {
        if (precio == null || precio < 0) {
            throw new ValidacionException("El precio no puede ser negativo.");
        }
    }

    static void stockValido(int stock) throws StockInvalidoException {
        if (stock < 0) {
            throw new StockInvalidoException("El stock no puede ser negativo.");
        }
    }

    static void cantidadValida(int cantidad) throws StockInvalidoException {
        if (cantidad <= 0) {
            throw new StockInvalidoException("La cantidad debe ser mayor a cero.");
        }
    }

    static String mailValido(String mail) throws ValidacionException {
        String limpio = textoObligatorio(mail, "mail");
        if (!limpio.matches(FORMATO_MAIL)) {
            throw new ValidacionException("El mail '" + limpio + "' no tiene un formato válido (ejemplo: nombre@dominio.com).");
        }
        return limpio;
    }
}
