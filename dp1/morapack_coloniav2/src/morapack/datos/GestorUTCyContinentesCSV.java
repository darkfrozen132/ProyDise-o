package morapack.datos;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de conversiones UTC y clasificación continental 
 * según datos exactos de aeropuertos_simple.csv
 */
public class GestorUTCyContinentesCSV {
    
    // Mapas para gestión de aeropuertos
    private static final Map<String, Integer> HUSOS_HORARIOS = new HashMap<>();
    private static final Map<String, String> CONTINENTES = new HashMap<>();
    
    static {
        configurarAeropuertos();
    }
    
    /**
     * Configura todos los aeropuertos con datos exactos del CSV aeropuertos_simple.csv
     */
    private static void configurarAeropuertos() {
        // SUDAMÉRICA (SAM) - 10 aeropuertos
        configurarAeropuerto("SKBO", -5, "SAM"); // Bogotá, Colombia
        configurarAeropuerto("SEQM", -5, "SAM"); // Quito, Ecuador
        configurarAeropuerto("SVMI", -4, "SAM"); // Caracas, Venezuela
        configurarAeropuerto("SBBR", -3, "SAM"); // Brasilia, Brasil
        configurarAeropuerto("SPIM", -5, "SAM"); // Lima, Perú (sede)
        configurarAeropuerto("SLLP", -4, "SAM"); // La Paz, Bolivia
        configurarAeropuerto("SCEL", -3, "SAM"); // Santiago de Chile, Chile
        configurarAeropuerto("SABE", -3, "SAM"); // Buenos Aires, Argentina
        configurarAeropuerto("SGAS", -4, "SAM"); // Asunción, Paraguay
        configurarAeropuerto("SUAA", -3, "SAM"); // Montevideo, Uruguay
        
        // EUROPA (EUR) - 10 aeropuertos
        configurarAeropuerto("LATI", +2, "EUR"); // Tirana, Albania
        configurarAeropuerto("EDDI", +2, "EUR"); // Berlín, Alemania
        configurarAeropuerto("LOWW", +2, "EUR"); // Viena, Austria
        configurarAeropuerto("EBCI", +2, "EUR"); // Bruselas, Bélgica (sede)
        configurarAeropuerto("UMMS", +3, "EUR"); // Minsk, Bielorrusia
        configurarAeropuerto("LBSF", +3, "EUR"); // Sofía, Bulgaria
        configurarAeropuerto("LKPR", +2, "EUR"); // Praga, República Checa
        configurarAeropuerto("LDZA", +2, "EUR"); // Zagreb, Croacia
        configurarAeropuerto("EKCH", +2, "EUR"); // Copenhague, Dinamarca
        configurarAeropuerto("EHAM", +2, "EUR"); // Ámsterdam, Holanda
        
        // ASIA (ASI) - 10 aeropuertos
        configurarAeropuerto("VIDP", +5, "ASI"); // Delhi, India
        configurarAeropuerto("OSDI", +3, "ASI"); // Damasco, Siria
        configurarAeropuerto("OERK", +3, "ASI"); // Riad, Arabia Saudita
        configurarAeropuerto("OMDB", +4, "ASI"); // Dubai, Emiratos A.U.
        configurarAeropuerto("OAKB", +4, "ASI"); // Kabul, Afganistán
        configurarAeropuerto("OOMS", +4, "ASI"); // Mascate, Omán
        configurarAeropuerto("OYSN", +3, "ASI"); // Sana, Yemen
        configurarAeropuerto("OPKC", +5, "ASI"); // Karachi, Pakistán
        configurarAeropuerto("UBBB", +2, "ASI"); // Baku, Azerbaiyán (sede)  
        configurarAeropuerto("OJAI", +3, "ASI"); // Amán, Jordania
    }
    
    /**
     * Configura un aeropuerto con su huso horario y continente
     */
    private static void configurarAeropuerto(String codigo, int husoHorario, String continente) {
        HUSOS_HORARIOS.put(codigo, husoHorario);
        CONTINENTES.put(codigo, continente);
    }
    
    /**
     * Convierte hora local a UTC
     */
    public static LocalTime convertirAUTC(String codigoAeropuerto, LocalTime horaLocal) {
        Integer huso = HUSOS_HORARIOS.get(codigoAeropuerto);
        if (huso == null) {
            throw new IllegalArgumentException("Aeropuerto no configurado: " + codigoAeropuerto);
        }
        
        return horaLocal.minusHours(huso);
    }
    
