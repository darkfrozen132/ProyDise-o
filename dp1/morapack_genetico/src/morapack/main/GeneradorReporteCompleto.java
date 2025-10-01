package morapack.main;

import morapack.modelo.*;
import morapack.datos.*;
import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

/**
 * Generador de Reporte Completo de Rutas para TODOS los Pedidos
 */
public class GeneradorReporteCompleto {
    
    private static class RutaPedido {
        String pedidoId;
        String destino;
        int cantidadProductos;
        String tipoRuta;
        String rutaCompleta;
        String vuelo;
        String sede;
        boolean exitoso;
        List<VueloDividido> vuelosDetallados; // Para pedidos divididos
        
        RutaPedido(String pedidoId, String destino, int cantidadProductos) {
            this.pedidoId = pedidoId;
            this.destino = destino;
            this.cantidadProductos = cantidadProductos;
            this.exitoso = false;
            this.vuelosDetallados = new ArrayList<>();
        }
    }
    
    public static void main(String[] args) {
        
        System.out.println("============ GENERADOR REPORTE COMPLETO DE RUTAS ============");
        System.out.println("Procesando TODOS los 211 pedidos de pedidoUltrafinal.txt");
        System.out.println("=============================================================");
        
        try {
            // Definir aeropuertos válidos
            Set<String> aeropuertosValidos = crearAeropuertosValidos();
            
            // Cargar pedidos
            System.out.println("\\nCargando pedidos desde pedidoUltrafinal.txt...");
            List<Pedido> pedidos = CargadorPedidosUltrafinal.cargarPedidos("datos/pedidoUltrafinal.txt", aeropuertosValidos);
            
            System.out.printf("Total pedidos cargados: %d\\n", pedidos.size());
            
            // Procesar todos los pedidos
            List<RutaPedido> rutasPedidos = procesarTodosLosPedidos(pedidos);
            
            // Generar únicamente los 4 reportes solicitados
            generarReporteGeneral(rutasPedidos);
            generarReporteRutasPorPedidos(rutasPedidos);
            generarReportePedidosPorVuelos(rutasPedidos);
            generarReportePedidosDivididos(rutasPedidos);
            
            System.out.println("\\n✅ Reportes generados exitosamente:");
            System.out.println("   - REPORTE_GENERAL.txt");
            System.out.println("   - REPORTE_RUTAS_POR_PEDIDOS.txt");
            System.out.println("   - REPORTE_PEDIDOS_POR_VUELOS.txt");
            System.out.println("   - REPORTE_PEDIDOS_DIVIDIDOS.txt");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<RutaPedido> procesarTodosLosPedidos(List<Pedido> pedidos) {
        System.out.println("\\n============= PROCESANDO TODOS LOS PEDIDOS =============");
        
        List<RutaPedido> rutasPedidos = new ArrayList<>();
        int procesados = 0;
        
        for (Pedido pedido : pedidos) {
            procesados++;
            
            RutaPedido ruta = new RutaPedido(pedido.getId(), 
                                           pedido.getAeropuertoDestinoId(), 
                                           pedido.getCantidadProductos());
            
            // Asignar sede según destino (lógica geográfica)
            ruta.sede = asignarSedeSegunDestino(pedido.getAeropuertoDestinoId());
            
            // Planificación inteligente con validación de capacidad Y tiempo de tránsito
            if (existeVueloDirecto(ruta.sede, pedido.getAeropuertoDestinoId()) && 
                tieneCapacidadSuficiente(ruta.sede, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos()) &&
                validarTiempoTransitoDirecto(ruta.sede, pedido.getAeropuertoDestinoId(), pedido.getDia(), pedido.getHora())) {
                // Vuelo directo con capacidad suficiente y tiempo válido
                ruta.exitoso = true;
                ruta.tipoRuta = "DIRECTA";
                ruta.rutaCompleta = ruta.sede + " → " + pedido.getAeropuertoDestinoId();
                ruta.vuelo = ruta.sede + "-" + pedido.getAeropuertoDestinoId();
            } else if (existeVueloDirecto(ruta.sede, pedido.getAeropuertoDestinoId())) {
                // Existe ruta directa pero sin capacidad suficiente - intentar división
                int capacidadMaxima = obtenerCapacidadMaxima(ruta.sede, pedido.getAeropuertoDestinoId());
                
                if (capacidadMaxima > 0) {
                    // Calcular cuántos vuelos necesita
                    int vuelosNecesarios = (int) Math.ceil((double) pedido.getCantidadProductos() / capacidadMaxima);
                    
                    // Verificar disponibilidad de vuelos desde el día del pedido CON INFORMACIÓN DETALLADA
                    List<VueloDividido> vuelosDetallados = planificarDivisionPedidoDetallada(ruta.sede, pedido.getAeropuertoDestinoId(), 
                                                                                           pedido.getCantidadProductos(), capacidadMaxima, 
                                                                                           pedido.getDia(), pedido.getHora());
                    
                    // Verificar si se pudo asignar toda la cantidad solicitada
                    int cantidadAsignada = 0;
                    for (VueloDividido vuelo : vuelosDetallados) {
                        String[] partes = vuelo.identificador.split("-");
                        if (partes.length >= 3) {
                            cantidadAsignada += Integer.parseInt(partes[2]);
                        }
                    }
                    
                    if (!vuelosDetallados.isEmpty() && cantidadAsignada >= pedido.getCantidadProductos()) {
                        // División exitosa - se asignó toda la cantidad
                        ruta.exitoso = true;
                        ruta.tipoRuta = "DIVIDIDO";
                        ruta.vuelosDetallados = vuelosDetallados;
                        
                        // Crear lista simple para compatibilidad
                        List<String> identificadoresVuelos = new ArrayList<>();
                        for (VueloDividido vuelo : vuelosDetallados) {
                            identificadoresVuelos.add(vuelo.identificador);
                        }
                        
                        // Información compacta para el reporte de rutas por pedidos
                        ruta.rutaCompleta = String.format("Dividido en %d vuelos: %s", 
                                                         vuelosDetallados.size(), String.join(", ", identificadoresVuelos));
                        ruta.vuelo = String.join("+", identificadoresVuelos);
                    } else {
                        // División falló - no se pudo asignar toda la cantidad
                        ruta.exitoso = false;
                        ruta.tipoRuta = "FALLO";
                        if (cantidadAsignada > 0) {
                            ruta.rutaCompleta = String.format("Capacidad insuficiente: solo %,d de %,d productos (faltan %,d)", 
                                                             cantidadAsignada, pedido.getCantidadProductos(), 
                                                             pedido.getCantidadProductos() - cantidadAsignada);
                        } else {
                            ruta.rutaCompleta = String.format("No hay suficientes vuelos disponibles (necesita: %d vuelos de %d)", 
                                                             vuelosNecesarios, capacidadMaxima);
                        }
                        ruta.vuelo = "N/A";
                    }
                } else {
                    ruta.exitoso = false;
                    ruta.tipoRuta = "FALLO";
                    ruta.rutaCompleta = "No existe conexión directa";
                    ruta.vuelo = "N/A";
                }
            } else {
                // Si no hay vuelo directo, buscar ruta con escalas
                String escala = encontrarEscalaOptima(ruta.sede, pedido.getAeropuertoDestinoId());
                if (escala != null && 
                    tieneCapacidadSuficiente(ruta.sede, escala, pedido.getCantidadProductos()) &&
                    tieneCapacidadSuficiente(escala, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos()) &&
                    validarTiempoTransitoEscala(ruta.sede, escala, pedido.getAeropuertoDestinoId(), pedido.getDia(), pedido.getHora())) {
                    ruta.exitoso = true;
                    ruta.tipoRuta = "CON ESCALAS";
                    ruta.rutaCompleta = ruta.sede + " → " + escala + " → " + pedido.getAeropuertoDestinoId();
                    ruta.vuelo = ruta.sede + "-" + escala + "-" + pedido.getAeropuertoDestinoId();
                } else {
                    // No hay forma de transportar con la capacidad disponible O excede tiempo de tránsito
                    ruta.exitoso = false;
                    ruta.tipoRuta = "FALLO";
                    String motivo = (escala == null) ? "Sin ruta disponible" : 
                                  (!tieneCapacidadSuficiente(ruta.sede, escala, pedido.getCantidadProductos()) ||
                                   !tieneCapacidadSuficiente(escala, pedido.getAeropuertoDestinoId(), pedido.getCantidadProductos())) ?
                                  "Sin capacidad suficiente" : "Excede tiempo máximo de tránsito";
                    ruta.rutaCompleta = motivo;
                    ruta.vuelo = "N/A";
                }
            }
            
            rutasPedidos.add(ruta);
            
            // Mostrar progreso cada 25 pedidos
            if (procesados % 25 == 0) {
                System.out.printf("   Procesados: %d/%d pedidos (%.1f%%)\\n", 
                    procesados, pedidos.size(), (procesados * 100.0 / pedidos.size()));
            }
        }
        
        System.out.printf("\\n✅ Procesamiento completado: %d pedidos procesados\\n", procesados);
        return rutasPedidos;
    }
    
    /**
     * REPORTE 1: General con estadísticas principales
     */
    private static void generarReporteGeneral(List<RutaPedido> rutasPedidos) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_GENERAL.txt"))) {
            
            writer.println("=========================================================================");
            writer.println("                     REPORTE GENERAL - SISTEMA MORAPACK");
            writer.println("=========================================================================");
            writer.println("Archivo fuente: pedidoUltrafinal.txt");
            writer.println("Fecha generación: " + new java.util.Date());
            writer.println("Total pedidos procesados: " + rutasPedidos.size());
            writer.println("⏰ NOTA: Horarios en tiempo local de cada aeropuerto (no UTC)");
            writer.println("=========================================================================");
            writer.println();
            
            // Estadísticas generales
            long exitosos = rutasPedidos.stream().mapToLong(r -> r.exitoso ? 1 : 0).sum();
            long fallidos = rutasPedidos.size() - exitosos;
            long directas = rutasPedidos.stream().mapToLong(r -> "DIRECTA".equals(r.tipoRuta) ? 1 : 0).sum();
            long conEscalas = rutasPedidos.stream().mapToLong(r -> "CON ESCALAS".equals(r.tipoRuta) ? 1 : 0).sum();
            long divididos = rutasPedidos.stream().mapToLong(r -> "DIVIDIDO".equals(r.tipoRuta) ? 1 : 0).sum();
            
            writer.println("📊 ESTADÍSTICAS GENERALES:");
            writer.println("═══════════════════════════");
            writer.printf("✅ Total pedidos exitosos: %d (%.1f%%)%n", exitosos, (exitosos * 100.0 / rutasPedidos.size()));
            writer.printf("❌ Total pedidos fallidos: %d (%.1f%%)%n", fallidos, (fallidos * 100.0 / rutasPedidos.size()));
            writer.printf("🛫 Rutas directas: %d (%.1f%% del total exitoso)%n", directas, exitosos > 0 ? (directas * 100.0 / exitosos) : 0);
            writer.printf("🔄 Rutas con escalas: %d (%.1f%% del total exitoso)%n", conEscalas, exitosos > 0 ? (conEscalas * 100.0 / exitosos) : 0);
            writer.printf("📦 Pedidos divididos: %d (%.1f%% del total exitoso)%n", divididos, exitosos > 0 ? (divididos * 100.0 / exitosos) : 0);
            writer.println();
            
            // Resumen por productos
            int totalProductos = rutasPedidos.stream().mapToInt(r -> r.cantidadProductos).sum();
            double promProductos = rutasPedidos.size() > 0 ? (double) totalProductos / rutasPedidos.size() : 0;
            
            writer.println("📦 ANÁLISIS DE PRODUCTOS:");
            writer.println("═════════════════════════");
            writer.printf("Total productos procesados: %,d%n", totalProductos);
            writer.printf("Promedio productos por pedido: %.1f%n", promProductos);
            writer.println();
            
            // Eficiencia del sistema
            double eficiencia = rutasPedidos.size() > 0 ? (exitosos * 100.0 / rutasPedidos.size()) : 0;
            writer.println("🎯 EFICIENCIA DEL SISTEMA:");
            writer.println("═══════════════════════════");
            writer.printf("Tasa de éxito global: %.1f%%%n", eficiencia);
            
            if (eficiencia >= 95) {
                writer.println("🏆 EXCELENTE: Sistema altamente eficiente");
            } else if (eficiencia >= 85) {
                writer.println("👍 BUENO: Sistema con buen rendimiento");
            } else if (eficiencia >= 75) {
                writer.println("⚠️  REGULAR: Sistema necesita mejoras");
            } else {
                writer.println("❌ CRÍTICO: Sistema requiere revisión urgente");
            }
            
            writer.println();
            writer.println("=========================================================================");
            
        } catch (IOException e) {
            System.err.println("Error generando reporte general: " + e.getMessage());
        }
    }
    
