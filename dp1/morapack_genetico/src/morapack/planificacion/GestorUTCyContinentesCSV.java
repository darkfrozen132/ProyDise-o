package morapack.planificacion;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor para conversiones UTC y validación de plazos por continentes
 * DATOS EXTRAÍDOS DIRECTAMENTE DE aeropuertos_simple.csv
 * 
 * FUNCIONALIDADES:
 * 🌍 Conversión de horarios locales a UTC 0
 * 📍 Clasificación de aeropuertos por continente (SAM, EUR, ASI)
 * ⏰ Gestión de zonas horarias según CSV
 * 📆 Validación de plazos (2 días continental, 3 días intercontinental)
 */
public class GestorUTCyContinentesCSV {
    
    // 🌍 MAPPING DE ZONAS HORARIAS POR AEROPUERTO - DATOS DEL CSV
    private static final Map<String, String> ZONAS_HORARIAS = new HashMap<>();
    private static final Map<String, Integer> OFFSET_UTC = new HashMap<>();
    private static final Map<String, String> CONTINENTES = new HashMap<>();
    
    static {
        // CONFIGURACIÓN EXACTA desde aeropuertos_simple.csv
        
        // SUDAMÉRICA (SAM) - Usando husos exactos del CSV
        configurarAeropuerto("SKBO", "SAM", "UTC-5", -5);    // Bogotá, Colombia, huso=-5
        configurarAeropuerto("SEQM", "SAM", "UTC-5", -5);    // Quito, Ecuador, huso=-5
        configurarAeropuerto("SVMI", "SAM", "UTC-4", -4);    // Caracas, Venezuela, huso=-4
        configurarAeropuerto("SBBR", "SAM", "UTC-3", -3);    // Brasilia, Brasil, huso=-3
        configurarAeropuerto("SPIM", "SAM", "UTC-5", -5);    // Lima, Perú (sede), huso=-5
        configurarAeropuerto("SLLP", "SAM", "UTC-4", -4);    // La Paz, Bolivia, huso=-4
        configurarAeropuerto("SCEL", "SAM", "UTC-3", -3);    // Santiago de Chile, huso=-3
        configurarAeropuerto("SABE", "SAM", "UTC-3", -3);    // Buenos Aires, Argentina, huso=-3
        configurarAeropuerto("SGAS", "SAM", "UTC-4", -4);    // Asunción, Paraguay, huso=-4
        configurarAeropuerto("SUAA", "SAM", "UTC-3", -3);    // Montevideo, Uruguay, huso=-3
        
        // EUROPA (EUR) - Usando husos exactos del CSV
        configurarAeropuerto("LATI", "EUR", "UTC+2", 2);     // Tirana, Albania, huso=2
        configurarAeropuerto("EDDI", "EUR", "UTC+2", 2);     // Berlín, Alemania, huso=2
        configurarAeropuerto("LOWW", "EUR", "UTC+2", 2);     // Viena, Austria, huso=2
        configurarAeropuerto("EBCI", "EUR", "UTC+2", 2);     // Bruselas, Bélgica (sede), huso=2
        configurarAeropuerto("UMMS", "EUR", "UTC+3", 3);     // Minsk, Bielorrusia, huso=3
        configurarAeropuerto("LBSF", "EUR", "UTC+3", 3);     // Sofía, Bulgaria, huso=3
        configurarAeropuerto("LKPR", "EUR", "UTC+2", 2);     // Praga, República Checa, huso=2
        configurarAeropuerto("LDZA", "EUR", "UTC+2", 2);     // Zagreb, Croacia, huso=2
        configurarAeropuerto("EKCH", "EUR", "UTC+2", 2);     // Copenhague, Dinamarca, huso=2
        configurarAeropuerto("EHAM", "EUR", "UTC+2", 2);     // Ámsterdam, Holanda, huso=2
        
        // ASIA (ASI) - Usando husos exactos del CSV
        configurarAeropuerto("VIDP", "ASI", "UTC+5", 5);     // Delhi, India, huso=5
        configurarAeropuerto("OSDI", "ASI", "UTC+3", 3);     // Damasco, Siria, huso=3
        configurarAeropuerto("OERK", "ASI", "UTC+3", 3);     // Riad, Arabia Saudita, huso=3
        configurarAeropuerto("OMDB", "ASI", "UTC+4", 4);     // Dubai, Emiratos A.U., huso=4
        configurarAeropuerto("OAKB", "ASI", "UTC+4", 4);     // Kabul, Afganistán, huso=4
        configurarAeropuerto("OOMS", "ASI", "UTC+4", 4);     // Mascate, Omán, huso=4
        configurarAeropuerto("OYSN", "ASI", "UTC+3", 3);     // Sana, Yemen, huso=3
        configurarAeropuerto("OPKC", "ASI", "UTC+5", 5);     // Karachi, Pakistán, huso=5
        configurarAeropuerto("UBBB", "ASI", "UTC+2", 2);     // Baku, Azerbaiyán (sede), huso=2
        configurarAeropuerto("OJAI", "ASI", "UTC+3", 3);     // Amán, Jordania, huso=3
    }
    
