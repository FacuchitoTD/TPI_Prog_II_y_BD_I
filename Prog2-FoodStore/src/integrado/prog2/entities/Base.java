package integrado.prog2.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase abstracta raíz del modelo. Aporta a todas las entidades la identidad (id),
 * la marca de baja lógica (eliminado) y la fecha de alta (createdAt).
 * <p>
 * equals y hashCode se definen una sola vez acá y los heredan las cinco entidades:
 * dos objetos son iguales si son de la misma clase y tienen el mismo id.
 */
public abstract class Base {

    private Long id;
    private boolean eliminado;
    private LocalDateTime createdAt;

    protected Base() {
        this.eliminado = false;
        this.createdAt = LocalDateTime.now();
    }

    protected Base(Long id, boolean eliminado, LocalDateTime createdAt) {
        this.id = id;
        this.eliminado = eliminado;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        Base entidad = (Base) otro;
        return id != null && id.equals(entidad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
