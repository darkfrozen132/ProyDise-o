package morapack.planificacion;

import morapack.modelo.*;
import java.util.*;

/**
 * Planificador avanzado que maneja múltiples escalas y optimización de capacidad
 * Basado en el algoritmo MoraPackGAOptimized
 */
public class PlanificadorAvanzadoEscalas {
    
    private static final int MIN_CONEXION_MINUTOS = 30;  // Tiempo mínimo entre conexiones
    private static final int VENTANA_RECOJO_MINUTOS = 120; // 2 horas para recoger
    private static final int MAX_EXPANSIONES = 50; // Límite de búsqueda
    
    private final List<Vuelo> vuelos;
    private final Map<String, List<Vuelo>> vuelosPorOrigen;
    private final Map<String, Integer> capacidadUsada;
    
    public PlanificadorAvanzadoEscalas(List<Vuelo> vuelos) {
        this.vuelos = vuelos;
        this.vuelosPorOrigen = new HashMap<>();
        this.capacidadUsada = new HashMap<>();
        
        // Indexar vuelos por aeropuerto de origen
        for (Vuelo vuelo : vuelos) {
            vuelosPorOrigen.computeIfAbsent(vuelo.getOrigen(), k -> new ArrayList<>()).add(vuelo);
        }
    }
    
    /**
     * Planifica una ruta completa desde una sede hasta un destino
     */
    public RutaCompleta planificarRuta(String sedeOrigen, String destino, int cantidad) {
        return construirRutaCompleta(sedeOrigen, destino, cantidad, 0, new HashSet<>());
    }
    
    /**
     * Construye una ruta completa con múltiples escalas posibles
     */
    private RutaCompleta construirRutaCompleta(String origen, String destino, int cantidad, 
                                             int tiempoInicialMinutos, Set<String> visitados) {
        
        // Evitar ciclos
        if (visitados.contains(origen)) {
            return null;
        }
        
        visitados.add(origen);
        
        // Buscar vuelo directo primero
        RutaCompleta rutaDirecta = buscarVueloDirecto(origen, destino, cantidad, tiempoInicialMinutos);
        if (rutaDirecta != null) {
            return rutaDirecta;
        }
        
        // Si no hay vuelo directo, buscar con escalas
        return buscarRutaConEscalas(origen, destino, cantidad, tiempoInicialMinutos, visitados);
    }
    
    /**
     * Busca un vuelo directo viable
     */
    private RutaCompleta buscarVueloDirecto(String origen, String destino, int cantidad, int tiempoMinimo) {
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) return null;
        
