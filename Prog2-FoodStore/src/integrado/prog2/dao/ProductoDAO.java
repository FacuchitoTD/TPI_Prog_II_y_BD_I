package integrado.prog2.dao;

import integrado.prog2.entities.Producto;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.exception.StockInvalidoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Acceso JDBC a la tabla producto. Es la única clase que conoce la columna categoria_id:
 * la recibe como parámetro en el alta y en la reasignación, pero nunca la guarda en el objeto.
 */
public class ProductoDAO implements GenericDAO<Producto> {

    private static final String COLUMNAS =
            "id, nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at";
    private static final String INSERT =
            "INSERT INTO producto (nombre, precio, descripcion, stock, imagen, disponible, eliminado, created_at, categoria_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, FALSE, ?, ?)";
    private static final String SELECT_ACTIVOS =
            "SELECT " + COLUMNAS + " FROM producto WHERE eliminado = FALSE ORDER BY id";
    private static final String SELECT_POR_ID =
            "SELECT " + COLUMNAS + " FROM producto WHERE id = ? AND eliminado = FALSE";
    private static final String SELECT_POR_CATEGORIA =
            "SELECT " + COLUMNAS + " FROM producto WHERE categoria_id = ? AND eliminado = FALSE ORDER BY id";
    private static final String CONTAR_ACTIVOS_POR_CATEGORIA =
            "SELECT COUNT(*) FROM producto WHERE categoria_id = ? AND eliminado = FALSE";
    private static final String UPDATE =
            "UPDATE producto SET nombre = ?, precio = ?, descripcion = ?, stock = ?, imagen = ?, disponible = ? "
                    + "WHERE id = ? AND eliminado = FALSE";
    private static final String REASIGNAR_CATEGORIA =
            "UPDATE producto SET categoria_id = ? WHERE id = ? AND eliminado = FALSE";
    private static final String BAJA_LOGICA =
            "UPDATE producto SET eliminado = TRUE WHERE id = ? AND eliminado = FALSE";
    private static final String DESCONTAR_STOCK =
            "UPDATE producto SET stock = stock - ? "
                    + "WHERE id = ? AND eliminado = FALSE AND disponible = TRUE AND stock >= ?";
    private static final String REPONER_STOCK =
            "UPDATE producto SET stock = stock + ? WHERE id = ?";
    private static final String ESTADO_PRODUCTO =
            "SELECT nombre, stock, disponible, eliminado FROM producto WHERE id = ?";

