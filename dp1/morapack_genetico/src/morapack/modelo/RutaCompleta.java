package morapack.modelo;
import java.util.*;

/**
 * Representa una ruta completa - versión compatible con String y Vuelo
 */
public class RutaCompleta {
    private List<String> tramos;
    private List<String> vuelosStr; // Para compatibilidad con código legacy
    private List<String> escalas;
    private String tipoRuta;
    
    public RutaCompleta() {
        this.tramos = new ArrayList<>();
        this.vuelosStr = new ArrayList<>();
        this.escalas = new ArrayList<>();
        this.tipoRuta = "DIRECTO";
    }
    
    // Retorna lista de Vuelo para compatibilidad - crea objetos Vuelo básicos
    public List<Vuelo> getVuelos() {
        List<Vuelo> vuelos = new ArrayList<>();
        for (String vStr : vuelosStr) {
            Vuelo v = new Vuelo();
            vuelos.add(v);
        }
        return vuelos;
    }
    
    // Acepta String
    public void agregarVuelo(String vueloStr) {
        vuelosStr.add(vueloStr);
    }
    
    // Acepta Vuelo  
    public void agregarVuelo(Vuelo vuelo) {
        vuelosStr.add(vuelo.toString());
    }
    
    public boolean esDirecta() {
        return vuelosStr.size() == 1;
    }
    
    public int getNumEscalas() {
        return Math.max(0, vuelosStr.size() - 1);
    }
    
    public List<String> getEscalas() {
        return escalas;
    }
    
    public List<String> getTramos() {
        return tramos;
    }
    
    public void agregarTramo(String tramo) {
        tramos.add(tramo);
    }
    
    public String getTipoRuta() {
        return tipoRuta;
    }
    
    public void setTipoRuta(String tipoRuta) {
        this.tipoRuta = tipoRuta;
    }
    
    public boolean esViable() {
        return !vuelosStr.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Ruta[" + vuelosStr.size() + " vuelos, " + getNumEscalas() + " escalas, " + tipoRuta + "]";
    }
}
