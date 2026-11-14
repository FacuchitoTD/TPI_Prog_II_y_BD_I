package integrado.prog2.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Categoría del catálogo. Se persiste en la tabla categoria.
 * Conoce sus productos (agregación 1:N); el producto no conoce a su categoría.
 */
public class Categoria extends Base {

    private String nombre;
    private String descripcion;
    private final List<Producto> productos;

    public Categoria() {
        super();
        this.productos = new ArrayList<>();
    }

    public Categoria(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Categoria(Long id, boolean eliminado, LocalDateTime createdAt, String nombre, String descripcion) {
        super(id, eliminado, createdAt);
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.productos = new ArrayList<>();
    }

    /**
     * Agrega un producto a la agregación, evitando nulos y repetidos.
     *
     * @param producto producto leído de la base para esta categoría
     */
    public void agregarProducto(Producto producto) {
        if (producto != null && !productos.contains(producto)) {
            productos.add(producto);
        }
    }

    /**
     * @return vista de sólo lectura de los productos cargados
     */
    public List<Producto> getProductos() {
        return Collections.unmodifiableList(productos);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return String.format("#%-5d %-25s %s", getId(), nombre, descripcion);
    }
}
