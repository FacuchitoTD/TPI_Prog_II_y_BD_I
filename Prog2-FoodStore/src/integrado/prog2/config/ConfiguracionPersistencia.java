package integrado.prog2.config;

import integrado.prog2.exception.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Lee los datos de conexión de META-INF/persistence.xml con el parser DOM del JDK.
 * Se lee una sola vez y se reutiliza (instancia única).
 */
public final class ConfiguracionPersistencia {

    private static final String RUTA_ARCHIVO = "META-INF/persistence.xml";
    private static final String PROP_DRIVER = "jakarta.persistence.jdbc.driver";
    private static final String PROP_URL = "jakarta.persistence.jdbc.url";
    private static final String PROP_USUARIO = "jakarta.persistence.jdbc.user";
    private static final String PROP_PASSWORD = "jakarta.persistence.jdbc.password";

    private static ConfiguracionPersistencia instancia;

    private final String unidad;
    private final String driver;
    private final String url;
    private final String usuario;
    private final String password;

    private ConfiguracionPersistencia(String unidad, String driver, String url, String usuario, String password) {
        this.unidad = unidad;
        this.driver = driver;
        this.url = url;
        this.usuario = usuario;
        this.password = password;
    }

    public static ConfiguracionPersistencia getInstancia() throws PersistenciaException {
        if (instancia == null) {
            instancia = leerArchivo();
        }
        return instancia;
    }

    private static ConfiguracionPersistencia leerArchivo() throws PersistenciaException {
        ClassLoader cargador = ConfiguracionPersistencia.class.getClassLoader();
        try (InputStream entrada = cargador.getResourceAsStream(RUTA_ARCHIVO)) {
            if (entrada == null) {
                throw new PersistenciaException("No se encontró " + RUTA_ARCHIVO
                        + " en el classpath. ¿Copiaste src/META-INF a bin/META-INF al compilar?");
            }
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document documento = parser.parse(entrada);

            Element unidadXml = (Element) documento.getElementsByTagName("persistence-unit").item(0);
            if (unidadXml == null) {
                throw new PersistenciaException("persistence.xml no define ninguna <persistence-unit>.");
            }

            Map<String, String> propiedades = new HashMap<>();
            NodeList nodos = unidadXml.getElementsByTagName("property");
            for (int i = 0; i < nodos.getLength(); i++) {
                Element propiedad = (Element) nodos.item(i);
                propiedades.put(propiedad.getAttribute("name"), propiedad.getAttribute("value"));
            }

            return new ConfiguracionPersistencia(
                    unidadXml.getAttribute("name"),
                    obligatoria(propiedades, PROP_DRIVER),
                    obligatoria(propiedades, PROP_URL),
                    obligatoria(propiedades, PROP_USUARIO),
                    obligatoria(propiedades, PROP_PASSWORD));

        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new PersistenciaException("No se pudo leer " + RUTA_ARCHIVO
                    + " (¿está bien formado el XML?): " + e.getMessage(), e);
        }
    }

    private static String obligatoria(Map<String, String> propiedades, String nombre) throws PersistenciaException {
        if (!propiedades.containsKey(nombre)) {
            throw new PersistenciaException("Falta la propiedad '" + nombre + "' en persistence.xml.");
        }
        return propiedades.get(nombre);
    }

    public String getUnidad() {
        return unidad;
    }

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPassword() {
        return password;
    }
}
