package morapack;

import morapack.dao.impl.*;
import morapack.datos.CargadorDatosCSV;
import morapack.modelo.Vuelo;
import morapack.modelo.Pedido;
import morapack.planificacion.PlanificadorConexiones;
import morapack.planificacion.RutaCompleta;
import java.util.List;

/**
 * Demostración del sistema avanzado de conexiones
 */
public class DemoConexiones {
    
    public static void main(String[] args) {
        System.out.println("=== DEMOSTRACIÓN DE SISTEMA DE CONEXIONES AVANZADO ===\n");
        
        // Cargar datos
        AeropuertoDAOImpl aeropuertoDAO = new AeropuertoDAOImpl();
        CargadorDatosCSV.cargarAeropuertos(aeropuertoDAO);
        List<Vuelo> vuelos = CargadorDatosCSV.cargarVuelos();
        
        System.out.println("📂 Datos cargados: " + vuelos.size() + " vuelos disponibles");
        
        // Crear planificador avanzado
        PlanificadorConexiones planificador = new PlanificadorConexiones(vuelos);
        
        // Casos de prueba realistas
        String[][] casosPrueba = {
            {"SPIM", "LKPR", "10:00", "Lima a Praga"},
            {"EBCI", "SUAA", "14:30", "Bruselas a Quito"},
            {"UBBB", "SGAS", "08:15", "Baku a Caracas"},
            {"SPIM", "EKCH", "16:45", "Lima a Copenhague"},
            {"EBCI", "SCEL", "12:00", "Bruselas a Santiago"}
        };
        
        System.out.println("\n🗺️  ANÁLISIS DE RUTAS CON CONEXIONES:");
        System.out.println("=".repeat(120));
        
        for (String[] caso : casosPrueba) {
            String origen = caso[0];
            String destino = caso[1];
            String hora = caso[2];
            String descripcion = caso[3];
            
            System.out.println("\n📍 CASO: " + descripcion + " (" + origen + " → " + destino + " a las " + hora + ")");
            System.out.println("-".repeat(80));
            
            // Buscar mejor ruta
            RutaCompleta mejorRuta = planificador.buscarMejorRuta(origen, destino, hora);
            
            if (mejorRuta != null && mejorRuta.esViable()) {
                System.out.println("✅ RUTA ENCONTRADA:");
                System.out.println("   " + mejorRuta.obtenerDescripcion());
                System.out.println("   Tiempo total: " + mejorRuta.calcularTiempoTotal() + " minutos");
                System.out.println("   Vuelos necesarios: " + mejorRuta.getVuelos().size());
                
                if (!mejorRuta.getEscalas().isEmpty()) {
                    System.out.println("   Escalas: " + String.join(" → ", mejorRuta.getEscalas()));
                }
            } else {
                System.out.println("❌ NO SE ENCONTRÓ RUTA VIABLE");
                System.out.println("   Posibles razones:");
                System.out.println("   • No hay vuelos desde " + origen);
                System.out.println("   • No hay vuelos hacia " + destino);
                System.out.println("   • Horarios incompatibles para conexiones");
            }
        }
        
        System.out.println("\n" + "=".repeat(120));
        System.out.println("📋 RESUMEN DEL ALGORITMO:");
        System.out.println("1. BUSCA VUELO DIRECTO: Origen → Destino");
        System.out.println("2. BUSCA 1 CONEXIÓN: Origen → Escala → Destino");
        System.out.println("3. BUSCA 2 CONEXIONES: Origen → Escala1 → Escala2 → Destino");
        System.out.println("4. VERIFICA TIEMPOS: Mínimo 60 minutos entre conexiones");
        System.out.println("5. OPTIMIZA: Selecciona ruta con menor tiempo total");
        System.out.println("=".repeat(120));
    }
}
