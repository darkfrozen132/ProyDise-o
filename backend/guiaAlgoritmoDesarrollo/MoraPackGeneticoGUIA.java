// MoraPackGAMensual.java
// GA con decodificador (órdenes sin origen): elige hub (entre EXPORT_HUBS) por subruta.
// Reglas: conexión mínima, due 2/3 días, ventana de recojo 2h, escalas, split entre subrutas.
// Control de capacidad de vuelo y de almacén por intervalos (difference array por slots).
// Mejoras:
// - Carga de pedidos mensuales dd-hh-mm-dest-###-IdClien (UTC).
// - Heurística: prioriza vuelos directos y “dos saltos” + evita revisitas.
// - Almacén destino: mantener ocupación hasta 2h después de COMPLETAR el pedido.

import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

public class MoraPackGAMensualNoBlock {

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

    static final Path LOG_FILE = Paths.get("logs/morapack-ga.log");
    static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final Path REPORT_FILE = Paths.get("logs/morapack-routes.txt");

    private static final Pattern LATITUDE_DMS = Pattern.compile(
        "Latitude:\\s*([0-9]{1,3})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*([NS])",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LONGITUDE_DMS = Pattern.compile(
        "Longitude:\\s*([0-9]{1,3})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*([EW])",
        Pattern.CASE_INSENSITIVE
    );

    // ===================== Modelos de datos ==========================
    enum Continent { SOUTH_AMERICA, EUROPE, ASIA, OTHER }

    static class Airport {
        String code;
        int gmt;            // horas offset UTC (puede ser negativo)
        int storageCap;     // capacidad de almacén
        double lat, lon;    // grados decimales
        Continent continent;
        boolean isExporter;

        public Airport(String code, int gmt, int storageCap, double lat, double lon) {
            this.code = code;
            this.gmt = gmt;
            this.storageCap = storageCap;
            this.lat = lat;
            this.lon = lon;
            this.continent = inferContinent(code);
            this.isExporter = EXPORT_HUBS.contains(code);
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
        String orig, dest;     // ICAO
        int depLocalMin;       // minutos desde 00:00 (hora local en ORIG)
        int arrLocalMin;       // minutos desde 00:00 (hora local en DEST)
        int capacity;
        public Flight(String o, String d, int dep, int arr, int cap) {
            orig=o; dest=d; depLocalMin=dep; arrLocalMin=arr; capacity=cap;
        }
    }

    // Órdenes SIN origen (el algoritmo elige el hub)
    static class Order {
        String dest;
        int qty;
        int releaseMinUTC;
        //datos del nuevo formato
        int dayOfMonth;          // 1..31
        String clientId;         // 7 dígitos zero-padded

        public Order(String dest, int qty, int releaseMinUTC, int dayOfMonth, String clientId) {
            this.dest = dest; this.qty = qty; this.releaseMinUTC = releaseMinUTC;
            this.dayOfMonth = dayOfMonth; this.clientId = clientId;
        }
    }

    // Uso concreto de un vuelo plantilla en un día específico
    static class FlightUse {
        Flight flight;
        int dayIndex;
        int depUTC, arrUTC;
        int qtyAssigned;
        public FlightUse(Flight f, int d, int depUTC, int arrUTC, int qty) {
            this.flight=f; this.dayIndex=d; this.depUTC=depUTC; this.arrUTC=arrUTC; this.qtyAssigned=qty;
        }
    }

    // Una subruta mueve un BLOQUE fijo de cantidad desde un hub hasta el destino
    static class SubRoute {
        String originHub;
        List<FlightUse> legs = new ArrayList<>(8);
        int qty;
        int arrivalUTC;
    }

    static class Solution {
        Map<Order, List<SubRoute>> routes = new HashMap<>();
        Map<String,Integer> capUsed = new HashMap<>(); // (flight,day) -> used
        int servedOnTime, servedLate, capViol, avgSlack;
        double objective;
    }

    static class World {
        Map<String,Airport> airports = new HashMap<>();
        List<Flight> flights = new ArrayList<>();
        Map<String,List<Flight>> outByAirport = new HashMap<>();
        List<String> hubList = new ArrayList<>();
    }

    // ===================== Tiempo / Conversión =======================
    static int parseHHMM(String hhmm) {
        String[] p = hhmm.trim().split(":");
        return Integer.parseInt(p[0])*60 + Integer.parseInt(p[1]);
    }
    static int toUTCFromLocal(Airport a, int localMin) { return localMin - a.gmt*60; }
    static String mmToHHMM(int m) {
        int x = m % 1440; if (x<0) x+=1440;
        return String.format("%02d:%02d", x/60, x%60);
    }
    static int slotOf(int utcMin, int numSlots){
        int s = utcMin / SLOT_MIN;
        if (s<0) return 0;
        if (s>=numSlots) return numSlots-1;
        return s;
    }

    static void logInfo(String tag, String message) {
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

    static void writeRoutesReport(Solution sol, List<Order> orders, Path file){
        if (sol == null) return;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write("Reporte de rutas MoraPack GA\n");
                bw.write("Generado: " + LocalDateTime.now().format(LOG_TS) + "\n\n");
                for (Order o : orders) {
                    List<SubRoute> lst = sol.routes.get(o);
                    int delivered = (lst==null)?0:lst.stream().mapToInt(s->s.qty).sum();
                    bw.write(String.format("Pedido destino %s qty=%d releaseUTC=%s delivered=%d day=%d client=%s%n",
                            o.dest, o.qty, mmToHHMM(o.releaseMinUTC), delivered, o.dayOfMonth,
                            o.clientId==null?"":o.clientId));
                    if (lst != null) {
                        int idx = 1;
                        for (SubRoute sr : lst) {
                            bw.write(String.format("  Subruta #%d hub=%s qty=%d arrivalUTC=%s%n",
                                    idx++, sr.originHub, sr.qty, mmToHHMM(sr.arrivalUTC)));
                            for (FlightUse fu : sr.legs) {
                                bw.write(String.format("    %s->%s D%d depUTC=%s arrUTC=%s qty=%d%n",
                                        fu.flight.orig, fu.flight.dest, fu.dayIndex,
                                        mmToHHMM(fu.depUTC), mmToHHMM(fu.arrUTC), fu.qtyAssigned));
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
            logInfo("REPORT", "Reporte escrito en " + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo escribir el reporte: " + e.getMessage());
            logInfo("REPORT", "Error escribiendo reporte: " + e.getMessage());
        }
    }

    // ===================== Lectura robusta de archivos ===============
    static List<String> readAllLinesAuto(Path p) throws IOException {
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
    static void loadAirports(Path file, World W) throws IOException {
        logInfo("AIRPORT", "Iniciando carga desde " + file.toAbsolutePath());
        int loaded = 0;
        for (String s: readAllLinesAuto(file)) {
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
                Airport airport = new Airport(code, gmt, cap, lat, lon);
                W.airports.put(code, airport);
                loaded++;
                logInfo("AIRPORT", "Loaded " + code + " GMT=" + gmt + " CAP=" + cap
                        + " LAT=" + lat + " LON=" + lon);
            }
        }
        logInfo("AIRPORT", "Total cargados: " + loaded);
        for (String h: EXPORT_HUBS) if (W.airports.containsKey(h)) W.hubList.add(h);
    }

    // flights: ORIG-DEST-HH:MM-HH:MM-CAP
    static void loadFlights(Path file, World W) throws IOException {
        logInfo("FLIGHT", "Iniciando carga desde " + file.toAbsolutePath());
        int loaded = 0;
        for (String s: readAllLinesAuto(file)) {
            String line = s.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("-");
            if (p.length < 5) continue;
            String orig = p[0].trim();
            String dest = p[1].trim();
            int dep = parseHHMM(p[2].trim());
            int arr = parseHHMM(p[3].trim());
            int cap = Integer.parseInt(p[4].trim());
            Flight f = new Flight(orig, dest, dep, arr, cap);
            W.flights.add(f);
            W.outByAirport.computeIfAbsent(orig, k->new ArrayList<>()).add(f);
            loaded++;
            logInfo("FLIGHT", String.format("Loaded %s->%s dep=%s arr=%s cap=%d",
                    orig, dest, p[2].trim(), p[3].trim(), cap));
        }
        W.outByAirport.values().forEach(lst -> lst.sort(Comparator.comparingInt(fl->fl.depLocalMin)));
        logInfo("FLIGHT", "Total cargados: " + loaded);
    }

    // ===================== GA: Cromosoma =============================
    static class Chromosome {
        double[] keys; // una key por vuelo plantilla
        Chromosome(int n) { keys = new double[n]; }
        Chromosome copy(){ Chromosome c=new Chromosome(keys.length); System.arraycopy(keys,0,c.keys,0,keys.length); return c; }
    }
    static Chromosome randomChromosome(int n, Random rnd){
        Chromosome c=new Chromosome(n);
        for(int i=0;i<n;i++) c.keys[i]=rnd.nextDouble();
        return c;
    }

    // ===================== Haversine / progreso ======================
    static double haversineKm(double lat1,double lon1,double lat2,double lon2){
        double R=6371.0;
        double dLat=Math.toRadians(lat2-lat1);
        double dLon=Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    // ===================== Precomputación de tiempos/capacidades =====
    static class Precomp {
        final int[][] depUTC;             // [fi][d]
        final int[][] arrUTC;             // [fi][d]
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

    // ===================== Stock por intervalos (difference arrays) ==
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

    // ===================== Selección de vuelos (score heurístico) ====
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

    static int maxStorageFit(StockTracker stock, String airport, int slotStart, int slotEnd, int maxQty) {
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

    static class SelectContext {
        World W;
        Precomp P;
        Chromosome chrom;
        String destTarget;
        double wKey = 1.0, wEarly = 0.1, wGeo = 0.05; // pesos existentes
        // pesos para “más directo”
        double wDirect = 50.0;   // gran bonus si dest == target
        double wTwoHop = 5.0;    // bonus si hay vuelo directo desde next -> target
        Map<String,Double> distToDestByAp = Collections.emptyMap();
        // aeropuertos con vuelo directo hacia el destino
        Set<String> hasDirectToTarget = Collections.emptySet();
    }

    static FlightCandidate selectByPriority(List<FlightCandidate> cand, SelectContext ctx){
        if (cand.isEmpty()) return null;
        int minArr = Integer.MAX_VALUE;
        for (int i=0;i<cand.size();i++){
            int v = cand.get(i).arrUTC;
            if (v<minArr) minArr = v;
        }
        double bestScore = -1e18;
        FlightCandidate best = null;
        for (int i=0;i<cand.size();i++){
            FlightCandidate c = cand.get(i);
            Flight f = ctx.W.flights.get(c.fi);
            int idx = ctx.P.flightIndex.get(f);
            double key = ctx.chrom.keys[idx];

            double timeGainHours = -((c.arrUTC - minArr) / 60.0);

            Double db = ctx.distToDestByAp.get(f.orig);
            Double da = ctx.distToDestByAp.get(f.dest);
            double progress = 0.0;
            if (db!=null && da!=null) progress = db - da;

            // bonus por “más directo”
            double directBonus = f.dest.equals(ctx.destTarget) ? ctx.wDirect : 0.0;
            double twoHopBonus = ctx.hasDirectToTarget.contains(f.dest) ? ctx.wTwoHop : 0.0;

            double score = ctx.wKey*key + ctx.wEarly*timeGainHours + ctx.wGeo*progress
                         + directBonus + twoHopBonus;

            if (score > bestScore){ bestScore=score; best=c; }
        }
        return best;
    }

    // ===================== Decoder (elige hub por subruta) ===========
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

    static String fkey(Flight f, int d){ return f.orig+">"+f.dest+"@D"+d+"#"+f.depLocalMin; }

    static int computeDueForHub(World W, String hub, String dest, int releaseMinUTC){
        Airport ah = W.airports.get(hub);
        Airport ad = W.airports.get(dest);
        boolean same = (ah!=null && ad!=null && ah.continent==ad.continent);
        return releaseMinUTC + (same ? DUE_SAME_MIN : DUE_CROSS_MIN) + PICKUP_WINDOW_MIN;
    }

    static List<String> hubs(World W){
        return W.hubList;
    }

    static int enumerateCandidates(DecodeContext dc, String current, int tNowUTC, int dueLimit, int neededQty){
        dc.candBuf.clear();
        int[] outIdx = dc.P.outIdxByAirport.getOrDefault(current, null);
        if (outIdx==null) return 0;

        Airport aOrig = dc.W.airports.get(current);
        if (aOrig==null) return 0;

        final int minDeparture = tNowUTC + MIN_TURN_MIN;

        for (int k=0; k<outIdx.length; k++){
            int fi = outIdx[k];
            Flight f = dc.W.flights.get(fi);
            Airport aDest = dc.W.airports.get(f.dest);
            if (aDest==null) continue;

            int[] depPerDay = dc.P.depUTC[fi];
            int[] arrPerDay = dc.P.arrUTC[fi];

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

                int used = dc.capUsed[fi][d];
                int avail = f.capacity - used;
                if (avail <= 0) continue;

                FlightCandidate fc = new FlightCandidate();
                fc.fi = fi; fc.dayIndex = d; fc.depUTC = depUTC; fc.arrUTC = arrUTC; fc.avail = avail;
                dc.candBuf.add(fc);
            }
        }

        if (dc.candBuf.size() > TOPK_CANDIDATES) {
            dc.candBuf.sort(Comparator.comparingInt(c -> c.arrUTC));
            dc.candBuf.subList(TOPK_CANDIDATES, dc.candBuf.size()).clear();
        }
        return dc.candBuf.size();
    }

    static Map<String,Double> distancesToDest(DecodeContext dc, String dest){
        return dc.distCache.computeIfAbsent(dest, key -> {
            Map<String,Double> map = new HashMap<>(dc.W.airports.size());
            Airport target = dc.W.airports.get(dest);
            if (target != null){
                for (Airport a : dc.W.airports.values()){
                    double d = haversineKm(a.lat, a.lon, target.lat, target.lon);
                    map.put(a.code, d);
                }
            }
            return map;
        });
    }

    static Set<String> directOriginsToDest(DecodeContext dc, String dest){
        return dc.directCache.computeIfAbsent(dest, key -> {
            Set<String> origins = new HashSet<>();
            for (Map.Entry<String,List<Flight>> e : dc.W.outByAirport.entrySet()){
                for (Flight f : e.getValue()){
                    if (f.dest.equals(dest)){
                        origins.add(e.getKey());
                        break;
                    }
                }
            }
            return origins;
        });
    }

    // Construye UNA subruta completa desde un hub hasta el destino
    static SubRoute buildSubrouteFromHub(DecodeContext dc, Order o, String hub, int dueLimit, int blockQty){
        if (blockQty <= 0) return null;

        String current = hub;
        int tNow = o.releaseMinUTC;
        int requestQty = blockQty;

        SelectContext sctx = new SelectContext();
        sctx.W = dc.W; sctx.P = dc.P; sctx.chrom = dc.chrom; sctx.destTarget = o.dest;
        sctx.distToDestByAp = distancesToDest(dc, o.dest);
        sctx.hasDirectToTarget = directOriginsToDest(dc, o.dest);

        int expansions = 0, maxExp = 2000;
        boolean firstLeg = true;
        Set<String> visited = new HashSet<>();
        visited.add(hub);

        List<LegStep> plan = new ArrayList<>(8);
        int pathCapacity = requestQty;
        int lastArrivalUTC = -1;

        while (!current.equals(o.dest) && expansions++ < maxExp) {
            int neededQty = Math.max(1, pathCapacity);
            int cc = enumerateCandidates(dc, current, tNow, dueLimit, neededQty);
            if (cc==0) return null;

            dc.candBuf.removeIf(c -> {
                String next = dc.W.flights.get(c.fi).dest;
                return visited.contains(next);
            });
            if (dc.candBuf.isEmpty()) return null;

            FlightCandidate chosen = null;
            while (true) {
                FlightCandidate best = selectByPriority(dc.candBuf, sctx);
                if (best == null) return null;

                Flight flight = dc.W.flights.get(best.fi);
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

                Airport apCur = dc.W.airports.get(current);
                boolean curIsHub = (apCur!=null && apCur.isExporter);
                if (!firstLeg && !curIsHub) {
                    waitAirport = current;
                    waitStartSlot = slotOf(tNow, dc.numSlots);
                    waitEndSlot = slotOf(best.depUTC, dc.numSlots);
                    if (waitStartSlot < waitEndSlot) {
                        int fit = maxStorageFit(dc.stock, waitAirport, waitStartSlot, waitEndSlot, proposedCapacity);
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

                LegStep step = new LegStep();
                step.flightIndex = best.fi;
                step.dayIndex = best.dayIndex;
                step.depUTC = best.depUTC;
                step.arrUTC = best.arrUTC;
                step.waitAirport = waitAirport;
                step.waitStartSlot = waitStartSlot;
                step.waitEndSlot = waitEndSlot;
                step.requiresStorage = requiresStorage;
                plan.add(step);

                current = flight.dest;
                tNow = best.arrUTC;
                lastArrivalUTC = tNow;
                firstLeg = false;
                visited.add(current);
                chosen = best;
                break;
            }

            if (chosen == null) return null;
        }

        if (!current.equals(o.dest)) return null;
        if (plan.isEmpty()) return null;
        if (pathCapacity <= 0) return null;

        int finalQty = Math.min(pathCapacity, requestQty);
        if (finalQty <= 0) return null;

        int destStartSlot = slotOf(lastArrivalUTC, dc.numSlots);
        int destEndSlot = slotOf(lastArrivalUTC + PICKUP_WINDOW_MIN, dc.numSlots);
        int destFit = maxStorageFit(dc.stock, o.dest, destStartSlot, destEndSlot, finalQty);
        if (destFit <= 0) return null;
        finalQty = Math.min(finalQty, destFit);
        if (finalQty <= 0) return null;

        SubRoute sr = new SubRoute();
        sr.originHub = hub;
        sr.qty = finalQty;
        sr.arrivalUTC = lastArrivalUTC;

        for (LegStep step : plan) {
            Flight flight = dc.W.flights.get(step.flightIndex);
            if (step.requiresStorage) {
                dc.stock.addInterval(step.waitAirport, step.waitStartSlot, step.waitEndSlot, finalQty);
            }

            int used = dc.capUsed[step.flightIndex][step.dayIndex];
            dc.capUsed[step.flightIndex][step.dayIndex] = used + finalQty;

            String key = fkey(flight, step.dayIndex);
            int usedMap = dc.capUsedMap.getOrDefault(key, 0);
            dc.capUsedMap.put(key, usedMap + finalQty);

            sr.legs.add(new FlightUse(flight, step.dayIndex, step.depUTC, step.arrUTC, finalQty));
        }

        dc.stock.addInterval(o.dest, destStartSlot, destEndSlot, finalQty);

        return sr;
    }

    // estructura para llevar las reservas de destino de un pedido y poder extenderlas
    static class DestReservation {
        int startSlot;
        int endSlot;
        int qty;
        DestReservation(int s, int e, int q){ startSlot=s; endSlot=e; qty=q; }
    }

    static Solution decode(World W, List<Order> orders, Chromosome chrom, int horizonDays, long seed){
        Precomp precomputed = precompute(W, horizonDays);
        List<Order> ordSorted = new ArrayList<>(orders);
        ordSorted.sort(Comparator.comparingInt(o->o.releaseMinUTC));
        return decodeSorted(W, ordSorted, chrom, horizonDays, seed, precomputed);
    }

    static Solution decode(World W, List<Order> orders, Chromosome chrom, int horizonDays, long seed, Precomp precomputed){
        List<Order> ordSorted = new ArrayList<>(orders);
        ordSorted.sort(Comparator.comparingInt(o->o.releaseMinUTC));
        return decodeSorted(W, ordSorted, chrom, horizonDays, seed, precomputed);
    }

    static Solution decodeSorted(World W, List<Order> ordSorted, Chromosome chrom, int horizonDays, long seed, Precomp precomputed){
        Solution sol = new Solution();
        sol.capUsed = new HashMap<>();
        StockTracker stock = new StockTracker(W, horizonDays);
        Precomp P = precomputed;

        DecodeContext dc = new DecodeContext();
        dc.W=W; dc.horizonDays=horizonDays; dc.capUsedMap=sol.capUsed; dc.P=P; dc.stock=stock; dc.chrom=chrom; dc.rnd=new Random(seed);
        dc.numSlots = (horizonDays*1440)/SLOT_MIN + 5;
        dc.capUsed = new int[W.flights.size()][horizonDays];

        int onTime=0, late=0, viol=0; long slackSum=0; int slackCnt=0;

        for (Order o: ordSorted){
            int remaining = o.qty;
            List<SubRoute> subroutes = new ArrayList<>(4);

            // reservas de destino para poder extenderlas cuando cambie el “último arribo”
            List<DestReservation> destHolds = new ArrayList<>();
            int lastArrival = -1;

            int guard=0, guardMax=500;
            while (remaining>0 && guard++<guardMax){
                SubRoute bestSr = null;
                int bestArr = Integer.MAX_VALUE;
                int requestQty = remaining;

                for (String hub: hubs(W)) {
                    int due = computeDueForHub(W, hub, o.dest, o.releaseMinUTC);
                    SubRoute sr = buildSubrouteFromHub(dc, o, hub, due, requestQty);
                    if (sr != null && sr.qty > 0 && sr.arrivalUTC < bestArr) {
                        bestArr = sr.arrivalUTC;
                        bestSr = sr;
                    }
                }

                if (bestSr == null) break;

                // registrar la reserva mínima que ya hizo buildSubroute...
                int start = slotOf(bestSr.arrivalUTC, dc.numSlots);
                int end   = slotOf(bestSr.arrivalUTC + PICKUP_WINDOW_MIN, dc.numSlots);
                destHolds.add(new DestReservation(start, end, bestSr.qty));

                subroutes.add(bestSr);
                remaining -= bestSr.qty;

                // actualizar “último arribo” y EXTENDER todas las reservas de destino hasta (last+120)
                if (bestSr.arrivalUTC > lastArrival) {
                    int newLast = bestSr.arrivalUTC;
                    int newEnd = slotOf(newLast + PICKUP_WINDOW_MIN, dc.numSlots);
                    for (DestReservation r: destHolds){
                        if (r.endSlot < newEnd){
                            // verificar sólo el tramo adicional [r.endSlot, newEnd)
                            if (stock.canFit(o.dest, r.endSlot, newEnd, r.qty)) {
                                stock.addInterval(o.dest, r.endSlot, newEnd, r.qty);
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

            sol.routes.put(o, subroutes);

            int delivered = 0;
            for (int i=0;i<subroutes.size();i++) delivered += subroutes.get(i).qty;

            if (delivered < o.qty) {
                late++;
            } else {
                SubRoute crit = null;
                int maxArr = Integer.MIN_VALUE;
                for (int i=0;i<subroutes.size();i++){
                    SubRoute s = subroutes.get(i);
                    if (s.arrivalUTC > maxArr){ maxArr = s.arrivalUTC; crit = s; }
                }
                if (crit != null){
                    int dueCrit = computeDueForHub(W, crit.originHub, o.dest, o.releaseMinUTC);
                    if (crit.arrivalUTC <= dueCrit) {
                        onTime++;
                        slackSum += (dueCrit - crit.arrivalUTC);
                        slackCnt++;
                    } else late++;
                } else late++;
            }
        }

        sol.servedOnTime = onTime; sol.servedLate = late; sol.capViol = viol;
        sol.avgSlack = (slackCnt==0)?0:(int)(slackSum/slackCnt);
        sol.objective = LAMBDA_ONTIME*onTime - LAMBDA_LATE*late - LAMBDA_CAPVIO*viol + LAMBDA_SLACK*sol.avgSlack;
        return sol;
    }

    // ===================== GA Core ==================================
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

    static class Scored {
        Chromosome c;
        double fit;
        Scored(Chromosome c, double fit){ this.c=c; this.fit=fit; }
    }

    static Solution runGA(World W, List<Order> orders, int horizonDays, long seed){
        Random rnd = new Random(seed);
        List<Chromosome> pop = new ArrayList<>(POP_SIZE);
        for (int i=0;i<POP_SIZE;i++) pop.add(randomChromosome(W.flights.size(), rnd));

        List<Order> ordersSortedMutable = new ArrayList<>(orders);
        ordersSortedMutable.sort(Comparator.comparingInt(o->o.releaseMinUTC));
        List<Order> ordersSorted = Collections.unmodifiableList(ordersSortedMutable);

        Precomp precomputed = precompute(W, horizonDays);
        Chromosome best=null; double bestFit=-1e18; int stall=0;

        for (int gen=1; gen<=MAX_GEN; gen++){
            List<Scored> scored = new ArrayList<>(POP_SIZE);
            for (int i=0;i<pop.size();i++){
                Chromosome c = pop.get(i);
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

            Scored iterBest = scored.get(0);
            double iterFit = iterBest.fit;

            if (iterFit > bestFit){ bestFit=iterFit; best=iterBest.c.copy(); stall=0; }
            else stall++;

            if (stall>=NO_IMPROV_LIMIT) break;
        }
        return decodeSorted(W, ordersSorted, best, horizonDays, seed, precomputed);
    }

    // ===================== Carga de órdenes ==========================

    // NEW: dd-hh-mm-dest-###-IdClien   (UTC)
    static List<Order> loadOrdersMonthly(Path file, World W) throws IOException {
        List<Order> L = new ArrayList<>();
        int lineNo = 0;
        logInfo("ORDER", "Iniciando carga desde " + file.toAbsolutePath());
        for (String s: readAllLinesAuto(file)){
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
                if (!W.airports.containsKey(dest)) continue;

                int qty = Integer.parseInt(qtyStr);
                // normalizar clientId a 7 dígitos
                String clientId = String.format("%07d", Integer.parseInt(idRaw));

                int releaseMinUTC = (dd-1)*1440 + hh*60 + mm;

                L.add(new Order(dest, qty, releaseMinUTC, dd, clientId));
                logInfo("ORDER", String.format("Loaded day=%d %s qty=%d release=%02d:%02d client=%s",
                        dd, dest, qty, hh, mm, clientId));
            } catch (Exception ignore) {
                // ignora línea mal formada
            }
        }
        logInfo("ORDER", "Total cargados: " + L.size());
        // IMPORTANTE: el horizonte debe cubrir los días usados.
        return L;
    }

    // ===================== Main (CLI) ================================
    public static void main(String[] args) throws Exception {
        Path airportsFile = Paths.get("src/main/java/com/twoalg/common/aeropuertos_exp.txt");
        Path flightsFile  = Paths.get("src/main/java/com/twoalg/common/planes_vuelos.txt");
        Path ordersFile   = Paths.get("src/main/java/com/twoalg/common/ordenes_dia20.txt"); // mensual

        World W = new World();
        loadAirports(airportsFile, W);
        loadFlights(flightsFile, W);

        // NEW: usa el formato mensual
        List<Order> orders = loadOrdersMonthly(ordersFile, W);
        // Si quieres usar el formato antiguo, usa: loadOrdersLegacy(ordersFile, W);

        // NOTA: horizonDays debe ser suficientemente grande para dd usados (ej. 7 días o 31).
        int horizonDays = 31; // 31 por ser un dataset de 1 mes
        long seed = 10260475L;

        Solution best = runGA(W, orders, horizonDays, seed);

        System.out.println("Objetivo: " + best.objective);
        System.out.println("On-time: " + best.servedOnTime + " | Late: " + best.servedLate +
                " | CapViol: " + best.capViol + " | AvgSlack(min): " + best.avgSlack);

        int deliveredOrders = best.servedOnTime + best.servedLate;
        double pctOnTime = deliveredOrders == 0 ? 0.0 : (best.servedOnTime * 100.0) / deliveredOrders;
        logInfo("RESULT", String.format(
                "Pedidos Entregados=%d OnTime=%d Late=%d PctOnTime=%.2f Fitness=%.4f",
                deliveredOrders, best.servedOnTime, best.servedLate, pctOnTime, best.objective));
        writeRoutesReport(best, orders, REPORT_FILE);

        int totalOrders = orders.size();
        System.out.printf("Pedidos entregados: %d/%d (%.2f%% on-time)%n",
                deliveredOrders, totalOrders,
                deliveredOrders==0?0.0:pctOnTime);
        System.out.println("Detalle de rutas exportado a: " + REPORT_FILE.toAbsolutePath());
    }
}
