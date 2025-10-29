package com.proyecto.backend.algoritmo.core;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.model.Pedido;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Decodificador basico para generar rutas de forma greedy
 *
 * Mejoras v2:
 * - Usa WorldTemporal con instancias de vuelos expandidas
 * - Verifica capacidades reales de cada vuelo
 * - Calcula dia correcto del pedido
 * - Usa fechas/horas UTC correctas
 *
 * Mejoras v3:
 * - Verifica capacidad de almacenes con ControladorAlmacenes
 * - Usa slots de tiempo (60 minutos) para rastrear ocupacion
 * - Reserva capacidad en almacenes intermedios y destino
 * - Busqueda BFS para multiples escalas (hasta MAX_ESCALAS)
 */
@Slf4j
public class DecodificadorBasico {

    // Maximo numero de escalas permitidas (configurable)
    private static final int MAX_ESCALAS = 3;

    private final WorldTemporal worldTemporal;
    private final ControladorAlmacenes controladorAlmacenes;
    private final CalculadorPlazos calculadorPlazos;

    public DecodificadorBasico(WorldTemporal worldTemporal, ControladorAlmacenes controladorAlmacenes) {
        this.worldTemporal = worldTemporal;
        this.controladorAlmacenes = controladorAlmacenes;
        this.calculadorPlazos = new CalculadorPlazos(worldTemporal);
    }

    /**
     * Clase interna para mantener rutas parciales durante busqueda BFS
     */
    private static class RutaParcial {
        String aeropuertoActual;
        List<VueloInstancia> vuelosAcumulados;
        int diaActual;
        int numeroEscalas;

        RutaParcial(String aeropuerto, int dia) {
            this.aeropuertoActual = aeropuerto;
            this.vuelosAcumulados = new ArrayList<>();
            this.diaActual = dia;
            this.numeroEscalas = 0;
        }

        RutaParcial(RutaParcial anterior, VueloInstancia vuelo) {
            this.aeropuertoActual = vuelo.getDestino();
            this.vuelosAcumulados = new ArrayList<>(anterior.vuelosAcumulados);
            this.vuelosAcumulados.add(vuelo);
            this.diaActual = vuelo.getDiaRelativo();
            this.numeroEscalas = anterior.numeroEscalas + 1;
        }
    }

    /**
     * Genera una solucion basica para una lista de pedidos
     *
     * @param pedidos Lista de pedidos a procesar
     * @return Solucion con rutas generadas y metricas calculadas
     */
    public Solution generarSolucion(List<Pedido> pedidos) {
        Solution solucion = new Solution();

        log.info("Iniciando generacion de rutas para {} pedidos", pedidos.size());

        int rutasGeneradas = 0;
        int rutasFallidas = 0;

        for (Pedido pedido : pedidos) {
            try {
                List<SubRuta> subrutas = generarRutasPedido(pedido);

                if (!subrutas.isEmpty()) {
                    solucion.agregarRutas(pedido, subrutas);
                    rutasGeneradas++;
                } else {
                    log.warn("No se pudo generar ruta para pedido {} a {}",
                            pedido.getId(), pedido.getAeropuertoDestinoId());
                    rutasFallidas++;
                    // Agregar pedido sin rutas (sera clasificado como NO_ENTREGADO)
                    solucion.agregarRutas(pedido, new ArrayList<>());
                }

            } catch (Exception e) {
                log.error("Error generando ruta para pedido {}", pedido.getId(), e);
                rutasFallidas++;
                // Agregar pedido sin rutas (sera clasificado como NO_ENTREGADO)
                solucion.agregarRutas(pedido, new ArrayList<>());
            }
        }

        log.info("Rutas generadas: {}, fallidas: {}", rutasGeneradas, rutasFallidas);

        // Calcular metricas de entrega (a tiempo, tarde, no entregados)
        solucion.calcularMetricas(calculadorPlazos);

        log.info("Metricas calculadas: {} a tiempo, {} tarde, {} no entregados",
                solucion.getPedidosATiempo(), solucion.getPedidosTarde(), solucion.getPedidosNoEntregados());

        // Calcular objetivo basico (por ahora solo el numero de rutas)
        // TODO: implementar funcion fitness completa
        solucion.setObjetivo(rutasGeneradas * 1.0);

        return solucion;
    }

