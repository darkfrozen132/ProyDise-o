import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer } from 'react-leaflet';
import { IoArrowBackOutline } from "react-icons/io5";
import Button from '@mui/material/Button';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './SimuladorSemanal.css';

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
	/* Datos estáticos de aeropuertos */
	const [airports, setAirports] = useState([
		{ name: 'Lima-Jorge Chávez', code: 'LIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 980, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
		{ name: 'Bogotá-El Dorado', code: 'BOG', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 720, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
		{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 850, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
		{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 960, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
		{ name: 'París-Charles de Gaulle', code: 'CDG', lat: 49.0097, lng: 2.5479, capacity: 1300, packages: 1040, region: 'Europa', country: 'Francia', operationType: 'Aeropuerto Regional' },
		{ name: 'Baku-Heydar Aliyev', code: 'GYD', lat: 40.4675, lng: 50.0467, capacity: 'ILIMITADO', packages: 800, isSede: true, region: 'Asia Central', country: 'Azerbaiyán', operationType: 'Sede Principal - Hub Asiático' },
		{ name: 'Estambul', code: 'IST', lat: 41.2619, lng: 28.7419, capacity: 1100, packages: 880, region: 'Asia Central', country: 'Turquía', operationType: 'Aeropuerto Regional' },
		{ name: 'Dubai', code: 'DXB', lat: 25.2532, lng: 55.3657, capacity: 1200, packages: 960, region: 'Asia Central', country: 'EAU', operationType: 'Aeropuerto Regional' },
		{ name: 'Madrid-Barajas', code: 'MAD', lat: 40.4983, lng: -3.5676, capacity: 900, packages: 720, region: 'Europa', country: 'España', operationType: 'Aeropuerto Regional' },
		{ name: 'México City', code: 'MEX', lat: 19.4363, lng: -99.0721, capacity: 800, packages: 640, region: 'América del Norte', country: 'México', operationType: 'Aeropuerto Regional' },
		{ name: 'Londres-Heathrow', code: 'LHR', lat: 51.4700, lng: -0.4543, capacity: 1400, packages: 1120, region: 'Europa', country: 'Reino Unido', operationType: 'Aeropuerto Regional' },
		{ name: 'Frankfurt', code: 'FRA', lat: 50.0379, lng: 8.5622, capacity: 1250, packages: 1000, region: 'Europa', country: 'Alemania', operationType: 'Aeropuerto Regional' }
	]);
	const intervalRef = useRef(); /* Referencia para el intervalo de simulación */

	/* Generar vuelos iniciales con lógica de origen, destino, tipo de avión, capacidad y carga */
	const generateInitialFlights = useCallback(() => {
		const flightCount = 402; const newFlights = []; const sedes = airports.filter(a => a.isSede);
		const flightTypes = [
			{ type: 'boeing737', name: 'Boeing 737', capacity: [150, 250] },
			{ type: 'airbus320', name: 'Airbus A320', capacity: [180, 280] },
			{ type: 'boeing777', name: 'Boeing 777', capacity: [300, 400] },
			{ type: 'cargo', name: 'Cargo', capacity: [50, 150] }
		];
		for (let i = 0; i < flightCount; i++) {
			let origin, destination;
			if (Math.random() < 0.6) {
				const sede = sedes[Math.floor(Math.random() * sedes.length)];
				const otherAirports = airports.filter(a => a.region === sede.region && a !== sede);
				if (Math.random() < 0.5) { origin = sede; destination = otherAirports.length > 0 ? otherAirports[Math.floor(Math.random() * otherAirports.length)] : airports[Math.floor(Math.random() * airports.length)]; }
				else { destination = sede; origin = otherAirports.length > 0 ? otherAirports[Math.floor(Math.random() * otherAirports.length)] : airports[Math.floor(Math.random() * airports.length)]; }
			} else { origin = airports[Math.floor(Math.random() * airports.length)]; destination = airports[Math.floor(Math.random() * airports.length)]; }
			while (destination === origin) destination = airports[Math.floor(Math.random() * airports.length)];
			const selectedType = flightTypes[Math.floor(Math.random() * flightTypes.length)];
			const [minCap, maxCap] = selectedType.capacity; const packageCapacity = Math.floor(Math.random() * (maxCap - minCap + 1)) + minCap;
			const loadFactor = 0.7 + Math.random() * 0.3; const currentPackages = Math.floor(packageCapacity * loadFactor);
			const loadPercentage = (currentPackages / packageCapacity) * 100; let aircraftColor; if (loadPercentage >= 80) aircraftColor = '#dc3545'; else if (loadPercentage >= 50) aircraftColor = '#ffc107'; else aircraftColor = '#28a745';
			const deltaLat = destination.lat - origin.lat; const deltaLng = destination.lng - origin.lng; const rotation = Math.atan2(deltaLng, deltaLat) * (180 / Math.PI);
			const initialProgress = Math.random(); const initialLat = origin.lat + deltaLat * initialProgress; const initialLng = origin.lng + deltaLng * initialProgress;
			let initialAltitude, initialSpeed; if (initialProgress < 0.1) { initialAltitude = initialProgress * 100000; initialSpeed = initialProgress * 2500; }
			else if (initialProgress < 0.9) { initialAltitude = 35000; initialSpeed = 500; }
			else { const landingProgress = (initialProgress - 0.9) / 0.1; initialAltitude = 30000 * (1 - landingProgress); initialSpeed = 450 * (1 - landingProgress * 0.6); }
			const flight = {
				id: `MP${String(i + 1).padStart(4, '0')}`,
				origin, destination, progress: initialProgress, altitude: initialAltitude, speed: initialSpeed, status: 'active',
				packageCapacity, currentPackages, packageType: 'MPE', isSameContinentFlight: origin.region === destination.region,
				currentLat: initialLat, currentLng: initialLng, aircraftType: selectedType.type,
				aircraftName: selectedType.name, aircraftColor, rotation
			};
			newFlights.push(flight);
		}
		setFlights(newFlights);
		const initialInAir = newFlights.reduce((acc, f) => acc + (f.altitude > 1000 ? 1 : 0), 0);
		setFlightsInAir(initialInAir);
	}, [airports]);

	/* Generar vuelos iniciales al montar el componente */
	useEffect(() => { generateInitialFlights(); }, [generateInitialFlights]);

	/* Lógica de simulación que avanza el tiempo y actualiza vuelos cada segundo */
	useEffect(() => {
		if (isRunning) {
			/* Avance semanal cada segundo */
			intervalRef.current = setInterval(() => {
				/* Avanzar tiempo actual */
				setCurrentTime(prev => { const newTime = new Date(prev); newTime.setDate(newTime.getDate() + 7 * speed); return newTime; });
				/* Actualizar tiempo transcurrido */
				setElapsedTime(prev => {
					const totalMinutes = prev.days * 24 * 60 + prev.hours * 60 + prev.minutes + 7 * 24 * 60 * speed;
					return { days: Math.floor(totalMinutes / (24 * 60)), hours: Math.floor((totalMinutes % (24 * 60)) / 60), minutes: totalMinutes % 60 };
				});
				/* Actualizar estado de vuelos */
				setFlights(prevFlights => {
					const deltas = {}; const addDelta = (code, amount) => { if (!code || !Number.isFinite(amount)) return; deltas[code] = (deltas[code] || 0) + amount; };
					const nextFlights = prevFlights.map(flight => {
						let newProgress = flight.progress + (0.25 * speed); // avance semanal
						const latDiff = flight.destination.lat - flight.origin.lat; const lngDiff = flight.destination.lng - flight.origin.lng;
						let newLat = flight.origin.lat + (latDiff * newProgress); let newLng = flight.origin.lng + (lngDiff * newProgress);
						let newAltitude, newSpeed;
						if (newProgress < 0.1) { newAltitude = newProgress * 100000; newSpeed = newProgress * 2500; }
						else if (newProgress < 0.9) { newAltitude = 30000 + Math.random() * 10000; newSpeed = 450 + Math.random() * 200; }
						else { const landingProgress = (newProgress - 0.9) / 0.1; newAltitude = 30000 * (1 - landingProgress); newSpeed = 450 * (1 - landingProgress * 0.6); }
						/* Si el vuelo ha llegado a su destino, actualizar paquetes y reasignar vuelo */
						if (newProgress >= 1) {
							addDelta(flight.destination.code, flight.currentPackages);
							addDelta(flight.origin.code, -flight.currentPackages);
							const newOrigin = airports[Math.floor(Math.random() * airports.length)]; let newDestination = airports[Math.floor(Math.random() * airports.length)];
							while (newDestination === newOrigin) newDestination = airports[Math.floor(Math.random() * airports.length)];
							const selectedType = flight.aircraftType; const [minCap, maxCap] = selectedType === 'cargo' ? [50, 150] : [150, 400];
							const newPackageCapacity = Math.floor(Math.random() * (maxCap - minCap + 1)) + minCap; const newLoadFactor = 0.7 + Math.random() * 0.3;
							const newCurrentPackages = Math.floor(newPackageCapacity * newLoadFactor); const newLoadPercentage = (newCurrentPackages / newPackageCapacity) * 100;
							let newAircraftColor; if (newLoadPercentage >= 80) newAircraftColor = '#dc3545'; else if (newLoadPercentage >= 50) newAircraftColor = '#ffc107'; else newAircraftColor = '#28a745';
							const newDeltaLat = newDestination.lat - newOrigin.lat; const newDeltaLng = newDestination.lng - newOrigin.lng;
							const newRotation = Math.atan2(newDeltaLng, newDeltaLat) * (180 / Math.PI);
							addDelta(newOrigin.code, -newCurrentPackages);
							return { ...flight, origin: newOrigin, destination: newDestination, progress: 0, altitude: 0, speed: 0, currentLat: newOrigin.lat, currentLng: newOrigin.lng, currentPackages: newCurrentPackages, packageCapacity: newPackageCapacity, aircraftColor: newAircraftColor, rotation: newRotation };
						}
						return { ...flight, progress: newProgress, altitude: newAltitude, speed: newSpeed, currentLat: newLat, currentLng: newLng };
					});
					/* Actualizar paquetes en aeropuertos según deltas calculados */
					const codes = Object.keys(deltas);
					if (codes.length > 0) {
						setAirports(prev => prev.map(a => { const delta = deltas[a.code] || 0; if (!delta) return a; let nextPackages = a.packages + delta; if (nextPackages < 0) nextPackages = 0; if (typeof a.capacity === 'number') nextPackages = Math.min(nextPackages, a.capacity); return { ...a, packages: nextPackages }; }));
					}
					/* Contar vuelos en el aire */
					const inAir = nextFlights.reduce((acc, f) => acc + (f.altitude > 1000 ? 1 : 0), 0);
					setFlightsInAir(inAir);
					return nextFlights;
				});
			}, 1000);
			return () => clearInterval(intervalRef.current);
		} else { clearInterval(intervalRef.current); }
	}, [isRunning, speed, airports]);

	const handlePlay = () => { setIsRunning(true); setSimulationStatus('Simulación semanal en ejecución'); };
	const handlePause = () => { setIsRunning(false); setSimulationStatus('Simulación semanal pausada'); };
	const handleStop = () => { setIsRunning(false); setSimulationStatus('Simulación semanal detenida'); };
	const handleSpeedChange = () => { const speeds = [1, 2, 4, 8]; const currentIndex = speeds.indexOf(speed); const nextSpeed = speeds[(currentIndex + 1) % speeds.length]; setSpeed(nextSpeed); };
	const handleRestart = () => { setIsRunning(false); setElapsedTime({ days: 0, hours: 0, minutes: 0 }); setCurrentTime(new Date(2024, 7, 27, 8, 0, 0)); generateInitialFlights(); setSimulationStatus('Sistema reiniciado'); setTimeout(() => { setSimulationStatus('Monitoreo semanal activo'); }, 2000); };
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

	return (
		<div className="section-content" id="simulationSection">
			<div className="simulation-sidebar">
				<div className="sidebar-header">
					<h3><i className="fas fa-info-circle"></i> Información del Sistema</h3>
				</div>
				<div className="sidebar-content">
					<div className="time-section">
						<div className="current-time">
							<label>Semana actual:</label>
							<div className="time-display">Semana {elapsedTime.days + 1}</div>
						</div>
					</div>
					<div className="modes-sidebar-container">
						<Button
							variant="outlined"
							size="small"
							startIcon={<IoArrowBackOutline />}  
							sx={{
								textTransform: 'none',            
								color: '#333',
								borderColor: '#b3b3b3',
								backgroundColor: '#f9f9f9',
								fontWeight: 500,
								borderRadius: '20px',
								px: 2,                             
								py: 0.5,                           
								'&:hover': {
									backgroundColor: '#e0e0e0',
									borderColor: '#999',
								},
								width: 'auto',
								minWidth: 'unset',
							}}
							onClick={() => navigate('/operaciones')}
						>
							Regresar
						</Button>
					</div>
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
					<div className="system-alerts">
						<div className="alert-header">⚠️ ALERTAS DEL SISTEMA</div>
						<div className="alert-list">
							<p>• Simulación semanal en progreso</p>
							<p>• Replanificación automática activada</p>
							<p>• Control de capacidades por semanas</p>
						</div>
					</div>
				</div>
			</div>
			<div className="simulation-main-content">
				<div className="content-wrapper">
					<div className="control-panel">
						<h2>Simulación Semanal</h2>
						<div className="status-section">
							<span className="status-label">Estado de la Simulación:</span>
							<span className="status-text">{simulationStatus}</span>
						</div>
						<div className="simulation-controls">
							<div className="control-buttons">
								<button className="sim-control-btn play-btn" onClick={handlePlay}>
									<i className="fas fa-play"></i> Iniciar
								</button>
							</div>
						</div>
					</div>
					<div className="map-container">
						<MapContainer center={[20.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10} zoomControl={true} doubleClickZoom={true} boxZoom={true} keyboard={true} touchZoom={true}>
							<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} />
							<DynamicMarkers flights={flights} airports={airports} activeView={activeView} showRoutes={showRoutes} />
						</MapContainer>
					</div>
				</div>
			</div>
		</div>
	);
};

export default SimuladorSemanal;
