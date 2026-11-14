package integrado.prog2.ui;

import java.util.List;
import java.util.Scanner;

/**
 * Lectura validada de datos por teclado y salida de mensajes.
 * Ningún método deja escapar una NumberFormatException: ante un dato mal ingresado vuelve a preguntar.
 */
public class Consola {

    private static final int TAMANIO_PAGINA = 20;

    private final Scanner scanner;

    public Consola(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Lee una línea sin validar. Si se cierra la entrada estándar, termina el programa de forma ordenada.
     */
    public String leerLinea(String mensaje) {
        System.out.print(mensaje);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("Se cerró la entrada de datos. Fin del programa.");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /**
     * Lee un texto obligatorio: insiste hasta que no esté vacío.
     */
    public String leerTexto(String mensaje) {
        String texto = leerLinea(mensaje);
        while (texto.isEmpty()) {
            error("El dato no puede quedar vacío.");
            texto = leerLinea(mensaje);
        }
        return texto;
    }

    /**
     * @return el texto ingresado, o null si se presionó Enter sin escribir nada
     */
    public String leerTextoOpcional(String mensaje) {
        String texto = leerLinea(mensaje);
        return texto.isEmpty() ? null : texto;
    }

    public int leerEntero(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                error("'" + texto + "' no es un número entero válido.");
            }
        }
    }

    /**
     * Lee un entero entre min y max inclusive (opciones de menú).
     */
    public int leerEnteroEnRango(String mensaje, int min, int max) {
        int valor = leerEntero(mensaje);
        while (valor < min || valor > max) {
            error("Opción fuera de rango: ingresá un número entre " + min + " y " + max + ".");
            valor = leerEntero(mensaje);
        }
        return valor;
    }

    /**
     * @return el entero ingresado, o null si se presionó Enter (conservar el valor anterior)
     */
    public Integer leerEnteroOpcional(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                error("'" + texto + "' no es un número entero válido.");
            }
        }
    }

    /**
     * Lee un número con decimales; acepta coma o punto como separador.
     */
    public double leerDecimal(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje);
            try {
                return Double.parseDouble(texto.replace(',', '.'));
            } catch (NumberFormatException e) {
                error("'" + texto + "' no es un número válido (ejemplo: 1500.50).");
            }
        }
    }

    /**
     * @return el número ingresado, o null si se presionó Enter (conservar el valor anterior)
     */
    public Double leerDecimalOpcional(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(texto.replace(',', '.'));
            } catch (NumberFormatException e) {
                error("'" + texto + "' no es un número válido (ejemplo: 1500.50).");
            }
        }
    }

    /**
     * Lee un id: entero positivo.
     */
    public Long leerId(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje);
            try {
                long id = Long.parseLong(texto);
                if (id > 0) {
                    return id;
                }
                error("El id debe ser un número mayor a cero.");
            } catch (NumberFormatException e) {
                error("'" + texto + "' no es un id válido.");
            }
        }
    }

    /**
     * Pregunta S/N hasta obtener una respuesta válida.
     */
    public boolean confirmar(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje + " (S/N): ").toUpperCase();
            if (texto.equals("S")) {
                return true;
            }
            if (texto.equals("N")) {
                return false;
            }
            error("Respondé S o N.");
        }
    }

    /**
     * @return true (S), false (N) o null si se presionó Enter (conservar el valor anterior)
     */
    public Boolean leerSiNoOpcional(String mensaje) {
        while (true) {
            String texto = leerLinea(mensaje).toUpperCase();
            if (texto.isEmpty()) {
                return null;
            }
            if (texto.equals("S")) {
                return Boolean.TRUE;
            }
            if (texto.equals("N")) {
                return Boolean.FALSE;
            }
            error("Respondé S, N o Enter para conservar.");
        }
    }

    /**
     * Muestra los valores de un enum numerados desde 1 y devuelve el elegido.
     *
     * @param <E>     tipo del enum (Rol, Estado o FormaPago)
     * @param titulo  texto a mostrar antes de las opciones
     * @param valores resultado de E.values()
     */
    public <E extends Enum<E>> E elegirOpcion(String titulo, E[] valores) {
        imprimirOpcionesEnum(titulo, valores);
        int opcion = leerEnteroEnRango("Opción: ", 1, valores.length);
        return valores[opcion - 1];
    }

    /**
     * Igual que elegirOpcion, pero con 0 para conservar el valor actual.
     *
     * @return el valor elegido, o null si se eligió 0
     */
    public <E extends Enum<E>> E elegirOpcionOpcional(String titulo, E[] valores, E actual) {
        imprimirOpcionesEnum(titulo + " (actual: " + actual + ")", valores);
        System.out.println("  0) Conservar " + actual);
        int opcion = leerEnteroEnRango("Opción: ", 0, valores.length);
        return opcion == 0 ? null : valores[opcion - 1];
    }

    /**
     * Imprime un listado de a páginas, para que listas largas (miles de productos) no tapen la pantalla.
     *
     * @param elementos   objetos a mostrar con su toString
     * @param mensajeVacio texto a mostrar si la lista está vacía
     * @return false si la lista estaba vacía
     */
    public boolean mostrarListado(List<?> elementos, String mensajeVacio) {
        if (elementos.isEmpty()) {
            info(mensajeVacio);
            return false;
        }
        for (int i = 0; i < elementos.size(); i++) {
            System.out.println("  " + elementos.get(i));
            boolean finDePagina = (i + 1) % TAMANIO_PAGINA == 0;
            boolean quedanMas = i + 1 < elementos.size();
            if (finDePagina && quedanMas) {
                String respuesta = leerLinea("  -- " + (i + 1) + " de " + elementos.size()
                        + ". Enter para seguir, 0 para cortar el listado: ");
                if (respuesta.equals("0")) {
                    break;
                }
            }
        }
        System.out.println("  (" + elementos.size() + " registro/s)");
        return true;
    }

    public void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }

    public void exito(String mensaje) {
        System.out.println("OK: " + mensaje);
    }

    public void error(String mensaje) {
        System.out.println("ERROR: " + mensaje);
    }

    public void info(String mensaje) {
        System.out.println(mensaje);
    }

    private <E extends Enum<E>> void imprimirOpcionesEnum(String titulo, E[] valores) {
        System.out.println(titulo);
        for (int i = 0; i < valores.length; i++) {
            System.out.println("  " + (i + 1) + ") " + valores[i]);
        }
    }
}
