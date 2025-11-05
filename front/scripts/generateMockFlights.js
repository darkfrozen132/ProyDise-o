const fs = require('fs');
const path = require('path');

// Rutas de vuelos con coordenadas de origen y destino
const routes = [
  { id: "RT0001", from: { lat: -12.0219, lng: -77.1143 }, to: { lat: 50.9014, lng: 4.4844 } },
  { id: "RT0002", from: { lat: 50.9014, lng: 4.4844 }, to: { lat: 40.4675, lng: 50.0467 } },
  { id: "RT0003", from: { lat: 40.4675, lng: 50.0467 }, to: { lat: -12.0219, lng: -77.1143 } },
  { id: "RT0004", from: { lat: -34.8222, lng: -58.5358 }, to: { lat: 40.4983, lng: -3.5676 } },
  { id: "RT0005", from: { lat: -33.3930, lng: -70.7858 }, to: { lat: 52.3105, lng: 4.7683 } },
  { id: "RT0006", from: { lat: 4.7016, lng: -74.1469 }, to: { lat: 49.0097, lng: 2.5479 } },
  { id: "RT0007", from: { lat: 40.4983, lng: -3.5676 }, to: { lat: 41.2753, lng: 28.7519 } },
  { id: "RT0008", from: { lat: 41.2753, lng: 28.7519 }, to: { lat: 40.4675, lng: 50.0467 } },
  { id: "RT0009", from: { lat: 52.3105, lng: 4.7683 }, to: { lat: 4.7016, lng: -74.1469 } },
  { id: "RT0010", from: { lat: 49.0097, lng: 2.5479 }, to: { lat: -34.8222, lng: -58.5358 } }
];

// Función de interpolación
function interpolate(start, end, progress) {
  return parseFloat((start + (end - start) * progress).toFixed(4));
}

// Función para calcular el ángulo de rotación del avión
function calculateAngle(fromLat, fromLng, toLat, toLng) {
  const deltaLat = toLat - fromLat;
  const deltaLng = toLng - fromLng;
  // Calcular ángulo en grados (atan2 da el ángulo correcto considerando los cuadrantes)
  const angleRadians = Math.atan2(deltaLng, deltaLat);
  const angleDegrees = angleRadians * (180 / Math.PI);
  return parseFloat(angleDegrees.toFixed(2));
}

// Crear directorio si no existe
const outputDir = path.join(__dirname, '..', 'public', 'mockData');
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Generar 30 archivos JSON (uno por segundo)
for (let second = 0; second < 30; second++) {
  const progress = second / 29; // 0 a 1 (dividido entre 29 para que el último sea 1.0)

  const flights = routes.map(route => {
    // Calcular el ángulo de rotación basado en la dirección del vuelo
    const angle = calculateAngle(route.from.lat, route.from.lng, route.to.lat, route.to.lng);

    return {
      id: route.id,
      currentLat: interpolate(route.from.lat, route.to.lat, progress),
      currentLng: interpolate(route.from.lng, route.to.lng, progress),
      angle: angle
    };
  });

  const filename = `segundo_${second}.json`;
  const filepath = path.join(outputDir, filename);

  fs.writeFileSync(filepath, JSON.stringify(flights, null, 2));
  console.log(`✓ Creado: ${filename}`);
}

console.log('\n✅ Se generaron 30 archivos JSON exitosamente en public/mockData/');
console.log('📁 Ubicación:', outputDir);
