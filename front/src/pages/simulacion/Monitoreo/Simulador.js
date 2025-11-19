import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import { Drawer, Dialog, DialogTitle, DialogContent, IconButton } from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import L from 'leaflet';
import LegendButton from "../../../components/ui/Button/LegendButton";
import LegendDialog from "../../../components/ui/Dialog/LegendDialog";
import 'leaflet/dist/leaflet.css';
import './Simulador.css';

// Fix for default markers
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

// Custom airplane icon
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

  return L.divIcon({
    html: `<div style="transform: rotate(${rotation}deg); display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));">
             ${iconSvg[type]}
           </div>`,
    className: 'airplane-icon',
    iconSize: type === 'cargo' ? [24, 24] : type === 'boeing777' ? [22, 22] : [20, 20],
    iconAnchor: type === 'cargo' ? [12, 12] : type === 'boeing777' ? [11, 11] : [10, 10],
    popupAnchor: [0, -12]
  });
};

// Custom airport icon
const createAirportIcon = (isSede = false, saturation = 0) => {
  let size, color, borderColor, borderWidth, shadow;

  if (isSede) {
    size = 32;
    color = '#dc3545';
    borderColor = '#FFD700';
    borderWidth = 4;
    shadow = '0 4px 16px rgba(220, 53, 69, 0.6)';
  } else {
    size = 22;
    borderColor = '#ffffff';
    borderWidth = 3;
    shadow = '0 3px 10px rgba(0,0,0,0.4)';

    if (saturation >= 80) {
      color = '#dc3545';
    } else if (saturation >= 50) {
      color = '#ffc107';
    } else {
      color = '#28a745';
    }
  }

  return new L.DivIcon({
    className: 'airport-marker',
    html: `<div style="
        background: ${color};
        border: ${borderWidth}px solid ${borderColor};
        border-radius: 50%;
        width: ${size}px;
        height: ${size}px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: ${shadow};
        position: relative;
        cursor: pointer;
        transition: all 0.3s ease;
      ">
        <i class="fas fa-${isSede ? 'building' : 'plane'}" style="
          color: white;
          font-size: ${size * 0.4}px;
          ${isSede ? '' : 'transform: rotate(45deg);'}
          text-shadow: 0 1px 3px rgba(0,0,0,0.5);
          font-weight: bold;
        "></i>
        ${isSede ? `
          <div style="
            position: absolute; 
            top: -6px; 
            right: -6px; 
            background: #FFD700;
            border: 2px solid white; 
            border-radius: 50%; 
            width: 12px; 
            height: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          ">
            <i class="fas fa-star" style="color: white; font-size: 6px;"></i>
          </div>
        ` : ''}
        ${!isSede && saturation >= 80 ? `
          <div style="
            position: absolute;
            top: -4px;
            right: -4px;
            background: #FF0000;
            border: 1px solid white;
            border-radius: 50%;
            width: 8px;
            height: 8px;
            animation: alertBlink 1s infinite;
          "></div>
        ` : ''}
      </div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2]
  });
};

// Component to update markers dynamically
function DynamicMarkers({ flights, airports, activeView, showRoutes }) {
  const map = useMap();
  useEffect(() => {
    const airportMarkers = [];
    const flightMarkers = [];

    // Clear existing markers
    map.eachLayer(layer => {
      if (layer instanceof L.Marker || layer instanceof L.Polyline) {
        map.removeLayer(layer);
      }
    });

    // Add airport markers
    if (activeView === 'airports' || activeView === 'flights') {
      airports.forEach(airport => {
        const isUnlimited = airport.capacity === 'ILIMITADO';
        const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
        const icon = createAirportIcon(airport.isSede, saturation);

        const marker = L.marker([airport.lat, airport.lng], { icon })
          .bindPopup(`
            <div class="popup-content">
              <div class="popup-header">
                <strong class="popup-title ${airport.isSede ? 'sede-title' : 'airport-title'}">${airport.name}</strong>
                ${airport.isSede ? `<span class="sede-label">🏢 SEDE PRINCIPAL</span>` : ''}
              </div>
              <div class="popup-details">
                <div><span>País:</span> <strong>${airport.country}</strong></div>
                <div><span>Código:</span> <strong>${airport.code}</strong></div>
                <div><span>Región:</span> <strong>${airport.region}</strong></div>
                <div><span>Tipo:</span> <strong>${airport.operationType}</strong></div>
              </div>
              <div class="popup-capacity">
                ${isUnlimited ? `
                  <div class="unlimited-capacity">
                    <span>♾️ CAPACIDAD ILIMITADA</span>
                    <small>Esta sede principal cuenta con almacenamiento ilimitado</small>
                    <div class="capacity-info">
                      <strong>${airport.packages.toLocaleString()}</strong> paquetes actuales
                    </div>
                  </div>
                ` : `
                  <div class="limited-capacity">
                    <div class="capacity-bar-container">
                      <div class="capacity-bar" style="width: ${Math.min(saturation, 100)}%; background-color: ${saturation >= 80 ? '#dc3545' : saturation >= 50 ? '#ffc107' : '#28a745'};"></div>
                    </div>
                    <div class="capacity-text">
                      <div class="capacity-info">
                        <strong>${airport.packages.toLocaleString()}</strong> paquetes / <strong>${typeof airport.capacity === 'number' ? airport.capacity.toLocaleString() : airport.capacity}</strong> capacidad
                      </div>
                      <div class="capacity-percentage">
                        ${saturation.toFixed(1)}%
                      </div>
                    </div>
                    ${saturation >= 80 ? `<div class="capacity-alert">⚠️ ALMACÉN EN RIESGO DE SATURACIÓN</div>` : ''}
                  </div>
                `}
              </div>
            </div>
          `);
        marker.addTo(map);
        airportMarkers.push(marker);
      });
    }

    // Add flight markers and routes
    if (activeView === 'flights' || activeView === 'routes') {
      flights.forEach(flight => {
        const icon = createAirplaneIcon(flight.aircraftType, flight.aircraftColor, flight.rotation);
        const loadPercentage = (flight.currentPackages / flight.packageCapacity) * 100;
        const isIntercontinental = !flight.isSameContinentFlight;

        const marker = L.marker([flight.currentLat, flight.currentLng], { icon })
          .bindPopup(`
            <div class="popup-content">
              <div class="popup-header">
                <strong class="popup-title">✈️ Vuelo ${flight.id}</strong>
                <span class="flight-type-label ${isIntercontinental ? 'intercontinental' : 'intracontinental'}">
                  ${isIntercontinental ? 'INTERCONTINENTAL' : 'INTRACONTINENTAL'}
                </span>
              </div>
              <div class="popup-route-info">
                <div>🛫 Origen: <strong>${flight.origin.name} (${flight.origin.code})</strong></div>
                <div>🛬 Destino: <strong>${flight.destination.name} (${flight.destination.code})</strong></div>
              </div>
              <div class="popup-progress">
                <div class="progress-bar-container">
                  <div class="progress-bar" style="width: ${Math.min(flight.progress * 100, 100)}%;"></div>
                </div>
                <span>${(flight.progress * 100).toFixed(1)}% completado</span>
              </div>
              <div class="popup-metrics">
                <div><span>Altitud:</span> <strong>${Math.round(flight.altitude).toLocaleString()} ft</strong></div>
                <div><span>Velocidad:</span> <strong>${Math.round(flight.speed)} nudos</strong></div>
              </div>
              <div class="popup-capacity">
                <div class="capacity-bar-container">
                  <div class="capacity-bar" style="width: ${Math.min(loadPercentage, 100)}%; background-color: ${loadPercentage >= 80 ? '#dc3545' : loadPercentage >= 50 ? '#ffc107' : '#28a745'};"></div>
                </div>
                <div class="capacity-info">
                  <strong>${flight.currentPackages}</strong> / <strong>${flight.packageCapacity}</strong> paquetes
                </div>
              </div>
            </div>
          `);
        marker.addTo(map);
        flightMarkers.push(marker);

        if (showRoutes) {
          const polyline = L.polyline([[flight.origin.lat, flight.origin.lng], [flight.destination.lat, flight.destination.lng]], {
            color: '#888',
            weight: 2,
            opacity: 0.6,
            dashArray: '5, 5'
          });
          polyline.addTo(map);
        }
      });
    }

    return () => {
      airportMarkers.forEach(marker => map.removeLayer(marker));
      flightMarkers.forEach(marker => map.removeLayer(marker));
    };
  }, [flights, airports, activeView, showRoutes, map]);

  return null;
}

const Simulador = () => {
  const navigate = useNavigate();
  const [flights, setFlights] = useState([]);
  const [flightsInAir, setFlightsInAir] = useState(0);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [showLegend, setShowLegend] = useState(false);
  const activeView = 'flights';
  const showRoutes = false;
  const speed = 1;
  const intervalRef = useRef();

  const [airports] = useState([
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

  // Hardcoded recurring schedule (wall-clock driven)
  const airportsByCode = useMemo(() => {
    const map = new Map();
    airports.forEach(a => map.set(a.code, a));
    return map;
  }, [airports]);

  const SCHEDULE = useMemo(() => [
    // Regional (América del Sur / Europa)
    { id: 'R1', o: 'LIM', d: 'BOG', intervalMin: 180, durationMin: 150, type: 'boeing737', offsets: [0, 60] },
    { id: 'R2', o: 'LIM', d: 'MEX', intervalMin: 240, durationMin: 330, type: 'boeing777', offsets: [30] },
    { id: 'R3', o: 'LIM', d: 'BRU', intervalMin: 720, durationMin: 840, type: 'boeing777', offsets: [120] },
    { id: 'R4', o: 'BOG', d: 'LIM', intervalMin: 180, durationMin: 150, type: 'airbus320', offsets: [20, 110] },
    { id: 'R5', o: 'BRU', d: 'AMS', intervalMin: 90, durationMin: 60, type: 'airbus320', offsets: [15, 45] },
    { id: 'R6', o: 'AMS', d: 'CDG', intervalMin: 120, durationMin: 80, type: 'airbus320', offsets: [10, 70] },
    { id: 'R7', o: 'CDG', d: 'LHR', intervalMin: 120, durationMin: 70, type: 'airbus320', offsets: [5, 65] },
    { id: 'R8', o: 'FRA', d: 'MAD', intervalMin: 180, durationMin: 140, type: 'airbus320', offsets: [25] },
    { id: 'R9', o: 'IST', d: 'DXB', intervalMin: 240, durationMin: 220, type: 'boeing777', offsets: [60] },
    { id: 'R10', o: 'DXB', d: 'IST', intervalMin: 240, durationMin: 220, type: 'boeing777', offsets: [120] },
    { id: 'R11', o: 'BRU', d: 'LIM', intervalMin: 720, durationMin: 840, type: 'boeing777', offsets: [360] },
    { id: 'R12', o: 'MEX', d: 'LIM', intervalMin: 240, durationMin: 330, type: 'boeing777', offsets: [150] },
  ], []);

  const computeRealtimeFlights = useCallback((now, speedFactor = 1) => {
    const ms = now.getTime();
    const flightsNow = [];
    const dayAnchor = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();

    let counter = 1;
    SCHEDULE.forEach(route => {
      const origin = airportsByCode.get(route.o);
      const destination = airportsByCode.get(route.d);
      if (!origin || !destination) return;
      const durationMs = route.durationMin * 60 * 1000;
      const intervalMs = route.intervalMin * 60 * 1000;
      (route.offsets || [0]).forEach(offMin => {
        const offsetMs = offMin * 60 * 1000;
        // number of cycles since anchor
        const since = (ms - (dayAnchor + offsetMs)) * Math.max(1, speedFactor);
        const mod = ((since % intervalMs) + intervalMs) % intervalMs; // positive modulo
        if (mod < durationMs) {
          const progress = Math.min(Math.max(mod / durationMs, 0), 1);
          const latDiff = destination.lat - origin.lat;
          const lngDiff = destination.lng - origin.lng;
          const currentLat = origin.lat + latDiff * progress;
          const currentLng = origin.lng + lngDiff * progress;
          let altitude, speed;
          if (progress < 0.1) { altitude = progress * 100000; speed = progress * 2500; }
          else if (progress < 0.9) { altitude = 30000 + 5000; speed = 500; }
          else { const p = (progress - 0.9) / 0.1; altitude = 30000 * (1 - p); speed = 450 * (1 - p * 0.6); }
          const rotation = Math.atan2(lngDiff, latDiff) * (180 / Math.PI);
          const capacity = route.type === 'cargo' ? 120 : route.type === 'boeing777' ? 360 : 220;
          const loadPct = route.type === 'boeing777' ? 0.78 : route.type === 'airbus320' ? 0.62 : 0.68;
          const currentPackages = Math.floor(capacity * loadPct);
          const color = loadPct >= 0.8 ? '#dc3545' : loadPct >= 0.5 ? '#ffc107' : '#28a745';
          flightsNow.push({
            id: `MP${String(counter++).padStart(4, '0')}`,
            origin, destination, progress, altitude, speed, status: 'active',
            packageCapacity: capacity, currentPackages, packageType: 'MPE', isSameContinentFlight: origin.region === destination.region,
            currentLat, currentLng, aircraftType: route.type, aircraftName: route.type.toUpperCase(), aircraftColor: color, rotation
          });
        }
      });
    });
    return flightsNow;
  }, [SCHEDULE, airportsByCode]);

  useEffect(() => {
    const tick = () => {
      const now = new Date();
      setCurrentTime(now);
      const nextFlights = computeRealtimeFlights(now, speed);
      setFlights(nextFlights);
      const inAir = nextFlights.reduce((acc, f) => acc + (f.altitude > 1000 ? 1 : 0), 0);
      setFlightsInAir(inAir);
    };
    tick();
    intervalRef.current = setInterval(tick, 1000);
    return () => clearInterval(intervalRef.current);
  }, [computeRealtimeFlights, speed]);

  const getSaturation = () => {
    const regularAirports = airports.filter(airport => !airport.isSede);
    if (regularAirports.length === 0) return "0.00";
    const totalCapacity = regularAirports.reduce((sum, airport) => sum + (typeof airport.capacity === 'number' ? airport.capacity : 0), 0);
    const totalPackages = regularAirports.reduce((sum, airport) => sum + airport.packages, 0);
    if (totalCapacity === 0) return "0.00";
    return ((totalPackages / totalCapacity) * 100).toFixed(2);
  };
  const getMostSaturatedAirport = () => {
    const regularAirports = airports.filter(airport => !airport.isSede && typeof airport.capacity === 'number');
    if (regularAirports.length === 0) return { name: 'N/A', capacity: 0, packages: 0, saturation: 0 };
    return regularAirports.reduce((max, airport) => {
      const saturation = (airport.packages / airport.capacity) * 100;
      const maxSaturation = (max.packages / max.capacity) * 100;
      return saturation > maxSaturation ? airport : max;
    });
  };

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
              <label>Hora actual:</label>
              <div className="time-display">{currentTime.toLocaleDateString('es-ES')}, {currentTime.toLocaleTimeString('es-ES')}</div>
            </div>
          </div>
          {/* Metricas de saturación */}
          <div className="stats-section">
            <h4><i className="fas fa-chart-line"></i> Métricas de Saturación</h4>
            <div className="metrics-grid">
              <div className="metric-card">
                <div className="metric-icon"><i className="fas fa-plane"></i></div>
                <div className="metric-content">
                  <div className="metric-label">Vuelos en el aire</div>
                  <div className="metric-value">{getFlightsByAltitude()}</div>
                  <div className="metric-sublabel">de 402 total</div>
                </div>
              </div>
              <div className="metric-card">
                <div className="metric-icon aircraft"><i className="fas fa-tachometer-alt"></i></div>
                <div className="metric-content">
                  <div className="metric-label">Saturación de aviones</div>
                  <div className="metric-value">{((getFlightsByAltitude() / 402) * 100).toFixed(1)}%</div>
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
      <div className="simulation-main-content">
        <div className="content-wrapper">
          <div className="map-container">
            <MapContainer
              center={[20.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={false} minZoom={2} maxZoom={10}
              zoomControl={true} doubleClickZoom={true} boxZoom={true} keyboard={true} touchZoom={true}>
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" attribution='© OpenStreetMap contributors' noWrap={true} />
              <DynamicMarkers flights={flights} airports={airports} activeView={activeView} showRoutes={showRoutes} />
            </MapContainer>
            {/* Botón de leyenda flotante */}
            <LegendButton onClick={() => setShowLegend(true)} />
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

export default Simulador;