        for (Vuelo vuelo : vuelosDesdeOrigen) {
            if (vuelo.getDestino().equals(destino)) {
                int tiempoSalida = convertirHoraAMinutos(vuelo.getHoraSalida());
                
                // Verificar que el tiempo de salida respete el mínimo
                if (tiempoSalida >= tiempoMinimo) {
                    // Verificar capacidad disponible
                    String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida();
                    int usado = capacidadUsada.getOrDefault(claveVuelo, 0);
                    
                    if (usado + cantidad <= vuelo.getCapacidad()) {
                        // Reservar capacidad
                        capacidadUsada.put(claveVuelo, usado + cantidad);
                        
                        // Crear ruta directa
                        RutaCompleta ruta = new RutaCompleta();
                        ruta.agregarVuelo(vuelo);
                        ruta.setTipoRuta("DIRECTO");
                        return ruta;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Busca rutas con una o múltiples escalas
     */
    private RutaCompleta buscarRutaConEscalas(String origen, String destino, int cantidad, 
                                            int tiempoMinimo, Set<String> visitados) {
        
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) return null;
        
        // Candidatos para escalas ordenados por prioridad
        List<CandidatoEscala> candidatos = new ArrayList<>();
        
        for (Vuelo vuelo : vuelosDesdeOrigen) {
            if (!vuelo.getDestino().equals(destino) && !visitados.contains(vuelo.getDestino())) {
                int tiempoSalida = convertirHoraAMinutos(vuelo.getHoraSalida());
                
                if (tiempoSalida >= tiempoMinimo) {
                    String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida();
                    int usado = capacidadUsada.getOrDefault(claveVuelo, 0);
                    
                    if (usado + cantidad <= vuelo.getCapacidad()) {
                        int tiempoLlegada = convertirHoraAMinutos(vuelo.getHoraLlegada());
                        double distancia = calcularDistanciaEstimada(vuelo.getDestino(), destino);
                        
                        candidatos.add(new CandidatoEscala(vuelo, claveVuelo, tiempoLlegada, distancia));
                    }
                }
            }
        }
        
        // Ordenar candidatos por distancia al destino (más cerca = mejor)
        candidatos.sort(Comparator.comparingDouble(c -> c.distanciaAlDestino));
        
        // Probar cada candidato hasta encontrar una ruta viable
        for (CandidatoEscala candidato : candidatos) {
            // Reservar capacidad temporalmente
            int usadoOriginal = capacidadUsada.getOrDefault(candidato.claveVuelo, 0);
            capacidadUsada.put(candidato.claveVuelo, usadoOriginal + cantidad);
            
            // Intentar continuar desde la escala
            int tiempoConexion = candidato.tiempoLlegada + MIN_CONEXION_MINUTOS;
            Set<String> nuevosVisitados = new HashSet<>(visitados);
            
            RutaCompleta rutaContinuacion = construirRutaCompleta(
                candidato.vuelo.getDestino(), destino, cantidad, tiempoConexion, nuevosVisitados);
            
            if (rutaContinuacion != null) {
                // Éxito: construir ruta completa
                RutaCompleta rutaCompleta = new RutaCompleta();
                rutaCompleta.agregarVuelo(candidato.vuelo);
                
                // Agregar todos los vuelos de la continuación
                for (Vuelo vuelo : rutaContinuacion.getVuelos()) {
                    rutaCompleta.agregarVuelo(vuelo);
                }
                
                // Configurar tipo y escalas
                List<String> escalas = new ArrayList<>();
                escalas.add(candidato.vuelo.getDestino());
                for (String escala : rutaContinuacion.getEscalas()) {
                    escalas.add(escala);
                }
                
                rutaCompleta.setEscalas(escalas);
                if (escalas.size() == 1) {
                    rutaCompleta.setTipoRuta("UNA_CONEXION");
                } else if (escalas.size() == 2) {
                    rutaCompleta.setTipoRuta("DOS_CONEXIONES");
                } else {
                    rutaCompleta.setTipoRuta("MULTIPLE_CONEXIONES");
                }
                
                return rutaCompleta;
            } else {
                // Fallo: restaurar capacidad
                capacidadUsada.put(candidato.claveVuelo, usadoOriginal);
            }
        }
        
        return null; // No se encontró ruta viable
    }
    
    /**
     * Convierte hora en formato HH:mm a minutos desde medianoche
     */
    private int convertirHoraAMinutos(String hora) {
        try {
            String[] partes = hora.split(":");
            int horas = Integer.parseInt(partes[0]);
            int minutos = Integer.parseInt(partes[1]);
            return horas * 60 + minutos;
        } catch (Exception e) {
            return 0; // Valor por defecto
        }
    }
    
    /**
     * Calcula distancia estimada entre dos aeropuertos (simplificada)
     */
    private double calcularDistanciaEstimada(String origen, String destino) {
        // Implementación simplificada basada en códigos ICAO
        // En una implementación real usaríamos coordenadas geográficas
        if (origen.equals(destino)) return 0.0;
        
        // Dar preferencia a conexiones lógicas por región
        if (esAeropuertoEuropeo(origen) && esAeropuertoEuropeo(destino)) return 1.0;
        if (esAeropuertoSudamericano(origen) && esAeropuertoSudamericano(destino)) return 1.0;
        if (esAeropuertoAsiatico(origen) && esAeropuertoAsiatico(destino)) return 1.0;
        
        return 2.0; // Conexión intercontinental
    }
    
    private boolean esAeropuertoEuropeo(String codigo) {
        return codigo.startsWith("E") || codigo.startsWith("L");
    }
    
    private boolean esAeropuertoSudamericano(String codigo) {
        return codigo.startsWith("S");
    }
    
    private boolean esAeropuertoAsiatico(String codigo) {
        return codigo.startsWith("O") || codigo.startsWith("U") || codigo.startsWith("V");
    }
    
    /**
     * Reinicia el estado de capacidades usadas
     */
    public void reiniciarCapacidades() {
        capacidadUsada.clear();
    }
    
    /**
     * Obtiene estadísticas de uso de capacidad
     */
    public Map<String, Integer> getEstadisticasCapacidad() {
        return new HashMap<>(capacidadUsada);
    }
    
    /**
     * Clase interna para manejar candidatos de escala
     */
    private static class CandidatoEscala {
        final Vuelo vuelo;
        final String claveVuelo;
        final int tiempoLlegada;
        final double distanciaAlDestino;
        
        CandidatoEscala(Vuelo vuelo, String claveVuelo, int tiempoLlegada, double distanciaAlDestino) {
            this.vuelo = vuelo;
            this.claveVuelo = claveVuelo;
            this.tiempoLlegada = tiempoLlegada;
            this.distanciaAlDestino = distanciaAlDestino;
        }
    }
}
