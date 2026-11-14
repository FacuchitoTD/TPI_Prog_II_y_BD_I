package integrado.prog2.ui;

import integrado.prog2.exception.FoodStoreException;

/**
 * Esqueleto común de los submenús ABM. Resuelve el ciclo mostrar → leer opción → ejecutar → capturar errores;
 * cada subclase sólo define su título, sus opciones y qué hace cada una.
 */
public abstract class Submenu {

    protected final Consola consola;

    protected Submenu(Consola consola) {
        this.consola = consola;
    }

    /**
     * Muestra el submenú hasta que se elija 0. Los errores se informan y se vuelve a mostrar el menú.
     */
    public void mostrar() {
        int opcion;
        do {
            consola.titulo(titulo());
            for (String linea : opciones()) {
                System.out.println(linea);
            }
            System.out.println("0) Volver al menu principal");
            opcion = consola.leerEnteroEnRango("Seleccione: ", 0, opciones().length);
            if (opcion != 0) {
                try {
                    ejecutar(opcion);
                } catch (FoodStoreException e) {
                    consola.error(e.getMessage());
                }
            }
        } while (opcion != 0);
    }

    /**
     * @return título del submenú, indicando dónde se guardan los datos
     */
    protected abstract String titulo();

    /**
     * @return una línea por opción, numeradas desde 1 y en orden
     */
    protected abstract String[] opciones();

    /**
     * @param opcion número elegido, entre 1 y opciones().length
     */
    protected abstract void ejecutar(int opcion) throws FoodStoreException;
}
