package integrado.prog2.entities;

import integrado.prog2.enums.Rol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Usuario del sistema. Vive sólo en memoria (no hay tabla usuario).
 * Conoce sus pedidos (asociación 1:N unidireccional); el pedido no conoce a su usuario.
 */
public class Usuario extends Base {

    private String nombre;
    private String apellido;
    private String mail;
    private String celular;
    private String contrasena;
    private Rol rol;
    private final List<Pedido> pedidos;

    public Usuario(String nombre, String apellido, String mail, String celular, String contrasena, Rol rol) {
        super();
        this.nombre = nombre;
        this.apellido = apellido;
        this.mail = mail;
        this.celular = celular;
        this.contrasena = contrasena;
        this.rol = rol;
        this.pedidos = new ArrayList<>();
    }

    /**
     * Registra un pedido confirmado dentro del usuario.
     *
     * @param pedido pedido ya validado por el servicio
     */
    public void agregarPedido(Pedido pedido) {
        if (pedido != null && !pedidos.contains(pedido)) {
            pedidos.add(pedido);
        }
    }

    /**
     * @return vista de sólo lectura de todos los pedidos, incluidos los dados de baja
     */
    public List<Pedido> getPedidos() {
        return Collections.unmodifiableList(pedidos);
    }

    /**
     * @return pedidos que no fueron dados de baja
     */
    public List<Pedido> getPedidosActivos() {
        List<Pedido> activos = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            if (!pedido.isEliminado()) {
                activos.add(pedido);
            }
        }
        return activos;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return String.format("#%-4d %-15s %-15s %-30s %-8s cel: %s",
                getId(), nombre, apellido, mail, rol, celular.isEmpty() ? "-" : celular);
    }
}
