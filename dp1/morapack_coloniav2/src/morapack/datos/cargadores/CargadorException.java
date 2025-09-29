package morapack.datos.cargadores;

/**
 * Excepción específica para errores durante la carga de archivos CSV.
 */
public class CargadorException extends Exception {

    /**
     * Constructor con mensaje
     * @param mensaje Descripción del error
     */
    public CargadorException(String mensaje) {
        super(mensaje);
    }

    /**
     * Constructor con mensaje y causa
     * @param mensaje Descripción del error
     * @param causa Excepción que causó el error
     */
    public CargadorException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}