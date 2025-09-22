package morapack.planificacion;

import morapack.modelo.Vuelo;
import java.util.*;

/**
 * Planificador avanzado de rutas con conexiones múltiples
 */
public class PlanificadorConexiones {
    
    private final List<Vuelo> vuelos;
    private final Map<String, List<Vuelo>> vuelosPorOrigen;
    private final Map<String, List<Vuelo>> vuelosPorDestino;
    
    public PlanificadorConexiones(List<Vuelo> vuelos) {
        this.vuelos = vuelos;
        this.vuelosPorOrigen = new HashMap<>();
        this.vuelosPorDestino = new HashMap<>();
        
        // Indexar vuelos por origen y destino para búsqueda rápida
        for (Vuelo vuelo : vuelos) {
            vuelosPorOrigen.computeIfAbsent(vuelo.getOrigen(), k -> new ArrayList<>()).add(vuelo);
            vuelosPorDestino.computeIfAbsent(vuelo.getDestino(), k -> new ArrayList<>()).add(vuelo);
        }
    }
    
    /**
     * Busca la mejor ruta (directa o con conexiones) de origen a destino
     */
    public RutaCompleta buscarMejorRuta(String origen, String destino, String horaDeseada) {
        // 1. Intentar vuelo directo primero
        RutaCompleta rutaDirecta = buscarVueloDirecto(origen, destino, horaDeseada);
        if (rutaDirecta != null && rutaDirecta.esViable()) {
            return rutaDirecta;
        }
        
        // 2. Buscar ruta con UNA conexión
        RutaCompleta rutaUnaConexion = buscarRutaUnaConexion(origen, destino, horaDeseada);
        if (rutaUnaConexion != null && rutaUnaConexion.esViable()) {
            return rutaUnaConexion;
        }
        
        // 3. Buscar ruta con DOS conexiones (máximo)
        RutaCompleta rutaDosConexiones = buscarRutaDosConexiones(origen, destino, horaDeseada);
        if (rutaDosConexiones != null && rutaDosConexiones.esViable()) {
            return rutaDosConexiones;
        }
        
        // 4. Si no hay rutas viables, devolver null
        return null;
    }
    
    /**
     * Busca vuelo directo
     */
    private RutaCompleta buscarVueloDirecto(String origen, String destino, String horaDeseada) {
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) return null;
        
        Vuelo mejorVuelo = null;
        int mejorDiferencia = Integer.MAX_VALUE;
        int minutosDeseados = convertirHoraAMinutos(horaDeseada);
        
        for (Vuelo vuelo : vuelosDesdeOrigen) {
            if (vuelo.getDestino().equals(destino)) {
                int minutosVuelo = convertirHoraAMinutos(vuelo.getHoraSalida());
                int diferencia = Math.abs(minutosVuelo - minutosDeseados);
                
                if (diferencia < mejorDiferencia) {
                    mejorDiferencia = diferencia;
                    mejorVuelo = vuelo;
                }
            }
        }
        
        if (mejorVuelo != null) {
            RutaCompleta ruta = new RutaCompleta();
            ruta.agregarVuelo(mejorVuelo);
            ruta.setTipoRuta("DIRECTO");
            return ruta;
        }
        
