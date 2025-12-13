package com.proyecto.backend.planificador.semanal.model;

import com.proyecto.backend.model.PedidoSemanal;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Decodificador genetico para el algoritmo genetico
 *
 * Interpreta un cromosoma como prioridades de pedidos y genera una solucion
 * mediante asignacion greedy en orden de prioridad.
 *
 * Cromosoma:
 *   - Cada gen representa la prioridad de un pedido
 *   - Valores en rango [0, 1]
 *   - Mayor valor = mayor prioridad (se procesa primero)
 *
 * Proceso de decodificacion:
 *   1. Ordenar pedidos segun prioridades del cromosoma (descendente)
 *   2. Para cada pedido (en orden):
 *      - Buscar mejor ruta disponible (BFS)
 *      - Asignar capacidades de vuelos y almacenes
 *   3. Calcular metricas y fitness
 *   4. Retornar solucion
 */
@Slf4j
public class DecodificadorGenetico {

    private final WorldTemporal worldTemporal;
    private final ControladorAlmacenes controladorAlmacenes;
    private final CalculadorPlazos calculadorPlazos;
    private final BuscadorRutas buscadorRutas;

    /**
     * Constructor
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     */
    public DecodificadorGenetico(WorldTemporal worldTemporal, ControladorAlmacenes controladorAlmacenes) {
        this.worldTemporal = worldTemporal;
        this.controladorAlmacenes = controladorAlmacenes;
        this.calculadorPlazos = new CalculadorPlazos(worldTemporal);
        this.buscadorRutas = new BuscadorRutas(worldTemporal, controladorAlmacenes);
    }

