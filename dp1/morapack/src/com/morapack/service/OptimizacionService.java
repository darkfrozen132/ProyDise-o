package com.morapack.service;

import com.morapack.model.Pedido;
import com.morapack.model.Sede;
import com.morapack.model.Aeropuerto;
import com.morapack.model.EstadoPedido;
import com.morapack.repository.jpa.PedidoRepository;
import com.morapack.repository.jpa.SedeRepository;
import com.morapack.repository.jpa.AeropuertoRepository;

// Imports del algoritmo genético
import genetico.algoritmo.AlgoritmoGenetico;
import genetico.Poblacion;
import genetico.Individuo;
import genetico.operators.seleccion.SeleccionTorneo;
import genetico.operators.cruce.CruceUnPunto;
import genetico.operators.mutacion.MutacionIntercambio;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de optimización que integra el algoritmo genético con la arquitectura Spring
 * Preparado para ser anotado con @Service cuando se integre Spring Boot
 */
public class OptimizacionService {
    
    private final PedidoRepository pedidoRepository;
    private final SedeRepository sedeRepository;
    private final AeropuertoRepository aeropuertoRepository;
    
    public OptimizacionService(PedidoRepository pedidoRepository, 
                             SedeRepository sedeRepository,
                             AeropuertoRepository aeropuertoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.sedeRepository = sedeRepository;
        this.aeropuertoRepository = aeropuertoRepository;
    }
    
    /**
     * Ejecuta la optimización de asignación de pedidos a sedes
     */
    public SolucionOptimizacion optimizarAsignacionPedidos() {
        return optimizarAsignacionPedidos(new ParametrosOptimizacion());
    }
    
    /**
     * Ejecuta la optimización con parámetros personalizados
     */
    public SolucionOptimizacion optimizarAsignacionPedidos(ParametrosOptimizacion parametros) {
        
        // 1. Obtener datos necesarios
        List<Pedido> pedidosPendientes = pedidoRepository.findByEstado(EstadoPedido.PENDIENTE);
        List<Sede> sedesActivas = sedeRepository.findByEstado(com.morapack.model.EstadoSede.ACTIVA);
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        
        if (pedidosPendientes.isEmpty()) {
            return new SolucionOptimizacion("No hay pedidos pendientes para optimizar", 0, 0.0);
        }
        
        if (sedesActivas.isEmpty()) {
            return new SolucionOptimizacion("No hay sedes activas disponibles", 0, 0.0);
        }
        
        // 2. Crear mapa de aeropuertos para acceso rápido
        Map<String, Aeropuerto> mapaAeropuertos = aeropuertos.stream()
            .collect(Collectors.toMap(Aeropuerto::getId, a -> a));
        
        // 3. Configurar y ejecutar algoritmo genético
        AlgoritmoGenetico<Integer> algoritmo = new AlgoritmoGenetico<>(
            parametros.getTamanoPoblacion(), 
            parametros.getNumeroGeneraciones()
        );
        
        algoritmo.setProbabilidadCruce(parametros.getProbabilidadCruce());
        algoritmo.setProbabilidadMutacion(parametros.getProbabilidadMutacion());
        algoritmo.setElitismo(true);
        // algoritmo.setTamanoElite(parametros.getTamanoElite()); // Método no disponible en la implementación actual
        
        // Configurar operadores
        algoritmo.setOperadorSeleccion(new SeleccionTorneo<>(3));
        algoritmo.setOperadorCruce(new CruceUnPunto<>());
        algoritmo.setOperadorMutacion(new MutacionIntercambio<>());
        
        // 4. Crear población inicial
        Poblacion<Integer> poblacionInicial = crearPoblacionInicial(
            pedidosPendientes, sedesActivas, mapaAeropuertos, parametros.getTamanoPoblacion()
        );
        
        // 5. Ejecutar optimización
        Individuo<Integer> mejorIndividuo = algoritmo.ejecutar(poblacionInicial);
        IndividuoMoraPack mejorIndividuoMoraPack = (IndividuoMoraPack) mejorIndividuo;
        
        // 6. Aplicar la solución encontrada
        SolucionOptimizacion solucion = aplicarSolucion(mejorIndividuoMoraPack, pedidosPendientes, sedesActivas);
        
        return solucion;
    }
    
