package integrado.prog2.dao;

import integrado.prog2.entities.Base;
import integrado.prog2.exception.PersistenciaException;
import java.util.List;

/**
 * Contrato común de los DAO JDBC. T sólo puede ser una entidad del modelo (subclase de Base).
 * <p>
 * El alta no forma parte del contrato porque no es igual para todas las entidades:
 * el producto necesita además el id de su categoría, que no es un atributo de Producto.
 *
 * @param <T> entidad que administra el DAO
 */
public interface GenericDAO<T extends Base> {

    /**
     * @return registros con eliminado = FALSE
     */
    List<T> findAll() throws PersistenciaException;

    /**
     * @param id clave primaria
     * @return la entidad activa con ese id, o null si no existe o está eliminada
     */
    T findById(Long id) throws PersistenciaException;

    /**
     * Actualiza los atributos propios de la entidad activa.
     */
    void update(T entidad) throws PersistenciaException;

    /**
     * Baja lógica: UPDATE ... SET eliminado = TRUE. Nunca se usa DELETE.
     */
    void softDelete(Long id) throws PersistenciaException;
}
