public class TestContinentes {
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
    
    private static boolean esRutaIntercontinental(String origen, String destino) {
        String continenteOrigen = obtenerContinente(origen);
        String continenteDestino = obtenerContinente(destino);
        System.out.println(origen + " (" + continenteOrigen + ") -> " + destino + " (" + continenteDestino + ")");
        return !continenteOrigen.equals(continenteDestino);
    }
    
    public static void main(String[] args) {
        // Pedido 231: del reporte
        System.out.println("Verificando pedido 231:");
        System.out.println("SPIM -> OJAI es intercontinental? " + esRutaIntercontinental("SPIM", "OJAI"));
        System.out.println();
        System.out.println("Verificando pedido 066:");
        System.out.println("SPIM -> OJAI es intercontinental? " + esRutaIntercontinental("SPIM", "OJAI"));
    }
}
