# 🚀 GUÍA ESPECÍFICA: WebSocket para Planificación con Algoritmo Genético

## 📋 **Tu Backend Real**

### Contrato Exacto:

```
REST API:
- POST /api/simulations/start
  Body: { fecha, factorK, tamanioPoblacion, maxGeneraciones, limiteGeneracionesSinMejora }
  Response: { sessionId: "UUID" }

- POST /api/simulations/{sessionId}/cancel
  Response: OK

WebSocket:
- Endpoint: /ws (SockJS + STOMP)
- Topic: /topic/simulations/{sessionId}
- Mensaje (ProgresoAGDTO):
  {
    status: 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'PAUSED',
    generacionActual: number,
    totalGeneraciones: number,
    mejorFitness: number,
    porcentaje: number,
    tiempoTranscurridoMs: number,
    etaMs: number,
    totalPedidos: number,
    solucion: PlanificacionResponseSimple
  }
```

---

## 🎯 **Flujo Minimalista (2 Botones)**

```
┌──────────────────────────────────────────────────────────┐
│              PANTALLA DE SIMULACIÓN                      │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Estado: 🟢 Conectado / 🔴 Desconectado                │
│                                                          │
│  ┌────────────────────┐    ┌────────────────────┐      │
│  │  INICIAR           │    │  DETENER/CANCELAR  │      │
│  │  SIMULACIÓN        │    │  (Deshabilitado)   │      │
│  └────────────────────┘    └────────────────────┘      │
│                                                          │
│  ╔════════════════════════════════════════════════╗     │
│  ║  Progreso: ████████████░░░░░░░░░ 65%          ║     │
│  ║  Generación: 13 / 20                          ║     │
│  ║  Fitness: 1234.56                             ║     │
│  ║  Tiempo: 5.2s | ETA: 3.1s                     ║     │
│  ╚════════════════════════════════════════════════╝     │
│                                                          │
│  🗺️ MAPA CON RUTAS (actualización en tiempo real)     │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 📊 **Estados y Botones**

| Estado Actual | Botón "Iniciar" | Botón "Detener" | Acción en Background |
|---------------|-----------------|-----------------|----------------------|
| **Desconectado** | ❌ Disabled | ❌ Disabled | Conectando WebSocket |
| **Conectado (IDLE)** | ✅ Enabled | ❌ Disabled | Esperando inicio |
| **Simulación RUNNING** | ❌ Disabled | ✅ Enabled | Recibiendo progreso |
| **Simulación COMPLETED** | ✅ Enabled | ❌ Disabled | Mostrando resultado final |
| **Simulación CANCELLED** | ✅ Enabled | ❌ Disabled | Reseteado |

---

## 🔄 **Flujo Paso a Paso**

### **Paso 1: Montar Componente**
```javascript
useEffect(() => {
  // Conectar WebSocket al cargar
  planificacionService.connect();
  
  // Cleanup al desmontar
  return () => {
    planificacionService.disconnect();
  };
}, []);
```

### **Paso 2: Usuario Click "Iniciar Simulación"**
```javascript
const handleIniciar = async () => {
  setEstado('INICIANDO');
  setBotonIniciarDisabled(true);
  
  try {
    const sessionId = await planificacionService.startSimulation({
      fecha: '2025-01-15',
      factorK: 5,
      tamanioPoblacion: 20,
      maxGeneraciones: 20,
      limiteGeneracionesSinMejora: 10
    });
    
    // Automáticamente se suscribe a /topic/simulations/{sessionId}
    setSessionId(sessionId);
    setEstado('RUNNING');
    setBotonDetenerDisabled(false);
    
  } catch (error) {
    alert('Error al iniciar: ' + error.message);
    setEstado('ERROR');
    setBotonIniciarDisabled(false);
  }
};
```

### **Paso 3: Recibir Progreso en Tiempo Real**
```javascript
planificacionService.onProgress((progreso) => {
  // Actualizar UI con cada mensaje
  setGeneracionActual(progreso.generacionActual);
  setTotalGeneraciones(progreso.totalGeneraciones);
  setFitness(progreso.mejorFitness);
  setPorcentaje(progreso.porcentaje);
  setTiempoTranscurrido(progreso.tiempoTranscurridoMs);
  setETA(progreso.etaMs);
  
  // Actualizar mapa con la solución actual
  if (progreso.solucion && progreso.solucion.vuelos) {
    setVuelos(progreso.solucion.vuelos);
  }
  
  // Detectar finalización
  if (progreso.status === 'COMPLETED') {
    setEstado('COMPLETED');
    setBotonIniciarDisabled(false);
    setBotonDetenerDisabled(true);
    alert('✅ Simulación completada!');
  }
  
  if (progreso.status === 'CANCELLED') {
    setEstado('CANCELLED');
    setBotonIniciarDisabled(false);
    setBotonDetenerDisabled(true);
  }
});
```

### **Paso 4: Usuario Click "Detener/Cancelar"**
```javascript
const handleDetener = async () => {
  try {
    await planificacionService.cancelSimulation();
    // El backend enviará un mensaje con status: 'CANCELLED'
    // El callback onProgress lo manejará
  } catch (error) {
    alert('Error al cancelar: ' + error.message);
  }
};
```

---

## 💻 **Código Completo del Componente**

```javascript
import React, { useState, useEffect } from 'react';
import { planificacionService } from '../services/PlanificacionService';

