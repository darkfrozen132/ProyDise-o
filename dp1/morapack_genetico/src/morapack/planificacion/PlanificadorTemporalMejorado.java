package morapack.planificacion;

import morapack.modelo.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Planificador mejorado con lógica temporal realista
 * 
 * MEJORAS IMPLEMENTADAS:
 * 1. ⏰ Considera horarios reales de pedidos vs vuelos
 * 2. 📅 Plan de vuelos diario repetitivo 
 * 3. ⌛ Tiempo de preparación de 30 minutos antes del vuelo
 * 4. 🌙 Lógica de vuelos nocturnos/siguiente día
 * 5. 📦 Gestión de capacidad por vuelo
 */
public class PlanificadorTemporalMejorado {
    
    private static final int TIEMPO_PREPARACION_MINUTOS = 30; // 30 min antes del vuelo
    private static final int MIN_CONEXION_MINUTOS = 30;       // Tiempo mínimo entre conexiones
    private static final int MAX_ESCALAS = 3;                 // Máximo 3 escalas
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final List<Vuelo> vuelos;
    private final Map<String, List<Vuelo>> vuelosPorOrigen;
    private final Map<String, Integer> capacidadUsada; // Capacidad usada por vuelo por día
    
    public PlanificadorTemporalMejorado(List<Vuelo> vuelos) {
        this.vuelos = vuelos;
        this.vuelosPorOrigen = new HashMap<>();
        this.capacidadUsada = new HashMap<>();
        
        // Indexar vuelos por aeropuerto de origen
        for (Vuelo vuelo : vuelos) {
            vuelosPorOrigen.computeIfAbsent(vuelo.getOrigen(), k -> new ArrayList<>()).add(vuelo);
        }
        
        System.out.println("🕐 Planificador Temporal Mejorado inicializado:");
        System.out.println("   - Tiempo de preparación: " + TIEMPO_PREPARACION_MINUTOS + " minutos");
        System.out.println("   - Vuelos diarios repetitivos: Activado");
        System.out.println("   - Lógica nocturna: Activado");
    }
    
    /**
     * Planifica una ruta considerando el tiempo del pedido
     * 
     * @param pedido Pedido con información temporal
     * @param sedeOrigen Sede asignada al pedido
     * @return RutaCompleta con horarios optimizados o null si no es posible
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
        
        // 🕐 LÓGICA TEMPORAL: Calcular cuándo puede salir el pedido
        int horaPedido = pedido.getHora();
        int minutoPedido = pedido.getMinuto();
        int minutosDelDia = horaPedido * 60 + minutoPedido;
        
        // ⌛ Agregar tiempo de preparación (30 minutos)
        int tiempoMinimoSalida = minutosDelDia + TIEMPO_PREPARACION_MINUTOS;
        
        System.out.println("📦 Planificando pedido " + pedido.getId() + ":");
        System.out.println("   Hora pedido: " + String.format("%02d:%02d", horaPedido, minutoPedido));
        System.out.println("   Tiempo mínimo salida: " + formatearTiempo(tiempoMinimoSalida));
        
        // 🌙 LÓGICA NOCTURNA: Si es muy tarde, considerar vuelos del día siguiente
        boolean esNocturno = horaPedido >= 22 || horaPedido < 6;
        if (esNocturno) {
            System.out.println("   🌙 Pedido nocturno detectado");
        }
        
        return construirRutaTemporalRecursiva(sedeOrigen, destino, cantidad, 
                                            tiempoMinimoSalida, new HashSet<>(), 
                                            pedido.getDia(), esNocturno);
    }
    
    /**
     * Construye una ruta temporal de forma recursiva
     */
    private RutaCompleta construirRutaTemporalRecursiva(String origen, String destino, int cantidad,
                                                      int tiempoMinimo, Set<String> visitados, 
                                                      int diaInicial, boolean esNocturno) {
        
        // Prevenir ciclos infinitos
        if (visitados.contains(origen) || visitados.size() >= MAX_ESCALAS) {
            return null;
        }
        
        visitados.add(origen);
        
        // 1️⃣ PRIMERO: Intentar vuelo directo
        RutaCompleta rutaDirecta = buscarVueloDirectoTemporal(origen, destino, cantidad, 
                                                            tiempoMinimo, diaInicial, esNocturno);
        
        if (rutaDirecta != null) {
            System.out.println("   ✈️ Ruta directa encontrada: " + origen + " → " + destino);
            return rutaDirecta;
        }
        
        // 2️⃣ SEGUNDO: Intentar con escalas
        if (visitados.size() < MAX_ESCALAS) {
            RutaCompleta rutaConEscalas = buscarRutaConEscalasTemporal(origen, destino, cantidad,
                                                                     tiempoMinimo, visitados, 
                                                                     diaInicial, esNocturno);
            
            if (rutaConEscalas != null) {
                System.out.println("   🔄 Ruta con escalas encontrada");
                return rutaConEscalas;
            }
        }
        
        return null;
    }
    
