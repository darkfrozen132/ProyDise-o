// GENETICO - Algoritmo Genético para Optimización de Rutas Logísticas
// Versión en español basada en prueba.java
//
// ALGORITMO GENÉTICO con Decodificador Greedy
// - GA con cromosoma: double[] keys (1 valor por vuelo plantilla)
// - Decoder: construye rutas hub→destino con heurística multi-criterio
// - Control de capacidad: vuelos (2D array) + almacenes (difference arrays)
// - Heurística: prioriza vuelos directos, progreso geográfico y llegadas tempranas
//
// CARACTERÍSTICAS:
// - Carga de pedidos mensuales: dd-hh-mm-dest-###-IdClien (UTC)
// - Evita revisitas con Set<visited>
// - Mantiene ocupación de almacén destino hasta 2h después de completar pedido
// - Fitness: 1.0*onTime - 1.5*late - 4.0*capViol + 0.001*slack
//
// EJECUTAR: java genetico.java
// ARCHIVOS REQUERIDOS: Aeropuertos.txt, PlanesDeVuelo.txt, Pedidos.txt

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

public class genetico {

    // ===================== Parámetros de negocio =====================
    static final int MIN_TURN_MIN = 30;               // conexión mínima
    static final int DUE_SAME_MIN = 2 * 24 * 60;      // 2 días
    static final int DUE_CROSS_MIN = 3 * 24 * 60;     // 3 días
    static final int PICKUP_WINDOW_MIN = 120;         // 2 horas de recojo

    // ===================== Parámetros GA =============================
    static final int POP_SIZE = 50;
    static final int MAX_GEN  = 200;
    static final double PCROSS = 0.8;
    static final double PMUT   = 0.05;
    static final int ELITE_K   = 4;
    static final int TOURN_K   = 3;
    static final int NO_IMPROV_LIMIT = 40;

    // Objetivo
    static final double LAMBDA_ONTIME = 1.0;
    static final double LAMBDA_LATE   = 1.5;
    static final double LAMBDA_CAPVIO = 4.0;
    static final double LAMBDA_SLACK  = 0.001;

    // Exportadores
    static final Set<String> EXPORT_HUBS = Set.of(
        "SPIM",  // Lima
        "EBCI",  // Bruselas
        "UBBB"   // Bakú
    );

    // Stock/Almacén: granularidad de slots para difference arrays
    static final int SLOT_MIN = 60; // 60 minutos

    // Recorte opcional de branching
    static final int TOPK_CANDIDATES = 2000;

    static final Path LOG_FILE = Paths.get("logs/genetico-ag.log");
    static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final Path REPORT_FILE = Paths.get("logs/genetico-rutas.txt");

