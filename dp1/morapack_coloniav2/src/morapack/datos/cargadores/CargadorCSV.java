package morapack.datos.cargadores;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase base abstracta para cargar archivos CSV.
 * Proporciona funcionalidad común para la lectura de archivos CSV.
 */
public abstract class CargadorCSV<T> {

    protected final String rutaArchivo;
    protected final boolean tieneEncabezado;

    /**
     * Constructor del cargador CSV
     * @param rutaArchivo Ruta del archivo CSV
     * @param tieneEncabezado true si el archivo tiene encabezado
     */
    public CargadorCSV(String rutaArchivo, boolean tieneEncabezado) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Ruta del archivo no puede estar vacía");
        }
        this.rutaArchivo = rutaArchivo.trim();
        this.tieneEncabezado = tieneEncabezado;
    }

    /**
     * Carga todos los elementos del archivo CSV
     * @return Lista de elementos cargados
     * @throws IOException si hay error al leer el archivo
     * @throws CargadorException si hay error al procesar los datos
     */
    public List<T> cargarTodos() throws IOException, CargadorException {
        validarArchivo();

        List<T> elementos = new ArrayList<>();
        List<String> lineasErroneas = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(rutaArchivo), StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;

            // Saltar encabezado si existe
            if (tieneEncabezado) {
                reader.readLine();
                numeroLinea++;
            }

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;

                // Saltar líneas vacías
                if (linea.trim().isEmpty()) {
                    continue;
                }

                try {
                    T elemento = procesarLinea(linea, numeroLinea);
                    if (elemento != null) {
                        elementos.add(elemento);
                    }
                } catch (Exception e) {
                    String error = String.format("Línea %d: %s - Error: %s",
                        numeroLinea, linea, e.getMessage());
                    lineasErroneas.add(error);
                }
            }
        }

        // Reportar errores si los hay
        if (!lineasErroneas.isEmpty()) {
            String mensajeError = String.format(
                "Se encontraron %d errores al cargar %s:%n%s",
                lineasErroneas.size(),
                rutaArchivo,
                String.join("\n", lineasErroneas)
            );
            throw new CargadorException(mensajeError);
        }

        return elementos;
    }

    /**
     * Carga un número limitado de elementos
     * @param limite Número máximo de elementos a cargar
     * @return Lista de elementos cargados
     * @throws IOException si hay error al leer el archivo
     * @throws CargadorException si hay error al procesar los datos
     */
    public List<T> cargar(int limite) throws IOException, CargadorException {
        if (limite <= 0) {
            throw new IllegalArgumentException("Límite debe ser mayor a 0");
        }

        validarArchivo();

        List<T> elementos = new ArrayList<>();
        List<String> lineasErroneas = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(rutaArchivo), StandardCharsets.UTF_8)) {

            String linea;
            int numeroLinea = 0;
            int elementosCargados = 0;

            // Saltar encabezado si existe
            if (tieneEncabezado) {
                reader.readLine();
                numeroLinea++;
            }

            while ((linea = reader.readLine()) != null && elementosCargados < limite) {
                numeroLinea++;

                // Saltar líneas vacías
                if (linea.trim().isEmpty()) {
                    continue;
                }

                try {
                    T elemento = procesarLinea(linea, numeroLinea);
                    if (elemento != null) {
                        elementos.add(elemento);
                        elementosCargados++;
                    }
                } catch (Exception e) {
                    String error = String.format("Línea %d: %s - Error: %s",
                        numeroLinea, linea, e.getMessage());
                    lineasErroneas.add(error);
                }
            }
        }

        // Reportar errores si los hay, pero no fallar completamente
        if (!lineasErroneas.isEmpty()) {
            System.err.printf("Advertencias al cargar %s:%n%s%n",
                rutaArchivo, String.join("\n", lineasErroneas));
        }

        return elementos;
    }

    /**
     * Cuenta el total de líneas válidas en el archivo
     * @return Número de líneas válidas (excluyendo encabezado y líneas vacías)
     * @throws IOException si hay error al leer el archivo
     * @throws CargadorException si el archivo no es válido
     */
    public int contarLineas() throws IOException, CargadorException {
        validarArchivo();

        int contador = 0;
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(rutaArchivo), StandardCharsets.UTF_8)) {

            String linea;

            // Saltar encabezado si existe
            if (tieneEncabezado) {
                reader.readLine();
            }

            while ((linea = reader.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    contador++;
                }
            }
        }

        return contador;
    }

    /**
     * Valida que el archivo existe y es legible
     * @throws CargadorException si el archivo no es válido
     */
    protected void validarArchivo() throws CargadorException {
        Path path = Paths.get(rutaArchivo);

        if (!Files.exists(path)) {
            throw new CargadorException("El archivo no existe: " + rutaArchivo);
        }

        if (!Files.isReadable(path)) {
            throw new CargadorException("El archivo no es legible: " + rutaArchivo);
        }

        if (Files.isDirectory(path)) {
            throw new CargadorException("La ruta es un directorio, no un archivo: " + rutaArchivo);
        }
    }

    /**
     * Obtiene información del archivo
     * @return String con información del archivo
     */
    public String getInformacionArchivo() {
        try {
            Path path = Paths.get(rutaArchivo);
            long tamanio = Files.size(path);
            int lineas = contarLineas();

            return String.format("Archivo: %s%nTamaño: %d bytes%nLíneas: %d%nEncabezado: %s",
                path.getFileName(), tamanio, lineas, tieneEncabezado ? "Sí" : "No");

        } catch (IOException | CargadorException e) {
            return String.format("Archivo: %s (Error al obtener información: %s)",
                rutaArchivo, e.getMessage());
        }
    }

    /**
     * Método abstracto que deben implementar las clases derivadas
     * para procesar cada línea del CSV
     * @param linea Línea del CSV a procesar
     * @param numeroLinea Número de línea para reportes de error
     * @return Elemento procesado o null si debe saltarse
     * @throws Exception si hay error al procesar la línea
     */
    protected abstract T procesarLinea(String linea, int numeroLinea) throws Exception;

    // Getters
    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public boolean tieneEncabezado() {
        return tieneEncabezado;
    }
}