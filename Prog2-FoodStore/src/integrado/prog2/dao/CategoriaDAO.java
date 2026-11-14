package integrado.prog2.dao;

import integrado.prog2.entities.Categoria;
import integrado.prog2.exception.PersistenciaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso JDBC a la tabla categoria.
 */
public class CategoriaDAO implements GenericDAO<Categoria> {

    private static final String COLUMNAS = "id, nombre, descripcion, eliminado, created_at";
    private static final String INSERT =
            "INSERT INTO categoria (nombre, descripcion, eliminado, created_at) VALUES (?, ?, FALSE, ?)";
    private static final String SELECT_ACTIVAS =
            "SELECT " + COLUMNAS + " FROM categoria WHERE eliminado = FALSE ORDER BY id";
    private static final String SELECT_POR_ID =
            "SELECT " + COLUMNAS + " FROM categoria WHERE id = ? AND eliminado = FALSE";
    private static final String UPDATE =
            "UPDATE categoria SET nombre = ?, descripcion = ? WHERE id = ? AND eliminado = FALSE";
    private static final String BAJA_LOGICA =
            "UPDATE categoria SET eliminado = TRUE WHERE id = ? AND eliminado = FALSE";
    private static final String CONTAR_NOMBRE_ACTIVO =
            "SELECT COUNT(*) FROM categoria WHERE nombre = ? AND eliminado = FALSE AND id <> ?";

    /**
     * Inserta la categoría y le asigna el id generado por la base.
     *
     * @param categoria categoría sin id
     * @return id generado (getGeneratedKeys)
     */
    public Long insert(Categoria categoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setString(2, categoria.getDescripcion());
            sentencia.setObject(3, categoria.getCreatedAt());
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    categoria.setId(claves.getLong(1));
                }
            }
            return categoria.getId();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "crear la categoría");
        }
    }

    @Override
    public List<Categoria> findAll() throws PersistenciaException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_ACTIVAS);
             ResultSet filas = sentencia.executeQuery()) {
            while (filas.next()) {
                categorias.add(mapear(filas));
            }
            return categorias;
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "listar las categorías");
        }
    }

    @Override
    public Categoria findById(Long id) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(SELECT_POR_ID)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? mapear(filas) : null;
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "buscar la categoría");
        }
    }

    @Override
    public void update(Categoria categoria) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(UPDATE)) {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setString(2, categoria.getDescripcion());
            sentencia.setLong(3, categoria.getId());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "actualizar la categoría");
        }
    }

    @Override
    public void softDelete(Long id) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(BAJA_LOGICA)) {
            sentencia.setLong(1, id);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "eliminar la categoría");
        }
    }

    /**
     * Consulta SQL de unicidad del nombre entre las categorías activas.
     *
     * @param nombre      nombre a verificar
     * @param idExcluido  id de la categoría que se está editando (se ignora en la búsqueda), o null en un alta
     * @return true si otra categoría activa ya usa ese nombre
     */
    public boolean existeNombreActivo(String nombre, Long idExcluido) throws PersistenciaException {
        try (Connection conexion = ConexionDB.getConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONTAR_NOMBRE_ACTIVO)) {
            sentencia.setString(1, nombre);
            sentencia.setLong(2, idExcluido == null ? 0L : idExcluido);
            try (ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw ConexionDB.traducir(e, "verificar el nombre de la categoría");
        }
    }

    private Categoria mapear(ResultSet fila) throws SQLException {
        return new Categoria(
                fila.getLong("id"),
                fila.getBoolean("eliminado"),
                fila.getObject("created_at", LocalDateTime.class),
                fila.getString("nombre"),
                fila.getString("descripcion"));
    }
}