    /**
     * Genera rutas para un pedido especifico
     * Estrategia: buscar desde el hub mas cercano al destino
     *
     * @param pedido Pedido a procesar
     * @return Lista de subrutas (normalmente 1)
     */
    private List<SubRuta> generarRutasPedido(Pedido pedido) {
        List<SubRuta> subrutas = new ArrayList<>();

        String destino = pedido.getAeropuertoDestinoId();
        int cantidad = pedido.getCantidadProductos();

        // Verificar que el destino exista
        Aeropuerto aeropuertoDestino = worldTemporal.getAeropuerto(destino);
        if (aeropuertoDestino == null) {
            log.warn("Aeropuerto destino {} no existe", destino);
            return subrutas;
        }

        // Calcular dia relativo del pedido
        LocalDate fechaPedido = LocalDate.of(pedido.getAnio(), pedido.getMes(), pedido.getDia());
        int diaRelativo = worldTemporal.calcularDiaRelativo(fechaPedido);

        if (diaRelativo < 0) {
            log.warn("Pedido {} fuera del horizonte temporal: {}",
                    pedido.getId(), fechaPedido);
            return subrutas;
        }

        // Intentar generar ruta desde cada hub
        for (String hub : worldTemporal.getHubs()) {
            SubRuta subruta = buscarRutaDesdeHub(hub, destino, cantidad, diaRelativo);

            if (subruta != null) {
                subrutas.add(subruta);
                break; // Solo necesitamos una ruta por ahora
            }
        }

        return subrutas;
    }

    /**
     * Busca una ruta desde un hub hasta el destino usando BFS
     * Explora rutas con 0 hasta MAX_ESCALAS escalas
     *
     * @param hub Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaRelativo Dia relativo del pedido
     * @return SubRuta o null si no se encuentra
     */
    private SubRuta buscarRutaDesdeHub(String hub, String destino, int cantidad, int diaRelativo) {
        return buscarRutaConEscalas(hub, destino, cantidad, diaRelativo);
    }

    /**
     * Busqueda BFS para encontrar ruta con verificacion de capacidades
     * Explora rutas desde hub hasta destino con limite de escalas
     *
     * @param origen Hub de origen
     * @param destino Aeropuerto destino
     * @param cantidad Cantidad de productos
     * @param diaInicial Dia relativo inicial
     * @return SubRuta o null si no se encuentra
     */
    private SubRuta buscarRutaConEscalas(String origen, String destino, int cantidad, int diaInicial) {
        Queue<RutaParcial> cola = new LinkedList<>();
        cola.add(new RutaParcial(origen, diaInicial));

        while (!cola.isEmpty()) {
            RutaParcial actual = cola.poll();

            // Si llegamos al destino, crear subruta
            if (actual.aeropuertoActual.equals(destino) && !actual.vuelosAcumulados.isEmpty()) {
                return crearSubRutaDesdeRutaParcial(origen, actual, cantidad);
            }

            // Si alcanzamos el maximo de escalas, no expandir mas
            if (actual.numeroEscalas >= MAX_ESCALAS) {
                continue;
            }

            // Explorar vuelos desde el aeropuerto actual
            expandirRutaParcial(actual, destino, cantidad, cola);
        }

        return null; // No se encontro ruta
    }

    /**
     * Expande una ruta parcial explorando vuelos disponibles
     *
     * @param rutaActual Ruta parcial actual
     * @param destinoFinal Destino final del pedido
     * @param cantidad Cantidad de productos
     * @param cola Cola de rutas a explorar
     */
    private void expandirRutaParcial(RutaParcial rutaActual, String destinoFinal,
                                     int cantidad, Queue<RutaParcial> cola) {
        String aeropuertoActual = rutaActual.aeropuertoActual;
        int diaActual = rutaActual.diaActual;

        // Buscar vuelos desde aeropuerto actual en dia actual y siguiente
        for (int diaOffset = 0; diaOffset <= 1; diaOffset++) {
            int dia = diaActual + diaOffset;

            if (dia >= worldTemporal.getNumeroDias()) {
                break; // Fuera del horizonte temporal
            }

            List<VueloInstancia> vuelosDisponibles = worldTemporal.getVuelosDesde(aeropuertoActual, dia);

            for (VueloInstancia vuelo : vuelosDisponibles) {
                // No volver al origen
                if (vuelo.getDestino().equals(rutaActual.vuelosAcumulados.isEmpty() ?
                        aeropuertoActual : rutaActual.vuelosAcumulados.get(0).getOrigen())) {
                    continue;
                }

                // Si no es el primer vuelo, verificar conexion valida
                if (!rutaActual.vuelosAcumulados.isEmpty()) {
                    VueloInstancia ultimoVuelo = rutaActual.vuelosAcumulados.get(
                            rutaActual.vuelosAcumulados.size() - 1);

                    if (vuelo.getSalidaUTC().isBefore(ultimoVuelo.getLlegadaUTC())) {
                        continue; // No puede tomar este vuelo (sale antes de que llegue)
                    }
                }

                // Verificar capacidad del vuelo
                if (!vuelo.tieneCapacidad(cantidad)) {
                    continue;
                }

                // Verificar capacidad del almacen destino del vuelo
                if (!verificarCapacidadAlmacen(vuelo, destinoFinal, cantidad, rutaActual)) {
                    continue;
                }

                // Agregar a la cola para explorar
                cola.add(new RutaParcial(rutaActual, vuelo));
            }
        }
    }

