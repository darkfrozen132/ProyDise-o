# 🔄 Ciclo de Vida del Tracking de Vuelos SSE

## 📊 Flujo Completo del Vuelo

### 1️⃣ **Inicio del Vuelo**

```http
POST /api/simulacion/iniciar
```

**Backend:**
- ✅ Marca el vuelo como `enVuelo = true` en BD
- ✅ Comienza a enviar eventos SSE cada 1 segundo
- ✅ Actualiza coordenadas y progreso en BD

**Frontend recibe:**
```json
{
  "progress": 0.0,
  "currentLatitude": -12.0219,  // Lima
  "currentLongitude": -77.1143,
  "completed": false
}
```

---

### 2️⃣ **Durante el Vuelo (100 segundos)**

**Cada segundo el backend envía:**
```json
{
  "progress": 1.0,   // → 2.0 → 3.0 → ... → 99.0
  "currentLatitude": -10.35,  // Coordenadas interpoladas
  "currentLongitude": -75.45,
  "completed": false
}
```

**Frontend:**
- 🗺️ Actualiza la posición del avión en el mapa
- 📊 Actualiza la barra de progreso
- ✈️ Dibuja la trayectoria del vuelo

---

### 3️⃣ **Llegada al Destino (Segundo 100)**

**Backend envía evento final:**
```json
{
  "progress": 100.0,
  "currentLatitude": 52.4731,   // Berlín
  "currentLongitude": 13.4040,
  "completed": true,  // ⭐ INDICADOR DE COMPLETADO
  "message": "Vuelo completado - Llegada a EDDI"
}
```

**Backend:**
- ✅ Marca `enVuelo = false` en BD
- ✅ Guarda progreso = 100% en BD
- ⏳ Espera 2 segundos (para que cliente reciba el evento)
- 🔌 Cierra todas las conexiones SSE automáticamente

---

### 4️⃣ **Cierre de Conexión**

**Frontend detecta:**
```javascript
trackingSSE.onerror = () => {
  console.log('🔌 Conexión cerrada - Vuelo finalizado');
  trackingSSE.close();
};
```

---

## 🎯 Diagrama de Secuencia

```
Usuario          Frontend              Backend            Base de Datos
  |                 |                      |                     |
  |-- Click Iniciar-|                      |                     |
  |                 |                      |                     |
  |                 |-- POST /iniciar ---->|                     |
  |                 |                      |-- enVuelo=true ---->|
  |                 |                      |                     |
  |                 |<-- SSE Connect ------|                     |
  |                 |                      |                     |
  |                 |<-- progress: 0% -----|                     |
  |<- Mapa muestra -|                      |                     |
  |   avión en Lima |                      |                     |
  |                 |                      |                     |
  |                 |<-- progress: 1% -----|-- UPDATE coords --->|
  |<- Avión se mueve|                      |                     |
  |                 |                      |                     |
  |                 |        ... (100 segundos) ...              |
  |                 |                      |                     |
  |                 |<-- progress: 99% ----|                     |
  |                 |                      |                     |
  |                 |<-- progress: 100% ---|                     |
  |                 |    completed: true   |                     |
  |<- VUELO COMPLETO|    message: "..."    |                     |
  |   + Notificación|                      |                     |
  |                 |                      |-- enVuelo=false --->|
  |                 |                      |                     |
  |                 |                      |--(espera 2s)        |
  |                 |                      |                     |
  |                 |<-- SSE Close --------|                     |
  |<- Desconectado -|                      |                     |
  |                 |                      |                     |
```

---

## 🔍 Detectar Finalización en Frontend

### Opción 1: Detectar campo `completed`

```javascript
trackingSSE.addEventListener('vuelo-update', (event) => {
  const data = JSON.parse(event.data);
  
  if (data.completed === true) {
    console.log('🎉 VUELO COMPLETADO!');
    console.log('📍 Llegó a:', data.destinationCode);
    console.log('💬', data.message);
    
    // Mostrar notificación
    mostrarNotificacion('Vuelo Completado', data.message);
    
    // Cambiar icono del avión a ✅
    avionMarker.setIcon(iconoCompletado);
    
    // Detener animaciones
    detenerAnimaciones();
  }
});
```

---

### Opción 2: Detectar progreso = 100%

```javascript
trackingSSE.addEventListener('vuelo-update', (event) => {
  const data = JSON.parse(event.data);
  
  if (data.progress >= 100) {
    console.log('✅ Progreso completo');
    finalizarVisualizacion();
  }
  
  actualizarBarraProgreso(data.progress);
});
```

---

### Opción 3: Detectar cierre de conexión

