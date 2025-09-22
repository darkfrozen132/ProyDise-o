package morapack.optimizacion;

import morapack.modelo.*;
import morapack.dao.impl.*;
import java.util.*;

/**
 * Función objetivo para optimizar asignación de productos en un día de operación
 * OBJETIVO: Retrasar el colapso logístico lo más tarde posible en el día
 * Enfoque: Análisis por PRODUCTO individual, no por pedido
 * Restricción principal: Capacidad máxima de todos los vuelos del día
 */
public class FuncionObjetivo {
    
    private final AeropuertoDAOImpl aeropuertoDAO;
    private final List<Pedido> pedidos;
    private final List<Vuelo> vuelos;
    private final double capacidadTotalDiaria; // Cdia = Capacidad máxima diaria
    
    /**
     * Constructor
     */
    public FuncionObjetivo(AeropuertoDAOImpl aeropuertoDAO, List<Pedido> pedidos, List<Vuelo> vuelos) {
        this.aeropuertoDAO = aeropuertoDAO;
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.capacidadTotalDiaria = calcularCapacidadTotalDiaria();
        
        System.out.printf("🎯 Función objetivo inicializada:%n");
        System.out.printf("   📦 Productos totales: %,d%n", getTotalProductos());
        System.out.printf("   🛩️  Vuelos disponibles: %,d%n", vuelos.size());
        System.out.printf("   📊 Capacidad total diaria (Cdia): %,d productos%n", (int)capacidadTotalDiaria);
        System.out.printf("   📈 Utilización actual: %.2f%%%n", (getTotalProductos() / capacidadTotalDiaria) * 100);
    }
    
    // Mapa de aeropuertos para consultas rápidas
    private final Map<String, Aeropuerto> aeropuertos;
    
    // CAPACIDADES MÁXIMAS POR DÍA DE OPERACIÓN
    private static final long CAPACIDAD_ALMACEN_AEROPUERTO = 100_000_000L; // 100 millones de espacios
    private static final int VUELOS_POR_DIA = 24; // Asumiendo 1 vuelo por hora
    
    // Pesos ajustados para evitar colapso logístico
    private static final double PESO_CAPACIDAD_DIA = 0.5;     // 50% - Crítico para evitar colapso
    private static final double PESO_DISTRIBUCION_TEMPORAL = 0.3; // 30% - Distribuir carga en el día
    private static final double PESO_EFICIENCIA_RUTA = 0.15;   // 15% - Optimizar rutas
    
