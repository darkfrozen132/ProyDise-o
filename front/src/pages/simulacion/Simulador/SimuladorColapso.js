import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './SImuladorColapso.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
	iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
	iconUrl: require('leaflet/dist/images/marker-icon.png'),
	shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const createAirplaneIcon = (type, color, rotation = 0) => L.divIcon({
	html: `<div style="transform: rotate(${rotation}deg); display:flex; align-items:center; justify-content:center; filter: drop-shadow(0 2px 4px rgba(0,0,0,.2));">
	<svg width="20" height="20" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><ellipse cx="10" cy="10" rx="1.5" ry="8" fill="${color}"/></svg></div>`,
	className: 'airplane-icon', iconSize: [20,20], iconAnchor: [10,10], popupAnchor: [0,-12]
});

const createAirportIcon = (isSede = false, saturation = 0) => new L.DivIcon({
	className: 'airport-marker',
	html: `<div style="background:${saturation>=80?'#dc3545':saturation>=50?'#ffc107':'#28a745'}; border:${isSede?4:3}px solid ${isSede?'#FFD700':'#fff'}; border-radius:50%; width:${isSede?32:22}px; height:${isSede?32:22}px;
	display:flex; align-items:center; justify-content:center;"><i class="fas fa-${isSede ? 'building' : 'plane'}" style="color:white;"></i></div>`,
	iconSize: [isSede?32:22, isSede?32:22], iconAnchor: [(isSede?32:22)/2, (isSede?32:22)/2]
});

function DynamicMarkers({ flights, airports, activeView, showRoutes }) {
	const map = (0, require('react-leaflet').useMap)();
	React.useEffect(() => {
		const airportMarkers = []; const flightMarkers = [];
		map.eachLayer(layer => { if (layer instanceof L.Marker || layer instanceof L.Polyline) map.removeLayer(layer); });
		if (activeView === 'airports' || activeView === 'flights') {
			airports.forEach(airport => {
				const isUnlimited = airport.capacity === 'ILIMITADO'; const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
				const marker = L.marker([airport.lat, airport.lng], { icon: createAirportIcon(airport.isSede, saturation) });
				marker.addTo(map); airportMarkers.push(marker);
			});
		}
		if (activeView === 'flights' || activeView === 'routes') {
			flights.forEach(flight => {
				const marker = L.marker([flight.currentLat, flight.currentLng], { icon: createAirplaneIcon(flight.aircraftType, flight.aircraftColor, flight.rotation) });
				marker.addTo(map); flightMarkers.push(marker);
				if (showRoutes) L.polyline([[flight.origin.lat, flight.origin.lng],[flight.destination.lat, flight.destination.lng]], { color:'#888', weight:2, opacity:.6, dashArray:'5,5' }).addTo(map);
			});
		}
		return () => { airportMarkers.forEach(m=>map.removeLayer(m)); flightMarkers.forEach(m=>map.removeLayer(m)); };
	}, [flights, airports, activeView, showRoutes, map]);
	return null;
}