```javascript
trackingSSE.onerror = (error) => {
  console.log('🔌 Conexión cerrada por el servidor');
  console.log('📝 Motivo: Vuelo finalizado automáticamente');
  
  // Limpiar recursos
  trackingSSE.close();
  
  // Mostrar mensaje final
  document.getElementById('estado').textContent = 'Vuelo Finalizado';
};
```

---

## 💡 Ejemplo Completo con Todas las Detecciones

```javascript
let trackingSSE = null;
let vueloCompletado = false;

function iniciarTracking() {
  trackingSSE = new EventSource('http://localhost:8080/api/vuelos/stream');
  
  // Manejar eventos de actualización
  trackingSSE.addEventListener('vuelo-update', (event) => {
    const data = JSON.parse(event.data);
    
    // Actualizar mapa
    actualizarPosicionAvion(data.currentLatitude, data.currentLongitude);
    
    // Actualizar progreso
    document.getElementById('progreso').textContent = data.progress.toFixed(1) + '%';
    
    // ⭐ DETECCIÓN 1: Campo completed
    if (data.completed === true && !vueloCompletado) {
      vueloCompletado = true;
      
      console.log('🎉 VUELO COMPLETADO!');
      console.log('📍 Destino alcanzado:', data.destinationCode);
      console.log('💬 Mensaje:', data.message);
      
      // Mostrar notificación
      Swal.fire({
        title: '✈️ Vuelo Completado!',
        text: data.message,
        icon: 'success',
        confirmButtonText: 'OK'
      });
      
      // Cambiar color del marcador
      avionMarker.setIcon(L.icon({
        iconUrl: 'avion-verde.png',
        iconSize: [32, 32]
      }));
      
      // Actualizar estado en UI
      document.getElementById('estado-vuelo').textContent = '✅ Completado';
      document.getElementById('estado-vuelo').className = 'completado';
    }
    
    // ⭐ DETECCIÓN 2: Progreso 100%
    if (data.progress >= 100 && !vueloCompletado) {
      console.log('📊 Progreso alcanzó el 100%');
    }
  });
  
  // ⭐ DETECCIÓN 3: Cierre de conexión
  trackingSSE.onerror = (error) => {
    console.log('🔌 Conexión SSE cerrada');
    
    if (vueloCompletado) {
      console.log('✅ Cierre esperado - Vuelo finalizado correctamente');
    } else {
      console.warn('⚠️ Cierre inesperado - Error en la conexión');
    }
    
    trackingSSE.close();
  };
  
  // Manejar evento cuando la conexión se abre
  trackingSSE.onopen = () => {
    console.log('🔌 Conexión SSE establecida');
    document.getElementById('estado-conexion').textContent = '🟢 Conectado';
  };
}

// Función para actualizar posición del avión
function actualizarPosicionAvion(lat, lng) {
  avionMarker.setLatLng([lat, lng]);
  map.panTo([lat, lng]);
  
  // Agregar punto a la trayectoria
  trayectoria.push([lat, lng]);
  lineaRuta.setLatLngs(trayectoria);
}
```

---

## 📝 Logs del Backend

```
🚀 Streaming de coordenadas iniciado
📍 Iniciando vuelo desde (-12.0219, -77.1143) hasta (52.4731, 13.404)

[Segundo 1]  Progreso: 1.0%   Lat: -11.38, Lng: -76.44
[Segundo 2]  Progreso: 2.0%   Lat: -10.74, Lng: -75.77
...
[Segundo 99] Progreso: 99.0%  Lat: 52.40, Lng: 13.37
[Segundo 100] Progreso: 100.0% Lat: 52.47, Lng: 13.40

✅ Vuelo completado - Enviando evento final
🔌 Cerrando 1 conexiones SSE...
✅ Todas las conexiones SSE cerradas
🔌 Todas las conexiones SSE cerradas - Vuelo finalizado
```

---

## ⚡ Resumen del Ciclo de Vida

| Fase | Duración | Estado BD | SSE | Frontend |
|------|----------|-----------|-----|----------|
| **Inicio** | 0s | `enVuelo=true` | Conectado | Avión en origen |
| **En vuelo** | 1-99s | Actualizando coords | Enviando eventos | Avión moviendose |
| **Llegada** | 100s | `enVuelo=false`, `progreso=100` | Evento final + `completed=true` | Notificación |
| **Cierre** | 102s | Finalizado | Conexión cerrada | Desconectado |

---

## 🎯 Beneficios del Sistema

1. ✅ **Indicador claro de finalización** (`completed: true`)
2. ✅ **Cierre automático de conexiones** (no quedan colgadas)
3. ✅ **Mensaje descriptivo** para el usuario
4. ✅ **Persistencia en BD** (puedes consultar el estado después)
5. ✅ **Múltiples formas de detección** (robustez)

¡Tu frontend ahora puede detectar perfectamente cuándo el vuelo termina! 🚀✈️
