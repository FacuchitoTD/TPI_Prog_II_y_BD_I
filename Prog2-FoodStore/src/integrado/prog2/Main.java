package integrado.prog2;

import integrado.prog2.dao.CategoriaDAO;
import integrado.prog2.dao.ProductoDAO;
import integrado.prog2.repository.PedidoRepository;
import integrado.prog2.repository.UsuarioRepository;
import integrado.prog2.service.CategoriaService;
import integrado.prog2.service.ConexionService;
import integrado.prog2.service.PedidoService;
import integrado.prog2.service.ProductoService;
import integrado.prog2.service.UsuarioService;
import integrado.prog2.ui.Consola;
import integrado.prog2.ui.MenuCategorias;
import integrado.prog2.ui.MenuPedidos;
import integrado.prog2.ui.MenuPrincipal;
import integrado.prog2.ui.MenuProductos;
import integrado.prog2.ui.MenuUsuarios;
import java.util.Scanner;

/**
 * Punto de entrada. Sólo arma los objetos de cada capa, los conecta entre sí y arranca el menú.
 * No contiene SQL ni reglas de negocio.
 */
public class Main {

    public static void main(String[] args) {
        CategoriaDAO categoriaDAO = new CategoriaDAO();
        ProductoDAO productoDAO = new ProductoDAO();
        UsuarioRepository usuarioRepository = new UsuarioRepository();
        PedidoRepository pedidoRepository = new PedidoRepository(usuarioRepository);

        CategoriaService categoriaService = new CategoriaService(categoriaDAO, productoDAO);
        ProductoService productoService = new ProductoService(productoDAO, categoriaDAO);
        UsuarioService usuarioService = new UsuarioService(usuarioRepository);
        PedidoService pedidoService = new PedidoService(pedidoRepository, usuarioRepository, usuarioService, productoService);
        ConexionService conexionService = new ConexionService();

        Consola consola = new Consola(new Scanner(System.in));
        MenuPrincipal menu = new MenuPrincipal(consola, conexionService,
                new MenuCategorias(consola, categoriaService),
                new MenuProductos(consola, productoService, categoriaService),
                new MenuUsuarios(consola, usuarioService),
                new MenuPedidos(consola, pedidoService, usuarioService, productoService));
        menu.iniciar();
    }
}
