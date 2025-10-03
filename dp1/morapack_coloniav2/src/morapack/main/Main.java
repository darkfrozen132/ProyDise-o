package morapack.main;

import morapack.colonia.algoritmo.AlgoritmoColoniaHormigas;
import morapack.core.problema.ProblemaMoraPack;
import morapack.core.solucion.Solucion;
import morapack.core.solucion.SolucionMoraPack;
import morapack.datos.modelos.Aeropuerto;
import morapack.datos.modelos.Pedido;
import morapack.datos.modelos.RedDistribucion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal del sistema MoraPack Colonia v2.
 *
 * Este programa utiliza algoritmos de colonias de hormigas (ACO) para optimizar
 * la distribucion de productos MPE desde 3 sedes principales (Lima, Bruselas, Baku)
 * hacia aeropuertos de destino en America, Asia y Europa.
 *
 * Caracteristicas principales:
 * - Entregas parciales multiples (un pedido puede tener varias entregas)
 * - Restricciones temporales (2 dias mismo continente, 3 dias diferente)
 * - Capacidades limitadas (vuelos y almacenes)
 * - Manejo de husos horarios globales
 */
public class Main {

    // Configuracion del sistema
    private static final int MES_EJECUCION = 1;      // Enero
    private static final int ANIO_EJECUCION = 2025;
    private static final int DIA_INICIO = 1;         // Dia 1 del mes

    // Parametros del algoritmo ACO (pueden ser ajustados segun necesidad)
    private static final int NUMERO_HORMIGAS = 5;   // Hormigas en la colonia
    private static final int MAX_ITERACIONES = 10;  // Iteraciones maximas
    private static final double TASA_EVAPORACION = 0.15; // Tasa de evaporacion de feromonas

    // Modo debug
    private static final boolean DEBUG_MODE = true;

    /**
     * Punto de entrada principal del programa
     */
    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println("          MORAPACK COLONIA V2 - ACO OPTIMIZER                ");
        System.out.println("   Sistema de Optimizacion de Rutas de Distribucion Global   ");
        System.out.println("==============================================================");
        System.out.println();

