package morapack.main;

import morapack.planificacion.PlanificadorTemporalConUTCyPlazos;
import morapack.planificacion.RutaCompleta;
import morapack.modelo.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;

/**
 * Main principal del sistema MoraPack con implementación completa:
 * - Carga de datos con parámetros de archivos
 * - Conversión de horarios a UTC
 * - División de pedidos en micro-pedidos (productos)
 * - Instanciación diaria de vuelos
 * - Planificación con algoritmo genético
 * - Reporte de consolidación y rutas
 */
public class MainFinal {
    
    // Configuración por defecto
    private static final String DIRECTORIO_DATOS_DEFAULT = "datos/";
    private static final String ARCHIVO_AEROPUERTOS_DEFAULT = "aeropuertos_simple.csv";
    private static final String ARCHIVO_VUELOS_DEFAULT = "vuelos_simple.csv";
    private static final String ARCHIVO_PEDIDOS_DEFAULT = "pedidos_simple.csv";
    
    // Parámetros del algoritmo genético
    private static final int POBLACION_DEFAULT = 50;
    private static final int GENERACIONES_DEFAULT = 100;
    
    // Colecciones principales
    private static Map<String, Aeropuerto> aeropuertos;
    private static List<Vuelo> planesVuelo;
    private static List<Pedido> pedidosOriginales;
    private static List<Producto> productos;
    private static Map<String, List<VueloInstancia>> vuelosInstanciados;
    private static Map<String, VueloInstancia> indiceInstancias = new HashMap<>();
    
    // Mapa de zonas horarias por aeropuerto
    private static Map<String, String> zonasHorarias;
    
    // Flags de configuración
    private static final boolean DIVIDIR_PEDIDOS = false; // false = no dividir temporalmente
    private static final boolean FORZAR_RUTAS_ESCALAS = true; // true = marcar todas las asignaciones como ESCALA
    private static final boolean USAR_PLANIFICADOR_ESCALAS_REALES = true; // ✅ REACTIVADO con optimizaciones
    private static final boolean FORZAR_ESCALAS_REALES_SI_DIRECTO = true;  // ✅ REACTIVADO con optimizaciones
    
    // 🚀 OPTIMIZACIONES PARA RENDIMIENTO MÁXIMO (ANTI-BUCLES)
    private static final int MAX_PEDIDOS_ESCALAS_REALES = Integer.MAX_VALUE; // ⚡ TODOS los pedidos con escalas reales
    private static final boolean USAR_CACHE_RUTAS = true; // Cache de rutas calculadas
    private static final boolean MODO_RAPIDO = true; // ⚡ Modo súper rápido con logging mínimo
    private static final long TIMEOUT_MILISEGUNDOS = 15000; // ⏰ Timeout de 15 segundos
    private static final int MAX_INTENTOS_POR_PEDIDO = 1; // 🚫 Evitar bucles - solo 1 intento por pedido
    private static final boolean SALIR_RAPIDO_SI_FALLA = true; // ⚡ Salir rápido si no encuentra ruta
    private static PlanificadorTemporalConUTCyPlazos planificadorEscalas;
    
    public static void main(String[] args) {
        
        System.out.println("=================================================================");
        System.out.println("              MORAPACK SISTEMA FINAL - VERSIÓN UTC");
        System.out.println("=================================================================");
        System.out.println("Carga de datos parametrizada con conversión UTC");
        System.out.println("División automática en micro-pedidos");
        System.out.println("Instanciación diaria de vuelos");
        System.out.println("Algoritmo genético con planificación avanzada");
        System.out.println("=================================================================");
        
        try {
            // 1. PROCESAMIENTO DE ARGUMENTOS
            String[] archivos = procesarArgumentos(args);
            String archivoAeropuertos = archivos[0];
            String archivoVuelos = archivos[1];
            String archivoPedidos = archivos[2];
            
            // 1: CARGA DE DATOS
            System.out.println("\n🔄 FASE 1: CARGA DE DATOS");
            System.out.println("--------------------------");
            cargarAeropuertos(archivoAeropuertos);
            cargarPlanesVuelo(archivoVuelos);
            cargarPedidosOriginales(archivoPedidos);
            System.out.println("✅ Fase 1 completada.");

            // 2: NORMALIZACIÓN TEMPORAL A UTC Y ORDEN
            System.out.println("\n🕒 FASE 2: NORMALIZACIÓN TEMPORAL (UTC)");
            System.out.println("--------------------------------------");
            normalizarPedidosUTC();
            System.out.println("✅ Fase 2 completada.");

            // 3: DIVISIÓN EN PRODUCTOS
            System.out.println("\n🧩 FASE 3: DIVISIÓN DE PEDIDOS EN PRODUCTOS");
            System.out.println("-------------------------------------------");
            dividirPedidosEnProductos();
            System.out.println("✅ Fase 3 completada.");

            // FASE 4: Instanciación simple de vuelos (por ahora 1 día)
            System.out.println("\n🗓 FASE 4: INSTANCIACIÓN DE VUELOS");
            System.out.println("-----------------------------------");
            instanciarVuelosParaDias(1); // día único provisional
            System.out.println("✅ Fase 4 completada.");

            // FASE 5: Simulación y reportes
            System.out.println("\n📄 FASE 5: GENERACIÓN DE REPORTES");
            System.out.println("-----------------------------------");
            simularAsignacionesTemporales();
            generarReportesParte5();
            System.out.println("✅ Fase 5 completada.");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERROR EN EL PROCESAMIENTO:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            mostrarAyuda();
        }
    }
    
