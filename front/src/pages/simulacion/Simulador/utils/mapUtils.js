/**
 * Utilidades relacionadas con el mapa de Leaflet
 * para el Simulador Semanal
 */

import L from 'leaflet';
import { bearingDegrees } from './timeUtils';
import { getAircraftColorByStatus } from './flightUtils';

/**
 * Crea un icono de avión personalizado con rotación
 * @param {Object} flight - Objeto vuelo para determinar el color
 * @param {number} rotation - Ángulo de rotación en grados (0 = norte)
 * @returns {L.DivIcon} - Icono de Leaflet
 */
export function createAirplaneIcon(flight, rotation = 0) {
  // Determinar color basado en el estado del vuelo
  const color = getAircraftColorByStatus(flight);
  
  const iconSvg = `<svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
      <ellipse cx="11" cy="11" rx="2" ry="10" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
      <ellipse cx="11" cy="8" rx="9" ry="1.8" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
      <ellipse cx="11" cy="15" rx="4" ry="1.2" fill="${color}" stroke="#ffffff" stroke-width="0.5"/>
      <path d="M11 17 L11 19.5 L10 19.5 L10 17 Z" fill="${color}" stroke="#ffffff" stroke-width="0.3"/>
    </svg>`;

  // Crear divIcon con el SVG correspondiente y transición suave
  return L.divIcon({
    html: `<div style="transform: rotate(${rotation}deg); display: flex; align-items: center; justify-content: center; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); transition: transform 0.3s ease-out, filter 0.3s ease-out;">${iconSvg}</div>`,
    className: 'airplane-icon-animated',
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -12]
  });
}

/**
 * Crea un icono de aeropuerto personalizado
 * @param {boolean} isSede - Si es aeropuerto sede/hub
 * @param {number} saturation - Nivel de saturación (0-100) para determinar color
 * @returns {L.DivIcon} - Icono de Leaflet
 */
export function createAirportIcon(isSede = false, saturation = 0) {
  let size, color, borderColor, borderWidth, shadow;
  
  // Color y tamaño según tipo y saturación
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
    
    // Color según saturación
    if (saturation >= 80) {
      color = '#dc3545'; // Rojo - Muy saturado
    } else if (saturation >= 50) {
      color = '#ffc107'; // Amarillo - Moderadamente saturado
    } else {
      color = '#28a745'; // Verde - Baja saturación
    }
  }
  
  // Crear divIcon con estilos
  return new L.DivIcon({
    className: 'airport-marker',
    html: `<div style="background: ${color}; border: ${borderWidth}px solid ${borderColor}; border-radius: 50%; width: ${size}px; height: ${size}px; display:flex;align-items:center;justify-content:center; box-shadow:${shadow}; position:relative; cursor:pointer; transition: all .3s ease;">
      <i class="fas fa-${isSede ? 'building' : 'plane'}" style="color:white; font-size:${size * 0.4}px; ${isSede ? '' : 'transform: rotate(45deg);'} text-shadow:0 1px 3px rgba(0,0,0,.5);"></i>
    </div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2]
  });
}

/**
 * Anima el movimiento suave de un marcador entre dos posiciones
 * También actualiza la rotación del avión en la dirección del movimiento
 * @param {L.Marker} marker - Marcador de Leaflet
 * @param {Object} startLatLng - Posición inicial {lat, lng}
 * @param {Object} endLatLng - Posición final {lat, lng}
 * @param {number} duration - Duración de la animación en ms (default 1000)
 */
export function animateMarker(marker, startLatLng, endLatLng, duration = 1000) {
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

/**
 * Crea una polilínea de ruta entre dos puntos
 * @param {Object} origin - {lat, lng}
 * @param {Object} destination - {lat, lng}
 * @param {Object} options - Opciones de estilo
 * @returns {L.Polyline} - Polilínea de Leaflet
 */
export function createRoutePolyline(origin, destination, options = {}) {
  const defaultOptions = {
    color: '#3b82f6',
    weight: 2,
    opacity: 0.6,
    dashArray: '5, 10'
  };
  
  return L.polyline(
    [[origin.lat, origin.lng], [destination.lat, destination.lng]],
    { ...defaultOptions, ...options }
  );
}

/**
 * Centra el mapa en un conjunto de vuelos
 * @param {L.Map} map - Instancia del mapa
 * @param {Array} flights - Array de vuelos con origin y destination
 */
export function fitMapToFlights(map, flights) {
  if (!flights || flights.length === 0) return;
  
  const bounds = L.latLngBounds();
  
  flights.forEach(flight => {
    if (flight.origin) {
      bounds.extend([flight.origin.lat, flight.origin.lng]);
    }
    if (flight.destination) {
      bounds.extend([flight.destination.lat, flight.destination.lng]);
    }
    if (flight.currentLat && flight.currentLng) {
      bounds.extend([flight.currentLat, flight.currentLng]);
    }
  });
  
  if (bounds.isValid()) {
    map.fitBounds(bounds, { padding: [50, 50] });
  }
}

/**
 * Crea un popup HTML para un aeropuerto
 * @param {Object} airport - Datos del aeropuerto
 * @returns {string} - HTML del popup
 */
export function createAirportPopup(airport) {
  const isSede = airport.esSede || airport.codigo === 'SPIM' || airport.codigo === 'EBCI' || airport.codigo === 'UBBB';
  
  return `
    <div style="min-width: 180px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
      <div style="background: ${isSede ? '#dc3545' : '#3b82f6'}; color: white; padding: 8px 12px; margin: -10px -10px 10px -10px; border-radius: 4px 4px 0 0;">
        <strong style="font-size: 14px;">${isSede ? '🏢' : '✈️'} ${airport.codigo || airport.code}</strong>
      </div>
      <div style="padding: 4px 0;">
        <div style="font-size: 13px; color: #1f2937; margin-bottom: 4px;">
          ${airport.nombre || airport.name || 'Sin nombre'}
        </div>
        ${airport.ciudad || airport.city ? 
          `<div style="font-size: 12px; color: #6b7280;">
            ${airport.ciudad || airport.city}${airport.pais || airport.country ? `, ${airport.pais || airport.country}` : ''}
          </div>` : ''
        }
        ${airport.capacidad !== undefined ?
          `<div style="font-size: 11px; color: #9ca3af; margin-top: 4px;">
            Capacidad: ${airport.capacidad}
          </div>` : ''
        }
      </div>
    </div>
  `;
}