const SimuladorColapso = () => {
	const navigate = useNavigate();
	const [flights, setFlights] = useState([]);
	const [flightsInAir, setFlightsInAir] = useState(0);
	const [currentTime, setCurrentTime] = useState(new Date(2024, 7, 27, 8, 0, 0));
	const [elapsedTime, setElapsedTime] = useState({ days: 0, hours: 0, minutes: 0 });
	const [isRunning, setIsRunning] = useState(true);
	const [speed, setSpeed] = useState(1);
	const [simulationStatus, setSimulationStatus] = useState('Simulación de colapso activa');
	const [activeView, setActiveView] = useState('flights');
	const [showRoutes, setShowRoutes] = useState(false);
	const [airports, setAirports] = useState([
		{ name: 'Lima-Jorge Chávez', code: 'LIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 1200, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
		{ name: 'Bogotá-El Dorado', code: 'BOG', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 850, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
		{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 1100, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
		{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 1150, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
		{ name: 'París-Charles de Gaulle', code: 'CDG', lat: 49.0097, lng: 2.5479, capacity: 1300, packages: 1250, region: 'Europa', country: 'Francia', operationType: 'Aeropuerto Regional' },
		{ name: 'Baku-Heydar Aliyev', code: 'GYD', lat: 40.4675, lng: 50.0467, capacity: 'ILIMITADO', packages: 1000, isSede: true, region: 'Asia Central', country: 'Azerbaiyán', operationType: 'Sede Principal - Hub Asiático' },
		{ name: 'Estambul', code: 'IST', lat: 41.2619, lng: 28.7419, capacity: 1100, packages: 1050, region: 'Asia Central', country: 'Turquía', operationType: 'Aeropuerto Regional' },
		{ name: 'Dubai', code: 'DXB', lat: 25.2532, lng: 55.3657, capacity: 1200, packages: 1180, region: 'Asia Central', country: 'EAU', operationType: 'Aeropuerto Regional' }
	]);
	const intervalRef = useRef();

	const generateInitialFlights = useCallback(() => {
		const flightCount = 402; const newFlights = [];
		const flightTypes = [
			{ type: 'boeing737', name: 'Boeing 737', capacity: [150, 250] },
			{ type: 'airbus320', name: 'Airbus A320', capacity: [180, 280] },
			{ type: 'boeing777', name: 'Boeing 777', capacity: [300, 400] },
			{ type: 'cargo', name: 'Cargo', capacity: [50, 150] }
		];
		for (let i = 0; i < flightCount; i++) {
			const origin = airports[Math.floor(Math.random() * airports.length)]; let destination = airports[Math.floor(Math.random() * airports.length)]; while (destination === origin) destination = airports[Math.floor(Math.random() * airports.length)];
			const selectedType = flightTypes[Math.floor(Math.random() * flightTypes.length)]; const [minCap, maxCap] = selectedType.capacity; const packageCapacity = Math.floor(Math.random() * (maxCap - minCap + 1)) + minCap;
			// Carga más alta para forzar saturación
			const loadFactor = 0.85 + Math.random() * 0.15; const currentPackages = Math.floor(packageCapacity * loadFactor);
			const loadPercentage = (currentPackages / packageCapacity) * 100; const aircraftColor = loadPercentage >= 80 ? '#dc3545' : loadPercentage >= 50 ? '#ffc107' : '#28a745';
			const deltaLat = destination.lat - origin.lat; const deltaLng = destination.lng - origin.lng; const rotation = Math.atan2(deltaLng, deltaLat) * (180 / Math.PI);
			const initialProgress = Math.random(); const initialLat = origin.lat + deltaLat * initialProgress; const initialLng = origin.lng + deltaLng * initialProgress;
			let initialAltitude = initialProgress < 0.1 ? initialProgress * 100000 : initialProgress < 0.9 ? 35000 : 10000; let initialSpeed = initialProgress < 0.1 ? initialProgress * 2500 : initialProgress < 0.9 ? 500 : 200;
			newFlights.push({ id:`MP${String(i+1).padStart(4,'0')}`, origin, destination, progress:initialProgress, altitude:initialAltitude, speed:initialSpeed, status:'active', packageCapacity, currentPackages, packageType:'MPE', isSameContinentFlight: origin.region === destination.region, currentLat:initialLat, currentLng:initialLng, aircraftType:selectedType.type, aircraftName:selectedType.name, aircraftColor, rotation });
		}
		setFlights(newFlights);
		setFlightsInAir(newFlights.reduce((acc,f)=>acc+(f.altitude>1000?1:0),0));
	}, [airports]);

	useEffect(()=>{ generateInitialFlights(); },[generateInitialFlights]);

	useEffect(()=>{
		if (isRunning) {
			intervalRef.current = setInterval(()=>{
				setCurrentTime(prev=>{ const t=new Date(prev); t.setDate(t.getDate()+7*speed); return t; });
				setElapsedTime(prev=>{ const total = prev.days*24*60 + prev.hours*60 + prev.minutes + 7*24*60*speed; return { days:Math.floor(total/(24*60)), hours:Math.floor((total%(24*60))/60), minutes: total%60 }; });
				setFlights(prevFlights=>{
					const deltas={}; const addDelta=(code,amt)=>{ if(!code||!Number.isFinite(amt))return; deltas[code]=(deltas[code]||0)+amt; };
					const next = prevFlights.map(f=>{
						let newProgress = f.progress + (0.35 * speed); // más rápido hacia completar para aumentar rotación
						const latDiff=f.destination.lat - f.origin.lat, lngDiff=f.destination.lng - f.origin.lng;
						let newLat = f.origin.lat + (latDiff*newProgress), newLng = f.origin.lng + (lngDiff*newProgress);
						let newAltitude, newSpeed; if (newProgress<0.1){ newAltitude=newProgress*100000; newSpeed=newProgress*2500; } else if (newProgress<0.9){ newAltitude=30000+Math.random()*10000; newSpeed=450+Math.random()*200; } else { const p=(newProgress-0.9)/0.1; newAltitude=30000*(1-p); newSpeed=450*(1-p*0.6); }
						if (newProgress>=1){
							// En colapso: origin en sedes (capacidad ilimitada) y destino en aeropuertos regulares para subir la saturación
							const regularSorted = [...airports].filter(a=>!a.isSede && typeof a.capacity==='number').sort((a,b)=> (b.packages/b.capacity) - (a.packages/a.capacity));
							const sedes = airports.filter(a=>a.isSede);
							const newOrigin = sedes.length ? sedes[Math.floor(Math.random()*sedes.length)] : airports[Math.floor(Math.random()*airports.length)];
							let newDestination = regularSorted[0] || airports[Math.floor(Math.random()*airports.length)];
							if (newDestination===newOrigin) newDestination = airports[(airports.indexOf(newOrigin)+1)%airports.length];
							addDelta(f.destination.code, f.currentPackages); addDelta(f.origin.code, -f.currentPackages);
							const [minCap,maxCap] = f.aircraftType==='cargo'?[50,150]:[150,400];
							const newPackageCapacity = Math.floor(Math.random()*(maxCap-minCap+1))+minCap;
							// Carga muy alta para forzar saturación
							const newLoadFactor = 0.9 + Math.random()*0.1; const newCurrentPackages = Math.floor(newPackageCapacity*newLoadFactor);
							const perc = (newCurrentPackages/newPackageCapacity)*100; const newAircraftColor = perc>=80?'#dc3545':perc>=50?'#ffc107':'#28a745';
							const dLat=newDestination.lat-newOrigin.lat, dLng=newDestination.lng-newOrigin.lng; const newRotation=Math.atan2(dLng,dLat)*(180/Math.PI);
							// Restar la carga en el origen (sede) no afecta saturación de regulares
							addDelta(newOrigin.code, -newCurrentPackages);
							return { ...f, origin:newOrigin, destination:newDestination, progress:0, altitude:0, speed:0, currentLat:newOrigin.lat, currentLng:newOrigin.lng, currentPackages:newCurrentPackages, packageCapacity:newPackageCapacity, aircraftColor:newAircraftColor, rotation:newRotation };
						}
						return { ...f, progress:newProgress, altitude:newAltitude, speed:newSpeed, currentLat:newLat, currentLng:newLng };
					});
					if (Object.keys(deltas).length){ setAirports(prev=> prev.map(a=>{ const d=deltas[a.code]||0; if(!d) return a; let p=a.packages+d; if(p<0) p=0; if(typeof a.capacity==='number') p=Math.min(p,a.capacity); return { ...a, packages:p }; })); }
					setFlightsInAir(next.reduce((acc,f)=>acc+(f.altitude>1000?1:0),0));
					return next;
				});
			},1000);
			return ()=>clearInterval(intervalRef.current);
		} else { clearInterval(intervalRef.current); }
	},[isRunning, speed, airports]);

	const getSaturation = () => {
		const regular = airports.filter(a=>!a.isSede && typeof a.capacity==='number');
		if(!regular.length) return '0.00';
		const cap = regular.reduce((s,a)=>s+a.capacity,0), pk = regular.reduce((s,a)=>s+a.packages,0);
		return cap? ((pk/cap)*100).toFixed(2):'0.00';
	};

	const collapsed = Number(getSaturation()) >= 90; // umbral de colapso

	return (
		<div className="section-content">
			<div className="simulation-sidebar">
				<div className="sidebar-header"><h3><i className="fas fa-cogs"></i> Modos de Operación</h3></div>
				<div className="sidebar-menu">
					<button className="sidebar-item" onClick={() => navigate('/simulador')}><i className="fas fa-broadcast-tower"></i><span>Monitoreo en Tiempo Real</span></button>
					<button className="sidebar-item" onClick={() => navigate('/simulador-semanal')}><i className="fas fa-calendar-week"></i><span>Simulación Semanal</span></button>
					<button className="sidebar-item active"><i className="fas fa-exclamation-triangle"></i><span>Simulación de Colapso</span></button>
				</div>
			</div>
			<div className="simulation-main-content">
				<div className="content-wrapper">
					<div className={`control-panel ${collapsed?'collapsed-state':''}`}>
						<h2>Simulación de Colapso</h2>
						<div className="status-section"><span className="status-label">Estado:</span><span className="status-text">{collapsed? '¡COLAPSO DEL SISTEMA!':'Simulación en ejecución'}</span></div>
						<div className="simulation-controls">
							<div className="control-buttons">
								<button className="sim-control-btn play-btn" onClick={()=>setIsRunning(true)}><i className="fas fa-play"></i> Iniciar</button>
								<button className="sim-control-btn pause-btn" onClick={()=>setIsRunning(false)}><i className="fas fa-pause"></i> Pausar</button>
								<button className="sim-control-btn stop-btn" onClick={()=>setIsRunning(false)}><i className="fas fa-stop"></i> Detener</button>
							</div>
						</div>
					</div>
					<div className="map-controls-panel">
						<button className="map-btn flights-btn active"><i className="fas fa-plane"></i> Vuelos</button>
						<button className="map-btn show-lines-btn" onClick={()=>{}}><i className="fas fa-road"></i> Mostrar Líneas</button>
					</div>
					<div className="map-container">
						<MapContainer center={[20,10]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10} zoomControl={true}>
							<TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} />
							<DynamicMarkers flights={flights} airports={airports} activeView={'flights'} showRoutes={false} />
						</MapContainer>
					</div>
				</div>
			</div>
		</div>
	);
};

export default SimuladorColapso;