    /**
     * Genera una solución aleatoria inicial
     */
    public int[] generarSolucionAleatoria() {
        Random random = new Random();
    private static final double PESO_PENALIZACION = 0.05;     // 5% - Penalizaciones menores
    
    // Constantes ajustadas para análisis por producto
    private static final double COSTO_POR_PRODUCTO_KM = 0.1;  // $0.1 por producto-kilómetro
    private static final double COSTO_SATURACION = 10000.0;   // Alto costo cuando se acerca capacidad máxima
    private static final double UMBRAL_COLAPSO = 0.85;        // 85% de capacidad = zona crítica
    
    public FuncionObjetivo(AeropuertoDAOImpl aeropuertoDAO, List<Pedido> pedidos) {
        this.aeropuertoDAO = aeropuertoDAO;
        this.pedidos = pedidos;
        this.aeropuertos = new HashMap<>();
        
        // Cargar aeropuertos en mapa para acceso rápido
        for (Aeropuerto aeropuerto : aeropuertoDAO.obtenerTodos()) {
            aeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
    }
    
    /**
     * Calcula el fitness basado en evitar el colapso logístico por día
     * ANÁLISIS POR PRODUCTO: cada producto se evalúa individualmente
     * @param solucion Array donde solucion[i] = índice del aeropuerto origen para el pedido i
     * @return Costo total (menor valor = mejor distribución que evita colapso)
     */
    public double calcularFitness(int[] solucion) {
        if (solucion.length != pedidos.size()) {
            return Double.MAX_VALUE; // Solución inválida
        }
        
        // Estructuras para análisis del día de operación
        Map<String, Integer> productosEnRuta = new HashMap<>(); // Aeropuerto -> total productos
        Map<Integer, List<Integer>> productosPorHora = new HashMap<>(); // Hora -> productos procesados
        List<Aeropuerto> aeropuertosSede = aeropuertoDAO.obtenerSedes();
        
        double costoTotal = 0.0;
        int totalProductos = 0;
        
        // ANÁLISIS POR PRODUCTO INDIVIDUAL (no por pedido)
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            int indiceOrigen = solucion[i];
            
            if (indiceOrigen < 0 || indiceOrigen >= aeropuertosSede.size()) {
                costoTotal += 50000; // Penalización alta por solución inválida
                continue;
            }
            
            Aeropuerto origen = aeropuertosSede.get(indiceOrigen);
            Aeropuerto destino = aeropuertos.get(pedido.getAeropuertoDestinoId());
            
            if (origen == null || destino == null) {
                costoTotal += 50000;
                continue;
            }
            
            // PROCESAR CADA PRODUCTO INDIVIDUALMENTE
            int cantidadProductos = pedido.getCantidadProductos();
            totalProductos += cantidadProductos;
            
            for (int producto = 0; producto < cantidadProductos; producto++) {
                // Calcular en qué hora del día se procesará este producto
                int hora = calcularHoraProcesamiento(pedido, producto, cantidadProductos);
                
                // Actualizar contadores
                productosEnRuta.merge(origen.getCodigoICAO(), 1, Integer::sum);
                productosPorHora.computeIfAbsent(hora, k -> new ArrayList<>()).add(1);
                
                // Calcular costos específicos para este producto
                costoTotal += calcularCostoIndividualProducto(origen, destino, pedido, producto, hora);
            }
        }
        
        // ANÁLISIS DE CAPACIDAD DIARIA Y RIESGO DE COLAPSO
        costoTotal += PESO_CAPACIDAD_DIA * calcularRiesgoColapso(productosEnRuta, productosPorHora, totalProductos);
        costoTotal += PESO_DISTRIBUCION_TEMPORAL * calcularDesbalanceTemporal(productosPorHora);
        
        return costoTotal;
    }
    
    /**
     * Calcula en qué hora del día se procesará un producto específico
     */
    private int calcularHoraProcesamiento(Pedido pedido, int numeroProducto, int totalProductos) {
        int horaBase = pedido.getHora();
        // Distribuir productos a lo largo de varias horas si hay muchos
        int incrementoHora = numeroProducto / 100; // Cada 100 productos = 1 hora más
        return (horaBase + incrementoHora) % 24;
    }
    
    /**
     * Calcula el costo individual de un producto específico
     */
    private double calcularCostoIndividualProducto(Aeropuerto origen, Aeropuerto destino, 
                                                   Pedido pedido, int numeroProducto, int hora) {
        double costo = 0.0;
        
        // Costo base por distancia GMT para este producto
        double distancia = origen.calcularDistancia(destino);
        costo += distancia * COSTO_POR_PRODUCTO_KM;
        
        // Factor de urgencia por hora (más caro en horas pico)
        double factorHora = calcularFactorHorario(hora);
        costo *= factorHora;
        
        // Penalización por prioridad alta
        if (pedido.getPrioridad() == 1) {
            costo *= 0.8; // Descuento para prioridad alta
        } else if (pedido.getPrioridad() == 3) {
            costo *= 1.2; // Sobrecosto para prioridad baja
        }
        
        return costo;
    }
    
    /**
     * Calcula el riesgo de colapso logístico basado en capacidades del día
     */
    private double calcularRiesgoColapso(Map<String, Integer> productosEnRuta, 
                                        Map<Integer, List<Integer>> productosPorHora, 
                                        int totalProductos) {
        double riesgo = 0.0;
        
        // Verificar si algún aeropuerto se acerca al límite
        for (Map.Entry<String, Integer> entry : productosEnRuta.entrySet()) {
            double porcentajeUso = (double) entry.getValue() / CAPACIDAD_ALMACEN_AEROPUERTO;
            
            if (porcentajeUso > UMBRAL_COLAPSO) {
                // Colapso inminente - penalización muy alta
                riesgo += COSTO_SATURACION * Math.pow(porcentajeUso - UMBRAL_COLAPSO, 2);
            } else if (porcentajeUso > 0.7) {
                // Zona de advertencia
                riesgo += COSTO_SATURACION * 0.1 * porcentajeUso;
            }
        }
        
        // Verificar capacidad total del sistema
        double capacidadTotalDia = aeropuertoDAO.obtenerSedes().size() * VUELOS_POR_DIA * 1000; // aprox
        double porcentajeUsoTotal = (double) totalProductos / capacidadTotalDia;
        
        if (porcentajeUsoTotal > UMBRAL_COLAPSO) {
            riesgo += COSTO_SATURACION * 2.0; // Penalización crítica
        }
        
        return riesgo;
    }
    
    /**
     * Calcula el desbalance temporal en la distribución de productos por hora
     */
    private double calcularDesbalanceTemporal(Map<Integer, List<Integer>> productosPorHora) {
        if (productosPorHora.isEmpty()) return 0.0;
        
        // Calcular promedio de productos por hora
        double totalProductos = productosPorHora.values().stream()
                .mapToInt(List::size)
                .sum();
        double promedio = totalProductos / 24.0; // 24 horas del día
        
        // Calcular varianza (penalizar horas con mucha carga)
        double varianza = 0.0;
        for (int hora = 0; hora < 24; hora++) {
            int productosEnHora = productosPorHora.getOrDefault(hora, new ArrayList<>()).size();
            double diferencia = productosEnHora - promedio;
            varianza += diferencia * diferencia;
        }
        
        return varianza / 24.0; // Desbalance promedio por hora
    }
    
    /**
     * Calcula factor de costo por hora del día
     */
    private double calcularFactorHorario(int hora) {
        // Horas pico (8-18) tienen mayor costo
        if (hora >= 8 && hora <= 18) {
            return 1.3; // 30% más caro
        } else if (hora >= 22 || hora <= 6) {
            return 0.8; // 20% más barato (horas nocturnas)
        }
        return 1.0; // Costo normal
    }
    
    /**
     * Calcula el costo basado en el tiempo de vuelo estimado
     */
    private double calcularCostoTiempo(Aeropuerto origen, Aeropuerto destino, Pedido pedido) {
        double distancia = origen.calcularDistancia(destino);
        
        // Velocidad promedio de vuelo comercial: 800 km/h (ajustado para unidades GMT)
        double tiempoVuelo = distancia / 800.0;
        
        // Factor de urgencia basado en la hora del pedido
        double factorUrgencia = 1.0;
        if (pedido.getHora() >= 6 && pedido.getHora() <= 10) {
            factorUrgencia = 1.2; // Horario pico matutino
        } else if (pedido.getHora() >= 17 && pedido.getHora() <= 20) {
            factorUrgencia = 1.3; // Horario pico vespertino
        }
        
        return tiempoVuelo * COSTO_POR_HORA * factorUrgencia;
    }
    
    /**
     * Calcula el costo basado en la capacidad y utilización
     */
    private double calcularCostoCapacidad(Pedido pedido) {
        int cantidad = pedido.getCantidadProductos();
        
        // Costo base por producto
        double costoBase = cantidad * 2.0;
        
        // Descuento por volumen (economías de escala)
        if (cantidad > 200) {
            costoBase *= 0.8; // 20% descuento
        } else if (cantidad > 100) {
            costoBase *= 0.9; // 10% descuento
        }
        
        return costoBase;
    }
    
    /**
     * Calcula penalizaciones por violar restricciones
     */
    private double calcularPenalizaciones(Pedido pedido, Aeropuerto origen, Aeropuerto destino) {
        double penalizacion = 0.0;
        
        // Penalización por prioridad alta no atendida rápidamente
        if (pedido.getPrioridad() == 1) { // Alta prioridad
            if (pedido.getHora() > 15) { // Pedido tardío en el día
                penalizacion += PENALIZACION_PRIORIDAD * 0.5;
            }
        }
        
        // Penalización por rutas intercontinentales (más complejas)
        if (!origen.getContinente().equals(destino.getContinente())) {
            penalizacion += PENALIZACION_PRIORIDAD * 0.3;
        }
        
        // Penalización por pedidos muy pequeños en rutas largas (ineficiente)
        double distancia = origen.calcularDistancia(destino);
        
        if (distancia > 5000 && pedido.getCantidadProductos() < 50) {
            penalizacion += PENALIZACION_PRIORIDAD * 0.2;
        }
        
        return penalizacion;
    }
    
    /**
     * Genera una solución aleatoria inicial
     */
    public int[] generarSolucionAleatoria() {
        Random random = new Random();
        int[] solucion = new int[pedidos.size()];
        int numSedes = aeropuertoDAO.obtenerSedes().size();
        
        for (int i = 0; i < solucion.length; i++) {
            solucion[i] = random.nextInt(numSedes);
        }
        
        return solucion;
    }
    
    /**
     * Evalúa y muestra detalles de una solución
     */
    public void evaluarSolucion(int[] solucion) {
        double fitness = calcularFitness(solucion);
        List<Aeropuerto> sedes = aeropuertoDAO.obtenerSedes();
        
        System.out.println("📊 EVALUACIÓN DE SOLUCIÓN:");
        System.out.println("Fitness total: $" + String.format("%.2f", fitness));
        System.out.println("\nAsignaciones:");
        
        for (int i = 0; i < Math.min(10, pedidos.size()); i++) { // Mostrar solo primeros 10
            Pedido pedido = pedidos.get(i);
            Aeropuerto sede = sedes.get(solucion[i]);
            Aeropuerto destino = aeropuertos.get(pedido.getAeropuertoDestinoId());
            
            if (destino != null) {
                double distancia = sede.calcularDistancia(destino);
                
                System.out.printf("  %s: %s → %s (%.0f u, %d productos, P%d)%n",
                    pedido.getId(), sede.getCodigoICAO(), destino.getCodigoICAO(),
                    distancia, pedido.getCantidadProductos(), pedido.getPrioridad());
            }
        }
        
        if (pedidos.size() > 10) {
            System.out.println("  ... y " + (pedidos.size() - 10) + " pedidos más");
        }
    }
}
