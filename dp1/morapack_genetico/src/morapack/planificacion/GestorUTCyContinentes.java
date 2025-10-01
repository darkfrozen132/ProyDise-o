package morapack.planificacion;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de zonas horarias UTC y clasificación de continentes
 * Maneja conversión de horarios locales a UTC 0 y determina distancias intercontinentales
 */
public class GestorUTCyContinentes {
    
    // Mapeo de aeropuertos a sus zonas horarias UTC
    private static final Map<String, String> ZONAS_HORARIAS = new HashMap<>();
    
    // Mapeo de aeropuertos a continentes
    private static final Map<String, String> CONTINENTES = new HashMap<>();
    
    static {
        // SUDAMÉRICA (UTC-5 a UTC-3)
        configurarAeropuerto("SPIM", "Sudamerica", "UTC-5", -5); // Lima, Perú (sede)
        configurarAeropuerto("SCEL", "Sudamerica", "UTC-3", -3); // Santiago, Chile
        configurarAeropuerto("SBGR", "Sudamerica", "UTC-3", -3); // São Paulo, Brasil
        configurarAeropuerto("SAEZ", "Sudamerica", "UTC-3", -3); // Buenos Aires, Argentina
        configurarAeropuerto("SKBO", "Sudamerica", "UTC-5", -5); // Bogotá, Colombia
        configurarAeropuerto("SEQU", "Sudamerica", "UTC-5", -5); // Quito, Ecuador
        configurarAeropuerto("SLLP", "Sudamerica", "UTC-4", -4); // La Paz, Bolivia
        configurarAeropuerto("SGAS", "Sudamerica", "UTC-3", -3); // Asunción, Paraguay
        configurarAeropuerto("SUDU", "Sudamerica", "UTC-3", -3); // Montevideo, Uruguay
        configurarAeropuerto("SMJP", "Sudamerica", "UTC-4", -4); // Cayenne, Guayana Francesa
        configurarAeropuerto("SYCJ", "Sudamerica", "UTC-3", -3); // Georgetown, Guyana
        configurarAeropuerto("SMZO", "Sudamerica", "UTC-4", -4); // Paramaribo, Suriname
        configurarAeropuerto("SVMI", "Sudamerica", "UTC-4", -4); // Caracas, Venezuela
        
        // EUROPA (UTC+0 a UTC+3)
        ZONAS_HORARIAS.put("EBCI", "UTC+1");  // Bruselas, Bélgica
        ZONAS_HORARIAS.put("LOWW", "UTC+1");  // Viena, Austria
        ZONAS_HORARIAS.put("EHAM", "UTC+1");  // Ámsterdam, Países Bajos
        ZONAS_HORARIAS.put("LKPR", "UTC+1");  // Praga, República Checa
        ZONAS_HORARIAS.put("EKCH", "UTC+1");  // Copenhague, Dinamarca
        ZONAS_HORARIAS.put("EDDI", "UTC+1");  // Berlín, Alemania
        ZONAS_HORARIAS.put("LDZA", "UTC+1");  // Zagreb, Croacia
        ZONAS_HORARIAS.put("LATI", "UTC+1");  // Tirana, Albania
        ZONAS_HORARIAS.put("LBSF", "UTC+2");  // Sofía, Bulgaria
        
        // ASIA/MEDIO ORIENTE/RUSIA (UTC+3 a UTC+8)
        ZONAS_HORARIAS.put("UBBB", "UTC+3");  // Moscú, Rusia
        ZONAS_HORARIAS.put("OMDB", "UTC+4");  // Dubái, EAU
        ZONAS_HORARIAS.put("OERK", "UTC+3");  // Riad, Arabia Saudí
        ZONAS_HORARIAS.put("VIDP", "UTC+5:30"); // Delhi, India
        ZONAS_HORARIAS.put("OAKB", "UTC+6");  // Kuwait
        ZONAS_HORARIAS.put("OSDI", "UTC+3");  // Damascus, Siria
        ZONAS_HORARIAS.put("OJAI", "UTC+4");  // Amman, Jordania
        ZONAS_HORARIAS.put("UMMS", "UTC+6");  // Minsk, Bielorrusia
        ZONAS_HORARIAS.put("OOMS", "UTC+4");  // Muscat, Omán
        ZONAS_HORARIAS.put("OPKC", "UTC+5");  // Karachi, Pakistán
        ZONAS_HORARIAS.put("OYSN", "UTC+3");  // Sanaa, Yemen
        
        // Continentes
        // SUDAMÉRICA
        CONTINENTES.put("SPIM", "SUDAMERICA");
        CONTINENTES.put("SKBO", "SUDAMERICA");
        CONTINENTES.put("SBBR", "SUDAMERICA");
        CONTINENTES.put("SGAS", "SUDAMERICA");
        CONTINENTES.put("SEQM", "SUDAMERICA");
        CONTINENTES.put("SUAA", "SUDAMERICA");
        CONTINENTES.put("SABE", "SUDAMERICA");
        CONTINENTES.put("SVMI", "SUDAMERICA");
        CONTINENTES.put("SCEL", "SUDAMERICA");
        CONTINENTES.put("SLLP", "SUDAMERICA");
        
        // EUROPA
        CONTINENTES.put("EBCI", "EUROPA");
        CONTINENTES.put("LOWW", "EUROPA");
        CONTINENTES.put("EHAM", "EUROPA");
        CONTINENTES.put("LKPR", "EUROPA");
        CONTINENTES.put("EKCH", "EUROPA");
        CONTINENTES.put("EDDI", "EUROPA");
        CONTINENTES.put("LDZA", "EUROPA");
        CONTINENTES.put("LATI", "EUROPA");
        CONTINENTES.put("LBSF", "EUROPA");
        
        // ASIA/MEDIO ORIENTE/RUSIA
        CONTINENTES.put("UBBB", "ASIA");
        CONTINENTES.put("OMDB", "ASIA");
        CONTINENTES.put("OERK", "ASIA");
        CONTINENTES.put("VIDP", "ASIA");
        CONTINENTES.put("OAKB", "ASIA");
        CONTINENTES.put("OSDI", "ASIA");
        CONTINENTES.put("OJAI", "ASIA");
        CONTINENTES.put("UMMS", "ASIA");
        CONTINENTES.put("OOMS", "ASIA");
        CONTINENTES.put("OPKC", "ASIA");
        CONTINENTES.put("OYSN", "ASIA");
    }
    