const SimuladorMinimalista = () => {
  // Estado
  const [conectado, setConectado] = useState(false);
  const [estado, setEstado] = useState('IDLE'); // IDLE, RUNNING, COMPLETED, CANCELLED
  const [sessionId, setSessionId] = useState(null);
  
  // Progreso
  const [generacionActual, setGeneracionActual] = useState(0);
  const [totalGeneraciones, setTotalGeneraciones] = useState(0);
  const [fitness, setFitness] = useState(0);
  const [porcentaje, setPorcentaje] = useState(0);
  const [tiempoTranscurrido, setTiempoTranscurrido] = useState(0);
  const [eta, setETA] = useState(0);
  
  // Datos
  const [vuelos, setVuelos] = useState([]);
  
  // Parámetros
  const [fecha, setFecha] = useState('2025-01-15');
  const [factorK, setFactorK] = useState(5);
  
  // Callbacks
  useEffect(() => {
    // Conectar
    planificacionService
      .onConnected(() => {
        console.log('✅ Conectado');
        setConectado(true);
      })
      .onDisconnected(() => {
        console.log('🔌 Desconectado');
        setConectado(false);
      })
      .onProgress((progreso) => {
        console.log('📨 Progreso:', progreso);
        
        setGeneracionActual(progreso.generacionActual);
        setTotalGeneraciones(progreso.totalGeneraciones);
        setFitness(progreso.mejorFitness);
        setPorcentaje(progreso.porcentaje);
        setTiempoTranscurrido(progreso.tiempoTranscurridoMs);
        setETA(progreso.etaMs);
        
        if (progreso.solucion?.vuelos) {
          setVuelos(progreso.solucion.vuelos);
        }
        
        if (progreso.status === 'COMPLETED') {
          setEstado('COMPLETED');
          alert('✅ Simulación completada!');
        }
        
        if (progreso.status === 'CANCELLED') {
          setEstado('CANCELLED');
        }
      })
      .onError((error) => {
        console.error('❌ Error:', error);
        alert('Error: ' + error.message);
      })
      .connect();
    
    // Cleanup
    return () => {
      planificacionService.disconnect();
    };
  }, []);
  
  // Handlers
  const handleIniciar = async () => {
    setEstado('INICIANDO');
    
    try {
      const id = await planificacionService.startSimulation({
        fecha,
        factorK,
        tamanioPoblacion: 20,
        maxGeneraciones: 20,
        limiteGeneracionesSinMejora: 10
      });
      
      setSessionId(id);
      setEstado('RUNNING');
      
    } catch (error) {
      alert('Error al iniciar: ' + error.message);
      setEstado('IDLE');
    }
  };
  
  const handleDetener = async () => {
    try {
      await planificacionService.cancelSimulation();
    } catch (error) {
      alert('Error al cancelar: ' + error.message);
    }
  };
  
  // Determinar si botones están habilitados
  const botonIniciarEnabled = conectado && (estado === 'IDLE' || estado === 'COMPLETED' || estado === 'CANCELLED');
  const botonDetenerEnabled = estado === 'RUNNING';
  
  return (
    <div className="simulador-minimalista">
      <h1>🎮 Simulador de Planificación</h1>
      
      {/* Estado de Conexión */}
      <div className="estado-conexion">
        {conectado ? '🟢 Conectado' : '🔴 Desconectado'}
        {sessionId && <span> | Session: {sessionId}</span>}
      </div>
      
      {/* Parámetros */}
      <div className="parametros">
        <label>
          Fecha:
          <input
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            disabled={estado === 'RUNNING'}
          />
        </label>
        
        <label>
          Factor K:
          <input
            type="number"
            value={factorK}
            onChange={(e) => setFactorK(Number(e.target.value))}
            disabled={estado === 'RUNNING'}
            min="1"
            max="10"
          />
        </label>
      </div>
      
      {/* Botones de Control */}
      <div className="controles">
        <button
          onClick={handleIniciar}
          disabled={!botonIniciarEnabled}
          className="btn-iniciar"
        >
          🚀 INICIAR SIMULACIÓN
        </button>
        
        <button
          onClick={handleDetener}
          disabled={!botonDetenerEnabled}
          className="btn-detener"
        >
          ⏹️ DETENER/CANCELAR
        </button>
      </div>
      
      {/* Barra de Progreso */}
      {estado === 'RUNNING' && (
        <div className="progreso">
          <div className="progreso-header">
            <span>Generación: {generacionActual} / {totalGeneraciones}</span>
            <span>{porcentaje.toFixed(1)}%</span>
          </div>
          
          <div className="progress-bar-container">
            <div
              className="progress-bar"
              style={{ width: `${porcentaje}%` }}
            />
          </div>
          
          <div className="progreso-stats">
            <div>Fitness: {fitness.toFixed(2)}</div>
            <div>Tiempo: {(tiempoTranscurrido / 1000).toFixed(1)}s</div>
            <div>ETA: {(eta / 1000).toFixed(1)}s</div>
          </div>
        </div>
      )}
      
      {/* Resultado */}
      {estado === 'COMPLETED' && (
        <div className="resultado">
          <h2>✅ Simulación Completada</h2>
          <p>Se generaron {vuelos.length} vuelos</p>
          <p>Fitness final: {fitness.toFixed(2)}</p>
        </div>
      )}
      
      {/* Mapa (placeholder) */}
      <div className="mapa">
        <h3>🗺️ Mapa de Rutas</h3>
        <p>{vuelos.length} vuelos visualizados</p>
        {/* Aquí va tu componente de mapa */}
      </div>
    </div>
  );
};

