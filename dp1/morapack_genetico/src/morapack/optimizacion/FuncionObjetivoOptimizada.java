package morapack.optimizacion;

import morapack.modelo.*;
import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import java.util.*;

/**
 * Función objetivo optimizada para evaluación de entregas a tiempo vs retrasadas
 * VARIABLES PRINCIPALES:
 * - Cantidad de productos entregados a tiempo (coeficiente +1)
 * - Cantidad de productos retrasados (coeficiente -1000)
 * RESTRICCIÓN PRINCIPAL: Cdia = Σ(Cv) capacidad total diaria de vuelos
 * OBJETIVO: Maximizar entregas a tiempo y minimizar retrasos
 */
public class FuncionObjetivoOptimizada {
    
    private final AeropuertoDAOImpl aeropuertoDAO;
    private final List<Pedido> pedidos;
    private final Map<String, Aeropuerto> aeropuertos;
    private final List<Vuelo> vuelos;
    private final int capacidadTotalDiaria; // Cdia = Σ(Cv)
    
    // Constantes para evaluación de entregas
    private static final double COEFICIENTE_ENTREGA_A_TIEMPO = 1.0;    // +1 por producto a tiempo
    private static final double COEFICIENTE_PRODUCTO_RETRASADO = -1000.0; // -1000 por producto retrasado
    
    // Umbrales para determinar entregas a tiempo
    private static final int LIMITE_HORAS_ENTREGA = 24;        // 24 horas para considerar a tiempo
    private static final double UMBRAL_SATURACION = 0.95;      // 95% de Cdia = saturación crítica
    private static final double FACTOR_PENALIZACION = 1000000; // Penalización alta por exceder Cdia
    
    public FuncionObjetivoOptimizada(AeropuertoDAOImpl aeropuertoDAO, List<Pedido> pedidos, List<Vuelo> vuelos) {
        this.aeropuertoDAO = aeropuertoDAO;
        this.pedidos = pedidos;
        this.vuelos = vuelos;
        this.aeropuertos = new HashMap<>();
        
        // Cargar aeropuertos en mapa para acceso rápido
        for (Aeropuerto aeropuerto : aeropuertoDAO.obtenerTodos()) {
            aeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
        
        // Calcular Cdia = Σ(Cv) - ÚNICA RESTRICCIÓN PRINCIPAL
        this.capacidadTotalDiaria = CargadorDatosCSV.calcularCapacidadTotalDiaria(vuelos);
    }
    
    /**
     * Función objetivo principal: Evaluar entregas a tiempo vs retrasadas
     * Fitness = (productos_a_tiempo * 1) + (productos_retrasados * -1000)
     */
    public double calcularFitness(int[] solucion) {
        if (solucion.length != pedidos.size()) {
            return -Double.MAX_VALUE;
        }
        
        // ANÁLISIS POR PRODUCTO INDIVIDUAL
        ProductosAnalisis analisis = analizarProductosPorSolucion(solucion);
        
        // Verificar si excede capacidad total diaria (penalización severa)
        if (analisis.totalProductos > capacidadTotalDiaria * UMBRAL_SATURACION) {
            return -FACTOR_PENALIZACION; // Penalización máxima por exceder Cdia
        }
        
        // Calcular productos entregados a tiempo vs retrasados
        int productosATiempo = calcularProductosATiempo(analisis);
        int productosRetrasados = analisis.totalProductos - productosATiempo;
        
        // Aplicar coeficientes
        double fitnessEntregasATiempo = productosATiempo * COEFICIENTE_ENTREGA_A_TIEMPO;
        double fitnessProductosRetrasados = productosRetrasados * COEFICIENTE_PRODUCTO_RETRASADO;
        
        // FITNESS TOTAL: Maximizar entregas a tiempo, minimizar retrasos
        return fitnessEntregasATiempo + fitnessProductosRetrasados;
    }
    
    /**
     * Obtiene la cantidad de productos entregados a tiempo para una solución
     */
    public int obtenerProductosATiempo(int[] solucion) {
        if (solucion.length != pedidos.size()) {
            return 0;
        }
        ProductosAnalisis analisis = analizarProductosPorSolucion(solucion);
        return calcularProductosATiempo(analisis);
    }
    
    /**
     * Obtiene la cantidad de productos retrasados para una solución
     */
    public int obtenerProductosRetrasados(int[] solucion) {
        if (solucion.length != pedidos.size()) {
            return 0;
        }
        ProductosAnalisis analisis = analizarProductosPorSolucion(solucion);
        int productosATiempo = calcularProductosATiempo(analisis);
        return analisis.totalProductos - productosATiempo;
    }
    
    /**
     * Analiza productos por solución propuesta
     */
    private ProductosAnalisis analizarProductosPorSolucion(int[] solucion) {
        ProductosAnalisis analisis = new ProductosAnalisis();
        List<Aeropuerto> sedes = aeropuertoDAO.obtenerSedes();
        
        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            if (solucion[i] < 0 || solucion[i] >= sedes.size()) {
                analisis.productosInvalidos += pedido.getCantidadProductos();
                continue;
            }
            
            Aeropuerto sede = sedes.get(solucion[i]);
            analisis.totalProductos += pedido.getCantidadProductos();
            
            // Análisis temporal por producto
            for (int producto = 0; producto < pedido.getCantidadProductos(); producto++) {
                int horaAsignada = calcularHoraAsignacion(pedido, producto);
                analisis.productosPorHora.merge(horaAsignada, 1, Integer::sum);
                
                // Priorización por producto
                if (pedido.getPrioridad() == 1) { // Alta prioridad
                    analisis.productosAltaPrioridad++;
                }
            }
            
            analisis.productosPorSede.merge(sede.getCodigoICAO(), 
                                          pedido.getCantidadProductos(), Integer::sum);
        }
        
        return analisis;
    }
    