    /**
     * Convierte una hora local de un aeropuerto a UTC 0
     */
    public static LocalTime convertirAUTC(String aeropuerto, LocalTime horaLocal) {
        String zonaHoraria = ZONAS_HORARIAS.get(aeropuerto);
        if (zonaHoraria == null) {
            System.err.println("⚠️ Zona horaria no definida para " + aeropuerto + ", usando UTC+0");
            return horaLocal;
        }
        
        try {
            int offsetHoras = 0;
            int offsetMinutos = 0;
            
            if (zonaHoraria.contains(":")) {
                // Casos como UTC+5:30
                String[] parts = zonaHoraria.replace("UTC", "").split(":");
                offsetHoras = Integer.parseInt(parts[0]);
                offsetMinutos = Integer.parseInt(parts[1]);
                if (offsetHoras < 0) {
                    offsetMinutos = -offsetMinutos;
                }
            } else {
                // Casos como UTC+3, UTC-5
                offsetHoras = Integer.parseInt(zonaHoraria.replace("UTC", ""));
            }
            
            // Convertir a UTC restando el offset
            LocalTime horaUTC = horaLocal.minusHours(offsetHoras).minusMinutes(offsetMinutos);
            
            return horaUTC;
            
        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo hora para " + aeropuerto + ": " + e.getMessage());
            return horaLocal;
        }
    }
    
