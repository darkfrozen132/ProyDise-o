import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer } from 'react-leaflet';
import { Drawer, IconButton } from '@mui/material';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import { IoArrowBackCircleOutline } from "react-icons/io5";
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './SimuladorSemanal.css';
import { 
	getAirports, 
	getFlights, 
	iniciarSimulacion, 
	pausarSimulacion, 
	reanudarSimulacion, 
	detenerSimulacion,
	conectarStreamSimulacion,
	obtenerEstadoSimulacion,
	consultarEstadoWebSocket,
	activarWebSocket,
	desactivarWebSocket,
	enviarMensajePruebaWS
} from '../../../config/api';
import { conectarWebSocket } from '../../../config/websocket';

/* Reparar iconos por defecto de Leaflet */
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
	iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
	iconUrl: require('leaflet/dist/images/marker-icon.png'),
	shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

/* Iconos de aviones personalizados como SVG dentro de divIcon */
const createAirplaneIcon = (type, color, rotation = 0) => {
	const iconSvg = {
		'boeing737': `<svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="10" cy="10" rx="1.5" ry="8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="10" cy="8" rx="7" ry="1.2" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="10" cy="14" rx="3" ry="0.8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<path d="M10 16 L10 18 L9.5 18 L9.5 16 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
		</svg>`,
		'airbus320': `<svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="10" cy="10" rx="1.8" ry="9" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="10" cy="7.5" rx="8" ry="1.5" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="10" cy="14.5" rx="3.5" ry="1" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<path d="M10 16.5 L10 18.5 L9.2 18.5 L9.2 16.5 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
		</svg>`,
		'boeing777': `<svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="11" cy="11" rx="2" ry="10" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="8" rx="9" ry="1.8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<ellipse cx="11" cy="15" rx="4" ry="1.2" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
			<path d="M11 17 L11 19.5 L10 19.5 L10 17 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
		</svg>`,
		'cargo': `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
			<ellipse cx="12" cy="12" rx="2.5" ry="10" fill="${color}" stroke="#ffffff" stroke-width="0.6"/>
			<ellipse cx="12" cy="9" rx="10" ry="2" fill="${color}" stroke="#ffffff" stroke-width="0.6"/>
			<ellipse cx="12" cy="16" rx="4.5" ry="1.3" fill="${color}" stroke="#ffffff" stroke-width="0.6"/>
			<path d="M12 18 L12 21 L11 21 L11 18 Z" fill="${color}" stroke="#ffffff" stroke-width="0.4"/>
		</svg>`
	};

	/* Crear divIcon con el SVG correspondiente */
	return L.divIcon({
		html: `<div style="transform: rotate(${rotation}deg); display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));">${iconSvg[type]}</div>`,
		className: 'airplane-icon',
		iconSize: type === 'cargo' ? [24, 24] : type === 'boeing777' ? [22, 22] : [20, 20],
		iconAnchor: type === 'cargo' ? [12, 12] : type === 'boeing777' ? [11, 11] : [10, 10],
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
				const icon = createAirplaneIcon(flight.aircraftType, flight.aircraftColor, flight.rotation);
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

const SimuladorSemanal = () => {
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
	const [startDate, setStartDate] = useState("");
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

	// ==================== ESTADO WEBSOCKET ====================
	const [wsConectado, setWsConectado] = useState(false);
	const [wsActivo, setWsActivo] = useState(false);
	const [mensajesWS, setMensajesWS] = useState([]);
	const wsRef = useRef(null); /* Referencia para el WebSocket */

	// ==================== FUNCIÓN PARA CONVERTIR RUTA DEL BACKEND A VUELO ====================
	const convertirRutaAVuelo = (ruta) => {
		// Determinar tipo de avión según capacidad de paquetes
		let aircraftType, aircraftName;
		if (ruta.totalPackages >= 300) {
			aircraftType = 'boeing777';
			aircraftName = 'Boeing 777';
		} else if (ruta.totalPackages >= 200) {
			aircraftType = 'airbus320';
			aircraftName = 'Airbus A320';
		} else if (ruta.totalPackages >= 100) {
			aircraftType = 'boeing737';
			aircraftName = 'Boeing 737';
		} else {
			aircraftType = 'cargo';
			aircraftName = 'Cargo';
		}

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
			aircraftType,
			aircraftName,
			aircraftColor,
			rotation
		};
	};

	// ==================== DEBUG: MONITOREAR CAMBIOS EN tiempoRealMs ====================
	useEffect(() => {
		console.log('🕐 tiempoRealMs actualizado a:', tiempoRealMs, 'ms =', {
			horas: Math.floor(tiempoRealMs / 3600000),
			minutos: Math.floor((tiempoRealMs % 3600000) / 60000),
			segundos: Math.floor((tiempoRealMs % 60000) / 1000)
		});
	}, [tiempoRealMs]);

	/* Cargar aeropuertos desde la API al montar el componente */
	useEffect(() => {
		const fetchAirports = async () => {
			try {
				setLoadingAirports(true);
				console.log('🔄 Iniciando carga de aeropuertos...');
				const data = await getAirports();
				console.log('✅ Aeropuertos cargados desde API:', data.length, 'aeropuertos');
				setAirports(data);
			} catch (error) {
				console.error('❌ Error al cargar aeropuertos desde API:', error);
				console.log('🔄 Usando datos de fallback...');
				// Fallback a datos estáticos en caso de error
				const fallbackData = [
					{ name: 'Lima-Jorge Chávez', code: 'SPIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 980, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
					{ name: 'Bogotá', code: 'SKBO', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 720, isSede: false, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
					{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 850, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 960, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' }
				];
				setAirports(fallbackData);
				console.log('✅ Datos de fallback cargados:', fallbackData.length, 'aeropuertos');
			} finally {
				setLoadingAirports(false);
				console.log('✅ Carga de aeropuertos finalizada');
			}
		};

		fetchAirports();
	}, []);

	/* ========== CARGA DE VUELOS DESACTIVADA TEMPORALMENTE ========== */
	/* Por ahora solo usamos vuelos generados localmente, sin llamar al API de vuelos */
	/* La carga desde API está comentada para enfocarnos en el SSE */

	// ==================== POLLING FALLBACK PARA ACTUALIZAR TIEMPO ====================
	// Este efecto actualiza el tiempo cada segundo mediante polling
	// Se usa como fallback si el SSE no envía actualizaciones continuas
	useEffect(() => {
		if (!simulacionActiva) {
			return;
		}

		console.log('⏱️ Iniciando polling para actualizar tiempo...');
		
		const pollingInterval = setInterval(async () => {
			try {
				const estado = await obtenerEstadoSimulacion();
				console.log('🔄 Polling - Estado actualizado:', {
					horaSimulada: estado.horaSimulada,
					tiempoRealMs: estado.tiempoRealTranscurridoMs,
					tickActual: estado.tickActual
				});
				
				setHoraSimulada(estado.horaSimulada);
				setTiempoRealMs(estado.tiempoRealTranscurridoMs);
				setTickActual(estado.tickActual);
				setTimeScale(estado.timeScale);
				setSimulacionActiva(estado.activa);
			} catch (error) {
				console.error('❌ Error en polling:', error);
			}
		}, 1000); // Actualizar cada 1 segundo

		return () => {
			console.log('⏹️ Deteniendo polling...');
			clearInterval(pollingInterval);
		};
	}, [simulacionActiva]);

	// ==================== CONEXIÓN SSE PARA TIEMPO DE SIMULACIÓN ====================
	useEffect(() => {
		// Solo conectar si la simulación está activa
		if (!simulacionActiva) {
			console.log('⏸️ SSE no conectado - simulación no activa');
			return;
		}

		console.log('📡 Conectando al stream SSE de simulación...');
		
		const eventSource = conectarStreamSimulacion(
			// Callback cuando llega un mensaje
			(data) => {
				console.log('✅ Datos SSE recibidos:', {
					horaSimulada: data.horaSimulada,
					tiempoRealMs: data.tiempoRealTranscurridoMs,
					tickActual: data.tickActual,
					timeScale: data.timeScale,
					activa: data.activa,
					rutasCount: data.rutasSolucion?.length || 0
				});
				setHoraSimulada(data.horaSimulada);
				setTiempoRealMs(data.tiempoRealTranscurridoMs);
				setTickActual(data.tickActual);
				setTimeScale(data.timeScale);
				setSimulacionActiva(data.activa);
				
				// Actualizar rutas y convertirlas a vuelos
				if (data.rutasSolucion && data.rutasSolucion.length > 0) {
					console.log('✈️ Actualizando rutas desde SSE:', data.rutasSolucion.length, 'rutas');
					setRutasSolucion(data.rutasSolucion);
					
					// Convertir rutas a formato de vuelos para el mapa
					const nuevosVuelos = data.rutasSolucion.map(ruta => convertirRutaAVuelo(ruta));
					setFlights(nuevosVuelos);
					
					// Contar vuelos en el aire
					const enAire = nuevosVuelos.filter(v => v.altitude > 1000).length;
					setFlightsInAir(enAire);
					console.log('✅ Vuelos actualizados:', nuevosVuelos.length, 'total,', enAire, 'en el aire');
				}
			},
			// Callback cuando hay error
			(error) => {
				console.error('❌ Error en stream SSE:', error);
				setSimulacionActiva(false);
			}
		);

		eventSourceRef.current = eventSource;

		// Cleanup: cerrar conexión al desmontar o cuando simulacionActiva cambie
		return () => {
			console.log('🔌 Cerrando conexión SSE...');
			if (eventSourceRef.current) {
				eventSourceRef.current.close();
				eventSourceRef.current = null;
			}
		};
	}, [simulacionActiva]);

	// ==================== CLEANUP WEBSOCKET AL DESMONTAR ====================
	useEffect(() => {
		return () => {
			if (wsRef.current) {
				console.log('🔌 Cerrando WebSocket al desmontar componente...');
				wsRef.current.cerrar();
			}
		};
	}, []);

	// ==================== FUNCIONES PARA CONTROLAR SIMULACIÓN ====================
	const handleIniciarSimulacion = async () => {
		try {
			console.log('🚀 Iniciando simulación...');
			const response = await iniciarSimulacion();
			console.log('✅ Respuesta de iniciar simulación:', response);
			setSimulacionActiva(true);
			setHoraSimulada(response.estado.horaSimulada);
			setTiempoRealMs(response.estado.tiempoRealTranscurridoMs);
			setTickActual(response.estado.tickActual);
			setTimeScale(response.estado.timeScale);
			
			// Cargar rutas iniciales si vienen en la respuesta
			if (response.estado.rutasSolucion && response.estado.rutasSolucion.length > 0) {
				console.log('✈️ Cargando rutas iniciales:', response.estado.rutasSolucion.length, 'rutas');
				setRutasSolucion(response.estado.rutasSolucion);
				
				// Convertir rutas a vuelos
				const vuelosIniciales = response.estado.rutasSolucion.map(ruta => convertirRutaAVuelo(ruta));
				setFlights(vuelosIniciales);
				
				const enAire = vuelosIniciales.filter(v => v.altitude > 1000).length;
				setFlightsInAir(enAire);
				console.log('✅ Vuelos iniciales cargados:', vuelosIniciales.length, 'total,', enAire, 'en el aire');
			}
			
			console.log('✅ Estado SSE inicializado:', {
				simulacionActiva: true,
				horaSimulada: response.estado.horaSimulada,
				tiempoRealMs: response.estado.tiempoRealTranscurridoMs,
				rutasCount: response.estado.rutasSolucion?.length || 0
			});
		} catch (error) {
			console.error('❌ Error al iniciar simulación:', error);
			alert('Error al conectar con el servidor. Verifica que el backend esté corriendo en http://127.0.0.1:8080');
		}
	};

	const handlePausarSimulacion = async () => {
		try {
			await pausarSimulacion();
			setSimulacionActiva(false);
		} catch (error) {
			console.error('Error al pausar simulación:', error);
		}
	};

	const handleReanudarSimulacion = async () => {
		try {
			const response = await reanudarSimulacion();
			setSimulacionActiva(true);
		} catch (error) {
			console.error('Error al reanudar simulación:', error);
		}
	};

	const handleDetenerSimulacion = async () => {
		try {
			await detenerSimulacion();
			setSimulacionActiva(false);
			setHoraSimulada(null);
			setTiempoRealMs(0);
			setTickActual(0);
		} catch (error) {
			console.error('Error al detener simulación:', error);
		}
	};

	// ==================== FUNCIONES WEBSOCKET ====================
	
	// Conectar WebSocket
	const handleConectarWS = () => {
		if (wsRef.current) {
			console.warn('⚠️ WebSocket ya está conectado');
			return;
		}

		console.log('🔌 Intentando conectar WebSocket...');
		const ws = conectarWebSocket(
			(data) => {
				console.log('📨 Mensaje WebSocket recibido:', data);
				setMensajesWS(prev => [...prev, data]);
			},
			(error) => {
				console.error('❌ Error WebSocket:', error);
				setWsConectado(false);
			},
			() => {
				console.log('✅ WebSocket conectado!');
				setWsConectado(true);
			},
			() => {
				console.log('🔌 WebSocket desconectado');
				setWsConectado(false);
				wsRef.current = null;
			}
		);
		wsRef.current = ws;
	};

	// Desconectar WebSocket
	const handleDesconectarWS = () => {
		if (wsRef.current) {
			wsRef.current.cerrar();
			wsRef.current = null;
			setWsConectado(false);
			console.log('🔌 WebSocket desconectado manualmente');
		}
	};

	// Activar WebSocket en el backend
	const handleActivarWS = async () => {
		try {
			const response = await activarWebSocket();
			setWsActivo(true);
			console.log('✅ WebSocket activado en backend:', response);
		} catch (error) {
			console.error('❌ Error al activar WebSocket:', error);
		}
	};

	// Desactivar WebSocket en el backend
	const handleDesactivarWS = async () => {
		try {
			const response = await desactivarWebSocket();
			setWsActivo(false);
			console.log('🛑 WebSocket desactivado en backend:', response);
		} catch (error) {
			console.error('❌ Error al desactivar WebSocket:', error);
		}
	};

	// Consultar estado del WebSocket
	const handleConsultarEstadoWS = async () => {
		try {
			const response = await consultarEstadoWebSocket();
			setWsActivo(response.activo || false);
			console.log('📊 Estado WebSocket:', response);
		} catch (error) {
			console.error('❌ Error al consultar estado:', error);
		}
	};

	// Enviar mensaje de prueba
	const handleEnviarMensajeWS = async () => {
		try {
			const mensaje = `Prueba desde frontend - ${new Date().toLocaleTimeString()}`;
			await enviarMensajePruebaWS(mensaje);
		} catch (error) {
			console.error('❌ Error al enviar mensaje:', error);
		}
	};

	// Limpiar mensajes
	const handleLimpiarMensajesWS = () => {
		setMensajesWS([]);
	};


	/* Generar vuelos iniciales con lógica de origen, destino, tipo de avión, capacidad y carga */
	/* ========== GENERACIÓN LOCAL DE VUELOS DESACTIVADA ========== */
	/* Ahora los vuelos se cargan desde el SSE del backend (rutasSolucion) */
	/* La siguiente función está comentada porque ya no se usa */
	/*
	const generateInitialFlights = useCallback(() => {
		// ... código comentado ...
	}, [airports]);

	useEffect(() => {
		if (airports.length > 0 && !loadingAirports) {
			console.log('🔄 Generando vuelos localmente (modo prueba SSE)...');
			generateInitialFlights();
		}
	}, [airports, loadingAirports, generateInitialFlights]);
	*/

	/* ========== SIMULACIÓN LOCAL DESACTIVADA ========== */
	/* El SSE del backend ahora maneja toda la lógica de simulación */
	/* La siguiente lógica está comentada porque ya no se usa */
	/*
	useEffect(() => {
		if (isRunning) {
			// ... lógica de simulación local comentada ...
		}
	}, [isRunning, speed, airports]);
	*/

	const handlePlay = () => {
		setIsRunning(true);
		setSimulationStatus('Simulación semanal en ejecución');
	};
	const handleStop = () => {
		setIsRunning(false);
		setSimulationStatus('Simulación semanal detenida');
	};
	const handleSpeedChange = () => { const speeds = [1, 2, 4, 8]; const currentIndex = speeds.indexOf(speed); const nextSpeed = speeds[(currentIndex + 1) % speeds.length]; setSpeed(nextSpeed); };
	const formatTime = (timeObj) => `${timeObj.days.toString().padStart(2, '0')} : ${timeObj.hours.toString().padStart(2, '0')} : ${timeObj.minutes.toString().padStart(2, '0')}`;

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
					left: open ? drawerWidth - 30 : 0,
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
								<div className="metric-card">
									<div className="metric-icon"><i className="fas fa-infinity"></i></div>
									<div className="metric-content">
										<div className="metric-label">Capacidad total</div>
										<div className="metric-value">∞</div>
										<div className="metric-sublabel">ilimitada</div>
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
							<div className="header-control-panel">
								{/* Botón para regresar a operaciones */}
								<button className="btn-back" onClick={goBack} title="Regresar">
									<IoArrowBackCircleOutline size={32} />
								</button>
								<h2>Simulación Semanal</h2>
							</div>

							{/* ==================== PANEL SIMPLE DE TIEMPO SSE ==================== */}
							<div style={{
								background: '#f8f9fa',
								borderRadius: '8px',
								padding: '15px 20px',
								marginBottom: '20px',
								border: '1px solid #dee2e6',
								display: 'flex',
								justifyContent: 'space-between',
								alignItems: 'center',
								flexWrap: 'wrap',
								gap: '15px'
							}}>
								{/* Información de tiempo */}
								<div style={{ display: 'flex', gap: '30px', flexWrap: 'wrap' }}>
									<div>
										<span style={{ fontSize: '14px', color: '#6c757d', marginRight: '8px' }}>
											Fecha y hora de simulación:
										</span>
										<span style={{ fontSize: '14px', fontWeight: '600', color: '#212529' }}>
											{horaSimulada ? new Date(horaSimulada).toLocaleString('es-ES', {
												day: '2-digit',
												month: '2-digit',
												year: 'numeric',
												hour: '2-digit',
												minute: '2-digit',
												second: '2-digit'
											}) : '--:--:--'}
										</span>
									</div>
									<div>
										<span style={{ fontSize: '14px', color: '#6c757d', marginRight: '8px' }}>
											Tiempo transcurrido:
										</span>
										<span style={{ fontSize: '14px', fontWeight: '600', color: '#212529' }}>
											{tickActual} segundos
										</span>
										<span style={{ fontSize: '12px', color: '#6c757d', marginLeft: '8px' }}>
											(Tick: {tickActual})
										</span>
									</div>
								</div>

								{/* Botones de control */}
								<div style={{ display: 'flex', gap: '10px' }}>
									<button
										onClick={handleIniciarSimulacion}
										disabled={simulacionActiva}
										style={{
											padding: '8px 16px',
											borderRadius: '6px',
											border: '1px solid #28a745',
											background: simulacionActiva ? '#e9ecef' : '#28a745',
											color: simulacionActiva ? '#6c757d' : 'white',
											fontSize: '14px',
											fontWeight: '500',
											cursor: simulacionActiva ? 'not-allowed' : 'pointer',
											transition: 'all 0.2s'
										}}
									>
										Iniciar
									</button>
									<button
										onClick={handleDetenerSimulacion}
										style={{
											padding: '8px 16px',
											borderRadius: '6px',
											border: '1px solid #dc3545',
											background: '#dc3545',
											color: 'white',
											fontSize: '14px',
											fontWeight: '500',
											cursor: 'pointer',
											transition: 'all 0.2s'
										}}
									>
										Detener
									</button>
								</div>
							</div>

							{/* ==================== PANEL PRUEBA WEBSOCKET ==================== */}
							<div style={{
								background: '#fff3cd',
								borderRadius: '8px',
								padding: '15px 20px',
								marginBottom: '20px',
								border: '1px solid #ffc107',
							}}>
								<h4 style={{ margin: '0 0 15px 0', fontSize: '16px', color: '#856404' }}>
									🔌 Prueba WebSocket
								</h4>
								
								{/* Indicadores de estado */}
								<div style={{ display: 'flex', gap: '20px', marginBottom: '15px', fontSize: '13px' }}>
									<span style={{ color: wsConectado ? '#28a745' : '#dc3545', fontWeight: '600' }}>
										● {wsConectado ? 'Conectado' : 'Desconectado'}
									</span>
									<span style={{ color: wsActivo ? '#28a745' : '#6c757d', fontWeight: '600' }}>
										Backend: {wsActivo ? 'Activo' : 'Inactivo'}
									</span>
									<span style={{ color: '#6c757d' }}>
										Mensajes: {mensajesWS.length}
									</span>
								</div>

								{/* Botones de control */}
								<div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
									<button onClick={handleConectarWS} disabled={wsConectado} 
										style={{ padding: '6px 12px', fontSize: '13px', background: wsConectado ? '#e9ecef' : '#007bff', color: wsConectado ? '#6c757d' : 'white', border: 'none', borderRadius: '4px', cursor: wsConectado ? 'not-allowed' : 'pointer' }}>
										Conectar
									</button>
									<button onClick={handleDesconectarWS} disabled={!wsConectado}
										style={{ padding: '6px 12px', fontSize: '13px', background: !wsConectado ? '#e9ecef' : '#6c757d', color: !wsConectado ? '#6c757d' : 'white', border: 'none', borderRadius: '4px', cursor: !wsConectado ? 'not-allowed' : 'pointer' }}>
										Desconectar
									</button>
									<button onClick={handleActivarWS}
										style={{ padding: '6px 12px', fontSize: '13px', background: '#28a745', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
										Activar Backend
									</button>
									<button onClick={handleDesactivarWS}
										style={{ padding: '6px 12px', fontSize: '13px', background: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
										Desactivar Backend
									</button>
									<button onClick={handleConsultarEstadoWS}
										style={{ padding: '6px 12px', fontSize: '13px', background: '#17a2b8', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
										Consultar Estado
									</button>
									<button onClick={handleEnviarMensajeWS}
										style={{ padding: '6px 12px', fontSize: '13px', background: '#ffc107', color: '#212529', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
										Enviar Mensaje
									</button>
									<button onClick={handleLimpiarMensajesWS}
										style={{ padding: '6px 12px', fontSize: '13px', background: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
										Limpiar
									</button>
								</div>

								{/* Últimos mensajes */}
								{mensajesWS.length > 0 && (
									<div style={{ marginTop: '15px', maxHeight: '100px', overflow: 'auto', background: 'white', padding: '10px', borderRadius: '4px', fontSize: '12px', fontFamily: 'monospace' }}>
										{mensajesWS.slice(-5).map((msg, idx) => (
											<div key={idx} style={{ marginBottom: '5px', color: '#212529' }}>
												{typeof msg === 'string' ? msg : JSON.stringify(msg)}
											</div>
										))}
									</div>
								)}
							</div>
						</div>
						
						{/* Mapa interactivo */}
						<div className="map-container">
							<MapContainer center={[20.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10} zoomControl={true} doubleClickZoom={true} boxZoom={true} keyboard={true} touchZoom={true}>
								<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} />
								<DynamicMarkers flights={flights} airports={airports} activeView={activeView} showRoutes={showRoutes} />
							</MapContainer>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

export default SimuladorSemanal;