    /**
     * Decodifica un cromosoma en una solucion
     *
     * @param cromosoma Cromosoma con prioridades de pedidos
     * @param pedidos Lista de pedidos a planificar
     * @return Solucion generada
     */
    public Solution decodificar(Chromosome cromosoma, List<PedidoSemanal> pedidos) {
        // Validar que el cromosoma tenga el numero correcto de genes
        if (cromosoma.getNumGenes() != pedidos.size()) {
            throw new IllegalArgumentException(
                String.format("Cromosoma tiene %d genes pero hay %d pedidos",
                    cromosoma.getNumGenes(), pedidos.size())
            );
        }

        Solution solucion = new Solution();

        log.debug("Decodificando cromosoma para {} pedidos", pedidos.size());

        // Ordenar pedidos segun prioridades del cromosoma (mayor prioridad primero)
        List<PedidoConPrioridad> pedidosOrdenados = ordenarPedidosPorPrioridad(cromosoma, pedidos);

        int rutasGeneradas = 0;
        int rutasFallidas = 0;

        // Procesar pedidos en orden de prioridad
        for (PedidoConPrioridad pedidoPriorizado : pedidosOrdenados) {
            PedidoSemanal pedido = pedidoPriorizado.pedido;

            try {
                List<SubRuta> subrutas = generarRutasPedido(pedido);

                if (!subrutas.isEmpty()) {
                    solucion.agregarRutas(pedido, subrutas);
                    rutasGeneradas++;
                } else {
                    log.debug("No se pudo generar ruta para pedido {} (prioridad: {:.3f})",
                            pedido.getId(), pedidoPriorizado.prioridad);
                    rutasFallidas++;
                    solucion.agregarRutas(pedido, new ArrayList<>());
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                solucion.agregarRutas(pedido, new ArrayList<>());
            }
        }

        log.debug("Decodificacion completada: {} rutas generadas, {} fallidas",
                rutasGeneradas, rutasFallidas);

        // Calcular metricas y fitness
        solucion.calcularMetricasYFitness(calculadorPlazos);

        return solucion;
    }

    /**
     * Ordena pedidos segun prioridades del cromosoma
     *
     * @param cromosoma Cromosoma con prioridades
     * @param pedidos Lista de pedidos
     * @return Lista de pedidos ordenados por prioridad (descendente)
     */
    private List<PedidoConPrioridad> ordenarPedidosPorPrioridad(Chromosome cromosoma, List<PedidoSemanal> pedidos) {
        return IntStream.range(0, pedidos.size())
            .mapToObj(i -> new PedidoConPrioridad(pedidos.get(i), cromosoma.getGen(i)))
            .sorted(Comparator.comparingDouble((PedidoConPrioridad p) -> p.prioridad).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Genera rutas para un pedido especifico
     * 
     * 🆕 MEJORA: Selecciona el hub más cercano al destino para optimizar rutas
     * y distribuir carga entre las 3 sedes (SPIM, EBCI, UBBB)
     * 
     * 🆕 MEJORA 2: Ahora pasa la hora del pedido al buscador para que seleccione
     * vuelos que salgan DESPUÉS de la hora del pedido, distribuyendo así los
     * pedidos entre diferentes horarios de vuelos.
     *
     * @param pedido Pedido a procesar
     * @return Lista de subrutas (normalmente 1)
     */
    private List<SubRuta> generarRutasPedido(PedidoSemanal pedido) {
        List<SubRuta> subrutas = new ArrayList<>();

        String destino = pedido.getAeropuertoDestinoId();
        int cantidad = pedido.getCantidadProductos();

        // Calcular dia relativo del pedido
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        int diaRelativo = worldTemporal.calcularDiaRelativo(fechaPedido);

        if (diaRelativo < 0) {
            log.warn("Pedido {} fuera del horizonte temporal: {}", pedido.getId(), fechaPedido);
            return subrutas;
        }

        // 🆕 Calcular hora mínima de salida (hora del pedido)
        // Los vuelos deben salir DESPUÉS de esta hora para que el pedido pueda estar listo
        java.time.LocalTime horaPedido = java.time.LocalTime.of(pedido.getHora(), pedido.getMinuto());
        
        // 🆕 ORDENAR HUBS POR CERCANÍA AL DESTINO
        // Esto asegura que los pedidos salgan del hub más cercano geográficamente
        List<String> hubsOrdenados = ordenarHubsPorCercania(destino);
        
        log.trace("Pedido {} -> Destino: {} | Hora: {} | Hubs ordenados por cercanía: {}", 
                  pedido.getId(), destino, horaPedido, hubsOrdenados);

        // Intentar generar ruta desde el hub más cercano primero
        for (String hub : hubsOrdenados) {
            // 🆕 Pasar hora mínima de salida al buscador
            SubRuta subruta = buscadorRutas.buscarRutaConHoraMinima(hub, destino, cantidad, diaRelativo, horaPedido);

            if (subruta != null) {
                log.trace("Pedido {} asignado a hub {} (destino: {}, hora salida >= {})", 
                         pedido.getId(), hub, destino, horaPedido);
                subrutas.add(subruta);
                break; // Solo necesitamos una ruta
            }
        }

        return subrutas;
    }
    
    /**
     * 🆕 Ordena los hubs por cercanía geográfica al destino
     * 
     * Estrategia:
     * 1. Calcular distancia de cada hub al destino
     * 2. Ordenar de menor a mayor distancia
     * 3. Si no se puede calcular distancia, mantener orden original
     * 
     * @param destino Código ICAO del aeropuerto destino
     * @return Lista de hubs ordenados por cercanía (más cercano primero)
     */
    private List<String> ordenarHubsPorCercania(String destino) {
        List<String> hubs = worldTemporal.getHubs();
        
        // Obtener aeropuerto destino para calcular distancias
        var aeropuertoDestino = worldTemporal.getAeropuerto(destino);
        
        if (aeropuertoDestino == null) {
            log.warn("Aeropuerto destino {} no encontrado, usando orden original de hubs", destino);
            return new ArrayList<>(hubs);
        }
        
        // Crear lista de hubs con distancias
        List<HubConDistancia> hubsConDistancia = new ArrayList<>();
        
        for (String hubCodigo : hubs) {
            var aeropuertoHub = worldTemporal.getAeropuerto(hubCodigo);
            
            if (aeropuertoHub != null) {
                double distancia = calcularDistanciaHaversine(
                    aeropuertoHub.getLatitud(), aeropuertoHub.getLongitud(),
                    aeropuertoDestino.getLatitud(), aeropuertoDestino.getLongitud()
                );
                hubsConDistancia.add(new HubConDistancia(hubCodigo, distancia));
            } else {
                // Si no encontramos el hub, asignarle distancia máxima
                hubsConDistancia.add(new HubConDistancia(hubCodigo, Double.MAX_VALUE));
            }
        }
        
        // Ordenar por distancia (menor primero)
        hubsConDistancia.sort(Comparator.comparingDouble(h -> h.distancia));
        
        // Extraer solo los códigos
        return hubsConDistancia.stream()
                .map(h -> h.hubCodigo)
                .collect(Collectors.toList());
    }
    
    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * 
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final double RADIO_TIERRA_KM = 6371.0;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return RADIO_TIERRA_KM * c;
    }
    
    /**
     * Clase auxiliar para asociar hub con su distancia al destino
     */
    private static class HubConDistancia {
        final String hubCodigo;
        final double distancia;
        
        HubConDistancia(String hubCodigo, double distancia) {
            this.hubCodigo = hubCodigo;
            this.distancia = distancia;
        }
    }

    /**
     * Clase interna para asociar pedido con su prioridad
     */
    private static class PedidoConPrioridad {
        final PedidoSemanal pedido;
        final double prioridad;

        PedidoConPrioridad(PedidoSemanal pedido, double prioridad) {
            this.pedido = pedido;
            this.prioridad = prioridad;
        }
    }
}
