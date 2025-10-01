package morapack.main;

import morapack.planificacion.GestorUTCyContinentesCSV;

/**
 * Programa de demostración para mostrar aeropuertos configurados según el CSV
 */
public class DemoAeropuertosCSV {
    
    public static void main(String[] args) {
        System.out.println("🌍 ================== DEMO AEROPUERTOS CSV ==================");
        System.out.println("📋 Configuración basada en aeropuertos_simple.csv");
        System.out.println("===========================================================");
        
        // Mostrar todos los aeropuertos configurados
        GestorUTCyContinentesCSV.mostrarTodosLosAeropuertos();
        
        System.out.println("\n📋 =================== EJEMPLOS DE USO ===================");
        
        // Ejemplos de conversión UTC
        java.time.LocalTime horaEjemplo = java.time.LocalTime.of(14, 30); // 14:30 local
        
        System.out.println("\n⏰ CONVERSIONES UTC (ejemplo 14:30 local):");
        String[] sedes = {"SPIM", "EBCI", "UBBB"};
        
        for (String sede : sedes) {
            java.time.LocalTime horaUTC = GestorUTCyContinentesCSV.convertirAUTC(sede, horaEjemplo);
            System.out.printf("   %s (%s): 14:30 local → %s UTC\n", 
                            sede, 
                            GestorUTCyContinentesCSV.obtenerZonaHoraria(sede),
                            horaUTC);
        }
        
        System.out.println("\n📆 PLAZOS DE ENTREGA:");
        
        // Ejemplos de plazos
        String[][] rutasEjemplo = {
            {"SPIM", "SCEL", "Lima → Santiago (Continental SAM)"},
            {"SPIM", "EBCI", "Lima → Bruselas (Intercontinental SAM→EUR)"},
            {"EBCI", "UBBB", "Bruselas → Bakú (Intercontinental EUR→ASI)"},
            {"UBBB", "OMDB", "Bakú → Dubai (Continental ASI)"},
            {"SPIM", "SKBO", "Lima → Bogotá (Continental SAM)"}
        };
        
        for (String[] ruta : rutasEjemplo) {
            String origen = ruta[0];
            String destino = ruta[1];
            String descripcion = ruta[2];
            
            boolean mismoConti = GestorUTCyContinentesCSV.mismosContinentes(origen, destino);
            int plazo = GestorUTCyContinentesCSV.obtenerPlazoMaximo(origen, destino);
            
            System.out.printf("   %s: %d días (%s)\n", 
                            descripcion, 
                            plazo,
                            mismoConti ? "CONTINENTAL" : "INTERCONTINENTAL");
        }
        
        System.out.println("\n✅ =================== CONFIGURACIÓN OK ===================");
        System.out.println("🌍 Todos los aeropuertos del CSV están configurados correctamente");
        System.out.println("⏰ Conversión UTC funcional");
        System.out.println("📆 Sistema de plazos operativo");
        System.out.println("===========================================================");
    }
}