    /**
     * Convierte una hora UTC a hora local de un aeropuerto
     */
    public static LocalTime convertirDeUTC(String aeropuerto, LocalTime horaUTC) {
        String zonaHoraria = ZONAS_HORARIAS.get(aeropuerto);
        if (zonaHoraria == null) {
            return horaUTC;
        }
        
        try {
            int offsetHoras = 0;
            int offsetMinutos = 0;
            
            if (zonaHoraria.contains(":")) {
                String[] parts = zonaHoraria.replace("UTC", "").split(":");
                offsetHoras = Integer.parseInt(parts[0]);
                offsetMinutos = Integer.parseInt(parts[1]);
                if (offsetHoras < 0) {
                    offsetMinutos = -offsetMinutos;
                }
            } else {
                offsetHoras = Integer.parseInt(zonaHoraria.replace("UTC", ""));
            }
            
            // Convertir de UTC sumando el offset
            LocalTime horaLocal = horaUTC.plusHours(offsetHoras).plusMinutes(offsetMinutos);
            
            return horaLocal;
            
        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo de UTC para " + aeropuerto + ": " + e.getMessage());
            return horaUTC;
        }
    }
    
    /**
     * Determina si dos aeropuertos están en el mismo continente
     */
    public static boolean mismosContinentes(String aeropuerto1, String aeropuerto2) {
        String continente1 = CONTINENTES.get(aeropuerto1);
        String continente2 = CONTINENTES.get(aeropuerto2);
        
        if (continente1 == null || continente2 == null) {
            System.err.println("⚠️ Continente no definido para " + aeropuerto1 + " o " + aeropuerto2);
            return false; // Asumir intercontinental si no se sabe
        }
        
        return continente1.equals(continente2);
    }
    
    /**
     * Obtiene el plazo máximo en días según si es continental o intercontinental
     */
    public static int obtenerPlazoMaximo(String aeropuertoOrigen, String aeropuertoDestino) {
        if (mismosContinentes(aeropuertoOrigen, aeropuertoDestino)) {
            return 2; // 2 días para mismo continente
        } else {
            return 3; // 3 días para intercontinental
        }
    }
    
    /**
     * Calcula los días transcurridos entre dos horarios
     */
    public static int calcularDiasTranscurridos(LocalTime horaInicial, int diaInicial, 
                                               LocalTime horaFinal, int diaFinal) {
        if (diaFinal > diaInicial) {
            return diaFinal - diaInicial;
        } else if (diaFinal == diaInicial) {
            if (horaFinal.isAfter(horaInicial)) {
                return 0; // Mismo día
            } else {
                return 1; // Día siguiente
            }
        } else {
            // Caso de cambio de mes (día final menor que inicial)
            return (31 - diaInicial) + diaFinal; // Estimación simplificada
        }
    }
    
    /**
     * Valida si una ruta cumple con el plazo máximo
     */
    public static boolean validarPlazoRuta(String aeropuertoOrigen, String aeropuertoDestino,
                                          LocalTime horaPedido, int diaPedido,
                                          LocalTime horaEntrega, int diaEntrega) {
        
        int plazoMaximo = obtenerPlazoMaximo(aeropuertoOrigen, aeropuertoDestino);
        int diasTranscurridos = calcularDiasTranscurridos(horaPedido, diaPedido, 
                                                        horaEntrega, diaEntrega);
        
        boolean cumple = diasTranscurridos <= plazoMaximo;
        
        if (!cumple) {
            System.err.printf("❌ PLAZO EXCEDIDO: %s→%s requiere %d días pero el plazo máximo es %d días\n",
                            aeropuertoOrigen, aeropuertoDestino, diasTranscurridos, plazoMaximo);
        }
        
        return cumple;
    }
    
    /**
     * Obtiene información de zona horaria de un aeropuerto
     */
    public static String obtenerZonaHoraria(String aeropuerto) {
        return ZONAS_HORARIAS.getOrDefault(aeropuerto, "UTC+0");
    }
    
    /**
     * Obtiene el continente de un aeropuerto
     */
    public static String obtenerContinente(String aeropuerto) {
        return CONTINENTES.getOrDefault(aeropuerto, "DESCONOCIDO");
    }
}
