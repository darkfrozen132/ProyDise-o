import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
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
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import vuelosSemana from '../../../assets/data/vuelosSemana.json';
import { getPlanificacionSemanal } from '../../../config/api';
import './SimuladorSemanal.css';
import './WebSocketStomp.css';
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
// 🆕 COMPONENTE DE INDICADOR DE ESTADO WEBSOCKET
// Nota: usePlanificacionWebSocket está deshabilitado - ver comentario en línea ~513
import WebSocketStatusIndicator from '../../../components/ui/WebSocketStatusIndicator';

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
/**
 * Determinar color del avión basado en el PORCENTAJE DE CARGA
 * - Azul: < 40% de capacidad usada (poco cargado)
 * - Amarillo: 40-60% de capacidad usada (carga media)
 * - Rojo: > 60% de capacidad usada (muy cargado)
 */
const getAircraftColorByStatus = (flight) => {
	// Calcular porcentaje de carga
	const capacidad = flight.packageCapacity || 1; // Evitar división por 0
	const cargaActual = flight.currentPackages || 0;
	const porcentajeCarga = (cargaActual / capacidad) * 100;
	
	// Colores según porcentaje de carga
	if (porcentajeCarga > 60) {
		return '#ef4444'; // 🔴 Rojo - Muy cargado (> 60%)
	} else if (porcentajeCarga >= 40) {
		return '#f59e0b'; // 🟡 Amarillo - Carga media (40-60%)
	} else {
		return '#3b82f6'; // 🔵 Azul - Poco cargado (< 40%)
	}
};

const createAirplaneIcon = (flight, rotation = 0) => {
	// Determinar color basado en el estado del vuelo
	const color = getAircraftColorByStatus(flight);
	
	const iconSvg = `<svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="11" cy="11" rx="2" ry="10" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="8" rx="9" ry="1.8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="15" rx="4" ry="1.2" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<path d="M11 17 L11 19.5 L10 19.5 L10 17 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
		</svg>`;

	/* Crear divIcon con el SVG correspondiente y transición suave */
	return L.divIcon({
		html: `<div style="transform: rotate(${rotation}deg); display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); transition: transform 0.3s ease-out, filter 0.3s ease-out;">${iconSvg}</div>`,
		className: 'airplane-icon-animated',
		iconSize: [22, 22],
		iconAnchor: [11, 11],
		popupAnchor: [0, -12]
	});
};

/**
 * Crear popup detallado para un vuelo
 */
