/**
 * Componente DynamicMarkers
 * Maneja los marcadores dinámicos en el mapa de Leaflet
 * Incluye marcadores de vuelos con animación y marcadores de aeropuertos
 */

import React, { useRef, useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import { 
  createAirplaneIcon, 
  createAirportIcon, 
  animateMarker,
  createFlightPopup 
} from '../utils';

/**
 * Componente para manejar marcadores dinámicos en el mapa
 * 
 * @param {Object} props
 * @param {Array} props.flights - Lista de vuelos base (no necesariamente en movimiento)
 * @param {Array} props.airports - Lista de aeropuertos
 * @param {string} props.activeView - Vista activa: 'airports', 'flights', 'routes'
 * @param {boolean} props.showRoutes - Si se muestran las rutas de vuelo
 * @param {Array} props.vuelosEnMovimiento - Vuelos con posiciones calculadas para el tiempo actual
 */
function DynamicMarkers({ 
  flights, 
  airports, 
  activeView, 
  showRoutes, 
  vuelosEnMovimiento 
}) {
  const map = useMap();
  const markersRef = useRef({}); // Guardar marcadores por ID para animarlos
  const polylinesRef = useRef({});
  
  // Actualizar marcadores cuando cambian vuelos, aeropuertos, vista activa o rutas
  useEffect(() => {
    console.log(`[DynamicMarkers] Recibidos ${flights.length} vuelos, activeView: ${activeView}`);
    
    const airportMarkers = [];
    
    // Limpiar marcadores de aeropuertos antiguos
    map.eachLayer(layer => { 
      if (layer instanceof L.Marker && layer.options.isAirport) {
        map.removeLayer(layer); 
      }
    });
    
    // Añadir marcadores de aeropuertos si la vista es 'airports' o 'flights'
    if (activeView === 'airports' || activeView === 'flights') {
      console.log(`[DynamicMarkers] Añadiendo ${airports.length} aeropuertos al mapa`);
      airports.forEach(airport => {
        const isUnlimited = airport.capacity === 'ILIMITADO';
        const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
        const icon = createAirportIcon(airport.isSede, saturation);
        const marker = L.marker([airport.lat, airport.lng], { 
          icon,
          isAirport: true // Flag para identificar
        }).bindPopup(`
          <div class="popup-content">
            <div class="popup-header">
              <strong class="popup-title ${airport.isSede ? 'sede-title' : 'airport-title'}">
                ${airport.name}
              </strong>
            </div>
          </div>
        `);
        marker.addTo(map); 
        airportMarkers.push(marker);
      });
    }
    
    // Actualizar o crear marcadores de vuelos con animación (SISTEMA REACTIVO)
    if (activeView === 'flights' || activeView === 'routes') {
      console.log(`[DynamicMarkers] Actualizando ${vuelosEnMovimiento.length} vuelos en el mapa`);
      
      const currentFlightIds = new Set();
      
      vuelosEnMovimiento.forEach((flight, index) => {
        // Verificar que las coordenadas sean válidas
        if (!flight.currentLat || !flight.currentLng || 
            isNaN(flight.currentLat) || isNaN(flight.currentLng)) {
          return;
        }
        
        currentFlightIds.add(flight.id);
        const existingMarker = markersRef.current[flight.id];
        
        // Usar posiciones pre-calculadas del useMemo
        const position = { lat: flight.currentLat, lng: flight.currentLng };
        const progress = flight.progress || 0;
        const status = flight.status || 'active';
        
        if (existingMarker) {
          // ANIMAR: Mover marcador existente suavemente a nueva posición
          const currentLatLng = existingMarker.getLatLng();
          const newLatLng = L.latLng(position.lat, position.lng);
          
          // Animar solo si el cambio es significativo (> 100m)
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
          // CREAR: Nuevo marcador para este vuelo
          console.log(`[DynamicMarkers] Vuelo nuevo ${index + 1}: ${flight.id} - Pos: [${position.lat.toFixed(3)}, ${position.lng.toFixed(3)}]`);
          
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
        if (showRoutes && flight.origin && flight.destination) {
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
      
      console.log(`[DynamicMarkers] Total marcadores activos: ${Object.keys(markersRef.current).length}`);
    } else {
      // Si no estamos en vista de vuelos, limpiar todos los marcadores de vuelos
      Object.values(markersRef.current).forEach(marker => map.removeLayer(marker));
      markersRef.current = {};
      
      Object.values(polylinesRef.current).forEach(polyline => map.removeLayer(polyline));
      polylinesRef.current = {};
    }
    
    // Limpiar marcadores de aeropuertos al desmontar
    return () => { 
      airportMarkers.forEach(m => map.removeLayer(m)); 
    };
  }, [vuelosEnMovimiento, airports, activeView, showRoutes, map, flights]);
  
  return null;
}

export default DynamicMarkers;
