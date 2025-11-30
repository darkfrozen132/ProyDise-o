# 🗺️ EJEMPLO DE USO: PlanificacionService con Visualización en Mapa

## 📋 Resumen

Este documento muestra cómo usar el `PlanificacionService` actualizado para **cargar aeropuertos**, **conectar WebSocket**, **recibir rutas** y **pintarlas en un mapa**.

---

## 🚀 FLUJO COMPLETO

### Paso 1: Importar el servicio

```javascript
import { planificacionService } from '../services/PlanificacionService';
```

### Paso 2: Configurar callbacks

```javascript
// Callback cuando se conecta
planificacionService.onConnected(() => {
  console.log('✅ WebSocket conectado');
});

// Callback para progreso
planificacionService.onProgreso((progreso) => {
  console.log(`📊 Generación ${progreso.generacionActual}/${progreso.totalGeneraciones}`);
  console.log(`⭐ Fitness: ${progreso.mejorFitness}`);
  console.log(`📈 Progreso: ${progreso.porcentaje.toFixed(1)}%`);
});

// 🗺️ NUEVO: Callback para rutas con coordenadas
planificacionService.onRutasProcesadas((rutasProcesadas) => {
  console.log(`🗺️ Recibidas ${rutasProcesadas.length} rutas con coordenadas`);
  pintarRutasEnMapa(rutasProcesadas); // ← TU FUNCIÓN DE PINTADO
});

// Callback cuando se completa
planificacionService.onCompletado((data) => {
  console.log('🏁 Planificación completada');
  console.log('✅ Mejor solución:', data.mejorFitness);
});

// Callback de errores
planificacionService.onError((error) => {
  console.error('❌ Error:', error.message);
});
```

### Paso 3: Inicializar (Cargar aeropuertos + Conectar)

```javascript
async function inicializar() {
  try {
    // 1. Cargar aeropuertos primero (IMPORTANTE)
    console.log('📍 Cargando aeropuertos...');
    await planificacionService.cargarAeropuertos();
    console.log('✅ Aeropuertos cargados');
    
    // 2. Conectar WebSocket
    console.log('🔌 Conectando WebSocket...');
    await planificacionService.connect();
    console.log('✅ WebSocket conectado');
    
    // 3. Ya está listo para iniciar simulación
    return true;
    
  } catch (error) {
    console.error('❌ Error en inicialización:', error);
    return false;
  }
}

// Llamar al montar el componente
inicializar();
```

### Paso 4: Iniciar simulación

```javascript
async function iniciarSimulacion() {
  try {
    const params = {
      tiempoActualSimulacion: "2025-01-02T08:00:00", // ISO format
      factorK: 5,
      tamanioPoblacion: 10,
      maxGeneraciones: 10,
      limiteGeneracionesSinMejora: 5
    };
    
    console.log('🚀 Iniciando simulación...');
    await planificacionService.iniciar(params);
    
    // El servicio se suscribirá automáticamente cuando reciba el sessionId
    // y empezará a llamar a tus callbacks
    
  } catch (error) {
    console.error('❌ Error al iniciar simulación:', error);
  }
}
```

### Paso 5: Pintar rutas en el mapa

```javascript
// Esta función será llamada automáticamente cuando lleguen rutas
function pintarRutasEnMapa(rutasProcesadas) {
  console.log(`🗺️ Pintando ${rutasProcesadas.length} rutas en el mapa...`);
  
  // Limpiar rutas anteriores (depende de tu librería)
  // mapa.clearLayers(); // Leaflet
  // markers.forEach(m => m.setMap(null)); // Google Maps
  
  rutasProcesadas.forEach((ruta, index) => {
    console.log(`Ruta ${index + 1}:`, {
      de: `${ruta.origen.ciudad} (${ruta.origen.codigo})`,
      a: `${ruta.destino.ciudad} (${ruta.destino.codigo})`,
      distancia: `${ruta.distanciaKm} km`,
      duracion: `${ruta.duracionHoras} hrs`
    });
    
    // ✨ OPCIÓN 1: LEAFLET
    // Agregar línea de ruta
    L.polyline([
      [ruta.origen.lat, ruta.origen.lon],
      [ruta.destino.lat, ruta.destino.lon]
    ], {
      color: '#10b981', // Verde
      weight: 2,
      opacity: 0.7
    }).addTo(mapa);
    
    // Agregar marcador de origen
    L.marker([ruta.origen.lat, ruta.origen.lon])
      .bindPopup(`
        <b>${ruta.origen.ciudad}</b><br>
        ${ruta.origen.codigo}<br>
        Salida: ${ruta.salida}
      `)
      .addTo(mapa);
    
    // Agregar marcador de destino
    L.marker([ruta.destino.lat, ruta.destino.lon])
      .bindPopup(`
        <b>${ruta.destino.ciudad}</b><br>
        ${ruta.destino.codigo}<br>
        Llegada: ${ruta.llegada}
      `)
      .addTo(mapa);
    
    // ✨ OPCIÓN 2: GOOGLE MAPS
    // new google.maps.Polyline({
    //   path: [
    //     { lat: ruta.origen.lat, lng: ruta.origen.lon },
    //     { lat: ruta.destino.lat, lng: ruta.destino.lon }
    //   ],
    //   geodesic: true,
    //   strokeColor: '#10b981',
    //   strokeOpacity: 0.7,
    //   strokeWeight: 2
    // }).setMap(mapa);
  });
  
  console.log(`✅ ${rutasProcesadas.length} rutas pintadas en el mapa`);
}
```

