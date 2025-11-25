import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer } from 'react-leaflet';
import { Drawer, Dialog, DialogTitle, DialogContent, IconButton } from '@mui/material';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import { IoArrowBackCircleOutline } from "react-icons/io5";
import { RiResetLeftFill } from "react-icons/ri";
import { FaStop } from "react-icons/fa6";
import { FaPlay } from "react-icons/fa6";
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import vuelosSemana from '../../../assets/data/vuelosSemana.json';
import { getPlanificacionSemanal } from '../../../config/api';
import './SimuladorSemanal.css';
import {
	getAirports,
	getFlights,
	iniciarSimulacion,
	pausarSimulacion,
	reanudarSimulacion,
	detenerSimulacion
} from '../../../config/api';
import LegendDialog from '../../../components/ui/Dialog/LegendDialog';
import LegendButton from '../../../components/ui/Button/LegendButton';
import { conectarWebSocketPlanificacion } from '../../../config/websocket';

/* Constantes de configuracion de tiempo de simulacion */
const DESIRED_TIME_SCALE = 500; // Valor de K
const REAL_TICK_MS = 1000; // Intervalo del reloj (1s)

/* Reparar iconos por defecto de Leaflet */
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
	iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
	iconUrl: require('leaflet/dist/images/marker-icon.png'),
	shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

/* Iconos de aviones personalizados como SVG dentro de divIcon */
const createAirplaneIcon = (color, rotation = 0) => {
	const iconSvg = `<svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="11" cy="11" rx="2" ry="10" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="8" rx="9" ry="1.8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="15" rx="4" ry="1.2" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<path d="M11 17 L11 19.5 L10 19.5 L10 17 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
		</svg>`;

	/* Crear divIcon con el SVG correspondiente */
	return L.divIcon({
		html: `<div style="transform: rotate(${rotation}deg); display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));">${iconSvg}</div>`,
		className: 'airplane-icon',
		iconSize: [22, 22],
		iconAnchor: [11, 11],
		popupAnchor: [0, -12]
	});
};

/* Iconos de aeropuertos personalizados */
const createAirportIcon = (isSede = false, saturation = 0) => {
	let size, color, borderColor, borderWidth, shadow;
	/* Color y tamaño según tipo y saturación */
	if (isSede) {
		size = 32; color = '#dc3545'; borderColor = '#FFD700'; borderWidth = 4; shadow = '0 4px 16px rgba(220, 53, 69, 0.6)';
	} else {
		size = 22; borderColor = '#ffffff'; borderWidth = 3; shadow = '0 3px 10px rgba(0,0,0,0.4)';
		if (saturation >= 80) color = '#dc3545'; else if (saturation >= 50) color = '#ffc107'; else color = '#28a745';
	}
	/* Crear divIcon con estilos */
	return new L.DivIcon({
		className: 'airport-marker',
		html: `<div style="background: ${color}; border: ${borderWidth}px solid ${borderColor}; border-radius: 50%; width: ${size}px; height: ${size}px; display:flex;align-items:center;justify-content:center; box-shadow:${shadow}; position:relative; cursor:pointer; transition: all .3s ease;">
			<i class="fas fa-${isSede ? 'building' : 'plane'}" style="color:white; font-size:${size * 0.4}px; ${isSede ? '' : 'transform: rotate(45deg);'} text-shadow:0 1px 3px rgba(0,0,0,.5);"></i>
		</div>`,
		iconSize: [size, size], iconAnchor: [size / 2, size / 2], popupAnchor: [0, -size / 2]
	});
};

/* Componente para manejar marcadores dinámicos en el mapa */
function DynamicMarkers({ flights, airports, activeView, showRoutes }) {
	const map = (0, require('react-leaflet').useMap)();
	/* Actualizar marcadores cuando cambian vuelos, aeropuertos, vista activa o rutas */
	React.useEffect(() => {
		const airportMarkers = []; const flightMarkers = [];
		map.eachLayer(layer => { if (layer instanceof L.Marker || layer instanceof L.Polyline) map.removeLayer(layer); });
		/* Añadir marcadores de aeropuertos si la vista es 'airports' o 'flights' */
		if (activeView === 'airports' || activeView === 'flights') {
			airports.forEach(airport => {
				const isUnlimited = airport.capacity === 'ILIMITADO';
				const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
				const icon = createAirportIcon(airport.isSede, saturation);
				const marker = L.marker([airport.lat, airport.lng], { icon }).bindPopup(`<div class="popup-content"><div class="popup-header"><strong class="popup-title ${airport.isSede ? 'sede-title' : 'airport-title'}">${airport.name}</strong></div></div>`);
				marker.addTo(map); airportMarkers.push(marker);
			});
		}
		/* Añadir marcadores de vuelos y rutas si la vista es 'flights' o 'routes' */
		if (activeView === 'flights' || activeView === 'routes') {
			flights.forEach(flight => {
				const icon = createAirplaneIcon(flight.aircraftColor, flight.rotation);
				const marker = L.marker([flight.currentLat, flight.currentLng], { icon }).bindPopup(`<div class="popup-content"><div class="popup-header"><strong class="popup-title">✈️ Vuelo ${flight.id}</strong></div></div>`);
				marker.addTo(map); flightMarkers.push(marker);
				if (showRoutes) {
					const polyline = L.polyline([[flight.origin.lat, flight.origin.lng], [flight.destination.lat, flight.destination.lng]], { color: '#888', weight: 2, opacity: 0.6, dashArray: '5, 5' });
					polyline.addTo(map);
				}
			});
		}
		/* Limpiar marcadores al desmontar o actualizar */
		return () => { airportMarkers.forEach(m => map.removeLayer(m)); flightMarkers.forEach(m => map.removeLayer(m)); };
	}, [flights, airports, activeView, showRoutes, map]);
	return null;
}

/* Función para parsear fecha simulada en UTC */
const parseSimDateUTC = (str) => str ? new Date(str.replace(' ', 'T') + ':00Z') : null;

