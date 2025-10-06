package morapack.planificacion;

import morapack.modelo.Vuelo;
import java.util.ArrayList;
import java.util.List;

/**
 * Versión mínima de RutaCompleta para el módulo genético.
 * Permite almacenar la secuencia de vuelos y etiquetar el tipo de ruta.
 */
public class RutaCompleta {
    private final List<Vuelo> vuelos = new ArrayList<>();
    private String tipoRuta;
    private final List<String> escalas = new ArrayList<>();

    public void agregarVuelo(Vuelo vuelo) { if (vuelo != null) vuelos.add(vuelo); }
    public boolean esViable() { return !vuelos.isEmpty(); }

    public String obtenerDescripcion() {
        if (vuelos.isEmpty()) return "SIN RUTA";
        if ("DIRECTO".equals(tipoRuta) && vuelos.size()==1) {
            Vuelo v = vuelos.get(0);
            return String.format("DIRECTO %s→%s %s-%s", v.getOrigen(), v.getDestino(), v.getHoraSalida(), v.getHoraLlegada());
        }
        StringBuilder sb = new StringBuilder(tipoRuta==null?"RUTA":tipoRuta).append(": ");
        for (int i=0;i<vuelos.size();i++) {
            Vuelo v = vuelos.get(i);
            if (i>0) sb.append(" → ");
            sb.append(v.getOrigen());
            if (i==vuelos.size()-1) sb.append(" → ").append(v.getDestino());
        }
        return sb.toString();
    }

    public List<Vuelo> getVuelos() { return vuelos; }
    public String getTipoRuta() { return tipoRuta; }
    public void setTipoRuta(String tipoRuta) { this.tipoRuta = tipoRuta; }
    public List<String> getEscalas() { return escalas; }
    public void setEscalas(List<String> esc) { escalas.clear(); if (esc!=null) escalas.addAll(esc); }

    // Added method
    public int calcularTiempoTotal() {
        if (vuelos.size()<1) return Integer.MAX_VALUE;
        try {
            Vuelo primero = vuelos.get(0); Vuelo ultimo = vuelos.get(vuelos.size()-1);
            int s = parse(primero.getHoraSalida()); int l = parse(ultimo.getHoraLlegada());
            return l - s;
        } catch(Exception e){ return Integer.MAX_VALUE; }
    }
    private int parse(String hhmm){ if(hhmm==null) return 0; String[] p=hhmm.split(":"); return Integer.parseInt(p[0])*60 + (p.length>1?Integer.parseInt(p[1]):0);} 
}