    /**
     * Calcula cuántos productos serán entregados a tiempo
     * Asume que productos con prioridad 1 siempre llegan a tiempo
     * y productos con menor prioridad pueden tener retrasos
     */
    private int calcularProductosATiempo(ProductosAnalisis analisis) {
        int productosATiempo = 0;
        
        // Productos de alta prioridad (P1) siempre a tiempo
        productosATiempo += analisis.productosAltaPrioridad;
        
        // Para productos de menor prioridad, calcular según distribución temporal
        int productosRestantes = analisis.totalProductos - analisis.productosAltaPrioridad;
        
        // Simular entregas basadas en distribución temporal
        int capacidadPorHora = capacidadTotalDiaria / 24; // Capacidad promedio por hora
        int productosEntregados = 0;
        
        for (int hora = 0; hora < 24 && productosEntregados < productosRestantes; hora++) {
            int productosEnHora = analisis.productosPorHora.getOrDefault(hora, 0);
            int productosQueSeEntregan = Math.min(productosEnHora, capacidadPorHora);
            
            // Solo contamos productos que llegan dentro del límite de tiempo
            if (hora <= LIMITE_HORAS_ENTREGA) {
                productosATiempo += productosQueSeEntregan;
            }
            
            productosEntregados += productosQueSeEntregan;
        }
        
        return productosATiempo;
    }
    
    /**
     * COMPONENTE PRINCIPAL: Calcular desbalance entre tasa de llegada y Cdia
     * Minimizar este valor = maximizar días hasta colapso
     */
    private double calcularDesbalanceTasaVsCapacidad(ProductosAnalisis analisis) {
        double tasaDiariaLlegada = analisis.totalProductos; // productos/día
        
        // Si excedemos Cdia = COLAPSO INMEDIATO
        if (tasaDiariaLlegada > capacidadTotalDiaria) {
            double exceso = tasaDiariaLlegada - capacidadTotalDiaria;
            return FACTOR_PENALIZACION * Math.pow(exceso / capacidadTotalDiaria, 2);
        }
        
        // Calcular porcentaje de utilización de Cdia
        double porcentajeUtilizacion = tasaDiariaLlegada / capacidadTotalDiaria;
        
        // Función que penaliza acercarse a la saturación
        // Cuanto más cerca de 1.0, más costoso (menos días hasta colapso)
        if (porcentajeUtilizacion > UMBRAL_SATURACION) {
            // Zona crítica - costo exponencial
            double factorCritico = (porcentajeUtilizacion - UMBRAL_SATURACION) / (1.0 - UMBRAL_SATURACION);
            return FACTOR_PENALIZACION * 0.1 * Math.pow(factorCritico, 3);
        } else {
            // Zona segura - costo lineal
            return porcentajeUtilizacion * 1000;
        }
    }
    