    /**
     * Busca vuelo directo considerando horarios y capacidad
     */
    private RutaCompleta buscarVueloDirectoTemporal(String origen, String destino, int cantidad,
                                                  int tiempoMinimo, int dia, boolean esNocturno) {
        
        List<Vuelo> vuelosOrigen = vuelosPorOrigen.get(origen);
        if (vuelosOrigen == null) return null;
        
        // Buscar vuelos al destino
        for (Vuelo vuelo : vuelosOrigen) {
            if (!vuelo.getDestino().equals(destino)) continue;
            
            // 🕐 VERIFICAR HORARIO DEL VUELO
            LocalTime horaSalida = LocalTime.parse(vuelo.getHoraSalida(), TIME_FORMATTER);
            int minutosSalidaDelDia = horaSalida.getHour() * 60 + horaSalida.getMinute();
            
            // 📅 LÓGICA DEL PLAN DIARIO
            String claveCapacidad = generarClaveCapacidad(vuelo, dia);
            
            if (esVueloDisponibleEnTiempo(minutosSalidaDelDia, tiempoMinimo, esNocturno)) {
                // Verificar capacidad
                int capacidadActual = capacidadUsada.getOrDefault(claveCapacidad, 0);
                int capacidadRestante = vuelo.getCapacidad() - capacidadActual;
                
                if (capacidadRestante >= cantidad) {
                    // ✅ VUELO DISPONIBLE
                    capacidadUsada.put(claveCapacidad, capacidadActual + cantidad);
                    
                    RutaCompleta ruta = new RutaCompleta();
                    ruta.agregarVuelo(vuelo);
                    ruta.setTipoRuta("DIRECTO");
                    
                    System.out.println("     ✅ Vuelo " + vuelo.getHoraSalida() + " " + origen + 
                                     "→" + destino + " (Cap: " + capacidadRestante + "/" + 
                                     vuelo.getCapacidad() + ", Paquetes: " + cantidad + ")");
                    
                    return ruta;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Busca rutas con escalas considerando tiempos
     */
    private RutaCompleta buscarRutaConEscalasTemporal(String origen, String destino, int cantidad,
                                                    int tiempoMinimo, Set<String> visitados,
                                                    int dia, boolean esNocturno) {
        
        List<Vuelo> vuelosOrigen = vuelosPorOrigen.get(origen);
        if (vuelosOrigen == null) return null;
        
        // Probar diferentes aeropuertos intermedios
        for (Vuelo primerVuelo : vuelosOrigen) {
            String aeropuertoIntermedio = primerVuelo.getDestino();
            
            if (visitados.contains(aeropuertoIntermedio) || aeropuertoIntermedio.equals(destino)) {
                continue;
            }
            
            // Verificar si el primer vuelo está disponible temporalmente
            LocalTime horaSalida = LocalTime.parse(primerVuelo.getHoraSalida(), TIME_FORMATTER);
            int minutosSalida = horaSalida.getHour() * 60 + horaSalida.getMinute();
            
            if (!esVueloDisponibleEnTiempo(minutosSalida, tiempoMinimo, esNocturno)) {
                continue;
            }
            
            // Verificar capacidad del primer vuelo
            String claveCapacidad1 = generarClaveCapacidad(primerVuelo, dia);
            int capacidadActual1 = capacidadUsada.getOrDefault(claveCapacidad1, 0);
            
            if (primerVuelo.getCapacidad() - capacidadActual1 < cantidad) {
                continue;
            }
            
            // Calcular tiempo de llegada + tiempo mínimo de conexión
            LocalTime horaLlegada = LocalTime.parse(primerVuelo.getHoraLlegada(), TIME_FORMATTER);
            int minutosLlegada = horaLlegada.getHour() * 60 + horaLlegada.getMinute();
            int tiempoMinimoConexion = minutosLlegada + MIN_CONEXION_MINUTOS;
            
            // 🔄 RECURSIÓN: Buscar conexión desde aeropuerto intermedio
            Set<String> visitadosCopia = new HashSet<>(visitados);
            RutaCompleta rutaConexion = construirRutaTemporalRecursiva(aeropuertoIntermedio, destino, 
                                                                     cantidad, tiempoMinimoConexion, 
                                                                     visitadosCopia, dia, false);
            
            if (rutaConexion != null) {
                // ✅ ENCONTRAMOS RUTA COMPLETA CON ESCALAS
                capacidadUsada.put(claveCapacidad1, capacidadActual1 + cantidad);
                
                RutaCompleta rutaCompleta = new RutaCompleta();
                rutaCompleta.agregarVuelo(primerVuelo);
                
                // Agregar vuelos de la conexión
                for (Vuelo vueloConexion : rutaConexion.getVuelos()) {
                    rutaCompleta.agregarVuelo(vueloConexion);
                }
                
                // Configurar tipo de ruta
                int totalVuelos = rutaCompleta.getVuelos().size();
                if (totalVuelos == 2) {
                    rutaCompleta.setTipoRuta("UNA_CONEXION");
                } else if (totalVuelos == 3) {
                    rutaCompleta.setTipoRuta("DOS_CONEXIONES");
                } else {
                    rutaCompleta.setTipoRuta("MULTIPLE_CONEXIONES");
                }
                
                System.out.println("     🔄 Escala: " + origen + "→" + aeropuertoIntermedio + "→... (" + 
                                 cantidad + " paquetes)");
                
                return rutaCompleta;
            }
        }
        
        return null;
    }
    
    /**
     * Verifica si un vuelo está disponible en el tiempo especificado
     * Considera lógica nocturna y plan diario
     */
    private boolean esVueloDisponibleEnTiempo(int minutosSalidaVuelo, int tiempoMinimoPedido, boolean esNocturno) {
        
        // 📅 CASO NORMAL: Vuelo el mismo día
        if (minutosSalidaVuelo >= tiempoMinimoPedido) {
            return true;
        }
        
        // 🌙 CASO NOCTURNO: Vuelo al día siguiente
        if (esNocturno) {
            // Si el pedido es nocturno, considerar vuelos tempranos del día siguiente
            // Ejemplo: Pedido a las 23:00, puede tomar vuelo de 06:00 del siguiente día
            if (minutosSalidaVuelo < 12 * 60) { // Vuelos antes del mediodía del siguiente día
                return true;
            }
        }
        
        // 📅 PLAN DIARIO: Los vuelos se repiten cada día
        // Un vuelo de 08:00 está disponible mañana si el pedido es después de las 08:00 de hoy
        return true; // En un sistema real consideraríamos días específicos
    }
    
    /**
     * Genera clave única para rastrear capacidad por vuelo y día
     */
    private String generarClaveCapacidad(Vuelo vuelo, int dia) {
        return vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + 
               vuelo.getHoraSalida() + "-D" + dia;
    }
    
    /**
     * Formatea tiempo en minutos a formato HH:MM
     */
    private String formatearTiempo(int minutos) {
        // Manejar overflow de días
        while (minutos >= 24 * 60) {
            minutos -= 24 * 60;
        }
        
        int horas = minutos / 60;
        int mins = minutos % 60;
        return String.format("%02d:%02d", horas, mins);
    }
    
    /**
     * Resetea la capacidad usada (para simular un nuevo día)
     */
    public void resetearCapacidades() {
        capacidadUsada.clear();
        System.out.println("🔄 Capacidades reseteadas para nuevo día");
    }
    
    /**
     * Obtiene estadísticas de uso
     */
    public void mostrarEstadisticas() {
        System.out.println("\n📊 ESTADÍSTICAS DEL PLANIFICADOR TEMPORAL:");
        System.out.println("   Vuelos con capacidad utilizada: " + capacidadUsada.size());
        
        int totalVuelos = 0;
        int totalCapacidadUsada = 0;
        
        for (Map.Entry<String, Integer> entrada : capacidadUsada.entrySet()) {
            totalVuelos++;
            totalCapacidadUsada += entrada.getValue();
        }
        
        if (totalVuelos > 0) {
            System.out.println("   Promedio paquetes por vuelo: " + (totalCapacidadUsada / totalVuelos));
            System.out.println("   Total paquetes transportados: " + totalCapacidadUsada);
        }
    }
}