    /**
     * Convierte hora UTC a local
     */
    public static LocalTime convertirDeUTC(String codigoAeropuerto, LocalTime horaUTC) {
        Integer huso = HUSOS_HORARIOS.get(codigoAeropuerto);
        if (huso == null) {
            throw new IllegalArgumentException("Aeropuerto no configurado: " + codigoAeropuerto);
        }
        
        return horaUTC.plusHours(huso);
    }
    
    /**
     * Obtiene el continente de un aeropuerto
     */
    public static String obtenerContinente(String codigoAeropuerto) {
        String continente = CONTINENTES.get(codigoAeropuerto);
        if (continente == null) {
            throw new IllegalArgumentException("Aeropuerto no configurado: " + codigoAeropuerto);
        }
        return continente;
    }
    
    /**
     * Determina si dos aeropuertos están en el mismo continente
     */
    public static boolean mismoContienente(String aeropuerto1, String aeropuerto2) {
        return obtenerContinente(aeropuerto1).equals(obtenerContinente(aeropuerto2));
    }
    
    /**
     * Calcula los días de plazo según distancia continental
     * - Mismo continente: 2 días
     * - Intercontinental: 3 días
     */
    public static int calcularPlazoDias(String origen, String destino) {
        return mismoContienente(origen, destino) ? 2 : 3;
    }
    
    /**
     * Valida si una ruta cumple con los plazos continentales
     */
    public static boolean validarPlazoRuta(String origen, String destino, 
                                         LocalTime horaSalida, int diaSalida,
                                         LocalTime horaLlegada, int diaLlegada) {
        
        int plazoMaximo = calcularPlazoDias(origen, destino);
        int diasTranscurridos = diaLlegada - diaSalida;
        
        // Si llega el mismo día o al día siguiente está bien
        if (diasTranscurridos <= 1) {
            return true;
        }
        
        // Verificar que no exceda el plazo máximo
        return diasTranscurridos <= plazoMaximo;
    }
    
    /**
     * Obtiene información detallada de un aeropuerto
     */
    public static String obtenerInfoAeropuerto(String codigo) {
        Integer huso = HUSOS_HORARIOS.get(codigo);
        String continente = CONTINENTES.get(codigo);
        
        if (huso == null || continente == null) {
            return "Aeropuerto no configurado: " + codigo;
        }
        
        String signo = huso >= 0 ? "+" : "";
        return String.format("%s: UTC%s%d (%s) - Continente: %s", 
                           codigo, signo, huso, huso, continente);
    }
    
    /**
     * Muestra todos los aeropuertos configurados organizados por continente
     */
    public static void mostrarTodosLosAeropuertos() {
        System.out.println("🌍 ===== AEROPUERTOS CONFIGURADOS (DATOS DEL CSV) =====");
        
        System.out.println("\n📍 SUDAMÉRICA (SAM):");
        mostrarAeropuertosPorContinente("SAM");
        
        System.out.println("\n📍 EUROPA (EUR):");
        mostrarAeropuertosPorContinente("EUR");
        
        System.out.println("\n📍 ASIA (ASI):");
        mostrarAeropuertosPorContinente("ASI");
        
        System.out.println();
    }
    
    /**
     * Muestra aeropuertos de un continente específico
     */
    private static void mostrarAeropuertosPorContinente(String continente) {
        CONTINENTES.entrySet().stream()
            .filter(entry -> entry.getValue().equals(continente))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.println("🛫 " + obtenerInfoAeropuerto(entry.getKey()));
            });
    }
    
    /**
     * Verifica si un aeropuerto está configurado
     */
    public static boolean esAeropuertoValido(String codigo) {
        return HUSOS_HORARIOS.containsKey(codigo);
    }
    
    /**
     * Obtiene el huso horario de un aeropuerto
     */
    public static int obtenerHusoHorario(String codigo) {
        Integer huso = HUSOS_HORARIOS.get(codigo);
        if (huso == null) {
            throw new IllegalArgumentException("Aeropuerto no configurado: " + codigo);
        }
        return huso;
    }
}