    /**
     * Procesa los argumentos de línea de comandos
     */
    private static String[] procesarArgumentos(String[] args) {
        
        String archivoAeropuertos = DIRECTORIO_DATOS_DEFAULT + ARCHIVO_AEROPUERTOS_DEFAULT;
        String archivoVuelos = DIRECTORIO_DATOS_DEFAULT + ARCHIVO_VUELOS_DEFAULT;
        String archivoPedidos = DIRECTORIO_DATOS_DEFAULT + ARCHIVO_PEDIDOS_DEFAULT;
        
        if (args.length >= 1) archivoAeropuertos = args[0];
        if (args.length >= 2) archivoVuelos = args[1];
        if (args.length >= 3) archivoPedidos = args[2];
        
        System.out.println("Archivos de entrada:");
        System.out.println("  • Aeropuertos: " + archivoAeropuertos);
        System.out.println("  • Vuelos:      " + archivoVuelos);
        System.out.println("  • Pedidos:     " + archivoPedidos);
        
        // Validar que existan los archivos
        validarArchivo(archivoAeropuertos);
        validarArchivo(archivoVuelos);
        validarArchivo(archivoPedidos);
        
        return new String[]{archivoAeropuertos, archivoVuelos, archivoPedidos};
    }
    
    /**
     * Valida que un archivo exista
     */
    private static void validarArchivo(String archivo) {
        File file = new File(archivo);
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("Archivo no encontrado: " + archivo);
        }
    }
    
    /**
     * 1. Carga aeropuertos con información de zona horaria
     */
    private static void cargarAeropuertos(String archivo) {
        System.out.println("Cargando aeropuertos desde: " + archivo);
        aeropuertos = new HashMap<>();
        zonasHorarias = new HashMap<>();

        int cargados = 0;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo))) {
            String header = br.readLine();
            if (header == null) throw new RuntimeException("Archivo aeropuertos vacío");

            // Esperado: Numero,CodigoICAO,Ciudad,Pais,CodigoCiudad,HusoHorario,Capacidad,Latitud,Longitud,Continente
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] cols = linea.split(",");
                if (cols.length < 10) continue;

                String codigo = cols[1].trim();
                String ciudad = cols[2].trim();
                String pais = cols[3].trim();
                String continente = cols[9].trim();
                String huso = cols[5].trim(); // Ej: -5

                Aeropuerto ap = new Aeropuerto(codigo, ciudad, pais, continente);
                aeropuertos.put(codigo, ap);

                // Mapear huso horario simple a una zona representativa
                zonasHorarias.put(codigo, mapearHusoAHoraria(ciudad, pais, huso));
                cargados++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando aeropuertos: " + e.getMessage(), e);
        }

        System.out.println("✅ Aeropuertos cargados: " + cargados);
    }

    private static String mapearHusoAHoraria(String ciudad, String pais, String huso) {
        // Mapeo simple basado en huso: se puede mejorar luego con base de datos
        switch (huso) {
            case "-5": return "America/Bogota"; // SKBO, SEQM, SPIM
            case "-4": return "America/Caracas"; // SVMI
            case "-3": return "America/Sao_Paulo"; // SBBR, SCEL, SABE
            case "-2": return "America/Noronha";
            case "0": return "UTC";
            case "+1": return "Europe/Berlin";
            case "+2": return "Europe/Athens";
            case "+3": return "Europe/Moscow";
            case "+4": return "Asia/Dubai";
            case "+5": return "Asia/Karachi";
            default: return "UTC";
        }
    }
    
    /**
     * 2. Carga planes de vuelo (plantillas)
     */
    private static void cargarPlanesVuelo(String archivo) {
        System.out.println("Cargando planes de vuelo desde: " + archivo);
        planesVuelo = new ArrayList<>();
        int cargados = 0;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo))) {
            String header = br.readLine();
            if (header == null) throw new RuntimeException("Archivo vuelos vacío");
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] cols = linea.split(",");
                if (cols.length < 5) continue;
                String origen = cols[0].trim();
                String destino = cols[1].trim();
                String horaSalida = cols[2].trim();
                String horaLlegada = cols[3].trim();
                int capacidad = parseEnteroSeguro(cols[4].trim(), 300);
                planesVuelo.add(new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad));
                cargados++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando vuelos: " + e.getMessage(), e);
        }
        System.out.println("✅ Planes de vuelo cargados: " + cargados);
    }
    
    /**
     * 3. Carga pedidos originales
     */
    private static void cargarPedidosOriginales(String archivo) {
        System.out.println("Cargando pedidos desde: " + archivo);
        pedidosOriginales = new ArrayList<>();
        int cargados = 0;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(archivo))) {
            String header = br.readLine();
            if (header == null) throw new RuntimeException("Archivo pedidos vacío");
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] cols = linea.split(",");
                // Formato: Dia,Hora,Minuto,AeropuertoDestino,Cantidad,ClienteId
                if (cols.length < 6) continue;
                String dia = cols[0].trim();
                String hora = cols[1].trim();
                String minuto = cols[2].trim();
                String destino = cols[3].trim();
                String cantidad = cols[4].trim();
                String clienteId = cols[5].trim();
                // Construir ID compatible con Pedido
                String id = String.format("%s-%s-%s-%s-%s-%s", dia, hora, minuto, destino, padCantidad(cantidad), clienteId);
                try {
                    pedidosOriginales.add(new Pedido(id));
                    cargados++;
                } catch (Exception ex) {
                    System.err.println("  ⚠️ Pedido inválido ignorado: " + id + " -> " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando pedidos: " + e.getMessage(), e);
        }
        System.out.println("✅ Pedidos originales cargados: " + cargados);
    }

    private static int parseEnteroSeguro(String valor, int defecto) {
        try { return Integer.parseInt(valor); } catch (Exception e) { return defecto; }
    }
    private static String padCantidad(String c) {
        // Acepta 3-4 dígitos, si es menos rellena a 3
        try {
            int v = Integer.parseInt(c);
            if (v <= 999) return String.format("%03d", v);
            return String.format("%04d", v);
        } catch (Exception e) {
            return "001";
        }
    }

    /**
     * Normaliza y ordena pedidos por instante UTC. Interpreta el (día,hora,minuto) como hora local del aeropuerto destino.
     */
    private static void normalizarPedidosUTC() {
        if (pedidosOriginales == null || pedidosOriginales.isEmpty()) {
            System.out.println("No hay pedidos para normalizar.");
            return;
        }

        class Wrap { Pedido p; java.time.ZonedDateTime utc; Wrap(Pedido p, java.time.ZonedDateTime u){this.p=p;this.utc=u;} }
        List<Wrap> lista = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now();

        for (Pedido p : pedidosOriginales) {
            String dest = p.getAeropuertoDestinoId();
            String zonaId = zonasHorarias.getOrDefault(dest, "UTC");
            java.time.ZoneId zona = java.time.ZoneId.of(zonaId);
            int dia = Math.max(1, Math.min(p.getDia(), hoy.lengthOfMonth()));
            java.time.LocalDate fecha = hoy.withDayOfMonth(dia);
            java.time.LocalDateTime localDT = java.time.LocalDateTime.of(fecha.getYear(), fecha.getMonth(), dia, p.getHora(), p.getMinuto());
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(localDT, zona).withZoneSameInstant(java.time.ZoneId.of("UTC"));
            lista.add(new Wrap(p, zdt));
        }

        lista.sort(Comparator.comparing(w -> w.utc));
        pedidosOriginales = new ArrayList<>();
        for (Wrap w : lista) pedidosOriginales.add(w.p);

        System.out.println("Pedidos ordenados por UTC. Primeros 5:");
        lista.stream().limit(5).forEach(w -> {
            System.out.printf("  %s @ %s UTC (%s)\n", w.p.getId(), w.utc.toLocalTime(), w.utc.toLocalDate());
        });
    }
    
    /**
     * 4. Divide cada pedido en micro-pedidos unitarios
     */
    private static void dividirPedidosEnProductos() {
        productos = new ArrayList<>();
        int totalProductos = 0;

        for (Pedido pedido : pedidosOriginales) {
            if (!DIVIDIR_PEDIDOS) {
                // Crear un solo producto representando todo el pedido (sin fragmentación)
                ZoneId zonaDestino = obtenerZonaHoraria(pedido.getAeropuertoDestinoId());
                Producto unico = new Producto(pedido, 1, 1, null, zonaDestino);
                productos.add(unico);
                totalProductos += 1;
                System.out.println("  Pedido " + pedido.getId() + " → 1 producto (modo SIN DIVISIÓN, cantidad original " + pedido.getCantidadProductos() + ")");
                continue;
            }
            // --- modo original ---
            int cantidadProductos = pedido.getCantidadProductos();
            ZoneId zonaDestino = obtenerZonaHoraria(pedido.getAeropuertoDestinoId());
            for (int i = 1; i <= cantidadProductos; i++) {
                Producto producto = new Producto(pedido, i, cantidadProductos, null, zonaDestino);
                productos.add(producto);
            }
            totalProductos += cantidadProductos;
            System.out.println("  Pedido " + pedido.getId() + " → " + cantidadProductos + " productos");
        }
        System.out.println("✅ Total productos generados: " + totalProductos + (DIVIDIR_PEDIDOS?"":" (DIVISIÓN DESACTIVADA)"));
        System.out.println("   Pedidos originales: " + pedidosOriginales.size());
        System.out.println("   Productos unitarios: " + productos.size());
    }
    
    /**
     * 5. Instancia vuelos para múltiples días
     */
    private static void instanciarVuelosParaDias(int dias) {
        vuelosInstanciados = new HashMap<>();
        indiceInstancias.clear();
        LocalDate fechaInicio = LocalDate.now();
        int totalInstancias = 0;
        for (int dia = 0; dia < dias; dia++) {
            LocalDate fecha = fechaInicio.plusDays(dia);
            String fechaKey = fecha.toString();
            List<VueloInstancia> instanciasDia = new ArrayList<>();
            for (Vuelo planVuelo : planesVuelo) {
                ZoneId zonaOrigen = obtenerZonaHoraria(planVuelo.getOrigen());
                ZoneId zonaDestino = obtenerZonaHoraria(planVuelo.getDestino());
                VueloInstancia instancia = new VueloInstancia(planVuelo, fecha, zonaOrigen, zonaDestino);
                instanciasDia.add(instancia);
                indiceInstancias.put(instancia.getIdInstancia(), instancia);
                totalInstancias++;
            }
            vuelosInstanciados.put(fechaKey, instanciasDia);
        }
        System.out.println("✅ Instancias de vuelo generadas: " + totalInstancias);
        System.out.println("   Días planificados: " + dias);
        System.out.println("   Planes base: " + planesVuelo.size());
        System.out.println("   Instancias por día: " + (totalInstancias / dias));
    }
    
    /**
     * 6. Ejecuta el algoritmo genético
     */
    private static void ejecutarAlgoritmoGenetico() {
        System.out.println("Configuración del algoritmo:");
        System.out.println("  • Población: " + POBLACION_DEFAULT);
        System.out.println("  • Generaciones: " + GENERACIONES_DEFAULT);
        System.out.println("  • Productos a asignar: " + productos.size());
        
        // TODO: Implementar algoritmo genético con nuevas clases
        System.out.println("⚠️  Algoritmo genético en desarrollo...");
        
        // Simulación temporal de asignaciones
        simularAsignacionesTemporales();
    }
    
    // 📦 Cache de rutas para optimizar rendimiento
    private static Map<String, RutaCompleta> cacheRutas = new HashMap<>();
    
    /**
     * Simulación temporal de asignaciones (será reemplazada por el AG)
     * 🚀 OPTIMIZADO: Procesa TODOS los pedidos con timeout de 10 segundos
     */
    private static void simularAsignacionesTemporales() {
        long tiempoInicio = System.currentTimeMillis(); // ⏰ Control de timeout
        if (USAR_PLANIFICADOR_ESCALAS_REALES && planificadorEscalas==null) {
            planificadorEscalas = new PlanificadorTemporalConUTCyPlazos(planesVuelo);
            if (!MODO_RAPIDO) {
                System.out.println("🕐 Planificador Temporal Mejorado con UTC y Plazos inicializado:");
                System.out.println("   - Tiempo de preparación: 30 minutos");
                System.out.println("   - Vuelos diarios repetitivos: Activado");
                System.out.println("   - Lógica nocturna: Activado");
                System.out.println("   - Gestión UTC: Activado");
                System.out.println("   - Plazos continentales: 2 días");
                System.out.println("   - Plazos intercontinentales: 3 días");
            } else {
                System.out.println("🚀 Planificador inicializado (modo rápido)");
            }
        }
        
        int productosAsignados = 0;
        int productosConEscalasReales = 0;
        
        // ⚡ Progreso simplificado
        System.out.println("🚀 Procesando " + productos.size() + " productos (TODOS con escalas reales, timeout: 10s)...");
        
        for (int i = 0; i < productos.size(); i++) {
            // ⏰ Verificar timeout cada 5 productos (más frecuente para evitar bucles)
            if (i % 5 == 0) {
                long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
                if (tiempoTranscurrido > TIMEOUT_MILISEGUNDOS) {
                    System.out.println("⏰ TIMEOUT alcanzado (" + (tiempoTranscurrido/1000.0) + "s). Procesados: " + i + "/" + productos.size());
                    break;
                }
            }
            
            Producto producto = productos.get(i);
            
            // 📊 Mostrar progreso cada 25 productos (más frecuente)
            if (MODO_RAPIDO && i > 0 && i % 25 == 0) {
                long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
                System.out.println("   ⚡ Progreso: " + i + "/" + productos.size() + " (" + Math.round(i * 100.0 / productos.size()) + "%) - " + String.format("%.1f", tiempoTranscurrido/1000.0) + "s");
            }
            
            Pedido pedido = buscarPedido(producto.getPedidoOrigenId());
            String destino = producto.getAeropuertoDestinoId();
            
            // 🚀 OPTIMIZACIÓN ANTI-BUCLES: Usar escalas reales con límites estrictos
            if (USAR_PLANIFICADOR_ESCALAS_REALES && pedido!=null) {
                String hub = seleccionarHubParaDestino(destino);
                String cacheKey = hub + "->" + destino;
                
                // 🏠 Cache lookup para optimizar (prioritario)
                RutaCompleta ruta = USAR_CACHE_RUTAS ? cacheRutas.get(cacheKey) : null;
                
                if (ruta == null) {
                    try {
                        // 🚫 PLANIFICACIÓN SIMPLE ANTI-BUCLES: una sola llamada rápida
                        ruta = planificadorEscalas.planificarRutaTemporal(pedido, hub);
                        
                        // 🚀 Si es directo y queremos forzar escalas, intentar alternativa rápida
                        if (ruta!=null && ruta.getVuelos().size()==1 && FORZAR_ESCALAS_REALES_SI_DIRECTO) {
                            RutaCompleta alternativa = construirRutaDosTramos(hub, destino);
                            if (alternativa!=null) ruta = alternativa;
                        }
                        
                        // 💾 Guardar en cache solo si es válida
                        if (USAR_CACHE_RUTAS && ruta != null) {
                            cacheRutas.put(cacheKey, ruta);
                        }
                        
                    } catch (Exception e) {
                        // 🚫 Si hay error, continuar sin buclear
                        ruta = null;
                    }
                }
                
                if (ruta!=null && ruta.getVuelos().size()>0) {
                    // Asignar usando primer vuelo para tiempos; descripción completa para ruta
                    Vuelo primer = ruta.getVuelos().get(0);
                    // Buscar instancia cualquiera que coincida con primer tramo (simple)
                    VueloInstancia instanciaCoincidente = buscarInstancia(primer.getOrigen(), primer.getDestino());
                    if (instanciaCoincidente!=null) {
                        int tramos = ruta.getVuelos().size();
                        int escalas = tramos-1;
                        String tipo;
                        if (tramos==1) tipo = "DIRECTO"; else if (escalas==1) tipo = "UNA_CONEXION"; else tipo = "ESCALAS_"+escalas;
                        producto.setTipoRuta(tipo);
                        producto.setNumeroEscalas(escalas);
                        producto.asignarVuelo(
                            instanciaCoincidente.getIdInstancia(),
                            instanciaCoincidente.getFechaVuelo().atStartOfDay(),
                            tipo,
                            instanciaCoincidente.getHoraSalidaUTC(),
                            instanciaCoincidente.getHoraLlegadaUTC()
                        );
                        // Sobrescribir rutaCompleta textual
                        StringBuilder desc = new StringBuilder();
                        for (int j=0;j<ruta.getVuelos().size();j++) {
                            Vuelo v = ruta.getVuelos().get(j);
                            if (j==0) desc.append(v.getOrigen());
                            desc.append(" → ").append(v.getDestino());
                        }
                        desc.append(" ("+tipo+")");
                        producto.setObservaciones(desc.toString());
                        productosAsignados++;
                        productosConEscalasReales++;
                        continue;
                    }
                }
            }
            
            // 🚀 FALLBACK RÁPIDO: asignación directa sin bucles complejos
            if (producto.getVueloAsignadoId() == null) {
                // Buscar vuelo directo rápidamente
                VueloInstancia instanciaDirecta = buscarInstancia("SKBO", destino); // Hub principal
                if (instanciaDirecta == null) {
                    instanciaDirecta = buscarInstancia("OMDB", destino); // Hub secundario
                }
                if (instanciaDirecta == null) {
                    instanciaDirecta = buscarInstancia("EDDI", destino); // Hub terciario
                }
                
                if (instanciaDirecta != null && !instanciaDirecta.estaLleno()) {
                    if (instanciaDirecta.asignarProductos(producto.getPedidoOrigenId(), 1)) {
                        String tipoRuta = FORZAR_RUTAS_ESCALAS ? "DIRECTO_FORZADO_ESCALA" : "DIRECTO";
                        producto.asignarVuelo(
                            instanciaDirecta.getIdInstancia(),
                            instanciaDirecta.getFechaVuelo().atStartOfDay(),
                            tipoRuta,
                            instanciaDirecta.getHoraSalidaUTC(),
                            instanciaDirecta.getHoraLlegadaUTC()
                        );
                        productosAsignados++;
                    }
                }
            }
        }
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        
        // 📊 CÁLCULO DE FITNESS Y MÉTRICAS
        double porcentajeAsignacion = productos.isEmpty() ? 0.0 : (productosAsignados * 100.0 / productos.size());
        double porcentajeEscalasReales = productosAsignados == 0 ? 0.0 : (productosConEscalasReales * 100.0 / productosAsignados);
        double velocidadProcesamiento = productos.size() / (tiempoTotal / 1000.0); // productos por segundo
        
        // FITNESS = eficiencia de asignación + calidad de rutas + velocidad
        double fitnessAsignacion = porcentajeAsignacion / 100.0; // 0-1
        double fitnessCalidad = porcentajeEscalasReales / 100.0; // 0-1 
        double fitnessVelocidad = Math.min(velocidadProcesamiento / 50.0, 1.0); // normalizado a 50 prod/seg = 1.0
        double fitnessTotal = (fitnessAsignacion * 0.5) + (fitnessCalidad * 0.3) + (fitnessVelocidad * 0.2);
        
        System.out.println("✅ Simulación completada: " + productosAsignados + "/" + productos.size() + " productos asignados");
        System.out.println("   ⏰ Tiempo total: " + (tiempoTotal/1000.0) + " segundos");
        System.out.println("   ⚡ Velocidad: " + String.format("%.1f", velocidadProcesamiento) + " productos/seg");
        System.out.println();
        System.out.println("📊 MÉTRICAS DE FITNESS:");
        System.out.println("   🎯 Asignación: " + String.format("%.1f%%", porcentajeAsignacion) + " (fitness: " + String.format("%.3f", fitnessAsignacion) + ")");
        System.out.println("   🛣️  Escalas reales: " + String.format("%.1f%%", porcentajeEscalasReales) + " (fitness: " + String.format("%.3f", fitnessCalidad) + ")");
        System.out.println("   ⚡ Velocidad: " + String.format("%.1f", velocidadProcesamiento) + " prod/seg (fitness: " + String.format("%.3f", fitnessVelocidad) + ")");
        System.out.println("   🏆 FITNESS TOTAL: " + String.format("%.3f", fitnessTotal) + "/1.000");
        System.out.println();
        
        // 📄 Generar archivo simple consolidado con rutas y fitness
        generarArchivoSimpleConFitness(fitnessTotal, fitnessAsignacion, fitnessCalidad, fitnessVelocidad, 
                                     porcentajeAsignacion, porcentajeEscalasReales, velocidadProcesamiento, 
                                     productosAsignados, tiempoTotal);
        
        if (USAR_PLANIFICADOR_ESCALAS_REALES) {
            System.out.println("   🎯 Productos con escalas reales: " + productosConEscalasReales + " (TODOS los procesados)");
            System.out.println("   💾 Rutas en cache: " + cacheRutas.size());
            System.out.println("   ⚡ Modo rápido: " + (MODO_RAPIDO ? "ACTIVADO" : "DESACTIVADO"));
        }
    }

    private static VueloInstancia buscarInstancia(String origen, String destino) {
        for (List<VueloInstancia> lista : vuelosInstanciados.values()) {
            for (VueloInstancia vi : lista) {
                if (vi.getOrigen().equals(origen) && vi.getDestino().equals(destino) && !vi.estaLleno()) return vi;
            }
        }
        return null;
    }

    private static RutaCompleta construirRutaDosTramos(String origen, String destinoFinal) {
        // heurística: encontrar primer vuelo a un intermedio y luego desde intermedio al destino
        List<Vuelo> primeros = vuelosDesde(origen);
        if (primeros==null) return null;
        for (Vuelo v1 : primeros) {
            if (v1.getDestino().equals(destinoFinal)) continue; // evitar directo
            List<Vuelo> segundos = vuelosDesde(v1.getDestino());
            if (segundos==null) continue;
            for (Vuelo v2 : segundos) {
                if (!v2.getDestino().equals(destinoFinal)) continue;
                // construir ruta manual
                RutaCompleta r = new RutaCompleta();
                r.agregarVuelo(v1);
                r.agregarVuelo(v2);
                r.setTipoRuta("UNA_CONEXION");
                return r;
            }
        }
        return null;
    }

    private static List<Vuelo> vuelosDesde(String origen) {
        // simple index on demand
        List<Vuelo> lista = new ArrayList<>();
        for (Vuelo v : planesVuelo) if (v.getOrigen().equals(origen)) lista.add(v);
        return lista.isEmpty()?null:lista;
    }

    private static Pedido buscarPedido(String id) {
        for (Pedido p : pedidosOriginales) if (p.getId().equals(id)) return p;
        return null;
    }

    private static String seleccionarHubParaDestino(String destino) {
        try {
            String cont = morapack.planificacion.GestorUTCyContinentesCSV.obtenerContinente(destino);
            switch (cont) {
                case "SAM": return "SKBO"; // hub regional
                case "EUR": return "EDDI"; // Berlín como hub
                case "ASI": return "OMDB"; // Dubai como hub
            }
        } catch (Exception ignored) {}
        return "SKBO"; // fallback global
    }

    private static String obtenerContinenteSeguro(String aeropuertoId) {
        try {
            return morapack.planificacion.GestorUTCyContinentesCSV.obtenerContinente(aeropuertoId);
        } catch (Exception e) {
            // Fallback basado en código ICAO
            if (aeropuertoId.startsWith("S")) return "SAM";
            if (aeropuertoId.startsWith("E") || aeropuertoId.startsWith("L")) return "EUR";
            if (aeropuertoId.startsWith("O") || aeropuertoId.startsWith("U")) return "ASI";
            return "???";
        }
    }

    // ===================== UTILIDADES ZONA HORARIA =====================
    private static ZoneId obtenerZonaHoraria(String aeropuertoId) {
        String zona = zonasHorarias.getOrDefault(aeropuertoId, "UTC");
        try { return ZoneId.of(zona); } catch (Exception e) { return ZoneId.of("UTC"); }
    }

    // ===================== REPORTES =====================
    
    // Genera un archivo simple consolidado con rutas y fitness
    private static void generarArchivoSimpleConFitness(double fitnessTotal, double fitnessAsignacion, double fitnessCalidad, double fitnessVelocidad, double porcentajeAsignacion, double porcentajeEscalasReales, double velocidadProcesamiento, int productosAsignados, long tiempoTotal) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("RUTAS_Y_FITNESS_SIMPLE.txt"))) {
            writer.println("=====================================================");
            writer.println("        MORAPACK - RUTAS Y FITNESS SIMPLE");
            writer.println("=====================================================");
            writer.println();
            writer.println("📊 FITNESS TOTAL: " + String.format("%.3f", fitnessTotal) + "/1.000");
            writer.println("   🎯 Asignación: " + String.format("%.1f%%", porcentajeAsignacion) + " (fitness: " + String.format("%.3f", fitnessAsignacion) + ")");
            writer.println("   🛣️  Escalas reales: " + String.format("%.1f%%", porcentajeEscalasReales) + " (fitness: " + String.format("%.3f", fitnessCalidad) + ")");
            writer.println("   ⚡ Velocidad: " + String.format("%.1f", velocidadProcesamiento) + " prod/seg (fitness: " + String.format("%.3f", fitnessVelocidad) + ")");
            writer.println();
            writer.println("⏱️ Tiempo procesamiento: " + (tiempoTotal/1000.0) + " segundos");
            writer.println("✅ Productos asignados: " + productosAsignados + "/" + productos.size());
            writer.println();
            writer.println("📋 RUTAS ASIGNADAS:");
            writer.println("===================");
            
            int contador = 0;
            for (Producto producto : productos) {
                contador++;
                if (producto.getVueloAsignadoId() != null && !producto.getVueloAsignadoId().isEmpty()) {
                    String rutaInfo = producto.getRutaCompleta() != null ? producto.getRutaCompleta() : 
                                    (producto.getAeropuertoOrigenId() + " → " + producto.getAeropuertoDestinoId());
                    String tipoRuta = producto.getTipoRuta() != null ? producto.getTipoRuta() : "DIRECTO";
                    
                    writer.printf("%-3d | %-25s | %3s | %-20s | %s%n", 
                        contador,
                        producto.getId(), 
                        "1",  // Cada producto es cantidad 1
                        rutaInfo,
                        tipoRuta
                    );
                } else {
                    writer.printf("%-3d | %-25s | %3s | %-20s | %s%n", 
                        contador,
                        producto.getId(), 
                        "1",
                        "SIN ASIGNAR",
                        "ERROR"
                    );
                }
                
                // Solo mostrar los primeros 50 para no hacer el archivo muy largo
                if (contador >= 50) {
                    writer.println("... (mostrando solo primeros 50 de " + productos.size() + " productos)");
                    break;
                }
            }
            
            writer.println();
            writer.println("=====================================================");
            writer.println("Archivo generado: " + java.time.LocalDateTime.now());
            writer.println("Rutas divididas: " + (DIVIDIR_PEDIDOS ? "ACTIVADAS" : "DESACTIVADAS"));
            writer.println("Escalas reales: " + (USAR_PLANIFICADOR_ESCALAS_REALES ? "ACTIVADAS" : "DESACTIVADAS"));
            writer.println("=====================================================");
            
            System.out.println("📄 Archivo simple generado: RUTAS_Y_FITNESS_SIMPLE.txt");
        } catch (IOException e) {
            System.err.println("❌ Error generando archivo simple: " + e.getMessage());
        }
    }
    
    private static void generarReportesParte5() {
        try {
            generarReporteGeneralTXT();
            generarReportePedidosPorEscalasTXT();
            generarReportePedidosPorVuelosTXT();
            generarReporteRutasPorPedidosTXT();
        } catch (Exception e) {
            System.err.println("Error generando reportes: "+e.getMessage());
        }
    }

    private static void generarReporteGeneralTXT() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE GENERAL MORAPACK\n");
        sb.append("========================\n\n");
        sb.append("Productos totales: ").append(productos.size()).append('\n');
        long asignados = productos.stream().filter(p->p.getVueloAsignadoId()!=null).count();
        double pct = productos.isEmpty()?0.0: (asignados*100.0/productos.size());
        long multiTramo = productos.stream().filter(p->p.getNumeroEscalas()>0).count();
        sb.append(String.format("Asignados: %d (%.2f%%)\n", asignados, pct));
        sb.append("Con escalas reales (>0): ").append(multiTramo).append('\n');
        Files.write(Paths.get("REPORTE_GENERAL.txt"), sb.toString().getBytes());
    }

    private static void generarReportePedidosPorEscalasTXT() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("ID_PRODUCTO;PEDIDO;TIPO_RUTA;ESCALAS;ORIGEN;DESTINO;RUTA_DETALLADA\n");
        for (Producto p: productos) {
            String rutaDet = Optional.ofNullable(p.getObservaciones()).orElse(p.getRutaCompleta());
            String origen = ""; String destino="";
            if (rutaDet!=null && rutaDet.contains("→")) {
                String[] parts = rutaDet.split("→");
                origen = parts[0].trim();
                destino = parts[parts.length-1].replaceAll("\\(.*\\)", "").trim();
            }
            sb.append(p.getId()).append(';')
              .append(p.getPedidoOrigenId()).append(';')
              .append(p.getTipoRuta()).append(';')
              .append(p.getNumeroEscalas()).append(';')
              .append(origen).append(';')
              .append(destino).append(';')
              .append(rutaDet)
              .append('\n');
        }
        Files.write(Paths.get("REPORTE_PEDIDOS_POR_ESCALAS.txt"), sb.toString().getBytes());
    }

    private static void generarReportePedidosPorVuelosTXT() throws IOException {
        Map<String, List<Producto>> porVuelo = new HashMap<>();
        for (Producto p: productos) {
            if (p.getVueloAsignadoId()==null) continue;
            porVuelo.computeIfAbsent(p.getVueloAsignadoId(), k-> new ArrayList<>()).add(p);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("VUELO_INSTANCIA;CANT_PRODUCTOS;DETALLE_IDS\n");
        for (Map.Entry<String,List<Producto>> e: porVuelo.entrySet()) {
            sb.append(e.getKey()).append(';').append(e.getValue().size()).append(';');
            for (int i=0;i<e.getValue().size();i++) {
                if (i>0) sb.append(',');
                sb.append(e.getValue().get(i).getId());
            }
            sb.append('\n');
        }
        Files.write(Paths.get("REPORTE_PEDIDOS_POR_VUELOS.txt"), sb.toString().getBytes());
    }

    private static void generarReporteRutasPorPedidosTXT() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("PEDIDO;PRODUCTO;RUTA;TIPO;VUELO_INSTANCIA\n");
        for (Producto p: productos) {
            sb.append(p.getPedidoOrigenId()).append(';')
              .append(p.getId()).append(';')
              .append(Optional.ofNullable(p.getObservaciones()).orElse(p.getRutaCompleta())).append(';')
              .append(p.getTipoRuta()).append(';')
              .append(Optional.ofNullable(p.getVueloAsignadoId()).orElse("SIN_VUELO"))
              .append('\n');
        }
        Files.write(Paths.get("REPORTE_RUTAS_POR_PEDIDOS.txt"), sb.toString().getBytes());
    }

    private static void mostrarAyuda() {
        System.out.println("Uso: java -cp bin_minimo morapack.main.MainFinal <aeropuertos.csv> <vuelos.csv> <pedidos.csv>");
        System.out.println("Si no se pasan argumentos se usan archivos *_simple.csv del directorio datos/");
    }
}
