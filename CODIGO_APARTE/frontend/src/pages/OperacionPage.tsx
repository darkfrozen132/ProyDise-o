import { useState } from 'react';
import { useOperacion } from '../hooks/useOperacion';
import { MapaVuelos } from '../components/mapas/MapaVuelos';
import { Play, RefreshCw, Radio, ShieldAlert, Clock, Activity, CheckCircle, AlertTriangle, PlaneLanding, Package } from 'lucide-react';

export default function OperacionPage() {
    const {
        aeropuertos,
        activeSegments,
        vuelosEnMovimiento,
        status,
        simClock,
        eventLog,
        metrics,
        speed,
        setSpeed,
        actions
    } = useOperacion();

    // Fecha fija para la demo (cambiar)
    const [fechaSimulacion] = useState("2025-01-01");

    const handleStart = () => {
        actions.iniciar({
            startDate: `${fechaSimulacion}T00:00:00`,
            endDate: `${fechaSimulacion}T23:59:59`,
            windowMinutes: 60
        });
    };

    // Formateadores de fecha
    const formatDate = (date: Date) => {
        return date.toLocaleDateString('es-PE', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            timeZone: 'UTC'
        });
    };

    return (
        <div className="flex h-screen w-full bg-gray-900 text-gray-200 overflow-hidden font-sans">

            {/* --- SIDEBAR --- */}
            <div className="w-96 flex flex-col border-r border-gray-700 bg-gray-800 shadow-2xl z-20">

                {/* HEADER */}
                <div className="p-4 bg-indigo-900 border-b border-indigo-800 shadow-md flex justify-between items-center">
                    <div>
                        <h1 className="text-lg font-bold flex items-center gap-2 text-white tracking-tight">
                            <Radio className={status === 'running' ? 'animate-pulse text-green-400' : 'text-gray-400'} size={20} />
                            TORRE DE CONTROL
                        </h1>
                        <p className="text-[10px] text-indigo-200 opacity-80 uppercase tracking-widest mt-1">
                            Operación Día a Día
                        </p>
                    </div>
                    {status === 'running' && (
                        <div className="badge badge-success gap-1 text-xs font-bold animate-pulse">
                            EN VIVO
                        </div>
                    )}
                </div>

                {/* CONTENIDO DEL PANEL */}
                <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-gray-600">
                    <div className="p-4 border-b border-gray-700 bg-gray-800/50">
                        {status === 'idle' ? (
                            <div className="space-y-4 animate-in fade-in slide-in-from-left-4">
                                <div className="bg-gray-700/50 p-3 rounded border border-gray-600">
                                    <label className="text-xs text-gray-400 uppercase font-bold mb-1 block">Fecha Operativa</label>
                                    <div className="text-white font-mono">{fechaSimulacion}</div>
                                </div>

                                <button
                                    onClick={handleStart}
                                    className="btn btn-primary w-full gap-2 shadow-lg shadow-indigo-500/20 border-none bg-indigo-600 hover:bg-indigo-500 text-white"
                                >
                                    <Play size={16} fill="currentColor" /> INICIAR TURNO
                                </button>
                            </div>
                        ) : (
                            <div className="animate-in zoom-in-95 duration-300 space-y-4">
                                {/* 1. RELOJ PRINCIPAL */}
                                <div className="bg-black/40 p-3 rounded-lg border border-gray-700 text-center relative overflow-hidden">
                                    <div className="absolute top-2 right-2">
                                        <Clock size={12} className="text-gray-500 animate-spin-slow" />
                                    </div>
                                    <div className="text-[10px] text-indigo-300 uppercase tracking-widest font-bold mb-1 border-b border-gray-700 pb-1">
                                        {simClock ? formatDate(simClock) : '---'}
                                    </div>
                                    <div className="text-3xl font-mono font-bold text-white tracking-wider shadow-black drop-shadow-lg py-1">
                                        {simClock ? simClock.toLocaleTimeString('es-PE', {timeZone: 'UTC'}) : '--:--'}
                                    </div>
                                    <div className="text-[9px] text-gray-500 font-bold tracking-[0.2em] uppercase">ZULU TIME (UTC)</div>
                                </div>

                                {/* 2. CONTROL VELOCIDAD */}
                                <div className="form-control">
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="text-[10px] uppercase text-gray-400 font-bold">Velocidad</span>
                                        <span className="badge badge-xs badge-primary font-mono">{speed}x</span>
                                    </div>
                                    <input
                                        type="range"
                                        min="1"
                                        max="3600"
                                        value={speed}
                                        onChange={(e) => setSpeed(Number(e.target.value))}
                                        className="range range-xs range-primary"
                                    />
                                </div>

                                {/* 3. GRID DE KPIs (Métricas) */}
                                <div className="grid grid-cols-2 gap-2">
                                    {/* Card: Total Procesado */}
                                    <div className="bg-gray-700/30 p-2 rounded border border-gray-700 relative">
                                        <div className="text-xs text-gray-400 uppercase mb-1 flex items-center gap-1">
                                            <Package size={10}/> Total Pedidos
                                        </div>
                                        <div className="text-xl font-bold text-white">{metrics.processed}</div>
                                    </div>

                                    {/* Card: En Tránsito */}
                                    <div className="bg-gray-700/30 p-2 rounded border border-gray-700">
                                         <div className="text-xs text-gray-400 uppercase mb-1 flex items-center gap-1">
                                            <Activity size={10}/> En Tránsito
                                        </div>
                                        <div className="text-xl font-bold text-blue-400">{metrics.ordersInTransit}</div>
                                    </div>

                                    {/* Card: Vuelos Activos */}
                                    <div className="bg-gray-700/30 p-2 rounded border border-gray-700">
                                         <div className="text-xs text-gray-400 uppercase mb-1 flex items-center gap-1">
                                            <PlaneLanding size={10}/> Vuelos Activos
                                        </div>
                                        <div className="text-xl font-bold text-yellow-400">{metrics.activeFlights}</div>
                                    </div>

                                    {/* Card: Vuelos Completados */}
                                    <div className="bg-gray-700/30 p-2 rounded border border-gray-700">
                                         <div className="text-xs text-gray-400 uppercase mb-1 flex items-center gap-1">
                                            <CheckCircle size={10}/> Vuelos Fin.
                                        </div>
                                        <div className="text-xl font-bold text-green-400">{metrics.completedFlights}</div>
                                    </div>
                                </div>

                                {/* 4. BARRA DE ESTADO DE TIEMPO */}
                                <div className="bg-gray-900 rounded p-2 border border-gray-700">
                                    <div className="flex justify-between text-[10px] uppercase text-gray-400 font-bold mb-2">
                                        <span>Estado de Entregas</span>
                                    </div>
                                    <div className="flex gap-2 items-center text-xs">
                                        <div className="flex items-center gap-1 text-green-400 font-bold">
                                            <CheckCircle size={14} />
                                            {metrics.processed - metrics.delayedOrders} OK
                                        </div>
                                        <div className="h-4 w-px bg-gray-700"></div>
                                        <div className="flex items-center gap-1 text-red-400 font-bold">
                                            <AlertTriangle size={14} />
                                            {metrics.delayedOrders} Retrasados
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* PANEL DE CRISIS */}
                    {status !== 'idle' && (
                        <div className="p-4 border-b border-gray-700 bg-red-900/5 relative overflow-hidden">
                            <h3 className="text-[10px] font-bold text-red-400 uppercase mb-3 flex items-center gap-2 tracking-wider">
                                <ShieldAlert size={12}/> Gestión de Crisis
                            </h3>
                            <div className="grid grid-cols-3 gap-2 mb-3">
                                {['SPIM', 'EBCI', 'UBBB'].map(code => (
                                    <button
                                        key={code}
                                        onClick={() => actions.bloquear(code)}
                                        className="btn btn-xs btn-outline btn-error hover:bg-red-600 hover:text-white text-[9px]"
                                        title={`Simular bloqueo en ${code}`}
                                    >
                                        {code}
                                    </button>
                                ))}
                            </div>
                            <button
                                onClick={() => actions.replanificar()}
                                disabled={status === 'buffering'}
                                className={`btn btn-sm w-full border-0 text-[10px] font-bold tracking-wide ${status === 'buffering' ? 'bg-yellow-600 text-white cursor-wait' : 'bg-yellow-500 hover:bg-yellow-400 text-black'}`}
                            >
                                <RefreshCw size={14} className={status === 'buffering' ? 'animate-spin' : ''}/>
                                {status === 'buffering' ? 'OPTIMIZANDO...' : 'REPLANIFICAR'}
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* --- MAPA --- */}
            <div className="flex-1 relative z-0 bg-gray-900">
                <MapaVuelos
                    aeropuertos={aeropuertos}
                    activeSegments={activeSegments}
                    vuelosEnMovimiento={vuelosEnMovimiento}
                    isLoading={false}
                    filtroHubActivo=""
                />
            </div>
        </div>
    );
}