    private static final Pattern LATITUDE_DMS = Pattern.compile(
        "Latitude:\\s*([0-9]{1,3})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*([NS])",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LONGITUDE_DMS = Pattern.compile(
        "Longitude:\\s*([0-9]{1,3})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*([EW])",
        Pattern.CASE_INSENSITIVE
    );

    // ===================== Modelos de datos ==========================
    enum Continente { AMERICA_SUR, EUROPA, ASIA, OTRO }

    static class Aeropuerto {
        String codigo;
        int gmt;                // horas offset UTC (puede ser negativo)
        int capacidadAlmacen;   // capacidad de almacén
        double lat, lon;        // grados decimales
        Continente continente;
        boolean esExportador;

        public Aeropuerto(String codigo, int gmt, int capacidadAlmacen, double lat, double lon) {
            this.codigo = codigo;
            this.gmt = gmt;
            this.capacidadAlmacen = capacidadAlmacen;
            this.lat = lat;
            this.lon = lon;
            this.continente = inferirContinente(codigo);
            this.esExportador = EXPORT_HUBS.contains(codigo);
        }
    }

    static Continente inferirContinente(String icao) {
        if (icao == null || icao.isEmpty()) return Continente.OTRO;
        char c = icao.charAt(0);
        if (c == 'S') return Continente.AMERICA_SUR;
        if (c == 'E' || c == 'L' || c == 'U') return Continente.EUROPA;
        if (c == 'O' || c == 'V') return Continente.ASIA;
        return Continente.OTRO;
    }

    static class Vuelo {
        String origen, destino;     // ICAO
        int salidaLocalMin;         // minutos desde 00:00 (hora local en ORIGEN)
        int llegadaLocalMin;        // minutos desde 00:00 (hora local en DESTINO)
        int capacidad;
        public Vuelo(String o, String d, int salida, int llegada, int cap) {
            origen=o; destino=d; salidaLocalMin=salida; llegadaLocalMin=llegada; capacidad=cap;
        }
    }

    // Órdenes SIN origen (el algoritmo elige el hub)
    static class Pedido {
        String destino;
        int cantidad;
        int liberacionMinUTC;
        // datos del nuevo formato
        int diaDelMes;          // 1..31
        String idCliente;       // 7 dígitos zero-padded

        public Pedido(String destino, int cantidad, int liberacionMinUTC, int diaDelMes, String idCliente) {
            this.destino = destino; this.cantidad = cantidad; this.liberacionMinUTC = liberacionMinUTC;
            this.diaDelMes = diaDelMes; this.idCliente = idCliente;
        }
    }

    // Uso concreto de un vuelo plantilla en un día específico
    static class UsoVuelo {
        Vuelo vuelo;
        int indiceDia;
        int salidaUTC, llegadaUTC;
        int cantidadAsignada;
        public UsoVuelo(Vuelo v, int d, int salidaUTC, int llegadaUTC, int cant) {
            this.vuelo=v; this.indiceDia=d; this.salidaUTC=salidaUTC; this.llegadaUTC=llegadaUTC; this.cantidadAsignada=cant;
        }
    }

    // Una subruta mueve un BLOQUE fijo de cantidad desde un hub hasta el destino
    static class SubRuta {
        String hubOrigen;
        List<UsoVuelo> tramos = new ArrayList<>(8);
        int cantidad;
        int llegadaUTC;
    }

    static class Solucion {
        Map<Pedido, List<SubRuta>> rutas = new HashMap<>();
        Map<String,Integer> capacidadUsada = new HashMap<>(); // (vuelo,dia) -> usado
        int servidosATiempo, servidosTarde, violacionesCap, holguraPromedio;
        double objetivo;
    }

    static class Mundo {
        Map<String,Aeropuerto> aeropuertos = new HashMap<>();
        List<Vuelo> vuelos = new ArrayList<>();
        Map<String,List<Vuelo>> salidasPorAeropuerto = new HashMap<>();
        List<String> listaHubs = new ArrayList<>();
    }

    // ===================== Tiempo / Conversión =======================
    static int parseHHMM(String hhmm) {
        String[] p = hhmm.trim().split(":");
        return Integer.parseInt(p[0])*60 + Integer.parseInt(p[1]);
    }
    static int aUTCDesdeLocal(Aeropuerto a, int minLocal) { return minLocal - a.gmt*60; }
    static String mmAHHMM(int m) {
        int x = m % 1440; if (x<0) x+=1440;
        return String.format("%02d:%02d", x/60, x%60);
    }
    static int slotDe(int utcMin, int numSlots){
        int s = utcMin / SLOT_MIN;
        if (s<0) return 0;
        if (s>=numSlots) return numSlots-1;
        return s;
    }

    static void registrarInfo(String tag, String message) {
        try {
            Path parent = LOG_FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            String line = String.format("%s [%s] %s%n",
                    LocalDateTime.now().format(LOG_TS), tag, message);
            Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir log: " + e.getMessage());
        }
    }

    static double dmsToDecimal(int deg, int min, int sec, char hemi){
        double val = deg + min / 60.0 + sec / 3600.0;
        char h = Character.toUpperCase(hemi);
        if (h == 'S' || h == 'W') val = -val;
        return val;
    }

    static double dmsToDecimal(String deg, String min, String sec, String hemi){
        int d = Integer.parseInt(deg);
        int m = Integer.parseInt(min);
        int s = Integer.parseInt(sec);
        char h = (hemi != null && !hemi.isEmpty()) ? hemi.charAt(0) : 'N';
        return dmsToDecimal(d, m, s, h);
    }

    static void escribirReporteRutas(Solucion sol, List<Pedido> orders, Path file){
        if (sol == null) return;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write("Reporte de rutas MoraPack GA\n");
                bw.write("Generado: " + LocalDateTime.now().format(LOG_TS) + "\n\n");
                for (Pedido o : orders) {
                    List<SubRuta> lst = sol.rutas.get(o);
                    int delivered = (lst==null)?0:lst.stream().mapToInt(s->s.cantidad).sum();
                    bw.write(String.format("Pedido destino %s qty=%d releaseUTC=%s delivered=%d day=%d client=%s%n",
                            o.destino, o.cantidad, mmAHHMM(o.liberacionMinUTC), delivered, o.diaDelMes,
                            o.idCliente==null?"":o.idCliente));
                    if (lst != null) {
                        int idx = 1;
                        for (SubRuta sr : lst) {
                            bw.write(String.format("  Subruta #%d hub=%s qty=%d arrivalUTC=%s%n",
                                    idx++, sr.hubOrigen, sr.cantidad, mmAHHMM(sr.llegadaUTC)));
                            for (UsoVuelo fu : sr.tramos) {
                                bw.write(String.format("    %s->%s D%d depUTC=%s arrUTC=%s qty=%d%n",
                                        fu.vuelo.origen, fu.vuelo.destino, fu.indiceDia,
                                        mmAHHMM(fu.salidaUTC), mmAHHMM(fu.llegadaUTC), fu.cantidadAsignada));
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
            registrarInfo("REPORT", "Reporte escrito en " + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo escribir el reporte: " + e.getMessage());
            registrarInfo("REPORT", "Error escribiendo reporte: " + e.getMessage());
        }
    }

    // ===================== Lectura robusta de archivos ===============
    static List<String> leerTodasLineasAuto(Path p) throws IOException {
        List<Charset> tries = List.of(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, Charset.forName("windows-1252"));
        for (Charset cs: tries) {
            try (BufferedReader br = Files.newBufferedReader(p, cs)) {
                List<String> out = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    if (!out.isEmpty() || !line.isEmpty()) {
                        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') line = line.substring(1);
                    }
                    out.add(line);
                }
                return out;
            } catch (MalformedInputException ignore) {}
        }
        throw new IOException("No se pudo decodificar el archivo con UTF-8/ISO-8859-1/Windows-1252");
    }

    // airports: CODE, GMT (-12..14), CAP (100..10000), opcional lat lon
    static void cargarAeropuertos(Path file, Mundo W) throws IOException {
        registrarInfo("AIRPORT", "Iniciando carga desde " + file.toAbsolutePath());
        int loaded = 0;
        for (String s: leerTodasLineasAuto(file)) {
            String line = s.trim();
            if (line.isEmpty()) continue;

            Double lat = null, lon = null;
            Matcher latMatcher = LATITUDE_DMS.matcher(s);
            if (latMatcher.find()) {
                lat = dmsToDecimal(latMatcher.group(1), latMatcher.group(2), latMatcher.group(3), latMatcher.group(4));
            }
            Matcher lonMatcher = LONGITUDE_DMS.matcher(s);
            if (lonMatcher.find()) {
                lon = dmsToDecimal(lonMatcher.group(1), lonMatcher.group(2), lonMatcher.group(3), lonMatcher.group(4));
            }

            String[] tok = line.split("\\s+|,");
            String code = null; Integer gmt=null, cap=null;
            for (String t: tok) {
                if (code==null) {
                    if (t.matches("[A-Z]{4}")) { code = t; }
                    continue;
                }
                if (gmt==null && t.matches("[+-]?\\d{1,2}")) {
                    int v = Integer.parseInt(t); if (v>=-12 && v<=14) { gmt=v; continue; }
                }
                if (cap==null && t.matches("\\d{2,9}")) {
                    int v = Integer.parseInt(t); if (v>=100 && v<=100000000) { cap=v; continue; }
                }
                if (lat==null && t.matches("[+-]?\\d+(\\.\\d+)?")) { lat = Double.parseDouble(t); continue; }
                if (lon==null && t.matches("[+-]?\\d+(\\.\\d+)?")) { lon = Double.parseDouble(t); continue; }
            }
            if (code!=null && gmt!=null && cap!=null) {
                if (lat==null) lat = 0.0; if (lon==null) lon = 0.0;
                Aeropuerto airport = new Aeropuerto(code, gmt, cap, lat, lon);
                W.aeropuertos.put(code, airport);
                loaded++;
                registrarInfo("AIRPORT", "Loaded " + code + " GMT=" + gmt + " CAP=" + cap
                        + " LAT=" + lat + " LON=" + lon);
            }
        }
        registrarInfo("AIRPORT", "Total cargados: " + loaded);
        for (String h: EXPORT_HUBS) if (W.aeropuertos.containsKey(h)) W.listaHubs.add(h);
    }

    // flights: ORIG-DEST-HH:MM-HH:MM-CAP
    static void cargarVuelos(Path file, Mundo W) throws IOException {
        registrarInfo("FLIGHT", "Iniciando carga desde " + file.toAbsolutePath());
        int loaded = 0;
        for (String s: leerTodasLineasAuto(file)) {
            String line = s.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("-");
            if (p.length < 5) continue;
            String orig = p[0].trim();
            String dest = p[1].trim();
            int dep = parseHHMM(p[2].trim());
            int arr = parseHHMM(p[3].trim());
            int cap = Integer.parseInt(p[4].trim());
            Vuelo f = new Vuelo(orig, dest, dep, arr, cap);
            W.vuelos.add(f);
            W.salidasPorAeropuerto.computeIfAbsent(orig, k->new ArrayList<>()).add(f);
            loaded++;
            registrarInfo("FLIGHT", String.format("Loaded %s->%s dep=%s arr=%s cap=%d",
                    orig, dest, p[2].trim(), p[3].trim(), cap));
        }
        W.salidasPorAeropuerto.values().forEach(lst -> lst.sort(Comparator.comparingInt(fl->fl.salidaLocalMin)));
        registrarInfo("FLIGHT", "Total cargados: " + loaded);
    }

    // ===================== GA: Cromosoma =============================
    static class Cromosoma {
        double[] claves; // una clave por vuelo plantilla
        Cromosoma(int n) { claves = new double[n]; }
        Cromosoma copy(){ Cromosoma c=new Cromosoma(claves.length); System.arraycopy(claves,0,c.claves,0,claves.length); return c; }
    }
    static Cromosoma randomChromosome(int n, Random rnd){
        Cromosoma c=new Cromosoma(n);
        for(int i=0;i<n;i++) c.claves[i]=rnd.nextDouble();
        return c;
    }

    // ===================== Haversine / progreso ======================
    static double haversinoKm(double lat1,double lon1,double lat2,double lon2){
        double R=6371.0;
        double dLat=Math.toRadians(lat2-lat1);
        double dLon=Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    // ===================== Precomputación de tiempos/capacidades =====
    static class Precalculo {
        final int[][] salidaUTC;             // [fi][d]
        final int[][] llegadaUTC;             // [fi][d]
        final Map<Vuelo,Integer> flightIndex = new HashMap<>();
        final Map<String,int[]> outIdxByAirport = new HashMap<>();

        Precalculo(int numFlights, int horizonDays){
            salidaUTC = new int[numFlights][horizonDays];
            llegadaUTC = new int[numFlights][horizonDays];
        }
    }

    static Precalculo precalcular(Mundo W, int horizonDays){
        int n = W.vuelos.size();
        Precalculo P = new Precalculo(n, horizonDays);

        for (int i=0;i<n;i++) P.flightIndex.put(W.vuelos.get(i), i);

        for (int fi=0; fi<n; fi++){
            Vuelo f = W.vuelos.get(fi);
            Aeropuerto aO = W.aeropuertos.get(f.origen), aD = W.aeropuertos.get(f.destino);
            int baseDep = aUTCDesdeLocal(aO, f.salidaLocalMin);
            int baseArr = aUTCDesdeLocal(aD, f.llegadaLocalMin);
            for (int d=0; d<horizonDays; d++){
                int dep = baseDep + d*1440;
                int arr = baseArr + d*1440;
                if (arr<dep) arr += 1440;
                P.salidaUTC[fi][d]=dep;
                P.llegadaUTC[fi][d]=arr;
            }
        }
        for (Map.Entry<String,List<Vuelo>> e: W.salidasPorAeropuerto.entrySet()){
            String ap = e.getKey();
            List<Vuelo> lst = e.getValue();
            int[] idx = new int[lst.size()];
            for (int i=0;i<idx.length;i++) idx[i] = P.flightIndex.get(lst.get(i));
            P.outIdxByAirport.put(ap, idx);
        }
        return P;
    }

    // ===================== Stock por intervalos (difference arrays) ==
    static class RastreadorStock {
        static class Store {
            final int[] delta;
            final int[] pref;
            boolean dirty = true;
            Store(int n){ delta=new int[n+1]; pref=new int[n+1]; }
        }
        final int numSlots;
        final Mundo W;
        final Map<String,Store> stores = new HashMap<>();

        RastreadorStock(Mundo W, int horizonDays){
            this.W = W;
            this.numSlots = (horizonDays*1440)/SLOT_MIN + 5;
        }
        Store get(String ap){
            return stores.computeIfAbsent(ap, k->new Store(numSlots));
        }
        int capacityOf(Aeropuerto a){
            if (a==null) return Integer.MAX_VALUE;
            return a.esExportador ? Integer.MAX_VALUE : a.capacidadAlmacen;
        }
        boolean canFit(String ap, int slotStart, int slotEnd, int qty){
            if (slotStart>=slotEnd || qty<=0) return true;
            Aeropuerto a=W.aeropuertos.get(ap);
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

    // ===================== Selección de vuelos (score heurístico) ====
    static class VueloCandidato {
        int fi, indiceDia, salidaUTC, llegadaUTC;
        int avail;
    }

    static class PasoTramo {
        int flightIndex;
        int indiceDia;
        int salidaUTC;
        int llegadaUTC;
        String waitAirport;
        int waitStartSlot;
        int waitEndSlot;
        boolean requiresStorage;
    }

    static int maximoAjusteAlmacen(RastreadorStock stock, String airport, int slotStart, int slotEnd, int maxQty) {
        if (maxQty <= 0) return 0;
        if (slotStart >= slotEnd) return maxQty;
        int lo = 0, hi = maxQty;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (stock.canFit(airport, slotStart, slotEnd, mid)) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    static class ContextoSeleccion {
        Mundo W;
        Precalculo P;
        Cromosoma chrom;
        String destTarget;
        double wKey = 1.0, wEarly = 0.1, wGeo = 0.05; // pesos existentes
        // pesos para “más directo”
        double wDirect = 50.0;   // gran bonus si dest == target
        double wTwoHop = 5.0;    // bonus si hay vuelo directo desde next -> target
        Map<String,Double> distToDestByAp = Collections.emptyMap();
        // aeropuertos con vuelo directo hacia el destino
        Set<String> hasDirectToTarget = Collections.emptySet();
    }

    static VueloCandidato seleccionarPorPrioridad(List<VueloCandidato> cand, ContextoSeleccion ctx){
        if (cand.isEmpty()) return null;
        int minArr = Integer.MAX_VALUE;
        for (int i=0;i<cand.size();i++){
            int v = cand.get(i).llegadaUTC;
            if (v<minArr) minArr = v;
        }
        double bestScore = -1e18;
        VueloCandidato best = null;
        for (int i=0;i<cand.size();i++){
            VueloCandidato c = cand.get(i);
            Vuelo f = ctx.W.vuelos.get(c.fi);
            int idx = ctx.P.flightIndex.get(f);
            double key = ctx.chrom.claves[idx];

            double timeGainHours = -((c.llegadaUTC - minArr) / 60.0);

            Double db = ctx.distToDestByAp.get(f.origen);
            Double da = ctx.distToDestByAp.get(f.destino);
            double progress = 0.0;
            if (db!=null && da!=null) progress = db - da;

            // bonus por “más directo”
            double directBonus = f.destino.equals(ctx.destTarget) ? ctx.wDirect : 0.0;
            double twoHopBonus = ctx.hasDirectToTarget.contains(f.destino) ? ctx.wTwoHop : 0.0;

            double score = ctx.wKey*key + ctx.wEarly*timeGainHours + ctx.wGeo*progress
                         + directBonus + twoHopBonus;

            if (score > bestScore){ bestScore=score; best=c; }
        }
        return best;
    }

    // ===================== Decoder (elige hub por subruta) ===========
    static class ContextoDecodificacion {
        Mundo W;
        int horizonDays;
        Map<String,Integer> capUsedMap;
        Precalculo P;
        RastreadorStock stock;
        Cromosoma chrom;
        Random rnd;
        int numSlots;
        List<VueloCandidato> candBuf = new ArrayList<>(64);
        int[][] capacidadUsada;
        Map<String, Map<String,Double>> distCache = new HashMap<>();
        Map<String, Set<String>> directCache = new HashMap<>();
    }

    static String fkey(Vuelo f, int d){ return f.origen+">"+f.destino+"@D"+d+"#"+f.salidaLocalMin; }

    static int computeDueForHub(Mundo W, String hub, String dest, int releaseMinUTC){
        Aeropuerto ah = W.aeropuertos.get(hub);
        Aeropuerto ad = W.aeropuertos.get(dest);
        boolean same = (ah!=null && ad!=null && ah.continente==ad.continente);
        return releaseMinUTC + (same ? DUE_SAME_MIN : DUE_CROSS_MIN) + PICKUP_WINDOW_MIN;
    }

    static List<String> hubs(Mundo W){
        return W.listaHubs;
    }

    static int enumerarCandidatos(ContextoDecodificacion dc, String current, int tNowUTC, int dueLimit, int neededQty){
        dc.candBuf.clear();
        int[] outIdx = dc.P.outIdxByAirport.getOrDefault(current, null);
        if (outIdx==null) return 0;

        Aeropuerto aOrig = dc.W.aeropuertos.get(current);
        if (aOrig==null) return 0;

        final int minDeparture = tNowUTC + MIN_TURN_MIN;

        for (int k=0; k<outIdx.length; k++){
            int fi = outIdx[k];
            Vuelo f = dc.W.vuelos.get(fi);
            Aeropuerto aDest = dc.W.aeropuertos.get(f.destino);
            if (aDest==null) continue;

            int[] depPerDay = dc.P.salidaUTC[fi];
            int[] arrPerDay = dc.P.llegadaUTC[fi];

            int baseDep = depPerDay[0];
            if (baseDep > dueLimit) continue;

            int dStart;
            if (minDeparture <= baseDep) dStart = 0;
            else {
                int delta = minDeparture - baseDep;
                dStart = delta / 1440;
                if (delta % 1440 != 0) dStart++;
            }
            if (dStart < 0) dStart = 0;

            for (int d=dStart; d<dc.horizonDays; d++){
                int depUTC = depPerDay[d];
                if (depUTC < minDeparture) continue;
                if (depUTC > dueLimit) break;

                int arrUTC = arrPerDay[d];
                if (arrUTC > dueLimit) continue;

                int used = dc.capacidadUsada[fi][d];
                int avail = f.capacidad - used;
                if (avail <= 0) continue;

                VueloCandidato fc = new VueloCandidato();
                fc.fi = fi; fc.indiceDia = d; fc.salidaUTC = depUTC; fc.llegadaUTC = arrUTC; fc.avail = avail;
                dc.candBuf.add(fc);
            }
        }

        if (dc.candBuf.size() > TOPK_CANDIDATES) {
            dc.candBuf.sort(Comparator.comparingInt(c -> c.llegadaUTC));
            dc.candBuf.subList(TOPK_CANDIDATES, dc.candBuf.size()).clear();
        }
        return dc.candBuf.size();
    }

    static Map<String,Double> distancesToDest(ContextoDecodificacion dc, String dest){
        return dc.distCache.computeIfAbsent(dest, key -> {
            Map<String,Double> map = new HashMap<>(dc.W.aeropuertos.size());
            Aeropuerto target = dc.W.aeropuertos.get(dest);
            if (target != null){
                for (Aeropuerto a : dc.W.aeropuertos.values()){
                    double d = haversinoKm(a.lat, a.lon, target.lat, target.lon);
                    map.put(a.codigo, d);
                }
            }
            return map;
        });
    }

    static Set<String> directOriginsToDest(ContextoDecodificacion dc, String dest){
        return dc.directCache.computeIfAbsent(dest, key -> {
            Set<String> origins = new HashSet<>();
            for (Map.Entry<String,List<Vuelo>> e : dc.W.salidasPorAeropuerto.entrySet()){
                for (Vuelo f : e.getValue()){
                    if (f.destino.equals(dest)){
                        origins.add(e.getKey());
                        break;
                    }
                }
            }
            return origins;
        });
    }

    // Construye UNA subruta completa desde un hub hasta el destino
    static SubRuta construirSubrutaDesdeHub(ContextoDecodificacion dc, Pedido o, String hub, int dueLimit, int blockQty){
        if (blockQty <= 0) return null;

        String current = hub;
        int tNow = o.liberacionMinUTC;
        int requestQty = blockQty;

        ContextoSeleccion sctx = new ContextoSeleccion();
        sctx.W = dc.W; sctx.P = dc.P; sctx.chrom = dc.chrom; sctx.destTarget = o.destino;
        sctx.distToDestByAp = distancesToDest(dc, o.destino);
        sctx.hasDirectToTarget = directOriginsToDest(dc, o.destino);

        int expansions = 0, maxExp = 2000;
        boolean firstLeg = true;
        Set<String> visited = new HashSet<>();
        visited.add(hub);

        List<PasoTramo> plan = new ArrayList<>(8);
        int pathCapacity = requestQty;
        int lastArrivalUTC = -1;

        while (!current.equals(o.destino) && expansions++ < maxExp) {
            int neededQty = Math.max(1, pathCapacity);
            int cc = enumerarCandidatos(dc, current, tNow, dueLimit, neededQty);
            if (cc==0) return null;

            dc.candBuf.removeIf(c -> {
                String next = dc.W.vuelos.get(c.fi).destino;
                return visited.contains(next);
            });
            if (dc.candBuf.isEmpty()) return null;

            VueloCandidato chosen = null;
            while (true) {
                VueloCandidato best = seleccionarPorPrioridad(dc.candBuf, sctx);
                if (best == null) return null;

                Vuelo flight = dc.W.vuelos.get(best.fi);
                int legAvail = best.avail;
                if (legAvail <= 0) {
                    dc.candBuf.remove(best);
                    if (dc.candBuf.isEmpty()) return null;
                    continue;
                }

                int proposedCapacity = Math.min(pathCapacity, legAvail);
                if (proposedCapacity <= 0) {
                    dc.candBuf.remove(best);
                    if (dc.candBuf.isEmpty()) return null;
                    continue;
                }

                String waitAirport = null;
                int waitStartSlot = 0;
                int waitEndSlot = 0;
                boolean requiresStorage = false;

                Aeropuerto apCur = dc.W.aeropuertos.get(current);
                boolean curIsHub = (apCur!=null && apCur.esExportador);
                if (!firstLeg && !curIsHub) {
                    waitAirport = current;
                    waitStartSlot = slotDe(tNow, dc.numSlots);
                    waitEndSlot = slotDe(best.salidaUTC, dc.numSlots);
                    if (waitStartSlot < waitEndSlot) {
                        int fit = maximoAjusteAlmacen(dc.stock, waitAirport, waitStartSlot, waitEndSlot, proposedCapacity);
                        if (fit <= 0) {
                            dc.candBuf.remove(best);
                            if (dc.candBuf.isEmpty()) return null;
                            continue;
                        }
                        proposedCapacity = Math.min(proposedCapacity, fit);
                        requiresStorage = true;
                    }
                }

                pathCapacity = proposedCapacity;

                PasoTramo step = new PasoTramo();
                step.flightIndex = best.fi;
                step.indiceDia = best.indiceDia;
                step.salidaUTC = best.salidaUTC;
                step.llegadaUTC = best.llegadaUTC;
                step.waitAirport = waitAirport;
                step.waitStartSlot = waitStartSlot;
                step.waitEndSlot = waitEndSlot;
                step.requiresStorage = requiresStorage;
                plan.add(step);

                current = flight.destino;
                tNow = best.llegadaUTC;
                lastArrivalUTC = tNow;
                firstLeg = false;
                visited.add(current);
                chosen = best;
                break;
            }

            if (chosen == null) return null;
        }

        if (!current.equals(o.destino)) return null;
        if (plan.isEmpty()) return null;
        if (pathCapacity <= 0) return null;

        int finalQty = Math.min(pathCapacity, requestQty);
        if (finalQty <= 0) return null;

        int destStartSlot = slotDe(lastArrivalUTC, dc.numSlots);
        int destEndSlot = slotDe(lastArrivalUTC + PICKUP_WINDOW_MIN, dc.numSlots);
        int destFit = maximoAjusteAlmacen(dc.stock, o.destino, destStartSlot, destEndSlot, finalQty);
        if (destFit <= 0) return null;
        finalQty = Math.min(finalQty, destFit);
        if (finalQty <= 0) return null;

        SubRuta sr = new SubRuta();
        sr.hubOrigen = hub;
        sr.cantidad = finalQty;
        sr.llegadaUTC = lastArrivalUTC;

        for (PasoTramo step : plan) {
            Vuelo flight = dc.W.vuelos.get(step.flightIndex);
            if (step.requiresStorage) {
                dc.stock.addInterval(step.waitAirport, step.waitStartSlot, step.waitEndSlot, finalQty);
            }

            int used = dc.capacidadUsada[step.flightIndex][step.indiceDia];
            dc.capacidadUsada[step.flightIndex][step.indiceDia] = used + finalQty;

            String key = fkey(flight, step.indiceDia);
            int usedMap = dc.capUsedMap.getOrDefault(key, 0);
            dc.capUsedMap.put(key, usedMap + finalQty);

            sr.tramos.add(new UsoVuelo(flight, step.indiceDia, step.salidaUTC, step.llegadaUTC, finalQty));
        }

        dc.stock.addInterval(o.destino, destStartSlot, destEndSlot, finalQty);

        return sr;
    }

    // estructura para llevar las reservas de destino de un pedido y poder extenderlas
    static class ReservaDestino {
        int startSlot;
        int endSlot;
        int cantidad;
        ReservaDestino(int s, int e, int q){ startSlot=s; endSlot=e; cantidad=q; }
    }

    static Solucion decode(Mundo W, List<Pedido> orders, Cromosoma chrom, int horizonDays, long seed){
        Precalculo precomputed = precalcular(W, horizonDays);
        List<Pedido> ordSorted = new ArrayList<>(orders);
        ordSorted.sort(Comparator.comparingInt(o->o.liberacionMinUTC));
        return decodificarOrdenado(W, ordSorted, chrom, horizonDays, seed, precomputed);
    }

    static Solucion decode(Mundo W, List<Pedido> orders, Cromosoma chrom, int horizonDays, long seed, Precalculo precomputed){
        List<Pedido> ordSorted = new ArrayList<>(orders);
        ordSorted.sort(Comparator.comparingInt(o->o.liberacionMinUTC));
        return decodificarOrdenado(W, ordSorted, chrom, horizonDays, seed, precomputed);
    }

    static Solucion decodificarOrdenado(Mundo W, List<Pedido> ordSorted, Cromosoma chrom, int horizonDays, long seed, Precalculo precomputed){
        Solucion sol = new Solucion();
        sol.capacidadUsada = new HashMap<>();
        RastreadorStock stock = new RastreadorStock(W, horizonDays);
        Precalculo P = precomputed;

        ContextoDecodificacion dc = new ContextoDecodificacion();
        dc.W=W; dc.horizonDays=horizonDays; dc.capUsedMap=sol.capacidadUsada; dc.P=P; dc.stock=stock; dc.chrom=chrom; dc.rnd=new Random(seed);
        dc.numSlots = (horizonDays*1440)/SLOT_MIN + 5;
        dc.capacidadUsada = new int[W.vuelos.size()][horizonDays];

        int onTime=0, late=0, viol=0; long slackSum=0; int slackCnt=0;

        for (Pedido o: ordSorted){
            int remaining = o.cantidad;
            List<SubRuta> subroutes = new ArrayList<>(4);

            // reservas de destino para poder extenderlas cuando cambie el “último arribo”
            List<ReservaDestino> destHolds = new ArrayList<>();
            int lastArrival = -1;

            int guard=0, guardMax=500;
            while (remaining>0 && guard++<guardMax){
                SubRuta bestSr = null;
                int bestArr = Integer.MAX_VALUE;
                int requestQty = remaining;

                for (String hub: hubs(W)) {
                    int due = computeDueForHub(W, hub, o.destino, o.liberacionMinUTC);
                    SubRuta sr = construirSubrutaDesdeHub(dc, o, hub, due, requestQty);
                    if (sr != null && sr.cantidad > 0 && sr.llegadaUTC < bestArr) {
                        bestArr = sr.llegadaUTC;
                        bestSr = sr;
                    }
                }

                if (bestSr == null) break;

                // registrar la reserva mínima que ya hizo buildSubroute...
                int start = slotDe(bestSr.llegadaUTC, dc.numSlots);
                int end   = slotDe(bestSr.llegadaUTC + PICKUP_WINDOW_MIN, dc.numSlots);
                destHolds.add(new ReservaDestino(start, end, bestSr.cantidad));

                subroutes.add(bestSr);
                remaining -= bestSr.cantidad;

                // actualizar “último arribo” y EXTENDER todas las reservas de destino hasta (last+120)
                if (bestSr.llegadaUTC > lastArrival) {
                    int newLast = bestSr.llegadaUTC;
                    int newEnd = slotDe(newLast + PICKUP_WINDOW_MIN, dc.numSlots);
                    for (ReservaDestino r: destHolds){
                        if (r.endSlot < newEnd){
                            // verificar sólo el tramo adicional [r.endSlot, newEnd)
                            if (stock.canFit(o.destino, r.endSlot, newEnd, r.cantidad)) {
                                stock.addInterval(o.destino, r.endSlot, newEnd, r.cantidad);
                                r.endSlot = newEnd;
                            } else {
                                // no cabe la extensión -> contamos violación (penaliza objetivo)
                                viol++;
                            }
                        }
                    }
                    lastArrival = newLast;
                }
            }

            sol.rutas.put(o, subroutes);

            int delivered = 0;
            for (int i=0;i<subroutes.size();i++) delivered += subroutes.get(i).cantidad;

            if (delivered < o.cantidad) {
                late++;
            } else {
                SubRuta crit = null;
                int maxArr = Integer.MIN_VALUE;
                for (int i=0;i<subroutes.size();i++){
                    SubRuta s = subroutes.get(i);
                    if (s.llegadaUTC > maxArr){ maxArr = s.llegadaUTC; crit = s; }
                }
                if (crit != null){
                    int dueCrit = computeDueForHub(W, crit.hubOrigen, o.destino, o.liberacionMinUTC);
                    if (crit.llegadaUTC <= dueCrit) {
                        onTime++;
                        slackSum += (dueCrit - crit.llegadaUTC);
                        slackCnt++;
                    } else late++;
                } else late++;
            }
        }

        sol.servidosATiempo = onTime; sol.servidosTarde = late; sol.violacionesCap = viol;
        sol.holguraPromedio = (slackCnt==0)?0:(int)(slackSum/slackCnt);
        sol.objetivo = LAMBDA_ONTIME*onTime - LAMBDA_LATE*late - LAMBDA_CAPVIO*viol + LAMBDA_SLACK*sol.holguraPromedio;
        return sol;
    }

    // ===================== GA Core ==================================
    static double aptitud(Mundo W, List<Pedido> ordersSorted, Cromosoma c, int horizonDays, Precalculo precomputed){
        return decodificarOrdenado(W, ordersSorted, c, horizonDays, 12345L, precomputed).objetivo;
    }

    static Cromosoma cruzar(Cromosoma a, Cromosoma b, Random rnd){
        if (rnd.nextDouble()>PCROSS) return rnd.nextBoolean()?a.copy():b.copy();
        Cromosoma c=new Cromosoma(a.claves.length);
        for (int i=0;i<a.claves.length;i++) c.claves[i]=(rnd.nextBoolean()?a.claves[i]:b.claves[i]);
        return c;
    }

    static void mutar(Cromosoma c, Random rnd){
        for (int i=0;i<c.claves.length;i++){
            if (rnd.nextDouble()<PMUT){
                double v = c.claves[i] + rnd.nextGaussian()*0.1;
                c.claves[i] = (v<0.0)?0.0:((v>1.0)?1.0:v);
            }
        }
    }

    static class Puntuado {
        Cromosoma c;
        double fit;
        Puntuado(Cromosoma c, double fit){ this.c=c; this.fit=fit; }
    }

    static Solucion ejecutarAG(Mundo W, List<Pedido> orders, int horizonDays, long seed){
        Random rnd = new Random(seed);
        List<Cromosoma> pop = new ArrayList<>(POP_SIZE);
        for (int i=0;i<POP_SIZE;i++) pop.add(randomChromosome(W.vuelos.size(), rnd));

        List<Pedido> ordersSortedMutable = new ArrayList<>(orders);
        ordersSortedMutable.sort(Comparator.comparingInt(o->o.liberacionMinUTC));
        List<Pedido> ordersSorted = Collections.unmodifiableList(ordersSortedMutable);

        Precalculo precomputed = precalcular(W, horizonDays);
        Cromosoma best=null; double bestFit=-1e18; int stall=0;

        for (int gen=1; gen<=MAX_GEN; gen++){
            List<Puntuado> scored = new ArrayList<>(POP_SIZE);
            for (int i=0;i<pop.size();i++){
                Cromosoma c = pop.get(i);
                scored.add(new Puntuado(c, aptitud(W, ordersSorted, c, horizonDays, precomputed)));
            }

            scored.sort((a,b)->Double.compare(b.fit, a.fit));
            List<Cromosoma> next = new ArrayList<>(POP_SIZE);
            for (int i=0;i<ELITE_K;i++) next.add(scored.get(i).c.copy());

            while (next.size()<POP_SIZE){
                Cromosoma p1 = scored.get(rnd.nextInt(scored.size())).c;
                Cromosoma p2 = scored.get(rnd.nextInt(scored.size())).c;
                Cromosoma ch = cruzar(p1,p2,rnd);
                mutar(ch,rnd);
                next.add(ch);
            }
            pop = next;

            Puntuado iterBest = scored.get(0);
            double iterFit = iterBest.fit;

            if (iterFit > bestFit){ bestFit=iterFit; best=iterBest.c.copy(); stall=0; }
            else stall++;

            if (stall>=NO_IMPROV_LIMIT) break;
        }
        return decodificarOrdenado(W, ordersSorted, best, horizonDays, seed, precomputed);
    }

    // ===================== Carga de órdenes ==========================

    // NEW: dd-hh-mm-dest-###-IdClien   (UTC)
    static List<Pedido> cargarPedidosMensuales(Path file, Mundo W) throws IOException {
        List<Pedido> L = new ArrayList<>();
        int lineNo = 0;
        registrarInfo("ORDER", "Iniciando carga desde " + file.toAbsolutePath());
        for (String s: leerTodasLineasAuto(file)){
            lineNo++;
            String line = s.trim(); if (line.isEmpty()) continue;
            String[] p = line.split("-");
            if (p.length < 6) {
                // formato inválido -> ignorar
                continue;
            }
            try {
                int dd = Integer.parseInt(p[0]);
                int hh = Integer.parseInt(p[1]);
                int mm = Integer.parseInt(p[2]);
                String dest = p[3].trim();
                String qtyStr = p[4].trim();
                String idRaw = p[5].trim();

                if (dd<1 || dd>31) continue;
                if (hh<0 || hh>23) continue;
                if (mm<0 || mm>59) continue;
                if (!dest.matches("[A-Z]{4}")) continue;
                if (!W.aeropuertos.containsKey(dest)) continue;

                int qty = Integer.parseInt(qtyStr);
                // normalizar clientId a 7 dígitos
                String clientId = String.format("%07d", Integer.parseInt(idRaw));

                int releaseMinUTC = (dd-1)*1440 + hh*60 + mm;

                L.add(new Pedido(dest, qty, releaseMinUTC, dd, clientId));
                registrarInfo("ORDER", String.format("Loaded day=%d %s qty=%d release=%02d:%02d client=%s",
                        dd, dest, qty, hh, mm, clientId));
            } catch (Exception ignore) {
                // ignora línea mal formada
            }
        }
        registrarInfo("ORDER", "Total cargados: " + L.size());
        // IMPORTANTE: el horizonte debe cubrir los días usados.
        return L;
    }

    // ===================== Main (CLI) ================================
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║     ALGORITMO GENÉTICO - OPTIMIZACIÓN RUTAS LOGÍSTICAS        ║");
        System.out.println("║                   (Versión en Español)                         ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        Path airportsFile = Paths.get("Aeropuertos.txt");
        Path flightsFile  = Paths.get("PlanesDeVuelo.txt");
        Path ordersFile   = Paths.get("Pedidos.txt");

        System.out.println("📂 Cargando datos...");
        Mundo W = new Mundo();
        cargarAeropuertos(airportsFile, W);
        cargarVuelos(flightsFile, W);
        List<Pedido> orders = cargarPedidosMensuales(ordersFile, W);

        System.out.println("   ✓ Aeropuertos: " + W.aeropuertos.size());
        System.out.println("   ✓ Vuelos: " + W.vuelos.size());
        System.out.println("   ✓ Pedidos: " + orders.size());
        System.out.println();

        if (orders.isEmpty()) {
            System.err.println("⚠ ERROR: No se cargaron pedidos. Verifica el archivo Pedidos.txt");
            return;
        }

        int horizonDays = 31; // 31 días para dataset mensual
        long seed = 10260475L;

        System.out.println("🧬 Parámetros del Algoritmo Genético:");
        System.out.println("   • Población: " + POP_SIZE + " individuos");
        System.out.println("   • Generaciones máximas: " + MAX_GEN);
        System.out.println("   • Tasa de cruce: " + (PCROSS*100) + "%");
        System.out.println("   • Tasa de mutación: " + (PMUT*100) + "%");
        System.out.println("   • Elitismo: " + ELITE_K + " mejores");
        System.out.println("   • Horizonte temporal: " + horizonDays + " días");
        System.out.println();

        System.out.println("🚀 Ejecutando optimización...");
        System.out.println();

        long inicio = System.currentTimeMillis();
        Solucion best = ejecutarAG(W, orders, horizonDays, seed);
        long duracion = System.currentTimeMillis() - inicio;

        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("✅ OPTIMIZACIÓN COMPLETADA");
        System.out.println();
        System.out.println("📊 RESULTADOS FINALES:");
        System.out.println("   ├─ Fitness (objetivo): " + String.format("%.4f", best.objetivo));
        System.out.println("   ├─ Pedidos a tiempo: " + best.servidosATiempo);
        System.out.println("   ├─ Pedidos tarde: " + best.servidosTarde);
        System.out.println("   ├─ Violaciones de capacidad: " + best.violacionesCap);
        System.out.println("   ├─ Holgura promedio: " + best.holguraPromedio + " minutos");
        System.out.println("   └─ Tiempo de ejecución: " + String.format("%.2f", duracion/1000.0) + " segundos");
        System.out.println();

        int deliveredOrders = best.servidosATiempo + best.servidosTarde;
        double pctOnTime = deliveredOrders == 0 ? 0.0 : (best.servidosATiempo * 100.0) / deliveredOrders;
        
        System.out.println("📈 ESTADÍSTICAS:");
        System.out.println("   ├─ Total pedidos procesados: " + orders.size());
        System.out.println("   ├─ Total pedidos entregados: " + deliveredOrders);
        System.out.println("   ├─ Tasa de cumplimiento: " + String.format("%.2f%%", pctOnTime));
        
        if (best.violacionesCap == 0) {
            System.out.println("   └─ Estado capacidad: ✓ Sin violaciones");
        } else {
            System.out.println("   └─ Estado capacidad: ⚠ " + best.violacionesCap + " violaciones");
        }
        
        System.out.println();
        System.out.println("📄 Reporte detallado guardado en:");
        System.out.println("   " + REPORT_FILE.toAbsolutePath());
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        registrarInfo("RESULT", String.format(
                "Pedidos Entregados=%d OnTime=%d Late=%d PctOnTime=%.2f Fitness=%.4f Tiempo=%.2fs",
                deliveredOrders, best.servidosATiempo, best.servidosTarde, pctOnTime, best.objetivo, duracion/1000.0));
        escribirReporteRutas(best, orders, REPORT_FILE);
    }
}
