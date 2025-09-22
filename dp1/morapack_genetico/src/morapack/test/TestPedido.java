package morapack.test;

import morapack.modelo.Pedido;

/**
 * Clase de prueba para demostrar el nuevo formato de pedidos
 */
public class TestPedido {
    
    public static void main(String[] args) {
        System.out.println("=== PRUEBAS DEL NUEVO FORMATO DE PEDIDOS ===\n");
        
        // Prueba 1: Crear pedido con constructor automático
        System.out.println("1. Creando pedido con generación automática de ID:");
        Pedido pedido1 = new Pedido("0001234", "SVMI", 45, 1);
        System.out.println("   " + pedido1);
        System.out.println("   ID generado: " + pedido1.getId());
        System.out.println("   Componentes: Día=" + pedido1.getDia() + 
                          ", Hora=" + pedido1.getHora() + 
                          ", Minuto=" + pedido1.getMinuto());
        System.out.println();
        
        // Prueba 2: Crear pedido con ID personalizado
        System.out.println("2. Creando pedido con ID personalizado:");
        String idPersonalizado = Pedido.crearId(15, 14, 30, "SBBR", 125, "9876543");
        System.out.println("   ID creado: " + idPersonalizado);
        
        Pedido pedido2 = new Pedido(idPersonalizado);
        pedido2.setPrioridad(2);
        pedido2.setEstado("PENDIENTE");
        System.out.println("   " + pedido2);
        System.out.println();
        
        // Prueba 3: Validación de formato
        System.out.println("3. Validación de formatos:");
        String[] idsParaPrueba = {
            "15-14-30-SBBR-125-9876543",  // Válido
            "01-08-45-SVMI-001-0000001",  // Válido
            "32-25-60-XXXX-000-123",      // Inválido (día, hora, minuto fuera de rango)
            "15-14-30-SB-125-9876543",    // Inválido (aeropuerto muy corto)
            "15-14-30-SBBR-1250-9876543", // Inválido (cantidad muy larga)
            "abc-def-ghi-SBBR-125-9876543" // Inválido (caracteres no numéricos)
        };
        
        for (String id : idsParaPrueba) {
            boolean esValido = Pedido.esFormatoValido(id);
            System.out.println("   " + id + " -> " + (esValido ? "VÁLIDO" : "INVÁLIDO"));
            
            if (esValido) {
                System.out.println("     Aeropuerto: " + Pedido.extraerAeropuertoDestino(id));
                System.out.println("     Cantidad: " + Pedido.extraerCantidad(id));
                System.out.println("     Cliente: " + Pedido.extraerClienteId(id));
            }
        }
        System.out.println();
        
        // Prueba 4: Ejemplos de IDs mensuales
        System.out.println("4. Ejemplos de pedidos mensuales:");
        String[] ejemplos = {
            Pedido.crearId(1, 8, 15, "SVMI", 1, "0000001"),
            Pedido.crearId(4, 14, 30, "SBBR", 89, "0123456"),
            Pedido.crearId(12, 23, 45, "SCFA", 256, "9876543"),
            Pedido.crearId(24, 1, 59, "KJFK", 999, "5555555")
        };
        
        for (String ejemplo : ejemplos) {
            Pedido p = new Pedido(ejemplo);
            p.setPrioridad((int)(Math.random() * 3) + 1);
            p.setEstado("PENDIENTE");
            System.out.println("   " + p);
        }
        
        System.out.println("\n=== FORMATO EXPLICADO ===");
        System.out.println("dd-hh-mm-dest-###-IdClien");
        System.out.println("dd:     días (01-31)");
        System.out.println("hh:     horas (01-23)");
        System.out.println("mm:     minutos (01-59)");
        System.out.println("dest:   código aeropuerto (4 letras, ej: SVMI, SBBR)");
        System.out.println("###:    cantidad productos (001-999)");
        System.out.println("IdClien: ID cliente (7 dígitos con ceros a la izquierda)");
    }
}
