package integrado.prog2.entities;

/**
 * Línea de un pedido. Vive en memoria como parte (composición) de un Pedido.
 * Conoce su producto (N:1 unidireccional); el producto no conoce sus detalles.
 */
public class DetallePedido extends Base {

    private int cantidad;
    private Double subtotal;
    private final Producto producto;

    public DetallePedido(int cantidad, Double subtotal, Producto producto) {
        super();
        this.cantidad = cantidad;
        this.subtotal = subtotal;
        this.producto = producto;
    }

    /**
     * @return precio por unidad con el que se registró la línea
     */
    public double getPrecioUnitario() {
        return subtotal / cantidad;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Producto getProducto() {
        return producto;
    }

    @Override
    public String toString() {
        return String.format("linea %-3d producto #%-5d %-30s %4d x $%10.2f = $%11.2f",
                getId(), producto.getId(), producto.getNombre(), cantidad, getPrecioUnitario(), subtotal);
    }
}
