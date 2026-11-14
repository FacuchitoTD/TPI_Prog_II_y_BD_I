package integrado.prog2.service;

import integrado.prog2.dao.CategoriaDAO;
import integrado.prog2.dao.ProductoDAO;
import integrado.prog2.entities.Categoria;
import integrado.prog2.entities.Producto;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.OperacionNoPermitidaException;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.exception.ValidacionException;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso de productos (épica 2) y operaciones de stock que usan los pedidos.
 */
public class ProductoService {

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;

    public ProductoService(ProductoDAO productoDAO, CategoriaDAO categoriaDAO) {
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
    }

    /**
     * HU-PROD-01.
     */
    public List<Producto> listar() throws PersistenciaException {
        return productoDAO.findAll();
    }

    /**
     * HU-PROD-02: carga los productos activos dentro de la agregación de la categoría.
     *
     * @return la categoría con su lista de productos completa
     */
    public Categoria listarPorCategoria(Long idCategoria) throws EntidadNoEncontradaException, PersistenciaException {
        Categoria categoria = buscarCategoriaActiva(idCategoria);
        for (Producto producto : productoDAO.findByCategoria(idCategoria)) {
            categoria.agregarProducto(producto);
        }
        return categoria;
    }

    /**
     * HU-PROD-03.
     *
     * @param disponible si es null se toma como disponible
     * @return el producto creado, con el id generado por la base
     */
    public Producto crear(String nombre, String descripcion, Double precio, int stock, String imagen,
                          Boolean disponible, Long idCategoria)
            throws ValidacionException, StockInvalidoException, EntidadNoEncontradaException, PersistenciaException {
        String nombreLimpio = Validaciones.textoObligatorio(nombre, "nombre");
        Validaciones.precioValido(precio);
        Validaciones.stockValido(stock);
        buscarCategoriaActiva(idCategoria);
        Producto producto = new Producto(nombreLimpio, precio, Validaciones.textoOpcional(descripcion), stock,
                Validaciones.textoOpcional(imagen), disponible == null ? Boolean.TRUE : disponible);
        productoDAO.insert(producto, idCategoria);
        return producto;
    }

    /**
     * @return el producto activo con ese id
     * @throws EntidadNoEncontradaException si no existe o está eliminado
     */
    public Producto buscar(Long id) throws EntidadNoEncontradaException, PersistenciaException {
        Producto producto = productoDAO.findById(id);
        if (producto == null) {
            throw new EntidadNoEncontradaException("No existe un producto activo con id " + id + ".");
        }
        return producto;
    }

    /**
     * Busca un producto que pueda agregarse a un pedido: activo y disponible.
     */
    public Producto buscarParaPedido(Long id)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, PersistenciaException {
        Producto producto = buscar(id);
        if (!producto.estaDisponible()) {
            throw new OperacionNoPermitidaException("El producto '" + producto.getNombre() + "' no está disponible.");
        }
        return producto;
    }

    /**
     * HU-PROD-04: los parámetros null (o texto vacío) conservan el valor anterior. No cambia la categoría.
     *
     * @return el producto actualizado
     */
    public Producto actualizar(Long id, String nombre, String descripcion, Double precio, Integer stock,
                               String imagen, Boolean disponible)
            throws EntidadNoEncontradaException, ValidacionException, StockInvalidoException, PersistenciaException {
        Producto producto = buscar(id);
        if (precio != null) {
            Validaciones.precioValido(precio);
            producto.setPrecio(precio);
        }
        if (stock != null) {
            Validaciones.stockValido(stock);
            producto.setStock(stock);
        }
        if (Validaciones.seIngreso(nombre)) {
            producto.setNombre(nombre.trim());
        }
        if (Validaciones.seIngreso(descripcion)) {
            producto.setDescripcion(descripcion.trim());
        }
        if (Validaciones.seIngreso(imagen)) {
            producto.setImagen(imagen.trim());
        }
        if (disponible != null) {
            producto.setDisponible(disponible);
        }
        productoDAO.update(producto);
        return producto;
    }

    /**
     * HU-PROD-04 (operación aparte): mueve el producto a otra categoría activa.
     *
     * @return la categoría de destino
     */
    public Categoria reasignarCategoria(Long idProducto, Long idCategoria)
            throws EntidadNoEncontradaException, PersistenciaException {
        buscar(idProducto);
        Categoria destino = buscarCategoriaActiva(idCategoria);
        productoDAO.reasignarCategoria(idProducto, idCategoria);
        return destino;
    }

    /**
     * HU-PROD-05: baja lógica. Los pedidos en memoria conservan su referencia al objeto Producto.
     */
    public void eliminar(Long id) throws EntidadNoEncontradaException, PersistenciaException {
        buscar(id);
        productoDAO.softDelete(id);
    }

    /**
     * Descuenta stock en la base dentro de una transacción (todo o nada).
     */
    public void descontarStock(Map<Long, Integer> cantidadesPorProducto)
            throws StockInvalidoException, PersistenciaException {
        productoDAO.descontarStock(cantidadesPorProducto);
    }

    /**
     * Repone stock en la base dentro de una transacción.
     */
    public void reponerStock(Map<Long, Integer> cantidadesPorProducto) throws PersistenciaException {
        productoDAO.reponerStock(cantidadesPorProducto);
    }

    private Categoria buscarCategoriaActiva(Long idCategoria) throws EntidadNoEncontradaException, PersistenciaException {
        Categoria categoria = categoriaDAO.findById(idCategoria);
        if (categoria == null) {
            throw new EntidadNoEncontradaException("No existe una categoría activa con id " + idCategoria + ".");
        }
        return categoria;
    }
}
