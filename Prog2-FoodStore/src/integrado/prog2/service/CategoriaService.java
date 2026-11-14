package integrado.prog2.service;

import integrado.prog2.dao.CategoriaDAO;
import integrado.prog2.dao.ProductoDAO;
import integrado.prog2.entities.Categoria;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.OperacionNoPermitidaException;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.exception.ValidacionException;
import java.util.List;

/**
 * Casos de uso de categorías (épica 1). Todo se resuelve contra la base de datos.
 */
public class CategoriaService {

    private final CategoriaDAO categoriaDAO;
    private final ProductoDAO productoDAO;

    public CategoriaService(CategoriaDAO categoriaDAO, ProductoDAO productoDAO) {
        this.categoriaDAO = categoriaDAO;
        this.productoDAO = productoDAO;
    }

    /**
     * HU-CAT-01.
     */
    public List<Categoria> listar() throws PersistenciaException {
        return categoriaDAO.findAll();
    }

    /**
     * HU-CAT-02: valida campos y unicidad del nombre, e inserta.
     *
     * @return la categoría creada, con el id generado por la base
     */
    public Categoria crear(String nombre, String descripcion) throws ValidacionException, PersistenciaException {
        String nombreLimpio = Validaciones.textoObligatorio(nombre, "nombre");
        String descripcionLimpia = Validaciones.textoObligatorio(descripcion, "descripción");
        validarNombreUnico(nombreLimpio, null);
        Categoria categoria = new Categoria(nombreLimpio, descripcionLimpia);
        categoriaDAO.insert(categoria);
        return categoria;
    }

    /**
     * @return la categoría activa con ese id
     * @throws EntidadNoEncontradaException si no existe o está eliminada
     */
    public Categoria buscar(Long id) throws EntidadNoEncontradaException, PersistenciaException {
        Categoria categoria = categoriaDAO.findById(id);
        if (categoria == null) {
            throw new EntidadNoEncontradaException("No existe una categoría activa con id " + id + ".");
        }
        return categoria;
    }

    /**
     * HU-CAT-03: los parámetros vacíos o null conservan el valor anterior.
     *
     * @return la categoría actualizada
     */
    public Categoria actualizar(Long id, String nuevoNombre, String nuevaDescripcion)
            throws EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        Categoria categoria = buscar(id);
        if (Validaciones.seIngreso(nuevoNombre)) {
            String nombreLimpio = nuevoNombre.trim();
            validarNombreUnico(nombreLimpio, id);
            categoria.setNombre(nombreLimpio);
        }
        if (Validaciones.seIngreso(nuevaDescripcion)) {
            categoria.setDescripcion(nuevaDescripcion.trim());
        }
        categoriaDAO.update(categoria);
        return categoria;
    }

    /**
     * HU-CAT-04: baja lógica, impedida si la categoría tiene productos activos.
     */
    public void eliminar(Long id)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, PersistenciaException {
        buscar(id);
        int productosActivos = productoDAO.contarActivosPorCategoria(id);
        if (productosActivos > 0) {
            throw new OperacionNoPermitidaException("No se puede eliminar la categoría: tiene " + productosActivos
                    + " producto(s) activo(s) asociado(s). Movelos a otra categoría o eliminalos primero.");
        }
        categoriaDAO.softDelete(id);
    }

    private void validarNombreUnico(String nombre, Long idExcluido) throws ValidacionException, PersistenciaException {
        if (categoriaDAO.existeNombreActivo(nombre, idExcluido)) {
            throw new ValidacionException("Ya existe otra categoría activa llamada '" + nombre + "'.");
        }
    }
}
