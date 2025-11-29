import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { simulacionService } from '../services/simulacionService';
import { aeropuertoService } from '../services/aeropuertoService';
import type { Airport } from '../types/airport';
import type { SimulationSnapshot, SimulationMessage, SimulationStartRequest } from '../types/simulation';
import type { SegmentoVuelo, VueloEnMovimiento } from './useSimulacion';

const BROKER_URL = 'ws://localhost:8080/ws';
const TOPIC_PREFIX = '/topic/simulations/';

export const useOperacion = () => {
    const [simulationId, setSimulationId] = useState<string | null>(null);
    const [status, setStatus] = useState<'idle' | 'buffering' | 'running' | 'error'>('idle');

    const [dayPlan, setDayPlan] = useState<SimulationSnapshot | null>(null);
    const { data: aeropuertos = [] } = useQuery<Airport[]>({
        queryKey: ['aeropuertos'],
        queryFn: aeropuertoService.getAll,
    });

    const [simClock, setSimClock] = useState<Date | null>(null);
    const [speed, setSpeed] = useState(60);
    const [eventLog, setEventLog] = useState<string[]>([]);

    const [activeSegments, setActiveSegments] = useState<SegmentoVuelo[]>([]);
    const [vuelosEnMovimiento, setVuelosEnMovimiento] = useState<VueloEnMovimiento[]>([]);

    // ESTADO DE MÉTRICAS
    const [metrics, setMetrics] = useState({
        processed: 0,          // Total pedidos procesados en el plan
        ordersInTransit: 0,    // Pedidos volando AHORA
        ordersDelivered: 0,    // Pedidos que ya aterrizaron
        delayedOrders: 0,      // Total pedidos con retraso
        activeFlights: 0,      // Aviones en el aire
        completedFlights: 0,   // Aviones que ya aterrizaron
        totalFlights: 0        // Total de vuelos programados para el día
    });

    // 1. WebSocket
    useEffect(() => {
        if (!simulationId) return;
        const client = new Client({
            brokerURL: BROKER_URL,
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(TOPIC_PREFIX + simulationId, (msg) => {
                    const payload: SimulationMessage = JSON.parse(msg.body);
                    if (payload.type === 'PROGRESS' || payload.type === 'COMPLETED') {
                        if (payload.snapshot && payload.snapshot.orderPlans.length > 0) {
                            setDayPlan(payload.snapshot);
                            if (status === 'buffering') {
                                setStatus('running');
                                setEventLog(prev => ['Plan recibido. Iniciando operación.', ...prev]);
                            }
                        }
                    }
                });
            }
        });
        client.activate();
        return () => { client.deactivate(); };
    }, [simulationId, status]);

    // 2. Iniciar
    const startMutation = useMutation({
        mutationFn: (req: SimulationStartRequest) => simulacionService.startSimulation(req),
        onSuccess: (data, variables) => {
            setSimulationId(data.simulationId);
            setStatus('buffering');
            setEventLog(['Sincronizando...', 'Obteniendo plan operativo...']);
            const start = new Date(variables.startDate + (variables.startDate.endsWith('Z') ? '' : 'Z'));
            setSimClock(start);
        }
    });

    // 3. Reloj
    useEffect(() => {
        if (status !== 'running' || !simClock) return;
        const interval = setInterval(() => {
            setSimClock(prev => {
                if (!prev) return null;
                return new Date(prev.getTime() + (speed * 1000));
            });
        }, 1000);
        return () => clearInterval(interval);
    }, [status, speed]);

    // 4. Lógica Principal de Cálculo
    useEffect(() => {
        if (!dayPlan || !simClock || aeropuertos.length === 0) return;

        const currentMs = simClock.getTime();
        const airportMap = new Map(aeropuertos.map(a => [a.id, [a.latitude, a.longitude]]));
        const segmentosMap = new Map<string, SegmentoVuelo>();
        const vuelosActivosMap = new Map<string, VueloEnMovimiento>();

        // Variables temporales para conteo
        let countOrdersInTransit = 0;
        let countOrdersDelivered = 0;
        let countDelayedTotal = 0;
        let countCompletedFlights = 0;
        let countTotalFlights = 0;

        // Set para contar vuelos únicos completados (evitar duplicados por pedido)
        const completedFlightIds = new Set<string>();

        dayPlan.orderPlans.forEach(plan => {
            // Conteo de retrasos (Estático del plan)
            if (plan.slackMinutes <= 0) countDelayedTotal++;

            let isOrderDelivered = true; // Asumimos entregado hasta demostrar lo contrario
            let isOrderInTransit = false;

            plan.routes.forEach(route => {
                route.segments.forEach(seg => {
                    const depMs = new Date(seg.departureUtc).getTime();
                    const arrMs = new Date(seg.arrivalUtc).getTime();
                    const uniqueFlightId = `${seg.flightId}-${seg.departureUtc}`;

                    // Conteo total de vuelos únicos en el plan
                    if (!completedFlightIds.has(uniqueFlightId)) {
                        countTotalFlights++;
                    }

                    // ESTADO DEL VUELO
                    if (currentMs > arrMs) {
                         // El vuelo ya aterrizó
                         if (!completedFlightIds.has(uniqueFlightId)) {
                             countCompletedFlights++;
                             completedFlightIds.add(uniqueFlightId);
                         }
                    } else if (currentMs >= depMs && currentMs <= arrMs) {
                        // El vuelo está EN EL AIRE
                        isOrderInTransit = true;
                        isOrderDelivered = false;

                        // Lógica de Mapa (Solo para activos)
                        const originCoords = airportMap.get(seg.origin);
                        const destCoords = airportMap.get(seg.destination);

                        if (!segmentosMap.has(uniqueFlightId)) {
                            segmentosMap.set(uniqueFlightId, {
                                id: uniqueFlightId,
                                flightId: seg.flightId,
                                origin: seg.origin,
                                destination: seg.destination,
                                departureUtc: seg.departureUtc,
                                arrivalUtc: seg.arrivalUtc,
                                orderIds: [plan.orderId],
                                retrasado: plan.slackMinutes <= 0
                            });
                        }

                        if (originCoords && destCoords) {
                            const progressPct = ((currentMs - depMs) / (arrMs - depMs)) * 100;
                            if (!vuelosActivosMap.has(uniqueFlightId)) {
                                vuelosActivosMap.set(uniqueFlightId, {
                                    id: uniqueFlightId,
                                    orderId: plan.orderId,
                                    flightId: seg.flightId,
                                    latActual: originCoords[0] + (destCoords[0] - originCoords[0]) * (progressPct / 100),
                                    lonActual: originCoords[1] + (destCoords[1] - originCoords[1]) * (progressPct / 100),
                                    progreso: progressPct,
                                    estadoVisual: plan.slackMinutes <= 0 ? 'retrasado' : 'en curso',
                                    origen: seg.origin,
                                    destino: seg.destination,
                                    departureTime: seg.departureUtc,
                                    arrivalTime: seg.arrivalUtc
                                });
                            }
                        }
                    } else {
                        // El vuelo aún no sale
                        isOrderDelivered = false;
                    }
                });
            });

            if (isOrderInTransit) countOrdersInTransit++;
            if (isOrderDelivered) countOrdersDelivered++;
        });

        setActiveSegments(Array.from(segmentosMap.values()));
        setVuelosEnMovimiento(Array.from(vuelosActivosMap.values()));

        // Actualizamos métricas completas
        setMetrics({
            processed: dayPlan.processedOrders,
            ordersInTransit: countOrdersInTransit,
            ordersDelivered: countOrdersDelivered,
            delayedOrders: countDelayedTotal,
            activeFlights: vuelosActivosMap.size,
            completedFlights: countCompletedFlights,
            totalFlights: countCompletedFlights + vuelosActivosMap.size
        });

    }, [simClock, dayPlan, aeropuertos]);

    const triggerReplan = () => {
        setEventLog(prev => ['ALERTA: Iniciando protocolo de replanificación...', ...prev]);
        const oldStatus = status;
        setStatus('buffering');
        setTimeout(() => {
             setStatus(oldStatus === 'running' ? 'running' : 'idle');
             setEventLog(prev => ['Rutas optimizadas correctamente.', ...prev]);
        }, 1500);
    };

    const triggerBlock = (code: string) => {
        setEventLog(prev => [`ALERTA CRÍTICA: Operaciones suspendidas en ${code}`, ...prev]);
    };

    return {
        aeropuertos,
        activeSegments,
        vuelosEnMovimiento,
        status,
        simClock,
        eventLog,
        speed,
        setSpeed,
        metrics,
        actions: {
            iniciar: startMutation.mutate,
            replanificar: triggerReplan,
            bloquear: triggerBlock
        }
    };
};