        try {
            // PASO 1: Cargar y configurar la red de distribucion
            System.out.println("=== PASO 1: CARGA DE DATOS ===");
            RedDistribucion red = cargarRedDistribucion();
            System.out.println();

            // PASO 2: Preparar el problema de optimizacion
            System.out.println("=== PASO 2: CONFIGURACION DEL PROBLEMA ===");
            ProblemaMoraPack problema = configurarProblema(red);
            System.out.println();

            // PASO 3: Ejecutar el algoritmo ACO
            System.out.println("=== PASO 3: EJECUCION DEL ALGORITMO ACO ===");
            SolucionMoraPack mejorSolucion = ejecutarAlgoritmoACO(problema);
            System.out.println();

            // PASO 4: Mostrar resultados
            System.out.println("=== PASO 4: RESULTADOS DE LA OPTIMIZACION ===");
            mostrarResultados(mejorSolucion, problema);
            System.out.println();

            // PASO 5: Mostrar detalles de los primeros 20 pedidos (para debugging)
            System.out.println("=== PASO 5: DETALLES DE LOS PRIMEROS 20 PEDIDOS ===");
            mostrarDetallesPedidos(mejorSolucion, problema, 400);
            System.out.println();

            System.out.println("==============================================================");
            System.out.println("                  EJECUCION COMPLETADA                        ");
            System.out.println("==============================================================");

        } catch (Exception e) {
            System.err.println("ERROR CRITICO: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Carga la red de distribucion completa con todos sus componentes:
     * - Aeropuertos globales (31 aeropuertos: 3 sedes + 28 destinos)
     * - Planes de vuelo (horarios y capacidades)
     * - Pedidos del mes (con filtrado automatico de destinos invalidos)
     *
     * @return RedDistribucion configurada y validada
     * @throws Exception si hay problemas en la carga de datos
     */
    private static RedDistribucion cargarRedDistribucion() throws Exception {
        System.out.println("Cargando datos del sistema...");
        System.out.println("   Mes: " + MES_EJECUCION + "/" + ANIO_EJECUCION);
        System.out.println();

        // Crear e inicializar la red
        RedDistribucion red = new RedDistribucion();

        // El metodo inicializar() carga automaticamente:
        // - datos/aeropuertos.csv
        // - datos/planes_de_vuelo.csv
        // - datos/pedidos/pedidos_01.csv
        // Y realiza todas las validaciones de integridad
        red.inicializar(MES_EJECUCION, ANIO_EJECUCION);

        // Establecer tiempo de referencia para simulacion
        LocalDateTime tiempoReferencia = LocalDateTime.of(ANIO_EJECUCION, MES_EJECUCION, DIA_INICIO, 0, 0);
        red.setTiempoReferencia(tiempoReferencia);

        System.out.println("Red de distribucion cargada exitosamente");
        System.out.println("   Tiempo de referencia: " + tiempoReferencia);

        return red;
    }

    /**
     * Configura el problema de optimizacion MoraPack.
     *
     * El problema incluye:
     * - Lista de pedidos a procesar (filtrados y validados)
     * - Red de distribucion (aeropuertos, vuelos, capacidades)
     * - Tiempo de inicio para calculos temporales
     * - Parametros de funcion objetivo (penalizaciones y bonificaciones)
     *
     * @param red Red de distribucion cargada
     * @return ProblemaMoraPack configurado
     */
    private static ProblemaMoraPack configurarProblema(RedDistribucion red) {
        System.out.println("Configurando problema de optimizacion...");

        // Obtener todos los pedidos cargados (ya filtrados por RedDistribucion)
        List<Pedido> pedidos = new ArrayList<>(red.getPedidos().values());

        System.out.println("   Total de pedidos a procesar: " + pedidos.size());

        // Calcular estadisticas de los pedidos
        int totalProductos = pedidos.stream()
                                    .mapToInt(Pedido::getCantidadProductos)
                                    .sum();

        System.out.println("   Total de productos: " + totalProductos);

        // Tiempo de inicio de la simulacion
        LocalDateTime tiempoInicio = red.getTiempoReferencia();

        // Crear problema con parametros por defecto optimizados
        // Constructor usa: alfa=1.0, beta=2.0, Q=100.0
        //                  pesoUrgencia=0.4, pesoCapacidad=0.3, pesoCosto=0.3
        //                  penalizacionRetraso=200.0, bonificacion=500.0
        ProblemaMoraPack problema = new ProblemaMoraPack(red, pedidos, tiempoInicio);

        System.out.println("Problema configurado exitosamente");
        System.out.println("   Funcion objetivo: MAXIMIZAR fitness");
        System.out.println("   (Mayor fitness = Mejor solucion)");

        return problema;
    }

    /**
     * Ejecuta el algoritmo de colonias de hormigas (ACO) para encontrar
     * la mejor solucion de rutas de distribucion.
     *
     * El algoritmo:
     * - Genera multiples soluciones usando hormigas virtuales
     * - Cada hormiga construye rutas basadas en feromonas y heuristicas
     * - Soporta entregas parciales multiples por pedido
     * - Optimiza para cumplir plazos y maximizar eficiencia
     *
     * @param problema Problema de optimizacion configurado
     * @return Mejor solucion encontrada
     */
    private static SolucionMoraPack ejecutarAlgoritmoACO(ProblemaMoraPack problema) {
        System.out.println("Inicializando colonia de hormigas...");
        System.out.println("   Numero de hormigas: " + NUMERO_HORMIGAS);
        System.out.println("   Iteraciones maximas: " + MAX_ITERACIONES);
        System.out.println("   Tasa de evaporacion: " + TASA_EVAPORACION);
        System.out.println();

        if (DEBUG_MODE) {
            System.out.println("[DEBUG] Creando algoritmo ACO...");
        }

        // Crear algoritmo con parametros configurados
        AlgoritmoColoniaHormigas algoritmo = new AlgoritmoColoniaHormigas(
            problema,
            NUMERO_HORMIGAS,
            MAX_ITERACIONES,
            TASA_EVAPORACION
        );

        if (DEBUG_MODE) {
            System.out.println("[DEBUG] Algoritmo creado exitosamente");
        }

        System.out.println("Ejecutando algoritmo ACO...");
        System.out.println("   (Esto puede tomar varios minutos)");
        System.out.println();

        if (DEBUG_MODE) {
            System.out.println("[DEBUG] Llamando a algoritmo.ejecutar()...");
            System.out.println("[DEBUG] Esto puede tardar dependiendo de la complejidad");
        }

        // Ejecutar el problema
        // El metodo ejecutar() ejecuta todas las iteraciones y retorna la mejor solucion
        Solucion solucion = algoritmo.ejecutar();

        if (DEBUG_MODE) {
            System.out.println("[DEBUG] algoritmo.ejecutar() retorno: " +
                             (solucion != null ? "solucion valida" : "null"));
        }

        // Convertir a SolucionMoraPack (cast seguro)
        SolucionMoraPack mejorSolucion = (SolucionMoraPack) solucion;

        System.out.println("Algoritmo ACO completado");

        return mejorSolucion;
    }

    /**
     * Muestra los resultados de la optimizacion de forma clara y detallada.
     *
     * Incluye:
     * - Fitness de la solucion (mayor = mejor)
     * - Estadisticas de pedidos completados
     * - Informacion sobre entregas parciales
     * - Cumplimiento de plazos
     * - Eficiencia del sistema
     *
     * @param solucion Mejor solucion encontrada por ACO
     */
    private static void mostrarResultados(SolucionMoraPack solucion, ProblemaMoraPack problema) {
        if (solucion == null) {
            System.out.println("No se encontro solucion valida");
            return;
        }

        System.out.println("==============================================================");
        System.out.println("                    SOLUCION OPTIMA                           ");
        System.out.println("==============================================================");
        System.out.println();

        // Fitness de la solucion (MAYOR = MEJOR)
        System.out.println("CALIDAD DE LA SOLUCION:");
        double fitness = solucion.getFitness();
        if (fitness < 0.01) {
            System.out.println("   Fitness: " + String.format("%.6e", fitness) + " (notacion cientifica)");
        } else if (fitness < 1.0) {
            System.out.println("   Fitness: " + String.format("%.6f", fitness));
        } else {
            System.out.println("   Fitness: " + String.format("%.2f", fitness));
        }
        System.out.println("   (Mayor fitness indica mejor solucion)");
        System.out.println();

        // Estadisticas generales de la solucion
        System.out.println("ESTADISTICAS GENERALES:");
        System.out.println("   " + solucion.getEstadisticas());
        System.out.println();

        // Estadisticas de entregas parciales
        System.out.println("ENTREGAS PARCIALES:");
        System.out.println("   " + solucion.getEstadisticasEntregasParciales());
        System.out.println();

        // Cumplimiento de plazos
        System.out.println("CUMPLIMIENTO:");
        if (solucion.cumplePlazos()) {
            System.out.println("   Todos los pedidos cumplen plazos de entrega");
        } else {
            System.out.println("   Algunos pedidos tienen retrasos");
        }
        System.out.println();

        // Estadisticas de instancias de vuelos
        System.out.println("USO DE VUELOS DIARIOS:");
        System.out.println("   " + problema.getRed().getEstadisticasInstancias());
        System.out.println();

        // Detalles adicionales
        System.out.println("DETALLES ADICIONALES:");
        System.out.println("   Tiempo de creacion: " + solucion.getTiempoCreacion());
        System.out.println();
    }

    /**
     * Muestra los detalles de las rutas de los primeros N pedidos
     *
     * @param solucion Solucion encontrada
     * @param problema Problema con los datos
     * @param numPedidos Numero de pedidos a mostrar
     */
    private static void mostrarDetallesPedidos(SolucionMoraPack solucion, ProblemaMoraPack problema, int numPedidos) {
        if (solucion == null) {
            System.out.println("No hay solucion para mostrar");
            return;
        }

        List<Pedido> pedidos = problema.getPedidos();
        RedDistribucion red = problema.getRed();

        System.out.println();
        System.out.println("==============================================================");
        System.out.println("        DETALLES DE RUTAS - PRIMEROS " + numPedidos + " PEDIDOS");
        System.out.println("==============================================================");
        System.out.println();

        int contador = 0;
        for (Pedido pedido : pedidos) {
            if (contador >= numPedidos) break;

            // Obtener ID numerico del pedido (usando hash)
            int idNumerico = pedido.getIdPedido().hashCode() & 0x7FFFFFFF;
            List<SolucionMoraPack.RutaProducto> rutas = solucion.getRutasProducto(idNumerico);

            if (rutas == null || rutas.isEmpty()) {
                continue;
            }

            contador++;

            System.out.println("------------------------------------------------------------------");
            System.out.println("PEDIDO #" + contador + ": " + pedido.getIdPedido());
            System.out.println("------------------------------------------------------------------");
            System.out.println("Destino: " + pedido.getCodigoDestino());
            System.out.println("Cantidad total: " + pedido.getCantidadProductos() + " productos");
            System.out.println("Fecha limite: Dia " + pedido.getDia() + " a las " +
                String.format("%02d:%02d", pedido.getHora(), pedido.getMinuto()));

            Aeropuerto destino = red.getAeropuerto(pedido.getCodigoDestino());
            if (destino != null && pedido.getTiempoLimiteEntrega() != null) {
                System.out.println("Plazo calculado: " + pedido.getTiempoLimiteEntrega() +
                    " (" + pedido.getDiasPlazo() + " dias)");
            }
            System.out.println();

            // Mostrar cada entrega
            System.out.println("Entregas planificadas: " + rutas.size());
            System.out.println();

            for (int i = 0; i < rutas.size(); i++) {
                SolucionMoraPack.RutaProducto ruta = rutas.get(i);

                System.out.println("  >>> ENTREGA " + (i + 1) + " de " + rutas.size());
                System.out.println("      Cantidad: " + ruta.getCantidadTransportada() + " productos (" +
                    String.format("%.1f%%", ruta.porcentajeCompletado() * 100.0) + " del total)");
                System.out.println("      Origen: " + ruta.getAeropuertoOrigen());
                System.out.println("      Destino: " + ruta.getAeropuertoDestino());
                System.out.println("      Cumple plazo: " + (ruta.cumplePlazo() ? "SI" : "NO"));
                System.out.println();

                // Mostrar segmentos de vuelo
                List<SolucionMoraPack.SegmentoVuelo> segmentos = ruta.getSegmentos();
                System.out.println("      Ruta (" + segmentos.size() + " segmento" +
                    (segmentos.size() > 1 ? "s" : "") + "):");

                for (int j = 0; j < segmentos.size(); j++) {
                    SolucionMoraPack.SegmentoVuelo seg = segmentos.get(j);
                    System.out.println("        " + (j + 1) + ". " + seg.getAeropuertoOrigen() +
                        " -> " + seg.getAeropuertoDestino());
                    System.out.println("           Vuelo: " + seg.getIdVuelo());
                    System.out.println("           Salida: " + seg.getHoraSalida());
                    System.out.println("           Llegada: " + seg.getHoraLlegada());

                    // Calcular duracion del segmento
                    long duracionMinutos = java.time.Duration.between(seg.getHoraSalida(), seg.getHoraLlegada()).toMinutes();
                    long horas = duracionMinutos / 60;
                    long minutos = duracionMinutos % 60;
                    System.out.println("           Duracion: " + horas + "h " + minutos + "m");
                    System.out.println();
                }

                // Tiempos totales de la entrega
                System.out.println("      Tiempo total de entrega:");
                System.out.println("        Salida: " + ruta.getTiempoSalida());
                System.out.println("        Llegada: " + ruta.getTiempoLlegada());

                long duracionTotalMinutos = java.time.Duration.between(
                    ruta.getTiempoSalida(), ruta.getTiempoLlegada()).toMinutes();
                long horasTotal = duracionTotalMinutos / 60;
                long minutosTotal = duracionTotalMinutos % 60;
                System.out.println("        Duracion total: " + horasTotal + "h " + minutosTotal + "m");
                System.out.println();
            }

            // Resumen del pedido
            int cantidadEntregada = rutas.stream().mapToInt(r -> r.getCantidadTransportada()).sum();
            boolean completado = cantidadEntregada == pedido.getCantidadProductos();
            boolean cumplePlazos = rutas.stream().allMatch(r -> r.cumplePlazo());

            System.out.println("RESUMEN DEL PEDIDO:");
            System.out.println("  Cantidad entregada: " + cantidadEntregada + "/" + pedido.getCantidadProductos() +
                " (" + String.format("%.1f%%", (cantidadEntregada * 100.0 / pedido.getCantidadProductos())) + ")");
            System.out.println("  Estado: " + (completado ? "COMPLETO" : "PARCIAL"));
            System.out.println("  Cumplimiento: " + (cumplePlazos ? "A TIEMPO" : "CON RETRASOS"));
            System.out.println();
        }

        System.out.println("==============================================================");
        System.out.println("Total de pedidos mostrados: " + contador + " de " + pedidos.size());
        System.out.println("==============================================================");
    }

    /**
     * Clase auxiliar para configuracion de ejecucion
     * Permite modificar parametros sin cambiar el codigo principal
     */
    public static class ConfiguracionEjecucion {
        private int mes = MES_EJECUCION;
        private int anio = ANIO_EJECUCION;
        private int numeroHormigas = NUMERO_HORMIGAS;
        private int maxIteraciones = MAX_ITERACIONES;
        private double tasaEvaporacion = TASA_EVAPORACION;

        // Getters y setters
        public int getMes() { return mes; }
        public void setMes(int mes) { this.mes = mes; }

        public int getAnio() { return anio; }
        public void setAnio(int anio) { this.anio = anio; }

        public int getNumeroHormigas() { return numeroHormigas; }
        public void setNumeroHormigas(int numeroHormigas) { this.numeroHormigas = numeroHormigas; }

        public int getMaxIteraciones() { return maxIteraciones; }
        public void setMaxIteraciones(int maxIteraciones) { this.maxIteraciones = maxIteraciones; }

        public double getTasaEvaporacion() { return tasaEvaporacion; }
        public void setTasaEvaporacion(double tasaEvaporacion) { this.tasaEvaporacion = tasaEvaporacion; }
    }
}
