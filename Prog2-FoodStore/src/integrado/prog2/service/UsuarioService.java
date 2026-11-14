package integrado.prog2.service;

import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Rol;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.ValidacionException;
import integrado.prog2.repository.UsuarioRepository;
import java.util.List;

/**
 * Casos de uso de usuarios (épica 3). Todo se resuelve en memoria.
 */
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * HU-USR-01.
     */
    public List<Usuario> listar() {
        return usuarioRepository.listarActivos();
    }

    /**
     * HU-USR-02: valida los datos y la unicidad del mail, y agrega el usuario a la colección.
     *
     * @return el usuario creado, con el id generado por el programa
     */
    public Usuario crear(String nombre, String apellido, String mail, String celular, String contrasena, Rol rol)
            throws ValidacionException {
        String nombreLimpio = Validaciones.textoObligatorio(nombre, "nombre");
        String apellidoLimpio = Validaciones.textoObligatorio(apellido, "apellido");
        String mailLimpio = Validaciones.mailValido(mail);
        String contrasenaLimpia = Validaciones.textoObligatorio(contrasena, "contraseña");
        if (rol == null) {
            throw new ValidacionException("Debe indicarse el rol del usuario.");
        }
        validarMailUnico(mailLimpio, null);
        Usuario usuario = new Usuario(nombreLimpio, apellidoLimpio, mailLimpio,
                Validaciones.textoOpcional(celular), contrasenaLimpia, rol);
        return usuarioRepository.guardar(usuario);
    }

    /**
     * @return el usuario activo con ese id
     * @throws EntidadNoEncontradaException si no existe o fue dado de baja
     */
    public Usuario buscarActivo(Long id) throws EntidadNoEncontradaException {
        Usuario usuario = usuarioRepository.buscarPorId(id);
        if (usuario == null || usuario.isEliminado()) {
            throw new EntidadNoEncontradaException("No existe un usuario activo con id " + id + ".");
        }
        return usuario;
    }

    /**
     * HU-USR-03: los parámetros null (o texto vacío) conservan el valor anterior.
     * Primero se valida todo y recién después se modifica el objeto, para no dejarlo a medio cambiar.
     *
     * @return el usuario actualizado
     */
    public Usuario actualizar(Long id, String nombre, String apellido, String mail, String celular,
                              String contrasena, Rol rol) throws EntidadNoEncontradaException, ValidacionException {
        Usuario usuario = buscarActivo(id);
        String mailNuevo = null;
        if (Validaciones.seIngreso(mail)) {
            mailNuevo = Validaciones.mailValido(mail);
            validarMailUnico(mailNuevo, id);
        }
        if (Validaciones.seIngreso(nombre)) {
            usuario.setNombre(nombre.trim());
        }
        if (Validaciones.seIngreso(apellido)) {
            usuario.setApellido(apellido.trim());
        }
        if (mailNuevo != null) {
            usuario.setMail(mailNuevo);
        }
        if (Validaciones.seIngreso(celular)) {
            usuario.setCelular(celular.trim());
        }
        if (Validaciones.seIngreso(contrasena)) {
            usuario.setContrasena(contrasena.trim());
        }
        if (rol != null) {
            usuario.setRol(rol);
        }
        return usuario;
    }

    /**
     * HU-USR-04: baja lógica; el usuario queda en la colección con su historial de pedidos.
     */
    public void eliminar(Long id) throws EntidadNoEncontradaException {
        Usuario usuario = buscarActivo(id);
        usuario.setEliminado(true);
    }

    private void validarMailUnico(String mail, Long idExcluido) throws ValidacionException {
        if (usuarioRepository.existeMail(mail, idExcluido)) {
            throw new ValidacionException("El mail '" + mail + "' ya pertenece a otro usuario.");
        }
    }
}
