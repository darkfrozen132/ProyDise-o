package morapack.datos;

import morapack.modelo.*;
import morapack.dao.impl.*;
import java.io.*;
import java.util.*;

/**
 * Cargador de datos desde archivos CSV para el sistema MoraPack
 */
public class CargadorDatosCSV {
    
    private static final String RUTA_DATOS = "datos/";
    
    /**
     * Carga aeropuertos desde el archivo CSV
     */
    public static void cargarAeropuertos(AeropuertoDAOImpl aeropuertoDAO) {
        String archivo = RUTA_DATOS + "aeropuertos_simple.csv";
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 6) {
                    String icao = campos[0].trim();
                    String ciudad = campos[1].trim();
                    String pais = campos[2].trim();
                    String codigo = campos[3].trim();
                    int gmt = Integer.parseInt(campos[4].trim());
                    int capacidad = Integer.parseInt(campos[5].trim());
                    
                    // Determinar continente basado en país
                    String continente = determinarContinente(pais);
                    
                    Aeropuerto aeropuerto = new Aeropuerto(icao, ciudad, pais, codigo, gmt, capacidad);
                    aeropuerto.setContinente(continente);
                    
                    // Marcar aeropuertos venezolanos como sedes
                    if ("Venezuela".equals(pais)) {
                        aeropuerto.setSede(true);
                    }
                    
                    aeropuertoDAO.crear(aeropuerto);
                }
            }
            
            System.out.println("✅ Aeropuertos cargados desde CSV: " + aeropuertoDAO.contarTotal());
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar aeropuertos: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("❌ Error en formato de números: " + e.getMessage());
        }
    }
    
    /**
     * Carga vuelos desde el archivo CSV completo (2866 vuelos) y retorna la lista
     */
    public static List<Vuelo> cargarVuelos() {
        String archivo = RUTA_DATOS + "vuelos_simple.csv";
        List<Vuelo> vuelos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 5) {
                    String origen = campos[0].trim();
                    String destino = campos[1].trim();
                    String horaSalida = campos[2].trim();
                    String horaLlegada = campos[3].trim();
                    int capacidad = Integer.parseInt(campos[4].trim());
                    
                    Vuelo vuelo = new Vuelo(origen, destino, horaSalida, horaLlegada, capacidad);
                    vuelos.add(vuelo);
                }
            }
            
            System.out.println("📊 Vuelos disponibles en CSV: " + vuelos.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al leer vuelos: " + e.getMessage());
        }
        
        return vuelos;
    }
    
    /**
     * Calcula la capacidad total diaria: Cdia = Σ(Cv) para todos los vuelos del día
     */
    public static int calcularCapacidadTotalDiaria(List<Vuelo> vuelos) {
        int capacidadTotal = vuelos.stream()
                .mapToInt(Vuelo::getCapacidad)
                .sum();
        
        System.out.println("📈 Capacidad total diaria (Cdia): " + capacidadTotal + " productos");
        return capacidadTotal;
    }
    
    /**
     * Carga clientes desde el archivo CSV
     */
    public static Map<String, String> cargarClientes() {
        String archivo = RUTA_DATOS + "clientes_simple.csv";
        Map<String, String> clientes = new HashMap<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String[] campos = linea.split(",");
                if (campos.length >= 6) {
                    String idCliente = campos[0].trim();
                    String nombre = campos[1].trim();
                    String email = campos[2].trim();
                    String telefono = campos[3].trim();
                    String tipo = campos[4].trim();
                    String credito = campos[5].trim();
                    
                    clientes.put(idCliente, nombre + " (" + tipo + ")");
                }
            }
            
            System.out.println("👥 Clientes cargados desde CSV: " + clientes.size());
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar clientes: " + e.getMessage());
        }
        
        return clientes;
    }
    
    /**
     * Carga pedidos desde el archivo CSV
     */
    public static void cargarPedidosDesdeCSV(PedidoDAOImpl pedidoDAO) {
        String archivo = RUTA_DATOS + "pedidos/pedidos_01.csv";
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                // Formato: IdPedido,AeropuertoOrigen
                String[] partes = linea.trim().split(",");
                if (partes.length >= 2) {
                    String idPedido = partes[0].trim();
                    String aeropuertoOrigen = partes[1].trim();
                    
                    // Validar formato del ID
                    if (Pedido.esFormatoValido(idPedido)) {
                        Pedido pedido = new Pedido(idPedido);
                        pedido.setAeropuertoOrigenId(aeropuertoOrigen); // Asignar aeropuerto origen
                        // Asignar prioridad aleatoria para pruebas
                        pedido.setPrioridad(1 + (int)(Math.random() * 3));
                        pedido.setEstado("PENDIENTE");
                        
                        pedidoDAO.crear(pedido);
                    } else {
                        System.err.println("⚠️  Formato inválido de pedido: " + idPedido);
                    }
                } else {
                    System.err.println("⚠️  Línea inválida en CSV: " + linea);
                }
            }
            
            System.out.println("📦 Pedidos cargados desde CSV: " + pedidoDAO.contarTotal());
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar pedidos: " + e.getMessage());
        }
    }
    
    /**
     * Carga pedidos desde un archivo específico (para procesamiento de múltiples archivos)
     */
    public static List<Pedido> cargarPedidosDesdeArchivo(String nombreArchivo) {
        String archivo = nombreArchivo;
        List<Pedido> pedidos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            boolean primeraLinea = true;
            
            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // Saltar cabecera
                    continue;
                }
                
                String idPedido = linea.trim();
                
                // Validar formato del ID
                if (Pedido.esFormatoValido(idPedido)) {
                    Pedido pedido = new Pedido(idPedido);
                    // Asignar aeropuerto origen aleatorio entre los 3 disponibles
                    String[] aeropuertosOrigen = {"SPIM", "EBCI", "UBBB"};
                    pedido.setAeropuertoOrigenId(aeropuertosOrigen[(int)(Math.random() * 3)]);
                    // Asignar prioridad aleatoria para pruebas
                    pedido.setPrioridad(1 + (int)(Math.random() * 3));
                    pedido.setEstado("PENDIENTE");
                    
                    pedidos.add(pedido);
                } else {
                    System.err.println("⚠️  Formato inválido de pedido: " + idPedido);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar pedidos desde " + nombreArchivo + ": " + e.getMessage());
        }
        
        return pedidos;
    }
    
    /**
     * Crea sedes automáticamente para aeropuertos marcados como sede
     */
    public static void crearSedesDesdeAeropuertos(AeropuertoDAOImpl aeropuertoDAO, SedeDAOImpl sedeDAO) {
        List<Aeropuerto> aeropuertosSede = aeropuertoDAO.obtenerSedes();
        
        for (Aeropuerto aeropuerto : aeropuertosSede) {
            String sedeId = "SEDE_" + aeropuerto.getCodigoICAO();
            String nombreSede = "Sede " + aeropuerto.getCiudad();
            int capacidad = aeropuerto.getCapacidadAlmacen() / 2; // 50% de la capacidad del aeropuerto
            String tipo = "PRINCIPAL"; // Los aeropuertos sede son principales
            
            Sede sede = new Sede(sedeId, nombreSede, aeropuerto.getId(), capacidad, tipo);
            sede.setEstado("ACTIVA");
            
            sedeDAO.crear(sede);
        }
        
        System.out.println("🏢 Sedes creadas desde aeropuertos: " + sedeDAO.contarTotal());
    }
    
    /**
     * Genera pedidos de ejemplo basados en los aeropuertos disponibles
     */
    public static void generarPedidosEjemplo(AeropuertoDAOImpl aeropuertoDAO, PedidoDAOImpl pedidoDAO) {
        List<Aeropuerto> destinos = aeropuertoDAO.obtenerDestinos(); // Aeropuertos que no son sedes
        String[] clientes = {"0000001", "0000123", "0001234", "0005678", "0009876", "0012345", "0054321", "0098765"};
        Random random = new Random();
        
        // Generar pedidos para una semana (7 días)
        for (int dia = 1; dia <= 7; dia++) {
            // 3-5 pedidos por día
            int pedidosPorDia = 3 + random.nextInt(3);
            
            for (int p = 0; p < pedidosPorDia; p++) {
                int hora = 8 + random.nextInt(10); // Entre 8 y 17 horas
                int minuto = random.nextInt(60);
                
                // Seleccionar destino aleatorio
                Aeropuerto destino = destinos.get(random.nextInt(destinos.size()));
                
                int cantidad = 1 + random.nextInt(500); // 1-500 productos
                String cliente = clientes[random.nextInt(clientes.length)];
                
                String idPedido = Pedido.crearId(dia, hora, minuto, destino.getCodigoICAO(), cantidad, cliente);
                
                try {
                    Pedido pedido = new Pedido(idPedido);
                    pedido.setPrioridad(1 + random.nextInt(3)); // Prioridad 1-3
                    pedido.setEstado("PENDIENTE");
                    
                    pedidoDAO.crear(pedido);
                } catch (Exception e) {
                    System.err.println("⚠️  Error creando pedido " + idPedido + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("📦 Pedidos generados: " + pedidoDAO.contarTotal());
    }
    
    /**
     * Carga completa del sistema desde archivos CSV
     */
    public static void cargarSistemaCompleto(AeropuertoDAOImpl aeropuertoDAO, 
                                           SedeDAOImpl sedeDAO, 
                                           PedidoDAOImpl pedidoDAO) {
        System.out.println("🚀 Iniciando carga del sistema desde datos CSV...\n");
        
        // 1. Cargar aeropuertos
        cargarAeropuertos(aeropuertoDAO);
        
        // 2. Cargar información de vuelos (para estadísticas)
        cargarVuelos();
        
        // 3. Cargar clientes
        Map<String, String> clientes = cargarClientes();
        
        // 4. Crear sedes desde aeropuertos
        crearSedesDesdeAeropuertos(aeropuertoDAO, sedeDAO);
        
        // 5. Cargar pedidos reales desde CSV
        cargarPedidosDesdeCSV(pedidoDAO);
        
        System.out.println("\n✨ Sistema cargado completamente:");
        System.out.println("   🛫 Aeropuertos: " + aeropuertoDAO.contarTotal());
        System.out.println("   🏢 Sedes: " + sedeDAO.contarTotal() + " (" + sedeDAO.contarActivas() + " activas)");
        System.out.println("   👥 Clientes: " + clientes.size());
        System.out.println("   📦 Pedidos: " + pedidoDAO.contarTotal() + " (" + pedidoDAO.contarPorEstado("PENDIENTE") + " pendientes)");
    }
    
    /**
     * Determina el continente basado en el país
     */
    private static String determinarContinente(String pais) {
        Map<String, String> paisContinente = new HashMap<>();
        
        // América del Sur
        paisContinente.put("Colombia", "América del Sur");
        paisContinente.put("Ecuador", "América del Sur");
        paisContinente.put("Venezuela", "América del Sur");
        paisContinente.put("Brasil", "América del Sur");
        paisContinente.put("Peru", "América del Sur");
        paisContinente.put("Bolivia", "América del Sur");
        paisContinente.put("Chile", "América del Sur");
        paisContinente.put("Argentina", "América del Sur");
        paisContinente.put("Paraguay", "América del Sur");
        paisContinente.put("Uruguay", "América del Sur");
        
        // Europa
        paisContinente.put("Albania", "Europa");
        paisContinente.put("Alemania", "Europa");
        paisContinente.put("Austria", "Europa");
        paisContinente.put("Belgica", "Europa");
        paisContinente.put("Bielorrusia", "Europa");
        paisContinente.put("Bulgaria", "Europa");
        paisContinente.put("Republica_Checa", "Europa");
        paisContinente.put("Croacia", "Europa");
        paisContinente.put("Dinamarca", "Europa");
        
        return paisContinente.getOrDefault(pais, "Desconocido");
    }
}
