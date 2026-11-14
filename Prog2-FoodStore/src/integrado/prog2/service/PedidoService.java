package integrado.prog2.service;

import integrado.prog2.entities.DetallePedido;
import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Producto;
import integrado.prog2.entities.Usuario;
import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.exception.EntidadNoEncontradaException;
import integrado.prog2.exception.OperacionNoPermitidaException;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.exception.StockInvalidoException;
import integrado.prog2.exception.ValidacionException;
import integrado.prog2.repository.PedidoRepository;
import integrado.prog2.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso de pedidos y detalles (épica 4).
 * Los pedidos viven en memoria, pero cada movimiento de stock se hace primero en la base
 * (en una transacción) y sólo si salió bien se modifica la memoria. Así, ante un error,
 * ninguno de los dos almacenamientos queda inconsistente.
 */
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;

    public PedidoService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
                         UsuarioService usuarioService, ProductoService productoService) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    /**
     * HU-PED-01: recorre Usuario → Pedidos. Incluye usuarios dados de baja para conservar el historial.
     *
     * @return cada usuario con sus pedidos activos (sólo usuarios que tengan alguno), en orden de alta
     */
    public Map<Usuario, List<Pedido>> listarPedidosPorUsuario() {
        Map<Usuario, List<Pedido>> resultado = new LinkedHashMap<>();
        for (Usuario usuario : usuarioRepository.listarTodos()) {
            List<Pedido> activos = usuario.getPedidosActivos();
            if (!activos.isEmpty()) {
                resultado.put(usuario, activos);
            }
        }
        return resultado;
    }

    /**
     * HU-PED-01 con filtro por usuario. Acepta usuarios dados de baja (consulta de historial).
     */
    public List<Pedido> listarPedidosDeUsuario(Long idUsuario) throws EntidadNoEncontradaException {
        Usuario usuario = usuarioRepository.buscarPorId(idUsuario);
        if (usuario == null) {
            throw new EntidadNoEncontradaException("No existe un usuario con id " + idUsuario + ".");
        }
        return usuario.getPedidosActivos();
    }

    /**
     * HU-PED-02, paso 1: el pedido no puede crearse sin un usuario activo.
     */
    public Usuario usuarioParaNuevoPedido(Long idUsuario) throws EntidadNoEncontradaException {
        return usuarioService.buscarActivo(idUsuario);
    }

    /**
     * HU-PED-02, paso 2: pedido en armado. Todavía no está registrado ni toca la base.
     */
    public Pedido nuevoPedido(FormaPago formaPago) throws ValidacionException {
        if (formaPago == null) {
            throw new ValidacionException("Debe indicarse la forma de pago.");
        }
        return new Pedido(formaPago);
    }

    /**
     * HU-PED-02, paso 3: agrega una línea al pedido en armado usando addDetallePedido.
     * Valida que la cantidad total de ese producto no supere el stock actual de la base.
     *
     * @return la línea del producto tal como quedó en el pedido
     */
    public DetallePedido agregarAlPedidoEnArmado(Pedido pedido, Long idProducto, int cantidad)
            throws StockInvalidoException, EntidadNoEncontradaException, OperacionNoPermitidaException,
            PersistenciaException {
        Validaciones.cantidadValida(cantidad);
        Producto producto = productoService.buscarParaPedido(idProducto);
        DetallePedido existente = pedido.findeDetallePedidoByProducto(producto);
        int yaCargado = existente == null ? 0 : existente.getCantidad();
        if (yaCargado + cantidad > producto.getStock()) {
            throw new StockInvalidoException("Stock insuficiente para '" + producto.getNombre() + "': hay "
                    + producto.getStock() + (yaCargado > 0 ? ", ya cargaste " + yaCargado : "")
                    + " y pediste " + cantidad + ".");
        }
        pedido.addDetallePedido(cantidad, cantidad * producto.getPrecio(), producto);
        return pedido.findeDetallePedidoByProducto(producto);
    }

    /**
     * HU-PED-02: quita una línea del pedido en armado (todavía no hubo movimiento de stock).
     */
    public void quitarDelPedidoEnArmado(Pedido pedido, Long idProducto) throws EntidadNoEncontradaException {
        DetallePedido detalle = buscarLinea(pedido, idProducto);
        pedido.deleteDetallePedidoByProducto(detalle.getProducto());
    }

    /**
     * HU-PED-02, paso final: calcula el total con Calculable, descuenta el stock de todas las líneas
     * en una única transacción y, sólo si el commit salió bien, registra el pedido dentro del usuario.
     * Si falta stock, la base hace rollback y el pedido no se registra.
     */
    public void confirmarPedido(Usuario usuario, Pedido pedido) throws OperacionNoPermitidaException,
            ValidacionException, StockInvalidoException, PersistenciaException {
        if (usuario == null || usuario.isEliminado()) {
            throw new OperacionNoPermitidaException("No se puede registrar un pedido sin un usuario activo.");
        }
        List<DetallePedido> lineas = pedido.getDetallesActivos();
        if (lineas.isEmpty()) {
            throw new ValidacionException("El pedido no tiene productos.");
        }
        pedido.calcularTotal();
        productoService.descontarStock(cantidadesPorProducto(lineas));
        pedidoRepository.registrar(usuario, pedido);
    }

    /**
     * @return el pedido activo con ese id
     */
    public Pedido buscarPedido(Long idPedido) throws EntidadNoEncontradaException {
        Pedido pedido = pedidoRepository.buscarPorId(idPedido);
        if (pedido == null || pedido.isEliminado()) {
            throw new EntidadNoEncontradaException("No existe un pedido activo con id " + idPedido + ".");
        }
        return pedido;
    }

    /**
     * @return el usuario que contiene al pedido (el pedido no lo conoce)
     */
    public Usuario duenoDe(Pedido pedido) {
        return pedidoRepository.buscarDueno(pedido);
    }

    /**
     * HU-PED-03: los parámetros null conservan el valor anterior.
     * Al pasar a CANCELADO se repone el stock en la base antes de cambiar el estado en memoria.
     *
     * @return el pedido actualizado
     */
    public Pedido actualizarEstadoYFormaPago(Long idPedido, Estado nuevoEstado, FormaPago nuevaFormaPago)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, PersistenciaException {
        Pedido pedido = buscarPedidoModificable(idPedido);
        if (nuevoEstado == Estado.CANCELADO) {
            productoService.reponerStock(cantidadesPorProducto(pedido.getDetallesActivos()));
        }
        if (nuevoEstado != null) {
            pedido.setEstado(nuevoEstado);
        }
        if (nuevaFormaPago != null) {
            pedido.setFormaPago(nuevaFormaPago);
        }
        return pedido;
    }

    /**
     * Consulta de una línea de un pedido registrado (usa findeDetallePedidoByProducto).
     */
    public DetallePedido buscarDetalle(Long idPedido, Long idProducto) throws EntidadNoEncontradaException {
        return buscarLinea(buscarPedido(idPedido), idProducto);
    }

    /**
     * Alta de una línea en un pedido vigente: primero descuenta en la base, después agrega en memoria.
     *
     * @return la línea del producto tal como quedó en el pedido
     */
    public DetallePedido agregarAPedido(Long idPedido, Long idProducto, int cantidad)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, StockInvalidoException,
            PersistenciaException {
        Pedido pedido = buscarPedidoModificable(idPedido);
        Validaciones.cantidadValida(cantidad);
        Producto producto = productoService.buscarParaPedido(idProducto);
        productoService.descontarStock(cantidadUnica(idProducto, cantidad));
        pedido.addDetallePedido(cantidad, cantidad * producto.getPrecio(), producto);
        return pedido.findeDetallePedidoByProducto(producto);
    }

    /**
     * Modificación de la cantidad de una línea de un pedido vigente.
     * Si aumenta se descuenta la diferencia en la base; si disminuye se repone.
     * El precio unitario se mantiene el que se registró en la línea.
     *
     * @return la línea modificada
     */
    public DetallePedido modificarCantidad(Long idPedido, Long idProducto, int nuevaCantidad)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, StockInvalidoException,
            PersistenciaException {
        Pedido pedido = buscarPedidoModificable(idPedido);
        Validaciones.cantidadValida(nuevaCantidad);
        DetallePedido detalle = buscarLinea(pedido, idProducto);
        int diferencia = nuevaCantidad - detalle.getCantidad();
        if (diferencia > 0) {
            productoService.descontarStock(cantidadUnica(idProducto, diferencia));
        } else if (diferencia < 0) {
            productoService.reponerStock(cantidadUnica(idProducto, -diferencia));
        }
        detalle.setSubtotal(detalle.getPrecioUnitario() * nuevaCantidad);
        detalle.setCantidad(nuevaCantidad);
        pedido.calcularTotal();
        return detalle;
    }

    /**
     * Baja de una línea de un pedido vigente: repone el stock en la base y da de baja la línea en memoria.
     * No se permite dejar un pedido sin líneas (para eso está eliminar o cancelar el pedido).
     */
    public void quitarDePedido(Long idPedido, Long idProducto)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException, PersistenciaException {
        Pedido pedido = buscarPedidoModificable(idPedido);
        DetallePedido detalle = buscarLinea(pedido, idProducto);
        if (pedido.getDetallesActivos().size() == 1) {
            throw new OperacionNoPermitidaException(
                    "Es la única línea del pedido. Para anularlo, cancelalo o eliminalo.");
        }
        productoService.reponerStock(cantidadUnica(idProducto, detalle.getCantidad()));
        pedido.deleteDetallePedidoByProducto(detalle.getProducto());
    }

    /**
     * HU-PED-04: baja lógica del pedido y sus líneas. Si estaba vigente, repone el stock en la base.
     *
     * @return true si se repuso stock
     */
    public boolean eliminarPedido(Long idPedido) throws EntidadNoEncontradaException, PersistenciaException {
        Pedido pedido = buscarPedido(idPedido);
        boolean reponer = pedido.isVigente();
        if (reponer) {
            productoService.reponerStock(cantidadesPorProducto(pedido.getDetallesActivos()));
        }
        pedido.darDeBaja();
        return reponer;
    }

    private Pedido buscarPedidoModificable(Long idPedido)
            throws EntidadNoEncontradaException, OperacionNoPermitidaException {
        Pedido pedido = buscarPedido(idPedido);
        if (!pedido.isVigente()) {
            throw new OperacionNoPermitidaException("El pedido #" + idPedido + " está " + pedido.getEstado()
                    + " y ya no admite modificaciones.");
        }
        return pedido;
    }

    private DetallePedido buscarLinea(Pedido pedido, Long idProducto) throws EntidadNoEncontradaException {
        Producto referencia = new Producto();
        referencia.setId(idProducto);
        DetallePedido detalle = pedido.findeDetallePedidoByProducto(referencia);
        if (detalle == null) {
            throw new EntidadNoEncontradaException("El pedido no tiene una línea activa del producto #" + idProducto + ".");
        }
        return detalle;
    }

    private Map<Long, Integer> cantidadesPorProducto(List<DetallePedido> lineas) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePedido linea : lineas) {
            Long idProducto = linea.getProducto().getId();
            cantidades.put(idProducto, cantidades.getOrDefault(idProducto, 0) + linea.getCantidad());
        }
        return cantidades;
    }

    private Map<Long, Integer> cantidadUnica(Long idProducto, int cantidad) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        cantidades.put(idProducto, cantidad);
        return cantidades;
    }
}
