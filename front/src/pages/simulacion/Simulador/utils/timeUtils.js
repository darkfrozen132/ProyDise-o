/**
 * Utilidades relacionadas con tiempo, fechas y cálculos de interpolación
 * para el Simulador Semanal de vuelos
 */

/**
 * Parsea una fecha string del simulador a objeto Date en UTC
 * @param {string} str - Fecha en formato "YYYY-MM-DD HH:MM" 
 * @returns {Date|null} - Date en UTC o null si string vacío
 */
export const parseSimDateUTC = (str) => {
  return str ? new Date(str.replace(' ', 'T') + ':00Z') : null;
};

/**
 * Calcula el rumbo geodésico en grados (0°=N, 90°=E, 180°=S, 270°=W)
 * Si el SVG "mira a la derecha", usar (brg - 90 + 360) % 360
 * @param {number} lat1 - Latitud origen
 * @param {number} lon1 - Longitud origen
 * @param {number} lat2 - Latitud destino
 * @param {number} lon2 - Longitud destino
 * @returns {number} - Ángulo en grados (0-360)
 */
export function bearingDegrees(lat1, lon1, lat2, lon2) {
  const toRad = d => d * Math.PI / 180;
  const toDeg = r => r * 180 / Math.PI;
  const phi1 = toRad(lat1), phi2 = toRad(lat2);
  const deltaLambda = toRad(lon2 - lon1);
  const y = Math.sin(deltaLambda) * Math.cos(phi2);
  const x = Math.cos(phi1) * Math.cos(phi2) - Math.sin(phi1) * Math.sin(phi2) * Math.cos(deltaLambda);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

/**
 * Calcula la posición interpolada de un vuelo basándose en el tiempo actual
 * @param {Object} vuelo - Objeto vuelo con origin, destination, fechaInicial, fechaFinal
 * @param {number} tiempoActualMs - Timestamp actual en milisegundos
 * @returns {Object} - {lat, lng, progress, status}
 */
export function calculateInterpolatedPosition(vuelo, tiempoActualMs) {
  // Verificar que el vuelo tenga timestamps
  if (!vuelo.fechaInicial || !vuelo.fechaFinal) {
    console.warn(`[timeUtils] Vuelo ${vuelo.id} sin timestamps - usando fallback`);
    // Fallback: retornar posición actual
    return {
      lat: vuelo.currentLat || vuelo.origin?.lat,
      lng: vuelo.currentLng || vuelo.origin?.lng,
      progress: vuelo.progress || 0,
      status: vuelo.status || 'active'
    };
  }

  // Extraer timestamps
  const horaSalida = new Date(vuelo.fechaInicial).getTime();
  const horaLlegada = new Date(vuelo.fechaFinal).getTime();
  const duracionVuelo = horaLlegada - horaSalida;
  
  // DEBUG: Log solo cada 2 segundos para no saturar consola
  if (Math.random() < 0.02) {
    const tiempoActual = new Date(tiempoActualMs);
    const salida = new Date(horaSalida);
    const llegada = new Date(horaLlegada);
    const enVuelo = tiempoActualMs >= horaSalida && tiempoActualMs < horaLlegada;
    console.log(`[timeUtils] Interpolando ${vuelo.id}:`);
    console.log(`   Salida:  ${salida.toISOString()}`);
    console.log(`   Llegada: ${llegada.toISOString()}`);
    console.log(`   Actual:  ${tiempoActual.toISOString()} ${enVuelo ? 'EN VUELO' : 'ESPERA'}`);
    console.log(`   Duracion: ${(duracionVuelo / 1000 / 60).toFixed(0)} minutos`);
  }

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

  // DEBUG: Log de posición calculada (solo algunos frames)
  if (Math.random() < 0.02) {
    console.log(`   Progreso: ${progreso.toFixed(1)}% | Pos: [${lat.toFixed(3)}, ${lng.toFixed(3)}]`);
  }

  return {
    lat,
    lng,
    progress: progreso,
    status: 'active'
  };
}
