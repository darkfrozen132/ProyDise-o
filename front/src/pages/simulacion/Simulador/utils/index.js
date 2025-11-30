/**
 * Punto de entrada para utilidades del Simulador Semanal
 * Re-exporta todas las funciones de utilidad para fácil importación
 */

// Time utilities
export {
  parseSimDateUTC,
  bearingDegrees,
  calculateInterpolatedPosition
} from './timeUtils';

// Flight utilities
export {
  getAircraftColorByStatus,
  createFlightPopup,
  normalizeFlightFromWebSocket,
  isFlightActive,
  filterVisibleFlights
} from './flightUtils';

// Map utilities
export {
  createAirplaneIcon,
  createAirportIcon,
  animateMarker,
  createRoutePolyline,
  fitMapToFlights,
  createAirportPopup
} from './mapUtils';
