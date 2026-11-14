package integrado.prog2.ui;

import integrado.prog2.entities.DetallePedido;
import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.exception.FoodStoreException;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.service.PedidoService;
import integrado.prog2.service.ProductoService;
import integrado.prog2.service.UsuarioService;
import java.util.List;
import java.util.Map;

/**
 * Submenú de la épica 4: pedidos y detalles en MEMORIA, con el stock de los productos en la BASE DE DATOS.
 */
public class MenuPedidos extends Submenu {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;

    public MenuPedidos(Consola consola, PedidoService pedidoService, UsuarioService usuarioService,
                       ProductoService productoService) {
        super(consola);
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    @Override
    protected String titulo() {
        return "PEDIDOS Y DETALLES (MEMORIA - el stock se actualiza en la BASE DE DATOS)";
    }

    @Override
    protected String[] opciones() {
        return new String[]{
                "1) Listar pedidos",
                "2) Listar pedidos de un usuario",
                "3) Crear pedido con detalles",
                "4) Actualizar estado / forma de pago",
                "5) Gestionar detalles de un pedido",
                "6) Eliminar pedido (baja logica)"
        };
    }

    @Override
    protected void ejecutar(int opcion) throws FoodStoreException {
        switch (opcion) {
            case 1:
                listarPedidos();
                break;
            case 2:
                listarPedidosDeUsuario();
                break;
            case 3:
                crearPedido();
                break;
            case 4:
                actualizarEstado();
                break;
            case 5:
                gestionarDetalles();
                break;
            case 6:
                eliminarPedido();
                break;
            default:
                break;
        }
    }

    private boolean listarPedidos() {
        Map<Usuario, List<Pedido>> pedidosPorUsuario = pedidoService.listarPedidosPorUsuario();
        if (pedidosPorUsuario.isEmpty()) {
            consola.info("No hay pedidos cargados en memoria.");
            return false;
        }
        for (Map.Entry<Usuario, List<Pedido>> entrada : pedidosPorUsuario.entrySet()) {
            Usuario usuario = entrada.getKey();
            System.out.println();
            System.out.println("Usuario #" + usuario.getId() + " - " + usuario.getNombreCompleto()
                    + (usuario.isEliminado() ? " [usuario dado de baja]" : ""));
            for (Pedido pedido : entrada.getValue()) {
                System.out.println("   " + pedido.toStringConDetalles());
            }
        }
        return true;
    }

    private void listarPedidosDeUsuario() throws FoodStoreException {
        if (!consola.mostrarListado(usuarioService.listar(), "No hay usuarios activos en memoria.")) {
            return;
        }
        Long idUsuario = consola.leerId("Id del usuario: ");
        List<Pedido> pedidos = pedidoService.listarPedidosDeUsuario(idUsuario);
        if (pedidos.isEmpty()) {
            consola.info("El usuario no tiene pedidos.");
            return;
        }
        for (Pedido pedido : pedidos) {
            System.out.println("   " + pedido.toStringConDetalles());
        }
    }

    private void crearPedido() throws FoodStoreException {
        consola.info("Usuarios activos:");
        if (!consola.mostrarListado(usuarioService.listar(), "No hay usuarios activos: creá uno antes de cargar un pedido.")) {
            return;
        }
        Usuario usuario = pedidoService.usuarioParaNuevoPedido(consola.leerId("Id del usuario que hace el pedido: "));
        FormaPago formaPago = consola.elegirOpcion("Forma de pago:", FormaPago.values());
        Pedido pedido = pedidoService.nuevoPedido(formaPago);

        boolean armando = true;
        while (armando) {
            consola.titulo("Pedido en armado para " + usuario.getNombreCompleto() + " - total $"
                    + String.format("%.2f", pedido.getTotal()));
            System.out.println("1) Agregar producto");
            System.out.println("2) Quitar producto");
            System.out.println("3) Ver resumen");
            System.out.println("4) Confirmar pedido");
            System.out.println("0) Cancelar (descarta el pedido)");
            int opcion = consola.leerEnteroEnRango("Seleccione: ", 0, 4);
            try {
                switch (opcion) {
                    case 1:
                        agregarAlPedidoEnArmado(pedido);
                        break;
                    case 2:
                        quitarDelPedidoEnArmado(pedido);
                        break;
                    case 3:
                        consola.info(pedido.toStringConDetalles());
                        break;
                    case 4:
                        armando = !confirmarPedido(usuario, pedido);
                        break;
                    default:
                        consola.info("Pedido descartado. No se modificó nada en memoria ni en la base.");
                        armando = false;
                        break;
                }
            } catch (StockInvalidoException | PersistenciaException e) {
                if (opcion == 4) {
                    consola.error(e.getMessage());
                    consola.error("Se hizo rollback: el stock de la base no cambió y el pedido NO se registró.");
                    armando = false;
                } else {
                    consola.error(e.getMessage());
                }
            } catch (FoodStoreException e) {
                consola.error(e.getMessage());
            }
        }
    }

    private void agregarAlPedidoEnArmado(Pedido pedido) throws FoodStoreException {
        consola.info("Productos de la base de datos:");
        if (!consola.mostrarListado(productoService.listar(), "No hay productos cargados en la base.")) {
            return;
        }
        Long idProducto = consola.leerId("Id del producto: ");
        int cantidad = consola.leerEntero("Cantidad: ");
        DetallePedido linea = pedidoService.agregarAlPedidoEnArmado(pedido, idProducto, cantidad);
        consola.exito("Línea cargada: " + linea);
    }

    private void quitarDelPedidoEnArmado(Pedido pedido) throws FoodStoreException {
        if (!consola.mostrarListado(pedido.getDetallesActivos(), "El pedido todavía no tiene productos.")) {
            return;
        }
        Long idProducto = consola.leerId("Id del producto a quitar: ");
        pedidoService.quitarDelPedidoEnArmado(pedido, idProducto);
        consola.exito("Producto quitado del pedido.");
    }

    private boolean confirmarPedido(Usuario usuario, Pedido pedido) throws FoodStoreException {
        consola.info("Resumen del pedido:");
        consola.info(pedido.toStringConDetalles());
        if (!consola.confirmar("¿Confirmás el pedido? Se descontará el stock en la base de datos")) {
            return false;
        }
        pedidoService.confirmarPedido(usuario, pedido);
        consola.exito("Pedido #" + pedido.getId() + " registrado en MEMORIA para " + usuario.getNombreCompleto()
                + ". Stock descontado en la BASE DE DATOS. Total: $" + String.format("%.2f", pedido.getTotal()));
        return true;
    }

    private Pedido elegirPedido(String mensaje) throws FoodStoreException {
        if (!listarPedidos()) {
            return null;
        }
        return pedidoService.buscarPedido(consola.leerId(mensaje));
    }

    private void actualizarEstado() throws FoodStoreException {
        Pedido pedido = elegirPedido("Id del pedido a actualizar: ");
        if (pedido == null) {
            return;
        }
        Estado estado = consola.elegirOpcionOpcional("Nuevo estado", Estado.values(), pedido.getEstado());
        FormaPago formaPago = consola.elegirOpcionOpcional("Nueva forma de pago", FormaPago.values(), pedido.getFormaPago());
        if (estado == null && formaPago == null) {
            consola.info("No se eligió ningún cambio.");
            return;
        }
        if (estado == Estado.CANCELADO
                && !consola.confirmar("Al cancelar se repone el stock en la base de datos. ¿Continuar?")) {
            consola.info("Operación cancelada.");
            return;
        }
        Pedido actualizado = pedidoService.actualizarEstadoYFormaPago(pedido.getId(), estado, formaPago);
        consola.exito("Pedido actualizado en memoria: " + actualizado);
        if (estado == Estado.CANCELADO) {
            consola.exito("Stock repuesto en la BASE DE DATOS.");
        }
    }

    private void gestionarDetalles() throws FoodStoreException {
        Pedido pedido = elegirPedido("Id del pedido: ");
        if (pedido == null) {
            return;
        }
        Long idPedido = pedido.getId();
        int opcion;
        do {
            consola.titulo("Detalles del pedido #" + idPedido + " - " + pedido.getEstado()
                    + " - total $" + String.format("%.2f", pedido.getTotal()));
            System.out.println("1) Ver lineas");
            System.out.println("2) Buscar linea por producto");
            System.out.println("3) Agregar producto (descuenta stock en la BASE DE DATOS)");
            System.out.println("4) Modificar cantidad de una linea (ajusta stock en la BASE DE DATOS)");
            System.out.println("5) Quitar producto (repone stock en la BASE DE DATOS)");
            System.out.println("0) Volver");
            opcion = consola.leerEnteroEnRango("Seleccione: ", 0, 5);
            try {
                switch (opcion) {
                    case 1:
                        consola.info(pedido.toStringConDetalles());
                        break;
                    case 2:
                        buscarLinea(idPedido);
                        break;
                    case 3:
                        agregarAPedido(idPedido);
                        break;
                    case 4:
                        modificarCantidad(pedido);
                        break;
                    case 5:
                        quitarDePedido(pedido);
                        break;
                    default:
                        break;
                }
            } catch (FoodStoreException e) {
                consola.error(e.getMessage());
            }
        } while (opcion != 0);
    }

    private void buscarLinea(Long idPedido) throws FoodStoreException {
        Long idProducto = consola.leerId("Id del producto a buscar en el pedido: ");
        DetallePedido linea = pedidoService.buscarDetalle(idPedido, idProducto);
        consola.info("Encontrada: " + linea);
    }

    private void agregarAPedido(Long idPedido) throws FoodStoreException {
        consola.info("Productos de la base de datos:");
        if (!consola.mostrarListado(productoService.listar(), "No hay productos cargados en la base.")) {
            return;
        }
        Long idProducto = consola.leerId("Id del producto: ");
        int cantidad = consola.leerEntero("Cantidad: ");
        DetallePedido linea = pedidoService.agregarAPedido(idPedido, idProducto, cantidad);
        consola.exito("Línea actualizada: " + linea);
    }

    private void modificarCantidad(Pedido pedido) throws FoodStoreException {
        consola.mostrarListado(pedido.getDetallesActivos(), "El pedido no tiene líneas.");
        Long idProducto = consola.leerId("Id del producto de la línea a modificar: ");
        int cantidad = consola.leerEntero("Nueva cantidad: ");
        DetallePedido linea = pedidoService.modificarCantidad(pedido.getId(), idProducto, cantidad);
        consola.exito("Línea actualizada: " + linea);
    }

    private void quitarDePedido(Pedido pedido) throws FoodStoreException {
        consola.mostrarListado(pedido.getDetallesActivos(), "El pedido no tiene líneas.");
        Long idProducto = consola.leerId("Id del producto a quitar: ");
        if (!consola.confirmar("¿Confirmás quitar la línea? Se repondrá el stock en la base")) {
            consola.info("Operación cancelada.");
            return;
        }
        pedidoService.quitarDePedido(pedido.getId(), idProducto);
        consola.exito("Línea quitada y stock repuesto en la BASE DE DATOS.");
    }

    private void eliminarPedido() throws FoodStoreException {
        Pedido pedido = elegirPedido("Id del pedido a eliminar: ");
        if (pedido == null) {
            return;
        }
        if (!consola.confirmar("¿Confirmás la baja lógica del pedido #" + pedido.getId() + "?")) {
            consola.info("Operación cancelada.");
            return;
        }
        boolean stockRepuesto = pedidoService.eliminarPedido(pedido.getId());
        consola.exito("Pedido #" + pedido.getId() + " y sus líneas dados de baja (eliminado = true).");
        if (stockRepuesto) {
            consola.exito("Como el pedido estaba vigente, se repuso el stock en la BASE DE DATOS.");
        }
    }
}