    /**
     * Verifica si hay capacidad en el almacen para este vuelo
     *
     * @param vuelo Vuelo a verificar
     * @param destinoFinal Destino final del pedido
     * @param cantidad Cantidad de productos
     * @param rutaActual Ruta parcial actual
     * @return true si hay capacidad
     */
    private boolean verificarCapacidadAlmacen(VueloInstancia vuelo, String destinoFinal,
                                              int cantidad, RutaParcial rutaActual) {
        String destinoVuelo = vuelo.getDestino();
        java.time.LocalDateTime llegada = vuelo.getLlegadaUTC();

        // Si es el destino final, ventana de recojo de 2 horas
        if (destinoVuelo.equals(destinoFinal)) {
            java.time.LocalDateTime fin = llegada.plusHours(2);
            return controladorAlmacenes.puedeCaber(destinoVuelo, llegada, fin, cantidad);
        }

        // Si es escala intermedia, calcular tiempo de espera hasta proximo vuelo
        // Por ahora usar ventana fija de 24 horas (peor caso)
        // TODO: optimizar calculando el proximo vuelo real
        java.time.LocalDateTime fin = llegada.plusHours(24);
        return controladorAlmacenes.puedeCaber(destinoVuelo, llegada, fin, cantidad);
    }

    /**
     * Crea una SubRuta desde una RutaParcial completa
     *
     * @param hubOrigen Hub de origen
     * @param rutaParcial Ruta parcial completa
     * @param cantidad Cantidad de productos
     * @return SubRuta creada
     */
    private SubRuta crearSubRutaDesdeRutaParcial(String hubOrigen, RutaParcial rutaParcial, int cantidad) {
        SubRuta subruta = new SubRuta(hubOrigen, cantidad);

        // Asignar capacidades y reservar almacenes
        for (int i = 0; i < rutaParcial.vuelosAcumulados.size(); i++) {
            VueloInstancia vuelo = rutaParcial.vuelosAcumulados.get(i);

            // Asignar capacidad del vuelo
            if (!vuelo.asignarCapacidad(cantidad)) {
                // Revertir asignaciones anteriores
                for (int j = 0; j < i; j++) {
                    rutaParcial.vuelosAcumulados.get(j).liberarCapacidad(cantidad);
                }
                return null; // No deberia pasar si verificamos bien antes
            }

            // Reservar almacen
            String destinoVuelo = vuelo.getDestino();
            java.time.LocalDateTime llegada = vuelo.getLlegadaUTC();
            java.time.LocalDateTime fin;

            // Ultimo vuelo: ventana de recojo de 2 horas
            if (i == rutaParcial.vuelosAcumulados.size() - 1) {
                fin = llegada.plusHours(2);
            } else {
                // Intermedio: hasta salida del proximo vuelo
                VueloInstancia proximoVuelo = rutaParcial.vuelosAcumulados.get(i + 1);
                fin = proximoVuelo.getSalidaUTC();
            }

            controladorAlmacenes.agregarIntervalo(destinoVuelo, llegada, fin, cantidad);

            // Convertir a VueloUso y agregar
            VueloUso vueloUso = vuelo.toVueloUso();
            vueloUso.setCantidadAsignada(cantidad);
            subruta.agregarVuelo(vueloUso);
        }

        log.debug("Ruta encontrada con {} escala(s): {} vuelos",
                rutaParcial.numeroEscalas - 1, rutaParcial.vuelosAcumulados.size());

        return subruta;
    }

}
