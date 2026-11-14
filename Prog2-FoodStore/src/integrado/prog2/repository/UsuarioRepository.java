package integrado.prog2.repository;

import integrado.prog2.entities.Usuario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Colección en memoria de usuarios. Los datos se pierden al cerrar el programa.
 * Los ids los genera el propio repositorio con un contador que nunca se reutiliza.
 */
public class UsuarioRepository {

    private final List<Usuario> usuarios = new ArrayList<>();
    private long proximoId = 1;

    /**
     * Asigna un id nuevo y agrega el usuario a la colección.
     *
     * @param usuario usuario ya validado
     * @return el mismo usuario, con su id
     */
    public Usuario guardar(Usuario usuario) {
        usuario.setId(proximoId);
        proximoId++;
        usuarios.add(usuario);
        return usuario;
    }

    /**
     * @return usuarios no eliminados
     */
    public List<Usuario> listarActivos() {
        List<Usuario> activos = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            if (!usuario.isEliminado()) {
                activos.add(usuario);
            }
        }
        return activos;
    }

    /**
     * @return todos los usuarios, incluidos los dados de baja (para consultar el historial de pedidos)
     */
    public List<Usuario> listarTodos() {
        return Collections.unmodifiableList(usuarios);
    }

    /**
     * @return el usuario con ese id (esté o no eliminado), o null si nunca existió
     */
    public Usuario buscarPorId(Long id) {
        for (Usuario usuario : usuarios) {
            if (usuario.getId().equals(id)) {
                return usuario;
            }
        }
        return null;
    }

    /**
     * Recorre la colección buscando otro usuario activo con el mismo mail (sin distinguir mayúsculas).
     *
     * @param mail       mail a verificar
     * @param idExcluido id del usuario que se está editando, o null en un alta
     * @return true si el mail ya está en uso
     */
    public boolean existeMail(String mail, Long idExcluido) {
        for (Usuario usuario : usuarios) {
            boolean esElMismo = usuario.getId().equals(idExcluido);
            if (!usuario.isEliminado() && !esElMismo && usuario.getMail().equalsIgnoreCase(mail)) {
                return true;
            }
        }
        return false;
    }
}
