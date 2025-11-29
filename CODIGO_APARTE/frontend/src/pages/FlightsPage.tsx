import React, { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Eye, Plus, RefreshCw } from "lucide-react";
import { useFlights } from "../hooks/useFlights";
import type { Vuelo, EstadoVuelo } from "../types/vuelo";
import VuelosSummary from "../components/VuelosSummary";
import { aeropuertoService } from "../services/aeropuertoService";

// --- FUNCIONES AUXILIARES ---
const formatMinutes = (minutes: number) => {
  if (isNaN(minutes)) return '00:00';
  const h = Math.floor(minutes / 60).toString().padStart(2, "0");
  const m = (minutes % 60).toString().padStart(2, "0");
  return `${h}:${m}`;
};

const formatDuration = (startMin: number, endMin: number) => {
  const duration = endMin - startMin;
  if (isNaN(duration) || duration < 0) return 'N/A';
  const h = Math.floor(duration / 60);
  const m = duration % 60;
  return `${h}h ${m}m`;
};

const formatGmtOffset = (offset?: number) => {
  if (offset === undefined || offset === null) return "GMT";
  const sign = offset >= 0 ? "+" : "";
  return `GMT${sign}${offset}`;
};

export default function FlightsPage() {
  const { list, create } = useFlights();
  const [showForm, setShowForm] = useState(false);
  const { data: aeropuertos = [] } = useQuery({
    queryKey: ["aeropuertos"],
    queryFn: aeropuertoService.getAll,
  });

  const [form, setForm] = useState<Omit<Vuelo, "id">>({
    origen: "",
    destino: "",
    salidaLocalMin: 0,
    llegadaLocalMin: 0,
    capacidad: 250,
  });

  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<EstadoVuelo | "todos">("todos");

  // --- MANEJADORES DE LÓGICA ---
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    create.mutate(form, {
      onSuccess: () => {
        setForm({
          origen: "", destino: "", salidaLocalMin: 0, llegadaLocalMin: 0, capacidad: 250,
        });
        setShowForm(false);
      },
    });
  };

  const handleClearFilters = () => {
    setSearchTerm("");
    setStatusFilter("todos");
  };

  // Filtrado
  const filteredFlights = useMemo(() => {
    if (!list.data) return [];
    return list.data.filter((flight) => {
      const flightStatus = flight.estado ?? "programado";
      if (statusFilter !== "todos" && flightStatus !== statusFilter) return false;
      if (searchTerm) {
        const lowerSearchTerm = searchTerm.toLowerCase();
        const flightCode = flight.codigoVuelo ?? "";
        return (
          flightCode.toLowerCase().includes(lowerSearchTerm) ||
          flight.origen.toLowerCase().includes(lowerSearchTerm) ||
          flight.destino.toLowerCase().includes(lowerSearchTerm)
        );
      }
      return true;
    });
  }, [list.data, searchTerm, statusFilter]);

  const airportLookup = useMemo(() => {
    const map = new Map<string, typeof aeropuertos[number]>();
    aeropuertos.forEach((ap) => {
      const key = ap.code || ap.id;
      if (key) {
        map.set(key, ap);
      }
      if (ap.id && ap.id !== key) {
        map.set(ap.id, ap);
      }
    });
    return map;
  }, [aeropuertos]);

  // --- ESTADOS DE CARGA ---
  if (list.isLoading)
    return <div className="p-6 text-center">Cargando vuelos...</div>;

  if (list.isError)
    return (
      <div className="p-6 text-center text-error">
        Error al cargar vuelos
        <button className="btn btn-sm ml-2" onClick={() => list.refetch()}>
          Reintentar
        </button>
      </div>
    );

  return (
    <div className="p-6 space-y-4">
      {/* Encabezado */}
      <div className="flex justify-between items-center">
        <h1 className="text-xl font-semibold">Gestión de Vuelos</h1>
        <div className="flex gap-2">
          <button
            className="btn btn-outline btn-sm"
            onClick={() => list.refetch()}
            disabled={list.isFetching}
          >
            <RefreshCw size={16} className={list.isFetching ? 'animate-spin' : ''} />
            Actualizar
          </button>
          <button
            className="btn btn-primary btn-sm flex items-center gap-2"
            onClick={() => setShowForm((s) => !s)}
          >
            <Plus size={16} />
            Nuevo Vuelo
          </button>
        </div>
      </div>
      <VuelosSummary />
      {/* Formulario */}
      {showForm && (
        <form
          onSubmit={handleSubmit}
          className="card bg-base-200 p-4 shadow-md border border-base-300"
        >
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            <div className="form-control">
              <label className="label">
                <span className="label-text">Origen</span>
              </label>
              <select
                className="select select-bordered select-sm w-full"
                value={form.origen}
                onChange={(e) => setForm({ ...form, origen: e.target.value })}
                required
              >
                <option value="" disabled>
                  Selecciona aeropuerto
                </option>
                {aeropuertos.map((ap) => (
                  <option key={ap.code ?? ap.id} value={ap.code ?? ap.id}>
                    {(ap.code ?? ap.id) ?? ""} - {ap.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-control">
              <label className="label">
                <span className="label-text">Destino</span>
              </label>
              <select
                className="select select-bordered select-sm w-full"
                value={form.destino}
                onChange={(e) => setForm({ ...form, destino: e.target.value })}
                required
              >
                <option value="" disabled>
                  Selecciona aeropuerto
                </option>
                {aeropuertos.map((ap) => (
                  <option key={ap.code ?? ap.id} value={ap.code ?? ap.id}>
                    {(ap.code ?? ap.id) ?? ""} - {ap.name}
                  </option>
                ))}
              </select>
            </div>
            <input type="number" placeholder="Minuto Salida" className="input input-bordered input-sm w-full" value={form.salidaLocalMin} onChange={(e) => setForm({ ...form, salidaLocalMin: parseInt(e.target.value) || 0 })} required />
            <input type="number" placeholder="Minuto Llegada" className="input input-bordered input-sm w-full" value={form.llegadaLocalMin} onChange={(e) => setForm({ ...form, llegadaLocalMin: parseInt(e.target.value) || 0 })} required />
            <input type="number" placeholder="Capacidad" className="input input-bordered input-sm w-full" value={form.capacidad} onChange={(e) => setForm({ ...form, capacidad: parseInt(e.target.value) || 250 })} required />
          </div>
          <div className="flex justify-end mt-4 gap-2">
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowForm(false)}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary btn-sm" disabled={create.isPending}>
              {create.isPending ? "Guardando..." : "Guardar"}
            </button>
          </div>
        </form>
      )}

      {/* Barra de filtros */}
      <div className="card bg-base-200 p-4 shadow-md border border-base-300">
        <div className="flex flex-wrap items-center gap-4">
          <input
            type="text"
            placeholder="Buscar por código, origen, destino..."
            className="input input-bordered input-sm w-full max-w-xs"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <select
            className="select select-bordered select-sm"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as any)}
          >
            <option value="todos">Todos los estados</option>
            <option value="en vuelo">En vuelo</option>
            <option value="programado">Programado</option>
            <option value="completado">Completado</option>
            <option value="retrasado">Retrasado</option>
          </select>
          <button className="btn btn-ghost btn-sm" onClick={handleClearFilters}>
              Limpiar filtros
          </button>
        </div>
      </div>

      {/* Tabla Vuelos */}
      <div className="overflow-x-auto">
        <table className="table table-zebra text-sm">
          <thead>
            <tr>
              <th>Vuelo</th>
              <th>Origen</th>
              <th>Destino</th>
              <th>Horario Local</th>
              <th>Duración</th>
              <th>Capacidad</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {!list.data || filteredFlights.length === 0 ? (
              <tr><td colSpan={7} className="text-center opacity-60 py-4">No se encontraron vuelos.</td></tr>
            ) : (
              filteredFlights.map((flight) => {
                const origenAirport = airportLookup.get(flight.origen);
                const destinoAirport = airportLookup.get(flight.destino);
                const origenNombre = origenAirport
                  ? `${origenAirport.name} (${origenAirport.id})`
                  : flight.origen;
                const destinoNombre = destinoAirport
                  ? `${destinoAirport.name} (${destinoAirport.id})`
                  : flight.destino;
                return (
                  <tr key={flight.id}>
                    <td><strong>{flight.codigoVuelo ?? `FL-${flight.id}`}</strong></td>
                    <td>
                      <div>{origenNombre}</div>
                      {origenAirport && (
                        <div className="text-xs opacity-60">
                          {formatGmtOffset(origenAirport.gmtOffsetHours)}
                        </div>
                      )}
                    </td>
                    <td>
                      <div>{destinoNombre}</div>
                      {destinoAirport && (
                        <div className="text-xs opacity-60">
                          {formatGmtOffset(destinoAirport.gmtOffsetHours)}
                        </div>
                      )}
                    </td>
                    <td>
                      <div className="font-mono">
                        {formatMinutes(flight.salidaLocalMin)} → {formatMinutes(flight.llegadaLocalMin)}
                      </div>
                      <div className="text-xs opacity-60">
                        Origen: {formatGmtOffset(origenAirport?.gmtOffsetHours)} / Destino: {formatGmtOffset(destinoAirport?.gmtOffsetHours)}
                      </div>
                    </td>
                    <td>{formatDuration(flight.salidaLocalMin, flight.llegadaLocalMin)}</td>
                    <td>
                      <div>{flight.capacidad} pax/día</div>
                      {flight.aeronave && <div className="text-xs opacity-60">Aeronave: {flight.aeronave}</div>}
                    </td>
                    <td className="flex gap-2">
                      <button className="btn btn-ghost btn-xs" title="Ver detalle"><Eye size={16} /></button>
                      {/* botones de Editar/Eliminar */}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
