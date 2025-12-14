import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet';
import { Box, Button, Typography, Paper, CircularProgress, Alert, Chip, LinearProgress } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import RefreshIcon from '@mui/icons-material/Refresh';
import FlightIcon from '@mui/icons-material/Flight';
import InventoryIcon from '@mui/icons-material/Inventory';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL, WS_URL } from '../../../config/api';
import { getAirports } from '../../../config/api';
import './SimuladorSemanal.css'; // Reutilizamos los estilos

/* Reparar iconos por defecto de Leaflet */
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
	iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
	iconUrl: require('leaflet/dist/images/marker-icon.png'),
	shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

/**
 * SimuladorDiario - Componente para Operación Diaria
 * 
 * A diferencia del SimuladorSemanal:
 * - NO requiere seleccionar fecha
 * - Procesa TODOS los pedidos de la tabla pedidos_diario de una vez
 * - Interfaz simplificada
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
const SimuladorDiario = () => {
	const navigate = useNavigate();
	
	// ==================== ESTADO PRINCIPAL ====================
	const [simulacionActiva, setSimulacionActiva] = useState(false);
	const [cargando, setCargando] = useState(false);
	const [error, setError] = useState(null);
	const [sessionId, setSessionId] = useState(null);
	
	// ==================== MÉTRICAS ====================
	const [totalPedidos, setTotalPedidos] = useState(0);
	const [pedidosProcesados, setPedidosProcesados] = useState(0);
	const [vuelosGenerados, setVuelosGenerados] = useState(0);
	const [generacionActual, setGeneracionActual] = useState(0);
	const [maxGeneraciones, setMaxGeneraciones] = useState(0);
	const [mejorFitness, setMejorFitness] = useState(0);
	const [progreso, setProgreso] = useState(0);
	
	// ==================== DATOS DE MAPA ====================
	const [airports, setAirports] = useState([]);
	const [flights, setFlights] = useState([]);
	const [loadingAirports, setLoadingAirports] = useState(true);
	
	// ==================== WEBSOCKET ====================
	const stompClientRef = useRef(null);
	const subscriptionRef = useRef(null);
	const [wsConectado, setWsConectado] = useState(false);
	
	// ==================== MENSAJES/LOG ====================
	const [mensajes, setMensajes] = useState([]);
	
	// ==================== EFECTO: CARGAR AEROPUERTOS ====================
	useEffect(() => {
		const cargarAeropuertos = async () => {
			try {
				setLoadingAirports(true);
				const data = await getAirports();
				setAirports(data || []);
				console.log(`✅ Cargados ${data?.length || 0} aeropuertos`);
			} catch (err) {
				console.error('❌ Error cargando aeropuertos:', err);
				setError('Error al cargar aeropuertos');
			} finally {
				setLoadingAirports(false);
			}
		};
		cargarAeropuertos();
	}, []);
	
	// ==================== EFECTO: CONECTAR WEBSOCKET ====================
	useEffect(() => {
		const conectarWebSocket = () => {
			console.log('🔌 Conectando WebSocket STOMP...');
			
			const socket = new SockJS(`${WS_URL}/ws`);
			const client = new Client({
				webSocketFactory: () => socket,
				reconnectDelay: 5000,
				heartbeatIncoming: 4000,
				heartbeatOutgoing: 4000,
				debug: (str) => {
					if (str.includes('CONNECTED') || str.includes('ERROR')) {
						console.log('🔌 STOMP:', str);
					}
				},
				onConnect: () => {
					console.log('✅ WebSocket STOMP conectado');
					setWsConectado(true);
				},
				onDisconnect: () => {
					console.log('⚠️ WebSocket desconectado');
					setWsConectado(false);
				},
				onStompError: (frame) => {
					console.error('❌ Error STOMP:', frame.headers?.message || 'Error desconocido');
					setWsConectado(false);
				}
			});
			
			client.activate();
			stompClientRef.current = client;
		};
		
		conectarWebSocket();
		
		return () => {
			if (subscriptionRef.current) {
				subscriptionRef.current.unsubscribe();
			}
			if (stompClientRef.current) {
				stompClientRef.current.deactivate();
			}
		};
	}, []);
	
	// ==================== PROCESAR MENSAJE DE SIMULACIÓN ====================
	const procesarMensajeSimulacion = useCallback((datos) => {
		console.log('📨 Mensaje recibido:', datos.tipo || datos.type);
		
		// Agregar al log
		setMensajes(prev => [...prev.slice(-50), {
			timestamp: new Date().toLocaleTimeString(),
			tipo: datos.tipo || datos.type || 'INFO',
			mensaje: datos.mensaje || JSON.stringify(datos).substring(0, 100)
		}]);
		
		// Procesar según tipo
		const tipo = datos.tipo || datos.type;
		
		if (tipo === 'PROGRESO_AG') {
			setGeneracionActual(datos.generacion || 0);
			setMaxGeneraciones(datos.maxGeneraciones || 0);
			setMejorFitness(datos.mejorFitness || 0);
			setProgreso(datos.progreso || 0);
			setPedidosProcesados(datos.pedidosProcesados || 0);
			setTotalPedidos(datos.pedidosTotales || 0);
			
			// Si hay solución, actualizar vuelos
			if (datos.solucion?.rutas) {
				const rutasArray = Array.isArray(datos.solucion.rutas) 
					? datos.solucion.rutas 
					: Object.values(datos.solucion.rutas);
				setVuelosGenerados(rutasArray.length);
			}
		} else if (tipo === 'ESTADO_SIMULACION') {
			if (datos.estado === 'COMPLETADO') {
				setSimulacionActiva(false);
				setCargando(false);
			}
		} else if (tipo === 'FLIGHT_UPDATE') {
			// Actualizar vuelos en el mapa
			if (datos.flights) {
				setFlights(datos.flights);
			}
		}
	}, []);
	
	// ==================== INICIAR SIMULACIÓN DIARIA ====================
	const iniciarSimulacion = async () => {
		setError(null);
		setCargando(true);
		setMensajes([]);
		setProgreso(0);
		setGeneracionActual(0);
		setPedidosProcesados(0);
		setVuelosGenerados(0);
		
		try {
			console.log('🚀 Iniciando simulación diaria...');
			
			const response = await fetch(`${API_BASE_URL}/api/simulations/daily/start`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					factorK: 5,
					tamanioPoblacion: 3,
					maxGeneraciones: 1,
					limiteGeneracionesSinMejora: 1
				})
			});
			
			if (!response.ok) {
				throw new Error(`Error HTTP: ${response.status}`);
			}
			
			const data = await response.json();
			console.log('📨 Respuesta del servidor:', data);
			
			if (data.sessionId) {
				setSessionId(data.sessionId);
				
				// Suscribirse al topic
				if (stompClientRef.current?.connected) {
					const topicUrl = `/topic/simulations/${data.sessionId}`;
					console.log(`📡 Suscribiéndose a: ${topicUrl}`);
					
					const subscription = stompClientRef.current.subscribe(
						topicUrl,
						(message) => {
							try {
								const datos = JSON.parse(message.body);
								procesarMensajeSimulacion(datos);
							} catch (err) {
								console.error('❌ Error parseando mensaje:', err);
							}
						}
					);
					subscriptionRef.current = subscription;
				}
				
				setSimulacionActiva(true);
				console.log('✅ Simulación diaria iniciada');
			} else {
				throw new Error(data.mensaje || 'No se recibió sessionId');
			}
			
		} catch (err) {
			console.error('❌ Error al iniciar simulación:', err);
			setError(err.message);
		} finally {
			setCargando(false);
		}
	};
	
	// ==================== DETENER SIMULACIÓN ====================
	const detenerSimulacion = async () => {
		if (!sessionId) return;
		
		try {
			console.log('🛑 Deteniendo simulación...');
			
			await fetch(`${API_BASE_URL}/api/simulations/daily/${sessionId}/cancel`, {
				method: 'POST'
			});
			
			if (subscriptionRef.current) {
				subscriptionRef.current.unsubscribe();
				subscriptionRef.current = null;
			}
			
			setSimulacionActiva(false);
			setSessionId(null);
			console.log('✅ Simulación detenida');
			
		} catch (err) {
			console.error('❌ Error al detener:', err);
		}
	};
	
	// ==================== LIMPIAR ESTADO ====================
	const limpiarEstado = () => {
		setMensajes([]);
		setProgreso(0);
		setGeneracionActual(0);
		setPedidosProcesados(0);
		setVuelosGenerados(0);
		setMejorFitness(0);
		setFlights([]);
		setError(null);
	};
	
	// ==================== RENDER ====================
	return (
		<Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#f5f5f5' }}>
			{/* Header */}
			<Paper 
				elevation={2} 
				sx={{ 
					p: 2, 
					display: 'flex', 
					justifyContent: 'space-between', 
					alignItems: 'center',
					borderRadius: 0
				}}
			>
				<Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
					<Typography variant="h5" sx={{ fontWeight: 'bold', color: '#1976d2' }}>
						🌅 Operación Diaria
					</Typography>
					<Chip 
						icon={wsConectado ? <FlightIcon /> : null}
						label={wsConectado ? 'Conectado' : 'Desconectado'} 
						color={wsConectado ? 'success' : 'error'}
						size="small"
					/>
				</Box>
				
				<Box sx={{ display: 'flex', gap: 1 }}>
					{!simulacionActiva ? (
						<Button 
							variant="contained" 
							color="primary" 
							startIcon={cargando ? <CircularProgress size={20} color="inherit" /> : <PlayArrowIcon />}
							onClick={iniciarSimulacion}
							disabled={cargando || !wsConectado}
						>
							{cargando ? 'Iniciando...' : 'Iniciar Simulación'}
						</Button>
					) : (
						<Button 
							variant="contained" 
							color="error" 
							startIcon={<StopIcon />}
							onClick={detenerSimulacion}
						>
							Detener
						</Button>
					)}
					<Button 
						variant="outlined" 
						startIcon={<RefreshIcon />}
						onClick={limpiarEstado}
						disabled={simulacionActiva}
					>
						Limpiar
					</Button>
					<Button 
						variant="outlined"
						onClick={() => navigate('/operaciones')}
					>
						Volver
					</Button>
				</Box>
			</Paper>
			
			{/* Error Alert */}
			{error && (
				<Alert severity="error" sx={{ mx: 2, mt: 1 }}>
					{error}
				</Alert>
			)}
			
			{/* Main Content */}
			<Box sx={{ flex: 1, display: 'flex', p: 2, gap: 2, overflow: 'hidden' }}>
				{/* Panel de métricas */}
				<Paper sx={{ width: 350, p: 2, overflow: 'auto' }}>
					<Typography variant="h6" gutterBottom>
						📊 Métricas
					</Typography>
					
					{/* Progreso general */}
					<Box sx={{ mb: 3 }}>
						<Typography variant="body2" color="text.secondary">
							Progreso del Algoritmo Genético
						</Typography>
						<LinearProgress 
							variant="determinate" 
							value={progreso} 
							sx={{ height: 10, borderRadius: 5, my: 1 }}
						/>
						<Typography variant="body2">
							Generación {generacionActual} / {maxGeneraciones || '?'}
						</Typography>
					</Box>
					
					{/* Stats grid */}
					<Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, mb: 3 }}>
						<Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#e3f2fd' }}>
							<InventoryIcon color="primary" />
							<Typography variant="h4">{pedidosProcesados}</Typography>
							<Typography variant="caption">Pedidos Procesados</Typography>
						</Paper>
						<Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#e8f5e9' }}>
							<FlightIcon color="success" />
							<Typography variant="h4">{vuelosGenerados}</Typography>
							<Typography variant="caption">Vuelos Generados</Typography>
						</Paper>
					</Box>
					
					<Box sx={{ mb: 3 }}>
						<Typography variant="body2" color="text.secondary">
							Total Pedidos: <strong>{totalPedidos}</strong>
						</Typography>
						<Typography variant="body2" color="text.secondary">
							Mejor Fitness: <strong>{mejorFitness.toFixed(2)}</strong>
						</Typography>
					</Box>
					
					{/* Log de mensajes */}
					<Typography variant="subtitle2" gutterBottom>
						📜 Log de Eventos
					</Typography>
					<Paper 
						variant="outlined" 
						sx={{ 
							maxHeight: 300, 
							overflow: 'auto', 
							p: 1, 
							bgcolor: '#fafafa',
							fontFamily: 'monospace',
							fontSize: '0.75rem'
						}}
					>
						{mensajes.length === 0 ? (
							<Typography variant="caption" color="text.secondary">
								Esperando mensajes...
							</Typography>
						) : (
							mensajes.map((msg, idx) => (
								<Box key={idx} sx={{ mb: 0.5 }}>
									<Typography 
										variant="caption" 
										component="span"
										sx={{ color: '#666' }}
									>
										[{msg.timestamp}]
									</Typography>
									{' '}
									<Typography 
										variant="caption" 
										component="span"
										sx={{ 
											color: msg.tipo === 'ERROR' ? 'error.main' : 'text.primary',
											fontWeight: msg.tipo === 'ERROR' ? 'bold' : 'normal'
										}}
									>
										{msg.tipo}: {msg.mensaje}
									</Typography>
								</Box>
							))
						)}
					</Paper>
				</Paper>
				
				{/* Mapa */}
				<Paper sx={{ flex: 1, overflow: 'hidden' }}>
					{loadingAirports ? (
						<Box sx={{ 
							height: '100%', 
							display: 'flex', 
							alignItems: 'center', 
							justifyContent: 'center' 
						}}>
							<CircularProgress />
							<Typography sx={{ ml: 2 }}>Cargando mapa...</Typography>
						</Box>
					) : (
						<MapContainer
							center={[0, 0]}
							zoom={2}
							style={{ height: '100%', width: '100%' }}
							scrollWheelZoom={true}
						>
							<TileLayer
								attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
								url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
							/>
							
							{/* Aeropuertos */}
							{airports.map(airport => (
								<Marker 
									key={airport.codigoICAO}
									position={[airport.latitud, airport.longitud]}
								>
									<Popup>
										<strong>{airport.codigoICAO}</strong><br/>
										{airport.ciudad}, {airport.pais}
									</Popup>
								</Marker>
							))}
							
							{/* Vuelos (líneas) */}
							{flights.map((flight, idx) => {
								if (!flight.origin || !flight.destination) return null;
								return (
									<Polyline
										key={`flight-${idx}`}
										positions={[
											[flight.origin.lat, flight.origin.lng],
											[flight.destination.lat, flight.destination.lng]
										]}
										color="#1976d2"
										weight={2}
										opacity={0.6}
									/>
								);
							})}
						</MapContainer>
					)}
				</Paper>
			</Box>
		</Box>
	);
};

export default SimuladorDiario;
