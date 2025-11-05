/**
 * Script para generar trayectorias de vuelos
 * Genera datos para 10 vuelos moviéndose durante 30 segundos
 */

// Coordenadas de aeropuertos (usando ubicaciones reales)
const routes = [
  // Vuelo 1: Lima → Bruselas
  { id: "RT0001", from: { lat: -12.0219, lng: -77.1143 }, to: { lat: 50.9014, lng: 4.4844 } },
  // Vuelo 2: Bruselas → Bakú
  { id: "RT0002", from: { lat: 50.9014, lng: 4.4844 }, to: { lat: 40.4675, lng: 50.0467 } },
  // Vuelo 3: Bakú → Lima
  { id: "RT0003", from: { lat: 40.4675, lng: 50.0467 }, to: { lat: -12.0219, lng: -77.1143 } },
  // Vuelo 4: Buenos Aires → Madrid
  { id: "RT0004", from: { lat: -34.8222, lng: -58.5358 }, to: { lat: 40.4983, lng: -3.5676 } },
  // Vuelo 5: Santiago → Ámsterdam
  { id: "RT0005", from: { lat: -33.3930, lng: -70.7858 }, to: { lat: 52.3105, lng: 4.7683 } },
  // Vuelo 6: Bogotá → París
  { id: "RT0006", from: { lat: 4.7016, lng: -74.1469 }, to: { lat: 49.0097, lng: 2.5479 } },
  // Vuelo 7: Madrid → Estambul
  { id: "RT0007", from: { lat: 40.4983, lng: -3.5676 }, to: { lat: 41.2753, lng: 28.7519 } },
  // Vuelo 8: Estambul → Bakú
  { id: "RT0008", from: { lat: 41.2753, lng: 28.7519 }, to: { lat: 40.4675, lng: 50.0467 } },
  // Vuelo 9: Ámsterdam → Bogotá
  { id: "RT0009", from: { lat: 52.3105, lng: 4.7683 }, to: { lat: 4.7016, lng: -74.1469 } },
  // Vuelo 10: París → Buenos Aires
  { id: "RT0010", from: { lat: 49.0097, lng: 2.5479 }, to: { lat: -34.8222, lng: -58.5358 } }
];

// Función de interpolación
function interpolate(start, end, progress) {
  return start + (end - start) * progress;
}

// Generar snapshots para cada segundo (0-29)
function generateSnapshots() {
  const snapshots = [];

  for (let second = 0; second < 30; second++) {
    const progress = second / 29; // 0 a 1

    const flights = routes.map(route => ({
      id: route.id,
      currentLat: parseFloat(interpolate(route.from.lat, route.to.lat, progress).toFixed(4)),
      currentLng: parseFloat(interpolate(route.from.lng, route.to.lng, progress).toFixed(4))
    }));

    snapshots.push(flights);
  }

  return snapshots;
}

// Generar y exportar
const snapshots = generateSnapshots();

// Para Node.js (si quieres ejecutar este script)
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { snapshots };
}

// Para navegador
if (typeof window !== 'undefined') {
  window.flightSnapshots = snapshots;
}

// Log para verificar
console.log('Snapshots generados:', snapshots.length);
console.log('Ejemplo segundo 0:', JSON.stringify(snapshots[0], null, 2));
console.log('Ejemplo segundo 15:', JSON.stringify(snapshots[15], null, 2));
console.log('Ejemplo segundo 29:', JSON.stringify(snapshots[29], null, 2));
