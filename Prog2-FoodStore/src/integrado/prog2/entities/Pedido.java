package integrado.prog2.entities;

import integrado.prog2.enums.Estado;
import integrado.prog2.enums.FormaPago;
import integrado.prog2.interfaces.Calculable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pedido de un usuario. Vive sólo en memoria, dentro de su Usuario.
 * Compone sus detalles (1:N): los crea, los busca y los da de baja él mismo.
 * Implementa Calculable para mantener actualizado su total.
 */
public class Pedido extends Base implements Calculable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalDate fecha;
    private Estado estado;
    private Double total;
    private FormaPago formaPago;
    private final List<DetallePedido> detalles;

    public Pedido(FormaPago formaPago) {
        super();
        this.fecha = LocalDate.now();
        this.estado = Estado.PENDIENTE;
        this.total = 0.0;
        this.formaPago = formaPago;
        this.detalles = new ArrayList<>();
    }

    /**
     * Agrega una línea al pedido. Si el producto ya figura en una línea activa,
     * suma cantidad y subtotal a esa línea en lugar de crear otra. Luego recalcula el total.
     *
     * @param cantidad unidades a agregar, mayor a cero
     * @param subtotal importe de esas unidades (cantidad * precio del producto)
     * @param producto producto leído de la base
     */
    public void addDetallePedido(int cantidad, Double subtotal, Producto producto) {
        if (producto == null || cantidad <= 0 || subtotal == null || subtotal < 0) {
            throw new IllegalArgumentException("Datos de detalle inválidos.");
        }
        DetallePedido existente = findeDetallePedidoByProducto(producto);
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            existente.setSubtotal(existente.getSubtotal() + subtotal);
        } else {
            DetallePedido nuevo = new DetallePedido(cantidad, subtotal, producto);
            nuevo.setId((long) detalles.size() + 1);
            detalles.add(nuevo);
        }
        calcularTotal();
    }

    /**
     * Busca la línea activa de un producto. La comparación usa equals de Base (por id),
     * así que alcanza con un Producto que tenga el id buscado.
     *
     * @param producto producto a buscar
     * @return la línea activa de ese producto, o null si no hay ninguna
     */
    public DetallePedido findeDetallePedidoByProducto(Producto producto) {
        for (DetallePedido detalle : detalles) {
            if (!detalle.isEliminado() && detalle.getProducto().equals(producto)) {
                return detalle;
            }
        }
        return null;
    }

    /**
     * Da de baja lógica la línea activa del producto indicado y recalcula el total.
     *
     * @param producto producto cuya línea se quita
     */
    public void deleteDetallePedidoByProducto(Producto producto) {
        DetallePedido detalle = findeDetallePedidoByProducto(producto);
        if (detalle != null) {
            detalle.setEliminado(true);
            calcularTotal();
        }
    }

    /**
     * Total = suma de los subtotales de las líneas activas.
     */
    @Override
    public void calcularTotal() {
        double suma = 0;
        for (DetallePedido detalle : getDetallesActivos()) {
            suma += detalle.getSubtotal();
        }
        this.total = suma;
    }

    /**
     * Baja lógica del pedido y, por composición, de todas sus líneas.
     */
    public void darDeBaja() {
        setEliminado(true);
        for (DetallePedido detalle : detalles) {
            detalle.setEliminado(true);
        }
    }

    /**
     * @return true si el pedido sigue en curso (PENDIENTE o CONFIRMADO) y admite cambios
     */
    public boolean isVigente() {
        return estado == Estado.PENDIENTE || estado == Estado.CONFIRMADO;
    }

    /**
     * @return líneas que no fueron dadas de baja
     */
    public List<DetallePedido> getDetallesActivos() {
        List<DetallePedido> activos = new ArrayList<>();
        for (DetallePedido detalle : detalles) {
            if (!detalle.isEliminado()) {
                activos.add(detalle);
            }
        }
        return activos;
    }

    /**
     * @return vista de sólo lectura de todas las líneas, incluidas las dadas de baja
     */
    public List<DetallePedido> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Double getTotal() {
        return total;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    /**
     * @return encabezado del pedido seguido de una línea por cada detalle activo
     */
    public String toStringConDetalles() {
        StringBuilder texto = new StringBuilder(toString());
        List<DetallePedido> activos = getDetallesActivos();
        if (activos.isEmpty()) {
            texto.append(System.lineSeparator()).append("      (sin líneas)");
        }
        for (DetallePedido detalle : activos) {
            texto.append(System.lineSeparator()).append("      ").append(detalle);
        }
        return texto.toString();
    }

    @Override
    public String toString() {
        String numero = getId() == null ? "(sin confirmar)" : "#" + getId();
        return String.format("Pedido %s | %s | %-10s | %-13s | total $%.2f",
                numero, fecha.format(FORMATO_FECHA), estado, formaPago, total);
    }
}
