// package com.proyecto.backend.algoritmo;

// Versión STANDALONE - ejecutar con: java genetico.java

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

/*
 * ALGORITMO: Genetic Algorithm con Decoder Greedy + Control de Capacidad
 * 
 * CROMOSOMA: double[] keys (1 valor por vuelo plantilla)
 * 
 * FITNESS: 1.0*onTime - 1.5*late - 4.0*capViol + 0.001*slack
 * 
 * DECODER (buildSubrouteFromHub):
 *   1. Para cada pedido (ordenados por release time):
 *      a. Para cada hub exportador:
 *         i.   Construir subruta hub→destino (greedy expansión)
 *         ii.  En cada paso:
 *              - Enumerar vuelos factibles (tiempo, capacidad)
 *              - Filtrar revisitas (Set<visited>)
 *              - Puntuar con: key + early + geo + direct + twoHop
 *              - Validar capacidad de almacén si hay espera
 *         iii. Seleccionar mejor por score
 *      b. Elegir hub con arribo más temprano
 *      c. Repetir hasta servir cantidad completa
 * 
 * HEURÍSTICA CLAVE:
 *   - wDirect=50.0:  Gran bonus si vuelo va directo al destino
 *   - wTwoHop=5.0:   Bonus si next hop tiene directo a destino
 *   - wGeo=0.05:     Progreso Haversine hacia destino
 *   - wEarly=0.1:    Prioriza llegadas tempranas
 *   - wKey=1.0:      Valor del gen (exploración GA)
 */
public class genetico {

    // ===================== Parámetros de negocio =====================
    private static final int MIN_TURN_MIN = 30;               // conexión mínima
    private static final int DUE_SAME_MIN = 2 * 24 * 60;      // 2 días
    private static final int DUE_CROSS_MIN = 3 * 24 * 60;     // 3 días
    private static final int PICKUP_WINDOW_MIN = 120;         // 2 horas de recojo

    // ===================== Parámetros GA =============================
    private static final int POP_SIZE = 50;
    private static final int MAX_GEN  = 200;
    private static final double PCROSS = 0.8;
    private static final double PMUT   = 0.05;
    private static final int ELITE_K   = 4;
    private static final int NO_IMPROV_LIMIT = 40;

    // Objetivo
    private static final double LAMBDA_ONTIME = 1.0;
    private static final double LAMBDA_LATE   = 1.5;
    private static final double LAMBDA_CAPVIO = 4.0;
    private static final double LAMBDA_SLACK  = 0.001;

    // Stock/Almacén
    private static final int SLOT_MIN = 60;
    private static final int TOPK_CANDIDATES = 2000;

    // Exportadores
    private Set<String> exportHubs = new HashSet<>(Arrays.asList("SPIM", "EBCI", "UBBB"));

    // Datos del problema
    private List<PedidoInput> pedidos;
    private List<PlanDeVueloInput> vuelos;
    private Map<String, AeropuertoInput> aeropuertos;
    private World world;
    private int horizonDays = 31;
    private Random random;

    // ===================== Clases de datos de entrada (simples) =====================
    
    static class AeropuertoInput {
        String codigo;
        String ciudad;
        double latitud;
        double longitud;
        int zonaHoraria;
        int capacidadAlmacenamiento;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getCiudad() { return ciudad; }
        public void setCiudad(String ciudad) { this.ciudad = ciudad; }
        public double getLatitud() { return latitud; }
        public void setLatitud(double latitud) { this.latitud = latitud; }
        public double getLongitud() { return longitud; }
        public void setLongitud(double longitud) { this.longitud = longitud; }
        public int getZonaHoraria() { return zonaHoraria; }
        public void setZonaHoraria(int zonaHoraria) { this.zonaHoraria = zonaHoraria; }
        public int getCapacidadAlmacenamiento() { return capacidadAlmacenamiento; }
        public void setCapacidadAlmacenamiento(int capacidadAlmacenamiento) { 
            this.capacidadAlmacenamiento = capacidadAlmacenamiento; 
        }
    }

    static class PlanDeVueloInput {
        String aeropuertoOrigen;
        String aeropuertoDestino;
        LocalTime horaSalida;
        LocalTime horaLlegada;
        int capacidad;

