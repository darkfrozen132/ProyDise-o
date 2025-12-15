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
 *      - 🆕 Si el pedido es muy grande, dividirlo en múltiples subrutas
 *      - Asignar capacidades de vuelos y almacenes
 *   3. Calcular metricas y fitness
 *   4. Retornar solucion
 */
@Slf4j
public class DecodificadorGenetico {

    // 🆕 Capacidad típica máxima de un vuelo (para decidir cuándo dividir)
    private static final int CAPACIDAD_TIPICA_VUELO = 350;
    
    // 🆕 Máximo número de divisiones por pedido (evitar fragmentación excesiva)
    private static final int MAX_DIVISIONES_POR_PEDIDO = 5;

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

        log.info("🧬 Decodificando cromosoma para {} pedidos", pedidos.size());

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
                    log.warn("❌ No ruta para pedido {} → destino: {} | hora: {}:{} | cantidad: {}",
                            pedido.getId(), pedido.getAeropuertoDestinoId(), 
                            pedido.getHora(), pedido.getMinuto(), pedido.getCantidadProductos());
                    rutasFallidas++;
                    solucion.agregarRutas(pedido, new ArrayList<>());
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                solucion.agregarRutas(pedido, new ArrayList<>());
            }
        }

        log.info("📊 Decodificacion: {} rutas OK, {} fallidas ({}% éxito)",
                rutasGeneradas, rutasFallidas, 
                pedidos.isEmpty() ? 0 : (rutasGeneradas * 100 / pedidos.size()));

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
     * 🆕 MEJORA 3: DIVISIÓN DE PEDIDOS GRANDES
     * Si un pedido excede la capacidad típica de un vuelo (350 paquetes),
     * se divide automáticamente en múltiples subrutas/vuelos.
     *
     * @param pedido Pedido a procesar
     * @return Lista de subrutas (puede ser múltiples si el pedido es grande)
     */
    private List<SubRuta> generarRutasPedido(PedidoSemanal pedido) {
        List<SubRuta> subrutas = new ArrayList<>();

        String destino = pedido.getAeropuertoDestinoId();
        int cantidadTotal = pedido.getCantidadProductos();

        // Calcular dia relativo del pedido
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        int diaRelativo = worldTemporal.calcularDiaRelativo(fechaPedido);

        if (diaRelativo < 0) {
            log.warn("Pedido {} fuera del horizonte temporal: {}", pedido.getId(), fechaPedido);
            return subrutas;
        }

        // Calcular hora mínima de salida (hora del pedido)
        java.time.LocalTime horaPedido = java.time.LocalTime.of(pedido.getHora(), pedido.getMinuto());
        
        // ORDENAR HUBS POR CERCANÍA AL DESTINO
        List<String> hubsOrdenados = ordenarHubsPorCercania(destino);
        
        // 🆕 VERIFICAR SI NECESITA DIVISIÓN
        if (cantidadTotal <= CAPACIDAD_TIPICA_VUELO) {
            // Pedido pequeño: buscar ruta normal (comportamiento original)
            return generarRutaSimple(pedido, destino, cantidadTotal, diaRelativo, horaPedido, hubsOrdenados);
        }
        
        // 🆕 PEDIDO GRANDE: Dividir en múltiples envíos
        log.info("📦 Pedido {} con {} paquetes excede capacidad típica ({}). Dividiendo...", 
                 pedido.getId(), cantidadTotal, CAPACIDAD_TIPICA_VUELO);
        
        return generarRutasDivididas(pedido, destino, cantidadTotal, diaRelativo, horaPedido, hubsOrdenados);
    }
    
    /**
     * 🆕 Genera una ruta simple para pedidos pequeños (comportamiento original)
     */
    private List<SubRuta> generarRutaSimple(PedidoSemanal pedido, String destino, int cantidad, 
            int diaRelativo, java.time.LocalTime horaPedido, List<String> hubsOrdenados) {
        List<SubRuta> subrutas = new ArrayList<>();
        
        for (String hub : hubsOrdenados) {
            SubRuta subruta = buscadorRutas.buscarRutaConHoraMinima(hub, destino, cantidad, diaRelativo, horaPedido);
            if (subruta != null) {
                log.trace("Pedido {} asignado a hub {} (destino: {}, hora salida >= {})", 
                         pedido.getId(), hub, destino, horaPedido);
                subrutas.add(subruta);
                break;
            }
        }
        
        return subrutas;
    }
    
    /**
     * 🆕 Genera múltiples rutas para pedidos grandes
     * Divide el pedido en varios envíos que quepan en los vuelos disponibles
     */
    private List<SubRuta> generarRutasDivididas(PedidoSemanal pedido, String destino, int cantidadTotal,
            int diaRelativo, java.time.LocalTime horaPedido, List<String> hubsOrdenados) {
        
        List<SubRuta> subrutas = new ArrayList<>();
        int cantidadRestante = cantidadTotal;
        int divisiones = 0;
        int diaActual = diaRelativo;
        java.time.LocalTime horaActual = horaPedido;
        
        // Intentar asignar toda la cantidad usando múltiples vuelos
        while (cantidadRestante > 0 && divisiones < MAX_DIVISIONES_POR_PEDIDO) {
            boolean asignado = false;
            
            // Intentar desde cada hub
            for (String hub : hubsOrdenados) {
                // Usar búsqueda con capacidad parcial
                BuscadorRutas.ResultadoBusquedaParcial resultado = 
                    buscadorRutas.buscarRutaConCapacidadParcial(hub, destino, cantidadRestante, diaActual, horaActual);
                
                if (resultado != null) {
                    SubRuta subruta = resultado.getSubruta();
                    int cantidadAsignada = resultado.getCantidadAsignada();
                    
                    // Actualizar la cantidad en la subruta
                    subruta.setCantidad(cantidadAsignada);
                    subrutas.add(subruta);
                    
                    cantidadRestante -= cantidadAsignada;
                    divisiones++;
                    asignado = true;
                    
                    log.debug("📦 Pedido {} división {}: {} paquetes vía {} (quedan {})", 
                             pedido.getId(), divisiones, cantidadAsignada, hub, cantidadRestante);
                    
                    // Para la siguiente iteración, usar una hora posterior
                    // (evitar asignar al mismo vuelo)
                    if (subruta.getVuelos() != null && !subruta.getVuelos().isEmpty()) {
                        // Avanzar la hora mínima para el siguiente envío
                        horaActual = horaActual.plusMinutes(30);
                        if (horaActual.getHour() >= 23) {
                            // Pasar al día siguiente
                            diaActual++;
                            horaActual = java.time.LocalTime.of(0, 0);
                        }
                    }
                    
                    break; // Salir del loop de hubs, continuar con la siguiente división
                }
            }
            
            // Si no se pudo asignar en ningún hub, intentar el día siguiente
            if (!asignado) {
                if (diaActual < diaRelativo + 2) {
                    diaActual++;
                    horaActual = java.time.LocalTime.of(0, 0);
                    log.trace("Pedido {}: intentando día {} para {} paquetes restantes", 
                             pedido.getId(), diaActual, cantidadRestante);
                } else {
                    log.warn("❌ Pedido {}: no se pudo asignar {} paquetes restantes después de {} divisiones", 
                            pedido.getId(), cantidadRestante, divisiones);
                    break;
                }
            }
        }
        
        // Log del resultado final
        if (!subrutas.isEmpty()) {
            int totalAsignado = subrutas.stream().mapToInt(SubRuta::getCantidad).sum();
            log.info("✅ Pedido {} dividido en {} envíos: {}/{} paquetes asignados ({}%)", 
                    pedido.getId(), subrutas.size(), totalAsignado, cantidadTotal,
                    (totalAsignado * 100 / cantidadTotal));
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
