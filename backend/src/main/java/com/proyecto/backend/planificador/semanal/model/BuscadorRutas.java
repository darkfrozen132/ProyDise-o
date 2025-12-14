package com.proyecto.backend.planificador.semanal.model;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Buscador de rutas usando BFS (Breadth-First Search)
 *
 * Encapsula la logica de busqueda de rutas con verificacion de capacidades
 * de vuelos y almacenes. Puede ser reutilizado por diferentes decodificadores.
 * 
 * 🆕 MEJORA: Soporta división de pedidos grandes en múltiples vuelos
 */
@Slf4j
public class BuscadorRutas {

    // Maximo numero de escalas permitidas
    private static final int MAX_ESCALAS = 3;
    
    // 🆕 Capacidad mínima para considerar un vuelo (evitar fragmentación excesiva)
    private static final int CAPACIDAD_MINIMA_UTIL = 50;

    private final WorldTemporal worldTemporal;
    private final ControladorAlmacenes controladorAlmacenes;

    /**
     * Constructor
     *
     * @param worldTemporal World temporal con vuelos expandidos
     * @param controladorAlmacenes Controlador de capacidad de almacenes
     */
    public BuscadorRutas(WorldTemporal worldTemporal, ControladorAlmacenes controladorAlmacenes) {
        this.worldTemporal = worldTemporal;
        this.controladorAlmacenes = controladorAlmacenes;
    }

    /**
     * Busca una ruta desde origen hasta destino usando BFS
     *
     * @param origen Aeropuerto de origen (hub)
     * @param destino Aeropuerto de destino
     * @param cantidad Cantidad de productos
     * @param diaInicial Dia relativo inicial
     * @return SubRuta o null si no se encuentra
     */
    public SubRuta buscarRuta(String origen, String destino, int cantidad, int diaInicial) {
        return buscarRutaConHoraMinima(origen, destino, cantidad, diaInicial, null);
    }
    
    /**
     * 🆕 Busca una ruta desde origen hasta destino usando BFS
     * Solo considera vuelos que salgan DESPUÉS de la hora mínima especificada.
     * 
     * Esto permite que pedidos de diferentes horas usen diferentes vuelos,
     * distribuyendo así los pedidos entre todos los horarios de vuelos disponibles.
     *
     * @param origen Aeropuerto de origen (hub)
     * @param destino Aeropuerto de destino
     * @param cantidad Cantidad de productos
     * @param diaInicial Dia relativo inicial
     * @param horaMinima Hora mínima de salida (puede ser null para ignorar)
     * @return SubRuta o null si no se encuentra
     */
    public SubRuta buscarRutaConHoraMinima(String origen, String destino, int cantidad, int diaInicial, java.time.LocalTime horaMinima) {
        // 🆕 PRIMER INTENTO: con restricción de hora mínima
        SubRuta resultado = buscarRutaInterno(origen, destino, cantidad, diaInicial, horaMinima);
        
        // 🆕 SEGUNDO INTENTO: sin restricción de hora (fallback)
        if (resultado == null && horaMinima != null) {
            log.trace("Segundo intento sin restricción de hora para {} → {}", origen, destino);
            resultado = buscarRutaInterno(origen, destino, cantidad, diaInicial, null);
        }
        
        return resultado;
    }
    
    /**
     * 🆕 Busca una ruta que pueda llevar AL MENOS una parte del pedido
     * Retorna la cantidad máxima que puede transportar en esa ruta
     * 
     * Usado para dividir pedidos grandes en múltiples envíos
     *
     * @param origen Aeropuerto de origen (hub)
     * @param destino Aeropuerto de destino
     * @param cantidadDeseada Cantidad ideal de productos
     * @param diaInicial Dia relativo inicial
     * @param horaMinima Hora mínima de salida (puede ser null)
     * @return ResultadoBusquedaParcial con la subruta y cantidad asignada, o null si no hay ruta
     */
    public ResultadoBusquedaParcial buscarRutaConCapacidadParcial(String origen, String destino, 
            int cantidadDeseada, int diaInicial, java.time.LocalTime horaMinima) {
        
        // Primero intentar con la cantidad completa
        SubRuta rutaCompleta = buscarRutaConHoraMinima(origen, destino, cantidadDeseada, diaInicial, horaMinima);
        if (rutaCompleta != null) {
            return new ResultadoBusquedaParcial(rutaCompleta, cantidadDeseada);
        }
        
        // Si no cabe completo, buscar la máxima capacidad disponible
        int capacidadMaxDisponible = obtenerCapacidadMaximaDisponible(origen, destino, diaInicial, horaMinima);
        
        if (capacidadMaxDisponible >= CAPACIDAD_MINIMA_UTIL) {
            // Intentar con la capacidad máxima disponible
            SubRuta rutaParcial = buscarRutaConHoraMinima(origen, destino, capacidadMaxDisponible, diaInicial, horaMinima);
            if (rutaParcial != null) {
                log.debug("🔀 Ruta parcial encontrada: {} → {} con {}/{} paquetes", 
                         origen, destino, capacidadMaxDisponible, cantidadDeseada);
                return new ResultadoBusquedaParcial(rutaParcial, capacidadMaxDisponible);
            }
        }
        
        return null;
    }
    
