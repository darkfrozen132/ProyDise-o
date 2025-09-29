package morapack.datos.modelos;

/**
 * Enumeración que representa los tres continentes soportados
 * por el sistema de distribución MoraPack.
 */
public enum Continente {

    SAM("SAM", "América del Sur"),
    EUR("EUR", "Europa"),
    ASI("ASI", "Asia");

    private final String codigo;
    private final String nombre;

    /**
     * Constructor del enum Continente
     * @param codigo Código de 3 letras del continente
     * @param nombre Nombre completo del continente
     */
    Continente(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    /**
     * Obtiene el código del continente
     * @return Código de 3 letras (SAM, EUR, ASI)
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Obtiene el nombre completo del continente
     * @return Nombre descriptivo del continente
     */
    public String getNombre() {
        return nombre;
    }


    /**
     * Calcula el plazo de entrega entre dos continentes
     * @param origen Continente de origen
     * @param destino Continente de destino
     * @return Días de plazo (2 si mismo continente, 3 si diferente)
     */
    public static int calcularPlazoEntrega(Continente origen, Continente destino) {
        if (origen == destino) {
            return 2; // Mismo continente
        } else {
            return 3; // Diferente continente
        }
    }

    /**
     * Obtiene un continente por su código
     * @param codigo Código de 3 letras (SAM, EUR, ASI)
     * @return Continente correspondiente
     * @throws IllegalArgumentException si el código no es válido
     */
    public static Continente porCodigo(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("Código de continente no puede ser null");
        }

        for (Continente continente : values()) {
            if (continente.codigo.equalsIgnoreCase(codigo.trim())) {
                return continente;
            }
        }

        throw new IllegalArgumentException("Código de continente no válido: " + codigo);
    }

    /**
     * Verifica si dos continentes son el mismo
     * @param otro Otro continente a comparar
     * @return true si son el mismo continente
     */
    public boolean esMismoContinente(Continente otro) {
        return this == otro;
    }

    /**
     * Verifica si el código de continente es válido
     * @param codigo Código a validar
     * @return true si el código es válido
     */
    public static boolean esCodigoValido(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return false;
        }

        try {
            porCodigo(codigo);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene todos los códigos de continente válidos
     * @return Array con todos los códigos válidos
     */
    public static String[] getCodigosValidos() {
        Continente[] continentes = values();
        String[] codigos = new String[continentes.length];

        for (int i = 0; i < continentes.length; i++) {
            codigos[i] = continentes[i].codigo;
        }

        return codigos;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", nombre, codigo);
    }
}