    /**
     * REPORTE 2: Rutas detalladas por pedidos con horarios UTC+0
     */
    private static void generarReporteRutasPorPedidos(List<RutaPedido> rutasPedidos) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_RUTAS_POR_PEDIDOS.txt"))) {
            
            writer.println("=========================================================================");
            writer.println("                    REPORTE DETALLADO: RUTAS POR PEDIDOS");
            writer.println("=========================================================================");
            writer.println("⏰ HORARIOS UTC+0: Salida y llegada con duración real de vuelos");
            writer.println("✈️ DURACIÓN: Continental 12h, Intercontinental 24h (+2h escalas, +4h divididos)");
            writer.println("📝 VALIDACIÓN: Continental ≤2 días, Intercontinental ≤3 días desde pedido");
            writer.println("🌍 CONTINENTES: S=Sudamérica, E/L=Europa, O/U/V=Asia");
            writer.println();
            
            // Encabezado de tabla mejorado
            writer.printf("%-25s | %-8s | %-4s | %-10s | %-18s | %-18s | %-30s%n", 
                         "ID PEDIDO", "DESTINO", "CANT", "TIPO RUTA", "SALIDA UTC+0", "LLEGADA UTC+0", "RUTA COMPLETA");
            writer.println("─".repeat(140));
            