        return null;
    }
    
    /**
     * Busca ruta con UNA conexión: Origen → Escala → Destino
     */
    private RutaCompleta buscarRutaUnaConexion(String origen, String destino, String horaDeseada) {
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) return null;
        
        RutaCompleta mejorRuta = null;
        int mejorTiempoTotal = Integer.MAX_VALUE;
        
        // Para cada vuelo desde origen
        for (Vuelo vuelo1 : vuelosDesdeOrigen) {
            String escala = vuelo1.getDestino();
            
            // Buscar vuelos desde la escala al destino final
            List<Vuelo> vuelosDesdeEscala = vuelosPorOrigen.get(escala);
            if (vuelosDesdeEscala == null) continue;
            
            for (Vuelo vuelo2 : vuelosDesdeEscala) {
                if (vuelo2.getDestino().equals(destino)) {
                    // Verificar que hay tiempo suficiente para la conexión
                    int llegadaEscala = convertirHoraAMinutos(vuelo1.getHoraLlegada());
                    int salidaEscala = convertirHoraAMinutos(vuelo2.getHoraSalida());
                    
                    // Mínimo 60 minutos para conexión
                    if (salidaEscala >= llegadaEscala + 60) {
                        int tiempoTotal = convertirHoraAMinutos(vuelo2.getHoraLlegada()) - 
                                         convertirHoraAMinutos(vuelo1.getHoraSalida());
                        
                        if (tiempoTotal < mejorTiempoTotal) {
                            mejorTiempoTotal = tiempoTotal;
                            mejorRuta = new RutaCompleta();
                            mejorRuta.agregarVuelo(vuelo1);
                            mejorRuta.agregarVuelo(vuelo2);
                            mejorRuta.setTipoRuta("UNA_CONEXION");
                            mejorRuta.setEscalas(Arrays.asList(escala));
                        }
                    }
                }
            }
        }
        
        return mejorRuta;
    }
    
    /**
     * Busca ruta con DOS conexiones: Origen → Escala1 → Escala2 → Destino
     */
    private RutaCompleta buscarRutaDosConexiones(String origen, String destino, String horaDeseada) {
        List<Vuelo> vuelosDesdeOrigen = vuelosPorOrigen.get(origen);
        if (vuelosDesdeOrigen == null) return null;
        
        RutaCompleta mejorRuta = null;
        int mejorTiempoTotal = Integer.MAX_VALUE;
        
        // Origen → Escala1
        for (Vuelo vuelo1 : vuelosDesdeOrigen) {
            String escala1 = vuelo1.getDestino();
            List<Vuelo> vuelosDesdeEscala1 = vuelosPorOrigen.get(escala1);
            if (vuelosDesdeEscala1 == null) continue;
            
            // Escala1 → Escala2
            for (Vuelo vuelo2 : vuelosDesdeEscala1) {
                String escala2 = vuelo2.getDestino();
                if (escala2.equals(destino)) continue; // Ya sería una conexión, no dos
                
                List<Vuelo> vuelosDesdeEscala2 = vuelosPorOrigen.get(escala2);
                if (vuelosDesdeEscala2 == null) continue;
                
                // Escala2 → Destino
                for (Vuelo vuelo3 : vuelosDesdeEscala2) {
                    if (vuelo3.getDestino().equals(destino)) {
                        // Verificar tiempos de conexión
                        int llegada1 = convertirHoraAMinutos(vuelo1.getHoraLlegada());
                        int salida2 = convertirHoraAMinutos(vuelo2.getHoraSalida());
                        int llegada2 = convertirHoraAMinutos(vuelo2.getHoraLlegada());
                        int salida3 = convertirHoraAMinutos(vuelo3.getHoraSalida());
                        
                        // Verificar conexiones viables (60 min mínimo cada una)
                        if (salida2 >= llegada1 + 60 && salida3 >= llegada2 + 60) {
                            int tiempoTotal = convertirHoraAMinutos(vuelo3.getHoraLlegada()) - 
                                             convertirHoraAMinutos(vuelo1.getHoraSalida());
                            
                            if (tiempoTotal < mejorTiempoTotal) {
                                mejorTiempoTotal = tiempoTotal;
                                mejorRuta = new RutaCompleta();
                                mejorRuta.agregarVuelo(vuelo1);
                                mejorRuta.agregarVuelo(vuelo2);
                                mejorRuta.agregarVuelo(vuelo3);
                                mejorRuta.setTipoRuta("DOS_CONEXIONES");
                                mejorRuta.setEscalas(Arrays.asList(escala1, escala2));
                            }
                        }
                    }
                }
            }
        }
        
        return mejorRuta;
    }
    
    private int convertirHoraAMinutos(String hora) {
        try {
            String[] partes = hora.split(":");
            int horas = Integer.parseInt(partes[0]);
            int minutos = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
            return horas * 60 + minutos;
        } catch (Exception e) {
            return 720; // 12:00 PM por defecto
        }
    }
}