export default SimuladorMinimalista;
```

---

## 🎨 **Estilos CSS**

```css
.simulador-minimalista {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.estado-conexion {
  background: #f8f9fa;
  padding: 10px 20px;
  border-radius: 5px;
  margin-bottom: 20px;
  font-weight: bold;
}

.parametros {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.parametros label {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.parametros input {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 5px;
}

.controles {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
}

.btn-iniciar, .btn-detener {
  padding: 15px 30px;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-iniciar {
  background: #28a745;
  color: white;
}

.btn-iniciar:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-detener {
  background: #dc3545;
  color: white;
}

.btn-detener:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.progreso {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.progreso-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-weight: bold;
}

.progress-bar-container {
  width: 100%;
  height: 30px;
  background: #e9ecef;
  border-radius: 15px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #007bff, #0056b3);
  transition: width 0.3s;
}

.progreso-stats {
  display: flex;
  justify-content: space-around;
  font-size: 14px;
  color: #666;
}

.resultado {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.mapa {
  background: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  min-height: 400px;
}
```

---

## ✅ **Checklist de Implementación**

- [ ] `PlanificacionService.js` actualizado con endpoints correctos
- [ ] Componente `SimuladorMinimalista.js` creado
- [ ] Estilos CSS aplicados
- [ ] Backend corriendo en `http://localhost:8000`
- [ ] Endpoint `/ws` habilitado con CORS
- [ ] Endpoint `POST /api/simulations/start` funcionando
- [ ] Endpoint `POST /api/simulations/{sessionId}/cancel` funcionando
- [ ] Topic `/topic/simulations/{sessionId}` enviando `ProgresoAGDTO`

---

## 🐛 **Troubleshooting**

### Problema: No se conecta al WebSocket
**Solución**: Verifica CORS en el backend:
```java
registry.addEndpoint("/ws")
    .setAllowedOrigins("http://localhost:3000")
    .withSockJS();
```

### Problema: No llega el sessionId
**Solución**: Verifica que el backend retorne:
```json
{
  "sessionId": "UUID-aquí"
}
```

### Problema: No se reciben mensajes de progreso
**Solución**: Verifica que el backend envíe a:
```java
messagingTemplate.convertAndSend(
    "/topic/simulations/" + sessionId,
    progresoDTO
);
```

---

**🎉 ¡Listo para producción!**
