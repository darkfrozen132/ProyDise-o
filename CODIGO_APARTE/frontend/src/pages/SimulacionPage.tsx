import { useState, useMemo, useRef, useEffect } from 'react';
import type { ChangeEvent } from 'react';
import { useSimulacion } from '../hooks/useSimulacion';
import { MapaVuelos } from '../components/mapas/MapaVuelos';
import { Check, AlertTriangle, Play, Pause, XCircle, Package, Plane, Database, Search, X } from 'lucide-react';
import type { OrderRequest } from '../types/orderRequest';
import type { SimulationOrderPlan, SimulationSnapshot } from '../types/simulation';
import { orderService } from '../services/orderService';

// Interfaz extendida para mantener fechas originales del archivo TXT
interface OrderRequestExtended extends OrderRequest {
  fechaOriginal: string;
}

export default function SimulacionPage() {
  const {
      aeropuertos,
      activeSegments,
      kpis,
      reloj,
      isLoading,
      isStarting,
      isError,
      estaActivo,
      estaVisualizando,
      iniciar,
      pausar,
      terminar,
      snapshotFinal,
      snapshotVisible,
      snapshotProgreso,
      vuelosEnMovimiento,
      hasSnapshots,
      tiempoSimulado,
      simSpeed,
      setSimSpeed,
      status,
  } = useSimulacion();

  const [ordenesParaSimular, setOrdenesParaSimular] = useState<OrderRequestExtended[]>([]);
  const [vistaPanel, setVistaPanel] = useState<'envios' | 'vuelos' | 'aeropuertos'>('envios');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [estaSincronizando, setEstaSincronizando] = useState(false);
  const [proyeccionGuardada, setProyeccionGuardada] = useState(false);

  const [searchTerm, setSearchTerm] = useState<string>('');
  const [filtroHub, setFiltroHub] = useState<string>('');
  const [dialogInfo, setDialogInfo] = useState<{ titulo: string; mensaje: string } | null>(null);

  const formatoInputDesdeIso = (iso: string) => iso.slice(0, 16);
  const ensureSeconds = (value?: string) => {
    if (!value) return undefined;
    return value.length === 16 ? `${value}:00` : value;
  };

  const actualizarRangoSimulacion = (ordenes: OrderRequestExtended[]) => {
    if (!ordenes.length) {
      setStartDate('');
      setEndDate('');
      return;
    }
    const sorted = [...ordenes].sort((a, b) =>
      new Date(`${a.creationLocal}Z`).getTime() - new Date(`${b.creationLocal}Z`).getTime()
    );
    setStartDate(formatoInputDesdeIso(sorted[0].creationLocal));
    setEndDate(formatoInputDesdeIso(sorted[sorted.length - 1].creationLocal));
  };

  const fechasOriginalesPorOrden = useMemo(() => {
    const mapa = new Map<string, { fecha: Date; fechaStr: string }>();
    ordenesParaSimular.forEach(orden => {
      mapa.set(orden.id, {
        fecha: new Date(orden.fechaOriginal),
        fechaStr: orden.fechaOriginal
      });
    });
    return mapa;
  }, [ordenesParaSimular]);

  const panelSnapshot: SimulationSnapshot | null =
    snapshotVisible ?? snapshotProgreso ?? snapshotFinal ?? null;

  // Agrupa pedidos por vuelo, mostrando horas UTC
  type FlightGroup = {
    flightId: string;
    origen: string;
    destino: string;
    pedidos: string[];
    hora: string;
    fecha: string;
    departureUtc: string;
    arrivalUtc: string;
    horaLlegada: string;
  };

  const vuelosPorFlightId = useMemo<Map<string, FlightGroup>>(() => {
    if (!panelSnapshot) return new Map();
    const mapa = new Map<string, FlightGroup>();

    panelSnapshot.orderPlans.forEach(plan => {
      plan.routes.forEach(ruta => {
        ruta.segments.forEach(segmento => {
          if (!segmento.departureUtc || !segmento.arrivalUtc) {
            return;
          }

          const uniqueFlightId = segmento.departureUtc;
          const diaVuelo = new Date(segmento.departureUtc).toLocaleDateString('es-PE', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                timeZone: 'UTC'
              });
          const horaSalida = new Date(segmento.departureUtc).toLocaleTimeString('es-PE', {
                hour: '2-digit',
                minute: '2-digit',
                timeZone: 'UTC'
              });
          const horaLlegada = new Date(segmento.arrivalUtc).toLocaleTimeString('es-PE', {
                hour: '2-digit',
                minute: '2-digit',
                timeZone: 'UTC'
              });
          if (!mapa.has(uniqueFlightId)) {
            mapa.set(uniqueFlightId, {
              flightId: segmento.flightId,
              origen: segmento.origin,
              destino: segmento.destination,
              pedidos: [],
              hora: horaSalida,
              fecha: diaVuelo,
              departureUtc: segmento.departureUtc,
              arrivalUtc: segmento.arrivalUtc,
              horaLlegada: horaLlegada
            });
          }
          const vueloData = mapa.get(uniqueFlightId)!;
          if (!vueloData.pedidos.includes(plan.orderId)) {
            vueloData.pedidos.push(plan.orderId);
          }
        });
      });
    });
    return mapa;
  }, [panelSnapshot]);

  // --- LÓGICA DE FILTRADO PARA LOS PANELES ---
  const enviosFiltrados = useMemo<SimulationOrderPlan[]>(() => {
    if (!panelSnapshot) return [];
    const term = searchTerm.toLowerCase();

    return panelSnapshot.orderPlans.filter(plan => {
      // Filtro por término de búsqueda (ID de pedido)
      const matchSearch = term === '' || plan.orderId.toLowerCase().includes(term);

      // Filtro por Hub de origen
      const matchHub = filtroHub === '' || plan.routes.some(ruta =>
        ruta.segments.some(seg => seg.origin === filtroHub)
      );

      return matchSearch && matchHub;
    });
  }, [panelSnapshot, searchTerm, filtroHub]);

  const vuelosFiltrados = useMemo<FlightGroup[]>(() => {
    const term = searchTerm.toLowerCase();

    return Array.from(vuelosPorFlightId.values())
      .filter(vuelo => {
        // Filtro por término de búsqueda (ID de pedido en el vuelo)
        const matchSearch = term === '' || vuelo.pedidos.some(pedidoId => pedidoId.toLowerCase().includes(term));

        // Filtro por Hub de origen
        const matchHub = filtroHub === '' || vuelo.origen === filtroHub;

        return matchSearch && matchHub;
      })
      .sort((a, b) => {
        const dateA = a.departureUtc ? new Date(a.departureUtc).getTime() : 0;
        const dateB = b.departureUtc ? new Date(b.departureUtc).getTime() : 0;
        return dateA - dateB;
      });
  }, [vuelosPorFlightId, searchTerm, filtroHub]);

  // Lógica de carga de archivo
  const handleArchivoCargado = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const text = await file.text();
    const lines = text.split('\n').filter(line => line.trim() !== '');

    try {
      const ordenesParseadas: OrderRequestExtended[] = lines.map(line => {
        const parts = line.split('-');

        if (parts.length < 7) throw new Error(`Línea inválida: "${line}"`);

        const id = parts[0].trim();
        const datePart = parts[1].trim();
        const hourPart = parts[2].trim();
        const minutePart = parts[3].trim();
        const dest = parts[4].trim();
        const qty = parts[5].trim();
        const ref = parts[6].trim();

        const year = datePart.substring(0, 4);
        const month = datePart.substring(4, 6);
        const day = datePart.substring(6, 8);

        const isoDate = `${year}-${month}-${day}`;
        const isoTime = `${hourPart.padStart(2, '0')}:${minutePart.padStart(2, '0')}:00`;
        const creationLocal = `${isoDate}T${isoTime}`;

        const quantity = parseInt(qty, 10);
        if (isNaN(quantity)) throw new Error(`Cantidad inválida: "${qty}"`);

        return {
          id: id,
          customerReference: ref,
          destinationAirportCode: dest,
          quantity: quantity,
          creationLocal: creationLocal,
          fechaOriginal: creationLocal
        };
      });
      setOrdenesParaSimular(ordenesParseadas);
      actualizarRangoSimulacion(ordenesParseadas);
      setProyeccionGuardada(false);
      alert(`Se cargaron ${ordenesParseadas.length} órdenes correctamente`);

    } catch (e: any) {
      console.error('❌ Error al parsear archivo:', e);
      alert(`❌ Error: ${e.message}`);
      setOrdenesParaSimular([]);
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleIniciarSimulacion = () => {
    const startUtc = ensureSeconds(startDate);
    const endUtc = ensureSeconds(endDate);
    const payload = {
      startDate: startUtc,
      endDate: endUtc,
      windowMinutes: 5,
    };
    iniciar(payload, { start: startUtc, end: endUtc });
  };

  const handleGuardarProyeccion = async () => {
    if (ordenesParaSimular.length === 0) {
      alert('Carga un archivo con órdenes proyectadas antes de sincronizar.');
      return;
    }
    setEstaSincronizando(true);
    setProyeccionGuardada(false);
    try {
      await orderService.createProjectedBatch(ordenesParaSimular);
      setProyeccionGuardada(true);
      alert(`Se guardaron ${ordenesParaSimular.length} órdenes proyectadas en el backend`);
    } catch (error) {
      console.error('Error guardando pedidos proyectados:', error);
      alert('❌ No se pudieron guardar todos los pedidos proyectados. Revisa la consola para más detalles.');
    } finally {
      setEstaSincronizando(false);
    }
  };

  const handleTerminarSimulacion = async () => {
    await terminar();
    setDialogInfo({
      titulo: 'Simulación terminada',
      mensaje: 'Se detuvo la simulación actual. Ahora puedes modificar los datos o iniciar una nueva corrida.',
    });
  };

  const handleCerrarDialogo = () => setDialogInfo(null);

  useEffect(() => {
    if (status === 'completed') {
      setDialogInfo({
        titulo: 'Simulación terminada',
        mensaje: 'La simulación finalizó correctamente. Puedes ajustar parámetros o comenzar otra simulación.',
      });
    }
  }, [status]);

  if (isLoading) {
    return <div className="p-6 text-center">Cargando datos base...</div>;
  }
  if (isError) {
    return <div className="p-6 text-center text-red-500">Error al cargar datos.</div>;
  }
  const mostrandoOverlay = estaActivo && !hasSnapshots;

  return (
    <>
    <div className="flex h-screen w-full bg-base-200 text-base-content">

      {/* ========== PANEL LATERAL IZQUIERDO ========== */}
      <div className="w-80 bg-base-100 shadow-lg flex flex-col border-r border-base-300">

        {/* Header del Panel */}
        <div className="bg-primary text-primary-content p-4">
          <h1 className="text-xl font-bold mb-2">Simulación Semanal</h1>
          {/* Controles */}
          <div className="space-y-2">
            {ordenesParaSimular.length > 0 && (
              <div className="text-xs text-success-content text-center">
                 {ordenesParaSimular.length} órdenes listas para sincronizar
              </div>
            )}

            <div className="space-y-2 text-xs text-primary-content/90">
              <div>
                <label className="block uppercase tracking-wide text-[10px] text-primary-content/70 mb-1">
                  Inicio (UTC)
                </label>
                <input
                  type="datetime-local"
                  className="input input-sm w-full"
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                  disabled={estaActivo}
                />
              </div>
              <div>
                <label className="block uppercase tracking-wide text-[10px] text-primary-content/70 mb-1">
                  Fin (UTC)
                </label>
                <input
                  type="datetime-local"
                  className="input input-sm w-full"
                  value={endDate}
                  onChange={(event) => setEndDate(event.target.value)}
                  disabled={estaActivo}
                />
                <p className="text-[10px] text-primary-content/70 mt-1">
                  Vacío = usa todo el rango de pedidos proyectados guardados.
                </p>
              </div>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              onChange={handleArchivoCargado}
              disabled
            />

            <div className="flex gap-2">
              <button
                className="btn btn-sm btn-primary flex-1"
                onClick={handleIniciarSimulacion}
                disabled={estaActivo || estaSincronizando || isStarting}
              >
                <Play size={16} /> {isStarting ? 'Preparando...' : 'Iniciar'}
              </button>
              <button
                className="btn btn-sm btn-warning flex-1"
                onClick={pausar}
                disabled={!estaActivo && !estaVisualizando}
              >
                <Pause size={16} /> Pausar
              </button>
            </div>

            <button className="btn btn-sm btn-error w-full" onClick={handleTerminarSimulacion}>
              <XCircle size={16} /> Terminar
            </button>
          </div>
        </div>
        <div className="px-3 py-2 bg-base-200 border-b border-base-300 text-xs text-base-content">
          <label className="block uppercase tracking-wide text-[10px] text-base-content/70 mb-1">
            Velocidad de simulación (x)
          </label>
          <input
            type="number"
            className="input input-sm w-full"
            min={1}
            max={86400}
            value={simSpeed}
            onChange={(e) => {
              const parsed = Number(e.target.value);
              setSimSpeed(Math.max(1, isNaN(parsed) ? (simSpeed || 1) : parsed));
            }}
          />
          <p className="text-[10px] text-base-content/70 mt-1">
            Controla cuántas veces más rápido avanza el reloj simulado.
          </p>
        </div>

        <div className="p-3 bg-base-200 border-b border-base-300 space-y-3">
          <h3 className="text-sm font-semibold text-base-content">Filtros de Visualización</h3>

          {/* Barra de Búsqueda */}
          <div className="relative">
            <input
              type="text"
              placeholder="Buscar por ID de pedido..."
              className="input input-sm w-full pl-8"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              disabled={!panelSnapshot}
            />
            <Search size={16} className="absolute left-2 top-1/2 -translate-y-1/2 text-base-content/50" />
            {searchTerm && (
              <button onClick={() => setSearchTerm('')} className="absolute right-2 top-1/2 -translate-y-1/2">
                <X size={16} className="text-base-content/60 hover:text-error" />
              </button>
            )}
          </div>

          {/* Filtros de Hub */}
          <div>
            <label className="block uppercase tracking-wide text-[10px] text-base-content/70 mb-2">
              Hub de Origen
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                className={`btn btn-xs ${filtroHub === 'SPIM' ? 'btn-warning' : 'btn-outline btn-ghost text-base-content/70'}`}
                onClick={() => setFiltroHub(f => f === 'SPIM' ? '' : 'SPIM')}
                disabled={!panelSnapshot}
              >
                Lima (SPIM)
              </button>
              <button
                className={`btn btn-xs ${filtroHub === 'EBCI' ? 'btn-warning' : 'btn-outline btn-ghost text-base-content/70'}`}
                onClick={() => setFiltroHub(f => f === 'EBCI' ? '' : 'EBCI')}
                disabled={!panelSnapshot}
              >
                Bruselas (EBCI)
              </button>
              <button
                className={`btn btn-xs ${filtroHub === 'UBBB' ? 'btn-warning' : 'btn-outline btn-ghost text-base-content/70'}`}
                onClick={() => setFiltroHub(f => f === 'UBBB' ? '' : 'UBBB')}
                disabled={!panelSnapshot}
              >
                Baku (UBBB)
              </button>
              <button
                className={`btn btn-xs ${filtroHub === '' ? 'btn-active' : 'btn-outline btn-ghost text-base-content/70'}`}
                onClick={() => setFiltroHub('')}
                disabled={!panelSnapshot}
              >
                Todos
              </button>
            </div>
          </div>
        </div>

        {/* Tabs del Panel */}
        <div className="flex border-b border-base-300 bg-base-200">
          <button
            className={`flex-1 py-2 text-sm font-medium transition-colors ${
              vistaPanel === 'envios'
                ? 'border-b-2 border-primary text-primary bg-base-100'
                : 'text-base-content/60 hover:text-primary hover:bg-base-300'
            }`}
            onClick={() => setVistaPanel('envios')}
          >
            <Package size={16} className="inline mr-1" />
            Envíos ({enviosFiltrados.length})
          </button>
          <button
            className={`flex-1 py-2 text-sm font-medium transition-colors ${
              vistaPanel === 'vuelos'
                ? 'border-b-2 border-primary text-primary bg-base-100'
                : 'text-base-content/60 hover:text-primary hover:bg-base-300'
            }`}
            onClick={() => setVistaPanel('vuelos')}
          >
            <Plane size={16} className="inline mr-1" />
            Vuelos ({vuelosFiltrados.length})
          </button>
          <button
            className={`flex-1 py-2 text-sm font-medium transition-colors ${
              vistaPanel === 'aeropuertos'
                ? 'border-b-2 border-primary text-primary bg-base-100'
                : 'text-base-content/60 hover:text-primary hover:bg-base-300'
            }`}
            onClick={() => setVistaPanel('aeropuertos')}
          >
            Aeropuertos
          </button>
        </div>

        {/* Contenido del Panel */}
        <div className="flex-1 overflow-y-auto p-3 space-y-2 bg-base-100">

          {/* ===== VISTA: ENVÍOS ===== */}
          {vistaPanel === 'envios' && (
            <>
              {(!panelSnapshot || panelSnapshot.orderPlans.length === 0) && (
                <div className="text-center text-base-content/60 py-8">
                  {ordenesParaSimular.length > 0
                    ? 'Sincroniza y presiona "Iniciar" para comenzar la simulación'
                    : 'Carga pedidos proyectados o usa los ya guardados para iniciar.'}
                </div>
              )}
              {panelSnapshot && enviosFiltrados.length === 0 && (
                 <div className="text-center text-base-content/60 py-8">
                  No se encontraron envíos con los filtros actuales.
                </div>
              )}

              {/* USAMOS enviosFiltrados en lugar de panelSnapshot.orderPlans */}
              {enviosFiltrados.map((plan) => {
                  const primerSegmento = plan.routes[0]?.segments[0];
                  const ultimoSegmento = plan.routes[plan.routes.length - 1]?.segments[
                    plan.routes[plan.routes.length - 1].segments.length - 1
                  ];

                  const esRetrasado = plan.slackMinutes <= 0;
                  const estado = esRetrasado ? 'error' : 'success';
                  const estadoTexto = esRetrasado ? 'Retrasado' : 'A tiempo';

                  // Intenta buscar el ID completo, y si falla, busca el ID base
                  const idBase = plan.orderId.split('_')[0];
                  const fechaOriginalData = fechasOriginalesPorOrden.get(plan.orderId) || fechasOriginalesPorOrden.get(idBase);

                  let fechaRegistro = 'N/A';
                  let horaRegistro = 'N/A';

                  if (fechaOriginalData) {
                    fechaRegistro = fechaOriginalData.fecha.toLocaleDateString('es-PE', {
                      day: '2-digit',
                      month: 'short',
                      year: 'numeric'
                    });
                    horaRegistro = fechaOriginalData.fecha.toLocaleTimeString('es-PE', {
                      hour: '2-digit',
                      minute: '2-digit'
                    });
                  }

                  return (
                    <div
                      key={plan.orderId}
                      className={`card bg-base-200 border-l-4 ${
                        estado === 'success' ? 'border-success' : 'border-error'
                      } shadow-sm hover:shadow-md transition-shadow`}
                    >
                      <div className="card-body p-3">
                        <div className="flex justify-between items-start">
                          <div>
                            <h3 className="font-bold text-sm text-primary">
                              Pedido {plan.orderId}
                            </h3>
                            <p className="text-xs text-base-content/70">
                              Cantidad: {plan.routes.reduce((acc, r) => acc + r.quantity, 0) || 'N/A'}
                            </p>
                          </div>
                          <span className={`badge badge-sm ${
                            estado === 'success' ? 'badge-success' : 'badge-error'
                          }`}>
                            {estadoTexto}
                          </span>
                        </div>

                        {/* Fecha y hora de REGISTRO */}
                        <div className="mt-2 pt-2 border-t border-base-300">
                          <p className="text-[10px] text-base-content/60 mb-1">Fecha de Registro:</p>
                          <div className="flex gap-3 text-xs">
                            <div>
                              <span className="text-base-content/70">📅</span> {fechaRegistro}
                            </div>
                            <div>
                              <span className="text-base-content/70">🕒</span> {horaRegistro}
                            </div>
                          </div>
                        </div>

                        <div className="text-xs mt-2 space-y-1">
                          <div className="flex justify-between">
                            <span className="text-base-content/70">Origen:</span>
                            <span className="font-semibold">{primerSegmento?.origin || 'N/A'}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-base-content/70">Destino:</span>
                            <span className="font-semibold">
                              {ultimoSegmento?.destination || 'N/A'}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-base-content/70">Holgura:</span>
                            <span className={`font-semibold ${
                              estado === 'success' ? 'text-success' : 'text-error'
                            }`}>
                              {plan.slackMinutes} min
                            </span>
                          </div>
                        </div>

                        {/* Ruta detallada */}
                        <div className="mt-2 pt-2 border-t border-base-300">
                          <p className="text-xs font-semibold text-base-content/70 mb-1">
                            Ruta ({plan.routes.reduce((acc, r) => acc + r.segments.length, 0)} tramos):
                          </p>
                          {plan.routes.map((ruta, rutaIdx) => (
                            <div key={rutaIdx}>
                              {ruta.segments.map((seg, segIdx) => (
                                <div key={segIdx} className="text-xs text-base-content/70 ml-2 mb-1">
                                  • {seg.origin} → {seg.destination}
                                  {seg.departureUtc && seg.arrivalUtc && (
                                    <span className="text-[10px] text-base-content/60 ml-2">
                                      ({new Date(seg.departureUtc).toLocaleTimeString('es-PE', {
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        timeZone: 'UTC'
                                      })} - {new Date(seg.arrivalUtc).toLocaleTimeString('es-PE', {
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        timeZone: 'UTC'
                                      })})
                                    </span>
                                  )}
                                </div>
                              ))}
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  );
                })
              }
            </>
          )}

          {/* ===== VISTA: VUELOS ===== */}
          {vistaPanel === 'vuelos' && (
            <>
              {vuelosPorFlightId.size === 0 && (
                <div className="text-center text-base-content/60 py-8">
                  No hay vuelos activos
                </div>
              )}
              {vuelosPorFlightId.size > 0 && vuelosFiltrados.length === 0 && (
                 <div className="text-center text-base-content/60 py-8">
                  No se encontraron vuelos con los filtros actuales.
                </div>
              )}

              {/* USAMOS vuelosFiltrados en lugar de Array.from(vuelosPorFlightId.values()) */}
              {vuelosFiltrados.map(vuelo => {
                  // Buscamos el vuelo en movimiento usando su ID único
                  const vueloEnCurso = vuelosEnMovimiento.find(v => v.id === vuelo.departureUtc);

                  return (
                    // El key debe ser único (departureUtc)
                    <div key={vuelo.departureUtc} className="card bg-base-200 shadow-sm hover:shadow-md transition-shadow">
                      <div className="card-body p-3">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <Plane size={16} className="text-primary" />
                            <span className="font-bold text-sm text-primary">
                              {vuelo.origen} → {vuelo.destino}
                            </span>
                          </div>

                          {vueloEnCurso && (
                            <span className={`badge badge-xs ${
                              vueloEnCurso.progreso >= 100
                                ? 'badge-info'
                                : 'badge-success'
                            }`}>
                              {vueloEnCurso.progreso >= 100 ? 'Aterrizado' : 'En vuelo'}
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-base-content/70 font-semibold mb-1">
                          📅 {vuelo.fecha}
                        </div>

                        <div className="flex justify-between text-xs text-base-content/60 mb-2">
                          <div>
                            <span className="text-base-content/60">Salida:</span> {vuelo.hora}
                          </div>
                          <div>
                            <span className="text-base-content/60">Llegada:</span> {vuelo.horaLlegada}
                          </div>
                        </div>

                        <div className="mt-2 pt-2 border-t border-base-300">
                          <p className="text-xs text-base-content/70 font-semibold mb-1">
                            Pedidos ({vuelo.pedidos.length}):
                          </p>
                          <div className="flex flex-wrap gap-1">
                            {vuelo.pedidos.map(pedido => (
                              <span key={pedido} className="badge badge-xs bg-base-300 text-base-content border-base-200">
                                {pedido}
                              </span>
                            ))}
                          </div>
                        </div>

                        {vueloEnCurso && (
                          <div className="mt-3">
                            <div className="w-full bg-base-300 rounded-full h-2">
                              <div
                                className="bg-success h-2 rounded-full transition-all"
                                style={{ width: `${Math.min(vueloEnCurso.progreso, 100)}%` }}
                              />
                            </div>
                            <p className="text-xs text-base-content/70 mt-1 text-center">
                              {Math.round(vueloEnCurso.progreso)}%
                              {vueloEnCurso.progreso >= 100 && ' - Completado'}
                            </p>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
            </>
          )}

          {/* ===== VISTA: AEROPUERTOS ===== */}
          {vistaPanel === 'aeropuertos' && (
            <>
              {aeropuertos.map(aeropuerto => (
                <div key={aeropuerto.id} className="card bg-base-200 shadow-sm hover:shadow-md transition-shadow">
                  <div className="card-body p-3">
                    <h3 className="font-bold text-sm text-primary">{aeropuerto.id}</h3>
                    <p className="text-xs text-base-content/80">{aeropuerto.name}</p>
                    <div className="text-xs text-base-content/70 mt-1">
                      <div>Lat: {aeropuerto.latitude?.toFixed(4) ?? 'N/A'}</div>
                      <div>Lon: {aeropuerto.longitude?.toFixed(4) ?? 'N/A'}</div>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      </div>

      {/* ========== ÁREA DEL MAPA ========== */}
      <div className="flex-1 flex flex-col">

        {/* Barra superior con KPIs y Reloj */}
        <div className="bg-base-100 shadow-md p-3 flex justify-between items-center z-10">
          <div className="flex gap-6 text-sm">
            <div className="flex items-center gap-2">
              <Check size={18} className="text-success" />
              <span>A tiempo: <strong>{kpis.entregas}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <AlertTriangle size={18} className="text-error" />
              <span>Retrasados: <strong>{kpis.retrasados}</strong></span>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-sm">
              <span className="text-base-content/70">Progreso: </span>
              <span className="font-mono font-semibold">{reloj}</span>
            </div>

            {/* Reloj Mundial UTC */}
            {tiempoSimulado && (
              <div className="flex gap-2">
                <div className="badge badge-info">
                  📅 {tiempoSimulado.toLocaleDateString('es-PE', {
                    day: '2-digit',
                    month: 'short',
                    year: 'numeric',
                    timeZone: 'UTC'
                  })}
                </div>
                <div className="badge badge-success">
                  🕒 {tiempoSimulado.toLocaleTimeString('es-PE', {
                      hour: '2-digit',
                      minute: '2-digit',
                      timeZone: 'UTC'
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Mapa */}
        <div className="flex-1 relative">

          {/* Mostramos overlay de carga si el GA está corriendo */}
          {mostrandoOverlay && (
            <div className="absolute top-0 left-0 w-full h-full bg-base-300/80 z-[1000] flex items-center justify-center text-base-content p-8">
              <div className="text-center space-y-4">
                <div
                  className="animate-spin rounded-full h-24 w-24 border-8 border-primary border-t-transparent mx-auto"
                ></div>
                <h2 className="text-2xl font-semibold">Planificando Rutas...</h2>
                <p className="text-lg text-base-content/70">
                  El Sistema está procesando las órdenes.
                </p>
                <div className="stats bg-base-200 shadow-xl">
                  <div className="stat">
                    <div className="stat-title">Órdenes Procesadas</div>
                    <div className="stat-value text-primary">
                      {reloj}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
          <MapaVuelos
            aeropuertos={aeropuertos}
            activeSegments={activeSegments}
            isLoading={isLoading || isStarting}
            vuelosEnMovimiento={vuelosEnMovimiento}
            filtroHubActivo={filtroHub}
          />
        </div>
      </div>
    </div>
    {dialogInfo && (
      <dialog className="modal" open>
        <div className="modal-box">
          <h3 className="font-bold text-lg mb-2">{dialogInfo.titulo}</h3>
          <p className="text-base-content/80">{dialogInfo.mensaje}</p>
          <div className="modal-action">
            <button className="btn btn-primary" onClick={handleCerrarDialogo}>Entendido</button>
          </div>
        </div>
        <form method="dialog" className="modal-backdrop">
          <button onClick={handleCerrarDialogo}>close</button>
        </form>
      </dialog>
    )}
    </>
  );
}