// Rumbo geodésico en grados (0°=N, 90°=E)
// Si tu SVG “mira a la derecha”, usa (brg - 90 + 360) % 360
function bearingDegrees(lat1, lon1, lat2, lon2) {
	const toRad = d => d * Math.PI / 180;
	const toDeg = r => r * 180 / Math.PI;
	const φ1 = toRad(lat1), φ2 = toRad(lat2);
	const Δλ = toRad(lon2 - lon1);
	const y = Math.sin(Δλ) * Math.cos(φ2);
	const x = Math.cos(φ1) * Math.cos(φ2) - Math.sin(φ1) * Math.sin(φ2) * Math.cos(Δλ);
	return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

const SimuladorSemanal = () => {
	const [fechaInicioSimulacion, setFechaInicioSimulacion] = useState(""); // la fecha que envías
	const [planFixed, setPlanFixed] = useState([]);  // lista de vuelos del JSON local
	const [simClock, setSimClock] = useState(null);  // reloj simulado (Date)

	const simStartRef = useRef(null);

	const navigate = useNavigate();
	const [flights, setFlights] = useState([]);
	const [flightsInAir, setFlightsInAir] = useState(0);
	const [currentTime, setCurrentTime] = useState(new Date(2024, 7, 27, 8, 0, 0));
	const [elapsedTime, setElapsedTime] = useState({ days: 0, hours: 0, minutes: 0 });
	const [isRunning, setIsRunning] = useState(true);
	const [speed, setSpeed] = useState(1);
	const [simulationStatus, setSimulationStatus] = useState('Monitoreo semanal activo');
	const [activeView, setActiveView] = useState('flights');
	const [showRoutes, setShowRoutes] = useState(false);
	const [showLegend, setShowLegend] = useState(false);

	/* Datos de aeropuertos - se cargarán desde la API */
	const [airports, setAirports] = useState([]);
	const [loadingAirports, setLoadingAirports] = useState(true);
	const intervalRef = useRef(); /* Referencia para el intervalo de simulación */

	// ==================== ESTADO SSE (TIEMPO DE SIMULACIÓN Y RUTAS) ====================
	const [simulacionActiva, setSimulacionActiva] = useState(false);
	const [horaSimulada, setHoraSimulada] = useState(null);
	const [tiempoRealMs, setTiempoRealMs] = useState(0);
	const [tickActual, setTickActual] = useState(0);
	const [timeScale, setTimeScale] = useState(10.0);
	const [rutasSolucion, setRutasSolucion] = useState([]); // Rutas que vienen del SSE
	const eventSourceRef = useRef(null); /* Referencia para el EventSource SSE */

	// ==================== ESTADO WEBSOCKET PLANIFICACIÓN ====================
	const [wsPlanificacion, setWsPlanificacion] = useState(null);
	const [wsConectado, setWsConectado] = useState(false);
	const [iteracionesPlanificacion, setIteracionesPlanificacion] = useState([]);
	const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle'); // idle, running, completed, error, waiting
	const wsPlanificacionRef = useRef(null);
	const [autoInicioIntentado, setAutoInicioIntentado] = useState(false);
	const [intentosRealizados, setIntentosRealizados] = useState(0); // Contador de reintentos
	const [tiempoRealTranscurrido, setTiempoRealTranscurrido] = useState(0); // Tiempo real en segundos
	const tiempoInicioRef = useRef(null); // Momento en que se inició la planificación
	const intervalTiempoRealRef = useRef(null); // Intervalo para actualizar tiempo real
	const [tiempoSimulacionActual, setTiempoSimulacionActual] = useState(null); // Hora de simulación del backend



	/* ==================== FUNCIÓN PARA CONVERTIR RUTA DEL BACKEND A VUELO ==================== */
	// NOTA: Esta función se usaba para convertir rutas SSE del backend
	// en vuelos compatibles con el mapa. En el modo actual (simClock + planFixed)
	// la simulación construye los flights directamente en el useEffect de simClock.
	// La dejamos por si volvemos a usar SSE en el futuro.
	const convertirRutaAVuelo = (ruta) => {
		// Determinar color según progreso y estado
		let aircraftColor;
		if (!ruta.enVuelo) {
			aircraftColor = '#6c757d'; // Gris si no está en vuelo
		} else if (ruta.progress >= 0.8) {
			aircraftColor = '#28a745'; // Verde cerca del destino
		} else if (ruta.progress >= 0.5) {
			aircraftColor = '#ffc107'; // Amarillo a mitad de camino
		} else {
			aircraftColor = '#007bff'; // Azul al inicio
		}

		// Calcular rotación basada en dirección
		const deltaLat = ruta.destinoLatitud - ruta.origenLatitud;
		const deltaLng = ruta.destinoLongitud - ruta.origenLongitud;
		const rotation = Math.atan2(deltaLng, deltaLat) * (180 / Math.PI);

		// Crear objeto de vuelo compatible con el mapa
		return {
			id: `RT${String(ruta.id).padStart(4, '0')}`,
			origin: {
				code: ruta.originCode,
				lat: ruta.origenLatitud,
				lng: ruta.origenLongitud,
				region: ruta.regionOrigen
			},
			destination: {
				code: ruta.destinationCode,
				lat: ruta.destinoLatitud,
				lng: ruta.destinoLongitud,
				region: ruta.regionDestino
			},
			progress: ruta.progress,
			altitude: ruta.altitude,
			speed: ruta.speed,
			status: ruta.enVuelo ? 'active' : 'grounded',
			packageCapacity: ruta.totalPackages,
			currentPackages: ruta.totalPackages,
			packageType: 'MPE',
			isSameContinentFlight: ruta.regionOrigen === ruta.regionDestino,
			currentLat: ruta.currentLatitude,
			currentLng: ruta.currentLongitude,
			aircraftColor,
			rotation
		};
	};

	/* ==================== DEBUG: MONITOREAR CAMBIOS EN tiempoRealMs ==================== */
	useEffect(() => {
		console.log('tiempoRealMs actualizado a:', tiempoRealMs, 'ms =', {
			horas: Math.floor(tiempoRealMs / 3600000),
			minutos: Math.floor((tiempoRealMs % 3600000) / 60000),
			segundos: Math.floor((tiempoRealMs % 60000) / 1000)
		});
	}, [tiempoRealMs]);

	/* ==================== Cargar aeropuertos desde API al montar ==================== */
	useEffect(() => {
		const fetchAirports = async () => {
			try {
				setLoadingAirports(true);
				console.log('Iniciando carga de aeropuertos...');
				const data = await getAirports();
				console.log('Aeropuertos cargados desde API:', data.length, 'aeropuertos');
				setAirports(data);
			} catch (error) {
				console.error('Error al cargar aeropuertos desde API:', error);
				console.log('Usando datos de fallback...');
				/* Fallback a datos estáticos en caso de error */
				const fallbackData = [
					{ name: 'Lima-Jorge Chávez', code: 'SPIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 980, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
					{ name: 'Bogotá', code: 'SKBO', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 720, isSede: false, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
					{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 850, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 960, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' }
				];
				setAirports(fallbackData);
				console.log('Datos de fallback cargados:', fallbackData.length, 'aeropuertos');
			} finally {
				setLoadingAirports(false);
				console.log('Carga de aeropuertos finalizada');
			}
		};

		fetchAirports();
	}, []);

	/* ==================== Cargar plan fijo desde JSON local al montar ==================== */
	useEffect(() => {
		if (vuelosSemana?.vuelos) {
			setPlanFixed(vuelosSemana.vuelos);
		}
	}, [vuelosSemana]);

	/* ==================== Efecto para actualizar vuelos según simClock ==================== */
	useEffect(() => {
		if (!simClock || planFixed.length === 0 || airports.length === 0) return;

		const nuevos = [];

		for (const vuelo of planFixed) {
			const start = parseSimDateUTC(vuelo.fechaInicial);
			const end = parseSimDateUTC(vuelo.fechaFinal);
			if (!start || !end) continue;

			// Ocultar completamente antes del inicio
			if (simClock < start) {
				continue; // No aparece hasta su fechaInicial
			}

			const totalMs = end - start;
			// Evitar divisiones raras si el backend manda algo mal
			if (!(totalMs > 0)) continue;

			const elapsed = Math.max(0, Math.min(totalMs, simClock - start));
			const progress = elapsed / totalMs; // 0..1

			// Busca aeropuertos por código ICAO
			const o = airports.find(a => String(a.code).toUpperCase() === String(vuelo.origenCodigoICAO).toUpperCase());
			const d = airports.find(a => String(a.code).toUpperCase() === String(vuelo.destinoCodigoICAO).toUpperCase());
			if (!o || !d) {
				console.warn('ICAO no encontrado:', vuelo.origenCodigoICAO, vuelo.destinoCodigoICAO);
				continue;
			}

			// Interpolación lineal
			const currentLat = o.lat + (d.lat - o.lat) * progress;
			const currentLng = o.lng + (d.lng - o.lng) * progress;

			// Rotación (si tu SVG apunta a la derecha, ajusta -90)
			const brg = bearingDegrees(o.lat, o.lng, d.lat, d.lng);
			const rotation = (brg - 90 + 360) % 360;

			// Estado y color
			const enVuelo = progress > 0 && progress < 1;
			const status = progress >= 1 ? 'arrived' : (progress <= 0 ? 'scheduled' : 'active');
			const totalPaquetes = vuelo.totalPaquetes ?? 0;
			const aircraftColor = '#007bff';

			// 🔀 Política al llegar:
			// A) Mantenerlo visible en el destino:
			const mostrarAlLlegar = true;
			if (!mostrarAlLlegar && progress >= 1) {
				continue; // ❗ Ocúltalo tras llegar
			}

			nuevos.push({
				id: `${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${start.getTime()}`,
				origin: { code: o.code, lat: o.lat, lng: o.lng, region: o.region },
				destination: { code: d.code, lat: d.lat, lng: d.lng, region: d.region },
				progress,
				altitude: enVuelo ? 35000 : 0,
				speed: enVuelo ? 850 : 0,
				status,
				currentLat, currentLng,
				aircraftColor,
				rotation,
				packageCapacity: totalPaquetes,
				currentPackages: totalPaquetes,
				packageType: 'MPE',
				isSameContinentFlight: o.region === d.region,
			});
		}

		setFlights(nuevos);
		setFlightsInAir(nuevos.filter(v => v.status === 'active').length);
	}, [simClock, planFixed, airports]);

	/* Efecto para avanzar el reloj de simulación en modo local */
	useEffect(() => {
		if (!simulacionActiva) return;

		const advanceMs = DESIRED_TIME_SCALE * REAL_TICK_MS;

		const id = setInterval(() => {
			setSimClock(prev => (prev ? new Date(prev.getTime() + advanceMs) : null));
			setTickActual(prev => prev + 1);          // 1 segundo
			setTiempoRealMs(prev => prev + REAL_TICK_MS);
		}, REAL_TICK_MS);

		return () => clearInterval(id);
	}, [simulacionActiva]);


	// ==================== FUNCIONES PARA CONTROLAR SIMULACIÓN ====================
	const handleIniciarSimulacion = async () => {
		console.log("Iniciando simulación desde:", fechaInicioSimulacion);
		// 1) Si ya está corriendo, no hacemos nada
		if (simulacionActiva) {
			console.log("La simulación ya está activa.");
			return;
		}

		// 2) Si ya hubo una simulación (simClock existe) y solo estaba pausada → reanudar
		if (simClock) {
			console.log("Reanudando simulación en:", simClock);
			setSimulacionActiva(true);
			return;
		}

		// 3) Si NO hay simClock (por ejemplo después de Reset) pero YA tenemos planFixed,
		//    solo reiniciamos la simulación desde el inicio usando el plan existente
		if (planFixed.length > 0 && simStartRef.current) {
			console.log("Reiniciando simulación usando el plan fijo ya cargado.");

			const inicioUTC = new Date(simStartRef.current);
			setSimClock(inicioUTC);

			setTiempoRealMs(0);
			setTickActual(0);
			setSimulacionActiva(true);   // 🔥 vuelve a encender el intervalo
			return;
		}

		// 4) Si llegamos aquí: NO está corriendo, NO hay simClock y NO hay planFixed
		//    => primera vez (o se borró el plan). Aquí sí llamamos al backend.
		try {
			if (!fechaInicioSimulacion) {
				alert("Por favor, selecciona una fecha de inicio.");
				return;
			}

			// Pasa fecha y K
			const factorK = 500;
			const plan = await getPlanificacionSemanal(fechaInicioSimulacion, factorK);

			const vuelos = Array.isArray(plan?.vuelos) ? plan.vuelos : [];
			if (!vuelos.length) {
				alert('El plan de vuelo está vacia.');
				return;
			}
			setPlanFixed(vuelos);
			console.log("Plan fijo recibido:", vuelos.length, "vuelos");
		} catch (e) {
			console.error("Error al cargar plan semanal:", e);
			alert("No se pudo obtener el plan semanal del backend.");
			return;
		}

		//Reloj local (sin SSE) -> iniciamos desde 00:00 en la fecha elegida
		const inicioUTC = new Date(`${fechaInicioSimulacion}T00:00:00Z`);
		setSimClock(inicioUTC);
		simStartRef.current = inicioUTC;

		setTiempoRealMs(0);
		setTickActual(0);
		setTimeScale(DESIRED_TIME_SCALE);
		setSimulacionActiva(true);
	};

	const handleDetenerSimulacion = () => {
		console.log("Simulación pausada");
  		setSimulacionActiva(false);   // solo pausa
	};

	const handleResetSimulacion = () => {
		console.log("Simulación reiniciada completamente.");

		setSimulacionActiva(false);
		setSimClock(null);
		setTiempoRealMs(0);
		setTickActual(0);
		setFlights([]);
		setFlightsInAir(0);
		// Si deseas también resetear planificaciones recibidas:
		// setPlanFixed([]);
	};

	// ==================== FUNCIONES WEBSOCKET DE PLANIFICACIÓN ====================
	
	/**
	 * Convertir vuelo de planificación al formato del mapa
	 */
	const convertirVueloPlanificacionAMapa = (vuelo) => {
		// Buscar coordenadas de aeropuertos (comparación case-insensitive)
		const origen = airports.find(a => 
			String(a.code).toUpperCase() === String(vuelo.origenCodigoICAO).toUpperCase()
		);
		const destino = airports.find(a => 
			String(a.code).toUpperCase() === String(vuelo.destinoCodigoICAO).toUpperCase()
		);
		
		if (!origen || !destino) {
			console.warn(`⚠️ Aeropuertos no encontrados: ${vuelo.origenCodigoICAO} o ${vuelo.destinoCodigoICAO}`);
			console.log('📍 Aeropuertos disponibles:', airports.map(a => a.code));
			return null;
		}

		// Calcular progreso basado en fechas
		let progress = 0.0;
		try {
			const fechaInicio = new Date(vuelo.fechaInicial);
			const fechaFin = new Date(vuelo.fechaFinal);
			const ahora = new Date();
			
			const totalDuracion = fechaFin - fechaInicio;
			const transcurrido = ahora - fechaInicio;
			
			if (totalDuracion > 0 && transcurrido > 0) {
				progress = Math.max(0, Math.min(1, transcurrido / totalDuracion));
			} else {
				progress = 0.1; // 10% del trayecto por defecto
			}
		} catch (e) {
			console.warn('⚠️ Error calculando progreso:', e);
			progress = 0.1;
		}
		
		// Interpolación de posición
		const currentLat = origen.lat + (destino.lat - origen.lat) * progress;
		const currentLng = origen.lng + (destino.lng - origen.lng) * progress;
		
		// Calcular rotación usando bearingDegrees
		const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
		const rotation = (brg - 90 + 360) % 360;
		
		// Determinar estado del vuelo
		const status = progress >= 1 ? 'arrived' : (progress <= 0 ? 'scheduled' : 'active');
		
		// Calcular total de paquetes
		const totalPaquetes = vuelo.pedidos?.reduce((sum, p) => sum + (p.cantidad || 0), 0) || 0;
		
		return {
			id: `PL-${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${Date.now()}-${Math.random()}`,
			origin: {
				code: vuelo.origenCodigoICAO,
				lat: origen.lat,
				lng: origen.lng,
				region: origen.region
			},
			destination: {
				code: vuelo.destinoCodigoICAO,
				lat: destino.lat,
				lng: destino.lng,
				region: destino.region
			},
			progress,
			altitude: progress > 0 && progress < 1 ? 35000 : 0,
			speed: progress > 0 && progress < 1 ? 850 : 0,
			status,
			currentLat,
			currentLng,
			aircraftColor: '#3b82f6', // Azul para vuelos de planificación
			rotation,
			packageCapacity: totalPaquetes,
			currentPackages: totalPaquetes,
			packageType: 'MPE',
			isSameContinentFlight: origen.region === destino.region,
			// Datos adicionales de planificación
			fechaInicial: vuelo.fechaInicial,
			fechaFinal: vuelo.fechaFinal,
			pedidos: vuelo.pedidos || []
		};
	};

	/**
	 * Conectar al WebSocket de planificación
	 */
	const handleConectarWsPlanificacion = () => {
		console.log('📡 handleConectarWsPlanificacion llamado');
		console.log('📡 Estado actual wsRef:', wsPlanificacionRef.current ? 'existe' : 'null');
		console.log('📡 Estado conectado:', wsPlanificacionRef.current?.estaConectado?.() ? 'SÍ' : 'NO');
		
		if (wsPlanificacionRef.current && wsPlanificacionRef.current.estaConectado()) {
			console.log('⚠️ WebSocket ya está conectado');
			return;
		}

		console.log('🚀 Llamando a conectarWebSocketPlanificacion...');
		const ws = conectarWebSocketPlanificacion(
			// onMessage: Procesar mensajes del servidor
			(data) => {
				console.log('📩 Mensaje de planificación:', data);
				
				switch(data.tipo) {
					case 'conexion':
						console.log('✅ Conexión establecida');
						break;
						
				case 'progreso':
					// Agregar iteración a la lista
					setIteracionesPlanificacion(prev => [data, ...prev].slice(0, 20));
					setEstadoPlanificacion('running');
					
					// Actualizar tiempo de simulación si viene en los datos
					if (data.datos?.tiempoSimulacionActual) {
						setTiempoSimulacionActual(data.datos.tiempoSimulacionActual);
					}
					
					console.log(`📊 Progreso - Iteración #${data.datos?.ejecucionNumero || '?'}`);
					console.log('📦 Datos de progreso:', {
						tieneSolucion: !!data.solucion,
						tieneVuelos: !!data.solucion?.vuelos,
						cantidadVuelos: data.solucion?.vuelos?.length || 0
					});
					
					// ✈️ GRAFICAR VUELOS EN EL MAPA (progreso no suele tener vuelos, solo info)
					if (data.solucion?.vuelos && data.solucion.vuelos.length > 0) {
						console.log(`✈️ Recibidos ${data.solucion.vuelos.length} vuelos en progreso #${data.datos?.ejecucionNumero}`);
						
						// Convertir vuelos al formato del mapa
						const vuelosParaMapa = data.solucion.vuelos
							.map(convertirVueloPlanificacionAMapa)
							.filter(v => v !== null); // Filtrar vuelos con aeropuertos no encontrados
						
						console.log(`🔄 Convertidos ${vuelosParaMapa.length} de ${data.solucion.vuelos.length} vuelos`);
						
						// ACUMULAR vuelos en el mapa (no reemplazar)
						setFlights(prevFlights => {
							const vuelosActualizados = [...prevFlights, ...vuelosParaMapa];
							console.log(`📊 Total vuelos en mapa: ${vuelosActualizados.length}`);
							return vuelosActualizados;
						});
						setFlightsInAir(prev => prev + vuelosParaMapa.length);
						
						console.log(`🗺️ Graficados ${vuelosParaMapa.length} vuelos en el mapa`);
					}
					break;				case 'completado':
					console.log('🎉 Planificación completada - Intervalo recibido');
					console.log('📦 Datos completos:', {
						iteracion: data.datos?.ejecucionNumero,
						tieneSolucion: !!data.solucion,
						tieneVuelos: !!data.solucion?.vuelos,
						cantidadVuelos: data.solucion?.vuelos?.length || 0
					});
					
					// Agregar resultado final a la lista
					setIteracionesPlanificacion(prev => [data, ...prev].slice(0, 50));
					
					// Actualizar tiempo de simulación
					if (data.datos?.tiempoSimulacionActual) {
						setTiempoSimulacionActual(data.datos.tiempoSimulacionActual);
						console.log('⏰ Tiempo simulación actualizado:', data.datos.tiempoSimulacionActual);
					}
					
					// ✈️ GRAFICAR SOLUCIÓN EN EL MAPA
					if (data.solucion?.vuelos && data.solucion.vuelos.length > 0) {
						console.log(`✈️ Procesando ${data.solucion.vuelos.length} vuelos de la iteración #${data.datos?.ejecucionNumero}`);
						console.log('🔍 Ejemplo de vuelo recibido:', data.solucion.vuelos[0]);
						
						// Convertir vuelos al formato del mapa
						const vuelosParaMapa = data.solucion.vuelos
							.map(convertirVueloPlanificacionAMapa)
							.filter(v => v !== null);
						
						console.log(`� Convertidos ${vuelosParaMapa.length} de ${data.solucion.vuelos.length} vuelos`);
						
						if (vuelosParaMapa.length > 0) {
							console.log('🗺️ Ejemplo de vuelo convertido:', vuelosParaMapa[0]);
							
							// Actualizar el mapa ACUMULANDO vuelos (no reemplazar)
							setFlights(prevFlights => {
								const vuelosActualizados = [...prevFlights, ...vuelosParaMapa];
								console.log(`📊 Total vuelos en mapa: ${vuelosActualizados.length} (${prevFlights.length} anteriores + ${vuelosParaMapa.length} nuevos)`);
								return vuelosActualizados;
							});
							
							setFlightsInAir(prev => {
								const nuevoTotal = prev + vuelosParaMapa.length;
								console.log(`✈️ Vuelos en aire actualizados: ${nuevoTotal}`);
								return nuevoTotal;
							});
							
							console.log(`✅ ${vuelosParaMapa.length} vuelos graficados exitosamente`);
						} else {
							console.warn('⚠️ No se pudieron convertir vuelos (aeropuertos no encontrados)');
						}
						
						// 🔄 CONTINUAR con la siguiente iteración
						const nuevoIntento = intentosRealizados + 1;
						const maxIntentos = 1000;
						
						if (nuevoIntento < maxIntentos) {
							console.log(`⏩ Solicitando siguiente intervalo... (Iteración ${nuevoIntento})`);
							setIntentosRealizados(nuevoIntento);
							
							// Pequeño delay para visualizar mejor (opcional)
							setTimeout(() => {
								const enviado = enviarSolicitudPlanificacion();
								if (!enviado) {
									console.error('❌ No se pudo enviar la siguiente solicitud');
									setEstadoPlanificacion('completed');
								}
							}, 100);
						} else {
							console.log(`🛑 Máximo de iteraciones alcanzado (${maxIntentos})`);
							setEstadoPlanificacion('completed');
							setAutoInicioIntentado(false);
							
							if (intervalTiempoRealRef.current) {
								clearInterval(intervalTiempoRealRef.current);
								intervalTiempoRealRef.current = null;
							}
						}
					} else {
						// No hay vuelos en este intervalo, continuar
						const nuevoIntento = intentosRealizados + 1;
						const maxIntentos = 1000;
						
						if (nuevoIntento < maxIntentos) {
							console.warn(`⚠️ Sin vuelos en iteración ${data.datos?.ejecucionNumero}. Continuando...`);
							setIntentosRealizados(nuevoIntento);
							
							setTimeout(() => {
								enviarSolicitudPlanificacion();
							}, 100);
						} else {
							console.log(`🛑 Máximo de iteraciones alcanzado sin vuelos`);
							setEstadoPlanificacion('completed');
							setAutoInicioIntentado(false);
							
							if (intervalTiempoRealRef.current) {
								clearInterval(intervalTiempoRealRef.current);
								intervalTiempoRealRef.current = null;
							}
						}
					}
					break;
					
				case 'error':
					console.error('❌ Error en planificación:', data.mensaje);
					
					// Si el error es "Ya hay una planificación en curso", mantener estado running
					if (data.mensaje?.includes('Ya hay una planificación en curso')) {
						console.log('⚠️ Ya hay una planificación en curso, manteniendo estado...');
						setEstadoPlanificacion('running'); // Mantener como running, no error
						// NO resetear autoInicioIntentado para evitar múltiples intentos
					} else {
						// Para otros errores, marcar como error
						setEstadoPlanificacion('error');
						setAutoInicioIntentado(false); // Resetear para permitir nuevo intento
						alert(`Error en planificación: ${data.mensaje}`);
						
						// Detener cronómetro de tiempo real
						if (intervalTiempoRealRef.current) {
							clearInterval(intervalTiempoRealRef.current);
							intervalTiempoRealRef.current = null;
						}
					}
					break;					default:
						console.log('📨 Mensaje desconocido:', data);
				}
			},
			// onError
			(error) => {
				console.error('❌ Error en WebSocket:', error);
				setEstadoPlanificacion('error');
			},
			// onOpen
			() => {
				console.log('✅ WebSocket de planificación conectado');
				setWsConectado(true);
				setEstadoPlanificacion('idle');
			},
			// onClose
			(event) => {
				console.log('🔌 WebSocket desconectado. Código:', event.code);
				setWsConectado(false);
				setEstadoPlanificacion('idle');
				wsPlanificacionRef.current = null;
			}
		);
		
		wsPlanificacionRef.current = ws;
		setWsPlanificacion(ws);
	};

	/**
	 * Desconectar WebSocket de planificación
	 */
	const handleDesconectarWsPlanificacion = () => {
		if (wsPlanificacionRef.current) {
			wsPlanificacionRef.current.cerrar();
			setWsConectado(false);
			setWsPlanificacion(null);
		}
	};

	/**
	 * Iniciar planificación con WebSocket
	 */
	// Función para enviar solicitud de planificación (sin validaciones de estado)
	const enviarSolicitudPlanificacion = () => {
		if (!wsPlanificacionRef.current?.estaConectado()) {
			console.warn('⚠️ WebSocket no conectado');
			return false;
		}

		if (!fechaInicioSimulacion) {
			console.warn('⚠️ No hay fecha de inicio seleccionada');
			return false;
		}

		// Enviar solicitud de planificación
		wsPlanificacionRef.current.iniciarPlanificacion(
			fechaInicioSimulacion,
			5, // Factor K para simulación semanal
			{
				tamanioPoblacion: 20,
				maxGeneraciones: 20,
				limiteGeneracionesSinMejora: 10
			}
		);
		
		console.log('📤 Solicitud de planificación enviada para fecha:', fechaInicioSimulacion);
		return true;
	};

	const handleIniciarPlanificacion = () => {
		if (!wsPlanificacionRef.current || !wsPlanificacionRef.current.estaConectado()) {
			console.warn('⚠️ WebSocket no conectado, esperando...');
			return;
		}

		if (!fechaInicioSimulacion) {
			console.warn('⚠️ No hay fecha de inicio seleccionada');
			alert('Por favor, selecciona una fecha de inicio');
			return;
		}

		// No iniciar si ya hay una planificación en curso
		if (estadoPlanificacion === 'running') {
			console.warn('⚠️ Ya hay una planificación en curso');
			return;
		}

		// 🧹 LIMPIAR TODO para nueva planificación
		console.log('🧹 Limpiando estado para nueva planificación...');
		setIteracionesPlanificacion([]);
		setFlights([]); // Limpiar vuelos del mapa
		setFlightsInAir(0);
		setIntentosRealizados(0);
		setEstadoPlanificacion('running');
		
		// 🕐 INICIAR CRONÓMETRO DE TIEMPO REAL
		tiempoInicioRef.current = Date.now();
		setTiempoRealTranscurrido(0);
		
		// Actualizar tiempo real cada segundo
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
		}
		intervalTiempoRealRef.current = setInterval(() => {
			if (tiempoInicioRef.current) {
				const transcurrido = Math.floor((Date.now() - tiempoInicioRef.current) / 1000);
				setTiempoRealTranscurrido(transcurrido);
			}
		}, 1000);

		console.log('🚀 Iniciando planificación para fecha:', fechaInicioSimulacion);
		enviarSolicitudPlanificacion();
	};

	/**
	 * Limpiar iteraciones de planificación
	 */
	const handleLimpiarIteraciones = () => {
		setIteracionesPlanificacion([]);
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false); // Permitir nuevo auto-inicio
	};

	/**
	 * Detener planificación continua y limpiar mapa
	 */
	const handleDetenerPlanificacion = () => {
		console.log('🛑 Deteniendo planificación continua...');
		
		// Detener cronómetro
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
			intervalTiempoRealRef.current = null;
		}
		
		// Resetear estado
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false);
		
		console.log('✅ Planificación detenida. Vuelos permanecen en el mapa.');
	};

	/**
	 * Limpiar todos los vuelos del mapa
	 */
	const handleLimpiarMapa = () => {
		console.log('🧹 Limpiando mapa...');
		setFlights([]);
		setFlightsInAir(0);
		setIteracionesPlanificacion([]);
		setIntentosRealizados(0);
		console.log('✅ Mapa limpiado');
	};

	// Auto-conectar WebSocket al montar el componente
	useEffect(() => {
		console.log('='.repeat(80));
		console.log('🔌 AUTO-CONECTANDO WEBSOCKET DE PLANIFICACIÓN');
		console.log('URL:', 'ws://localhost:8000/ws/planificacion');
		console.log('='.repeat(80));
		
		// Delay para evitar problemas con React Strict Mode (doble ejecución en desarrollo)
		const timer = setTimeout(() => {
			console.log('⏰ Iniciando conexión WebSocket...');
			handleConectarWsPlanificacion();
		}, 100);
		
		// Cleanup al desmontar
		return () => {
			clearTimeout(timer);
			if (wsPlanificacionRef.current) {
				console.log('🧹 Limpiando WebSocket de planificación...');
				wsPlanificacionRef.current.cerrar();
			}
			// Limpiar intervalo de tiempo real
			if (intervalTiempoRealRef.current) {
				clearInterval(intervalTiempoRealRef.current);
			}
		};
	}, []);

	// Auto-iniciar planificación cuando se selecciona fecha y WebSocket está conectado
	useEffect(() => {
		// Solo auto-iniciar si:
		// 1. WebSocket está conectado
		// 2. Hay fecha seleccionada
		// 3. No se ha intentado auto-iniciar aún
		// 4. No hay planificación en curso (estado idle)
		if (wsConectado && fechaInicioSimulacion && !autoInicioIntentado && estadoPlanificacion === 'idle') {
			console.log('🚀 Auto-iniciando planificación...');
			setAutoInicioIntentado(true);
			
			// Pequeño delay para asegurar que la conexión esté estable
			setTimeout(() => {
				handleIniciarPlanificacion();
			}, 500);
		}
	}, [wsConectado, fechaInicioSimulacion, autoInicioIntentado, estadoPlanificacion]);

	// Resetear flag cuando cambia la fecha
	useEffect(() => {
		setAutoInicioIntentado(false);
	}, [fechaInicioSimulacion]);

	/* Calcular métricas de saturación de aeropuertos */
	const getSaturation = () => {
		const regularAirports = airports.filter(airport => !airport.isSede);
		if (regularAirports.length === 0) return "0.00";
		const totalCapacity = regularAirports.reduce((sum, airport) => sum + (typeof airport.capacity === 'number' ? airport.capacity : 0), 0);
		const totalPackages = regularAirports.reduce((sum, airport) => sum + airport.packages, 0);
		if (totalCapacity === 0) return "0.00";
		return ((totalPackages / totalCapacity) * 100).toFixed(2);
	};
	/* Encontrar aeropuerto más saturado */
	const getMostSaturatedAirport = () => {
		const regularAirports = airports.filter(airport => !airport.isSede && typeof airport.capacity === 'number');
		if (regularAirports.length === 0) return { name: 'N/A', capacity: 0, packages: 0, saturation: 0 };
		return regularAirports.reduce((max, airport) => {
			const saturation = (airport.packages / airport.capacity) * 100;
			const maxSaturation = (max.packages / max.capacity) * 100;
			return saturation > maxSaturation ? airport : max;
		});
	};
	/* Obtener aeropuerto más saturado */
	const mostSaturatedAirport = getMostSaturatedAirport();
	const getFlightsByAltitude = () => flightsInAir;

	/* Estado y lógica para el drawer lateral */
	const drawerWidth = 300; // ancho del drawer
	const [open, setOpen] = useState(false);

	/* Accion de boton de Regresar */
	const goBack = () => {
		window.history.back(); // retrocede una página
	};

	/* Mostrar indicador de carga mientras se obtienen los aeropuertos */
	if (loadingAirports) {
		return (
			<div className="section-content" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
				<div style={{ textAlign: 'center', color: '#fff' }}>
					<i className="fas fa-spinner fa-spin" style={{ fontSize: '48px', marginBottom: '20px' }}></i>
					<p style={{ fontSize: '18px' }}>Cargando aeropuertos...</p>
				</div>
			</div>
		);
	}

	/* ==================== Texto dinamico para los botones ==================== */
	let startButtonLabel = "Iniciar";

	if (simulacionActiva) {
		startButtonLabel = "Iniciar";
	} else if (simClock) {
		// hubo simulación antes y ahora está pausada
		startButtonLabel = "Reanudar";
	}

	return (
		<div className="section-content" id="simulationSection">
			{/* Botón semicircular pegado al borde */}
			<IconButton
				onClick={() => setOpen(v => !v)}
				aria-label="toggle drawer"
				sx={{
					position: 'fixed',
					top: '50%',
					transform: 'translateY(-50%)',
					left: open ? drawerWidth - 15 : 0,
					zIndex: 1201,
					width: 36,
					height: 72,
					borderRadius: open ? '36px 0 0 36px' : '0 36px 36px 0',
					backgroundColor: '#2c4a6b',
					color: '#fff',
					boxShadow: '0 6px 18px rgba(0,0,0,0.25)',
					transition: 'left .35s ease, border-radius .35s ease, background-color .2s ease',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					'&:hover': { backgroundColor: '#496c92ff' },
				}}
			>
				{open ? <ChevronLeftIcon /> : <ChevronRightIcon />}
			</IconButton>
			{/* Drawer lateral */}
			<Drawer
				variant="persistent"
				anchor="left"
				open={open}
				sx={{
					width: open ? drawerWidth : 0,
					flexShrink: 0,
					'& .MuiDrawer-paper': {
						width: drawerWidth,
						boxSizing: 'border-box',
						padding: 0,
						position: 'relative',
						overflow: 'visible',
					},
				}}
			>
				{/* Barra lateral con información y controles */}
				<div className="simulation-sidebar">
					<div className="modes-sidebar-container"
						style={{
							display: 'flex',
							justifyContent: 'center',
							alignItems: 'flex-start',
							width: '100%',
							paddingTop: '2px',
							paddingBottom: '20px',
						}}>
					</div>
					<div className="sidebar-header">
						<h3><i className="fas fa-info-circle"></i> Información del Sistema</h3>
					</div>
					<div className="sidebar-content">
						<div className="time-section">
							<div className="current-time">
								<label>Semana actual:</label>
								<div className="time-display">Semana {elapsedTime.days}</div>
							</div>
						</div>

						{/* Controles de simulación */}
						<div className="stats-section">
							<h4><i className="fas fa-chart-line"></i> Métricas de Saturación</h4>
							<div className="metrics-grid">
								<div className="metric-card">
									<div className="metric-icon"><i className="fas fa-plane"></i></div>
									<div className="metric-content">
										<div className="metric-label">Vuelos en el aire</div>
										<div className="metric-value">{flights.length}</div>
										<div className="metric-sublabel">de 402 total</div>
									</div>
								</div>
								<div className="metric-card">
									<div className="metric-icon aircraft"><i className="fas fa-tachometer-alt"></i></div>
									<div className="metric-content">
										<div className="metric-label">Saturación de aviones</div>
										<div className="metric-value">{((flights.length / 402) * 100).toFixed(1)}%</div>
										<div className="metric-sublabel">capacidad aérea</div>
									</div>
								</div>
								<div className="metric-card">
									<div className="metric-icon airport"><i className="fas fa-warehouse"></i></div>
									<div className="metric-content">
										<div className="metric-label">Saturación aeropuertos</div>
										<div className="metric-value">{getSaturation()}%</div>
										<div className="metric-sublabel">almacenes regulares</div>
									</div>
								</div>
								<div className="metric-card sede">
									<div className="metric-icon"><i className="fas fa-building"></i></div>
									<div className="metric-content">
										<div className="metric-label">Sedes principales</div>
										<div className="metric-value">3/3</div>
										<div className="metric-sublabel">operativas</div>
									</div>
								</div>
							</div>
						</div>
						{/* Aeropuerto más saturado */}
						<div className="airport-section">
							<h4>Aeropuertos más saturados</h4>
							<div className="airport-info">
								<div className="airport-name">{mostSaturatedAirport.name}</div>
								<div className="airport-details">
									<div>Capacidad: {mostSaturatedAirport.capacity.toLocaleString()}</div>
									<div>Paquetes: {mostSaturatedAirport.packages.toLocaleString()}</div>
									<div className="saturation-highlight">Saturación: {((mostSaturatedAirport.packages / mostSaturatedAirport.capacity) * 100).toFixed(2)}%</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</Drawer>

			{/* Contenedor principal que se ajusta al ancho del drawer */}
			<div
				style={{
					flexGrow: 1,
					minWidth: 0,
					transition: 'margin 0.2s ease', // transición suave al abrir/cerrar el drawer
					marginLeft: 0,
					paddingLeft: 0,
					boxSizing: 'border-box',
				}}
			>
				{/* Contenido principal con mapa*/}
				<div className="simulation-main-content">
					<div className="content-wrapper">
						{/* Panel de control superior */}
						<div className="control-panel">
							<div className="header-control-panel" style={{ marginTop: '-15px' }}>
								{/* Botón para regresar a operaciones */}
								<button className="btn-back" onClick={goBack} title="Regresar">
									<IoArrowBackCircleOutline size={32} />
								</button>
								<h2>Simulación Semanal</h2>
							</div>

							{/* ==================== PANEL SIMPLE DE TIEMPO SSE ==================== */}
							<div>
								<div style={{
									borderRadius: '8px',
									padding: '15px 20px',
									marginTop: '-45px',
									marginBottom: '-35px',
									display: 'flex',
									justifyContent: 'space-between',
									alignItems: 'center',
									flexWrap: 'wrap',
									gap: '15px'
								}}>
									{/* Indicador de WebSocket */}
									<div style={{
										padding: '10px 15px',
										background: wsConectado ? '#d4edda' : '#f8d7da',
										border: `2px solid ${wsConectado ? '#28a745' : '#dc3545'}`,
										borderRadius: '8px',
										display: 'flex',
										alignItems: 'center',
										gap: '10px'
									}}>
										<span style={{
											width: '12px',
											height: '12px',
											borderRadius: '50%',
											background: wsConectado ? '#28a745' : '#dc3545',
											display: 'inline-block',
											animation: wsConectado ? 'none' : 'pulse 1.5s infinite'
										}}></span>
										<span style={{ fontSize: '14px', fontWeight: '600', color: wsConectado ? '#155724' : '#721c24' }}>
											WebSocket: {wsConectado ? 'Conectado ✅' : 'Desconectado ❌'}
										</span>
										{!wsConectado && (
											<button
												onClick={handleConectarWsPlanificacion}
												style={{
													padding: '4px 12px',
													fontSize: '12px',
													background: '#007bff',
													color: 'white',
													border: 'none',
													borderRadius: '4px',
													cursor: 'pointer'
												}}
											>
												Reconectar
											</button>
										)}
									</div>

									{/* Selector de fecha de inicio */}
									<div className="form-group" style={{ margin: 0 }}>
										<label className="form-label" htmlFor="fecha-inicio">
											Fecha de Inicio:
										</label>
										<input
											type="date"
											id="fecha-inicio"
											className="date-input"
											value={fechaInicioSimulacion}
											onChange={(e) => setFechaInicioSimulacion(e.target.value)}
										/>
									</div>
									{/* Panel de información de tiempo */}
									<div style={{
										background: '#f8f9fa',
										borderRadius: '8px',
										padding: '15px 20px',
										border: '1px solid #dee2e6',
										flex: 1,
										minWidth: '400px'
									}}>
										<div style={{
											display: 'flex',
											gap: '30px',
											flexWrap: 'wrap'
										}}>
											<div>
												<span style={{ fontSize: '14px', color: '#6c757d', marginRight: '8px' }}>
													Fecha y hora de simulación:
												</span>
												<span style={{ fontSize: '14px', fontWeight: '600', color: '#212529' }}>
													{tiempoSimulacionActual ? 
														new Date(tiempoSimulacionActual).toLocaleString('es-ES', {
															timeZone: 'UTC',
															day: '2-digit',
															month: '2-digit',
															year: 'numeric',
															hour: '2-digit',
															minute: '2-digit',
															second: '2-digit',
															hour12: false
														})
														: fechaInicioSimulacion ? 
														new Date(fechaInicioSimulacion).toLocaleString('es-ES', {
															timeZone: 'UTC',
															day: '2-digit',
															month: '2-digit',
															year: 'numeric',
															hour: '2-digit',
															minute: '2-digit',
															second: '2-digit',
															hour12: false
														}) 
														: '--:--:--'
													}
												</span>
											</div>
											<div>
												<span style={{ fontSize: '14px', color: '#6c757d', marginRight: '8px' }}>
													Tiempo transcurrido:
												</span>
												<span style={{ fontSize: '14px', fontWeight: '600', color: '#212529' }}>
													{tiempoRealTranscurrido > 0 ? (
														<>
															{Math.floor(tiempoRealTranscurrido / 60)}m {tiempoRealTranscurrido % 60}s
														</>
													) : '0s'}
												</span>
												<span style={{ fontSize: '12px', color: '#6c757d', marginLeft: '8px' }}>
													({estadoPlanificacion === 'running' || estadoPlanificacion === 'waiting' ? 'En ejecución' : 'Detenido'})
												</span>
											</div>
										</div>
									</div>

									{/* Botones de control */}
									<div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
										<div className="status-container" style={{ marginBottom: 0 }}>
											<span className="status-label">Estado:</span>
											<div className="status-indicator">
												<span className={`status-dot ${simulacionActiva ? "active" : "stopped"}`} />
												<span className="status-text">{simulacionActiva ? 'Ejecutándose' : 'Detenida'}</span>
											</div>
										</div>
										
										{/* Indicador de planificación WebSocket en tiempo real */}
										{(estadoPlanificacion === 'running' || estadoPlanificacion === 'waiting') && (
											<div style={{
												padding: '8px 12px',
												background: '#d1ecf1',
												border: '1px solid #17a2b8',
												borderRadius: '6px',
												display: 'flex',
												alignItems: 'center',
												gap: '8px',
												fontSize: '13px',
												color: '#0c5460'
											}}>
												<span style={{ 
													display: 'inline-block',
													width: '12px',
													height: '12px',
													borderRadius: '50%',
													border: '2px solid currentColor',
													borderTopColor: 'transparent',
													animation: 'spin 1s linear infinite'
												}}></span>
												{`⏱️ Planificación continua... (Iteración ${intentosRealizados})`}
											</div>
										)}
										{/*contoles de simulacion*/}
										<div className="simulation-controls">
											<div className="control-buttons" style={{ display: 'flex', gap: '10px' }}>
												<button
													onClick={handleIniciarSimulacion}
													disabled={simulacionActiva}
													style={{
														padding: '8px 16px',
														borderRadius: '6px',
														border: simulacionActiva ? '1px solid #e9ecef' : '1px solid #28a745',
														background: simulacionActiva ? '#e9ecef' : '#28a745',
														color: simulacionActiva ? '#6c757d' : 'white',
														fontSize: '14px',
														fontWeight: '500',
														cursor: simulacionActiva ? 'not-allowed' : 'pointer',
														display: 'flex',
														alignItems: 'center',
														gap: '8px',
														transition: 'all 0.2s'
													}}
												>	
													<FaPlay size={18} />
													{startButtonLabel}
												</button>
												<button
													onClick={handleDetenerSimulacion}
													style={{
														padding: '8px 16px',
														borderRadius: '6px',
														border: simulacionActiva ? '1px solid #dc3545': '1px solid #e9ecef',
														background: simulacionActiva ? '#dc3545' : '#e9ecef',
														color: simulacionActiva ? 'white' : '#6c757d',
														fontSize: '14px',
														fontWeight: '500',
														cursor: simulacionActiva ? 'pointer' : 'not-allowed',
														display: 'flex',
														alignItems: 'center',
														gap: '8px',
														transition: 'all 0.2s'
													}}
												>
													<FaStop size={18} />
													Detener
												</button>
												<button
													onClick={handleResetSimulacion}
													disabled={planFixed.length === 0}
													style={{
														padding: '8px 16px',
														borderRadius: '6px',
														border: planFixed.length === 0 ? '#e9ecef' : '1px solid #6c757d',
														background: planFixed.length === 0 ? '#e9ecef' : '#6c757d',
														color: planFixed.length === 0 ? '#adb5bd' : 'white',
														fontSize: '14px',
														fontWeight: '500',
														cursor: planFixed.length === 0 ? 'not-allowed' : 'pointer',
														transition: 'all 0.2s',
														display: 'flex',
														alignItems: 'center',
														gap: '8px'
													}}
												>
													<RiResetLeftFill size={18} />
													Reiniciar
												</button>
											</div>
										</div>

										{/* Controles de planificación WebSocket */}
										{wsConectado && (
											<div style={{ display: 'flex', gap: '10px', marginLeft: '20px', paddingLeft: '20px', borderLeft: '2px solid #dee2e6' }}>
												<button
													onClick={handleDetenerPlanificacion}
													disabled={estadoPlanificacion !== 'running'}
													style={{
														padding: '8px 16px',
														borderRadius: '6px',
														border: estadoPlanificacion === 'running' ? '1px solid #ffc107' : '1px solid #e9ecef',
														background: estadoPlanificacion === 'running' ? '#ffc107' : '#e9ecef',
														color: estadoPlanificacion === 'running' ? 'white' : '#6c757d',
														fontSize: '14px',
														fontWeight: '500',
														cursor: estadoPlanificacion === 'running' ? 'pointer' : 'not-allowed',
														transition: 'all 0.2s',
														display: 'flex',
														alignItems: 'center',
														gap: '8px'
													}}
												>
													<FaStop size={16} />
													Detener Planificación
												</button>
												<button
													onClick={handleLimpiarMapa}
													disabled={flights.length === 0}
													style={{
														padding: '8px 16px',
														borderRadius: '6px',
														border: flights.length > 0 ? '1px solid #17a2b8' : '1px solid #e9ecef',
														background: flights.length > 0 ? '#17a2b8' : '#e9ecef',
														color: flights.length > 0 ? 'white' : '#6c757d',
														fontSize: '14px',
														fontWeight: '500',
														cursor: flights.length > 0 ? 'pointer' : 'not-allowed',
														transition: 'all 0.2s',
														display: 'flex',
														alignItems: 'center',
														gap: '8px'
													}}
												>
													🧹 Limpiar Mapa
												</button>
											</div>
										)}
									</div>
								</div>
							</div>
						</div>


						{/* Mapa interactivo */}
						<div className="map-container">
							<MapContainer center={[20.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10} zoomControl={true} doubleClickZoom={true} boxZoom={true} keyboard={true} touchZoom={true}>
								<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} />
								<DynamicMarkers flights={flights} airports={airports} activeView={activeView} showRoutes={showRoutes} />
							</MapContainer>
							{/* Botón de leyenda flotante */}
							<LegendButton onClick={() => setShowLegend(true)} />
						</div>
					</div>
				</div>
			</div>
			{/* Diálogo de Leyenda */}
			<LegendDialog
				open={showLegend}
				onClose={() => setShowLegend(false)}
			/>
		</div>
	);
};

export default SimuladorSemanal;
