package integrado.prog2.service;

import integrado.prog2.config.ConfiguracionPersistencia;
import integrado.prog2.dao.ConexionDB;
import integrado.prog2.exception.PersistenciaException;

/**
 * Casos de uso de configuración (épica 5): diagnóstico de arranque y consulta de la conexión activa.
 */
public class ConexionService {

    /**
     * HU-CFG-02: abre y cierra una conexión de prueba.
     *
     * @throws PersistenciaException con la causa probable y los pasos a revisar
     */
    public void verificarConexion() throws PersistenciaException {
        ConexionDB.probarConexion();
    }

    /**
     * HU-CFG-01: datos leídos de META-INF/persistence.xml.
     */
    public ConfiguracionPersistencia obtenerConfiguracion() throws PersistenciaException {
        return ConfiguracionPersistencia.getInstancia();
    }
}
