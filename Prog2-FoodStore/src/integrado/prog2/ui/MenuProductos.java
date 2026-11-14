package integrado.prog2.ui;

import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Producto;
import integrado.prog2.exception.FoodStoreException;
import integrado.prog2.service.CategoriaService;
import integrado.prog2.service.ProductoService;

/**
 * Submenú de la épica 2: ABM de productos contra la BASE DE DATOS.
 */
public class MenuProductos extends Submenu {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public MenuProductos(Consola consola, ProductoService productoService, CategoriaService categoriaService) {
        super(consola);
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @Override
    protected String titulo() {
        return "PRODUCTOS (BASE DE DATOS)";
    }

    @Override
    protected String[] opciones() {
        return new String[]{
                "1) Listar todos",
                "2) Listar los productos de una categoria",
                "3) Crear",
                "4) Editar datos",
                "5) Mover a otra categoria",
                "6) Eliminar (baja logica)"
        };
    }

    @Override
    protected void ejecutar(int opcion) throws FoodStoreException {
        switch (opcion) {
            case 1:
                listar();
                break;
            case 2:
                listarPorCategoria();
                break;
            case 3:
                crear();
                break;
            case 4:
                editar();
                break;
            case 5:
                moverDeCategoria();
                break;
            case 6:
                eliminar();
                break;
            default:
                break;
        }
    }

    private boolean listar() throws FoodStoreException {
        consola.info("Productos activos en la base de datos:");
        return consola.mostrarListado(productoService.listar(), "No hay productos cargados.");
    }

    private boolean listarCategorias() throws FoodStoreException {
        consola.info("Categorías disponibles:");
        return consola.mostrarListado(categoriaService.listar(), "No hay categorías cargadas. Creá una primero.");
    }

    private void listarPorCategoria() throws FoodStoreException {
        if (!listarCategorias()) {
            return;
        }
        Long idCategoria = consola.leerId("Id de la categoría: ");
        Categoria categoria = productoService.listarPorCategoria(idCategoria);
        consola.info("Productos de la categoría '" + categoria.getNombre() + "':");
        consola.mostrarListado(categoria.getProductos(), "La categoría no tiene productos activos.");
    }

    private void crear() throws FoodStoreException {
        String nombre = consola.leerTexto("Nombre: ");
        String descripcion = consola.leerLinea("Descripción: ");
        double precio = consola.leerDecimal("Precio: ");
        int stock = consola.leerEntero("Stock: ");
        String imagen = consola.leerLinea("Imagen (nombre de archivo o URL, opcional): ");
        boolean disponible = consola.confirmar("¿Está disponible para la venta?");
        if (!listarCategorias()) {
            return;
        }
        Long idCategoria = consola.leerId("Id de la categoría a la que pertenece: ");
        Producto producto = productoService.crear(nombre, descripcion, precio, stock, imagen, disponible, idCategoria);
        consola.exito("Producto guardado en la BASE DE DATOS con id " + producto.getId() + ".");
    }

    private void editar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id del producto a editar: ");
        Producto actual = productoService.buscar(id);
        consola.info("Datos actuales: " + actual);
        consola.info("Dejá vacío (Enter) lo que no quieras cambiar.");
        String nombre = consola.leerTextoOpcional("Nombre [" + actual.getNombre() + "]: ");
        String descripcion = consola.leerTextoOpcional("Descripción [" + actual.getDescripcion() + "]: ");
        Double precio = consola.leerDecimalOpcional("Precio [" + actual.getPrecio() + "]: ");
        Integer stock = consola.leerEnteroOpcional("Stock [" + actual.getStock() + "]: ");
        String imagen = consola.leerTextoOpcional("Imagen [" + actual.getImagen() + "]: ");
        Boolean disponible = consola.leerSiNoOpcional("¿Disponible? S/N [" + (actual.estaDisponible() ? "S" : "N") + "]: ");
        Producto actualizado = productoService.actualizar(id, nombre, descripcion, precio, stock, imagen, disponible);
        consola.exito("Producto actualizado: " + actualizado);
    }

    private void moverDeCategoria() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long idProducto = consola.leerId("Id del producto a mover: ");
        Producto producto = productoService.buscar(idProducto);
        if (!listarCategorias()) {
            return;
        }
        Long idCategoria = consola.leerId("Id de la nueva categoría: ");
        Categoria destino = productoService.reasignarCategoria(idProducto, idCategoria);
        consola.exito("'" + producto.getNombre() + "' ahora pertenece a la categoría '" + destino.getNombre() + "'.");
    }

    private void eliminar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id del producto a eliminar: ");
        Producto producto = productoService.buscar(id);
        if (!consola.confirmar("¿Confirmás la baja lógica de '" + producto.getNombre() + "'?")) {
            consola.info("Operación cancelada.");
            return;
        }
        productoService.eliminar(id);
        consola.exito("Producto '" + producto.getNombre() + "' dado de baja (eliminado = TRUE).");
    }
}
