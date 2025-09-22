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
public class FuncionObjetivoNuevo {
    
    private final AeropuertoDAOImpl aeropuertoDAO;
    private final List<Pedido> pedidos;
    private final Map<String, Aeropuerto> aeropuertos;
    
    // CAPACIDADES MÁXIMAS POR DÍA DE OPERACIÓN
    private static final long CAPACIDAD_ALMACEN_AEROPUERTO = 100_000_000L; // 100 millones de espacios
    private static final int VUELOS_POR_DIA = 24; // Asumiendo 1 vuelo por hora
    
    // Pesos ajustados para evitar colapso logístico
    private static final double PESO_CAPACIDAD_DIA = 0.5;     // 50% - Crítico para evitar colapso
    private static final double PESO_DISTRIBUCION_TEMPORAL = 0.3; // 30% - Distribuir carga en el día
    private static final double PESO_EFICIENCIA_RUTA = 0.15;   // 15% - Optimizar rutas
    private static final double PESO_PENALIZACION = 0.05;     // 5% - Penalizaciones menores
    
    // Constantes ajustadas para análisis por producto
    private static final double COSTO_POR_PRODUCTO_KM = 0.1;  // $0.1 por producto-kilómetro
    private static final double COSTO_SATURACION = 10000.0;   // Alto costo cuando se acerca capacidad máxima
    private static final double UMBRAL_COLAPSO = 0.85;        // 85% de capacidad = zona crítica
    
    public FuncionObjetivoNuevo(AeropuertoDAOImpl aeropuertoDAO, List<Pedido> pedidos) {
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
     * Muestra detalles de una solución para análisis
     */
    public void mostrarDetallesSolucion(int[] solucion) {
        List<Aeropuerto> sedes = aeropuertoDAO.obtenerSedes();
        
        System.out.println("📊 ANÁLISIS DETALLADO DE SOLUCIÓN:");
        System.out.println("Asignaciones por PRODUCTO (muestra primeros 10 pedidos):");
        
        int totalProductosMostrados = 0;
        for (int i = 0; i < Math.min(pedidos.size(), 10); i++) {
            Pedido pedido = pedidos.get(i);
            Aeropuerto sede = sedes.get(solucion[i]);
            Aeropuerto destino = aeropuertos.get(pedido.getAeropuertoDestinoId());
            
            if (destino != null) {
                double distancia = sede.calcularDistancia(destino);
                int cantidadProductos = pedido.getCantidadProductos();
                totalProductosMostrados += cantidadProductos;
                
                System.out.printf("  %s: %s → %s (%d productos, %.0f u, P%d, %02d:%02d)%n",
                    pedido.getId(), sede.getCodigoICAO(), destino.getCodigoICAO(),
                    cantidadProductos, distancia, pedido.getPrioridad(), 
                    pedido.getHora(), pedido.getMinuto());
            }
        }
        
        if (pedidos.size() > 10) {
            System.out.printf("  ... y %d pedidos más%n", pedidos.size() - 10);
        }
        
        // Mostrar estadísticas de capacidad
        Map<String, Integer> productosEnRuta = new HashMap<>();
        int totalProductos = 0;
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            if (solucion[i] < sedes.size()) {
                Aeropuerto sede = sedes.get(solucion[i]);
                int cantidad = pedido.getCantidadProductos();
                productosEnRuta.merge(sede.getCodigoICAO(), cantidad, Integer::sum);
                totalProductos += cantidad;
            }
        }
        
        System.out.printf("📦 Total productos procesados: %d%n", totalProductos);
        System.out.printf("🏭 Uso máximo por aeropuerto: %.1f%% de capacidad%n", 
            productosEnRuta.values().stream().mapToInt(Integer::intValue).max().orElse(0) 
            * 100.0 / CAPACIDAD_ALMACEN_AEROPUERTO);
        
        System.out.println("🎯 Función objetivo optimizada para EVITAR COLAPSO LOGÍSTICO");
    }
}
