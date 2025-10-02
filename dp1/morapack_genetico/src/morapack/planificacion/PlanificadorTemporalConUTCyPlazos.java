package morapack.planificacion;

import morapack.modelo.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class PlanificadorTemporalConUTCyPlazos {
    
    private static final int TIEMPO_PREPARACION_MINUTOS = 30; // 30 min antes del vuelo
    private static final int MIN_CONEXION_MINUTOS = 30;       // Tiempo mínimo entre conexiones
    private static final int MAX_ESCALAS = 3;                 // Máximo 3 escalas
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
        
        return construirRutaTemporalConPlazos(sedeOrigen, destino, cantidad, 
                                            tiempoMinimoSalidaUTC, new HashSet<>(), 
                                            pedido.getDia(), esNocturno, plazoMaximo, 
                                            horaPedidoUTC, pedido.getDia());
    }
    
    /**
     * Construye una ruta temporal considerando plazos máximos
     */
    private RutaCompleta construirRutaTemporalConPlazos(String origen, String destino, int cantidad,
                                                      int tiempoMinimo, Set<String> visitados, 
                                                      int diaInicial, boolean esNocturno, int plazoMaximo,
                                                      LocalTime horaPedidoUTC, int diaPedido) {
        
        // Prevenir ciclos infinitos
        if (visitados.contains(origen) || visitados.size() >= MAX_ESCALAS) {
            return null;
        }
        
        visitados.add(origen);
        
        // Buscar vuelos desde el origen
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null || vuelosDesdeOrigen.isEmpty()) {
            return null;
        }
        
        // 🎯 BÚSQUEDA DIRECTA: Buscar vuelo directo al destino
        for (Vuelo vuelo : vuelosDesdeOrigen) {
            if (vuelo.getDestino().equals(destino)) {
                
                // Convertir horarios del vuelo a UTC
                LocalTime horaSalidaLocal = LocalTime.parse(vuelo.getHoraSalida());
                LocalTime horaLlegadaLocal = LocalTime.parse(vuelo.getHoraLlegada());
                
                LocalTime horaSalidaUTC = GestorUTCyContinentesCSV.convertirAUTC(origen, horaSalidaLocal);
                LocalTime horaLlegadaUTC = GestorUTCyContinentesCSV.convertirAUTC(destino, horaLlegadaLocal);
                
                if (esVueloDisponibleEnTiempoUTC(vuelo, tiempoMinimo, cantidad, diaInicial, esNocturno)) {
                    
                    // 📆 VALIDAR PLAZO: Verificar que la entrega esté dentro del plazo
                    int diaLlegada = calcularDiaLlegada(diaInicial, horaSalidaUTC, horaLlegadaUTC, esNocturno);
                    
                    boolean cumplePlazo = GestorUTCyContinentesCSV.validarPlazoRuta(
                        origen, destino, horaPedidoUTC, diaPedido, horaLlegadaUTC, diaLlegada
                    );
                    
                    if (!cumplePlazo) {
                        System.out.printf("   ❌ Vuelo directo excede plazo: %s→%s\n", origen, destino);
                        continue; // Buscar otra opción
                    }
                    
                    System.out.printf("     ✅ Vuelo %s %s→%s (Cap: %d/%d, Paquetes: %d)\n", 
                              vuelo.getHoraSalida(), origen, destino, 
                              vuelo.getCapacidad(), vuelo.getCapacidad(), cantidad);
                    System.out.println("   ✈️ Ruta directa encontrada: " + origen + " → " + destino);
                    System.out.printf("   📆 Entrega en %d días (dentro del plazo de %d días)\n", 
                                    GestorUTCyContinentesCSV.calcularDiasTranscurridos(horaPedidoUTC, diaPedido, horaLlegadaUTC, diaLlegada),
                                    plazoMaximo);
                    
                    // Crear ruta directa exitosa
                    RutaCompleta ruta = new RutaCompleta();
                    ruta.agregarVuelo(vuelo);
                    ruta.setTipoRuta("DIRECTO");
                    
                    // Actualizar capacidad usada
                    String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida() + "-" + diaInicial;
                    capacidadUsada.merge(claveVuelo, cantidad, Integer::sum);
                    
                    return ruta;
                }
            }
        }
        
        // 🔄 BÚSQUEDA CON ESCALAS: Si no hay vuelo directo, buscar con conexiones
        for (Vuelo vuelo : vuelosDesdeOrigen) {
            String aeropuertoConexion = vuelo.getDestino();
            
            // No hacer escala en el destino final ni en el origen
            if (aeropuertoConexion.equals(destino) || aeropuertoConexion.equals(origen)) {
                continue;
            }
            
            if (esVueloDisponibleEnTiempoUTC(vuelo, tiempoMinimo, cantidad, diaInicial, esNocturno)) {
                
                // Calcular tiempo de llegada a la escala para la conexión
                LocalTime horaLlegadaEscala = LocalTime.parse(vuelo.getHoraLlegada());
                LocalTime horaLlegadaEscalaUTC = GestorUTCyContinentesCSV.convertirAUTC(aeropuertoConexion, horaLlegadaEscala);
                
                int tiempoLlegadaEscala = horaLlegadaEscalaUTC.getHour() * 60 + horaLlegadaEscalaUTC.getMinute();
                int tiempoMinimoConexion = tiempoLlegadaEscala + MIN_CONEXION_MINUTOS;
                
                int diaConexion = calcularDiaLlegada(diaInicial, 
                    GestorUTCyContinentesCSV.convertirAUTC(origen, LocalTime.parse(vuelo.getHoraSalida())), 
                    horaLlegadaEscalaUTC, esNocturno);
                
                // Validar que la escala no exceda el plazo
                int diasHastaEscala = GestorUTCyContinentesCSV.calcularDiasTranscurridos(
                    horaPedidoUTC, diaPedido, horaLlegadaEscalaUTC, diaConexion
                );
                
                if (diasHastaEscala >= plazoMaximo) {
                    continue; // Esta escala ya excede el plazo
                }
                
                // Buscar recursivamente desde la escala
                RutaCompleta rutaConexion = construirRutaTemporalConPlazos(
                    aeropuertoConexion, destino, cantidad, tiempoMinimoConexion, 
                    new HashSet<>(visitados), diaConexion, false, plazoMaximo, horaPedidoUTC, diaPedido
                );
                
                if (rutaConexion != null && rutaConexion.esViable()) {
                    System.out.printf("     🔄 Escala: %s→%s→... (%d paquetes)\n", 
                                    origen, aeropuertoConexion, cantidad);
                    System.out.println("   🔄 Ruta con escalas encontrada");
                    
                    // Crear ruta con escalas
                    RutaCompleta rutaCompleta = new RutaCompleta();
                    rutaCompleta.agregarVuelo(vuelo);
                    
                    // Agregar vuelos de la conexión
                    for (Vuelo vueloConexion : rutaConexion.getVuelos()) {
                        rutaCompleta.agregarVuelo(vueloConexion);
                    }
                    
                    // Determinar tipo de ruta
                    int numEscalas = rutaCompleta.getVuelos().size() - 1;
                    if (numEscalas == 1) {
                        rutaCompleta.setTipoRuta("UNA_CONEXION");
                        rutaCompleta.getEscalas().add(aeropuertoConexion);
                    } else if (numEscalas == 2) {
                        rutaCompleta.setTipoRuta("DOS_CONEXIONES");
                        rutaCompleta.getEscalas().addAll(rutaConexion.getEscalas());
                        rutaCompleta.getEscalas().add(0, aeropuertoConexion);
                    }
                    
                    // Actualizar capacidad usada
                    String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida() + "-" + diaInicial;
                    capacidadUsada.merge(claveVuelo, cantidad, Integer::sum);
                    
                    return rutaCompleta;
                }
            }
        }
        
        return null; // No se encontró ruta viable
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
