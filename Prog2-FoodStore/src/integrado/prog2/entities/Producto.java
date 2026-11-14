package integrado.prog2.entities;

import java.time.LocalDateTime;

/**
 * Producto del catálogo. Se persiste en la tabla producto.
 * No tiene atributo de categoría: la clave foránea categoria_id sólo existe en la base
 * y la maneja ProductoDAO, para respetar la relación unidireccional del UML.
 */
public class Producto extends Base {

    private String nombre;
    private Double precio;
    private String descripcion;
    private int stock;
    private String imagen;
    private Boolean disponible;

    public Producto() {
        super();
    }

    public Producto(String nombre, Double precio, String descripcion, int stock, String imagen, Boolean disponible) {
        super();
        this.nombre = nombre;
        this.precio = precio;
        this.descripcion = descripcion;
        this.stock = stock;
        this.imagen = imagen;
        this.disponible = disponible;
    }

    public Producto(Long id, boolean eliminado, LocalDateTime createdAt, String nombre, Double precio,
                    String descripcion, int stock, String imagen, Boolean disponible) {
        super(id, eliminado, createdAt);
        this.nombre = nombre;
        this.precio = precio;
        this.descripcion = descripcion;
        this.stock = stock;
        this.imagen = imagen;
        this.disponible = disponible;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    /**
     * @return true sólo si el producto está marcado como disponible
     */
    public boolean estaDisponible() {
        return Boolean.TRUE.equals(disponible);
    }

    @Override
    public String toString() {
        return String.format("#%-5d %-30s $%10.2f   stock: %-5d %s",
                getId(), nombre, precio, stock, estaDisponible() ? "disponible" : "NO disponible");
    }
}