        public String getAeropuertoOrigen() { return aeropuertoOrigen; }
        public void setAeropuertoOrigen(String aeropuertoOrigen) { 
            this.aeropuertoOrigen = aeropuertoOrigen; 
        }
        public String getAeropuertoDestino() { return aeropuertoDestino; }
        public void setAeropuertoDestino(String aeropuertoDestino) { 
            this.aeropuertoDestino = aeropuertoDestino; 
        }
        public LocalTime getHoraSalida() { return horaSalida; }
        public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }
        public LocalTime getHoraLlegada() { return horaLlegada; }
        public void setHoraLlegada(LocalTime horaLlegada) { this.horaLlegada = horaLlegada; }
        public int getCapacidad() { return capacidad; }
        public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    }

    static class PedidoInput {
        int dia;
        int hora;
        int minuto;
        String aeropuertoDestino;
        int cantidadProductos;
        String clienteId;

        public int getDia() { return dia; }
        public void setDia(int dia) { this.dia = dia; }
        public int getHora() { return hora; }
        public void setHora(int hora) { this.hora = hora; }
        public int getMinuto() { return minuto; }
        public void setMinuto(int minuto) { this.minuto = minuto; }
        public String getAeropuertoDestino() { return aeropuertoDestino; }
        public void setAeropuertoDestino(String aeropuertoDestino) { 
            this.aeropuertoDestino = aeropuertoDestino; 
        }
        public int getCantidadProductos() { return cantidadProductos; }
        public void setCantidadProductos(int cantidadProductos) { 
            this.cantidadProductos = cantidadProductos; 
        }
        public String getClienteId() { return clienteId; }
        public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    }

    // ===================== Modelos de datos ==========================
    enum Continent { SOUTH_AMERICA, EUROPE, ASIA, OTHER }

    static class Airport {
        String code;
        int gmt;
        int storageCap;
        double lat, lon;
        Continent continent;
        boolean isExporter;

        public Airport(String code, int gmt, int storageCap, double lat, double lon, boolean isExporter) {
            this.code = code;
            this.gmt = gmt;
            this.storageCap = storageCap;
            this.lat = lat;
            this.lon = lon;
            this.continent = inferContinent(code);
            this.isExporter = isExporter;
        }
    }

    static Continent inferContinent(String icao) {
        if (icao == null || icao.isEmpty()) return Continent.OTHER;
        char c = icao.charAt(0);
        if (c == 'S') return Continent.SOUTH_AMERICA;
        if (c == 'E' || c == 'L' || c == 'U') return Continent.EUROPE;
        if (c == 'O' || c == 'V') return Continent.ASIA;
        return Continent.OTHER;
    }

    static class Flight {
        String orig, dest;
        int depLocalMin;
        int arrLocalMin;
        int capacity;
        public Flight(String o, String d, int dep, int arr, int cap) {
            orig=o; dest=d; depLocalMin=dep; arrLocalMin=arr; capacity=cap;
        }
    }

    static class Order {
        String dest;
        int qty;
        int releaseMinUTC;
        int dayOfMonth;
        String clientId;

        public Order(String dest, int qty, int releaseMinUTC, int dayOfMonth, String clientId) {
            this.dest = dest;
            this.qty = qty;
            this.releaseMinUTC = releaseMinUTC;
            this.dayOfMonth = dayOfMonth;
            this.clientId = clientId;
        }
    }

    static class FlightUse {
        Flight flight;
        int dayIndex;
        int depUTC, arrUTC;
        int qtyAssigned;
        public FlightUse(Flight f, int d, int depUTC, int arrUTC, int qty) {
            this.flight=f; this.dayIndex=d; this.depUTC=depUTC; this.arrUTC=arrUTC; this.qtyAssigned=qty;
        }
    }

    static class SubRoute {
        String originHub;
        List<FlightUse> legs = new ArrayList<>(8);
        int qty;
        int arrivalUTC;
    }

    static class Solution {
        Map<Order, List<SubRoute>> routes = new HashMap<>();
        Map<String,Integer> capUsed = new HashMap<>();
        int servedOnTime, servedLate, capViol, avgSlack;
        double objective;
    }

    static class World {
        Map<String,Airport> airports = new HashMap<>();
        List<Flight> flights = new ArrayList<>();
        Map<String,List<Flight>> outByAirport = new HashMap<>();
        List<String> hubList = new ArrayList<>();
    }

    // ===================== Precomputación ============================
    static class Precomp {
        final int[][] depUTC;
        final int[][] arrUTC;
        final Map<Flight,Integer> flightIndex = new HashMap<>();
        final Map<String,int[]> outIdxByAirport = new HashMap<>();

        Precomp(int numFlights, int horizonDays){
            depUTC = new int[numFlights][horizonDays];
            arrUTC = new int[numFlights][horizonDays];
        }
    }

    static Precomp precompute(World W, int horizonDays){
        int n = W.flights.size();
        Precomp P = new Precomp(n, horizonDays);

        for (int i=0;i<n;i++) P.flightIndex.put(W.flights.get(i), i);

        for (int fi=0; fi<n; fi++){
            Flight f = W.flights.get(fi);
            Airport aO = W.airports.get(f.orig), aD = W.airports.get(f.dest);
            int baseDep = toUTCFromLocal(aO, f.depLocalMin);
            int baseArr = toUTCFromLocal(aD, f.arrLocalMin);
            for (int d=0; d<horizonDays; d++){
                int dep = baseDep + d*1440;
                int arr = baseArr + d*1440;
                if (arr<dep) arr += 1440;
                P.depUTC[fi][d]=dep;
                P.arrUTC[fi][d]=arr;
            }
        }
        for (Map.Entry<String,List<Flight>> e: W.outByAirport.entrySet()){
            String ap = e.getKey();
            List<Flight> lst = e.getValue();
            int[] idx = new int[lst.size()];
            for (int i=0;i<idx.length;i++) idx[i] = P.flightIndex.get(lst.get(i));
            P.outIdxByAirport.put(ap, idx);
        }
        return P;
    }

    // ===================== Stock Tracker =============================
    static class StockTracker {
        static class Store {
            final int[] delta;
            final int[] pref;
            boolean dirty = true;
            Store(int n){ delta=new int[n+1]; pref=new int[n+1]; }
        }
        final int numSlots;
        final World W;
        final Map<String,Store> stores = new HashMap<>();

        StockTracker(World W, int horizonDays){
            this.W = W;
            this.numSlots = (horizonDays*1440)/SLOT_MIN + 5;
        }
        Store get(String ap){
            return stores.computeIfAbsent(ap, k->new Store(numSlots));
        }
        int capacityOf(Airport a){
            if (a==null) return Integer.MAX_VALUE;
            return a.isExporter ? Integer.MAX_VALUE : a.storageCap;
        }
        boolean canFit(String ap, int slotStart, int slotEnd, int qty){
            if (slotStart>=slotEnd || qty<=0) return true;
            Airport a=W.airports.get(ap);
            int cap = capacityOf(a);
            if (cap==Integer.MAX_VALUE) return true;
            Store st = get(ap);
            if (st.dirty){
                int run=0;
                for (int i=0;i<st.pref.length;i++){ run += st.delta[i]; st.pref[i]=run; }
                st.dirty=false;
            }
            for (int t=slotStart; t<slotEnd; t++){
                if (st.pref[t] + qty > cap) return false;
            }
            return true;
        }
        void addInterval(String ap, int slotStart, int slotEnd, int qty){
            if (slotStart>=slotEnd || qty==0) return;
            Store st = get(ap);
            st.delta[slotStart]+=qty; st.delta[slotEnd]-=qty;
            st.dirty = true;
        }
    }

    // ===================== Utilidades ================================
    static int toUTCFromLocal(Airport a, int localMin) { return localMin - a.gmt*60; }
    static int slotOf(int utcMin, int numSlots){
        int s = utcMin / SLOT_MIN;
        if (s<0) return 0;
        if (s>=numSlots) return numSlots-1;
        return s;
    }
    static double haversineKm(double lat1,double lon1,double lat2,double lon2){
        double R=6371.0;
        double dLat=Math.toRadians(lat2-lat1);
        double dLon=Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    // ===================== Cromosoma =================================
    static class Chromosome {
        double[] keys;
        Chromosome(int n) { keys = new double[n]; }
        Chromosome copy(){ Chromosome c=new Chromosome(keys.length); System.arraycopy(keys,0,c.keys,0,keys.length); return c; }
    }
    static Chromosome randomChromosome(int n, Random rnd){
        Chromosome c=new Chromosome(n);
        for(int i=0;i<n;i++) c.keys[i]=rnd.nextDouble();
        return c;
    }

    // ===================== Contextos =================================
    static class SelectContext {
        World W;
        Precomp P;
        Chromosome chrom;
        String destTarget;
        double wKey = 1.0, wEarly = 0.1, wGeo = 0.05;
        double wDirect = 50.0;
        double wTwoHop = 5.0;
        Map<String,Double> distToDestByAp = Collections.emptyMap();
        Set<String> hasDirectToTarget = Collections.emptySet();
    }

    static class FlightCandidate {
        int fi, dayIndex, depUTC, arrUTC;
        int avail;
    }

    static class LegStep {
        int flightIndex;
        int dayIndex;
        int depUTC;
        int arrUTC;
        String waitAirport;
        int waitStartSlot;
        int waitEndSlot;
        boolean requiresStorage;
    }

    static class DecodeContext {
        World W;
        int horizonDays;
        Map<String,Integer> capUsedMap;
        Precomp P;
        StockTracker stock;
        Chromosome chrom;
        Random rnd;
        int numSlots;
        List<FlightCandidate> candBuf = new ArrayList<>(64);
        int[][] capUsed;
        Map<String, Map<String,Double>> distCache = new HashMap<>();
        Map<String, Set<String>> directCache = new HashMap<>();
    }

    static class DestReservation {
        int startSlot;
        int endSlot;
        int qty;
        DestReservation(int s, int e, int q){ startSlot=s; endSlot=e; qty=q; }
    }

    static class Scored {
        Chromosome c;
        double fit;
        Scored(Chromosome c, double fit){ this.c=c; this.fit=fit; }
    }

    // ===================== Constructor ===============================
    public genetico() {
        this.random = new Random();
    }

    public void inicializar(List<PedidoInput> pedidos, List<PlanDeVueloInput> vuelos, List<AeropuertoInput> aeropuertosList) {
        this.pedidos = pedidos;
        this.vuelos = vuelos;

        this.aeropuertos = new HashMap<>();
        for (AeropuertoInput aeropuerto : aeropuertosList) {
            this.aeropuertos.put(aeropuerto.getCodigo(), aeropuerto);
        }

        this.world = convertToWorld(aeropuertosList, vuelos);

        System.out.println("=== ALGORITMO GENÉTICO INICIALIZADO ===");
        System.out.println("Pedidos: " + pedidos.size() + ", Vuelos: " + vuelos.size() + ", Aeropuertos: " + aeropuertos.size());
    }

    private World convertToWorld(List<AeropuertoInput> aeropuertosList, List<PlanDeVueloInput> vuelosList) {
        World W = new World();
        
        for (Aeropuerto a : aeropuertosList) {
            boolean isHub = exportHubs.contains(a.getCodigoICAO());
            Airport ap = new Airport(
                a.getCodigoICAO(),
                a.getZonaHoraria(),
                a.getCapacidadAlmacenamiento(),
                a.getLatitud(),
                a.getLongitud(),
                isHub
            );
            W.airports.put(ap.code, ap);
            if (isHub) W.hubList.add(ap.code);
        }
        
        for (PlanDeVuelo v : vuelosList) {
            LocalTime salida = v.getHoraSalida();
            LocalTime llegada = v.getHoraLlegada();
            
            int depMin = salida.getHour() * 60 + salida.getMinute();
            int arrMin = llegada.getHour() * 60 + llegada.getMinute();
            
            Flight f = new Flight(v.getAeropuertoOrigen(), v.getAeropuertoDestino(), depMin, arrMin, v.getCapacidadMaxima());
            W.flights.add(f);
            W.outByAirport.computeIfAbsent(f.orig, k->new ArrayList<>()).add(f);
        }
        
        W.outByAirport.values().forEach(lst -> lst.sort(Comparator.comparingInt(fl->fl.depLocalMin)));
        
        return W;
    }

    private List<Order> convertOrders(List<Pedido> pedidosList) {
        List<Order> orders = new ArrayList<>();
        for (Pedido p : pedidosList) {
            int releaseMinUTC = (p.getDia()-1)*1440 + p.getHora()*60 + p.getMinuto();
            orders.add(new Order(p.getAeropuertoDestino(), p.getCantidadProductos(), releaseMinUTC, p.getDia(), p.getClienteId()));
        }
        return orders;
    }

    // ===================== Ejecutar GA ===============================
    public Solution ejecutar() {
        if (pedidos == null || vuelos == null) {
            throw new IllegalStateException("Debe inicializar el algoritmo");
        }
        
        log.info("Iniciando GA...");
        List<Order> orders = convertOrders(pedidos);
        Solution best = runGA(world, orders, horizonDays, System.currentTimeMillis());
        
        log.info("Completado! Fitness: {}, OnTime: {}, Late: {}", best.objective, best.servedOnTime, best.servedLate);
        return best;
    }

    static Solution runGA(World W, List<Order> orders, int horizonDays, long seed){
        Random rnd = new Random(seed);
        List<Chromosome> pop = new ArrayList<>(POP_SIZE);
        for (int i=0;i<POP_SIZE;i++) pop.add(randomChromosome(W.flights.size(), rnd));

        List<Order> ordersSorted = new ArrayList<>(orders);
        ordersSorted.sort(Comparator.comparingInt(o->o.releaseMinUTC));

        Precomp precomputed = precompute(W, horizonDays);
        Chromosome best=null; double bestFit=-1e18; int stall=0;

        for (int gen=1; gen<=MAX_GEN; gen++){
            List<Scored> scored = new ArrayList<>(POP_SIZE);
            for (Chromosome c : pop){
                scored.add(new Scored(c, fitness(W, ordersSorted, c, horizonDays, precomputed)));
            }

            scored.sort((a,b)->Double.compare(b.fit, a.fit));
            List<Chromosome> next = new ArrayList<>(POP_SIZE);
            for (int i=0;i<ELITE_K;i++) next.add(scored.get(i).c.copy());

            while (next.size()<POP_SIZE){
                Chromosome p1 = scored.get(rnd.nextInt(scored.size())).c;
                Chromosome p2 = scored.get(rnd.nextInt(scored.size())).c;
                Chromosome ch = crossover(p1,p2,rnd);
                mutate(ch,rnd);
                next.add(ch);
            }
            pop = next;

            double iterFit = scored.get(0).fit;
            if (iterFit > bestFit){ bestFit=iterFit; best=scored.get(0).c.copy(); stall=0; }
            else stall++;

            if (stall>=NO_IMPROV_LIMIT) break;
        }
        return decodeSorted(W, ordersSorted, best, horizonDays, seed, precomputed);
    }

    static double fitness(World W, List<Order> ordersSorted, Chromosome c, int horizonDays, Precomp precomputed){
        return decodeSorted(W, ordersSorted, c, horizonDays, 12345L, precomputed).objective;
    }

    static Chromosome crossover(Chromosome a, Chromosome b, Random rnd){
        if (rnd.nextDouble()>PCROSS) return rnd.nextBoolean()?a.copy():b.copy();
        Chromosome c=new Chromosome(a.keys.length);
        for (int i=0;i<a.keys.length;i++) c.keys[i]=(rnd.nextBoolean()?a.keys[i]:b.keys[i]);
        return c;
    }

    static void mutate(Chromosome c, Random rnd){
        for (int i=0;i<c.keys.length;i++){
            if (rnd.nextDouble()<PMUT){
                double v = c.keys[i] + rnd.nextGaussian()*0.1;
                c.keys[i] = (v<0.0)?0.0:((v>1.0)?1.0:v);
            }
        }
    }

    // ===================== Decodificador (Decoder) ==================
    static Solution decodeSorted(World W, List<Order> ordenesSorted, Chromosome cromosoma, int diasHorizonte, long semilla, Precomp precomputado){
        Solution solucion = new Solution();
        solucion.capUsed = new HashMap<>();
        StockTracker rastreadorStock = new StockTracker(W, diasHorizonte);
        Precomp P = precomputado;

        DecodeContext contextoDecodificacion = new DecodeContext();
        contextoDecodificacion.W=W; 
        contextoDecodificacion.horizonDays=diasHorizonte; 
        contextoDecodificacion.capUsedMap=solucion.capUsed; 
        contextoDecodificacion.P=P; 
        contextoDecodificacion.stock=rastreadorStock; 
        contextoDecodificacion.chrom=cromosoma; 
        contextoDecodificacion.rnd=new Random(semilla);
        contextoDecodificacion.numSlots = (diasHorizonte*1440)/SLOT_MIN + 5;
        contextoDecodificacion.capUsed = new int[W.flights.size()][diasHorizonte];

        int aTiempo=0, tarde=0, violaciones=0; 
        long sumaHolgura=0; 
        int contadorHolgura=0;

        for (Order orden: ordenesSorted){
            int restante = orden.qty;
            List<SubRoute> subrutas = new ArrayList<>(4);

            List<DestReservation> reservasDestino = new ArrayList<>();
            int ultimaLlegada = -1;

            int guardia=0, guardiaMax=500;
            while (restante>0 && guardia++<guardiaMax){
                SubRoute mejorSubruta = null;
                int mejorLlegada = Integer.MAX_VALUE;
                int cantidadSolicitada = restante;

                for (String hub: obtenerHubs(W)) {
                    int fechaLimite = calcularFechaLimiteParaHub(W, hub, orden.dest, orden.releaseMinUTC);
                    SubRoute subruta = construirSubrutaDesdeHub(contextoDecodificacion, orden, hub, fechaLimite, cantidadSolicitada);
                    if (subruta != null && subruta.qty > 0 && subruta.arrivalUTC < mejorLlegada) {
                        mejorLlegada = subruta.arrivalUTC;
                        mejorSubruta = subruta;
                    }
                }

                if (mejorSubruta == null) break;

                int inicioSlot = slotOf(mejorSubruta.arrivalUTC, contextoDecodificacion.numSlots);
                int finSlot = slotOf(mejorSubruta.arrivalUTC + PICKUP_WINDOW_MIN, contextoDecodificacion.numSlots);
                reservasDestino.add(new DestReservation(inicioSlot, finSlot, mejorSubruta.qty));

                subrutas.add(mejorSubruta);
                restante -= mejorSubruta.qty;

                // Extender reservas de destino
                if (mejorSubruta.arrivalUTC > ultimaLlegada) {
                    int nuevaUltimaLlegada = mejorSubruta.arrivalUTC;
                    int nuevoFinSlot = slotOf(nuevaUltimaLlegada + PICKUP_WINDOW_MIN, contextoDecodificacion.numSlots);
                    for (DestReservation reserva: reservasDestino){
                        if (reserva.endSlot < nuevoFinSlot){
                            if (rastreadorStock.canFit(orden.dest, reserva.endSlot, nuevoFinSlot, reserva.qty)) {
                                rastreadorStock.addInterval(orden.dest, reserva.endSlot, nuevoFinSlot, reserva.qty);
                                reserva.endSlot = nuevoFinSlot;
                            } else {
                                violaciones++;
                            }
                        }
                    }
                    ultimaLlegada = nuevaUltimaLlegada;
                }
            }

            solucion.routes.put(orden, subrutas);

            int entregado = 0;
            for (int i=0;i<subrutas.size();i++) entregado += subrutas.get(i).qty;

            if (entregado < orden.qty) {
                tarde++;
            } else {
                SubRoute subutaCritica = null;
                int llegadaMaxima = Integer.MIN_VALUE;
                for (int i=0;i<subrutas.size();i++){
                    SubRoute subruta = subrutas.get(i);
                    if (subruta.arrivalUTC > llegadaMaxima){ 
                        llegadaMaxima = subruta.arrivalUTC; 
                        subutaCritica = subruta; 
                    }
                }
                if (subutaCritica != null){
                    int fechaLimiteCritica = calcularFechaLimiteParaHub(W, subutaCritica.originHub, orden.dest, orden.releaseMinUTC);
                    if (subutaCritica.arrivalUTC <= fechaLimiteCritica) {
                        aTiempo++;
                        sumaHolgura += (fechaLimiteCritica - subutaCritica.arrivalUTC);
                        contadorHolgura++;
                    } else tarde++;
                } else tarde++;
            }
        }

        solucion.servedOnTime = aTiempo; 
        solucion.servedLate = tarde; 
        solucion.capViol = violaciones;
        solucion.avgSlack = (contadorHolgura==0)?0:(int)(sumaHolgura/contadorHolgura);
        solucion.objective = LAMBDA_ONTIME*aTiempo - LAMBDA_LATE*tarde - LAMBDA_CAPVIO*violaciones + LAMBDA_SLACK*solucion.avgSlack;
        return solucion;
    }

    static int calcularFechaLimiteParaHub(World W, String hub, String destino, int tiempoLiberacionMinUTC){
        Airport aeropuertoHub = W.airports.get(hub);
        Airport aeropuertoDestino = W.airports.get(destino);
        boolean mismoContinente = (aeropuertoHub!=null && aeropuertoDestino!=null && aeropuertoHub.continent==aeropuertoDestino.continent);
        return tiempoLiberacionMinUTC + (mismoContinente ? DUE_SAME_MIN : DUE_CROSS_MIN) + PICKUP_WINDOW_MIN;
    }

    static List<String> obtenerHubs(World W){
        return W.hubList;
    }

    static SubRoute construirSubrutaDesdeHub(DecodeContext contextoDecodificacion, Order orden, String hub, int fechaLimite, int cantidadBloque){
        if (cantidadBloque <= 0) return null;

        String actual = hub;
        int tiempoActual = orden.releaseMinUTC;
        int cantidadSolicitada = cantidadBloque;

        SelectContext contextoSeleccion = new SelectContext();
        contextoSeleccion.W = contextoDecodificacion.W; 
        contextoSeleccion.P = contextoDecodificacion.P; 
        contextoSeleccion.chrom = contextoDecodificacion.chrom; 
        contextoSeleccion.destTarget = orden.dest;
        contextoSeleccion.distToDestByAp = obtenerDistanciasADestino(contextoDecodificacion, orden.dest);
        contextoSeleccion.hasDirectToTarget = obtenerOrigenesDirectosADestino(contextoDecodificacion, orden.dest);

        int expansiones = 0, maxExpansiones = 2000;
        boolean primerTramo = true;
        Set<String> visitados = new HashSet<>();
        visitados.add(hub);

        List<LegStep> plan = new ArrayList<>(8);
        int capacidadRuta = cantidadSolicitada;
        int ultimaLlegadaUTC = -1;

        while (!actual.equals(orden.dest) && expansiones++ < maxExpansiones) {
            int cantidadNecesaria = Math.max(1, capacidadRuta);
            int conteo = enumerarCandidatos(contextoDecodificacion, actual, tiempoActual, fechaLimite, cantidadNecesaria);
            if (conteo==0) return null;

            contextoDecodificacion.candBuf.removeIf(candidato -> {
                String siguiente = contextoDecodificacion.W.flights.get(candidato.fi).dest;
                return visitados.contains(siguiente);
            });
            if (contextoDecodificacion.candBuf.isEmpty()) return null;

            FlightCandidate elegido = null;
            while (true) {
                FlightCandidate mejor = seleccionarPorPrioridad(contextoDecodificacion.candBuf, contextoSeleccion);
                if (mejor == null) return null;

                Flight vuelo = contextoDecodificacion.W.flights.get(mejor.fi);
                int disponibleTramo = mejor.avail;
                if (disponibleTramo <= 0) {
                    contextoDecodificacion.candBuf.remove(mejor);
                    if (contextoDecodificacion.candBuf.isEmpty()) return null;
                    continue;
                }

                int capacidadPropuesta = Math.min(capacidadRuta, disponibleTramo);
                if (capacidadPropuesta <= 0) {
                    contextoDecodificacion.candBuf.remove(mejor);
                    if (contextoDecodificacion.candBuf.isEmpty()) return null;
                    continue;
                }

                String aeropuertoEspera = null;
                int inicioSlotEspera = 0;
                int finSlotEspera = 0;
                boolean requiereAlmacenamiento = false;

                Airport aeropuertoActual = contextoDecodificacion.W.airports.get(actual);
                boolean esHubActual = (aeropuertoActual!=null && aeropuertoActual.isExporter);
                if (!primerTramo && !esHubActual) {
                    aeropuertoEspera = actual;
                    inicioSlotEspera = slotOf(tiempoActual, contextoDecodificacion.numSlots);
                    finSlotEspera = slotOf(mejor.depUTC, contextoDecodificacion.numSlots);
                    if (inicioSlotEspera < finSlotEspera) {
                        int ajuste = calcularMaximoAjusteAlmacenamiento(contextoDecodificacion.stock, aeropuertoEspera, inicioSlotEspera, finSlotEspera, capacidadPropuesta);
                        if (ajuste <= 0) {
                            contextoDecodificacion.candBuf.remove(mejor);
                            if (contextoDecodificacion.candBuf.isEmpty()) return null;
                            continue;
                        }
                        capacidadPropuesta = Math.min(capacidadPropuesta, ajuste);
                        requiereAlmacenamiento = true;
                    }
                }

                capacidadRuta = capacidadPropuesta;

                LegStep paso = new LegStep();
                paso.flightIndex = mejor.fi;
                paso.dayIndex = mejor.dayIndex;
                paso.depUTC = mejor.depUTC;
                paso.arrUTC = mejor.arrUTC;
                paso.waitAirport = aeropuertoEspera;
                paso.waitStartSlot = inicioSlotEspera;
                paso.waitEndSlot = finSlotEspera;
                paso.requiresStorage = requiereAlmacenamiento;
                plan.add(paso);

                actual = vuelo.dest;
                tiempoActual = mejor.arrUTC;
                ultimaLlegadaUTC = tiempoActual;
                primerTramo = false;
                visitados.add(actual);
                elegido = mejor;
                break;
            }

            if (elegido == null) return null;
        }

        if (!actual.equals(orden.dest)) return null;
        if (plan.isEmpty()) return null;
        if (capacidadRuta <= 0) return null;

        int cantidadFinal = Math.min(capacidadRuta, cantidadSolicitada);
        if (cantidadFinal <= 0) return null;

        int inicioSlotDestino = slotOf(ultimaLlegadaUTC, contextoDecodificacion.numSlots);
        int finSlotDestino = slotOf(ultimaLlegadaUTC + PICKUP_WINDOW_MIN, contextoDecodificacion.numSlots);
        int ajusteDestino = calcularMaximoAjusteAlmacenamiento(contextoDecodificacion.stock, orden.dest, inicioSlotDestino, finSlotDestino, cantidadFinal);
        if (ajusteDestino <= 0) return null;
        cantidadFinal = Math.min(cantidadFinal, ajusteDestino);
        if (cantidadFinal <= 0) return null;

        SubRoute subruta = new SubRoute();
        subruta.originHub = hub;
        subruta.qty = cantidadFinal;
        subruta.arrivalUTC = ultimaLlegadaUTC;

        for (LegStep paso : plan) {
            Flight vuelo = contextoDecodificacion.W.flights.get(paso.flightIndex);
            if (paso.requiresStorage) {
                contextoDecodificacion.stock.addInterval(paso.waitAirport, paso.waitStartSlot, paso.waitEndSlot, cantidadFinal);
            }

            int usado = contextoDecodificacion.capUsed[paso.flightIndex][paso.dayIndex];
            contextoDecodificacion.capUsed[paso.flightIndex][paso.dayIndex] = usado + cantidadFinal;

            String clave = generarClaveVuelo(vuelo, paso.dayIndex);
            int usadoMapa = contextoDecodificacion.capUsedMap.getOrDefault(clave, 0);
            contextoDecodificacion.capUsedMap.put(clave, usadoMapa + cantidadFinal);

            subruta.legs.add(new FlightUse(vuelo, paso.dayIndex, paso.depUTC, paso.arrUTC, cantidadFinal));
        }

        contextoDecodificacion.stock.addInterval(orden.dest, inicioSlotDestino, finSlotDestino, cantidadFinal);

        return subruta;
    }

    static String generarClaveVuelo(Flight vuelo, int dia){ 
        return vuelo.orig+">"+vuelo.dest+"@D"+dia+"#"+vuelo.depLocalMin; 
    }

    static int enumerarCandidatos(DecodeContext contextoDecodificacion, String actual, int tiempoActualUTC, int fechaLimite, int cantidadNecesaria){
        contextoDecodificacion.candBuf.clear();
        int[] indicesSalida = contextoDecodificacion.P.outIdxByAirport.getOrDefault(actual, null);
        if (indicesSalida==null) return 0;

        Airport aeropuertoOrigen = contextoDecodificacion.W.airports.get(actual);
        if (aeropuertoOrigen==null) return 0;

        final int salidaMinima = tiempoActualUTC + MIN_TURN_MIN;

        for (int k=0; k<indicesSalida.length; k++){
            int indiceVuelo = indicesSalida[k];
            Flight vuelo = contextoDecodificacion.W.flights.get(indiceVuelo);
            Airport aeropuertoDestino = contextoDecodificacion.W.airports.get(vuelo.dest);
            if (aeropuertoDestino==null) continue;

            int[] salidaPorDia = contextoDecodificacion.P.depUTC[indiceVuelo];
            int[] llegadaPorDia = contextoDecodificacion.P.arrUTC[indiceVuelo];

            int salidaBase = salidaPorDia[0];
            if (salidaBase > fechaLimite) continue;

            int diaInicio;
            if (salidaMinima <= salidaBase) diaInicio = 0;
            else {
                int delta = salidaMinima - salidaBase;
                diaInicio = delta / 1440;
                if (delta % 1440 != 0) diaInicio++;
            }
            if (diaInicio < 0) diaInicio = 0;

            for (int dia=diaInicio; dia<contextoDecodificacion.horizonDays; dia++){
                int salidaUTC = salidaPorDia[dia];
                if (salidaUTC < salidaMinima) continue;
                if (salidaUTC > fechaLimite) break;

                int llegadaUTC = llegadaPorDia[dia];
                if (llegadaUTC > fechaLimite) continue;

                int usado = contextoDecodificacion.capUsed[indiceVuelo][dia];
                int disponible = vuelo.capacity - usado;
                if (disponible <= 0) continue;

                FlightCandidate candidatoVuelo = new FlightCandidate();
                candidatoVuelo.fi = indiceVuelo; 
                candidatoVuelo.dayIndex = dia; 
                candidatoVuelo.depUTC = salidaUTC; 
                candidatoVuelo.arrUTC = llegadaUTC; 
                candidatoVuelo.avail = disponible;
                contextoDecodificacion.candBuf.add(candidatoVuelo);
            }
        }

        if (contextoDecodificacion.candBuf.size() > TOPK_CANDIDATES) {
            contextoDecodificacion.candBuf.sort(Comparator.comparingInt(candidato -> candidato.arrUTC));
            contextoDecodificacion.candBuf.subList(TOPK_CANDIDATES, contextoDecodificacion.candBuf.size()).clear();
        }
        return contextoDecodificacion.candBuf.size();
    }

    static FlightCandidate seleccionarPorPrioridad(List<FlightCandidate> candidatos, SelectContext contexto){
        if (candidatos.isEmpty()) return null;
        int llegadaMinima = Integer.MAX_VALUE;
        for (int i=0;i<candidatos.size();i++){
            int valor = candidatos.get(i).arrUTC;
            if (valor<llegadaMinima) llegadaMinima = valor;
        }
        double mejorPuntuacion = -1e18;
        FlightCandidate mejor = null;
        for (int i=0;i<candidatos.size();i++){
            FlightCandidate candidato = candidatos.get(i);
            Flight vuelo = contexto.W.flights.get(candidato.fi);
            int indice = contexto.P.flightIndex.get(vuelo);
            double clave = contexto.chrom.keys[indice];

            double gananciaHoras = -((candidato.arrUTC - llegadaMinima) / 60.0);

            Double distanciaOrigen = contexto.distToDestByAp.get(vuelo.orig);
            Double distanciaDestino = contexto.distToDestByAp.get(vuelo.dest);
            double progreso = 0.0;
            if (distanciaOrigen!=null && distanciaDestino!=null) progreso = distanciaOrigen - distanciaDestino;

            double bonusDirecto = vuelo.dest.equals(contexto.destTarget) ? contexto.wDirect : 0.0;
            double bonusDosSaltos = contexto.hasDirectToTarget.contains(vuelo.dest) ? contexto.wTwoHop : 0.0;

            double puntuacion = contexto.wKey*clave + contexto.wEarly*gananciaHoras + contexto.wGeo*progreso
                         + bonusDirecto + bonusDosSaltos;

            if (puntuacion > mejorPuntuacion){ mejorPuntuacion=puntuacion; mejor=candidato; }
        }
        return mejor;
    }

    static int calcularMaximoAjusteAlmacenamiento(StockTracker rastreadorStock, String aeropuerto, int inicioSlot, int finSlot, int cantidadMaxima) {
        if (cantidadMaxima <= 0) return 0;
        if (inicioSlot >= finSlot) return cantidadMaxima;
        int minimo = 0, maximo = cantidadMaxima;
        while (minimo < maximo) {
            int medio = (minimo + maximo + 1) >>> 1;
            if (rastreadorStock.canFit(aeropuerto, inicioSlot, finSlot, medio)) minimo = medio;
            else maximo = medio - 1;
        }
        return minimo;
    }

    static Map<String,Double> obtenerDistanciasADestino(DecodeContext contextoDecodificacion, String destino){
        return contextoDecodificacion.distCache.computeIfAbsent(destino, clave -> {
            Map<String,Double> mapa = new HashMap<>(contextoDecodificacion.W.airports.size());
            Airport aeropuertoObjetivo = contextoDecodificacion.W.airports.get(destino);
            if (aeropuertoObjetivo != null){
                for (Airport aeropuerto : contextoDecodificacion.W.airports.values()){
                    double distancia = haversineKm(aeropuerto.lat, aeropuerto.lon, aeropuertoObjetivo.lat, aeropuertoObjetivo.lon);
                    mapa.put(aeropuerto.code, distancia);
                }
            }
            return mapa;
        });
    }

    static Set<String> obtenerOrigenesDirectosADestino(DecodeContext contextoDecodificacion, String destino){
        return contextoDecodificacion.directCache.computeIfAbsent(destino, clave -> {
            Set<String> origenes = new HashSet<>();
            for (Map.Entry<String,List<Flight>> entrada : contextoDecodificacion.W.outByAirport.entrySet()){
                for (Flight vuelo : entrada.getValue()){
                    if (vuelo.dest.equals(destino)){
                        origenes.add(entrada.getKey());
                        break;
                    }
                }
            }
            return origenes;
        });
    }

    // ===================== MÉTODO MAIN PARA EJECUCIÓN STANDALONE =====================
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     ALGORITMO GENÉTICO - OPTIMIZACIÓN DE RUTAS LOGÍSTICAS     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Ruta base para los archivos de datos
            String rutaBase = "src/main/resources/datos/";
            
            System.out.println("📂 Cargando datos desde: " + rutaBase);
            System.out.println();

            // 1. Cargar aeropuertos
            System.out.print("   [1/3] Cargando aeropuertos... ");
            List<Aeropuerto> aeropuertos = cargarAeropuertos(rutaBase + "Aeropuertos.txt");
            System.out.println("✓ " + aeropuertos.size() + " aeropuertos cargados");

            // 2. Cargar planes de vuelo
            System.out.print("   [2/3] Cargando planes de vuelo... ");
            List<PlanDeVuelo> planesDeVuelo = cargarPlanesDeVuelo(rutaBase + "PlanesDeVuelo.txt");
            System.out.println("✓ " + planesDeVuelo.size() + " vuelos cargados");

            // 3. Cargar pedidos
            System.out.print("   [3/3] Cargando pedidos... ");
            List<Pedido> pedidos = cargarPedidos(rutaBase + "Pedidos.txt");
            System.out.println("✓ " + pedidos.size() + " pedidos cargados");

            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();

            // 4. Inicializar y ejecutar algoritmo
            System.out.println("🧬 Inicializando algoritmo genético...");
            genetico algoritmo = new genetico();
            algoritmo.inicializar(pedidos, planesDeVuelo, aeropuertos);

            System.out.println();
            System.out.println("🚀 Ejecutando optimización...");
            System.out.println("   • Población: " + POP_SIZE + " individuos");
            System.out.println("   • Generaciones máximas: " + MAX_GEN);
            System.out.println("   • Tasa de cruce: " + (PCROSS*100) + "%");
            System.out.println("   • Tasa de mutación: " + (PMUT*100) + "%");
            System.out.println();

            long inicio = System.currentTimeMillis();
            Solution solucion = algoritmo.ejecutar();
            long duracion = System.currentTimeMillis() - inicio;

            // 5. Mostrar resultados
            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();
            System.out.println("✅ OPTIMIZACIÓN COMPLETADA");
            System.out.println();
            System.out.println("📊 RESULTADOS:");
            System.out.println("   ├─ Fitness: " + String.format("%.4f", solucion.objective));
            System.out.println("   ├─ Pedidos a tiempo: " + solucion.servedOnTime);
            System.out.println("   ├─ Pedidos tarde: " + solucion.servedLate);
            System.out.println("   ├─ Violaciones capacidad: " + solucion.capViol);
            System.out.println("   ├─ Holgura promedio: " + String.format("%.2f", (double)solucion.avgSlack) + " min");
            System.out.println("   └─ Tiempo ejecución: " + duracion + " ms");
            System.out.println();

            // 6. Estadísticas adicionales
            double tasaExito = (solucion.servedOnTime * 100.0) / (solucion.servedOnTime + solucion.servedLate);
            System.out.println("📈 ESTADÍSTICAS:");
            System.out.println("   ├─ Tasa de éxito: " + String.format("%.2f%%", tasaExito));
            System.out.println("   ├─ Total servidos: " + (solucion.servedOnTime + solucion.servedLate));
            
            if (solucion.capViol == 0) {
                System.out.println("   └─ Estado capacidad: ✓ Sin violaciones");
            } else {
                System.out.println("   └─ Estado capacidad: ⚠ " + solucion.capViol + " violaciones");
            }
            
            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (IOException e) {
            System.err.println();
            System.err.println("❌ ERROR: No se pudieron cargar los archivos de datos");
            System.err.println("   Detalle: " + e.getMessage());
            System.err.println();
            System.err.println("   Asegúrate de que los archivos existan en:");
            System.err.println("   • src/main/resources/datos/Aeropuertos.txt");
            System.err.println("   • src/main/resources/datos/PlanesDeVuelo.txt");
            System.err.println("   • src/main/resources/datos/Pedidos.txt");
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ ERROR DURANTE LA EJECUCIÓN:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ===================== MÉTODOS DE CARGA DE DATOS =====================

    private static List<Aeropuerto> cargarAeropuertos(String rutaArchivo) throws IOException {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                
                // Ignorar líneas de encabezado y vacías
                if (linea.isEmpty() || linea.startsWith("PDDS") || linea.startsWith("*") || 
                    linea.contains("America") || linea.contains("Europa") || 
                    linea.contains("Asia") || linea.contains("Africa") || linea.contains("GMT")) {
                    continue;
                }

                try {
                    // Formato: 01   SKBO   Bogota   Colombia   bogo   -5   430   Latitude: 04° 42' 05" N   Longitude:  74° 08' 49" W
                    String[] partes = linea.split("\\s+");
                    
                    if (partes.length >= 7) {
                        String codigo = partes[1];
                        String ciudad = partes[2];
                        int gmtOffset = Integer.parseInt(partes[5]);
                        int capacidad = Integer.parseInt(partes[6]);
                        
                        // Extraer latitud y longitud
                        double latitud = extraerCoordenada(linea, "Latitude:");
                        double longitud = extraerCoordenada(linea, "Longitude:");
                        
                        Aeropuerto aeropuerto = new Aeropuerto();
                        aeropuerto.setCodigo(codigo);
                        aeropuerto.setCiudad(ciudad);
                        aeropuerto.setLatitud(latitud);
                        aeropuerto.setLongitud(longitud);
                        aeropuerto.setZonaHoraria(gmtOffset);
                        aeropuerto.setCapacidadAlmacenamiento(capacidad);
                        
                        aeropuertos.add(aeropuerto);
                    }
                } catch (Exception e) {
                    // Ignorar líneas con formato incorrecto
                    System.err.println("   ⚠ Advertencia: línea ignorada en aeropuertos: " + linea);
                }
            }
        }
        
        return aeropuertos;
    }

    private static double extraerCoordenada(String linea, String palabra) {
        try {
            int inicio = linea.indexOf(palabra);
            if (inicio == -1) return 0.0;
            
            String resto = linea.substring(inicio + palabra.length()).trim();
            String[] partes = resto.split("\\s+");
            
            if (partes.length >= 4) {
                int grados = Integer.parseInt(partes[0].replace("°", ""));
                int minutos = Integer.parseInt(partes[1].replace("'", ""));
                int segundos = Integer.parseInt(partes[2].replace("\"", ""));
                String direccion = partes[3];
                
                double coordenada = grados + (minutos / 60.0) + (segundos / 3600.0);
                
                if (direccion.equals("S") || direccion.equals("W")) {
                    coordenada = -coordenada;
                }
                
                return coordenada;
            }
        } catch (Exception e) {
            // Coordenada por defecto
        }
        return 0.0;
    }

    private static List<PlanDeVuelo> cargarPlanesDeVuelo(String rutaArchivo) throws IOException {
        List<PlanDeVuelo> vuelos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                
                if (linea.isEmpty()) continue;

                try {
                    // Formato: SKBO-SEQM-03:34-05:21-0300
                    String[] partes = linea.split("-");
                    
                    if (partes.length == 5) {
                        String origen = partes[0];
                        String destino = partes[1];
                        LocalTime horaSalida = LocalTime.parse(partes[2]);
                        LocalTime horaLlegada = LocalTime.parse(partes[3]);
                        int capacidad = Integer.parseInt(partes[4]);
                        
                        PlanDeVuelo vuelo = new PlanDeVuelo();
                        vuelo.setAeropuertoOrigen(origen);
                        vuelo.setAeropuertoDestino(destino);
                        vuelo.setHoraSalida(horaSalida);
                        vuelo.setHoraLlegada(horaLlegada);
                        vuelo.setCapacidad(capacidad);
                        
                        vuelos.add(vuelo);
                    }
                } catch (Exception e) {
                    System.err.println("   ⚠ Advertencia: línea ignorada en vuelos: " + linea);
                }
            }
        }
        
        return vuelos;
    }

    private static List<Pedido> cargarPedidos(String rutaArchivo) throws IOException {
        List<Pedido> pedidos = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                
                if (linea.isEmpty()) continue;

                try {
                    // Formato: 17-15-47-SKBO-324-0000015
                    String[] partes = linea.split("-");
                    
                    if (partes.length == 6) {
                        int dia = Integer.parseInt(partes[0]);
                        int hora = Integer.parseInt(partes[1]);
                        int minuto = Integer.parseInt(partes[2]);
                        String aeropuertoDestino = partes[3];
                        int cantidadProductos = Integer.parseInt(partes[4]);
                        String clienteId = partes[5];
                        
                        Pedido pedido = new Pedido();
                        pedido.setDia(dia);
                        pedido.setHora(hora);
                        pedido.setMinuto(minuto);
                        pedido.setAeropuertoDestino(aeropuertoDestino);
                        pedido.setCantidadProductos(cantidadProductos);
                        pedido.setClienteId(clienteId);
                        
                        pedidos.add(pedido);
                    }
                } catch (Exception e) {
                    System.err.println("   ⚠ Advertencia: línea ignorada en pedidos: " + linea);
                }
            }
        }
        
        return pedidos;
    }
}
