package integrado.prog2.repository;

import integrado.prog2.entities.Pedido;
import integrado.prog2.entities.Usuario;

/**
 * Gestión en memoria de los pedidos. No tiene una colección propia:
 * cada pedido se guarda dentro de su usuario y se llega a él recorriendo Usuario → Pedidos.
 */
public class PedidoRepository {

    private final UsuarioRepository usuarioRepository;
    private long proximoId = 1;

    public PedidoRepository(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Asigna un id nuevo al pedido y lo agrega a la lista de pedidos del usuario.
     */
    public void registrar(Usuario usuario, Pedido pedido) {
        pedido.setId(proximoId);
        proximoId++;
        usuario.agregarPedido(pedido);
    }

    /**
     * @return el pedido con ese id (esté o no eliminado), o null si no existe
     */
    public Pedido buscarPorId(Long id) {
        for (Usuario usuario : usuarioRepository.listarTodos()) {
            for (Pedido pedido : usuario.getPedidos()) {
                if (pedido.getId().equals(id)) {
                    return pedido;
                }
            }
        }
        return null;
    }

    /**
     * Como el pedido no conoce a su usuario, se busca qué usuario lo contiene.
     *
     * @return el usuario dueño del pedido, o null si no se encuentra
     */
    public Usuario buscarDueno(Pedido pedido) {
        for (Usuario usuario : usuarioRepository.listarTodos()) {
            if (usuario.getPedidos().contains(pedido)) {
                return usuario;
            }
        }
        return null;
    }
}
