package morapack.planificacion;

import morapack.modelo.Pedido;
import morapack.modelo.Vuelo;
import java.util.*;

/**
 * Stub simplificado del PlanificadorAvanzadoEscalas para permitir compilación.
 * Implementa sólo las firmas usadas por el algoritmo genético. Estrategia: buscar primer vuelo directo disponible.
 */
public class PlanificadorAvanzadoEscalas {
    private final Map<String,List<Vuelo>> vuelosPorOrigen = new HashMap<>();

    public PlanificadorAvanzadoEscalas(List<Vuelo> vuelos) {
        for (Vuelo v: vuelos) vuelosPorOrigen.computeIfAbsent(v.getOrigen(),k->new ArrayList<>()).add(v);
    }

    public RutaCompleta planificarRuta(String sedeOrigen, String destino, int cantidad) { return planificarRuta(sedeOrigen,destino,cantidad,0); }

    public RutaCompleta planificarRuta(String sedeOrigen, String destino, int cantidad, int tiempoMinimoPedido) {
        List<Vuelo> lista = vuelosPorOrigen.get(sedeOrigen); if (lista==null) return null;
        for (Vuelo v: lista) if (v.getDestino().equals(destino)) { RutaCompleta r=new RutaCompleta(); r.agregarVuelo(v); r.setTipoRuta("DIRECTO"); return r; }
        return null; // sin escalas en stub
    }

    public static int calcularTiempoMinimoPedido(Pedido pedido) { return pedido.getHora()*60 + pedido.getMinuto() + 30; }
    public void reiniciarCapacidades() { }
    public Map<String,Integer> getEstadisticasCapacidad() { return Collections.emptyMap(); }
}
