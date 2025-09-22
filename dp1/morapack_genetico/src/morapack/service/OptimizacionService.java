package morapack.service;

import morapack.dao.*;
import morapack.dao.impl.*;
import morapack.modelo.*;
import morapack.genetico.core.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para la optimización de asignación de pedidos usando algoritmos genéticos
 */
public class OptimizacionService {
    
    private final PedidoDAO pedidoDAO;
    private final SedeDAO sedeDAO;
    private final AeropuertoDAO aeropuertoDAO;
    private final AlgoritmoGenetico algoritmoGenetico;
    
    public OptimizacionService() {
        this.pedidoDAO = new PedidoDAOImpl();
        this.sedeDAO = new SedeDAOImpl();
        this.aeropuertoDAO = new AeropuertoDAOImpl();
        this.algoritmoGenetico = new AlgoritmoGenetico();
    }
    
    public OptimizacionService(PedidoDAO pedidoDAO, SedeDAO sedeDAO, AeropuertoDAO aeropuertoDAO) {
        this.pedidoDAO = pedidoDAO;
        this.sedeDAO = sedeDAO;
        this.aeropuertoDAO = aeropuertoDAO;
        this.algoritmoGenetico = new AlgoritmoGenetico();
    }
    
    /**
     * Optimiza la asignación de pedidos a sedes usando algoritmo genético
     */
    public ResultadoOptimizacion optimizarAsignaciones() {
        List<Pedido> pedidosPendientes = pedidoDAO.obtenerParaOptimizacion();
        List<Sede> sedesDisponibles = sedeDAO.obtenerDisponibles();
        
        if (pedidosPendientes.isEmpty() || sedesDisponibles.isEmpty()) {
            return new ResultadoOptimizacion(false, "No hay pedidos pendientes o sedes disponibles", 
                                           new ArrayList<>(), 0.0, 0);
        }
        
        // Crear población inicial de soluciones
        Poblacion poblacionInicial = crearPoblacionInicial(pedidosPendientes, sedesDisponibles);
        
        // Configurar parámetros del algoritmo genético
        configurarAlgoritmoGenetico();
        
        // Ejecutar optimización
        Poblacion poblacionFinal = algoritmoGenetico.ejecutar(poblacionInicial);
        
        // Obtener mejor solución
        IndividuoAsignacion mejorSolucion = (IndividuoAsignacion) poblacionFinal.getMejorIndividuo();
        
        // Aplicar asignaciones
        List<AsignacionPedido> asignaciones = aplicarAsignaciones(mejorSolucion, pedidosPendientes, sedesDisponibles);
        
        return new ResultadoOptimizacion(true, "Optimización completada exitosamente", 
                                       asignaciones, mejorSolucion.getFitness(), 
                                       poblacionFinal.getGeneracionActual());
    }
    
    /**
     * Crea una población inicial de soluciones aleatorias
     */
    private Poblacion crearPoblacionInicial(List<Pedido> pedidos, List<Sede> sedes) {
        int tamanoPoblacion = Math.min(50, Math.max(10, pedidos.size() * 2));
        Poblacion poblacion = new Poblacion(tamanoPoblacion);
        
        for (int i = 0; i < tamanoPoblacion; i++) {
            IndividuoAsignacion individuo = new IndividuoAsignacion(pedidos.size(), sedes.size());
            individuo.inicializar();
            individuo.calcularFitness(pedidos, sedes);
            poblacion.agregarIndividuo(individuo);
        }
        
        return poblacion;
    }
    
    /**
     * Configura los parámetros del algoritmo genético
     */
    private void configurarAlgoritmoGenetico() {
        algoritmoGenetico.setTamanoPoblacion(50);
        algoritmoGenetico.setNumeroGeneraciones(100);
        algoritmoGenetico.setProbabilidadCruce(0.8);
        algoritmoGenetico.setProbabilidadMutacion(0.1);
        algoritmoGenetico.setElitismo(true);
        algoritmoGenetico.setPorcentajeElite(0.1);
    }
    
