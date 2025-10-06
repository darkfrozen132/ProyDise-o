package morapack.planificacion;

import morapack.modelo.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class PlanificadorTemporalConUTCyPlazos {
    
    private static final int TIEMPO_PREPARACION_MINUTOS = 30; // Preparación antes de primer vuelo y entre conexiones
    private static final int MAX_ESCALAS = 6;                 // Hasta 6 escalas permitidas
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final Map<String, List<Vuelo>> vuelosPorOrigen;
    private final Map<String, Integer> capacidadUsada; // Capacidad usada por vuelo por día
    
    public PlanificadorTemporalConUTCyPlazos(List<Vuelo> vuelos) {
        this.vuelosPorOrigen = new HashMap<>();
        this.capacidadUsada = new HashMap<>();
        
        // Indexar vuelos por aeropuerto de origen
        for (Vuelo vuelo : vuelos) {
            vuelosPorOrigen.computeIfAbsent(vuelo.getOrigen(), k -> new ArrayList<>()).add(vuelo);
        }
        
        System.out.println("🕐 Planificador Temporal Mejorado con UTC y Plazos inicializado:");
        System.out.println("   - Tiempo de preparación: " + TIEMPO_PREPARACION_MINUTOS + " minutos");
        System.out.println("   - Vuelos diarios repetitivos: Activado");
        System.out.println("   - Lógica nocturna: Activado");
        System.out.println("   - Gestión UTC: Activado");
        System.out.println("   - Plazos continentales: 2 días");
        System.out.println("   - Plazos intercontinentales: 3 días");
    }
    
    /**
     * Planifica una ruta considerando el tiempo del pedido, UTC y plazos
     */
    public RutaCompleta planificarRutaTemporal(Pedido pedido, String sedeOrigen) {
        String destino = pedido.getAeropuertoDestinoId();
        int cantidad = pedido.getCantidadProductos();
        
        // Validaciones básicas
        if (destino.equals("SPIM") || destino.equals("EBCI") || destino.equals("UBBB")) {
            return null; // No enviar a nuestras propias sedes
        }
        
        if (sedeOrigen.equals(destino)) {
            return null; // Origen = destino no tiene sentido
        }
        
        // 🌍 CONVERSIÓN UTC: Convertir hora del pedido a UTC
        LocalTime horaPedidoLocal = LocalTime.of(pedido.getHora(), pedido.getMinuto());
        LocalTime horaPedidoUTC = GestorUTCyContinentesCSV.convertirAUTC(sedeOrigen, horaPedidoLocal);
        
        // ⏰ LÓGICA TEMPORAL: Calcular cuándo puede salir el pedido en UTC
        int minutosDelDiaUTC = horaPedidoUTC.getHour() * 60 + horaPedidoUTC.getMinute();
        int tiempoMinimoSalidaUTC = minutosDelDiaUTC + TIEMPO_PREPARACION_MINUTOS;
        
        System.out.println("📦 Planificando pedido " + pedido.getId() + ":");
        System.out.printf("   Hora pedido local (%s): %02d:%02d\n", 
            GestorUTCyContinentesCSV.obtenerZonaHoraria(sedeOrigen),
            pedido.getHora(), pedido.getMinuto());
        System.out.printf("   Hora pedido UTC: %s\n", horaPedidoUTC.format(TIME_FORMATTER));
        System.out.printf("   Tiempo mínimo salida UTC: %s\n", formatearTiempo(tiempoMinimoSalidaUTC));
        
        // 📆 VALIDACIÓN DE PLAZOS: Verificar plazo máximo permitido
        int plazoMaximo = GestorUTCyContinentesCSV.obtenerPlazoMaximo(sedeOrigen, destino);
        boolean esIntercontinental = !GestorUTCyContinentesCSV.mismosContinentes(sedeOrigen, destino);
        
        System.out.printf("   🌍 Ruta: %s (%s) → %s (%s)\n", 
            sedeOrigen, GestorUTCyContinentesCSV.obtenerContinente(sedeOrigen),
            destino, GestorUTCyContinentesCSV.obtenerContinente(destino));
        System.out.printf("   📆 Tipo: %s (Plazo máximo: %d días)\n", 
            esIntercontinental ? "INTERCONTINENTAL" : "CONTINENTAL", plazoMaximo);
        
        // 🌙 LÓGICA NOCTURNA: Si es muy tarde, considerar vuelos del día siguiente
        boolean esNocturno = horaPedidoUTC.getHour() >= 22 || horaPedidoUTC.getHour() < 6;
        if (esNocturno) {
            System.out.println("   🌙 Pedido nocturno detectado (UTC)");
        }
        
        // Backtracking: explorar todas las rutas viables y elegir la de llegada más temprana
        List<RutaEvaluada> candidatas = new ArrayList<>();
        explorarRutas(sedeOrigen, destino, cantidad, tiempoMinimoSalidaUTC, new LinkedHashSet<>(),
                pedido.getDia(), esNocturno, plazoMaximo, horaPedidoUTC, pedido.getDia(), candidatas, 0);

        if (candidatas.isEmpty()) return null;
        candidatas.sort(Comparator.comparingLong(r -> r.minutoLlegadaAbsoluto));
        return candidatas.get(0).ruta;
    }
    
    // Estructura para evaluar rutas
    private static class RutaEvaluada { RutaCompleta ruta; long minutoLlegadaAbsoluto; RutaEvaluada(RutaCompleta r,long m){ruta=r;minutoLlegadaAbsoluto=m;} }

    private void explorarRutas(String origen, String destino, int cantidad, int tiempoMinimoUTC,
                                LinkedHashSet<String> visitados, int diaActual, boolean esNocturno,
                                int plazoMaximo, LocalTime horaPedidoUTC, int diaPedido,
                                List<RutaEvaluada> candidatas, int profundidad) {

        if (visitados.contains(origen) || profundidad > MAX_ESCALAS) return;
        visitados.add(origen);

        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) { visitados.remove(origen); return; }

        for (Vuelo vuelo : vuelosDesdeOrigen) {
            LocalTime horaSalidaLocal = LocalTime.parse(vuelo.getHoraSalida());
            LocalTime horaLlegadaLocal = LocalTime.parse(vuelo.getHoraLlegada());
            LocalTime horaSalidaUTC = GestorUTCyContinentesCSV.convertirAUTC(origen, horaSalidaLocal);
            LocalTime horaLlegadaUTC = GestorUTCyContinentesCSV.convertirAUTC(vuelo.getDestino(), horaLlegadaLocal);

            int minutosSalidaUTC = horaSalidaUTC.getHour()*60 + horaSalidaUTC.getMinute();
            int minutosLlegadaUTC = horaLlegadaUTC.getHour()*60 + horaLlegadaUTC.getMinute();

            boolean disponible = (esNocturno || tiempoMinimoUTC > 24*60) || minutosSalidaUTC >= tiempoMinimoUTC;
            if (!disponible) continue;

            // Día de llegada (suma 1 si cruza medianoche en UTC)
            int diaLlegada = diaActual + ((horaLlegadaUTC.isBefore(horaSalidaUTC)) ? 1 : 0);

            // Validar plazo (llegada global)
            boolean cumplePlazo = GestorUTCyContinentesCSV.validarPlazoRuta(
                origen, vuelo.getDestino(), horaPedidoUTC, diaPedido, horaLlegadaUTC, diaLlegada);
            if (!cumplePlazo) continue;

            // Capacidad (no parcial todavía)
            String clave = vuelo.getOrigen()+"-"+vuelo.getDestino()+"-"+vuelo.getHoraSalida()+"-"+diaActual;
            int usada = capacidadUsada.getOrDefault(clave,0);
            if (vuelo.getCapacidad() - usada < cantidad) continue;

            // Clonar ruta base
            RutaCompleta rutaParcial = new RutaCompleta();
            rutaParcial.agregarVuelo(vuelo);

            if (vuelo.getDestino().equals(destino)) {
                int escalas = rutaParcial.getVuelos().size()-1;
                if (escalas==0) rutaParcial.setTipoRuta("DIRECTO");
                else if (escalas==1) rutaParcial.setTipoRuta("UNA_CONEXION");
                else if (escalas==2) rutaParcial.setTipoRuta("DOS_CONEXIONES");
                else rutaParcial.setTipoRuta("ESCALAS_"+escalas);

                long minutoAbsolutoLlegada = diaLlegada*24L*60L + minutosLlegadaUTC;
                candidatas.add(new RutaEvaluada(rutaParcial, minutoAbsolutoLlegada));
                continue;
            }

            // Preparar exploración siguiente tramo
            int nuevoTiempoMinimo = minutosLlegadaUTC + TIEMPO_PREPARACION_MINUTOS;
            int nuevoDia = diaLlegada;
            LinkedHashSet<String> copiaVisitados = new LinkedHashSet<>(visitados);
            explorarRutas(vuelo.getDestino(), destino, cantidad, nuevoTiempoMinimo, copiaVisitados,
                    nuevoDia, false, plazoMaximo, horaPedidoUTC, diaPedido, candidatas, profundidad+1);
        }

        visitados.remove(origen);
    }
    
    /**
     * Verifica si un vuelo está disponible considerando tiempo UTC y capacidad
     */
    private boolean esVueloDisponibleEnTiempoUTC(Vuelo vuelo, int tiempoMinimoUTC, int cantidad, 
                                               int dia, boolean esNocturno) {
        
        // Convertir hora de salida del vuelo a UTC
        LocalTime horaSalidaLocal = LocalTime.parse(vuelo.getHoraSalida());
        LocalTime horaSalidaUTC = GestorUTCyContinentesCSV.convertirAUTC(vuelo.getOrigen(), horaSalidaLocal);
        
        int minutosSalidaUTC = horaSalidaUTC.getHour() * 60 + horaSalidaUTC.getMinute();
        
        // Verificar disponibilidad temporal
        boolean disponibleTiempo;
        if (esNocturno || tiempoMinimoUTC > 24 * 60) {
            // Para pedidos nocturnos o que exceden el día, el vuelo puede ser del día siguiente
            disponibleTiempo = true;
        } else {
            disponibleTiempo = minutosSalidaUTC >= tiempoMinimoUTC;
        }
        
        if (!disponibleTiempo) {
            return false;
        }
        
        // Verificar capacidad disponible
        String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida() + "-" + dia;
        int capacidadUsadaActual = capacidadUsada.getOrDefault(claveVuelo, 0);
        int capacidadDisponible = vuelo.getCapacidad() - capacidadUsadaActual;
        
        return capacidadDisponible >= cantidad;
    }
    
    /**
     * Calcula el día de llegada considerando vuelos nocturnos
     */
    private int calcularDiaLlegada(int diaSalida, LocalTime horaSalida, LocalTime horaLlegada, boolean esNocturno) {
        if (esNocturno || horaLlegada.isBefore(horaSalida)) {
            return diaSalida + 1; // Llega al día siguiente
        }
        return diaSalida; // Llega el mismo día
    }
    
    /**
     * Formatea tiempo en minutos a HH:mm
     */
    private String formatearTiempo(int minutos) {
        int horas = (minutos / 60) % 24;
        int mins = minutos % 60;
        return String.format("%02d:%02d", horas, mins);
    }
}
