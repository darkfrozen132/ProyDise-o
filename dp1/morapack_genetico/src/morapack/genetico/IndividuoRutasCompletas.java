package morapack.genetico;

import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import morapack.planificacion.RutaCompleta;
import morapack.planificacion.PlanificadorConexiones;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Individuo del algoritmo genético que representa rutas completas
 */
public class IndividuoRutasCompletas {
    
    private List<RutaCompleta> rutas;
    private List<Pedido> pedidos;
    private PlanificadorConexiones planificador;
    private double fitness;
    
    public IndividuoRutasCompletas(List<Pedido> pedidos, List<Vuelo> vuelos) {
        this.pedidos = pedidos;
        this.planificador = new PlanificadorConexiones(vuelos);
        this.rutas = new ArrayList<>();
        this.fitness = 0.0;
    }
    
    /**
     * INICIALIZACIÓN: Genera rutas aleatorias para cada pedido
     */
    public void inicializarAleatorio() {
        rutas.clear();
        Random random = new Random();
        
        for (Pedido pedido : pedidos) {
            // Extraer origen y destino del pedido
            String[] partes = pedido.getId().split("-");
            String destino = partes.length > 3 ? partes[3] : "SKBO";
            String hora = partes.length > 1 ? partes[1] + ":" + partes[2] : "12:00";
            
            // Origen aleatorio de las 3 sedes
            String[] origenes = {"SPIM", "EBCI", "UBBB"};
            String origen = origenes[random.nextInt(3)];
            
            // Buscar mejor ruta
            RutaCompleta ruta = planificador.buscarMejorRuta(origen, destino, hora);
            if (ruta == null || !ruta.esViable()) {
                // Si no hay ruta, crear una ruta vacía (penalización)
                ruta = new RutaCompleta();
                ruta.setTipoRuta("SIN_RUTA");
            }
            
            rutas.add(ruta);
        }
    }
    
    /**
     * CRUCE: Combina rutas de dos padres
     */
    public IndividuoRutasCompletas cruzarCon(IndividuoRutasCompletas otro, double probabilidad) {
        IndividuoRutasCompletas hijo = new IndividuoRutasCompletas(this.pedidos, null);
        hijo.planificador = this.planificador;
        
        Random random = new Random();
        
        for (int i = 0; i < pedidos.size(); i++) {
            if (random.nextDouble() < probabilidad) {
                // Tomar ruta del primer padre
                hijo.rutas.add(this.rutas.get(i));
            } else {
                // Tomar ruta del segundo padre
                hijo.rutas.add(otro.rutas.get(i));
            }
        }
        
        return hijo;
    }
    
    /**
     * MUTACIÓN: Replanifica algunas rutas aleatoriamente
     */
    public void mutar(double probabilidad) {
        Random random = new Random();
        
        for (int i = 0; i < rutas.size(); i++) {
            if (random.nextDouble() < probabilidad) {
                Pedido pedido = pedidos.get(i);
                
                // Extraer información del pedido
                String[] partes = pedido.getId().split("-");
                String destino = partes.length > 3 ? partes[3] : "SKBO";
                String hora = partes.length > 1 ? partes[1] + ":" + partes[2] : "12:00";
                
                // Cambiar origen (mutación)
                String[] origenes = {"SPIM", "EBCI", "UBBB"};
                String nuevoOrigen = origenes[random.nextInt(3)];
                
                // Replanificar ruta
                RutaCompleta nuevaRuta = planificador.buscarMejorRuta(nuevoOrigen, destino, hora);
                if (nuevaRuta != null && nuevaRuta.esViable()) {
                    rutas.set(i, nuevaRuta);
                }
            }
        }
    }
    
    /**
     * EVALUACIÓN: Calcula fitness basado en rutas completas
     */
    public void evaluarFitness() {
        double fitnessTotal = 0.0;
        int productosATiempo = 0;
        int productosRetrasados = 0;
        
        for (int i = 0; i < rutas.size(); i++) {
            RutaCompleta ruta = rutas.get(i);
            Pedido pedido = pedidos.get(i);
            
            if (!ruta.esViable()) {
                // Penalización por no tener ruta
                productosRetrasados += pedido.getCantidadProductos();
                fitnessTotal -= 1000 * pedido.getCantidadProductos();
                continue;
            }
            
            // Evaluar si llega a tiempo
            int tiempoTotal = ruta.calcularTiempoTotal();
            String[] partes = pedido.getId().split("-");
            int horaDeseada = Integer.parseInt(partes[1]) * 60 + Integer.parseInt(partes[2]);
            
            // Calcular hora de llegada estimada
            int horaLlegada = tiempoTotal; // Simplificado
            
            if (horaLlegada <= horaDeseada + 120) { // 2 horas de tolerancia
                productosATiempo += pedido.getCantidadProductos();
                fitnessTotal += pedido.getCantidadProductos();
            } else {
                productosRetrasados += pedido.getCantidadProductos();
                fitnessTotal -= 1000 * pedido.getCantidadProductos();
            }
            
            // Penalización por conexiones (más conexiones = más costo)
            int numConexiones = ruta.getVuelos().size() - 1;
            fitnessTotal -= numConexiones * 100; // Costo por conexión
        }
        
        this.fitness = fitnessTotal;
    }
    
    /**
     * Obtiene descripción detallada del individuo
     */
    public String obtenerDescripcion() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INDIVIDUO CON RUTAS COMPLETAS ===\n");
        sb.append(String.format("Fitness: %.2f\n", fitness));
        
        for (int i = 0; i < Math.min(5, rutas.size()); i++) {
            RutaCompleta ruta = rutas.get(i);
            Pedido pedido = pedidos.get(i);
            
            sb.append(String.format("Pedido %d: %s -> %s\n", 
                     i + 1, pedido.getId(), ruta.obtenerDescripcion()));
        }
        
        return sb.toString();
    }
    
    // Getters y setters
    public List<RutaCompleta> getRutas() { return rutas; }
    public double getFitness() { return fitness; }
    public void setFitness(double fitness) { this.fitness = fitness; }
}