    /**
     * Calcular desbalance temporal en distribución de productos
     */
    private double calcularDesbalanceTemporal(ProductosAnalisis analisis) {
        if (analisis.productosPorHora.isEmpty()) return 0.0;
        
        double promedioPorHora = (double) analisis.totalProductos / 24.0;
        double varianza = 0.0;
        
        for (int hora = 0; hora < 24; hora++) {
            int productosEnHora = analisis.productosPorHora.getOrDefault(hora, 0);
            double diferencia = productosEnHora - promedioPorHora;
            varianza += diferencia * diferencia;
        }
        
        return varianza / 24.0;
    }
    
    /**
     * Calcular eficiencia en asignación de productos
     */
    private double calcularEficienciaAsignacion(ProductosAnalisis analisis) {
        double costo = 0.0;
        
        // Penalizar productos inválidos
        costo += analisis.productosInvalidos * 100;
        
        // Bonificar buena distribución entre sedes
        int sedesUtilizadas = analisis.productosPorSede.size();
        int sedesDisponibles = aeropuertoDAO.obtenerSedes().size();
        
        if (sedesUtilizadas > 0) {
            double factorDistribucion = (double) sedesUtilizadas / sedesDisponibles;
            costo += (1.0 - factorDistribucion) * 500; // Penalizar baja utilización de sedes
        }
        
        return costo;
    }
    
    /**
     * Calcular hora de asignación para un producto específico
     */
    private int calcularHoraAsignacion(Pedido pedido, int numeroProducto) {
        int horaBase = pedido.getHora();
        
        // Distribuir productos de pedidos grandes a lo largo del día
        if (pedido.getCantidadProductos() > 100) {
            int incremento = numeroProducto / 50; // Cada 50 productos = +1 hora
            return (horaBase + incremento) % 24;
        }
        
        return horaBase;
    }
    
    /**
     * Clase auxiliar para análisis de productos
     */
    private static class ProductosAnalisis {
        int totalProductos = 0;
        int productosInvalidos = 0;
        int productosAltaPrioridad = 0;
        Map<Integer, Integer> productosPorHora = new HashMap<>();
        Map<String, Integer> productosPorSede = new HashMap<>();
    }
    
    /**
     * Genera solución aleatoria inicial
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
     * Muestra análisis detallado de una solución
     */
    public void mostrarAnalisisSolucion(int[] solucion) {
        ProductosAnalisis analisis = analizarProductosPorSolucion(solucion);
        
        System.out.println("📊 ANÁLISIS DE SOLUCIÓN - RESTRICCIÓN Cdia:");
        System.out.printf("📈 Capacidad total diaria (Cdia): %d productos%n", capacidadTotalDiaria);
        System.out.printf("📦 Total productos procesados: %d productos%n", analisis.totalProductos);
        
        double porcentajeUso = (double) analisis.totalProductos / capacidadTotalDiaria * 100;
        System.out.printf("📊 Utilización de Cdia: %.1f%%%n", porcentajeUso);
        
        if (analisis.totalProductos > capacidadTotalDiaria) {
            System.out.println("❌ ¡COLAPSO! Excede capacidad diaria");
        } else if (porcentajeUso > 95) {
            System.out.println("⚠️  ZONA CRÍTICA - Muy cerca del colapso");
        } else if (porcentajeUso > 80) {
            System.out.println("🟡 ZONA DE ADVERTENCIA");
        } else {
            System.out.println("✅ ZONA SEGURA");
        }
        
        System.out.printf("🔥 Productos alta prioridad: %d%n", analisis.productosAltaPrioridad);
        System.out.printf("🏭 Sedes utilizadas: %d/%d%n", 
                         analisis.productosPorSede.size(), aeropuertoDAO.obtenerSedes().size());
        
        // Mostrar distribución horaria (sample)
        System.out.println("⏰ Distribución temporal (muestra):");
        for (int hora = 8; hora <= 17; hora++) {
            int productos = analisis.productosPorHora.getOrDefault(hora, 0);
            if (productos > 0) {
                System.out.printf("  %02d:00 → %d productos%n", hora, productos);
            }
        }
        
        System.out.println("🎯 OBJETIVO: Minimizar desbalance vs Cdia (maximizar días hasta colapso)");
    }
    
    public int getCapacidadTotalDiaria() {
        return capacidadTotalDiaria;
    }
}