    /**
     * 🆕 Obtiene la capacidad máxima disponible para una ruta
     * Busca el vuelo con más espacio libre en la ruta más corta
     */
    private int obtenerCapacidadMaximaDisponible(String origen, String destino, int diaInicial, java.time.LocalTime horaMinima) {
        int maxCapacidad = 0;
        
        // Buscar en los próximos 2 días
        for (int diaOffset = 0; diaOffset <= 1; diaOffset++) {
            int dia = diaInicial + diaOffset;
            if (dia >= worldTemporal.getNumeroDias()) break;
            
            List<VueloInstancia> vuelos = worldTemporal.getVuelosDesde(origen, dia);
            
            for (VueloInstancia vuelo : vuelos) {
                if (!vuelo.esModificable()) continue;
                
                // Filtro de hora mínima para el primer día
                if (diaOffset == 0 && horaMinima != null) {
                    java.time.LocalTime horaSalida = vuelo.getSalidaUTC().toLocalTime();
                    if (horaSalida.isBefore(horaMinima)) continue;
                }
                
                int capacidadRestante = vuelo.getCapacidadRestante();
                
                // Si es vuelo directo al destino
                if (vuelo.getDestino().equals(destino)) {
                    maxCapacidad = Math.max(maxCapacidad, capacidadRestante);
                } else {
                    // Vuelo con escala - considerar la capacidad del primer tramo
                    // (la capacidad efectiva será la mínima entre todos los tramos)
                    maxCapacidad = Math.max(maxCapacidad, capacidadRestante);
                }
            }
        }
        
        return maxCapacidad;
    }
    
    /**
     * 🆕 Clase para retornar resultado de búsqueda parcial
     */
    public static class ResultadoBusquedaParcial {
        private final SubRuta subruta;
        private final int cantidadAsignada;
        
        public ResultadoBusquedaParcial(SubRuta subruta, int cantidadAsignada) {
            this.subruta = subruta;
            this.cantidadAsignada = cantidadAsignada;
        }
        
        public SubRuta getSubruta() { return subruta; }
        public int getCantidadAsignada() { return cantidadAsignada; }
    }
    
    /**
     * Búsqueda interna con BFS
     */
    private SubRuta buscarRutaInterno(String origen, String destino, int cantidad, int diaInicial, java.time.LocalTime horaMinima) {
        Queue<RutaParcial> cola = new LinkedList<>();
        RutaParcial inicial = new RutaParcial(origen, diaInicial);
        inicial.horaMinima = horaMinima;
        cola.add(inicial);
        
        int vuelosEvaluados = 0;
        int sinCapacidad = 0;
        int horaTemprana = 0;
        int sinAlmacen = 0;

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
            int[] stats = expandirRutaParcialConHoraMinimaConStats(actual, destino, cantidad, cola);
            vuelosEvaluados += stats[0];
            sinCapacidad += stats[1];
            horaTemprana += stats[2];
            sinAlmacen += stats[3];
        }
        
        // Log de diagnóstico si no encuentra ruta
        if (vuelosEvaluados > 0 && sinCapacidad > 0) {
            log.debug("🔍 BFS {} → {}: evaluados={}, sinCapacidad={}, horaTemprana={}, sinAlmacen={}",
                    origen, destino, vuelosEvaluados, sinCapacidad, horaTemprana, sinAlmacen);
        }

