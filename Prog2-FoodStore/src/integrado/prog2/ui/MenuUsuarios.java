package integrado.prog2.ui;

import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Rol;
import integrado.prog2.exception.FoodStoreException;
import integrado.prog2.service.UsuarioService;

/**
 * Submenú de la épica 3: ABM de usuarios en MEMORIA.
 */
public class MenuUsuarios extends Submenu {

    private final UsuarioService usuarioService;

    public MenuUsuarios(Consola consola, UsuarioService usuarioService) {
        super(consola);
        this.usuarioService = usuarioService;
    }

    @Override
    protected String titulo() {
        return "USUARIOS (MEMORIA - se pierden al cerrar el programa)";
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

    private boolean listar() {
        consola.info("Usuarios activos en memoria:");
        return consola.mostrarListado(usuarioService.listar(), "No hay usuarios cargados en memoria.");
    }

    private void crear() throws FoodStoreException {
        String nombre = consola.leerTexto("Nombre: ");
        String apellido = consola.leerTexto("Apellido: ");
        String mail = consola.leerTexto("Mail: ");
        String celular = consola.leerLinea("Celular (opcional): ");
        String contrasena = consola.leerTexto("Contraseña: ");
        Rol rol = consola.elegirOpcion("Rol:", Rol.values());
        Usuario usuario = usuarioService.crear(nombre, apellido, mail, celular, contrasena, rol);
        consola.exito("Usuario agregado en MEMORIA con id " + usuario.getId()
                + " (se pierde al cerrar el programa).");
    }

    private void editar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id del usuario a editar: ");
        Usuario actual = usuarioService.buscarActivo(id);
        consola.info("Dejá vacío (Enter) lo que no quieras cambiar.");
        String nombre = consola.leerTextoOpcional("Nombre [" + actual.getNombre() + "]: ");
        String apellido = consola.leerTextoOpcional("Apellido [" + actual.getApellido() + "]: ");
        String mail = consola.leerTextoOpcional("Mail [" + actual.getMail() + "]: ");
        String celular = consola.leerTextoOpcional("Celular [" + actual.getCelular() + "]: ");
        String contrasena = consola.leerTextoOpcional("Contraseña [sin cambios]: ");
        Rol rol = consola.elegirOpcionOpcional("Rol", Rol.values(), actual.getRol());
        Usuario actualizado = usuarioService.actualizar(id, nombre, apellido, mail, celular, contrasena, rol);
        consola.exito("Usuario actualizado en memoria: " + actualizado);
    }

    private void eliminar() throws FoodStoreException {
        if (!listar()) {
            return;
        }
        Long id = consola.leerId("Id del usuario a eliminar: ");
        Usuario usuario = usuarioService.buscarActivo(id);
        if (!consola.confirmar("¿Confirmás la baja lógica de " + usuario.getNombreCompleto() + "?")) {
            consola.info("Operación cancelada.");
            return;
        }
        usuarioService.eliminar(id);
        consola.exito("Usuario dado de baja (eliminado = true). Sus pedidos siguen disponibles como historial.");
    }
}