    /**
     * Crea la población inicial para el algoritmo genético
     */
    private Poblacion<Integer> crearPoblacionInicial(List<Pedido> pedidos, List<Sede> sedes, 
                                                   Map<String, Aeropuerto> aeropuertos, int tamanoPoblacion) {
        Poblacion<Integer> poblacion = new Poblacion<>(tamanoPoblacion);
        
        for (int i = 0; i < tamanoPoblacion; i++) {
            IndividuoMoraPack individuo = new IndividuoMoraPack(pedidos, sedes, aeropuertos);
            individuo.inicializarAleatorio();
            poblacion.añadirIndividuo(individuo);
        }
        
        return poblacion;
    }
    
    /**
     * Aplica la solución encontrada por el algoritmo genético
     */
    private SolucionOptimizacion aplicarSolucion(IndividuoMoraPack mejorIndividuo, 
                                                List<Pedido> pedidos, List<Sede> sedes) {
        
        Map<String, Integer> distribucionPorSede = new HashMap<>();
        double costoTotal = 0.0;
        int pedidosAsignados = 0;
        
        // Aplicar asignaciones
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            Sede sedeAsignada = mejorIndividuo.obtenerSedeParaPedido(i);
            
            if (sedeAsignada != null) {
                // Asignar pedido a sede
                pedido.setSedeAsignadaId(sedeAsignada.getId());
                pedido.setEstado(EstadoPedido.ASIGNADO);
                pedidoRepository.save(pedido);
                
                // Actualizar estadísticas
                distribucionPorSede.merge(sedeAsignada.getId(), 1, Integer::sum);
                pedidosAsignados++;
                
                // Calcular costo (aproximado)
                costoTotal += calcularCostoPedido(pedido, sedeAsignada);
            }
        }
        
