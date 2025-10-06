package morapack.main;

import morapack.modelo.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generador simple de rutas que crea un TXT con las rutas básicas y fitness
 */
public class GeneradorRutasSimple {
    
    // Clase interna para rutas detalladas
    private static class RutaDetallada {
        String descripcion;
        List<String> vuelosUsados;
        String tipo;
        
        RutaDetallada(String descripcion, List<String> vuelosUsados, String tipo) {
            this.descripcion = descripcion;
            this.vuelosUsados = vuelosUsados;
            this.tipo = tipo;
        }
    }
    
    private static Map<String, Aeropuerto> aeropuertos;
    private static List<Vuelo> vuelos;
    private static List<Pedido> pedidos;
    
    public static void main(String[] args) {
        System.out.println("=== GENERADOR SIMPLE DE RUTAS CON FITNESS ===");
        
        try {
            // Usar archivos por defecto o argumentos
            String archivoAeropuertos = args.length > 0 ? args[0] : "datos/aeropuertos_completo.csv";
            String archivoVuelos = args.length > 1 ? args[1] : "datos/planes_vuelo_completo.csv";
            String archivoPedidos = args.length > 2 ? args[2] : "datos/pedidos_completo.csv";
            
            // Cargar datos
            System.out.println("Cargando datos...");
            cargarAeropuertos(archivoAeropuertos);
            cargarVuelos(archivoVuelos);
            cargarPedidos(archivoPedidos);
            
            // Generar rutas simples
            System.out.println("Generando rutas y calculando fitness...");
            generarRutasYFitness();
            
            System.out.println("✅ Archivo RUTAS_Y_FITNESS.txt generado exitosamente!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void cargarAeropuertos(String archivo) throws IOException {
        aeropuertos = new HashMap<>();
        List<String> lineas = Files.readAllLines(Paths.get(archivo));
        
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).replaceAll("\\r", "").trim();
            if (linea.isEmpty()) continue;
            
            String[] datos = linea.split(",");
            if (datos.length >= 10) {
                try {
                    String codigo = datos[1].trim(); // CodigoICAO
                    String ciudad = datos[2].trim(); // Ciudad
                    String pais = datos[3].trim(); // Pais
                    int huso = Integer.parseInt(datos[5].trim()); // HusoHorario
                    int capacidad = Integer.parseInt(datos[6].trim()); // Capacidad
                    double lat = Double.parseDouble(datos[7].trim()); // Latitud
                    double lon = Double.parseDouble(datos[8].trim()); // Longitud
                    
                    Aeropuerto aeropuerto = new Aeropuerto(codigo, ciudad, pais, "", huso, capacidad, lat, lon);
                    aeropuertos.put(codigo, aeropuerto);
                } catch (NumberFormatException e) {
                    System.out.println("Error procesando línea " + i + ": " + linea);
                }
            }
        }
        System.out.println("Aeropuertos cargados: " + aeropuertos.size());
    }
    