---

## 📊 EJEMPLO COMPLETO EN REACT

```javascript
import React, { useEffect, useState, useRef } from 'react';
import { planificacionService } from '../services/PlanificacionService';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const SimuladorConMapa = () => {
  const [conectado, setConectado] = useState(false);
  const [simulacionActiva, setSimulacionActiva] = useState(false);
  const [progreso, setProgreso] = useState(null);
  const [rutas, setRutas] = useState([]);
  const mapaRef = useRef(null);
  const marcadoresRef = useRef([]);

  // Inicializar mapa y servicio
  useEffect(() => {
    // Crear mapa Leaflet
    const mapa = L.map('mapa').setView([0, 0], 2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(mapa);
    mapaRef.current = mapa;

    // Configurar callbacks del servicio
    planificacionService
      .onConnected(() => {
        console.log('✅ WebSocket conectado');
        setConectado(true);
      })
      .onProgreso((data) => {
        console.log('📊 Progreso:', data.porcentaje.toFixed(1) + '%');
        setProgreso(data);
      })
      .onRutasProcesadas((rutasProcesadas) => {
        console.log('🗺️ Rutas recibidas:', rutasProcesadas.length);
        setRutas(rutasProcesadas);
        pintarRutas(rutasProcesadas);
      })
      .onCompletado(() => {
        console.log('🏁 Simulación completada');
        setSimulacionActiva(false);
      })
      .onError((error) => {
        console.error('❌ Error:', error);
        alert('Error: ' + error.message);
      });

    // Inicializar
    inicializar();

    // Cleanup
    return () => {
      planificacionService.disconnect();
      if (mapaRef.current) {
        mapaRef.current.remove();
      }
    };
  }, []);

  // Inicializar: cargar aeropuertos + conectar WebSocket
  const inicializar = async () => {
    try {
      await planificacionService.cargarAeropuertos();
      await planificacionService.connect();
    } catch (error) {
      console.error('❌ Error en inicialización:', error);
    }
  };

  // Iniciar simulación
  const handleIniciar = async () => {
    try {
      setSimulacionActiva(true);
      
      await planificacionService.iniciar({
        tiempoActualSimulacion: "2025-01-02T08:00:00",
        factorK: 5,
        tamanioPoblacion: 10,
        maxGeneraciones: 10,
        limiteGeneracionesSinMejora: 5
      });
      
    } catch (error) {
      console.error('❌ Error al iniciar:', error);
      setSimulacionActiva(false);
    }
  };

  // Pintar rutas en el mapa
  const pintarRutas = (rutasProcesadas) => {
    const mapa = mapaRef.current;
    if (!mapa) return;

    // Limpiar marcadores anteriores
    marcadoresRef.current.forEach(m => mapa.removeLayer(m));
    marcadoresRef.current = [];

    // Pintar nuevas rutas
    rutasProcesadas.forEach(ruta => {
      // Línea de ruta
      const linea = L.polyline([
        [ruta.origen.lat, ruta.origen.lon],
        [ruta.destino.lat, ruta.destino.lon]
      ], {
        color: '#10b981',
        weight: 2,
        opacity: 0.7
      }).addTo(mapa);
      
      marcadoresRef.current.push(linea);

      // Marcador origen
      const markerOrigen = L.marker([ruta.origen.lat, ruta.origen.lon])
        .bindPopup(`<b>${ruta.origen.ciudad}</b><br>${ruta.origen.codigo}`)
        .addTo(mapa);
      
      marcadoresRef.current.push(markerOrigen);
    });

    // Ajustar vista del mapa
    if (rutasProcesadas.length > 0) {
      const bounds = L.latLngBounds(
        rutasProcesadas.map(r => [r.origen.lat, r.origen.lon])
      );
      mapa.fitBounds(bounds);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      {/* Panel lateral */}
      <div style={{ width: '300px', padding: '20px', background: '#f3f4f6' }}>
        <h2>Simulador con Mapa</h2>
        
        <div style={{ marginBottom: '10px' }}>
          Estado: {conectado ? '🟢 Conectado' : '🔴 Desconectado'}
        </div>

        <button 
          onClick={handleIniciar}
          disabled={!conectado || simulacionActiva}
          style={{
            padding: '10px 20px',
            background: '#10b981',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            cursor: conectado && !simulacionActiva ? 'pointer' : 'not-allowed',
            width: '100%'
          }}
        >
          {simulacionActiva ? '⏳ Ejecutando...' : '▶️ Iniciar Simulación'}
        </button>

        {progreso && (
          <div style={{ marginTop: '20px' }}>
            <h3>Progreso</h3>
            <div>Generación: {progreso.generacionActual}/{progreso.totalGeneraciones}</div>
            <div>Progreso: {progreso.porcentaje?.toFixed(1)}%</div>
            <div>Fitness: {progreso.mejorFitness?.toFixed(2)}</div>
            <div style={{
              background: '#e5e7eb',
              height: '20px',
              borderRadius: '10px',
              overflow: 'hidden',
              marginTop: '10px'
            }}>
              <div style={{
                background: '#10b981',
                height: '100%',
                width: `${progreso.porcentaje}%`,
                transition: 'width 0.3s'
              }} />
            </div>
          </div>
        )}

        {rutas.length > 0 && (
          <div style={{ marginTop: '20px' }}>
            <h3>Rutas</h3>
            <div>Total: {rutas.length}</div>
          </div>
        )}
      </div>

      {/* Mapa */}
      <div id="mapa" style={{ flex: 1 }} />
    </div>
  );
};

export default SimuladorConMapa;
```