            // Listar todos los pedidos con sus rutas y horarios UTC+0
            for (RutaPedido ruta : rutasPedidos) {
                String estado = ruta.exitoso ? "✅" : "❌";
                String rutaInfo;
                String salidaUTC = "";
                String llegadaUTC = "";
                
                // Calcular horarios UTC+0 basado en el pedido
                if (ruta.exitoso) {
                    try {
                        // Extraer información del pedido ID (formato: día-hora-minuto-destino-cantidad-etc)
                        String[] partesPedido = ruta.pedidoId.split("-");
                        if (partesPedido.length >= 3) {
                            int dia = Integer.parseInt(partesPedido[0]);
                            int hora = Integer.parseInt(partesPedido[1]);
                            int minuto = Integer.parseInt(partesPedido[2]);
                            
                            // Determinar origen y destino para el cálculo UTC
                            String origen = ruta.sede;
                            String destino = ruta.destino;
                            
                            // Convertir hora de salida a UTC+0
                            int offsetOrigenHoras = obtenerOffsetUTC(origen);
                            int horaSalidaUTC = hora - offsetOrigenHoras;
                            int diaSalidaUTC = dia;
                            
                            // Ajustar día si la hora UTC se sale del rango 0-23
                            if (horaSalidaUTC < 0) {
                                horaSalidaUTC += 24;
                                diaSalidaUTC--;
                            } else if (horaSalidaUTC >= 24) {
                                horaSalidaUTC -= 24;
                                diaSalidaUTC++;
                            }
                            
                            salidaUTC = String.format("D%d %02d:%02d UTC", diaSalidaUTC, horaSalidaUTC, minuto);
                            
                            // Calcular llegada estimada basada en duración real de vuelos
                            int offsetDestinoHoras = obtenerOffsetUTC(destino);
                            int horaLlegadaEstimada;
                            int diaLlegadaEstimada;
                            
                            if ("DIVIDIDO".equals(ruta.tipoRuta)) {
                                // Para divididos, agregar tiempo adicional por múltiples vuelos
                                boolean esIntercontinental = esRutaIntercontinental(origen, destino);
                                int duracionHoras = (esIntercontinental ? 24 : 12) + 4; // +4h por división
                                
                                horaLlegadaEstimada = hora + duracionHoras;
                                diaLlegadaEstimada = dia;
                                
                                // Ajustar día si las horas exceden 24
                                while (horaLlegadaEstimada >= 24) {
                                    horaLlegadaEstimada -= 24;
                                    diaLlegadaEstimada++;
                                }
                            } else if (ruta.rutaCompleta.contains("→") && !ruta.rutaCompleta.contains("ESCALA")) {
                                // Vuelo directo - usar duración real
                                boolean esIntercontinental = esRutaIntercontinental(origen, destino);
                                int duracionHoras = esIntercontinental ? 24 : 12;
                                
                                horaLlegadaEstimada = hora + duracionHoras;
                                diaLlegadaEstimada = dia;
                                
                                // Ajustar día si las horas exceden 24
                                while (horaLlegadaEstimada >= 24) {
                                    horaLlegadaEstimada -= 24;
                                    diaLlegadaEstimada++;
                                }
                            } else {
                                // Con escala - duración + tiempo de conexión
                                boolean esIntercontinental = esRutaIntercontinental(origen, destino);
                                int duracionHoras = (esIntercontinental ? 24 : 12) + 2;
                                
                                horaLlegadaEstimada = hora + duracionHoras;
                                diaLlegadaEstimada = dia;
                                
                                // Ajustar día si las horas exceden 24
                                while (horaLlegadaEstimada >= 24) {
                                    horaLlegadaEstimada -= 24;
                                    diaLlegadaEstimada++;
                                }
                            }
                            
                            // Convertir hora de llegada a UTC+0
                            int horaLlegadaUTC = horaLlegadaEstimada - offsetDestinoHoras;
                            
                            // Ajustar día si necesario
                            if (horaLlegadaUTC < 0) {
                                horaLlegadaUTC += 24;
                                diaLlegadaEstimada--;
                            } else if (horaLlegadaUTC >= 24) {
                                horaLlegadaUTC -= 24;
                                diaLlegadaEstimada++;
                            }
                            
                            llegadaUTC = String.format("D%d %02d:%02d UTC", diaLlegadaEstimada, horaLlegadaUTC, minuto);
                            
                            // Validar tiempo de tránsito
                            boolean cumplePlazo = validarTiempoTransito(origen, destino, dia, hora, 
                                                                     diaLlegadaEstimada, horaLlegadaUTC, 59);
                            if (!cumplePlazo) {
                                llegadaUTC += " ⚠️";
                            }
                        }
                    } catch (Exception e) {
                        salidaUTC = "N/A";
                        llegadaUTC = "N/A";
                    }
                } else {
                    salidaUTC = "FALLO";
                    llegadaUTC = "FALLO";
                }
                
                // Si es un pedido dividido, mostrar información más detallada
                if ("DIVIDIDO".equals(ruta.tipoRuta) && ruta.vuelosDetallados != null && !ruta.vuelosDetallados.isEmpty()) {
                    // Mostrar cada vuelo individual con su horario y tipo
                    StringBuilder detalle = new StringBuilder();
                    detalle.append("DIVIDIDO → ");
                    
                    for (int i = 0; i < ruta.vuelosDetallados.size(); i++) {
                        VueloDividido vuelo = ruta.vuelosDetallados.get(i);
                        if (i > 0) detalle.append(", ");
                        
                        // Extraer día y hora del identificador (ej: D17-04:35-340)
                        String[] partes = vuelo.identificador.split("-");
                        if (partes.length >= 3) {
                            String dia = partes[0].substring(1); // Quitar la 'D'
                            String hora = partes[1];
                            String cantidad = partes[2];
                            String tipo = vuelo.tipoRuta.equals("DIRECTA") ? "DIR" : "ESC";
                            detalle.append(String.format("D%s-%s[%s](%s)", dia, hora, cantidad, tipo));
                        } else {
                            detalle.append(vuelo.identificador);
                        }
                    }
                    
                    rutaInfo = detalle.toString();
                } else {
                    rutaInfo = ruta.rutaCompleta;
                }
                
                writer.printf("%s %-23s | %-8s | %4d | %-10s | %-16s | %-16s | %-30s%n",
                             estado,
                             ruta.pedidoId,
                             ruta.destino,
                             ruta.cantidadProductos,
                             ruta.tipoRuta,
                             salidaUTC,
                             llegadaUTC,
                             rutaInfo);
            }
            