        return null; // No se encontro ruta
    }
    
    /**
     * 🆕 Versión con estadísticas para diagnóstico
     */
    private int[] expandirRutaParcialConHoraMinimaConStats(RutaParcial rutaActual, String destinoFinal,
                                     int cantidad, Queue<RutaParcial> cola) {
        int vuelosEvaluados = 0;
        int sinCapacidad = 0;
        int horaTemprana = 0;
        int sinAlmacen = 0;
        
        String aeropuertoActual = rutaActual.aeropuertoActual;
        int diaActual = rutaActual.diaActual;

        // Buscar vuelos desde aeropuerto actual en dia actual y siguiente
        for (int diaOffset = 0; diaOffset <= 1; diaOffset++) {
            int dia = diaActual + diaOffset;

            if (dia >= worldTemporal.getNumeroDias()) {
                break;
            }

            List<VueloInstancia> vuelosDisponibles = worldTemporal.getVuelosDesde(aeropuertoActual, dia);

            for (VueloInstancia vuelo : vuelosDisponibles) {
                vuelosEvaluados++;
                
                // ⚠️ FILTRO CRÍTICO: NO usar vuelos que ya despegaron o aterrizaron
                if (!vuelo.esModificable()) {
                    continue; // Vuelo EN_VUELO o ATERRIZADO → NO se puede reasignar
                }
                
                // 🆕 FILTRO DE HORA MÍNIMA: Solo para el primer vuelo
                if (rutaActual.vuelosAcumulados.isEmpty() && rutaActual.horaMinima != null) {
                    java.time.LocalTime horaSalidaVuelo = vuelo.getSalidaUTC().toLocalTime();
                    if (diaOffset == 0 && horaSalidaVuelo.isBefore(rutaActual.horaMinima)) {
                        horaTemprana++;
                        continue;
                    }
                }

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
                        continue;
                    }
                }

                // Verificar capacidad del vuelo
                if (!vuelo.tieneCapacidad(cantidad)) {
                    sinCapacidad++;
                    continue;
                }

                // Verificar capacidad del almacen
                if (!verificarCapacidadAlmacen(vuelo, destinoFinal, cantidad, rutaActual)) {
                    sinAlmacen++;
                    continue;
                }

                // Agregar a la cola para explorar
                cola.add(new RutaParcial(rutaActual, vuelo));
            }
        }
        
        return new int[] { vuelosEvaluados, sinCapacidad, horaTemprana, sinAlmacen };
    }

    /**
     * Expande una ruta parcial explorando vuelos disponibles
     */
    private void expandirRutaParcial(RutaParcial rutaActual, String destinoFinal,
                                     int cantidad, Queue<RutaParcial> cola) {
        expandirRutaParcialConHoraMinima(rutaActual, destinoFinal, cantidad, cola);
    }
    
    /**
     * 🆕 Expande una ruta parcial explorando vuelos disponibles
     * Considera la hora mínima de salida para el primer vuelo
     */
    private void expandirRutaParcialConHoraMinima(RutaParcial rutaActual, String destinoFinal,
                                     int cantidad, Queue<RutaParcial> cola) {
        String aeropuertoActual = rutaActual.aeropuertoActual;
        int diaActual = rutaActual.diaActual;

        // Buscar vuelos desde aeropuerto actual en dia actual y siguiente
        for (int diaOffset = 0; diaOffset <= 1; diaOffset++) {
            int dia = diaActual + diaOffset;

            if (dia >= worldTemporal.getNumeroDias()) {
                break;
            }

            List<VueloInstancia> vuelosDisponibles = worldTemporal.getVuelosDesde(aeropuertoActual, dia);

            for (VueloInstancia vuelo : vuelosDisponibles) {
                // ⚠️ FILTRO CRÍTICO: NO usar vuelos que ya despegaron o aterrizaron
                if (!vuelo.esModificable()) {
                    continue; // Vuelo EN_VUELO o ATERRIZADO → NO se puede reasignar
                }
                
                // 🆕 FILTRO DE HORA MÍNIMA: Solo para el primer vuelo
                // Si es el primer vuelo y hay hora mínima, el vuelo debe salir DESPUÉS de esa hora
                if (rutaActual.vuelosAcumulados.isEmpty() && rutaActual.horaMinima != null) {
                    java.time.LocalTime horaSalidaVuelo = vuelo.getSalidaUTC().toLocalTime();
                    if (diaOffset == 0 && horaSalidaVuelo.isBefore(rutaActual.horaMinima)) {
                        // El vuelo sale antes de que el pedido esté listo → NO usar
                        continue;
                    }
                }

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
                        continue;
                    }
                }

                // Verificar capacidad del vuelo
                if (!vuelo.tieneCapacidad(cantidad)) {
                    continue;
                }

                // Verificar capacidad del almacen
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

        // Si es escala intermedia, ventana de 24 horas (peor caso)
        java.time.LocalDateTime fin = llegada.plusHours(24);
        return controladorAlmacenes.puedeCaber(destinoVuelo, llegada, fin, cantidad);
    }

    /**
     * Crea una SubRuta desde una RutaParcial completa
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
                return null;
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

    /**
     * Clase interna para mantener rutas parciales durante busqueda BFS
     */
    private static class RutaParcial {
        String aeropuertoActual;
        List<VueloInstancia> vuelosAcumulados;
        int diaActual;
        int numeroEscalas;
        java.time.LocalTime horaMinima; // 🆕 Hora mínima de salida para el primer vuelo

        RutaParcial(String aeropuerto, int dia) {
            this.aeropuertoActual = aeropuerto;
            this.vuelosAcumulados = new ArrayList<>();
            this.diaActual = dia;
            this.numeroEscalas = 0;
            this.horaMinima = null;
        }

        RutaParcial(RutaParcial anterior, VueloInstancia vuelo) {
            this.aeropuertoActual = vuelo.getDestino();
            this.vuelosAcumulados = new ArrayList<>(anterior.vuelosAcumulados);
            this.vuelosAcumulados.add(vuelo);
            this.diaActual = vuelo.getDiaRelativo();
            this.numeroEscalas = anterior.numeroEscalas + 1;
            this.horaMinima = anterior.horaMinima; // 🆕 Propagar hora mínima
        }
    }
}
