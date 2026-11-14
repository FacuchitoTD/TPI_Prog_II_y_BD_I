package integrado.prog2.ui;

import integrado.prog2.config.ConfiguracionPersistencia;
import integrado.prog2.exception.PersistenciaException;
import integrado.prog2.service.ConexionService;

/**
 * Menú principal: diagnóstico de conexión al arrancar y acceso a los cuatro submenús.
 */
public class MenuPrincipal {

    private final Consola consola;
    private final ConexionService conexionService;
    private final Submenu menuCategorias;
    private final Submenu menuProductos;
    private final Submenu menuUsuarios;
    private final Submenu menuPedidos;

    public MenuPrincipal(Consola consola, ConexionService conexionService, Submenu menuCategorias,
                         Submenu menuProductos, Submenu menuUsuarios, Submenu menuPedidos) {
        this.consola = consola;
        this.conexionService = conexionService;
        this.menuCategorias = menuCategorias;
        this.menuProductos = menuProductos;
        this.menuUsuarios = menuUsuarios;
        this.menuPedidos = menuPedidos;
    }

    /**
     * HU-CFG-02 y ciclo principal del programa.
     */
    public void iniciar() {
        diagnosticarConexion();
        int opcion;
        do {
            imprimirMenu();
            opcion = consola.leerEnteroEnRango("Seleccione: ", 0, 5);
            try {
                switch (opcion) {
                    case 1:
                        menuCategorias.mostrar();
                        break;
                    case 2:
                        menuProductos.mostrar();
                        break;
                    case 3:
                        menuUsuarios.mostrar();
                        break;
                    case 4:
                        menuPedidos.mostrar();
                        break;
                    case 5:
                        mostrarConfiguracion();
                        break;
                    default:
                        consola.info("Fin del programa. Los usuarios y pedidos en memoria se descartan;"
                                + " categorías y productos quedan guardados en la base de datos.");
                        break;
                }
            } catch (RuntimeException e) {
                consola.error("Ocurrió un error inesperado (" + e.getClass().getSimpleName() + ": "
                        + e.getMessage() + "). Se vuelve al menú principal.");
            }
        } while (opcion != 0);
    }

    private void imprimirMenu() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("     SISTEMA DE PEDIDOS (FOOD STORE)");
        System.out.println("==============================================");
        System.out.println(" 1) Categorias .......... [BASE DE DATOS]");
        System.out.println(" 2) Productos ........... [BASE DE DATOS]");
        System.out.println(" 3) Usuarios ............ [MEMORIA]");
        System.out.println(" 4) Pedidos y detalles .. [MEMORIA]");
        System.out.println(" 5) Ver configuracion de conexion");
        System.out.println(" 0) Salir");
    }

    private void diagnosticarConexion() {
        consola.info("Verificando la conexión con la base de datos...");
        try {
            conexionService.verificarConexion();
            ConfiguracionPersistencia config = conexionService.obtenerConfiguracion();
            consola.exito("Conexión establecida (unidad " + config.getUnidad() + ", " + config.getUrl() + ").");
        } catch (PersistenciaException e) {
            consola.error("No se pudo conectar con la base de datos.");
            consola.info(e.getMessage());
            consola.info("El programa sigue funcionando: las opciones [MEMORIA] se pueden usar;"
                    + " las de [BASE DE DATOS] van a informar el error hasta que se resuelva.");
        }
    }

    private void mostrarConfiguracion() {
        consola.titulo("CONFIGURACION DE CONEXION (src/META-INF/persistence.xml)");
        try {
            ConfiguracionPersistencia config = conexionService.obtenerConfiguracion();
            consola.info("Unidad de persistencia : " + config.getUnidad());
            consola.info("URL JDBC               : " + config.getUrl());
            consola.info("Driver                 : " + config.getDriver());
            consola.info("Usuario                : " + config.getUsuario());
            conexionService.verificarConexion();
            consola.info("Estado                 : conexión OK");
        } catch (PersistenciaException e) {
            consola.info("Estado                 : SIN CONEXIÓN");
            consola.info(e.getMessage());
        }
    }
}
