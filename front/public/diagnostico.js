// 🔍 Script de Diagnóstico WebSocket
// Copiar y pegar en la consola del navegador (F12 → Console)

console.log('🔍 ========== DIAGNÓSTICO DEL SISTEMA ==========');

// 1. Verificar React App
console.log('\n📱 1. Estado de la aplicación React:');
try {
  const rootElement = document.getElementById('root');
  console.log('   ✅ Root element existe:', !!rootElement);
  console.log('   ✅ React renderizado:', rootElement?.children.length > 0);
} catch (e) {
  console.error('   ❌ Error:', e.message);
}

// 2. Verificar Leaflet Map
console.log('\n🗺️ 2. Estado del mapa Leaflet:');
try {
  const mapContainer = document.querySelector('.leaflet-container');
  console.log('   ✅ Contenedor del mapa:', !!mapContainer);
  
  const markers = document.querySelectorAll('.leaflet-marker-pane .leaflet-marker-icon');
  console.log('   📍 Total de marcadores:', markers.length);
  
  const airportMarkers = Array.from(markers).filter(m => 
    m.innerHTML.includes('fa-plane') || m.innerHTML.includes('fa-building')
  );
  console.log('   🏢 Marcadores de aeropuertos:', airportMarkers.length);
  
  const airplaneMarkers = Array.from(markers).filter(m => 
    m.innerHTML.includes('✈') || m.querySelector('.airplane-icon')
  );
  console.log('   ✈️ Marcadores de aviones:', airplaneMarkers.length);
} catch (e) {
  console.error('   ❌ Error:', e.message);
}

// 3. Verificar WebSocket
console.log('\n📡 3. Estado del WebSocket:');
try {
  // Buscar elementos de estado en el DOM
  const wsStatus = document.querySelector('[class*="websocket"], [class*="connection"]');
  console.log('   ℹ️ Elemento de estado:', wsStatus?.textContent || 'No encontrado');
  
  // Verificar conexión por logs recientes
  const hasWSLogs = performance.getEntriesByType('navigation').length > 0;
  console.log('   ℹ️ Página cargada:', hasWSLogs);
} catch (e) {
  console.error('   ❌ Error:', e.message);
}

// 4. Verificar datos en memoria (si están expuestos)
console.log('\n💾 4. Datos en memoria:');
try {
  // Intentar acceder a datos globales (si existen)
  if (typeof window.flights !== 'undefined') {
    console.log('   ✅ Vuelos cargados:', window.flights?.length || 0);
  } else {
    console.log('   ℹ️ Variable window.flights no expuesta');
  }
  
  if (typeof window.airports !== 'undefined') {
    console.log('   ✅ Aeropuertos cargados:', window.airports?.length || 0);
  } else {
    console.log('   ℹ️ Variable window.airports no expuesta');
  }
} catch (e) {
  console.error('   ❌ Error:', e.message);
}

// 5. Verificar logs de consola recientes
console.log('\n📋 5. Instrucciones para diagnóstico manual:');
console.log('   1. Buscar en consola: "📍 Aeropuertos disponibles"');
console.log('      → Debe mostrar: "📍 Aeropuertos disponibles: 30"');
console.log('');
console.log('   2. Buscar en consola: "📨 Mensaje recibido"');
console.log('      → Si aparece: WebSocket está funcionando ✅');
console.log('      → Si NO aparece: Backend no está enviando mensajes ❌');
console.log('');
console.log('   3. Buscar en consola: "✈️ Procesando segments"');
console.log('      → Si aparece: Estructura correcta ✅');
console.log('      → Si NO aparece: Formato JSON diferente ⚠️');
console.log('');
console.log('   4. Buscar en consola: "🎬 Sistema híbrido ACTIVADO"');
console.log('      → Si aparece: Interpolación temporal funcionando ✅');
console.log('      → Si NO aparece: Sin timestamps o estructura incorrecta ⚠️');
console.log('');
console.log('   5. Buscar en consola: "❌ Aeropuerto no encontrado"');
console.log('      → Si aparece: Códigos de aeropuerto no coinciden ❌');

console.log('\n🔍 ========== FIN DEL DIAGNÓSTICO ==========');
console.log('');
console.log('📝 PRÓXIMOS PASOS:');
console.log('1. Copiar TODOS los logs desde el inicio de la página');
console.log('2. Buscar los mensajes mencionados arriba');
console.log('3. Reportar qué aparece y qué NO aparece');
console.log('');
console.log('💡 TIP: Usa Ctrl+F para buscar emojis en la consola:');
console.log('   - 📍 (Aeropuertos)');
console.log('   - 📨 (Mensajes WebSocket)');
console.log('   - ✈️ (Vuelos)');
console.log('   - 🎬 (Sistema híbrido)');
console.log('   - ❌ (Errores)');