        return new SolucionOptimizacion(
            "Optimización completada exitosamente",
            pedidosAsignados,
            mejorIndividuo.getFitness(),
            costoTotal,
            distribucionPorSede
        );
    }
    
    /**
     * Calcula el costo aproximado de enviar un pedido desde una sede
     */
    private double calcularCostoPedido(Pedido pedido, Sede sede) {
        // Implementación simplificada
        double costoBase = pedido.getCantidadProductos() * 10.0;
        double factorPrioridad = 4.0 - pedido.getPrioridad();
        return costoBase * factorPrioridad;
    }
    
    /**
     * Clase para representar la solución de optimización
     */
    public static class SolucionOptimizacion {
        private final String mensaje;
        private final int pedidosAsignados;
        private final double fitness;
        private final double costoTotal;
        private final Map<String, Integer> distribucionPorSede;
        private final long timestamp;
        
        public SolucionOptimizacion(String mensaje, int pedidosAsignados, double fitness) {
            this(mensaje, pedidosAsignados, fitness, 0.0, new HashMap<>());
        }
        
        public SolucionOptimizacion(String mensaje, int pedidosAsignados, double fitness, 
                                  double costoTotal, Map<String, Integer> distribucionPorSede) {
            this.mensaje = mensaje;
            this.pedidosAsignados = pedidosAsignados;
            this.fitness = fitness;
            this.costoTotal = costoTotal;
            this.distribucionPorSede = distribucionPorSede != null ? new HashMap<>(distribucionPorSede) : new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public String getMensaje() { return mensaje; }
        public int getPedidosAsignados() { return pedidosAsignados; }
        public double getFitness() { return fitness; }
        public double getCostoTotal() { return costoTotal; }
        public Map<String, Integer> getDistribucionPorSede() { return new HashMap<>(distribucionPorSede); }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("SolucionOptimizacion{mensaje='%s', pedidosAsignados=%d, fitness=%.6f, costoTotal=%.2f}",
                               mensaje, pedidosAsignados, fitness, costoTotal);
        }
    }
    
    /**
     * Parámetros configurables para la optimización
     */
    public static class ParametrosOptimizacion {
        private int tamanoPoblacion = 50;
        private int numeroGeneraciones = 500;
        private double probabilidadCruce = 0.8;
        private double probabilidadMutacion = 0.1;
        private int tamanoElite = 5;
        
        // Constructor por defecto
        public ParametrosOptimizacion() {}
        
        // Constructor con parámetros
        public ParametrosOptimizacion(int tamanoPoblacion, int numeroGeneraciones, 
                                    double probabilidadCruce, double probabilidadMutacion, int tamanoElite) {
            this.tamanoPoblacion = tamanoPoblacion;
            this.numeroGeneraciones = numeroGeneraciones;
            this.probabilidadCruce = probabilidadCruce;
            this.probabilidadMutacion = probabilidadMutacion;
            this.tamanoElite = tamanoElite;
        }
        
        // Getters y setters
        public int getTamanoPoblacion() { return tamanoPoblacion; }
        public void setTamanoPoblacion(int tamanoPoblacion) { this.tamanoPoblacion = tamanoPoblacion; }
        
        public int getNumeroGeneraciones() { return numeroGeneraciones; }
        public void setNumeroGeneraciones(int numeroGeneraciones) { this.numeroGeneraciones = numeroGeneraciones; }
        
        public double getProbabilidadCruce() { return probabilidadCruce; }
        public void setProbabilidadCruce(double probabilidadCruce) { this.probabilidadCruce = probabilidadCruce; }
        
        public double getProbabilidadMutacion() { return probabilidadMutacion; }
        public void setProbabilidadMutacion(double probabilidadMutacion) { this.probabilidadMutacion = probabilidadMutacion; }
        
        public int getTamanoElite() { return tamanoElite; }
        public void setTamanoElite(int tamanoElite) { this.tamanoElite = tamanoElite; }
    }
    
    /**
     * Individuo del algoritmo genético especializado para MoraPack
     */
    public static class IndividuoMoraPack extends Individuo<Integer> {
        private List<Pedido> pedidos;
        private List<Sede> sedes;
        private Map<String, Aeropuerto> aeropuertos;
        
        public IndividuoMoraPack(List<Pedido> pedidos, List<Sede> sedes, Map<String, Aeropuerto> aeropuertos) {
            super(pedidos.size());
            this.pedidos = pedidos;
            this.sedes = sedes;
            this.aeropuertos = aeropuertos;
        }
        
        @Override
        protected Integer[] crearGenotipo(int tamaño) {
            return new Integer[tamaño];
        }
        
        @Override
        public void inicializarAleatorio() {
            Random random = new Random();
            for (int i = 0; i < genotipo.length; i++) {
                genotipo[i] = random.nextInt(sedes.size());
            }
            fitnessCalculado = false;
        }
        
        @Override
        public double calcularFitness() {
            if (fitnessCalculado) return fitness;
            
            double costoTotal = 0.0;
            Map<String, Integer> cargaPorSede = new HashMap<>();
            
            // Calcular costo y verificar restricciones
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido pedido = pedidos.get(i);
                Sede sede = sedes.get(genotipo[i]);
                
                // Contar carga por sede
                cargaPorSede.merge(sede.getId(), 1, Integer::sum);
                
                // Verificar capacidad
                if (cargaPorSede.get(sede.getId()) > sede.getCapacidadMaxima()) {
                    costoTotal += 10000; // Penalización por exceder capacidad
                }
                
                // Calcular costo base
                Aeropuerto aeropuertoOrigen = aeropuertos.get(sede.getAeropuertoId());
                Aeropuerto aeropuertoDestino = aeropuertos.get(pedido.getAeropuertoDestinoId());
                
                if (aeropuertoOrigen != null && aeropuertoDestino != null) {
                    boolean mismoContinente = aeropuertoOrigen.mismoContinente(aeropuertoDestino);
                    double distancia = aeropuertoOrigen.calcularDistancia(aeropuertoDestino);
                    costoTotal += pedido.calcularCosto(mismoContinente, distancia);
                }
            }
            
            // Fitness inverso al costo (menor costo = mayor fitness)
            this.fitness = 1.0 / (1.0 + costoTotal);
            this.fitnessCalculado = true;
            
            return fitness;
        }
        
        @Override
        public Individuo<Integer> clonar() {
            IndividuoMoraPack clon = new IndividuoMoraPack(pedidos, sedes, aeropuertos);
            clon.genotipo = this.genotipo.clone();
            clon.fitness = this.fitness;
            clon.fitnessCalculado = this.fitnessCalculado;
            return clon;
        }
        
        public Sede obtenerSedeParaPedido(int indicePedido) {
            if (indicePedido >= 0 && indicePedido < genotipo.length) {
                return sedes.get(genotipo[indicePedido]);
            }
            return null;
        }
    }
}
