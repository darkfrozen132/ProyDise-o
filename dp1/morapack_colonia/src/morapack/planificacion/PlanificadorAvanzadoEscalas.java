package morapack.planificacion;

import morapack.modelo.*;
import java.util.*;

/**
 * Planificador avanzado que maneja múltiples escalas y optimización de capacidad
 * Basado en el algoritmo MoraPackGAOptimized
 */
public class PlanificadorAvanzadoEscalas {
    
    private static final int MIN_CONEXION_MINUTOS = 30;  // Tiempo mínimo entre conexiones
    private static final int VENTANA_RECOJO_MINUTOS = 30; // 30 minutos para recoger (más realista)
    private static final int MAX_ESCALAS = 5; // Máximo 2 escalas
    private static final int MAX_CANDIDATOS = 5; // Máximo candidatos por escala
    
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
        return planificarRuta(sedeOrigen, destino, cantidad, 0); // Sin restricción temporal
    }
    
    /**
     * Planifica una ruta completa desde una sede hasta un destino con tiempo mínimo
     * @param sedeOrigen Aeropuerto origen (sede)
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de paquetes
     * @param tiempoMinimoPedido Tiempo mínimo en minutos (hora del pedido + ventana de recojo)
     */
    public RutaCompleta planificarRuta(String sedeOrigen, String destino, int cantidad, int tiempoMinimoPedido) {
        // Validar que el destino no sea una sede de MoraPack
        if (destino.equals("SPIM") || destino.equals("EBCI") || destino.equals("UBBB")) {
            return null; // No planificar rutas hacia las propias sedes (silencioso)
        }
        
        // Validar que el origen y destino sean diferentes
        if (sedeOrigen.equals(destino)) {
            System.out.println("⚠️ ADVERTENCIA: Origen y destino son iguales (" + sedeOrigen + ") - esto no tiene sentido");
            return null;
        }
        
        return construirRutaCompleta(sedeOrigen, destino, cantidad, tiempoMinimoPedido, new HashSet<>());
    }
    
    /**
     * Construye una ruta completa con múltiples escalas posibles (limitada)
     */
    private RutaCompleta construirRutaCompleta(String origen, String destino, int cantidad, 
                                             int tiempoInicialMinutos, Set<String> visitados) {
        
        // Evitar ciclos y limitar profundidad
        if (visitados.contains(origen) || visitados.size() >= MAX_ESCALAS) {
            return null;
        }
        
        visitados.add(origen);
        
        // 🧪 ESTRATEGIA MIXTA: A veces buscar escalas incluso si hay vuelos directos (para testing)
        RutaCompleta rutaDirecta = buscarVueloDirecto(origen, destino, cantidad, tiempoInicialMinutos);
        
        // 80% probabilidad de usar directo si está disponible, 20% explorar escalas
        if (rutaDirecta != null && Math.random() < 0.8) {
            return rutaDirecta;
        }
        
        // Si no hay vuelo directo O decidimos explorar escalas, buscar con escalas
        if (visitados.size() < MAX_ESCALAS) {
            RutaCompleta rutaConEscalas = buscarRutaConEscalas(origen, destino, cantidad, tiempoInicialMinutos, visitados);
            
            // Si encontramos ruta con escalas, usarla. Si no, usar la directa (si existe)
            if (rutaConEscalas != null) {
                return rutaConEscalas;
            } else if (rutaDirecta != null) {
                return rutaDirecta;
            }
        }
        
        // Fallback: usar directa si existe
        if (rutaDirecta != null) {
            return rutaDirecta;
        }
        
        // Si no hay vuelo directo, buscar con escalas (solo si no hemos llegado al límite)
        if (visitados.size() < MAX_ESCALAS) {
            return buscarRutaConEscalas(origen, destino, cantidad, tiempoInicialMinutos, visitados);
        }
        
        return null;
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
        
        // Limitar número de candidatos para evitar explosión combinatorial
        if (candidatos.size() > MAX_CANDIDATOS) {
            candidatos = candidatos.subList(0, MAX_CANDIDATOS);
        }
        
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
                // Verificar que la ruta realmente llegue al destino final
                List<Vuelo> vuelosContinuacion = rutaContinuacion.getVuelos();
                if (!vuelosContinuacion.isEmpty()) {
                    Vuelo ultimoVuelo = vuelosContinuacion.get(vuelosContinuacion.size() - 1);
                    if (!ultimoVuelo.getDestino().equals(destino)) {
                        // La ruta no llega al destino final - esto es un error
                        capacidadUsada.put(candidato.claveVuelo, usadoOriginal);
                        continue; // Probar siguiente candidato
                    }
                }
                
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
     * Calcula el tiempo mínimo de despegue basado en la hora del pedido
     * @param pedido El pedido con información temporal
     * @return Tiempo mínimo en minutos desde medianoche
     */
    public static int calcularTiempoMinimoPedido(Pedido pedido) {
        // Obtener hora del pedido (día, hora, minuto del ID)
        int horaPedido = pedido.getHora() * 60 + pedido.getMinuto();
        
        // Agregar ventana de recojo (2 horas)
        int tiempoMinimo = horaPedido + VENTANA_RECOJO_MINUTOS;
        
        return tiempoMinimo;
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
