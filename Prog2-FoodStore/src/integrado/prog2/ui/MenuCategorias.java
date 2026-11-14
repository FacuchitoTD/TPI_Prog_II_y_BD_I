package integrado.prog2.ui;

import integrado.prog2.entities.Categoria;
import integrado.prog2.exception.FoodStoreException;
import integrado.prog2.service.CategoriaService;

/**
 * Submenú de la épica 1: ABM de categorías contra la BASE DE DATOS.
 */
public class MenuCategorias extends Submenu {

    private final CategoriaService categoriaService;

    public MenuCategorias(Consola consola, CategoriaService categoriaService) {
        super(consola);
        this.categoriaService = categoriaService;
    }

    @Override
    protected String titulo() {
        return "CATEGORIAS (BASE DE DATOS)";
    }

    @Override
    protected String[] opciones() {
        return new String[]{
                "1) Listar",
                "2) Crear",
                "3) Editar",
                "4) Eliminar (baja logica)"
        };
    }

    @Override
    protected void ejecutar(int opcion) throws FoodStoreException {
        switch (opcion) {
            case 1:
                listar();
                break;
            case 2:
                crear();
                break;
            case 3:
                editar();
                break;
            case 4:
                eliminar();
                break;
            default:
                break;
        }
    }

    private boolean listar() throws FoodStoreException {
        consola.info("Categorías activas en la base de datos:");
        return consola.mostrarListado(categoriaService.listar(), "No hay categorías cargadas.");
    }

    private void crear() throws FoodStoreException {
        String nombre = consola.leerTexto("Nombre: ");
        String descripcion = consola.leerTexto("Descripción: ");
        Categoria categoria = categoriaService.crear(nombre, descripcion);
        consola.exito("Categoría guardada en la BASE DE DATOS con id " + categoria.getId() + ".");
    }

    private void editar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id de la categoría a editar: ");
        Categoria actual = categoriaService.buscar(id);
        consola.info("Datos actuales: " + actual);
        consola.info("Dejá vacío (Enter) lo que no quieras cambiar.");
        String nombre = consola.leerTextoOpcional("Nuevo nombre [" + actual.getNombre() + "]: ");
        String descripcion = consola.leerTextoOpcional("Nueva descripción [" + actual.getDescripcion() + "]: ");
        Categoria actualizada = categoriaService.actualizar(id, nombre, descripcion);
        consola.exito("Categoría actualizada: " + actualizada);
    }

    private void eliminar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id de la categoría a eliminar: ");
        Categoria categoria = categoriaService.buscar(id);
        if (!consola.confirmar("¿Confirmás la baja lógica de '" + categoria.getNombre() + "'?")) {
            consola.info("Operación cancelada.");
            return;
        }
        categoriaService.eliminar(id);
        consola.exito("Categoría '" + categoria.getNombre() + "' dada de baja (eliminado = TRUE).");
    }
}