    private static void cargarVuelos(String archivo) throws IOException {
        vuelos = new ArrayList<>();
        List<String> lineas = Files.readAllLines(Paths.get(archivo));
        
        for (int i = 1; i < lineas.size(); i++) {
            String[] datos = lineas.get(i).split(",");
            if (datos.length >= 5) {
                String origen = datos[0].trim();
                String destino = datos[1].trim();
                String horaSalida = datos[2].trim();
                String horaLlegada = datos[3].trim();
                int capacidad = Integer.parseInt(datos[4].trim());
                
                Vuelo vuelo = new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad);
                vuelos.add(vuelo);
            }
        }
        System.out.println("Vuelos cargados: " + vuelos.size());
    }
    
    private static void cargarPedidos(String archivo) throws IOException {
        pedidos = new ArrayList<>();
        List<String> lineas = Files.readAllLines(Paths.get(archivo));
        
        for (int i = 1; i < lineas.size(); i++) {
            String[] datos = lineas.get(i).split(",");
            if (datos.length >= 6) {
                String dia = datos[0].trim();
                String hora = datos[1].trim();
                String minuto = datos[2].trim();
                String destino = datos[3].trim();
                int cantidad = Integer.parseInt(datos[4].trim());
                String clienteId = datos[5].trim();
                
                // Crear ID del pedido en formato esperado
                String id = String.format("%02d-%02d-%02d-%s-%03d-%s",
                    Integer.parseInt(dia), Integer.parseInt(hora), Integer.parseInt(minuto),
                    destino, cantidad, clienteId);
                
                Pedido pedido = new Pedido(id);
                
                // Asignar origen basado en la sede más cercana al destino
                String origenAsignado = asignarSedeOptima(destino);
                pedido.setAeropuertoOrigenId(origenAsignado);
                pedido.setAeropuertoDestinoId(destino);
                pedido.setCantidadProductos(cantidad);
                pedidos.add(pedido);
            }
        }
        System.out.println("Pedidos cargados: " + pedidos.size());
    }
    
    private static void generarRutasYFitness() throws IOException {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        sb.append("=== REPORTE DE RUTAS Y FITNESS ===\n");
        sb.append("Generado: ").append(LocalDateTime.now().format(formatter)).append("\n");
        sb.append("Total pedidos: ").append(pedidos.size()).append("\n");
        sb.append("Total vuelos disponibles: ").append(vuelos.size()).append("\n");
        sb.append("Total aeropuertos: ").append(aeropuertos.size()).append("\n\n");
        
        // Estadísticas generales
        int pedidosDomesticos = 0;
        int pedidosInternacionales = 0;
        int totalCarga = 0;
        
        // Procesamiento de pedidos
        Map<String, List<String>> rutasPorPedido = new HashMap<>();
        Map<String, Integer> vuelosUsados = new HashMap<>();
        
        // Conteo de paquetes por vuelo y control de capacidad
        Map<String, Integer> paquetesPorVuelo = new HashMap<>();
        Map<String, String> tipoVuelo = new HashMap<>();
        Map<String, Integer> capacidadDisponible = new HashMap<>();
        
        // Inicializar capacidades disponibles
        for (Vuelo vuelo : vuelos) {
            String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida();
            capacidadDisponible.put(claveVuelo, vuelo.getCapacidad());
        }
        
        sb.append("=== ANÁLISIS DE PEDIDOS Y RUTAS ===\n\n");
        
        int contadorPedidos = 0;
        for (Pedido pedido : pedidos) {
            totalCarga += pedido.getCantidadProductos();
            
            // Determinar tipo de pedido
            Aeropuerto origenAero = aeropuertos.get(pedido.getAeropuertoOrigenId());
            Aeropuerto destinoAero = aeropuertos.get(pedido.getAeropuertoDestinoId());
            
            boolean esDomestico = esRutaDomestica(origenAero, destinoAero);
            if (esDomestico) {
                pedidosDomesticos++;
            } else {
                pedidosInternacionales++;
            }
            
            // Buscar y asignar la mejor ruta disponible
            RutaDetallada mejorRuta = buscarMejorRutaDisponible(pedido, paquetesPorVuelo, tipoVuelo, capacidadDisponible);
            
            List<String> rutasTexto = new ArrayList<>();
            if (mejorRuta != null) {
                rutasTexto.add(mejorRuta.descripcion);
            }
            rutasPorPedido.put(pedido.getId(), rutasTexto);
            
            // Solo agregar los primeros 20 pedidos al reporte detallado para que no sea muy largo
            if (contadorPedidos < 20) {
                sb.append("PEDIDO: ").append(pedido.getId()).append("\n");
                sb.append("  Origen: ").append(pedido.getAeropuertoOrigenId()).append(" → Destino: ").append(pedido.getAeropuertoDestinoId()).append("\n");
                sb.append("  Cantidad: ").append(pedido.getCantidadProductos()).append(" paquetes\n");
                
                if (mejorRuta == null) {
                    sb.append("  ❌ RUTA: No hay capacidad disponible en vuelos para este pedido\n");
                } else {
                    sb.append("  ✅ RUTA ASIGNADA: ").append(mejorRuta.descripcion).append("\n");
                }
                sb.append("\n");
            }
            
            contadorPedidos++;
            if (contadorPedidos % 100 == 0) {
                System.out.println("Procesados " + contadorPedidos + " pedidos...");
            }
        }
        
        if (contadorPedidos > 20) {
            sb.append("... (").append(contadorPedidos - 20).append(" pedidos adicionales procesados)\n\n");
        }
        
        // Calcular fitness
        sb.append("=== CÁLCULO DE FITNESS ===\n\n");
        
        double fitnessCobertura = calcularFitnessCobertura(rutasPorPedido);
        double fitnessEficiencia = calcularFitnessEficiencia(vuelosUsados);
        double fitnessDistribucion = calcularFitnessDistribucion(pedidosDomesticos, pedidosInternacionales);
        double fitnessCapacidad = calcularFitnessCapacidad(totalCarga);
        
        double fitnessTotal = (fitnessCobertura * 0.4) + (fitnessEficiencia * 0.3) + 
                            (fitnessDistribucion * 0.2) + (fitnessCapacidad * 0.1);
        
        sb.append("FITNESS COBERTURA (40%): ").append(String.format("%.2f", fitnessCobertura)).append("\n");
        sb.append("FITNESS EFICIENCIA (30%): ").append(String.format("%.2f", fitnessEficiencia)).append("\n");
        sb.append("FITNESS DISTRIBUCIÓN (20%): ").append(String.format("%.2f", fitnessDistribucion)).append("\n");
        sb.append("FITNESS CAPACIDAD (10%): ").append(String.format("%.2f", fitnessCapacidad)).append("\n");
        sb.append("---\n");
        sb.append("🎯 FITNESS TOTAL: ").append(String.format("%.2f", fitnessTotal)).append(" 🎯\n\n");
        
        // Estadísticas finales
        sb.append("=== ESTADÍSTICAS GENERALES ===\n");
        sb.append("Pedidos domésticos: ").append(pedidosDomesticos).append("\n");
        sb.append("Pedidos internacionales: ").append(pedidosInternacionales).append("\n");
        sb.append("Total carga: ").append(totalCarga).append(" paquetes\n");
        sb.append("Aeropuertos disponibles: ").append(aeropuertos.size()).append("\n");
        sb.append("Vuelos disponibles: ").append(vuelos.size()).append("\n");
        
        int pedidosCubiertos = 0;
        for (List<String> rutas : rutasPorPedido.values()) {
            if (!rutas.isEmpty()) pedidosCubiertos++;
        }
        sb.append("Pedidos con rutas encontradas: ").append(pedidosCubiertos).append(" de ").append(pedidos.size()).append("\n");
        sb.append("Cobertura de rutas: ").append(String.format("%.1f%%", 
                (double) pedidosCubiertos / pedidos.size() * 100)).append("\n\n");
        
        // Reporte detallado de vuelos utilizados
        sb.append("=== REPORTE DE VUELOS UTILIZADOS ===\n");
        int vuelosContinentales = 0;
        int vuelosIntercontinentales = 0;
        int totalPaquetesTransportados = 0;
        
        for (Map.Entry<String, Integer> entry : paquetesPorVuelo.entrySet()) {
            String claveVuelo = entry.getKey();
            int paquetes = entry.getValue();
            String tipo = tipoVuelo.get(claveVuelo);
            int capacidadRestante = capacidadDisponible.get(claveVuelo);
            
            // Encontrar la capacidad original del vuelo
            int capacidadOriginal = capacidadRestante + paquetes;
            double porcentajeUso = (double) paquetes / capacidadOriginal * 100;
            
            totalPaquetesTransportados += paquetes;
            
            if ("CONTINENTAL".equals(tipo)) {
                vuelosContinentales++;
            } else if ("INTERCONTINENTAL".equals(tipo)) {
                vuelosIntercontinentales++;
            }
            
            sb.append("Vuelo: ").append(claveVuelo)
              .append(" | Tipo: ").append(tipo)
              .append(" | Carga: ").append(paquetes).append("/").append(capacidadOriginal)
              .append(" (").append(String.format("%.1f%%", porcentajeUso)).append(")")
              .append(" | Disponible: ").append(capacidadRestante).append("\n");
        }
        
        sb.append("\n=== RESUMEN DE VUELOS ===\n");
        sb.append("Vuelos continentales utilizados: ").append(vuelosContinentales).append("\n");
        sb.append("Vuelos intercontinentales utilizados: ").append(vuelosIntercontinentales).append("\n");
        sb.append("Total vuelos utilizados: ").append(paquetesPorVuelo.size()).append(" de ").append(vuelos.size()).append("\n");
        sb.append("Total paquetes transportados: ").append(totalPaquetesTransportados).append("\n");
        
        // Guardar archivo
        Files.write(Paths.get("RUTAS_Y_FITNESS.txt"), sb.toString().getBytes());
        
        // También mostrar resumen en consola
        System.out.println("\n=== RESUMEN DE FITNESS ===");
        System.out.println("🎯 FITNESS TOTAL: " + String.format("%.2f", fitnessTotal) + " 🎯");
        System.out.println("Cobertura: " + String.format("%.2f", fitnessCobertura) + "%");
        System.out.println("Eficiencia: " + String.format("%.2f", fitnessEficiencia));
        System.out.println("Distribución: " + String.format("%.2f", fitnessDistribucion));
        System.out.println("Capacidad: " + String.format("%.2f", fitnessCapacidad));
        System.out.println("Pedidos procesados: " + contadorPedidos);
        System.out.println("Pedidos con rutas: " + pedidosCubiertos + " (" + String.format("%.1f%%", (double) pedidosCubiertos / pedidos.size() * 100) + ")");
    }
    
    private static RutaDetallada buscarMejorRutaDisponible(Pedido pedido, 
                                                          Map<String, Integer> paquetesPorVuelo, 
                                                          Map<String, String> tipoVuelo,
                                                          Map<String, Integer> capacidadDisponible) {
        
        // Primero intentar vuelos directos (tienen prioridad)
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getOrigen().equals(pedido.getAeropuertoOrigenId()) && 
                vuelo.getDestino().equals(pedido.getAeropuertoDestinoId())) {
                
                String claveVuelo = vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getHoraSalida();
                
                // Verificar si hay capacidad suficiente
                if (capacidadDisponible.get(claveVuelo) >= pedido.getCantidadProductos()) {
                    
                    // Asignar el pedido a este vuelo
                    paquetesPorVuelo.put(claveVuelo, 
                        paquetesPorVuelo.getOrDefault(claveVuelo, 0) + pedido.getCantidadProductos());
                    capacidadDisponible.put(claveVuelo, 
                        capacidadDisponible.get(claveVuelo) - pedido.getCantidadProductos());
                    
                    String tipo = determinarTipoVuelo(vuelo.getOrigen(), vuelo.getDestino());
                    tipoVuelo.put(claveVuelo, tipo);
                    
                    String descripcion = String.format("%s → %s (%s DIRECTO, %s-%s, Carga: %d/%d paq)",
                        vuelo.getOrigen(), vuelo.getDestino(), tipo,
                        vuelo.getHoraSalida(), vuelo.getHoraLlegada(), 
                        paquetesPorVuelo.get(claveVuelo), vuelo.getCapacidad());
                    
                    return new RutaDetallada(descripcion, Arrays.asList(claveVuelo), tipo);
                }
            }
        }
        
        // Si no hay vuelos directos disponibles, buscar con 1 escala
        for (Vuelo vuelo1 : vuelos) {
            if (vuelo1.getOrigen().equals(pedido.getAeropuertoOrigenId())) {
                for (Vuelo vuelo2 : vuelos) {
                    if (vuelo2.getOrigen().equals(vuelo1.getDestino()) && 
                        vuelo2.getDestino().equals(pedido.getAeropuertoDestinoId()) &&
                        !vuelo1.equals(vuelo2) &&
                        !vuelo1.getDestino().equals(pedido.getAeropuertoOrigenId()) && // No regresar al origen
                        !vuelo2.getDestino().equals(vuelo1.getOrigen())) { // No hacer ida y vuelta
                        
                        String claveVuelo1 = vuelo1.getOrigen() + "-" + vuelo1.getDestino() + "-" + vuelo1.getHoraSalida();
                        String claveVuelo2 = vuelo2.getOrigen() + "-" + vuelo2.getDestino() + "-" + vuelo2.getHoraSalida();
                        
                        // Verificar si ambos vuelos tienen capacidad suficiente
                        if (capacidadDisponible.get(claveVuelo1) >= pedido.getCantidadProductos() &&
                            capacidadDisponible.get(claveVuelo2) >= pedido.getCantidadProductos()) {
                            
                            // Asignar el pedido a ambos vuelos
                            paquetesPorVuelo.put(claveVuelo1, 
                                paquetesPorVuelo.getOrDefault(claveVuelo1, 0) + pedido.getCantidadProductos());
                            paquetesPorVuelo.put(claveVuelo2, 
                                paquetesPorVuelo.getOrDefault(claveVuelo2, 0) + pedido.getCantidadProductos());
                            capacidadDisponible.put(claveVuelo1, 
                                capacidadDisponible.get(claveVuelo1) - pedido.getCantidadProductos());
                            capacidadDisponible.put(claveVuelo2, 
                                capacidadDisponible.get(claveVuelo2) - pedido.getCantidadProductos());
                            
                            String tipo1 = determinarTipoVuelo(vuelo1.getOrigen(), vuelo1.getDestino());
                            String tipo2 = determinarTipoVuelo(vuelo2.getOrigen(), vuelo2.getDestino());
                            String tipoRuta = (tipo1.equals("INTERCONTINENTAL") || tipo2.equals("INTERCONTINENTAL")) 
                                ? "INTERCONTINENTAL" : "CONTINENTAL";
                            
                            tipoVuelo.put(claveVuelo1, tipo1);
                            tipoVuelo.put(claveVuelo2, tipo2);
                            
                            String descripcion = String.format("%s → %s → %s (ESCALA %s: V1:%s %s-%s[%d/%d], V2:%s %s-%s[%d/%d])",
                                vuelo1.getOrigen(), vuelo1.getDestino(), vuelo2.getDestino(), tipoRuta,
                                tipo1, vuelo1.getHoraSalida(), vuelo1.getHoraLlegada(), 
                                paquetesPorVuelo.get(claveVuelo1), vuelo1.getCapacidad(),
                                tipo2, vuelo2.getHoraSalida(), vuelo2.getHoraLlegada(),
                                paquetesPorVuelo.get(claveVuelo2), vuelo2.getCapacidad());
                            
                            return new RutaDetallada(descripcion, Arrays.asList(claveVuelo1, claveVuelo2), tipoRuta);
                        }
                    }
                }
            }
        }
        
        // No se encontró ruta disponible con capacidad suficiente
        return null;
    }
    

    
    private static String asignarSedeOptima(String destino) {
        // Las tres sedes disponibles: Lima, Bruselas, Baku
        String[] sedes = {"SPIM", "EBCI", "UBBB"}; // Lima, Bruselas, Baku
        
        Aeropuerto aeropuertoDestino = aeropuertos.get(destino);
        if (aeropuertoDestino == null) return "SPIM"; // Por defecto Lima
        
        String mejorSede = "SPIM";
        double menorDistancia = Double.MAX_VALUE;
        
        // Encontrar la sede más cercana al destino
        for (String sede : sedes) {
            Aeropuerto aeropuertoSede = aeropuertos.get(sede);
            if (aeropuertoSede != null) {
                double distancia = calcularDistanciaAproximada(aeropuertoSede, aeropuertoDestino);
                if (distancia < menorDistancia) {
                    menorDistancia = distancia;
                    mejorSede = sede;
                }
            }
        }
        
        return mejorSede;
    }
    
    private static double calcularDistanciaAproximada(Aeropuerto origen, Aeropuerto destino) {
        double difLat = origen.getLatitud() - destino.getLatitud();
        double difLon = origen.getLongitud() - destino.getLongitud();
        return Math.sqrt(difLat * difLat + difLon * difLon);
    }
    
    private static String determinarTipoVuelo(String origenCodigo, String destinoCodigo) {
        Aeropuerto origen = aeropuertos.get(origenCodigo);
        Aeropuerto destino = aeropuertos.get(destinoCodigo);
        
        if (origen == null || destino == null) return "DESCONOCIDO";
        
        // Calcular distancia aproximada
        double difLat = Math.abs(origen.getLatitud() - destino.getLatitud());
        double difLon = Math.abs(origen.getLongitud() - destino.getLongitud());
        
        // Si la diferencia es muy grande, es intercontinental
        if (difLat > 30 || difLon > 60) {
            return "INTERCONTINENTAL";
        } else {
            return "CONTINENTAL";
        }
    }
    
    private static boolean esRutaDomestica(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null) return false;
        
        // Simplificación: considerar doméstico si están en el mismo continente aproximado
        double difLat = Math.abs(origen.getLatitud() - destino.getLatitud());
        double difLon = Math.abs(origen.getLongitud() - destino.getLongitud());
        
        return difLat < 30 && difLon < 50; // Aproximación continental
    }
    
    private static double calcularFitnessCobertura(Map<String, List<String>> rutasPorPedido) {
        int pedidosCubiertos = 0;
        for (List<String> rutas : rutasPorPedido.values()) {
            if (!rutas.isEmpty()) pedidosCubiertos++;
        }
        return (double) pedidosCubiertos / pedidos.size() * 100.0;
    }
    
    private static double calcularFitnessEficiencia(Map<String, Integer> vuelosUsados) {
        if (vuelosUsados.isEmpty()) return 25.0; // Valor base si no hay vuelos usados
        
        double utilizacion = (double) vuelosUsados.size() / vuelos.size();
        return utilizacion * 100.0;
    }
    
    private static double calcularFitnessDistribucion(int domesticos, int internacionales) {
        int total = domesticos + internacionales;
        if (total == 0) return 0.0;
        
        double balanceIdeal = 0.3; // 30% domésticos ideal (la mayoría serán internacionales)
        double ratioActual = (double) domesticos / total;
        double diferencia = Math.abs(ratioActual - balanceIdeal);
        
        return Math.max(0.0, 100.0 - (diferencia * 200.0));
    }
    
    private static double calcularFitnessCapacidad(int totalCarga) {
        // Calcular capacidad total disponible
        int capacidadTotal = vuelos.stream().mapToInt(Vuelo::getCapacidad).sum();
        
        if (capacidadTotal == 0) return 0.0;
        
        double utilizacionCapacidad = (double) totalCarga / capacidadTotal;
        
        return Math.min(utilizacionCapacidad * 100.0, 100.0);
    }
}
