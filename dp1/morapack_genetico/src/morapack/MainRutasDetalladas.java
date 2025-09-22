package morapack;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.optimizacion.FuncionObjetivoOptimizada;
import morapack.modelo.Vuelo;
import morapack.modelo.Pedido;
import morapack.modelo.Aeropuerto;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase para mostrar rutas detalladas generadas por cada archivo de pedidos
 */
public class MainRutasDetalladas {
    
    private static final String[] AEROPUERTOS_ORIGEN = {"SPIM", "EBCI", "UBBB"}; // Lima, Bruselas, Baku
    
    public static void main(String[] args) {
        System.out.println("=== SISTEMA MORAPACK - RUTAS DETALLADAS POR ARCHIVO ===\n");
        
        // Inicializar DAOs
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        
        // Leer los CSV base
        System.out.println("📂 Cargando datos CSV base...");
        CargadorDatosCSV.cargarAeropuertos(aeropuertoDAO);
        List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
        CargadorDatosCSV.cargarClientes();
        
        // Crear mapa de aeropuertos para acceso rápido
        Map<String, Aeropuerto> mapaAeropuertos = new HashMap<>();
        for (Aeropuerto aeropuerto : aeropuertoDAO.obtenerTodos()) {
            mapaAeropuertos.put(aeropuerto.getCodigoICAO(), aeropuerto);
        }
        
        System.out.println("✅ Datos base cargados: " + aeropuertoDAO.contarTotal() + " aeropuertos, " + 
                          vuelos.size() + " vuelos");
        
        // Crear archivo de reporte
        String nombreArchivo = "reporte_rutas_detalladas_" + 
                              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(nombreArchivo))) {
            // Escribir cabecera del reporte
            writer.println("=".repeat(120));
            writer.println("                    SISTEMA MORAPACK - REPORTE DE RUTAS DETALLADAS");
            writer.println("=".repeat(120));
            writer.println("Fecha de generación: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            writer.println("Datos base: " + aeropuertoDAO.contarTotal() + " aeropuertos, " + vuelos.size() + " vuelos");
            writer.println("Aeropuertos origen: SPIM (Lima), EBCI (Bruselas), UBBB (Baku)");
            writer.println("=".repeat(120));
            
            // Procesar solo los primeros 5 archivos para mostrar rutas detalladas
            for (int i = 1; i <= 5; i++) {
                mostrarRutasArchivo(i, aeropuertoDAO, vuelos, mapaAeropuertos, writer);
            }
            
            writer.println("\n" + "=".repeat(120));
            writer.println("                              FIN DEL REPORTE");
            writer.println("=".repeat(120));
            
            System.out.println("\n📄 Reporte exportado exitosamente a: " + nombreArchivo);
            
        } catch (IOException e) {
            System.err.println("❌ Error al crear el archivo de reporte: " + e.getMessage());
        }
        
        // Procesar archivos para mostrar en consola también
        for (int i = 1; i <= 5; i++) {
            mostrarRutasArchivo(i, aeropuertoDAO, vuelos, mapaAeropuertos, null);
        }
    }
    
    private static void mostrarRutasArchivo(int numeroArchivo, AeropuertoDAOImpl aeropuertoDAO, 
                                          List<Vuelo> vuelos, Map<String, Aeropuerto> mapaAeropuertos, PrintWriter writer) {
        
        String nombreArchivo = String.format("pedidos_%02d.csv", numeroArchivo);
        String separador = "\n" + "=".repeat(100);
        String titulo = "📁 ARCHIVO: " + nombreArchivo;
        String separadorLinea = "=".repeat(100);
        
        // Escribir en consola
        System.out.println(separador);
        System.out.println(titulo);
        System.out.println(separadorLinea);
        
        // Escribir en archivo si está disponible
        if (writer != null) {
            writer.println(separador);
            writer.println(titulo);
            writer.println(separadorLinea);
        }
        
        // Cargar pedidos desde archivo específico
        List<Pedido> pedidosArchivo = CargadorDatosCSV.cargarPedidosDesdeArchivo(nombreArchivo);
        
        if (pedidosArchivo.isEmpty()) {
            String mensaje = "⚠️  No se pudieron cargar pedidos desde " + nombreArchivo;
            System.out.println(mensaje);
            if (writer != null) writer.println(mensaje);
            return;
        }
        
        String infoPedidos = "📦 Pedidos cargados: " + pedidosArchivo.size();
        System.out.println(infoPedidos);
        if (writer != null) writer.println(infoPedidos);
        
        // Crear función objetivo para evaluar este conjunto de pedidos
        FuncionObjetivoOptimizada funcionObj = new FuncionObjetivoOptimizada(aeropuertoDAO, pedidosArchivo, vuelos);
        
        // Generar solución aleatoria y evaluar
        int[] solucion = funcionObj.generarSolucionAleatoria();
        double fitness = funcionObj.calcularFitness(solucion);
        int productosATiempo = funcionObj.obtenerProductosATiempo(solucion);
        int productosRetrasados = funcionObj.obtenerProductosRetrasados(solucion);
        
        String fitnessInfo = String.format("🎯 Fitness: %.0f | ✅ A tiempo: %d | ⏰ Retrasados: %d", 
                         fitness, productosATiempo, productosRetrasados);
        System.out.println(fitnessInfo);
        if (writer != null) writer.println(fitnessInfo);
        
        // Mostrar rutas generadas
        String tituloRutas = "\n🗺️  RUTAS GENERADAS:";
        String separadorTabla = "-".repeat(100);
        String cabecera = String.format("%-8s %-15s %-15s %-12s %-8s %-10s %-25s", 
                         "Pedido", "Origen", "Destino", "Productos", "Hora", "Estado", "Vuelo Asignado");
        
        System.out.println(tituloRutas);
        System.out.println(separadorTabla);
        System.out.println(cabecera);
        System.out.println(separadorTabla);
        
        if (writer != null) {
            writer.println(tituloRutas);
            writer.println(separadorTabla);
            writer.println(cabecera);
            writer.println(separadorTabla);
        }
        
        Random random = new Random(numeroArchivo * 1000); // Seed basado en número de archivo
        
        for (int i = 0; i < Math.min(pedidosArchivo.size(), 20); i++) { // Mostrar máximo 20 rutas por archivo
            Pedido pedido = pedidosArchivo.get(i);
            
            // Extraer información del pedido
            String[] partesPedido = pedido.getId().split("-");
            String destino = partesPedido.length > 3 ? partesPedido[3] : "UNKNOWN";
            String productos = partesPedido.length > 4 ? partesPedido[4] : "000";
            String hora = partesPedido.length > 1 ? partesPedido[1] + ":" + (partesPedido.length > 2 ? partesPedido[2] : "00") : "00:00";
            
            // Asignar origen de forma aleatoria pero consistente para este archivo
            String origenAsignado = AEROPUERTOS_ORIGEN[i % 3];
            
            // Buscar vuelo real que conecte origen con destino
            Vuelo vueloOptimo = buscarVueloOptimo(origenAsignado, destino, vuelos, hora);
            
            // Determinar estado basado en disponibilidad de vuelo y horarios
            String estado = determinarEstadoEntrega(origenAsignado, destino, hora, vueloOptimo);
            
            String infoVuelo = (vueloOptimo != null) ? 
                              String.format("%s→%s (%s-%s)", 
                                          vueloOptimo.getOrigen(), vueloOptimo.getDestino(),
                                          vueloOptimo.getHoraSalida(), vueloOptimo.getHoraLlegada()) :
                              "NO HAY VUELO DIRECTO";
            
            String lineaRuta = String.format("%-8s %-15s %-15s %-12s %-8s %-10s %-25s",
                             "P" + (i + 1),
                             origenAsignado,
                             destino,
                             productos,
                             hora,
                             estado,
                             infoVuelo);
            
            System.out.println(lineaRuta);
            if (writer != null) writer.println(lineaRuta);
        }
        
        // Mostrar estadísticas del archivo
        String estadisticas = String.format("📊 ESTADÍSTICAS: Total pedidos: %d | Rutas mostradas: %d", 
                         pedidosArchivo.size(), Math.min(pedidosArchivo.size(), 20));
        
        System.out.println(separadorTabla);
        System.out.println(estadisticas);
        
        if (writer != null) {
            writer.println(separadorTabla);
            writer.println(estadisticas);
        }
        
        // Mostrar distribución de orígenes
        String tituloDistribucion = "\n🌍 DISTRIBUCIÓN DE AEROPUERTOS ORIGEN:";
        System.out.println(tituloDistribucion);
        if (writer != null) writer.println(tituloDistribucion);
        
        int[] conteoOrigen = new int[3];
        for (int i = 0; i < pedidosArchivo.size(); i++) {
            conteoOrigen[i % 3]++;
        }
        for (int j = 0; j < 3; j++) {
            String lineaDistribucion = String.format("   %s: %d pedidos (%.1f%%)", 
                             AEROPUERTOS_ORIGEN[j], conteoOrigen[j], 
                             (conteoOrigen[j] * 100.0) / pedidosArchivo.size());
            System.out.println(lineaDistribucion);
            if (writer != null) writer.println(lineaDistribucion);
        }
    }
    
    /**
     * Busca el vuelo óptimo que conecte origen con destino
     */
    private static Vuelo buscarVueloOptimo(String origen, String destino, List<Vuelo> vuelos, String horaPedido) {
        Vuelo mejorVuelo = null;
        int mejorTiempo = Integer.MAX_VALUE;
        
        // Convertir hora del pedido a minutos para comparación
        int minutosDeseados = convertirHoraAMinutos(horaPedido);
        
        for (Vuelo vuelo : vuelos) {
            // Buscar vuelos que salgan del origen especificado
            if (vuelo.getOrigen().equals(origen)) {
                // Para vuelos directos al destino
                if (vuelo.getDestino().equals(destino)) {
                    int tiempoVuelo = convertirHoraAMinutos(vuelo.getHoraSalida());
                    int diferencia = Math.abs(tiempoVuelo - minutosDeseados);
                    
                    if (diferencia < mejorTiempo) {
                        mejorTiempo = diferencia;
                        mejorVuelo = vuelo;
                    }
                }
                // Si no hay vuelo directo, buscar conexiones (simplificado)
                else if (mejorVuelo == null) {
                    // Usar cualquier vuelo que salga del origen como alternativa
                    mejorVuelo = vuelo;
                }
            }
        }
        
        return mejorVuelo;
    }
    
    /**
     * Determina si la entrega será a tiempo basado en disponibilidad de vuelos y horarios
     */
    private static String determinarEstadoEntrega(String origen, String destino, String horaPedido, Vuelo vuelo) {
        if (vuelo == null) {
            return "SIN VUELO";
        }
        
        // Convertir horas a minutos para comparación
        int minutosDeseados = convertirHoraAMinutos(horaPedido);
        int minutosSalida = convertirHoraAMinutos(vuelo.getHoraSalida());
        int minutosLlegada = convertirHoraAMinutos(vuelo.getHoraLlegada());
        
        // Si el vuelo llega antes de la hora deseada, está a tiempo
        if (minutosLlegada <= minutosDeseados) {
            return "A TIEMPO";
        }
        // Si el vuelo llega hasta 2 horas después, considerarlo aceptable
        else if (minutosLlegada <= minutosDeseados + 120) {
            return "ACEPTABLE";
        }
        // Si llega más de 2 horas tarde, está retrasado
        else {
            return "RETRASADO";
        }
    }
    
    /**
     * Convierte formato de hora HH:MM a minutos desde medianoche
     */
    private static int convertirHoraAMinutos(String hora) {
        try {
            String[] partes = hora.split(":");
            int horas = Integer.parseInt(partes[0]);
            int minutos = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
            return horas * 60 + minutos;
        } catch (Exception e) {
            return 720; // 12:00 PM por defecto si hay error
        }
    }
}