    private static void configurarAeropuerto(String codigo, String continente, String zona, int offset) {
        CONTINENTES.put(codigo, continente);
        ZONAS_HORARIAS.put(codigo, zona);
        OFFSET_UTC.put(codigo, offset);
    }
    
    /**
     * Convierte hora local de un aeropuerto a UTC 0
     */
    public static LocalTime convertirAUTC(String aeropuerto, LocalTime horaLocal) {
        int offset = OFFSET_UTC.getOrDefault(aeropuerto, 0);
        return horaLocal.minusHours(offset);
    }
    
    /**
     * Obtiene la zona horaria de un aeropuerto
     */
    public static String obtenerZonaHoraria(String aeropuerto) {
        return ZONAS_HORARIAS.getOrDefault(aeropuerto, "UTC+0");
    }
    
    /**
     * Obtiene el continente de un aeropuerto
     */
    public static String obtenerContinente(String aeropuerto) {
        return CONTINENTES.getOrDefault(aeropuerto, "Desconocido");
    }
    
    /**
     * Verifica si dos aeropuertos están en el mismo continente
     */
    public static boolean mismosContinentes(String aeropuerto1, String aeropuerto2) {
        String continente1 = CONTINENTES.get(aeropuerto1);
        String continente2 = CONTINENTES.get(aeropuerto2);
        return continente1 != null && continente1.equals(continente2);
    }
    
    /**
     * Obtiene el plazo máximo de entrega (2 días continental, 3 días intercontinental)
     */
    public static int obtenerPlazoMaximo(String origen, String destino) {
        return mismosContinentes(origen, destino) ? 2 : 3;
    }
    
    /**
     * Valida si una ruta cumple con el plazo máximo permitido
     */
    public static boolean validarPlazoRuta(String origen, String destino, 
                                         LocalTime horaPedido, int diaPedido,
                                         LocalTime horaEntrega, int diaEntrega) {
        int plazoMaximo = obtenerPlazoMaximo(origen, destino);
        int diasTranscurridos = calcularDiasTranscurridos(horaPedido, diaPedido, horaEntrega, diaEntrega);
        return diasTranscurridos <= plazoMaximo;
    }
    
    /**
     * Calcula los días transcurridos entre pedido y entrega
     */
    public static int calcularDiasTranscurridos(LocalTime horaPedido, int diaPedido,
                                              LocalTime horaEntrega, int diaEntrega) {
        int diasCompletos = diaEntrega - diaPedido;
        
        // Si entrega el mismo día pero después del pedido, cuenta como 0 días
        if (diasCompletos == 0 && !horaEntrega.isBefore(horaPedido)) {
            return 0;
        }
        
        // Si entrega el mismo día pero antes del pedido (cruce de medianoche), cuenta como 1 día
        if (diasCompletos == 0 && horaEntrega.isBefore(horaPedido)) {
            return 1;
        }
        
        return diasCompletos;
    }
    
    /**
     * Muestra información de configuración de un aeropuerto
     */
    public static void mostrarInfoAeropuerto(String codigo) {
        System.out.printf("🛫 %s: %s (%s) - Continente: %s\n", 
                         codigo, 
                         obtenerZonaHoraria(codigo),
                         OFFSET_UTC.getOrDefault(codigo, 0) >= 0 ? "+" + OFFSET_UTC.get(codigo) : OFFSET_UTC.get(codigo),
                         obtenerContinente(codigo));
    }
    
    /**
     * Muestra todos los aeropuertos configurados
     */
    public static void mostrarTodosLosAeropuertos() {
        System.out.println("🌍 ===== AEROPUERTOS CONFIGURADOS (DATOS DEL CSV) =====");
        
        System.out.println("\n📍 SUDAMÉRICA (SAM):");
        for (String codigo : CONTINENTES.keySet()) {
            if ("SAM".equals(CONTINENTES.get(codigo))) {
                mostrarInfoAeropuerto(codigo);
            }
        }
        
        System.out.println("\n📍 EUROPA (EUR):");
        for (String codigo : CONTINENTES.keySet()) {
            if ("EUR".equals(CONTINENTES.get(codigo))) {
                mostrarInfoAeropuerto(codigo);
            }
        }
        
        System.out.println("\n📍 ASIA (ASI):");
        for (String codigo : CONTINENTES.keySet()) {
            if ("ASI".equals(CONTINENTES.get(codigo))) {
                mostrarInfoAeropuerto(codigo);
            }
        }
    }
}