    /**
     * Inserta el producto asociado a una categoría y le asigna el id generado.
     *
     * @param producto    producto sin id
     * @param idCategoria valor para la columna categoria_id (no se guarda en el objeto)
     * @return id generado (getGeneratedKeys)
     */
    public Long insert(Producto producto, Long idCategoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, producto.getNombre());
            sentencia.setDouble(2, producto.getPrecio());
            sentencia.setString(3, producto.getDescripcion());
            sentencia.setInt(4, producto.getStock());
            sentencia.setString(5, producto.getImagen());
            sentencia.setBoolean(6, producto.estaDisponible());
            sentencia.setObject(7, producto.getCreatedAt());
            sentencia.setLong(8, idCategoria);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    producto.setId(claves.getLong(1));
                }
            }
            return producto.getId();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "crear el producto");
        }
    }

    @Override
    public List<Producto> findAll() throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_ACTIVOS);
             ResultSet filas = sentencia.executeQuery()) {
            return mapearTodos(filas);
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "listar los productos");
        }
    }

    @Override
    public Producto findById(Long id) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_POR_ID)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? mapear(filas) : null;
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "buscar el producto");
        }
    }

    /**
     * Navegación Categoría → Productos: productos activos de una categoría.
     *
     * @param idCategoria valor de categoria_id a filtrar
     */
    public List<Producto> findByCategoria(Long idCategoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_POR_CATEGORIA)) {
            sentencia.setLong(1, idCategoria);
            try (ResultSet filas = sentencia.executeQuery()) {
                return mapearTodos(filas);
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "listar los productos de la categoría");
        }
    }

    /**
     * @param idCategoria categoría a consultar
     * @return cantidad de productos activos que la referencian
     */
    public int contarActivosPorCategoria(Long idCategoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONTAR_ACTIVOS_POR_CATEGORIA)) {
            sentencia.setLong(1, idCategoria);
            try (ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getInt(1);
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "contar los productos de la categoría");
        }
    }

    /**
     * Actualiza los atributos propios del producto. No toca categoria_id.
     */
    @Override
    public void update(Producto producto) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(UPDATE)) {
            sentencia.setString(1, producto.getNombre());
            sentencia.setDouble(2, producto.getPrecio());
            sentencia.setString(3, producto.getDescripcion());
            sentencia.setInt(4, producto.getStock());
            sentencia.setString(5, producto.getImagen());
            sentencia.setBoolean(6, producto.estaDisponible());
            sentencia.setLong(7, producto.getId());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "actualizar el producto");
        }
    }

    /**
     * Mueve el producto a otra categoría actualizando sólo la clave foránea.
     */
    public void reasignarCategoria(Long idProducto, Long idCategoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(REASIGNAR_CATEGORIA)) {
            sentencia.setLong(1, idCategoria);
            sentencia.setLong(2, idProducto);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "cambiar la categoría del producto");
        }
    }

    @Override
    public void softDelete(Long id) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(BAJA_LOGICA)) {
            sentencia.setLong(1, id);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "eliminar el producto");
        }
    }

    /**
     * Descuenta el stock de varios productos en una única transacción.
     * Cada UPDATE sólo afecta la fila si el producto sigue activo, disponible y con stock suficiente;
     * si alguno no cumple, se hace rollback de todo y no se descuenta nada.
     *
     * @param cantidadesPorProducto id de producto → unidades a descontar
     * @throws StockInvalidoException si algún producto no tiene stock suficiente
     */
    public void descontarStock(Map<Long, Integer> cantidadesPorProducto)
            throws PersistenciaException, StockInvalidoException {
        try (Connection conexion = ConexionDB.getConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement sentencia = conexion.prepareStatement(DESCONTAR_STOCK)) {
                for (Map.Entry<Long, Integer> linea : cantidadesPorProducto.entrySet()) {
                    sentencia.setInt(1, linea.getValue());
                    sentencia.setLong(2, linea.getKey());
                    sentencia.setInt(3, linea.getValue());
                    if (sentencia.executeUpdate() == 0) {
                        String motivo = explicarFaltante(conexion, linea.getKey(), linea.getValue());
                        conexion.rollback();
                        throw new StockInvalidoException(motivo);
                    }
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "descontar el stock");
        }
    }

    /**
     * Devuelve stock a varios productos en una única transacción
     * (al cancelar o eliminar un pedido vigente, o al quitar una línea).
     *
     * @param cantidadesPorProducto id de producto → unidades a reponer
     */
    public void reponerStock(Map<Long, Integer> cantidadesPorProducto) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement sentencia = conexion.prepareStatement(REPONER_STOCK)) {
                for (Map.Entry<Long, Integer> linea : cantidadesPorProducto.entrySet()) {
                    sentencia.setInt(1, linea.getValue());
                    sentencia.setLong(2, linea.getKey());
                    sentencia.executeUpdate();
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "reponer el stock");
        }
    }

    private String explicarFaltante(Connection conexion, Long idProducto, int pedido) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(ESTADO_PRODUCTO)) {
            sentencia.setLong(1, idProducto);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next() || fila.getBoolean("eliminado")) {
                    return "El producto #" + idProducto + " ya no existe o fue dado de baja.";
                }
                String nombre = fila.getString("nombre");
                if (!fila.getBoolean("disponible")) {
                    return "El producto '" + nombre + "' no está disponible para la venta.";
                }
                return "Stock insuficiente para '" + nombre + "': hay " + fila.getInt("stock")
                        + " y se necesitan " + pedido + ".";
            }
        }
    }

    private List<Producto> mapearTodos(ResultSet filas) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        while (filas.next()) {
            productos.add(mapear(filas));
        }
        return productos;
    }

    private Producto mapear(ResultSet fila) throws SQLException {
        return new Producto(
                fila.getLong("id"),
                fila.getBoolean("eliminado"),
                fila.getObject("created_at", LocalDateTime.class),
                fila.getString("nombre"),
                fila.getDouble("precio"),
                fila.getString("descripcion"),
                fila.getInt("stock"),
                fila.getString("imagen"),
                fila.getBoolean("disponible"));
    }
}
