package morapack.planificacion;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador ligero para conversión UTC y continentes usado por el planificador genético.
 * Basado en la versión de colonia v2, pero sólo con los métodos requeridos y firmas usadas.
 */
public class GestorUTCyContinentesCSV {
    private static final Map<String,Integer> HUSOS = new HashMap<>();
    private static final Map<String,String> CONT = new HashMap<>();

    static { // Datos mínimos (extender si se usan más aeropuertos)
        registrar("SKBO", -5, "SAM"); registrar("SEQM", -5, "SAM"); registrar("SVMI", -4, "SAM");
        registrar("SBBR", -3, "SAM"); registrar("SPIM", -5, "SAM"); registrar("SLLP", -4, "SAM");
        registrar("SCEL", -3, "SAM"); registrar("SABE", -3, "SAM"); registrar("SGAS", -4, "SAM"); registrar("SUAA", -3, "SAM");
        registrar("LATI", +2, "EUR"); registrar("EDDI", +2, "EUR"); registrar("LOWW", +2, "EUR"); registrar("EBCI", +2, "EUR");
        registrar("UMMS", +3, "EUR"); registrar("LBSF", +3, "EUR"); registrar("LKPR", +2, "EUR"); registrar("LDZA", +2, "EUR");
        registrar("EKCH", +2, "EUR"); registrar("EHAM", +2, "EUR");
        registrar("VIDP", +5, "ASI"); registrar("OSDI", +3, "ASI"); registrar("OERK", +3, "ASI"); registrar("OMDB", +4, "ASI");
        registrar("OAKB", +4, "ASI"); registrar("OOMS", +4, "ASI"); registrar("OYSN", +3, "ASI"); registrar("OPKC", +5, "ASI");
        registrar("UBBB", +2, "ASI"); registrar("OJAI", +3, "ASI");
    }

    private static void registrar(String cod, int huso, String cont){HUSOS.put(cod,huso);CONT.put(cod,cont);}    

    public static LocalTime convertirAUTC(String aeropuerto, LocalTime horaLocal) {
        Integer h = HUSOS.get(aeropuerto); if (h==null) throw new IllegalArgumentException("Aeropuerto desconocido: "+aeropuerto);
        return horaLocal.minusHours(h);
    }

    public static String obtenerZonaHoraria(String aeropuerto) {
        Integer h = HUSOS.get(aeropuerto); if (h==null) return "UTC";
        return String.format("UTC%+d", h);
    }

    public static int obtenerPlazoMaximo(String origen, String destino) { return mismosContinentes(origen,destino)?2:3; }

    public static boolean mismosContinentes(String a, String b) { return obtenerContinente(a).equals(obtenerContinente(b)); }

    public static String obtenerContinente(String a) { String c = CONT.get(a); if (c==null) throw new IllegalArgumentException("Aeropuerto desconocido: "+a); return c; }

    public static boolean validarPlazoRuta(String origen, String destino, LocalTime horaPedidoUTC, int diaPedido, LocalTime horaLlegadaUTC, int diaLlegada) {
        int dias = diaLlegada - diaPedido; // simplificado
        int max = obtenerPlazoMaximo(origen,destino);
        return dias <= max; // acepta mismo día o días dentro del máximo
    }

    public static int calcularDiasTranscurridos(LocalTime horaSalidaUTC, int diaSalida, LocalTime horaLlegadaUTC, int diaLlegada) {
        return diaLlegada - diaSalida;
    }

    public static void mostrarTodosLosAeropuertos(){ /* no-op en stub */ }
}