---

## 🎯 FORMATO DE DATOS DE RUTAS PROCESADAS

Cada ruta en el array `rutasProcesadas` tiene esta estructura:

```javascript
{
  pedidoId: 123,
  vueloId: "SPIM-SCIE-002",
  origen: {
    codigo: "SPIM",
    lat: -12.0219,
    lon: -77.1143,
    ciudad: "Lima",
    pais: "Peru",
    nombre: "Jorge Chávez International Airport"
  },
  destino: {
    codigo: "SCIE",
    lat: -33.3929,
    lon: -70.7858,
    ciudad: "Santiago",
    pais: "Chile",
    nombre: "Arturo Merino Benítez International Airport"
  },
  salida: "2025-01-02T05:30",
  llegada: "2025-01-02T07:15",
  duracionHoras: 1.75,
  distanciaKm: 850.5
}
```

---

## 🐛 DEBUGGING

### Ver rutas en la consola del navegador

Después de recibir rutas, puedes acceder a ellas con:

```javascript
window.ultimasRutas
```

### Verificar aeropuertos cargados

```javascript
planificacionService.aeropuertosEstanCargados() // true/false
planificacionService.getAeropuertosMap() // { "SPIM": {...}, "SCIE": {...} }
```

### Obtener coordenadas de un aeropuerto específico

```javascript
planificacionService.getCoordenadas("SPIM")
// { lat: -12.0219, lon: -77.1143, ciudad: "Lima", pais: "Peru" }
```

---

## ✅ Checklist de Integración

- [ ] Importar `planificacionService`
- [ ] Configurar callbacks (`onConnected`, `onProgreso`, `onRutasProcesadas`)
- [ ] Llamar `cargarAeropuertos()` al inicio
- [ ] Llamar `connect()` después de cargar aeropuertos
- [ ] Iniciar simulación con `iniciar(params)`
- [ ] Implementar función `pintarRutasEnMapa(rutasProcesadas)`
- [ ] Usar `ruta.origen.lat/lon` y `ruta.destino.lat/lon` para pintar
- [ ] Limpiar marcadores anteriores antes de pintar nuevos

---

## 🎨 Mejoras Opcionales

### Colores por estado de ruta
```javascript
const color = ruta.duracionHoras > 2 ? '#ef4444' : '#10b981';
```

### Animación de progreso
```javascript
let progress = 0;
setInterval(() => {
  progress += 0.01;
  if (progress > 1) progress = 0;
  // Actualizar posición del avión en la línea
}, 100);
```

### Tooltip con información
```javascript
L.marker([lat, lon]).bindTooltip(`
  Vuelo: ${ruta.vueloId}<br>
  Duración: ${ruta.duracionHoras} hrs<br>
  Distancia: ${ruta.distanciaKm} km
`, { permanent: false });
```

---

¡Listo! Ahora tu frontend tiene todo lo necesario para visualizar las rutas en el mapa 🗺️✨