    /**
     * Aplica las asignaciones encontradas por el algoritmo genético
     */
    private List<AsignacionPedido> aplicarAsignaciones(IndividuoAsignacion solucion, 
                                                      List<Pedido> pedidos, 
                                                      List<Sede> sedes) {
        List<AsignacionPedido> asignaciones = new ArrayList<>();
        
        for (int i = 0; i < pedidos.size(); i++) {
            int indiceSedeAsignada = solucion.getGenes()[i];
            if (indiceSedeAsignada >= 0 && indiceSedeAsignada < sedes.size()) {
                Pedido pedido = pedidos.get(i);
                Sede sede = sedes.get(indiceSedeAsignada);
                
                // Actualizar estado del pedido
                pedido.setEstado("ASIGNADO");
                pedido.setSedeAsignadaId(sede.getId());
                pedidoDAO.actualizar(pedido);
                
                // Asignar pedido a la sede
                sede.asignarPedido(pedido.getId());
                sedeDAO.actualizar(sede);
                
                asignaciones.add(new AsignacionPedido(pedido.getId(), sede.getId(), 
                                                    calcularCostoAsignacion(pedido, sede)));
            }
        }
        
        return asignaciones;
    }
    
    /**
     * Calcula el costo de asignar un pedido a una sede específica
     */
    private double calcularCostoAsignacion(Pedido pedido, Sede sede) {
        // Obtener aeropuertos de origen y destino
        Optional<Aeropuerto> aeropuertoOrigen = aeropuertoDAO.obtenerPorId(pedido.getAeropuertoOrigenId());
        Optional<Aeropuerto> aeropuertoSede = aeropuertoDAO.obtenerPorId(sede.getAeropuertoId());
        
        if (aeropuertoOrigen.isPresent() && aeropuertoSede.isPresent()) {
            // Calcular distancia entre aeropuertos
            double distancia = calcularDistancia(aeropuertoOrigen.get(), aeropuertoSede.get());
            
            // Costo base por distancia
            double costoDistancia = distancia * 0.5; // $0.5 por km
            
            // Factor de urgencia
            double factorUrgencia = "URGENTE".equals(pedido.getPrioridad()) ? 1.5 : 1.0;
            
            // Factor de capacidad de la sede
            double factorCapacidad = sede.puedeAtender() ? 1.0 : 2.0;
            
            return costoDistancia * factorUrgencia * factorCapacidad;
        }
        
        return 1000.0; // Costo alto si no se pueden obtener los aeropuertos
    }
    
    /**
     * Calcula la distancia entre dos aeropuertos usando la fórmula de Haversine
     */
    private double calcularDistancia(Aeropuerto origen, Aeropuerto destino) {
        double lat1 = Math.toRadians(origen.getLatitud());
        double lon1 = Math.toRadians(origen.getLongitud());
        double lat2 = Math.toRadians(destino.getLatitud());
        double lon2 = Math.toRadians(destino.getLongitud());
        
        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;
        
        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dlon/2) * Math.sin(dlon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double r = 6371; // Radio de la Tierra en km
        
        return r * c;
    }
    
    /**
     * Obtiene estadísticas del sistema
     */
    public EstadisticasSistema obtenerEstadisticas() {
        long totalPedidos = pedidoDAO.contarTotal();
        long pedidosPendientes = pedidoDAO.contarPorEstado("PENDIENTE");
        long pedidosAsignados = pedidoDAO.contarPorEstado("ASIGNADO");
        
        long totalSedes = sedeDAO.contarTotal();
        long sedesActivas = sedeDAO.contarActivas();
        double capacidadTotal = sedeDAO.calcularCapacidadTotal();
        double capacidadDisponible = sedeDAO.calcularCapacidadDisponible();
        
        return new EstadisticasSistema(totalPedidos, pedidosPendientes, pedidosAsignados,
                                     totalSedes, sedesActivas, capacidadTotal, capacidadDisponible);
    }
    
    // Getters para acceso a DAOs
    public PedidoDAO getPedidoDAO() { return pedidoDAO; }
    public SedeDAO getSedeDAO() { return sedeDAO; }
    public AeropuertoDAO getAeropuertoDAO() { return aeropuertoDAO; }
}