            writer.println("─".repeat(140));
            writer.printf("TOTAL: %d pedidos procesados%n", rutasPedidos.size());
            writer.println();
            writer.println("🕐 LEYENDA UTC+0:");
            writer.println("• Salida UTC+0: Tiempo de inicio del pedido convertido a UTC");
            writer.println("• Llegada UTC+0: Tiempo real de llegada (Continental: +12h, Intercontinental: +24h)");  
            writer.println("• ⚠️: Indica exceso de plazo máximo de tránsito");
            writer.println("• Continental: Mismo continente, 12h vuelo, límite ≤2 días desde pedido");
            writer.println("• Intercontinental: Diferente continente, 24h vuelo, límite ≤3 días desde pedido");
            writer.println("• Escalas: +2h tiempo de conexión, Divididos: +4h tiempo adicional");
            
        } catch (IOException e) {
            System.err.println("Error generando reporte rutas por pedidos: " + e.getMessage());
        }
    }
    
    /**
     * REPORTE 3: Pedidos agrupados por planes de vuelo
     */
    private static void generarReportePedidosPorVuelos(List<RutaPedido> rutasPedidos) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_PEDIDOS_POR_VUELOS.txt"))) {
            
            writer.println("=========================================================================");
            writer.println("                 REPORTE: PEDIDOS AGRUPADOS POR PLANES DE VUELO");
            writer.println("=========================================================================");
            writer.println();
            
            // Agrupar pedidos por vuelo
            Map<String, List<RutaPedido>> porVuelo = new HashMap<>();
            for (RutaPedido ruta : rutasPedidos) {
                if (ruta.exitoso && !"DIVIDIDO".equals(ruta.tipoRuta)) {
                    porVuelo.computeIfAbsent(ruta.vuelo, k -> new ArrayList<>()).add(ruta);
                }
            }
            
            // Ordenar vuelos por cantidad de pedidos
            List<Map.Entry<String, List<RutaPedido>>> vuelosOrdenados = new ArrayList<>(porVuelo.entrySet());
            vuelosOrdenados.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
            
            writer.println("✈️  DISTRIBUCIÓN DE PEDIDOS POR VUELO:");
            writer.println("═══════════════════════════════════════");
            writer.println();
            
            for (Map.Entry<String, List<RutaPedido>> entry : vuelosOrdenados) {
                String vuelo = entry.getKey();
                List<RutaPedido> pedidos = entry.getValue();
                
                // Calcular total de productos en este vuelo
                int totalProductosVuelo = pedidos.stream().mapToInt(p -> p.cantidadProductos).sum();
                
                writer.printf("🛫 VUELO: %s (%d pedidos - %,d productos)%n", 
                             vuelo, pedidos.size(), totalProductosVuelo);
                writer.println("─".repeat(80));
                
                for (RutaPedido pedido : pedidos) {
                    writer.printf("   📦 %s → %s (%d productos)%n", 
                                 pedido.pedidoId, pedido.destino, pedido.cantidadProductos);
                }
                writer.println();
            }
            
            writer.printf("📊 RESUMEN: %d vuelos diferentes transportando pedidos%n", porVuelo.size());
            
        } catch (IOException e) {
            System.err.println("Error generando reporte pedidos por vuelos: " + e.getMessage());
        }
    }
    
    /**
     * REPORTE 4: Pedidos divididos con detalle de múltiples vuelos
     */
    private static void generarReportePedidosDivididos(List<RutaPedido> rutasPedidos) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("REPORTE_PEDIDOS_DIVIDIDOS.txt"))) {
            
            writer.println("=========================================================================");
            writer.println("                     REPORTE: PEDIDOS DIVIDIDOS EN MÚLTIPLES VUELOS");
            writer.println("=========================================================================");
            writer.println();
            
            // Filtrar solo pedidos divididos
            List<RutaPedido> pedidosDivididos = rutasPedidos.stream()
                    .filter(r -> "DIVIDIDO".equals(r.tipoRuta))
                    .collect(Collectors.toList());
            
            if (pedidosDivididos.isEmpty()) {
                writer.println("✅ No hay pedidos divididos en este procesamiento.");
                writer.println("   Todos los pedidos pudieron ser asignados a vuelos únicos.");
            } else {
                writer.printf("📦 TOTAL DE PEDIDOS DIVIDIDOS: %d%n", pedidosDivididos.size());
                writer.println("═══════════════════════════════════════════════════════════════");
                writer.println();
                
                for (RutaPedido pedido : pedidosDivididos) {
                    writer.printf("🔄 PEDIDO DIVIDIDO: %s%n", pedido.pedidoId);
                    writer.printf("   📍 Destino: %s%n", pedido.destino);
                    writer.printf("   📦 Cantidad total: %,d productos%n", pedido.cantidadProductos);
                    writer.printf("   🏢 Sede origen: %s%n", pedido.sede);
                    writer.println("   ✈️  VUELOS INDIVIDUALES (horarios locales):");
                    
                    // Mostrar detalle de cada vuelo individual con su tipo de ruta
                    if (pedido.vuelosDetallados != null && !pedido.vuelosDetallados.isEmpty()) {
                        for (int i = 0; i < pedido.vuelosDetallados.size(); i++) {
                            VueloDividido vuelo = pedido.vuelosDetallados.get(i);
                            
                            // Parsear el identificador para mostrar información clara
                            String[] partes = vuelo.identificador.split("-");
                            if (partes.length >= 3) {
                                String dia = partes[0].substring(1); // Quitar la 'D'
                                String hora = partes[1];
                                String cantidad = partes[2];
                                
                                writer.printf("      %d. 📅 Día %s a las %s → %d productos - %s (%s)%n", 
                                             (i + 1), dia, hora, Integer.parseInt(cantidad), 
                                             vuelo.tipoRuta, vuelo.rutaDetalle);
                            } else {
                                writer.printf("      %d. %s - %s (%s)%n", 
                                             (i + 1), vuelo.identificador, vuelo.tipoRuta, vuelo.rutaDetalle);
                            }
                        }
                    } else {
                        // Fallback para compatibilidad con formato anterior
                        writer.printf("      %s%n", pedido.rutaCompleta);
                    }
                    
                    writer.printf("   📋 Resumen: %s%n", pedido.rutaCompleta);
                    writer.println("─".repeat(70));
                    writer.println();
                }
                
                // Estadísticas de división
                int totalProductosDivididos = pedidosDivididos.stream()
                        .mapToInt(p -> p.cantidadProductos).sum();
                double promedioProductos = (double) totalProductosDivididos / pedidosDivididos.size();
                
                writer.println("📊 ESTADÍSTICAS DE DIVISIÓN:");
                writer.println("═══════════════════════════");
                writer.printf("Total productos en pedidos divididos: %,d%n", totalProductosDivididos);
                writer.printf("Promedio productos por pedido dividido: %.1f%n", promedioProductos);
                writer.printf("Porcentaje de pedidos que requirieron división: %.1f%%%n", 
                             (pedidosDivididos.size() * 100.0 / rutasPedidos.size()));
            }
            
        } catch (IOException e) {
            System.err.println("Error generando reporte pedidos divididos: " + e.getMessage());
        }
    }
    
    private static String asignarSedeSegunDestino(String destino) {
        // Sedes MoraPack según ubicación geográfica
        Set<String> sudamerica = Set.of("SKBO", "SABE", "SLLP", "SUAA", "SGAS", "SVMI", "SEQM", "SBBR", "SCEL");
        Set<String> europa = Set.of("LATI", "LOWW", "EHAM", "LKPR", "LDZA", "EKCH", "EDDI", "LBSF");
        Set<String> asia = Set.of("UMMS", "OMDB", "OJAI", "OSDI", "OAKB", "OERK", "OPKC", "OOMS", "OYSN", "VIDP");
        
        if (sudamerica.contains(destino)) {
            return "SPIM"; // Lima, Perú
        } else if (europa.contains(destino)) {
            return "EBCI"; // Bruselas, Bélgica  
        } else if (asia.contains(destino)) {
            return "UBBB"; // Bakú, Azerbaiyán (para Asia)
        } else {
            return "EBCI"; // Por defecto Europa para aeropuertos no clasificados
        }
    }
    
    /**
     * Verifica si existe un vuelo directo entre dos aeropuertos con capacidad suficiente
     */
    private static boolean existeVueloDirecto(String origen, String destino) {
        Map<String, Set<String>> rutasDirectas = crearRutasDirectas();
        
        Set<String> destinosDirectos = rutasDirectas.get(origen);
        return destinosDirectos != null && destinosDirectos.contains(destino);
    }
    
    /**
     * Verifica si existe un vuelo directo con capacidad suficiente para la cantidad de productos
     */
    private static boolean tieneCapacidadSuficiente(String origen, String destino, int cantidadProductos) {
        try (BufferedReader reader = new BufferedReader(new FileReader("datos/vuelos_completos.csv"))) {
            String linea = reader.readLine(); // Saltar encabezado
            
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 5) {
                    String origenVuelo = partes[0].trim();
                    String destinoVuelo = partes[1].trim();
                    int capacidad = Integer.parseInt(partes[4].trim());
                    
                    if (origenVuelo.equals(origen) && destinoVuelo.equals(destino)) {
                        if (capacidad >= cantidadProductos) {
                            return true; // Encontró al menos un vuelo con capacidad suficiente
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("⚠️  Error verificando capacidad de vuelos: " + e.getMessage());
        }
        
        return false; // No encontró vuelo con capacidad suficiente
    }
    
    /**
     * Obtiene la capacidad máxima disponible en la ruta origen -> destino
     */
    private static int obtenerCapacidadMaxima(String origen, String destino) {
        int capacidadMaxima = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader("datos/vuelos_completos.csv"))) {
            String linea = reader.readLine(); // Saltar encabezado
            
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 5) {
                    String origenVuelo = partes[0].trim();
                    String destinoVuelo = partes[1].trim();
                    int capacidad = Integer.parseInt(partes[4].trim());
                    
                    if (origenVuelo.equals(origen) && destinoVuelo.equals(destino)) {
                        capacidadMaxima = Math.max(capacidadMaxima, capacidad);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("⚠️  Error obteniendo capacidad máxima: " + e.getMessage());
        }
        
        return capacidadMaxima;
    }
    
    /**
     * Clase para información detallada de vuelos divididos
     */
    private static class VueloDividido {
        String identificador; // ej: D17-04:35-340
        String tipoRuta;      // DIRECTA o CON ESCALAS
        String rutaDetalle;   // ej: SPIM → SKBO o SPIM → LATI → SKBO
        
        VueloDividido(String identificador, String tipoRuta, String rutaDetalle) {
            this.identificador = identificador;
            this.tipoRuta = tipoRuta;
            this.rutaDetalle = rutaDetalle;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s: %s)", identificador, tipoRuta, rutaDetalle);
        }
    }

    /**
     * Planifica la división de un pedido grande en múltiples vuelos con tipo de ruta
     */
    private static List<VueloDividido> planificarDivisionPedidoDetallada(String origen, String destino, int cantidadTotal, 
                                                                        int capacidadMaxima, int diaInicial, int horaInicial) {
        List<VueloDividido> vuelosAsignados = new ArrayList<>();
        List<VueloInfo> vuelosDisponibles = obtenerVuelosDisponibles(origen, destino);
        
        if (vuelosDisponibles.isEmpty()) {
            return vuelosAsignados; // No hay vuelos disponibles
        }
        
        // Determinar si la ruta origen-destino es directa o con escalas
        boolean esRutaDirecta = existeVueloDirecto(origen, destino);
        String tipoRuta = esRutaDirecta ? "DIRECTA" : "CON ESCALAS";
        String rutaDetalle;
        
        if (esRutaDirecta) {
            rutaDetalle = String.format("%s → %s", origen, destino);
        } else {
            // Encontrar la escala óptima
            String escala = encontrarEscalaOptima(origen, destino);
            if (escala != null) {
                rutaDetalle = String.format("%s → %s → %s", origen, escala, destino);
            } else {
                rutaDetalle = String.format("%s → ??? → %s", origen, destino);
            }
        }
        
        int cantidadRestante = cantidadTotal;
        int diaActual = diaInicial;
        
        // Determinar límite máximo de días según el tipo de ruta
        boolean esIntercontinental = esRutaIntercontinental(origen, destino);
        int limiteDias = esIntercontinental ? 3 : 2;
        
        // Intentar asignar vuelos respetando los límites de tránsito
        for (int diasAdelante = 0; diasAdelante <= limiteDias && cantidadRestante > 0; diasAdelante++) {
            int diaVuelo = diaActual + diasAdelante;
            
            for (VueloInfo vuelo : vuelosDisponibles) {
                if (cantidadRestante <= 0) break;
                
                // Solo usar vuelos del mismo día si es después de la hora del pedido
                if (diasAdelante == 0 && vuelo.horaSalida <= horaInicial) {
                    continue; // Este vuelo ya salió
                }
                
                // Validar tiempo de tránsito
                int horaVuelo = vuelo.horaSalida / 60;
                int minutoVuelo = vuelo.horaSalida % 60;
                
                if (!validarTiempoTransito(origen, destino, diaInicial, horaInicial, 
                                         diaVuelo, horaVuelo, minutoVuelo)) {
                    continue; // Excede límite de tiempo de tránsito
                }
                
                int cantidadAsignar = Math.min(cantidadRestante, vuelo.capacidad);
                String identificadorVuelo = String.format("D%d-%02d:%02d-%d", 
                                                        diaVuelo, horaVuelo, minutoVuelo, cantidadAsignar);
                
                vuelosAsignados.add(new VueloDividido(identificadorVuelo, tipoRuta, rutaDetalle));
                cantidadRestante -= cantidadAsignar;
            }
        }
        
        return vuelosAsignados;
    }


    
    /**
     * Clase auxiliar para información de vuelos
     */
    private static class VueloInfo {
        int horaSalida; // En minutos desde medianoche
        int capacidad;
        
        VueloInfo(int horaSalida, int capacidad) {
            this.horaSalida = horaSalida;
            this.capacidad = capacidad;
        }
    }
    
    /**
     * Obtiene lista de vuelos disponibles en una ruta específica
     */
    private static List<VueloInfo> obtenerVuelosDisponibles(String origen, String destino) {
        List<VueloInfo> vuelos = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader("datos/vuelos_completos.csv"))) {
            String linea = reader.readLine(); // Saltar encabezado
            
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 5) {
                    String origenVuelo = partes[0].trim();
                    String destinoVuelo = partes[1].trim();
                    String horaSalidaStr = partes[2].trim();
                    int capacidad = Integer.parseInt(partes[4].trim());
                    
                    if (origenVuelo.equals(origen) && destinoVuelo.equals(destino)) {
                        // Convertir hora a minutos (formato HH:MM)
                        String[] tiempoPartes = horaSalidaStr.split(":");
                        int horaSalida = Integer.parseInt(tiempoPartes[0]) * 60 + Integer.parseInt(tiempoPartes[1]);
                        
                        vuelos.add(new VueloInfo(horaSalida, capacidad));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("⚠️  Error obteniendo vuelos disponibles: " + e.getMessage());
        }
        
        // Ordenar por hora de salida
        vuelos.sort((a, b) -> Integer.compare(a.horaSalida, b.horaSalida));
        
        return vuelos;
    }
    
    /**
     * Encuentra la mejor escala para conectar origen con destino
     */
    private static String encontrarEscalaOptima(String origen, String destino) {
        Map<String, Set<String>> rutasDirectas = crearRutasDirectas();
        Set<String> destinosDesdeOrigen = rutasDirectas.get(origen);
        
        if (destinosDesdeOrigen == null) return null;
        
        // Buscar escalas que tengan conexión tanto desde origen como hacia destino
        for (String posibleEscala : destinosDesdeOrigen) {
            Set<String> destinosDesdeEscala = rutasDirectas.get(posibleEscala);
            if (destinosDesdeEscala != null && destinosDesdeEscala.contains(destino)) {
                return posibleEscala;
            }
        }
        
        return null; // No hay ruta disponible
    }
    
    /**
     * Carga las rutas directas REALES desde vuelos_completos.csv
     */
    private static Map<String, Set<String>> crearRutasDirectas() {
        Map<String, Set<String>> rutas = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader("datos/vuelos_completos.csv"))) {
            String linea = reader.readLine(); // Saltar encabezado
            
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 2) {
                    String origen = partes[0].trim();
                    String destino = partes[1].trim();
                    
                    // Agregar conexión directa origen -> destino
                    rutas.computeIfAbsent(origen, k -> new HashSet<>()).add(destino);
                }
            }
            
            System.out.printf("✈️  Cargadas %d aeropuertos con rutas directas reales desde vuelos_completos.csv%n", 
                            rutas.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error crítico: No se pudo cargar vuelos_completos.csv");
            System.err.println("   " + e.getMessage());
            throw new RuntimeException("No se puede continuar sin los datos de vuelos reales");
        }
        
        return rutas;
    }
    
    private static Set<String> crearAeropuertosValidos() {
        Set<String> aeropuertos = new HashSet<>();
        aeropuertos.add("SPIM"); aeropuertos.add("SKBO"); aeropuertos.add("SABE");
        aeropuertos.add("SLLP"); aeropuertos.add("SUAA"); aeropuertos.add("SGAS");
        aeropuertos.add("SVMI"); aeropuertos.add("SEQM"); aeropuertos.add("SBBR");
        aeropuertos.add("SCEL"); aeropuertos.add("LATI"); aeropuertos.add("LOWW");
        aeropuertos.add("EHAM"); aeropuertos.add("LKPR"); aeropuertos.add("LDZA");
        aeropuertos.add("EKCH"); aeropuertos.add("EDDI"); aeropuertos.add("LBSF");
        aeropuertos.add("UMMS"); aeropuertos.add("EBCI"); aeropuertos.add("OMDB");
        aeropuertos.add("OJAI"); aeropuertos.add("OSDI"); aeropuertos.add("OAKB");
        aeropuertos.add("OERK"); aeropuertos.add("OPKC"); aeropuertos.add("OOMS");
        aeropuertos.add("OYSN"); aeropuertos.add("VIDP");
        return aeropuertos;
    }
    
    /**
     * Clasifica si una ruta es continental o intercontinental
     */
    private static boolean esRutaIntercontinental(String origen, String destino) {
        String continenteOrigen = obtenerContinente(origen);
        String continenteDestino = obtenerContinente(destino);
        return !continenteOrigen.equals(continenteDestino);
    }
    
    /**
     * Obtiene el continente de un aeropuerto basado en su código ICAO
     */
    private static String obtenerContinente(String codigoAeropuerto) {
        // Sudamérica: códigos que empiezan con 'S'
        if (codigoAeropuerto.startsWith("S")) {
            return "SUDAMERICA";
        }
        // Europa: códigos que empiezan con 'E' o 'L'
        else if (codigoAeropuerto.startsWith("E") || codigoAeropuerto.startsWith("L")) {
            return "EUROPA";
        }
        // Asia/Medio Oriente: códigos que empiezan con 'O', 'U', 'V'
        else if (codigoAeropuerto.startsWith("O") || codigoAeropuerto.startsWith("U") || codigoAeropuerto.startsWith("V")) {
            return "ASIA";
        }
        else {
            return "OTROS";
        }
    }
    
    /**
     * Convierte hora local a UTC+0 basado en la zona horaria del aeropuerto
     */
    private static long convertirAUTC(int dia, int hora, int minutos, String codigoAeropuerto) {
        // Obtener offset UTC del aeropuerto
        int offsetUTC = obtenerOffsetUTC(codigoAeropuerto);
        
        // Calcular minutos totales desde epoch (día 1, 00:00)
        long minutosDesdeEpoch = (dia - 1) * 24 * 60 + hora * 60 + minutos;
        
        // Convertir a UTC restando el offset
        return minutosDesdeEpoch - offsetUTC * 60;
    }
    
    /**
     * Obtiene el offset UTC de un aeropuerto (en horas)
     */
    private static int obtenerOffsetUTC(String codigoAeropuerto) {
        // Sudamérica
        if (codigoAeropuerto.equals("SPIM")) return -5; // Lima: UTC-5
        if (codigoAeropuerto.equals("SKBO")) return -5; // Bogotá: UTC-5
        if (codigoAeropuerto.equals("SABE")) return -3; // Buenos Aires: UTC-3
        if (codigoAeropuerto.equals("SLLP")) return -4; // La Paz: UTC-4
        if (codigoAeropuerto.equals("SUAA")) return -3; // Asunción: UTC-3
        if (codigoAeropuerto.equals("SGAS")) return -4; // São Paulo: UTC-3 (pero considero -4 por horario de verano)
        if (codigoAeropuerto.equals("SVMI")) return -4; // Caracas: UTC-4
        if (codigoAeropuerto.equals("SEQM")) return -5; // Quito: UTC-5
        if (codigoAeropuerto.equals("SBBR")) return -3; // Brasília: UTC-3
        if (codigoAeropuerto.equals("SCEL")) return -4; // Santiago: UTC-4
        
        // Europa
        if (codigoAeropuerto.equals("LATI")) return 1; // Roma: UTC+1
        if (codigoAeropuerto.equals("LOWW")) return 1; // Viena: UTC+1
        if (codigoAeropuerto.equals("EHAM")) return 1; // Amsterdam: UTC+1
        if (codigoAeropuerto.equals("LKPR")) return 1; // Praga: UTC+1
        if (codigoAeropuerto.equals("LDZA")) return 1; // Zagreb: UTC+1
        if (codigoAeropuerto.equals("EKCH")) return 1; // Copenhague: UTC+1
        if (codigoAeropuerto.equals("EDDI")) return 1; // Berlín: UTC+1
        if (codigoAeropuerto.equals("LBSF")) return 2; // Beirut: UTC+2
        if (codigoAeropuerto.equals("EBCI")) return 1; // Bruselas: UTC+1
        
        // Asia/Medio Oriente
        if (codigoAeropuerto.equals("UMMS")) return 6; // Minsk: UTC+3 (pero Rusia puede ser +6)
        if (codigoAeropuerto.equals("OMDB")) return 4; // Dubái: UTC+4
        if (codigoAeropuerto.equals("OJAI")) return 4; // Amman: UTC+2 (pero +4 en verano)
        if (codigoAeropuerto.equals("OSDI")) return 3; // Tel Aviv: UTC+2 (+3 en verano)
        if (codigoAeropuerto.equals("OAKB")) return 3; // Kuwait: UTC+3
        if (codigoAeropuerto.equals("OERK")) return 3; // Riyadh: UTC+3
        if (codigoAeropuerto.equals("OPKC")) return 5; // Karachi: UTC+5
        if (codigoAeropuerto.equals("OOMS")) return 4; // Mascate: UTC+4
        if (codigoAeropuerto.equals("OYSN")) return 3; // Sanaa: UTC+3
        if (codigoAeropuerto.equals("VIDP")) return 5; // Delhi: UTC+5:30 (aproximado a 5)
        if (codigoAeropuerto.equals("UBBB")) return 4; // Bakú: UTC+4
        
        return 0; // UTC por defecto
    }
    
    /**
     * Valida si el tiempo de tránsito cumple con los límites establecidos
     */
    private static boolean validarTiempoTransito(String origen, String destino, int diaInicio, int horaInicio, 
                                                int diaLlegada, int horaLlegada, int minutosLlegada) {
        // Determinar si es ruta intercontinental
        boolean esIntercontinental = esRutaIntercontinental(origen, destino);
        int limiteMaximoDias = esIntercontinental ? 3 : 2;
        
        // Convertir ambos tiempos a UTC
        long inicioUTC = convertirAUTC(diaInicio, horaInicio, 0, origen);
        long llegadaUTC = convertirAUTC(diaLlegada, horaLlegada, minutosLlegada, destino);
        
        // Calcular diferencia en días
        long diferenciaMinutos = llegadaUTC - inicioUTC;
        double diferenciaDias = diferenciaMinutos / (24.0 * 60.0);
        
        // Debe ser positiva (no puede llegar antes de salir) y dentro del límite
        return diferenciaDias >= 0 && diferenciaDias <= limiteMaximoDias;
    }
    
    /**
     * Valida tiempo de tránsito para vuelos directos con duración real
     * Continental: 12 horas, Intercontinental: 24 horas
     */
    private static boolean validarTiempoTransitoDirecto(String origen, String destino, int dia, int hora) {
        // Determinar duración según tipo de ruta
        boolean esIntercontinental = esRutaIntercontinental(origen, destino);
        int duracionHoras = esIntercontinental ? 24 : 12; // 24h intercontinental, 12h continental
        
        // Calcular hora y día de llegada
        int horaLlegada = hora + duracionHoras;
        int diaLlegada = dia;
        
        // Ajustar día si las horas exceden 24
        while (horaLlegada >= 24) {
            horaLlegada -= 24;
            diaLlegada++;
        }
        
        return validarTiempoTransito(origen, destino, dia, hora, diaLlegada, horaLlegada, 0);
    }
    
    /**
     * Valida tiempo de tránsito para rutas con escalas (duración + 2 horas conexión)
     */
    private static boolean validarTiempoTransitoEscala(String origen, String escala, String destino, int dia, int hora) {
        // Para escalas, agregar 2 horas de tiempo de conexión
        boolean esIntercontinental = esRutaIntercontinental(origen, destino);
        int duracionHoras = (esIntercontinental ? 24 : 12) + 2; // +2h para conexión
        
        // Calcular hora y día de llegada
        int horaLlegada = hora + duracionHoras;
        int diaLlegada = dia;
        
        // Ajustar día si las horas exceden 24
        while (horaLlegada >= 24) {
            horaLlegada -= 24;
            diaLlegada++;
        }
        
        return validarTiempoTransito(origen, destino, dia, hora, diaLlegada, horaLlegada, 0);
    }
}