const createFlightPopup = (flight) => {
	const statusIcon = 
		flight.status === 'completed' || flight.progress >= 100 ? '✅' :
		flight.status === 'delayed' || flight.retrasado ? '🔴' : '🔵';
	
	const statusText = 
		flight.status === 'completed' || flight.progress >= 100 ? 'Completado' :
		flight.status === 'delayed' || flight.retrasado ? 'Retrasado' : 'En curso';
	
	const color = getAircraftColorByStatus(flight);
	
	return `
		<div style="min-width: 200px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
			<div style="background: linear-gradient(135deg, ${color}dd 0%, ${color}aa 100%); color: white; padding: 8px 12px; margin: -10px -10px 10px -10px; border-radius: 4px 4px 0 0;">
				<strong style="font-size: 15px;">✈️ ${flight.id}</strong>
			</div>
			
			<div style="padding: 4px 0;">
				<div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #e5e7eb;">
					<div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">
						<strong>Ruta:</strong>
					</div>
					<div style="font-size: 14px; font-weight: 600; color: #1f2937;">
						${flight.origin?.code || 'N/A'} → ${flight.destination?.code || 'N/A'}
					</div>
					${flight.origin?.region && flight.destination?.region ? 
						`<div style="font-size: 11px; color: #9ca3af; margin-top: 2px;">
							${flight.isSameContinentFlight ? '🌍 Mismo continente' : '🌏 Intercontinental'}
						</div>` : ''
					}
				</div>
				
				<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
					<div>
						<div style="font-size: 11px; color: #6b7280;">Progreso</div>
						<div style="font-size: 14px; font-weight: 600; color: #1f2937;">
							${Math.round(flight.progress || 0)}%
						</div>
					</div>
					<div>
						<div style="font-size: 11px; color: #6b7280;">Estado</div>
						<div style="font-size: 13px; font-weight: 600;">
							${statusIcon} ${statusText}
						</div>
					</div>
				</div>
				
				${flight.speed ? 
					`<div style="margin-bottom: 6px;">
						<div style="font-size: 11px; color: #6b7280;">Velocidad</div>
						<div style="font-size: 13px; color: #1f2937;">${flight.speed} km/h</div>
					</div>` : ''
				}
				
				${flight.altitude ? 
					`<div style="margin-bottom: 6px;">
						<div style="font-size: 11px; color: #6b7280;">Altitud</div>
						<div style="font-size: 13px; color: #1f2937;">${flight.altitude.toLocaleString()} ft</div>
					</div>` : ''
				}
				
				${flight.packageType ? 
					`<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #e5e7eb;">
						<div style="font-size: 11px; color: #6b7280;">Tipo de paquete</div>
						<div style="font-size: 13px; color: #1f2937; font-weight: 500;">
							${flight.packageType === 'AG' ? '📦 Algoritmo Genético' : 
							  flight.packageType === 'Inicial' ? '🎯 Planificación Inicial' : 
							  flight.packageType}
						</div>
						${flight.currentPackages !== undefined && flight.packageCapacity !== undefined ?
							`<div style="font-size: 12px; color: #6b7280; margin-top: 2px;">
								Capacidad: ${flight.currentPackages}/${flight.packageCapacity} paquetes
							</div>` : ''
						}
					</div>` : ''
				}
				
				${flight.pedidoId ? 
					`<div style="margin-top: 6px; font-size: 11px; color: #6b7280;">
						Pedido: <span style="font-family: monospace; color: #1f2937;">${flight.pedidoId}</span>
					</div>` : ''
				}
			</div>
		</div>
	`;
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
function DynamicMarkers({ flights, airports, activeView, showRoutes, vuelosEnMovimiento }) {
	const map = (0, require('react-leaflet').useMap)();
	const markersRef = React.useRef({}); // Guardar marcadores por ID para animarlos
	const polylinesRef = React.useRef({});
	const lastLogRef = React.useRef({ count: 0, time: 0, activeCount: 0 }); // 🚀 Throttle para logs
	
	/* Actualizar marcadores cuando cambian vuelos, aeropuertos, vista activa o rutas */
	React.useEffect(() => {
		// 🚀 Log solo cada 5 segundos o si cambió cantidad de vuelos
		const now = Date.now();
		if (flights.length !== lastLogRef.current.count || now - lastLogRef.current.time > 5000) {
			console.log(`🗺️ DynamicMarkers - Recibidos ${flights.length} vuelos, activeView: ${activeView}`);
			lastLogRef.current = { count: flights.length, time: now };
		}
		
		const airportMarkers = [];
		
		// Limpiar marcadores de aeropuertos antiguos
		map.eachLayer(layer => { 
			if (layer instanceof L.Marker && layer.options.isAirport) {
				map.removeLayer(layer); 
			}
		});
		
		/* Añadir marcadores de aeropuertos si la vista es 'airports' o 'flights' */
		if (activeView === 'airports' || activeView === 'flights') {
			console.log(`🏢 Añadiendo ${airports.length} aeropuertos al mapa`);
			airports.forEach(airport => {
				const isUnlimited = airport.capacity === 'ILIMITADO';
				const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
				const icon = createAirportIcon(airport.isSede, saturation);
				const marker = L.marker([airport.lat, airport.lng], { 
					icon,
					isAirport: true // Flag para identificar
				}).bindPopup(`<div class="popup-content"><div class="popup-header"><strong class="popup-title ${airport.isSede ? 'sede-title' : 'airport-title'}">${airport.name}</strong></div></div>`);
				marker.addTo(map); 
				airportMarkers.push(marker);
			});
		}
		
		/* Actualizar o crear marcadores de vuelos con animación (SISTEMA REACTIVO) */
		if (activeView === 'flights' || activeView === 'routes') {
			// 🆕 FILTRAR: Solo mostrar vuelos ACTIVOS (en vuelo), no 'waiting' ni 'completed'
			const vuelosActivos = vuelosEnMovimiento.filter(v => 
				v.status === 'active' && v.progress > 0 && v.progress < 100
			);
			// 🚀 Log solo si cambió la cantidad (evitar spam)
			if (vuelosActivos.length !== lastLogRef.current.activeCount) {
				console.log(`✈️ Aviones en vuelo: ${vuelosActivos.length}/${vuelosEnMovimiento.length}`);
				lastLogRef.current.activeCount = vuelosActivos.length;
			}
			
			const currentFlightIds = new Set();
			
			vuelosActivos.forEach((flight, index) => {
				// Verificar que las coordenadas sean válidas
				if (!flight.currentLat || !flight.currentLng || 
						isNaN(flight.currentLat) || isNaN(flight.currentLng)) {
					return;
				}
				
				currentFlightIds.add(flight.id);
				const existingMarker = markersRef.current[flight.id];
				
				// ✅ Usar posiciones pre-calculadas del useMemo
				const position = { lat: flight.currentLat, lng: flight.currentLng };
				const progress = flight.progress || 0;
				const status = flight.status || 'active';
				
				if (existingMarker) {
					// 🎬 ANIMAR: Mover marcador existente suavemente a nueva posición
					const currentLatLng = existingMarker.getLatLng();
					const newLatLng = L.latLng(position.lat, position.lng);
					
					// 🎬 Animar solo si el cambio es significativo (> 100m)
					// Esto suaviza correcciones del backend o saltos temporales
					const distance = currentLatLng.distanceTo(newLatLng);
					if (distance > 100) {
						animateMarker(existingMarker, currentLatLng, newLatLng, 1000); // 1 segundo de animación
					} else {
						existingMarker.setLatLng(newLatLng); // actualización instantánea
					}
					
					// Actualizar ícono (ahora con estado y color dinámicos)
					const newIcon = createAirplaneIcon(flight, flight.rotation);
					existingMarker.setIcon(newIcon);
					
					// Actualizar popup con información detallada y actualizada
					const popupContent = createFlightPopup(flight);
					existingMarker.setPopupContent(popupContent);
					
				} else {
					// 🆕 CREAR: Nuevo marcador para este vuelo
					// 🚀 Log deshabilitado para rendimiento
					// console.log(`  ✈️ Vuelo nuevo: ${flight.id}`);
					
					const icon = createAirplaneIcon(flight, flight.rotation);
					const popupContent = createFlightPopup(flight);
					
					const marker = L.marker([position.lat, position.lng], { 
						icon,
						isFlight: true // Flag para identificar
					}).bindPopup(popupContent);
					
					marker.addTo(map);
					markersRef.current[flight.id] = marker;
				}
				
				// Actualizar o crear polyline de ruta
				if (showRoutes) {
					const routeKey = `${flight.origin.lat},${flight.origin.lng}-${flight.destination.lat},${flight.destination.lng}`;
					
					if (!polylinesRef.current[routeKey]) {
						const polyline = L.polyline(
							[[flight.origin.lat, flight.origin.lng], [flight.destination.lat, flight.destination.lng]], 
							{ color: '#888', weight: 2, opacity: 0.6, dashArray: '5, 5' }
						);
						polyline.addTo(map);
						polylinesRef.current[routeKey] = polyline;
					}
				}
			});
			
			// Remover marcadores de vuelos que ya no existen
			Object.keys(markersRef.current).forEach(flightId => {
				if (!currentFlightIds.has(flightId)) {
					map.removeLayer(markersRef.current[flightId]);
					delete markersRef.current[flightId];
				}
			});
			
			// Limpiar polylines si showRoutes está desactivado
			if (!showRoutes) {
				Object.values(polylinesRef.current).forEach(polyline => map.removeLayer(polyline));
				polylinesRef.current = {};
			}
			
			// 🚀 Log de marcadores deshabilitado para rendimiento
			// console.log(`✅ Marcadores activos: ${Object.keys(markersRef.current).length}`);
		} else {
			// Si no estamos en vista de vuelos, limpiar todos los marcadores de vuelos
			Object.values(markersRef.current).forEach(marker => map.removeLayer(marker));
			markersRef.current = {};
			
			Object.values(polylinesRef.current).forEach(polyline => map.removeLayer(polyline));
			polylinesRef.current = {};
		}
		
		/* Limpiar marcadores de aeropuertos al desmontar */
		return () => { 
			airportMarkers.forEach(m => map.removeLayer(m)); 
		};
	}, [vuelosEnMovimiento, airports, activeView, showRoutes, map]); // 🎯 USA vuelosEnMovimiento
	
	return null;
}

/**
 * 🆕 SISTEMA HÍBRIDO: Calcular posición interpolada basada en tiempo
 * Inspirado en MapaVuelos.tsx - Interpolación lineal continua
 */
function calculateInterpolatedPosition(vuelo, tiempoActualMs) {
	// Verificar que el vuelo tenga timestamps
	if (!vuelo.fechaInicial || !vuelo.fechaFinal) {
		console.warn(`⚠️ Vuelo ${vuelo.id} sin timestamps - usando fallback`);
		// Fallback: retornar posición actual
		return {
			lat: vuelo.currentLat || vuelo.origin?.lat,
			lng: vuelo.currentLng || vuelo.origin?.lng,
			progress: vuelo.progress || 0,
			status: vuelo.status || 'active'
		};
	}

	// 🆕 FIX: Forzar interpretación como UTC si no tiene 'Z'
	let fechaInicialStr = vuelo.fechaInicial;
	let fechaFinalStr = vuelo.fechaFinal;
	if (typeof fechaInicialStr === 'string' && !fechaInicialStr.endsWith('Z')) {
		fechaInicialStr = fechaInicialStr + 'Z';
	}
	if (typeof fechaFinalStr === 'string' && !fechaFinalStr.endsWith('Z')) {
		fechaFinalStr = fechaFinalStr + 'Z';
	}

	// Extraer timestamps
	const horaSalida = new Date(fechaInicialStr).getTime();
	const horaLlegada = new Date(fechaFinalStr).getTime();
	const duracionVuelo = horaLlegada - horaSalida;
	
	// 🐛 DEBUG: Log muy reducido para no saturar consola (🚀 Optimizado)
	// Deshabilitado en producción - descomentar para debug
	/*
	if (Math.random() < 0.005) { // 0.5% de probabilidad
		const tiempoActual = new Date(tiempoActualMs);
		const enVuelo = tiempoActualMs >= horaSalida && tiempoActualMs < horaLlegada;
		console.log(`🔍 ${vuelo.id}: ${enVuelo ? '✈️' : '⏸️'} ${(duracionVuelo / 1000 / 60).toFixed(0)}min`);
	}
	*/

	// Si el vuelo no ha empezado, está en origen
	if (tiempoActualMs < horaSalida) {
		return {
			lat: vuelo.origin.lat,
			lng: vuelo.origin.lng,
			progress: 0,
			status: 'waiting'
		};
	}

	// Si el vuelo ya terminó, está en destino
	if (tiempoActualMs >= horaLlegada) {
		return {
			lat: vuelo.destination.lat,
			lng: vuelo.destination.lng,
			progress: 100,
			status: 'completed'
		};
	}

	// Calcular progreso (0-100%)
	const tiempoTranscurrido = tiempoActualMs - horaSalida;
	const progreso = (tiempoTranscurrido / duracionVuelo) * 100;
	const ratio = progreso / 100;

	// Interpolación lineal entre origen y destino
	const lat = vuelo.origin.lat + (vuelo.destination.lat - vuelo.origin.lat) * ratio;
	const lng = vuelo.origin.lng + (vuelo.destination.lng - vuelo.origin.lng) * ratio;

	// 🐛 DEBUG deshabilitado para rendimiento
	// if (Math.random() < 0.02) { console.log(`   ✈️ Progreso: ${progreso.toFixed(1)}%`); }

	return {
		lat,
		lng,
		progress: progreso,
		status: 'active'
	};
}

/**
 * Animar movimiento suave de marcador entre dos posiciones
 * También actualiza la rotación del avión en la dirección del movimiento
 */
function animateMarker(marker, startLatLng, endLatLng, duration = 1000) {
	const startTime = Date.now();
	const startLat = startLatLng.lat;
	const startLng = startLatLng.lng;
	const endLat = endLatLng.lat;
	const endLng = endLatLng.lng;
	
	// Calcular el ángulo de rotación basado en la dirección del movimiento
	const rotation = bearingDegrees(startLat, startLng, endLat, endLng);
	
	function frame() {
		const elapsed = Date.now() - startTime;
		const progress = Math.min(elapsed / duration, 1);
		
		// Easing suave (ease-out cúbico) - más realista para movimiento de aviones
		const eased = 1 - Math.pow(1 - progress, 3);
		
		const currentLat = startLat + (endLat - startLat) * eased;
		const currentLng = startLng + (endLng - startLng) * eased;
		
		marker.setLatLng([currentLat, currentLng]);
		
		if (progress < 1) {
			requestAnimationFrame(frame);
		}
	}
	
	requestAnimationFrame(frame);
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
	const [horaInicioSimulacion, setHoraInicioSimulacion] = useState("00:00"); // la hora que envías (formato HH:mm)
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
	const airportsRef = useRef([]); // 🔥 Ref para tener siempre el valor actual de airports
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
	// ⚠️ NOTA: El hook usePlanificacionWebSocket está DESHABILITADO porque el backend
	// no tiene el endpoint /ws/planificacion registrado. La simulación usa STOMP en /ws.
	// Para habilitar, registrar PlanificacionWebSocketHandler en WebSocketConfig.java
	
	// Variables placeholder para compatibilidad (el hook no se usa)
	const wsConnected = false;
	const iniciarPlanificacion = async () => { console.warn('⚠️ WebSocket planificación no disponible'); };
	const wsLimpiarIteraciones = () => {};
	const wsDetenerPlanificacion = () => {};
	const wsOnMessage = () => {};
	
	// Estados locales de planificación (para control local de UI)
	const [iteracionesPlanificacion, setIteracionesPlanificacion] = useState([]);
	const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle');
	
	// Estados adicionales que no están en el hook
	const [autoInicioIntentado, setAutoInicioIntentado] = useState(false);
	const [intentosRealizados, setIntentosRealizados] = useState(0); // Contador de reintentos
	const [tiempoRealTranscurrido, setTiempoRealTranscurrido] = useState(0); // Tiempo real en segundos
	const tiempoInicioRef = useRef(null); // Momento en que se inició la planificación
	const intervalTiempoRealRef = useRef(null); // Intervalo para actualizar tiempo real
	const [tiempoSimulacionActual, setTiempoSimulacionActual] = useState(null); // Hora de simulación del backend

	// ==================== NUEVO: ESTADO WEBSOCKET STOMP (SIMULACIÓN SEMANAL) ====================
	const [wsStompConectado, setWsStompConectado] = useState(false);
	const [sessionId, setSessionId] = useState(null);
	const [progresoAG, setProgresoAG] = useState(null);
	const [mensajesSimulacion, setMensajesSimulacion] = useState([]);
	const [estadoSimulacionStomp, setEstadoSimulacionStomp] = useState('disconnected'); // disconnected, connecting, connected, running, completed, error
	const stompClientRef = useRef(null);
	const subscriptionRef = useRef(null);

	// ==================== SISTEMA DE COLA Y RELOJ LOCAL ====================
	const [colaVuelos, setColaVuelos] = useState([]);           // Buffer de vuelos pendientes del WebSocket
	const [vuelosEnAire, setVuelosEnAire] = useState([]);       // Vuelos activos (procesándose en animación)
	const [relojLocal, setRelojLocal] = useState(null);         // Reloj de simulación local (independiente)
	const [kActual, setKActual] = useState(500);                // Factor K actual (adaptable)
	const [kBase] = useState(500);                               // Factor K base (constante)
	const [modoRalentizado, setModoRalentizado] = useState(false); // Indica si la simulación está ralentizada
	const [simulacionLocalActiva, setSimulacionLocalActiva] = useState(false); // Si la animación local corre
	const relojLocalRef = useRef(null);                          // Ref para el reloj local (evita closures)
	const colaVuelosRef = useRef([]);                            // Ref para la cola (evita closures)
	const TICK_REAL_MS = 250;                                    // Intervalo de actualización en ms (🚀 Optimizado: 4 FPS)
	
	// ========== BUFFER DE 20 SEGUNDOS PARA DAR VENTAJA AL BACKEND ==========
	const [bufferActivo, setBufferActivo] = useState(false);     // Si el buffer está activo (primeros 20 segundos)
	const vuelosBufferRef = useRef([]);                          // Vuelos acumulados durante el buffer
	const bufferTimeoutRef = useRef(null);                       // Timeout para finalizar buffer
	const BUFFER_DELAY_MS = 20000;                               // 🆕 20 segundos de buffer inicial
	const UMBRAL_COLA_BAJA = 3;                                  // Si cola < 3, ralentizar (no usado en modo constante)
	const FACTOR_RALENTIZADO = 0.25;                             // K se reduce a 25% cuando cola baja (no usado)

	// ========== SISTEMA DE TIEMPO SIMULADO (REACTIVO) ==========
	const [tiempoSimuladoBackend, setTiempoSimuladoBackend] = useState(null);
	const [ultimaActualizacionReal, setUltimaActualizacionReal] = useState(null);
	const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
	const [tiempoSimulado, setTiempoSimulado] = useState(Date.now());
	const [speedMultiplier, setSpeedMultiplier] = useState(DESIRED_TIME_SCALE); // 500x
	const tiempoSimuladoBackendRef = useRef(null); // 🆕 Ref para verificar si ya se inicializó

	// 🔥 Refs para las funciones de procesamiento (evitan circular dependencies)
	const procesarVuelosDirectosRef = useRef(null);
	const procesarRutasSimulacionRef = useRef(null);
	
	// 🆔 Contador único para generar IDs de vuelos (evita duplicados)
	const contadorVuelosRef = useRef(0);
	const procesarRutasSnapshotRef = useRef(null);
	const procesarSegmentsSnapshotRef = useRef(null); // 🆕 Para nueva estructura JSON
	
	// ✅ ELIMINADO: Refs de tiempo movidas a estado reactivo (ver línea ~215)
	
	// ==================== 📊 MÉTRICAS DE VUELOS ====================
	// Para diagnosticar si el backend envía vuelos tarde o si hay problemas de graficación
	const [metricasVuelos, setMetricasVuelos] = useState({
		totalRecibidosBackend: 0,      // Total de vuelos únicos recibidos del backend
		totalEnEstadoFlights: 0,       // Vuelos en el estado flights[] actual
		totalGraficados: 0,            // Vuelos que se están mostrando en el mapa
		totalPerdidosAntesDeTiempo: 0, // Vuelos que llegaron y ya terminaron antes de graficarse
		totalPerdidosDespuesDeTiempo: 0, // Vuelos que no empezaron porque el tiempo simulado ya pasó
		ultimaActualizacion: null      // Timestamp de última actualización
	});
	const vuelosRecibidosIdsRef = useRef(new Set()); // IDs únicos recibidos (para evitar contar duplicados)



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
				airportsRef.current = data; // 🔥 Actualizar ref
			} catch (error) {
				console.error('Error al cargar aeropuertos desde API:', error);
				console.log('Usando datos de fallback...');
				/* Fallback a datos estáticos en caso de error */
				const fallbackData = [
					{ name: 'Bruselas-Charleroi', code: 'EBCI', lat: 50.4592, lng: 4.4538, capacity: 'ILIMITADO', packages: 1200, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Lima-Jorge Chávez', code: 'SPIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 980, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
					{ name: 'Bogotá', code: 'SKBO', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 720, isSede: false, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
					{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 850, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 960, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
					// Agregar todos los aeropuertos que el backend envía
					{ name: 'Muscat', code: 'OOMS', lat: 23.5933, lng: 58.2844, capacity: 800, packages: 0, isSede: false, region: 'Asia', country: 'Omán', operationType: 'Aeropuerto Regional' },
					{ name: 'Brasilia', code: 'SBBR', lat: -15.8697, lng: -47.9206, capacity: 950, packages: 0, isSede: false, region: 'América del Sur', country: 'Brasil', operationType: 'Aeropuerto Regional' },
					{ name: 'Quito', code: 'SEQM', lat: -0.1277, lng: -78.3575, capacity: 750, packages: 0, isSede: false, region: 'América del Sur', country: 'Ecuador', operationType: 'Aeropuerto Regional' },
					{ name: 'New Delhi', code: 'VIDP', lat: 28.5562, lng: 77.1000, capacity: 1100, packages: 0, isSede: false, region: 'Asia', country: 'India', operationType: 'Aeropuerto Regional' },
					{ name: 'Amman', code: 'OJAI', lat: 31.7226, lng: 35.9932, capacity: 700, packages: 0, isSede: false, region: 'Asia', country: 'Jordania', operationType: 'Aeropuerto Regional' },
					{ name: 'Amsterdam-Schiphol', code: 'EHAM', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 0, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
					{ name: 'Prague', code: 'LKPR', lat: 50.1008, lng: 14.2600, capacity: 850, packages: 0, isSede: false, region: 'Europa', country: 'República Checa', operationType: 'Aeropuerto Regional' },
					{ name: 'Sana\'a', code: 'OYSN', lat: 15.4762, lng: 44.2189, capacity: 600, packages: 0, isSede: false, region: 'Asia', country: 'Yemen', operationType: 'Aeropuerto Regional' },
					{ name: 'Minsk', code: 'UMMS', lat: 53.8824, lng: 28.0307, capacity: 750, packages: 0, isSede: false, region: 'Europa', country: 'Bielorrusia', operationType: 'Aeropuerto Regional' },
					{ name: 'Porto Alegre', code: 'SGAS', lat: -29.9944, lng: -51.1714, capacity: 800, packages: 0, isSede: false, region: 'América del Sur', country: 'Brasil', operationType: 'Aeropuerto Regional' },
					{ name: 'Sofia', code: 'LBSF', lat: 42.6950, lng: 23.4114, capacity: 700, packages: 0, isSede: false, region: 'Europa', country: 'Bulgaria', operationType: 'Aeropuerto Regional' },
					{ name: 'Berlin-Tempelhof', code: 'EDDI', lat: 52.4726, lng: 13.4040, capacity: 900, packages: 0, isSede: false, region: 'Europa', country: 'Alemania', operationType: 'Aeropuerto Regional' },
					{ name: 'La Paz', code: 'SLLP', lat: -16.5133, lng: -68.1925, capacity: 650, packages: 0, isSede: false, region: 'América del Sur', country: 'Bolivia', operationType: 'Aeropuerto Regional' },
					{ name: 'Dubai', code: 'OMDB', lat: 25.2528, lng: 55.3644, capacity: 1300, packages: 0, isSede: false, region: 'Asia', country: 'EAU', operationType: 'Aeropuerto Regional' },
					{ name: 'Buenos Aires-Ezeiza', code: 'SABE', lat: -34.8222, lng: -58.5358, capacity: 1000, packages: 0, isSede: false, region: 'América del Sur', country: 'Argentina', operationType: 'Aeropuerto Regional' },
					{ name: 'Riyadh', code: 'OERK', lat: 24.9578, lng: 46.6987, capacity: 950, packages: 0, isSede: false, region: 'Asia', country: 'Arabia Saudita', operationType: 'Aeropuerto Regional' },
					{ name: 'Karachi', code: 'OPKC', lat: 24.9056, lng: 67.1608, capacity: 900, packages: 0, isSede: false, region: 'Asia', country: 'Pakistán', operationType: 'Aeropuerto Regional' },
					{ name: 'Vienna', code: 'LOWW', lat: 48.1103, lng: 16.5697, capacity: 950, packages: 0, isSede: false, region: 'Europa', country: 'Austria', operationType: 'Aeropuerto Regional' },
					{ name: 'Asunción', code: 'SUAA', lat: -25.2400, lng: -57.5194, capacity: 600, packages: 0, isSede: false, region: 'América del Sur', country: 'Paraguay', operationType: 'Aeropuerto Regional' },
					{ name: 'Tirana', code: 'LATI', lat: 41.4147, lng: 19.7206, capacity: 550, packages: 0, isSede: false, region: 'Europa', country: 'Albania', operationType: 'Aeropuerto Regional' },
					{ name: 'Zagreb', code: 'LDZA', lat: 45.7429, lng: 16.0688, capacity: 700, packages: 0, isSede: false, region: 'Europa', country: 'Croacia', operationType: 'Aeropuerto Regional' },
					{ name: 'Damascus', code: 'OSDI', lat: 33.4114, lng: 36.5156, capacity: 650, packages: 0, isSede: false, region: 'Asia', country: 'Siria', operationType: 'Aeropuerto Regional' },
					{ name: 'Baku', code: 'UBBB', lat: 40.4675, lng: 50.0467, capacity: 800, packages: 0, isSede: false, region: 'Asia', country: 'Azerbaiyán', operationType: 'Aeropuerto Regional' }
				];
				setAirports(fallbackData);
				airportsRef.current = fallbackData; // 🔥 Actualizar ref con fallback
				console.log('✅ Datos de fallback cargados:', fallbackData.length, 'aeropuertos');
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

	// ========== INTERVALO: Incrementar tiempoMovimiento ==========
	// ❌ DESACTIVADO: Ahora el reloj local controla todo el tiempo
	// Este sistema creaba conflictos con el reloj local
	/*
	useEffect(() => {
		if (!ultimaActualizacionReal || !tiempoSimuladoBackend) return;

		console.log('🕐 Iniciando intervalo de tiempo simulado');

		const interval = setInterval(() => {
			const tiempoReal = Date.now() - ultimaActualizacionReal;
			setTiempoMovimiento(tiempoReal);
		}, 50); // 20 FPS (cada 50ms)

		return () => {
			console.log('🛑 Deteniendo intervalo de tiempo simulado');
			clearInterval(interval);
		};
	}, [ultimaActualizacionReal, tiempoSimuladoBackend]);
	*/

	// ========== CALCULAR: Tiempo Simulado ==========
	// ❌ DESACTIVADO: Ahora el reloj local actualiza tiempoSimulado directamente
	// Este sistema creaba conflictos con el reloj local
	/*
	useEffect(() => {
		if (!tiempoSimuladoBackend) {
			setTiempoSimulado(Date.now());
			return;
		}

		// Calcular tiempo simulado: base + (tiempo_real × velocidad)
		const msSimuladosPasados = tiempoMovimiento * speedMultiplier;
		const nuevoTiempoSimulado = tiempoSimuladoBackend + msSimuladosPasados;
		
		setTiempoSimulado(nuevoTiempoSimulado);

		// Debug cada 2 segundos (solo algunos frames)
		if (Math.random() < 0.02) {
			const fechaSimulada = new Date(nuevoTiempoSimulado);
			console.log(`⏰ Tiempo simulado: ${fechaSimulada.toISOString()}`);
			console.log(`   Base: ${new Date(tiempoSimuladoBackend).toISOString()}`);
			console.log(`   Δ Real: ${(tiempoMovimiento / 1000).toFixed(1)}s`);
			console.log(`   Velocidad: ${speedMultiplier}x`);
		}
	}, [tiempoSimuladoBackend, tiempoMovimiento, speedMultiplier]);
	*/

	// ========== CALCULAR: Vuelos en Movimiento (REACTIVO) ==========
	const vuelosEnMovimiento = useMemo(() => {
		if (!tiempoSimulado || flights.length === 0) {
			return [];
		}

		// 🚀 Debug deshabilitado para rendimiento
		// if (Math.random() < 0.02) { console.log(`✈️ Re-calculando ${flights.length} vuelos`); }

		return flights.map(flight => {
			// Calcular posición interpolada basada en tiempo simulado
			const interpolated = calculateInterpolatedPosition(flight, tiempoSimulado);
			
			// Calcular rotación del avión
			const bearing = bearingDegrees(
				interpolated.lat,
				interpolated.lng,
				flight.destination.lat,
				flight.destination.lng
			);
			const rotation = (bearing - 90 + 360) % 360;

			return {
				...flight,
				currentLat: interpolated.lat,
				currentLng: interpolated.lng,
				progress: interpolated.progress,
				status: interpolated.status,
				rotation: rotation
			};
		});
	}, [flights, tiempoSimulado]); // 🎯 DEPENDENCIAS REACTIVAS

	// Debug: Cantidad de vuelos en el aire (🚀 log reducido para rendimiento)
	useEffect(() => {
		const enAire = vuelosEnMovimiento.filter(v => 
			v.status === 'active' && v.progress > 0 && v.progress < 100
		).length;
		
		// 📊 Contar por estado para diagnóstico
		const waiting = vuelosEnMovimiento.filter(v => v.status === 'waiting').length;
		const completed = vuelosEnMovimiento.filter(v => v.status === 'completed').length;
		const active = vuelosEnMovimiento.filter(v => v.status === 'active').length;
		
		if (enAire !== flightsInAir) {
			setFlightsInAir(enAire);
			// Log deshabilitado - la información ya se muestra en el UI
			// console.log(`🛫 Aviones: ${enAire}/${vuelosEnMovimiento.length}`);
		}
		
		// 📊 ACTUALIZAR MÉTRICAS de vuelos graficados con desglose por estado
		setMetricasVuelos(prev => ({
			...prev,
			totalEnEstadoFlights: flights.length,
			totalGraficados: enAire,
			// 🆕 Desglose por estado
			vuelosWaiting: waiting,
			vuelosCompleted: completed,
			vuelosActive: active
		}));
	}, [vuelosEnMovimiento, flightsInAir, flights.length]);

	// 📊 EFECTO: Mostrar métricas cada 10 segundos en consola
	useEffect(() => {
		if (!simulacionLocalActiva) return;
		
		const interval = setInterval(() => {
			const m = metricasVuelos;
			
			console.log(`\n📊 ========== MÉTRICAS DE VUELOS ==========`);
			console.log(`📨 Recibidos del backend:     ${m.totalRecibidosBackend}`);
			console.log(`📋 En estado flights[]:       ${m.totalEnEstadoFlights}`);
			console.log(`✈️  Graficados en mapa:        ${m.totalGraficados}`);
			console.log(`─────────────────────────────────────────`);
			console.log(`⏳ En espera (waiting):       ${m.vuelosWaiting || 0} ← no han despegado aún`);
			console.log(`� En vuelo (active):         ${m.vuelosActive || 0}`);
			console.log(`✅ Completados (completed):   ${m.vuelosCompleted || 0} ← ya aterrizaron`);
			console.log(`⚠️  Llegaron tarde:            ${m.totalPerdidosAntesDeTiempo}`);
			console.log(`📊 ==========================================\n`);
			
			// 🔴 DIAGNÓSTICO: Si hay vuelos pero ninguno graficado
			if (m.totalEnEstadoFlights > 0 && m.totalGraficados === 0) {
				if (m.vuelosWaiting > 0) {
					console.warn(`🔴 DIAGNÓSTICO: ${m.vuelosWaiting} vuelos esperando despegar. El tiempo simulado está ANTES de que empiecen los vuelos.`);
				}
				if (m.vuelosCompleted > 0) {
					console.warn(`🔴 DIAGNÓSTICO: ${m.vuelosCompleted} vuelos ya aterrizaron. El tiempo simulado está DESPUÉS de que terminaron.`);
				}
			}
		}, 10000); // Cada 10 segundos
		
		return () => clearInterval(interval);
	}, [simulacionLocalActiva, metricasVuelos]);

	/* ==================== VUELOS LOCALES DESHABILITADOS - SOLO WEBSOCKET ==================== */
	// ❌ COMENTADO: Ya no usamos vuelos locales basados en planFixed y simClock
	// ✅ AHORA: Todos los vuelos vienen del WebSocket mediante procesarRutasSimulacion()
	
	// useEffect(() => {
	// 	if (!simClock || planFixed.length === 0 || airports.length === 0) return;
	// 	... código comentado ...
	// }, [simClock, planFixed, airports]);

	/* ==================== RELOJ LOCAL DESHABILITADO - SOLO WEBSOCKET ==================== */
	// ❌ COMENTADO: Ya no avanzamos el reloj localmente
	// ✅ AHORA: El tiempo viene del backend en los mensajes WebSocket
	
	// useEffect(() => {
	// 	if (!simulacionActiva) return;
	// 	const advanceMs = DESIRED_TIME_SCALE * REAL_TICK_MS;
	// 	const id = setInterval(() => { ... }, REAL_TICK_MS);
	// 	return () => clearInterval(id);
	// }, [simulacionActiva]);

	// ==================== 🆕 RELOJ LOCAL CON VELOCIDAD CONSTANTE ====================
	// Sistema simplificado: Buffer inicial de 20 segundos, luego velocidad constante K=500
	// SIN sistema adaptativo - avance uniforme del tiempo
	const flightsInAirRef = useRef(0);
	
	// Actualizar ref cuando cambia flightsInAir
	useEffect(() => {
		flightsInAirRef.current = flightsInAir;
	}, [flightsInAir]);
	
	useEffect(() => {
		if (!simulacionLocalActiva || !relojLocalRef.current) return;
		
		// 🎯 VELOCIDAD CONSTANTE: Siempre usa K=500 (DESIRED_TIME_SCALE)
		// Sin adaptación basada en aviones visibles
		const K_CONSTANTE = DESIRED_TIME_SCALE; // 500x
		
		const interval = setInterval(() => {
			// Calcular milisegundos simulados por tick
			// Fórmula: msSimulados = (TICK_REAL_MS / 1000) * K * 1000 = TICK_REAL_MS * K
			const msSimulados = TICK_REAL_MS * K_CONSTANTE;
			
			// Avanzar el reloj local
			const nuevoTiempo = new Date(relojLocalRef.current.getTime() + msSimulados);
			
			relojLocalRef.current = nuevoTiempo;
			setRelojLocal(nuevoTiempo);
			setSimClock(nuevoTiempo);
			setTiempoSimulacionActual(nuevoTiempo.toISOString());
			
			// 🆕 IMPORTANTE: Actualizar tiempoSimulado para la interpolación de vuelos
			setTiempoSimulado(nuevoTiempo.getTime());
			
			// Debug cada 20 segundos aproximadamente (80 ticks @ 250ms)
			if (Math.random() < 0.0125) {
				const avionesEnPantalla = flightsInAirRef.current;
				console.log(`⏰ Reloj: ${nuevoTiempo.toISOString().slice(11,19)} | K=${K_CONSTANTE} | Aviones=${avionesEnPantalla}`);
			}
		}, TICK_REAL_MS);
		
		return () => clearInterval(interval);
	}, [simulacionLocalActiva]);



	// ==================== FUNCIONES PARA CONTROLAR SIMULACIÓN ====================
	const handleIniciarSimulacion = async () => {
		console.log("Iniciando simulación desde:", fechaInicioSimulacion, "a las", horaInicioSimulacion);
		
		// Validar fecha
		if (!fechaInicioSimulacion) {
			alert("Por favor, selecciona una fecha de inicio.");
			return;
		}

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

		// 🔥 NUEVA LÓGICA: Usar WebSocket STOMP para la simulación
		try {
			// Conectar WebSocket si no está conectado
			if (!wsStompConectado) {
				console.log('🔌 WebSocket no conectado. Conectando automáticamente...');
				conectarWebSocketStomp();
				// Esperar un momento para que se conecte
				await new Promise(resolve => setTimeout(resolve, 1500));
			}

			// Verificar conexión nuevamente
			if (!stompClientRef.current?.connected) {
				console.log('⏳ Esperando conexión WebSocket...');
				await new Promise(resolve => setTimeout(resolve, 1000));
			}

			// Limpiar estado anterior
			console.log('🧹 Limpiando estado anterior...');
			setFlights([]);
			setFlightsInAir(0);
			setProgresoAG(null);
			contadorVuelosRef.current = 0;
			
			// Iniciar reloj local
			const inicioUTC = new Date(`${fechaInicioSimulacion}T${horaInicioSimulacion}:00Z`);
			setSimClock(inicioUTC);
			simStartRef.current = inicioUTC;
			setTiempoRealMs(0);
			setTickActual(0);
			setTimeScale(DESIRED_TIME_SCALE);

			// 🆕 INICIAR SISTEMA DE COLA Y RELOJ LOCAL (pero NO activar aún)
			setRelojLocal(inicioUTC);
			relojLocalRef.current = inicioUTC;
			setColaVuelos([]);
			colaVuelosRef.current = [];
			setVuelosEnAire([]);
			setKActual(kBase);
			setModoRalentizado(false);
			// ⏸️ NO ACTIVAR RELOJ AÚN - Se activa después del buffer de 15 segundos
			setSimulacionLocalActiva(false);
			setTiempoSimulacionActual(inicioUTC.toISOString()); // Mostrar en UI
			
			// 🆕 INICIALIZAR tiempoSimulado con la fecha de inicio (para interpolación)
			setTiempoSimulado(inicioUTC.getTime());
			// Reiniciar refs de tiempo del backend
			tiempoSimuladoBackendRef.current = null;
			setTiempoSimuladoBackend(null);
			
			// 🆕 ACTIVAR BUFFER DE 15 SEGUNDOS para acumular vuelos iniciales
			console.log(`⏳ ACTIVANDO BUFFER DE ${BUFFER_DELAY_MS/1000} SEGUNDOS para acumular vuelos iniciales...`);
			setBufferActivo(true);
			vuelosBufferRef.current = [];
			
			// Limpiar timeout anterior si existe
			if (bufferTimeoutRef.current) {
				clearTimeout(bufferTimeoutRef.current);
			}
			
			// Después de 15 segundos, finalizar buffer y activar animación
			bufferTimeoutRef.current = setTimeout(() => {
				console.log(`✅ BUFFER COMPLETADO - ${vuelosBufferRef.current.length} vuelos acumulados`);
				setBufferActivo(false);
				
				// Procesar todos los vuelos acumulados
				if (vuelosBufferRef.current.length > 0) {
					console.log(`🚀 Procesando ${vuelosBufferRef.current.length} vuelos del buffer...`);
					// Combinar todos los vuelos en un solo array y setear flights
					setFlights(vuelosBufferRef.current);
					setFlightsInAir(vuelosBufferRef.current.filter(v => v.status === 'active').length);
					
					// 🚀 ACTIVAR RELOJ LOCAL
					setSimulacionLocalActiva(true);
					console.log(`🚀 Reloj local ACTIVADO - ${vuelosBufferRef.current.length} vuelos listos para animar`);
				} else {
					console.warn(`⚠️ Buffer vacío - no hay vuelos para animar, esperando más datos...`);
				}
				
				// Limpiar buffer
				vuelosBufferRef.current = [];
			}, BUFFER_DELAY_MS);

			// 🔄 Llamar al endpoint REST para iniciar simulación
			console.log('🚀 Llamando al backend para iniciar simulación...');
			const response = await fetch('http://localhost:8000/api/simulations/start', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					fecha: fechaInicioSimulacion,
					hora: horaInicioSimulacion,
					factorK: 5
				})
			});

			if (!response.ok) {
				throw new Error(`Error HTTP: ${response.status}`);
			}

			const data = await response.json();
			console.log('📨 Respuesta del servidor:', data);

			if (data.sessionId) {
				setSessionId(data.sessionId);
				
				// Suscribirse al topic si hay conexión STOMP
				if (stompClientRef.current?.connected) {
					const topicUrl = `/topic/simulations/${data.sessionId}`;
					console.log(`📡 Suscribiéndose a: ${topicUrl}`);

					const subscription = stompClientRef.current.subscribe(
						topicUrl,
						(message) => {
							try {
								const datos = JSON.parse(message.body);
								console.log('📨 Mensaje WebSocket:', datos);
								procesarMensajeSimulacion(datos);
							} catch (error) {
								console.error('❌ Error parseando mensaje:', error);
							}
						}
					);
					subscriptionRef.current = subscription;
				}
			}

			setSimulacionActiva(true);
			console.log('✅ Simulación iniciada correctamente');

		} catch (error) {
			console.error('❌ Error al iniciar simulación:', error);
			alert(`Error al iniciar simulación: ${error.message}`);
		}
	};

	const handleDetenerSimulacion = async () => {
		console.log("🛑 Deteniendo simulación...");
		setSimulacionActiva(false);
		
		// Cancelar simulación en el backend si hay sessionId
		if (sessionId) {
			try {
				const response = await fetch(`http://localhost:8000/api/simulations/${sessionId}/cancel`, {
					method: 'POST'
				});
				if (response.ok) {
					console.log('✅ Simulación cancelada en el servidor');
				}
			} catch (error) {
				console.error('⚠️ Error al cancelar en servidor:', error);
			}
		}

		// Desuscribirse del topic WebSocket
		if (subscriptionRef.current) {
			try {
				subscriptionRef.current.unsubscribe();
				subscriptionRef.current = null;
				console.log('📡 Desuscrito del topic');
			} catch (error) {
				console.error('⚠️ Error al desuscribirse:', error);
			}
		}

		// Limpiar estado local de animación
		setSimulacionLocalActiva(false);
		setColaVuelos([]);
		setVuelosEnAire([]);
		colaVuelosRef.current = [];
	};

	const handleResetSimulacion = () => {
		console.log("Simulación reiniciada completamente.");

		setSimulacionActiva(false);
		setSimClock(null);
		setTiempoRealMs(0);
		setTickActual(0);
		setFlights([]);
		setFlightsInAir(0);
		contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos
		
		// 📊 RESETEAR MÉTRICAS DE VUELOS
		setMetricasVuelos({
			totalRecibidosBackend: 0,
			totalEnEstadoFlights: 0,
			totalGraficados: 0,
			totalPerdidosAntesDeTiempo: 0,
			totalPerdidosDespuesDeTiempo: 0,
			ultimaActualizacion: null
		});
		vuelosRecibidosIdsRef.current = new Set();
		
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
		
		// Generar ID único con contador incremental (SIN Date.now() para evitar duplicados)
		contadorVuelosRef.current += 1;
		const idUnico = `PL-${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${contadorVuelosRef.current}`;
		
		return {
			id: idUnico,
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

	// ==================== FUNCIONES DE PLANIFICACIÓN ====================
	// Nota: handleConectarWsPlanificacion fue eliminada - ahora usamos el hook usePlanificacionWebSocket
	// que maneja la conexión automáticamente con reconexión y heartbeat

	const handleIniciarPlanificacion = () => {
		// ✨ Usar el nuevo hook para verificar conexión
		if (!wsConnected) {
			console.warn('⚠️ WebSocket no conectado, esperando...');
			alert('WebSocket no conectado. Espera un momento...');
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
		wsLimpiarIteraciones(); // ✨ También limpiar en el hook
		setFlights([]); // Limpiar vuelos del mapa
		setFlightsInAir(0);
		setIntentosRealizados(0);
		setEstadoPlanificacion('running');
		contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos
		
		// 🕐 INICIAR TIEMPO DE SIMULACIÓN (basado en fecha y hora elegida por usuario)
		const fechaSimulacion = new Date(`${fechaInicioSimulacion}T${horaInicioSimulacion}:00Z`);
		simStartRef.current = fechaSimulacion;
		setSimClock(fechaSimulacion);
		console.log('🕐 Tiempo de simulación iniciado en:', fechaSimulacion.toISOString());
		
		// 🕐 INICIAR CRONÓMETRO DE TIEMPO REAL (para UI)
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
		
		// ✨ Usar la función del hook para iniciar planificación
		iniciarPlanificacion(
			fechaInicioSimulacion,
			5, // Factor K para simulación semanal
			{
				tamanioPoblacion: 20,
				maxGeneraciones: 20,
				limiteGeneracionesSinMejora: 10,
				hora: horaInicioSimulacion
			}
		).then(sessionId => {
			console.log('✅ Planificación iniciada con ID:', sessionId);
		}).catch(error => {
			console.error('❌ Error al iniciar planificación:', error);
			setEstadoPlanificacion('error');
		});
	};

	/**
	 * Limpiar iteraciones de planificación
	 */
	const handleLimpiarIteraciones = () => {
		setIteracionesPlanificacion([]);
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false); // Permitir nuevo auto-inicio
		wsLimpiarIteraciones(); // También limpiar en el hook
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
		
		// Detener planificación en el hook
		wsDetenerPlanificacion();
		
		// Resetear estado
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false);
		
		console.log('✅ Planificación detenida. Vuelos permanecen en el mapa.');
	};

	/**
	 * Limpiar todos los vuelos del mapa
	 */
	const handleLimpiarMapa = () => {
		console.log('🧹 Limpiando mapa y reseteando simulación...');
		
		// Limpiar vuelos
		setFlights([]);
		setFlightsInAir(0);
		setIteracionesPlanificacion([]);
		setIntentosRealizados(0);
		contadorVuelosRef.current = 0;
		
		// ✅ RESETEAR TIEMPO SIMULADO
		setTiempoSimuladoBackend(null);
		tiempoSimuladoBackendRef.current = null; // 🆕 También resetear la ref
		setUltimaActualizacionReal(null);
		setTiempoMovimiento(0);
		setTiempoSimulado(Date.now());
		
		// Limpiar progreso
		setProgresoAG(null);
		setMensajesSimulacion([]);
		
		console.log('✅ Mapa limpiado y simulación reseteada');
	};

	// 🆕 EFECTO: Registrar callback para procesar mensajes del WebSocket mejorado
	useEffect(() => {
		wsOnMessage((data) => {
			console.log('📩 [Hook] Mensaje de planificación:', data);
			
			// Procesar tiempo simulado
			if (data.datos?.tiempoSimulacionActual) {
				let timestampStr = data.datos.tiempoSimulacionActual;
				if (typeof timestampStr === 'string' && !timestampStr.endsWith('Z')) {
					timestampStr = timestampStr + 'Z';
				}
				const timestampMs = typeof timestampStr === 'string' 
					? new Date(timestampStr).getTime() 
					: timestampStr;
				setTiempoSimulacionActual(data.datos.tiempoSimulacionActual);
				setTiempoSimuladoBackend(timestampMs);
				setTiempoSimulado(timestampMs);
				tiempoSimuladoBackendRef.current = timestampMs;
				setUltimaActualizacionReal(Date.now());
				setTiempoMovimiento(0);
			}
			
			// Procesar vuelos si vienen en la solución
			if (data.solucion?.vuelos && data.solucion.vuelos.length > 0) {
				console.log(`✈️ [Hook] Procesando ${data.solucion.vuelos.length} vuelos`);
				
				const vuelosParaMapa = data.solucion.vuelos
					.map(convertirVueloPlanificacionAMapa)
					.filter(v => v !== null);
				
				if (vuelosParaMapa.length > 0) {
					setFlights(prevFlights => {
						const flightsMap = new Map(prevFlights.map(f => [f.id, f]));
						vuelosParaMapa.forEach(vuelo => flightsMap.set(vuelo.id, vuelo));
						return Array.from(flightsMap.values());
					});
					setFlightsInAir(prev => prev + vuelosParaMapa.length);
				}
				
				// Si es tipo completado, solicitar siguiente iteración
				if (data.tipo === 'completado') {
					const nuevoIntento = intentosRealizados + 1;
					if (nuevoIntento < 1000) {
						setIntentosRealizados(nuevoIntento);
						setTimeout(() => {
							iniciarPlanificacion(fechaInicioSimulacion, 5, {
								tamanioPoblacion: 20,
								maxGeneraciones: 20,
								hora: horaInicioSimulacion
							});
						}, 100);
					}
				}
			}
		});
	}, [wsOnMessage, fechaInicioSimulacion, horaInicioSimulacion, intentosRealizados, iniciarPlanificacion]);

	// 🆕 Limpiar intervalo de tiempo real al desmontar
	useEffect(() => {
		return () => {
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
		if (wsConnected && fechaInicioSimulacion && !autoInicioIntentado && estadoPlanificacion === 'idle') {
			console.log('🚀 Auto-iniciando planificación...');
			setAutoInicioIntentado(true);
			
			// Pequeño delay para asegurar que la conexión esté estable
			setTimeout(() => {
				handleIniciarPlanificacion();
			}, 500);
		}
	}, [wsConnected, fechaInicioSimulacion, autoInicioIntentado, estadoPlanificacion]);

	// Resetear flag cuando cambia la fecha
	useEffect(() => {
		setAutoInicioIntentado(false);
	}, [fechaInicioSimulacion]);

	// ==================== FUNCIONES WEBSOCKET STOMP (SIMULACIÓN SEMANAL) ====================
	
	/**
	 * Conectar WebSocket STOMP
	 */
	const conectarWebSocketStomp = useCallback(() => {
		if (stompClientRef.current && stompClientRef.current.active) {
			console.log('⚠️ WebSocket STOMP ya está conectado');
			return;
		}

		console.log('📡 Conectando WebSocket STOMP...');
		setEstadoSimulacionStomp('connecting');

		const socket = new SockJS('http://localhost:8000/ws');
		
		const stompClient = new Client({
			webSocketFactory: () => socket,
			reconnectDelay: 5000,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,
			
			onConnect: () => {
				console.log('✅ WebSocket STOMP conectado');
				setWsStompConectado(true);
				setEstadoSimulacionStomp('connected');
				agregarMensaje('✅ Conexión WebSocket establecida', 'success');
			},
			
			onStompError: (frame) => {
				console.error('❌ Error STOMP:', frame.headers['message']);
				console.error('Detalles:', frame.body);
				setEstadoSimulacionStomp('error');
				agregarMensaje(`❌ Error STOMP: ${frame.headers['message']}`, 'error');
			},
			
			onWebSocketError: (error) => {
				console.error('❌ Error WebSocket:', error);
				setWsStompConectado(false);
				setEstadoSimulacionStomp('error');
				agregarMensaje('❌ Error de conexión WebSocket', 'error');
			},
			
			onDisconnect: () => {
				console.log('🔌 WebSocket STOMP desconectado');
				setWsStompConectado(false);
				setEstadoSimulacionStomp('disconnected');
				agregarMensaje('🔌 WebSocket desconectado', 'warning');
			}
		});

		stompClient.activate();
		stompClientRef.current = stompClient;
	}, []);

	/**
	 * Desconectar WebSocket STOMP
	 */
	const desconectarWebSocketStomp = useCallback(() => {
		if (subscriptionRef.current) {
			subscriptionRef.current.unsubscribe();
			subscriptionRef.current = null;
		}

		if (stompClientRef.current) {
			stompClientRef.current.deactivate();
			stompClientRef.current = null;
		}

		setWsStompConectado(false);
		setEstadoSimulacionStomp('disconnected');
		console.log('🔌 WebSocket STOMP desconectado completamente');
	}, []);

	// 🆔 Contador para IDs únicos de mensajes
	const contadorMensajesRef = useRef(0);
	
	/**
	 * Agregar mensaje al log
	 */
	const agregarMensaje = useCallback((mensaje, tipo = 'info') => {
		contadorMensajesRef.current += 1;
		const nuevoMensaje = {
			id: `msg-${Date.now()}-${contadorMensajesRef.current}-${Math.random().toString(36).substr(2, 9)}`,
			texto: mensaje,
			tipo, // success, error, warning, info
			timestamp: new Date().toLocaleTimeString('es-ES')
		};
		setMensajesSimulacion(prev => [nuevoMensaje, ...prev].slice(0, 50));
	}, []);

	/**
	 * Iniciar simulación semanal con WebSocket STOMP
	 */
	const iniciarSimulacionWebSocketStomp = useCallback(async () => {
		if (!fechaInicioSimulacion) {
			alert('Por favor, selecciona una fecha de inicio');
			return;
		}

		if (!wsStompConectado) {
			alert('WebSocket no conectado. Conectando...');
			conectarWebSocketStomp();
			return;
		}

		try {
			console.log('🚀 Iniciando simulación semanal con WebSocket STOMP...');
			
			// 🧹 LIMPIAR VUELOS ANTERIORES antes de iniciar nueva simulación
			console.log('🧹 Limpiando vuelos anteriores...');
			setFlights([]);
			setFlightsInAir(0);
			setProgresoAG(null);
			setMensajesSimulacion([]);
			contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos
			
			// 📊 RESETEAR MÉTRICAS DE VUELOS
			setMetricasVuelos({
				totalRecibidosBackend: 0,
				totalEnEstadoFlights: 0,
				totalGraficados: 0,
				totalPerdidosAntesDeTiempo: 0,
				totalPerdidosDespuesDeTiempo: 0,
				ultimaActualizacion: null
			});
			vuelosRecibidosIdsRef.current = new Set();
			
			setEstadoSimulacionStomp('running');
			agregarMensaje(`🚀 Iniciando simulación para ${fechaInicioSimulacion} a las ${horaInicioSimulacion}`, 'info');

			// 1. Llamar al endpoint REST para iniciar
			const response = await fetch('http://localhost:8000/api/simulations/start', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					fecha: fechaInicioSimulacion,
					hora: horaInicioSimulacion, // Hora de inicio (formato HH:mm)
					factorK: 5 // Factor K fijo para simulación semanal
				})
			});

			if (!response.ok) {
				throw new Error(`Error HTTP: ${response.status}`);
			}

			const data = await response.json();
			console.log('📨 Respuesta del servidor:', data);

			if (!data.sessionId) {
				throw new Error('No se recibió sessionId del servidor');
			}

			setSessionId(data.sessionId);
			agregarMensaje(`✅ Simulación iniciada - Session ID: ${data.sessionId}`, 'success');

			// 2. Suscribirse al topic del WebSocket
			const topicUrl = `/topic/simulations/${data.sessionId}`;
			console.log(`📡 Suscribiéndose a: ${topicUrl}`);

			const subscription = stompClientRef.current.subscribe(
				topicUrl,
				(message) => {
					try {
						const datos = JSON.parse(message.body);
						console.log('📨 Mensaje recibido:', datos);
						procesarMensajeSimulacion(datos);
					} catch (error) {
						console.error('❌ Error parseando mensaje:', error);
						agregarMensaje('❌ Error procesando mensaje del servidor', 'error');
					}
				}
			);

			subscriptionRef.current = subscription;
			agregarMensaje(`📡 Suscrito a: ${topicUrl}`, 'success');

		} catch (error) {
			console.error('❌ Error iniciando simulación:', error);
			setEstadoSimulacionStomp('error');
			agregarMensaje(`❌ Error: ${error.message}`, 'error');
			alert(`Error al iniciar simulación: ${error.message}`);
		}
	}, [fechaInicioSimulacion, wsStompConectado, conectarWebSocketStomp, agregarMensaje]);

	/**
	 * Procesar mensajes recibidos del WebSocket
	 */
	const procesarMensajeSimulacion = useCallback((datos) => {
		// Determinar tipo de mensaje
		if (datos.tipo === 'PROGRESO_AG') {
			// Mensaje de progreso del Algoritmo Genético
			console.log(`🧬 Progreso AG - Generación ${datos.generacion}/${datos.maxGeneraciones}`);
			
			// 🕐 ACTUALIZAR TIEMPO SIMULADO DEL BACKEND (convertir a timestamp)
			if (datos.fechaSimulada) {
				// 🆕 FIX: Forzar interpretación como UTC si no tiene 'Z'
				let fechaStr = datos.fechaSimulada;
				if (typeof fechaStr === 'string' && !fechaStr.endsWith('Z')) {
					fechaStr = fechaStr + 'Z';
				}
				const timestampSimulado = new Date(fechaStr).getTime();
				
				// ✅ ACTUALIZAR ESTADO Y REF
				setTiempoSimuladoBackend(timestampSimulado);
				setTiempoSimulado(timestampSimulado); // 🆕 CRÍTICO: Sincronizar tiempoSimulado para interpolación
				tiempoSimuladoBackendRef.current = timestampSimulado; // 🆕 Sincronizar ref
				setUltimaActualizacionReal(Date.now());
				setTiempoMovimiento(0); // Resetear movimiento
				
				// 🆕 ACTUALIZAR tiempoSimulacionActual para mostrar en UI
				setTiempoSimulacionActual(datos.fechaSimulada);
				
				console.log(`⏰ Backend - Tiempo simulado actualizado: ${fechaStr} -> ${new Date(timestampSimulado).toISOString()}`);
			}
			
			setProgresoAG({
				generacion: datos.generacion,
				maxGeneraciones: datos.maxGeneraciones,
				progreso: datos.progreso,
				mejorFitness: datos.mejorFitness,
				fitnessPromedio: datos.fitnessPromedio,
				pedidosProcesados: datos.pedidosProcesados,
				pedidosTotales: datos.pedidosTotales,
				fechaSimulada: datos.fechaSimulada
			});

			agregarMensaje(
				`🧬 Generación ${datos.generacion}/${datos.maxGeneraciones} - Fitness: ${datos.mejorFitness?.toFixed(2)} - Progreso: ${datos.progreso?.toFixed(1)}%`,
				'info'
			);

			// 🔥 NUEVO: Procesar vuelos directos (no rutas con subRutas)
			// Usar refs para llamar a las funciones más recientes (evita closure problem)
			if (datos.solucion?.vuelos && datos.solucion.vuelos.length > 0) {
				console.log(`✈️ Procesando ${datos.solucion.vuelos.length} vuelos directos...`);
				if (procesarVuelosDirectosRef.current) {
					procesarVuelosDirectosRef.current(datos.solucion.vuelos);
				}
			} 
			// Fallback: Si viene en formato antiguo (rutas con subRutas)
			else if (datos.solucion?.rutas) {
				console.log(`✈️ Procesando ${datos.solucion.rutas.length} rutas...`);
				if (procesarRutasSimulacionRef.current) {
					procesarRutasSimulacionRef.current(datos.solucion.rutas);
				}
			}

		} else if (datos.type === 'PROGRESS' || datos.status === 'RUNNING') {
			// 🆕 NUEVA ESTRUCTURA: SimulationMessage con snapshot
			console.log(`🎮 Simulación corriendo - Snapshot recibido`);
			
			if (datos.snapshot) {
				const snapshot = datos.snapshot;
				console.log(`📊 Snapshot: ${snapshot.processedOrders}/${snapshot.totalOrders} pedidos procesados`);
				console.log(`📈 Fitness: ${snapshot.fitness?.toFixed(4)}`);
				
				agregarMensaje(
					`🎮 Progreso: ${snapshot.processedOrders}/${snapshot.totalOrders} pedidos - Fitness: ${snapshot.fitness?.toFixed(4)}`,
					'info'
				);
				
				// 🆕 PROCESAR SEGMENTS del snapshot (nueva estructura)
				if (snapshot.orderPlans && snapshot.orderPlans.length > 0) {
					console.log(`✈️ Procesando segments del snapshot...`);
					if (procesarSegmentsSnapshotRef.current) {
						procesarSegmentsSnapshotRef.current(snapshot);
					}
				}
			}
			// Fallback para estructura antigua
			else if (datos.solution?.routes) {
				console.log(`✈️ [Formato antiguo] Procesando ${datos.solution.routes.length} rutas...`);
				if (procesarRutasSnapshotRef.current) {
					procesarRutasSnapshotRef.current(datos.solution.routes);
				}
			}

		} else if (datos.type === 'COMPLETED' || datos.status === 'COMPLETED') {
			// Simulación completada
			console.log('🎉 Simulación completada exitosamente');
			setEstadoSimulacionStomp('completed');
			agregarMensaje('🎉 Simulación completada exitosamente', 'success');

			// 🆕 Procesar snapshot final si existe
			if (datos.snapshot?.orderPlans) {
				console.log(`✈️ Procesando snapshot final...`);
				if (procesarSegmentsSnapshotRef.current) {
					procesarSegmentsSnapshotRef.current(datos.snapshot);
				}
			}
			// Fallback para formato antiguo
			else if (datos.solution?.routes) {
				if (procesarRutasSnapshotRef.current) {
					procesarRutasSnapshotRef.current(datos.solution.routes);
				}
			}

			// Desuscribirse del topic
			if (subscriptionRef.current) {
				subscriptionRef.current.unsubscribe();
				subscriptionRef.current = null;
			}

		} else if (datos.tipo === 'ERROR') {
			// Error en la simulación
			console.error('❌ Error en simulación:', datos.mensaje);
			setEstadoSimulacionStomp('error');
			agregarMensaje(`❌ Error: ${datos.mensaje}`, 'error');
		}
	}, [agregarMensaje]); // 🔥 No incluir funciones de procesamiento (causa circular reference)

	/**
	 * 🆕 Procesar vuelos DIRECTOS (formato nuevo del backend)
	 * Formato: {origenCodigoICAO, destinoCodigoICAO, fechaInicial, fechaFinal, pedidos, totalPaquetes}
	 */
	/**
	 * 🆕 Procesar segments del snapshot (nueva estructura JSON)
	 * Extrae todos los segments de orderPlans → routes → segments
	 */
	const procesarSegmentsSnapshot = useCallback((snapshot) => {
		const currentAirports = airportsRef.current;
		console.log(`\n🔍 procesarSegmentsSnapshot - Snapshot recibido`);
		console.log(`📍 Aeropuertos disponibles: ${currentAirports.length}`);
		console.log(`⏰ Tiempo simulado: ${snapshot.generatedAt}`);
		
		if (!snapshot.orderPlans || snapshot.orderPlans.length === 0) {
			console.warn('⚠️ No hay orderPlans en el snapshot');
			return;
		}
		
		const nuevosVuelos = [];
		let segmentIndex = 0;
		
		// Iterar por cada orderPlan
		snapshot.orderPlans.forEach((orderPlan, orderIndex) => {
			const orderId = orderPlan.orderId;
			const orderSlackMinutes = orderPlan.slackMinutes;
			
			console.log(`\n📦 Pedido ${orderIndex + 1}/${snapshot.orderPlans.length}: ${orderId}`);
			console.log(`   Holgura: ${orderSlackMinutes} minutos ${orderSlackMinutes > 0 ? '✅' : '⚠️'}`);
			
			// Iterar por cada ruta del pedido
			orderPlan.routes.forEach((route, routeIndex) => {
				console.log(`   📍 Ruta ${routeIndex + 1}: ${route.segments.length} segmentos`);
				
				// Iterar por cada segment (VUELO) de la ruta
				route.segments.forEach((segment, segIndex) => {
					segmentIndex++;
					
					console.log(`\n   ✈️ Segment ${segmentIndex}: ${segment.flightId}`);
					console.log(`      ${segment.origin} → ${segment.destination}`);
					console.log(`      Despegue: ${segment.departureUtc}`);
					console.log(`      Llegada:  ${segment.arrivalUtc}`);
					console.log(`      Cantidad: ${segment.quantity} paquetes`);
					
					// Buscar aeropuertos de origen y destino
					const origen = currentAirports.find(a => 
						a.code.toUpperCase() === segment.origin.toUpperCase()
					);
					const destino = currentAirports.find(a => 
						a.code.toUpperCase() === segment.destination.toUpperCase()
					);
					
					if (!origen) {
						console.error(`      ❌ Aeropuerto ORIGEN no encontrado: "${segment.origin}"`);
						return;
					}
					
					if (!destino) {
						console.error(`      ❌ Aeropuerto DESTINO no encontrado: "${segment.destination}"`);
						return;
					}
					
					console.log(`      ✅ Origen: ${origen.code} [${origen.lat.toFixed(2)}, ${origen.lng.toFixed(2)}]`);
					console.log(`      ✅ Destino: ${destino.code} [${destino.lat.toFixed(2)}, ${destino.lng.toFixed(2)}]`);
					
					// Calcular rotación del avión
					const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
					const rotation = (brg - 90 + 360) % 360;
					
					// Determinar estado basado en holgura
					let status = 'active';
					if (orderSlackMinutes <= 0) {
						status = 'retrasado';
					}
					
					// Posición inicial (será calculada por interpolación)
					const progress = 0;
					const currentLat = origen.lat;
					const currentLng = origen.lng;
					
					// 🆕 Crear ID único combinando múltiples factores para evitar duplicados
					const uniqueId = `SNAP-${segment.flightId}-${orderId}-${routeIndex}-${segIndex}-${segmentIndex}-${Math.random().toString(36).substr(2, 6)}`;
					
					// 🆕 Crear objeto de vuelo con timestamps del segment
					const nuevoVuelo = {
						id: uniqueId,
						flightId: segment.flightId,
						origin: {
							code: segment.origin,
							lat: origen.lat,
							lng: origen.lng,
							region: origen.region
						},
						destination: {
							code: segment.destination,
							lat: destino.lat,
							lng: destino.lng,
							region: destino.region
						},
						// ⏰ TIMESTAMPS PARA INTERPOLACIÓN HÍBRIDA
						fechaInicial: segment.departureUtc,
						fechaFinal: segment.arrivalUtc,
						// Información del pedido
						pedidoId: orderId,
						slackMinutes: orderSlackMinutes,
						// Posición y estado
						progress,
						currentLat,
						currentLng,
						rotation,
						status,
						// Metadatos
						altitude: 35000,
						speed: 850,
						packageCapacity: segment.quantity,
						currentPackages: segment.quantity,
						packageType: 'SNAPSHOT',
						isSameContinentFlight: origen.region === destino.region,
						aircraftColor: orderSlackMinutes <= 0 ? '#ef4444' : '#3b82f6', // Rojo si retrasado
					};
					
					console.log(`      ✅ Vuelo creado con interpolación temporal`);
					console.log(`      🎬 Sistema híbrido ACTIVADO`);
					
					nuevosVuelos.push(nuevoVuelo);
				});
			});
		});
		
		console.log(`\n📊 ============================================`);
		console.log(`📊 Total de segments procesados: ${segmentIndex}`);
		console.log(`📊 Total de vuelos creados: ${nuevosVuelos.length}`);
		const vuelosConInterpolacion = nuevosVuelos.filter(v => v.fechaInicial && v.fechaFinal).length;
		console.log(`🎬 Vuelos con interpolación temporal: ${vuelosConInterpolacion}/${nuevosVuelos.length}`);
		console.log(`📊 ============================================\n`);
		
		if (nuevosVuelos.length > 0) {
			console.log(`🗺️ Primer vuelo (ejemplo):`, {
				id: nuevosVuelos[0].id,
				flightId: nuevosVuelos[0].flightId,
				from: nuevosVuelos[0].origin.code,
				to: nuevosVuelos[0].destination.code,
				departure: nuevosVuelos[0].fechaInicial,
				arrival: nuevosVuelos[0].fechaFinal,
				orderId: nuevosVuelos[0].pedidoId,
				status: nuevosVuelos[0].status
			});
			
			// 🔥 ELIMINAR DUPLICADOS usando Map
			const flightsMap = new Map(nuevosVuelos.map(v => [v.id, v]));
			const vuelosUnicos = Array.from(flightsMap.values());
			
			if (vuelosUnicos.length < nuevosVuelos.length) {
				console.warn(`⚠️ Se encontraron ${nuevosVuelos.length - vuelosUnicos.length} vuelos duplicados, eliminados`);
			}
			
			// 🔥 REEMPLAZAR todos los vuelos (sin duplicados)
			console.log(`🔄 Reemplazando flights array con ${vuelosUnicos.length} vuelos únicos`);
			setFlights(vuelosUnicos);
			
			// 🆕 AUTO-INICIALIZAR TIEMPO SIMULADO basado en los vuelos
			// Si el backend no envía tiempoSimulacionActual, usar la fecha más temprana de los vuelos
			const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
			if (vuelosConFecha.length > 0) {
				const fechaMasTemprana = Math.min(
					...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime())
				);
				
				// Verificar si tiempoSimuladoBackend aún no está establecido usando ref
				if (tiempoSimuladoBackendRef.current === null) {
					console.log(`🕐 AUTO-INICIALIZANDO tiempo simulado a: ${new Date(fechaMasTemprana).toISOString()}`);
					tiempoSimuladoBackendRef.current = fechaMasTemprana; // Marcar como inicializado
					setTiempoSimuladoBackend(fechaMasTemprana);
					setTiempoSimulado(fechaMasTemprana); // 🆕 CRÍTICO: Sincronizar tiempoSimulado para interpolación
					setTiempoSimulacionActual(new Date(fechaMasTemprana).toISOString()); // 🆕 Actualizar UI
					setUltimaActualizacionReal(Date.now());
					setTiempoMovimiento(0);
				}
			}
		}
	}, []);
	
	procesarSegmentsSnapshotRef.current = procesarSegmentsSnapshot;

	const procesarVuelosDirectos = useCallback((vuelos) => {
		const currentAirports = airportsRef.current; // 🔥 Usar ref para tener valor actual
		console.log(`\n🔍 procesarVuelosDirectos - Recibidos ${vuelos?.length || 0} vuelos`);
		console.log(`📍 Aeropuertos disponibles: ${currentAirports.length}`);
		
		if (!vuelos || vuelos.length === 0) {
			console.warn('⚠️ No hay vuelos para procesar');
			return;
		}
		
		const nuevosVuelos = [];
		const baseTimestamp = Date.now(); // 🆕 Timestamp base para todo el lote

		vuelos.forEach((vuelo, index) => {
			console.log(`\n✈️ Vuelo ${index + 1}/${vuelos.length}`);
			console.log(`   Origen: ${vuelo.origenCodigoICAO} → Destino: ${vuelo.destinoCodigoICAO}`);
			console.log(`   Paquetes: ${vuelo.totalPaquetes}`);
			
			// Buscar aeropuertos de origen y destino (case-insensitive)
			const origen = currentAirports.find(a => 
				a.code.toUpperCase() === vuelo.origenCodigoICAO.toUpperCase()
			);
			const destino = currentAirports.find(a => 
				a.code.toUpperCase() === vuelo.destinoCodigoICAO.toUpperCase()
			);

			if (!origen) {
				console.error(`   ❌ Aeropuerto ORIGEN no encontrado: "${vuelo.origenCodigoICAO}"`);
				console.log(`   📋 Aeropuertos disponibles (primeros 5):`, currentAirports.slice(0, 5).map(a => a.code));
				return;
			}
			
			if (!destino) {
				console.error(`   ❌ Aeropuerto DESTINO no encontrado: "${vuelo.destinoCodigoICAO}"`);
				console.log(`   📋 Aeropuertos disponibles (primeros 5):`, currentAirports.slice(0, 5).map(a => a.code));
				return;
			}

			console.log(`   ✅ Origen: ${origen.code} [${origen.lat}, ${origen.lng}]`);
			console.log(`   ✅ Destino: ${destino.code} [${destino.lat}, ${destino.lng}]`);
			
			// 🆕 USAR FECHA DE SIMULACIÓN (no fecha actual del sistema)
			const tieneFechas = vuelo.fechaInicial && vuelo.fechaFinal;
			if (tieneFechas) {
				const salida = new Date(vuelo.fechaInicial).getTime();
				const llegada = new Date(vuelo.fechaFinal).getTime();
				
				console.log(`   ⏰ Fechas: ${vuelo.fechaInicial} → ${vuelo.fechaFinal}`);
				console.log(`   ✈️ Vuelo configurado correctamente para animación`);
			} else {
				console.log(`   ⚠️ Sin fechas de vuelo - usando posición estática`);
			}

			// 🔥 Usar progreso fijo para visualización inmediata (si no hay fechas)
			const progress = 0.5; // Mitad del recorrido

			// Interpolación de posición inicial
			const currentLat = origen.lat + (destino.lat - origen.lat) * progress;
			const currentLng = origen.lng + (destino.lng - origen.lng) * progress;

			// Calcular rotación
			const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
			const rotation = (brg - 90 + 360) % 360;

			// 🆕 ID único ESTABLE: origen + destino + fechaInicial (sin timestamps aleatorios)
			// Esto evita crear duplicados cuando el backend envía el mismo vuelo múltiples veces
			const vueloKey = `${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${vuelo.fechaInicial || index}`;
			const uniqueId = `WS-${vueloKey}`;

			// Crear objeto de vuelo (con timestamps para interpolación híbrida)
			const nuevoVuelo = {
				id: uniqueId,
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
				altitude: 35000, // Siempre en vuelo
				speed: 850, // Siempre con velocidad
				status: 'active', // Siempre activo para visualización
				currentLat,
				currentLng,
				aircraftColor: '#3b82f6', // 🔵 Azul para vuelos del WebSocket
				rotation,
				packageCapacity: vuelo.totalPaquetes || 1,
				currentPackages: vuelo.totalPaquetes || 1,
				packageType: 'WS',
				isSameContinentFlight: origen.region === destino.region,
				vuelo: `WS-${vuelo.pedidos?.[0]?.idPedido || index}`,
				pedidoId: vuelo.pedidos?.[0]?.idPedido,
				fechaInicial: vuelo.fechaInicial,
				fechaFinal: vuelo.fechaFinal
			};
			
			console.log(`   ✅ Vuelo creado en posición: [${currentLat.toFixed(2)}, ${currentLng.toFixed(2)}]`);
			if (nuevoVuelo.fechaInicial && nuevoVuelo.fechaFinal) {
				console.log(`   🎬 Sistema híbrido ACTIVADO para este vuelo`);
			}
			nuevosVuelos.push(nuevoVuelo);
		});

		console.log(`\n📊 ============================================`);
		console.log(`📊 Total de vuelos WebSocket creados: ${nuevosVuelos.length}`);
		const vuelosConInterpolacion = nuevosVuelos.filter(v => v.fechaInicial && v.fechaFinal).length;
		console.log(`🎬 Vuelos con interpolación temporal: ${vuelosConInterpolacion}/${nuevosVuelos.length}`);
		console.log(`📊 ============================================\n`);
		
		if (nuevosVuelos.length > 0) {
			console.log(`🗺️ Primer vuelo (ejemplo):`, {
				id: nuevosVuelos[0].id,
				from: nuevosVuelos[0].origin.code,
				to: nuevosVuelos[0].destination.code,
				position: [nuevosVuelos[0].currentLat, nuevosVuelos[0].currentLng],
				color: nuevosVuelos[0].aircraftColor,
				packages: nuevosVuelos[0].packageCapacity
			});
			
			// 🔥 ELIMINAR DUPLICADOS usando Map
const flightsMap = new Map(nuevosVuelos.map(v => [v.id, v]));
const vuelosUnicos = Array.from(flightsMap.values());

if (vuelosUnicos.length < nuevosVuelos.length) {
console.warn(`⚠️ Se encontraron ${nuevosVuelos.length - vuelosUnicos.length} vuelos duplicados, eliminados`);
}

// 📊 ACTUALIZAR MÉTRICAS - Contar vuelos nuevos recibidos del backend
const tiempoActualParaMetricas = relojLocalRef.current ? relojLocalRef.current.getTime() : Date.now();
let perdidosAntes = 0;
let perdidosDespues = 0;

vuelosUnicos.forEach(vuelo => {
	// Solo contar si es un ID nuevo (no duplicado de entregas anteriores)
	if (!vuelosRecibidosIdsRef.current.has(vuelo.id)) {
		vuelosRecibidosIdsRef.current.add(vuelo.id);
		
		// Verificar si el vuelo llegó "tarde" (ya terminó según el tiempo simulado)
		if (vuelo.fechaFinal) {
			const fechaFin = new Date(vuelo.fechaFinal).getTime();
			if (fechaFin < tiempoActualParaMetricas) {
				perdidosAntes++;
			}
		}
		// Verificar si el vuelo llegó "muy tarde" (ni siquiera empezó)
		if (vuelo.fechaInicial) {
			const fechaInicio = new Date(vuelo.fechaInicial).getTime();
			if (fechaInicio > tiempoActualParaMetricas + (60 * 60 * 1000)) { // 1 hora en el futuro, posible error
				perdidosDespues++;
			}
		}
	}
});

// Actualizar métricas
setMetricasVuelos(prev => ({
	...prev,
	totalRecibidosBackend: vuelosRecibidosIdsRef.current.size,
	totalPerdidosAntesDeTiempo: prev.totalPerdidosAntesDeTiempo + perdidosAntes,
	totalPerdidosDespuesDeTiempo: prev.totalPerdidosDespuesDeTiempo + perdidosDespues,
	ultimaActualizacion: new Date().toISOString()
}));

if (perdidosAntes > 0 || perdidosDespues > 0) {
	console.warn(`📊 MÉTRICAS: ${perdidosAntes} vuelos llegaron tarde (ya aterrizaron), ${perdidosDespues} vuelos muy adelantados`);
}

// 🆕 LOG: Mostrar rango de fechas de los vuelos
const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
if (vuelosConFecha.length > 0) {
const fechaMasTemprana = Math.min(...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime()));
const fechaMasTardia = Math.max(...vuelosConFecha.map(v => new Date(v.fechaFinal).getTime()));
console.log(`�� Rango de vuelos: ${new Date(fechaMasTemprana).toISOString()} → ${new Date(fechaMasTardia).toISOString()}`);
console.log(`⏰ Tiempo simulado actual: ${new Date(relojLocalRef.current).toISOString()}`);
}

// 🆕 SISTEMA DE BUFFER DE 15 SEGUNDOS
// Si el buffer está activo, acumular vuelos en lugar de activar animación
if (bufferActivo) {
console.log(`⏳ BUFFER ACTIVO - Acumulando ${vuelosUnicos.length} vuelos (total: ${vuelosBufferRef.current.length + vuelosUnicos.length})`);

// Agregar vuelos únicos al buffer (evitar duplicados)
const idsExistentes = new Set(vuelosBufferRef.current.map(v => v.id));
const nuevosParaBuffer = vuelosUnicos.filter(v => !idsExistentes.has(v.id));
vuelosBufferRef.current = [...vuelosBufferRef.current, ...nuevosParaBuffer];

console.log(`📦 Buffer ahora tiene ${vuelosBufferRef.current.length} vuelos`);
// NO activar reloj durante el buffer - se activa cuando termina el timeout
return;
}

// 🔥 COMBINAR con vuelos existentes (sin duplicados) - Solo si buffer NO está activo
console.log(`🔄 Combinando ${vuelosUnicos.length} vuelos nuevos con existentes`);

setFlights(prevFlights => {
// 🆕 LIMPIAR vuelos que ya aterrizaron (fechaFinal < tiempoSimulado - 30min margen)
const relojActual = relojLocalRef.current;
const tiempoActualMs = relojActual instanceof Date ? relojActual.getTime() : (relojActual || Date.now());
const margenMs = 30 * 60 * 1000; // 30 minutos de margen después de aterrizar
const vuelosActivos = prevFlights.filter(v => {
	if (!v.fechaFinal) return true; // Mantener si no tiene fecha
	const fechaAterrizaje = new Date(v.fechaFinal).getTime();
	return fechaAterrizaje > (tiempoActualMs - margenMs);
});

if (vuelosActivos.length < prevFlights.length) {
	console.log(`🧹 Limpiados ${prevFlights.length - vuelosActivos.length} vuelos que ya aterrizaron`);
}

const existingIds = new Set(vuelosActivos.map(v => v.id));
const nuevosNoRepetidos = vuelosUnicos.filter(v => !existingIds.has(v.id));
const combinados = [...vuelosActivos, ...nuevosNoRepetidos];
console.log(`📊 Total vuelos después de combinar: ${combinados.length} (activos: ${vuelosActivos.length}, nuevos: ${nuevosNoRepetidos.length})`);
return combinados;
});

// 🚀 ACTIVAR RELOJ LOCAL (solo si no está ya activo)
if (!simulacionLocalActiva) {
setSimulacionLocalActiva(true);
console.log(`🚀 Reloj local ACTIVADO - ${vuelosUnicos.length} vuelos listos para animar`);
}

// ✅ Verificar que el estado se actualizó
setTimeout(() => {
console.log(`✅ Verificación: nuevos vuelos procesados = ${vuelosUnicos.length}`);
}, 100);

const vuelosActivos = vuelosUnicos.filter(v => v.status === 'active').length;
setFlightsInAir(prev => prev + vuelosActivos);
console.log(`Vuelos activos anadidos: ${vuelosActivos}`);
		} else {
			console.error(`NO SE CREARON VUELOS!`);
			console.error(`   Vuelos recibidos:`, vuelos);
			console.error(`   Aeropuertos disponibles:`, currentAirports.length);
		}
	}, []);

	procesarVuelosDirectosRef.current = procesarVuelosDirectos;

	/**
	 * Procesar rutas del snapshot de simulación
	 */
	const procesarRutasSnapshot = useCallback((routes) => {
		// Similar a procesarRutasSimulacion pero para formato de snapshot
		// Implementar según el formato específico de tu backend
		console.log(`📊 Procesando ${routes.length} rutas del snapshot`);
	}, []); // 🔥 Sin dependencias
	procesarRutasSnapshotRef.current = procesarRutasSnapshot;

	/**
	 * Cancelar simulación en curso
	 */
	const cancelarSimulacionStomp = useCallback(async () => {
		if (!sessionId) {
			alert('No hay simulación activa para cancelar');
			return;
		}

		try {
			console.log(`🛑 Cancelando simulación ${sessionId}...`);
			
			const response = await fetch(`http://localhost:8000/api/simulations/${sessionId}/cancel`, {
				method: 'POST'
			});

			if (response.ok) {
				agregarMensaje('🛑 Simulación cancelada', 'warning');
				setEstadoSimulacionStomp('cancelled');
				
				// Desuscribirse
				if (subscriptionRef.current) {
					subscriptionRef.current.unsubscribe();
					subscriptionRef.current = null;
				}
			} else {
				throw new Error('Error al cancelar simulación');
			}
		} catch (error) {
			console.error('❌ Error cancelando simulación:', error);
			agregarMensaje(`❌ Error: ${error.message}`, 'error');
		}
	}, [sessionId, agregarMensaje]);

	/**
	 * Limpiar todo (mensajes, vuelos, estado)
	 */
	const limpiarTodoStomp = useCallback(() => {
		setMensajesSimulacion([]);
		setProgresoAG(null);
		setFlights([]);
		setFlightsInAir(0);
		setSessionId(null);
		setEstadoSimulacionStomp('connected');
		console.log('🧹 Todo limpiado');
	}, []);

	// Auto-conectar WebSocket STOMP al montar (OPCIONAL)
	useEffect(() => {
		// Comentar si no quieres auto-conexión
		// conectarWebSocketStomp();
		
		return () => {
			desconectarWebSocketStomp();
		};
	}, [conectarWebSocketStomp, desconectarWebSocketStomp]);

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
						
						{/* 📊 PANEL DE MÉTRICAS DE DIAGNÓSTICO */}
						<div className="stats-section" style={{ marginTop: '15px' }}>
							<h4><i className="fas fa-chart-bar"></i> Diagnóstico de Vuelos</h4>
							<div style={{ 
								backgroundColor: '#1a1a2e', 
								borderRadius: '8px', 
								padding: '12px',
								fontSize: '13px'
							}}>
								<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
									<span style={{ color: '#9ca3af' }}>📨 Recibidos backend:</span>
									<span style={{ color: '#3b82f6', fontWeight: 'bold' }}>{metricasVuelos.totalRecibidosBackend}</span>
								</div>
								<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
									<span style={{ color: '#9ca3af' }}>📋 En estado flights[]:</span>
									<span style={{ color: '#22c55e', fontWeight: 'bold' }}>{metricasVuelos.totalEnEstadoFlights}</span>
								</div>
								
								{/* 🆕 DESGLOSE POR ESTADO */}
								<div style={{ 
									backgroundColor: '#0f172a', 
									borderRadius: '6px', 
									padding: '8px', 
									marginBottom: '8px',
									border: '1px solid #1e3a5f'
								}}>
									<div style={{ fontSize: '11px', color: '#64748b', marginBottom: '6px' }}>Desglose por estado:</div>
									<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
										<span style={{ color: '#f59e0b', fontSize: '12px' }}>⏳ Esperando (waiting):</span>
										<span style={{ color: '#f59e0b', fontWeight: 'bold' }}>{metricasVuelos.vuelosWaiting || 0}</span>
									</div>
									<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
										<span style={{ color: '#22c55e', fontSize: '12px' }}>🛫 En vuelo (active):</span>
										<span style={{ color: '#22c55e', fontWeight: 'bold' }}>{metricasVuelos.vuelosActive || 0}</span>
									</div>
									<div style={{ display: 'flex', justifyContent: 'space-between' }}>
										<span style={{ color: '#6366f1', fontSize: '12px' }}>✅ Aterrizados (completed):</span>
										<span style={{ color: '#6366f1', fontWeight: 'bold' }}>{metricasVuelos.vuelosCompleted || 0}</span>
									</div>
								</div>
								
								<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
									<span style={{ color: '#9ca3af' }}>✈️ Graficados en mapa:</span>
									<span style={{ color: '#f59e0b', fontWeight: 'bold' }}>{metricasVuelos.totalGraficados}</span>
								</div>
								<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', borderTop: '1px solid #374151', paddingTop: '8px' }}>
									<span style={{ color: '#ef4444' }}>⚠️ Llegaron tarde:</span>
									<span style={{ color: metricasVuelos.totalPerdidosAntesDeTiempo > 0 ? '#ef4444' : '#22c55e', fontWeight: 'bold' }}>
										{metricasVuelos.totalPerdidosAntesDeTiempo}
									</span>
								</div>
								{/* Barra de eficiencia */}
								<div style={{ marginTop: '10px' }}>
									<div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
										<span style={{ color: '#9ca3af', fontSize: '11px' }}>Eficiencia de graficación:</span>
										<span style={{ 
											color: metricasVuelos.totalRecibidosBackend > 0 
												? ((metricasVuelos.totalGraficados / metricasVuelos.totalRecibidosBackend) * 100 >= 80 ? '#22c55e' : '#f59e0b')
												: '#9ca3af',
											fontSize: '11px'
										}}>
											{metricasVuelos.totalRecibidosBackend > 0 
												? `${((metricasVuelos.totalGraficados / metricasVuelos.totalRecibidosBackend) * 100).toFixed(1)}%` 
												: '-'}
										</span>
									</div>
									<div style={{ 
										backgroundColor: '#374151', 
										borderRadius: '4px', 
										height: '6px',
										overflow: 'hidden'
									}}>
										<div style={{ 
											backgroundColor: metricasVuelos.totalRecibidosBackend > 0 
												? ((metricasVuelos.totalGraficados / metricasVuelos.totalRecibidosBackend) * 100 >= 80 ? '#22c55e' : '#f59e0b')
												: '#374151',
											width: metricasVuelos.totalRecibidosBackend > 0 
												? `${Math.min(100, (metricasVuelos.totalGraficados / metricasVuelos.totalRecibidosBackend) * 100)}%`
												: '0%',
											height: '100%',
											transition: 'width 0.3s ease'
										}}></div>
									</div>
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
									{/* 🆕 INDICADOR DE WEBSOCKET - Muestra estado de STOMP (simulación real) */}
									<WebSocketStatusIndicator
										connectionState={wsStompConectado ? 'connected' : estadoSimulacionStomp === 'connecting' ? 'connecting' : 'disconnected'}
										connectionQuality={wsStompConectado ? 'good' : 'unknown'}
										latency={null}
										reconnectAttempt={0}
										maxReconnectAttempts={10}
										showQuality={false}
									/>

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

									{/* Selector de hora de inicio */}
									<div className="form-group" style={{ margin: 0 }}>
										<label className="form-label" htmlFor="hora-inicio">
											Hora de Inicio:
										</label>
										<input
											type="time"
											id="hora-inicio"
											className="date-input"
											value={horaInicioSimulacion}
											onChange={(e) => setHoraInicioSimulacion(e.target.value)}
											style={{ width: '100px' }}
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

									{/* Botones de control SIMPLIFICADOS */}
									<div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
										{/* Controles de simulación SIMPLIFICADOS */}
										<div className="simulation-controls">
											<div className="control-buttons" style={{ display: 'flex', gap: '10px' }}>
												<button
													onClick={handleIniciarSimulacion}
													disabled={simulacionActiva}
													style={{
														padding: '10px 20px',
														borderRadius: '6px',
														border: 'none',
														background: simulacionActiva ? '#e9ecef' : '#28a745',
														color: simulacionActiva ? '#6c757d' : 'white',
														fontSize: '15px',
														fontWeight: '600',
														cursor: simulacionActiva ? 'not-allowed' : 'pointer',
														display: 'flex',
														alignItems: 'center',
														gap: '8px',
														transition: 'all 0.2s',
														boxShadow: simulacionActiva ? 'none' : '0 2px 8px rgba(40, 167, 69, 0.3)'
													}}
												>	
													<FaPlay size={16} />
													Comenzar Simulación
												</button>
												<button
													onClick={handleDetenerSimulacion}
													disabled={!simulacionActiva}
													style={{
														padding: '10px 20px',
														borderRadius: '6px',
														border: 'none',
														background: simulacionActiva ? '#dc3545' : '#e9ecef',
														color: simulacionActiva ? 'white' : '#6c757d',
														fontSize: '15px',
														fontWeight: '600',
														cursor: simulacionActiva ? 'pointer' : 'not-allowed',
														display: 'flex',
														alignItems: 'center',
														gap: '8px',
														transition: 'all 0.2s',
														boxShadow: simulacionActiva ? '0 2px 8px rgba(220, 53, 69, 0.3)' : 'none'
													}}
												>
													<FaStop size={16} />
													Detener Simulación
												</button>
											</div>
										</div>

										{/* Controles de planificación WebSocket - OCULTOS */}
										{false && wsConnected && (
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

							{/* ==================== PANEL WEBSOCKET STOMP (SIMULACIÓN SEMANAL) - OCULTO ==================== */}
							<div style={{
								marginTop: '20px',
								padding: '20px',
								background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
								borderRadius: '12px',
								boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
								display: 'none' // 🔥 OCULTO - Solo usar botones Iniciar/Detener
							}}>
								<div style={{
									display: 'flex',
									justifyContent: 'space-between',
									alignItems: 'center',
									marginBottom: '20px'
								}}>
									<h3 style={{ color: 'white', margin: 0, fontSize: '20px', fontWeight: '600' }}>
										🌐 WebSocket STOMP - Simulación en Tiempo Real
									</h3>
									
									{/* Indicador de estado */}
									<div style={{
										padding: '8px 16px',
										borderRadius: '20px',
										background: wsStompConectado ? '#3b82f6' : '#ef4444',
										color: 'white',
										fontWeight: '600',
										fontSize: '14px',
										display: 'flex',
										alignItems: 'center',
										gap: '8px',
										boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
									}}>
										<span style={{
											width: '10px',
											height: '10px',
											borderRadius: '50%',
											background: 'white',
											animation: wsStompConectado ? 'pulse 2s infinite' : 'none'
										}}></span>
										{wsStompConectado ? 'CONECTADO' : 'DESCONECTADO'}
									</div>
								</div>

								{/* Controles */}
								<div style={{
									display: 'flex',
									gap: '12px',
									flexWrap: 'wrap',
									marginBottom: '20px'
								}}>
									{!wsStompConectado ? (
										<button
											onClick={conectarWebSocketStomp}
											style={{
												padding: '10px 20px',
												background: '#3b82f6',
												color: 'white',
												border: 'none',
												borderRadius: '8px',
												fontWeight: '600',
												cursor: 'pointer',
												transition: 'all 0.3s',
												boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)'
											}}
										>
											🔌 Conectar WebSocket
										</button>
									) : (
										<>
											<button
												onClick={iniciarSimulacionWebSocketStomp}
												disabled={estadoSimulacionStomp === 'running' || !fechaInicioSimulacion}
												style={{
													padding: '10px 20px',
													background: estadoSimulacionStomp === 'running' ? '#6b7280' : '#3b82f6',
													color: 'white',
													border: 'none',
													borderRadius: '8px',
													fontWeight: '600',
													cursor: estadoSimulacionStomp === 'running' ? 'not-allowed' : 'pointer',
													transition: 'all 0.3s',
													boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)'
												}}
											>
												🚀 Iniciar Simulación Semanal
											</button>

											<button
												onClick={cancelarSimulacionStomp}
												disabled={!sessionId || estadoSimulacionStomp !== 'running'}
												style={{
													padding: '10px 20px',
													background: sessionId && estadoSimulacionStomp === 'running' ? '#ef4444' : '#6b7280',
													color: 'white',
													border: 'none',
													borderRadius: '8px',
													fontWeight: '600',
													cursor: sessionId && estadoSimulacionStomp === 'running' ? 'pointer' : 'not-allowed',
													transition: 'all 0.3s'
												}}
											>
												🛑 Cancelar Simulación
											</button>

											<button
												onClick={limpiarTodoStomp}
												style={{
													padding: '10px 20px',
													background: '#f59e0b',
													color: 'white',
													border: 'none',
													borderRadius: '8px',
													fontWeight: '600',
													cursor: 'pointer',
													transition: 'all 0.3s'
												}}
											>
												🧹 Limpiar Todo
											</button>
											
											{/* 🔥 BOTÓN DE PRUEBA - Añadir vuelos manualmente */}
											<button
												onClick={() => {
													console.log('🧪 PRUEBA: Añadiendo vuelos de prueba...');
													const vuelosPrueba = [
														{
															id: `TEST-${Date.now()}-1`,
															origin: { code: 'KJFK', lat: 40.6413, lng: -73.7781, region: 'North America' },
															destination: { code: 'EGLL', lat: 51.4700, lng: -0.4543, region: 'Europe' },
															progress: 0.5,
															altitude: 35000,
															speed: 850,
															status: 'active',
															currentLat: 46.0, // Mitad del Atlántico
															currentLng: -37.0,
															aircraftColor: '#3b82f6', // Azul
															rotation: 45,
															packageCapacity: 1,
															currentPackages: 1,
															packageType: 'TEST'
														},
														{
															id: `TEST-${Date.now()}-2`,
															origin: { code: 'EGLL', lat: 51.4700, lng: -0.4543, region: 'Europe' },
															destination: { code: 'RJTT', lat: 35.5494, lng: 139.7798, region: 'Asia' },
															progress: 0.3,
															altitude: 35000,
															speed: 850,
															status: 'active',
															currentLat: 55.0, // Europa del Este
															currentLng: 50.0,
															aircraftColor: '#ef4444', // Rojo
															rotation: 90,
															packageCapacity: 1,
															currentPackages: 1,
															packageType: 'TEST'
														}
													];
													
													console.log('🧪 Vuelos de prueba:', vuelosPrueba);
													setFlights(prev => [...prev, ...vuelosPrueba]);
													setFlightsInAir(prev => prev + 2);
													console.log('✅ Vuelos de prueba añadidos');
												}}
												style={{
													padding: '10px 20px',
													background: '#8b5cf6',
													color: 'white',
													border: 'none',
													borderRadius: '8px',
													fontWeight: '600',
													cursor: 'pointer',
													transition: 'all 0.3s'
												}}
											>
												🧪 Test Vuelos
											</button>

											<button
												onClick={desconectarWebSocketStomp}
												style={{
													padding: '10px 20px',
													background: '#6b7280',
													color: 'white',
													border: 'none',
													borderRadius: '8px',
													fontWeight: '600',
													cursor: 'pointer',
													transition: 'all 0.3s'
												}}
											>
												🔌 Desconectar
											</button>
										</>
									)}
								</div>

								{/* Progreso del AG */}
								{progresoAG && (
									<div style={{
										background: 'rgba(255, 255, 255, 0.95)',
										borderRadius: '10px',
										padding: '20px',
										marginBottom: '20px',
										boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
									}}>
										<h4 style={{ margin: '0 0 15px 0', color: '#1f2937', fontSize: '16px', fontWeight: '600' }}>
											🧬 Progreso del Algoritmo Genético
										</h4>
										
										{/* Barra de progreso */}
										<div style={{
											width: '100%',
											height: '30px',
											background: '#e5e7eb',
											borderRadius: '15px',
											overflow: 'hidden',
											marginBottom: '15px',
											position: 'relative'
										}}>
											<div style={{
												width: `${progresoAG.progreso || 0}%`,
												height: '100%',
												background: 'linear-gradient(90deg, #3b82f6 0%, #3b82f6 100%)',
												transition: 'width 0.5s ease',
												display: 'flex',
												alignItems: 'center',
												justifyContent: 'center',
												color: 'white',
												fontWeight: '600',
												fontSize: '14px'
											}}>
												{progresoAG.progreso?.toFixed(1)}%
											</div>
										</div>

										{/* Métricas */}
										<div style={{
											display: 'grid',
											gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
											gap: '15px'
										}}>
											<div style={{ background: '#f3f4f6', padding: '12px', borderRadius: '8px' }}>
												<div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>Generación</div>
												<div style={{ fontSize: '20px', fontWeight: '700', color: '#1f2937' }}>
													{progresoAG.generacion} / {progresoAG.maxGeneraciones}
												</div>
											</div>

											<div style={{ background: '#f3f4f6', padding: '12px', borderRadius: '8px' }}>
												<div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>Mejor Fitness</div>
												<div style={{ fontSize: '20px', fontWeight: '700', color: '#3b82f6' }}>
													{progresoAG.mejorFitness?.toFixed(2)}
												</div>
											</div>

											<div style={{ background: '#f3f4f6', padding: '12px', borderRadius: '8px' }}>
												<div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>Fitness Promedio</div>
												<div style={{ fontSize: '20px', fontWeight: '700', color: '#3b82f6' }}>
													{progresoAG.fitnessPromedio?.toFixed(2)}
												</div>
											</div>

											<div style={{ background: '#f3f4f6', padding: '12px', borderRadius: '8px' }}>
												<div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '4px' }}>Pedidos Procesados</div>
												<div style={{ fontSize: '20px', fontWeight: '700', color: '#f59e0b' }}>
													{progresoAG.pedidosProcesados} / {progresoAG.pedidosTotales}
												</div>
											</div>
										</div>
									</div>
								)}

								{/* Log de mensajes */}
								<div style={{
									background: 'rgba(255, 255, 255, 0.95)',
									borderRadius: '10px',
									padding: '15px',
									maxHeight: '300px',
									overflowY: 'auto',
									boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
								}}>
									<h4 style={{ margin: '0 0 12px 0', color: '#1f2937', fontSize: '14px', fontWeight: '600' }}>
										📝 Log de Eventos ({mensajesSimulacion.length})
									</h4>
									
									{mensajesSimulacion.length === 0 ? (
										<div style={{ textAlign: 'center', color: '#6b7280', padding: '20px', fontSize: '14px' }}>
											No hay mensajes aún
										</div>
									) : (
										<div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
											{mensajesSimulacion.map(msg => (
												<div
													key={msg.id}
													style={{
														padding: '10px 12px',
														borderRadius: '6px',
														background: 
															msg.tipo === 'success' ? '#d1fae5' :
															msg.tipo === 'error' ? '#fee2e2' :
															msg.tipo === 'warning' ? '#fef3c7' :
															'#dbeafe',
														borderLeft: `4px solid ${
															msg.tipo === 'success' ? '#3b82f6' :
															msg.tipo === 'error' ? '#ef4444' :
															msg.tipo === 'warning' ? '#f59e0b' :
															'#3b82f6'
														}`,
														fontSize: '13px',
														color: '#1f2937'
													}}
												>
													<span style={{ fontWeight: '600', marginRight: '8px', fontSize: '11px', color: '#6b7280' }}>
														{msg.timestamp}
													</span>
													{msg.texto}
												</div>
											))}
										</div>
									)}
								</div>
							</div>
						</div>


						{/* Mapa interactivo */}
						<div className="map-container">
							<MapContainer center={[20.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10} zoomControl={true} doubleClickZoom={true} boxZoom={true} keyboard={true} touchZoom={true} worldCopyJump={true} maxBoundsViscosity={1.0} maxBounds={[[-90, -180], [90, 180]]}>
								<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} bounds={[[-90, -180], [90, 180]]} />
								<DynamicMarkers 
									flights={flights} 
									airports={airports} 
									activeView={activeView} 
									showRoutes={showRoutes} 
									vuelosEnMovimiento={vuelosEnMovimiento}
								/>
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
