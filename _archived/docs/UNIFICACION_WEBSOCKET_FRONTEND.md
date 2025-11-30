# 🔄 Unificación de WebSocket - Frontend React vs HTML Simple

## 📊 Análisis Comparativo

### ✅ visualizador-ag.html (FUNCIONA)
```javascript
// 1. Conexión simple
const socket = new SockJS('http://localhost:8000/ws');
stompClient = Stomp.over(socket);
stompClient.debug = null;

// 2. Conectar
stompClient.connect({}, onSuccess, onError);

// 3. Iniciar simulación (REST)
fetch('http://localhost:8000/api/simulations/start', {
  method: 'POST',
  body: JSON.stringify({ fecha, factorK, ... })
})

// 4. Suscribirse al topic
const topic = '/topic/simulations/' + sessionId;
stompClient.subscribe(topic, (mensaje) => {
  const data = JSON.parse(mensaje.body);
  if (data.tipo === "PROGRESO_AG") {
    actualizarVisualizador(data);
  }
});
```

### ❌ SimuladorSemanal.js (COMPLEJO - Tiene bugs)
```javascript
// PROBLEMA 1: Tiene 2 sistemas diferentes de WebSocket
// - conectarWebSocketPlanificacion() → Obsoleto
// - Client STOMP → Correcto pero mal implementado

// PROBLEMA 2: Lógica duplicada y confusa
// - wsPlanificacionRef (obsoleto)
// - stompClientRef (correcto)

// PROBLEMA 3: Estado fragmentado
// - wsConectado (planificación)
// - wsStompConectado (simulación)
```

## 🎯 Solución: Unificar con la Lógica del HTML Simple

### Cambios Necesarios:

1. **Eliminar** `conectarWebSocketPlanificacion` (obsoleto)
2. **Usar solo** `Client` de `@stomp/stompjs`
3. **Unificar** la conexión y suscripción

---

## 🔧 IMPLEMENTACIÓN: Crear Servicio Unificado

### Paso 1: Crear `SimulacionStompService.js`

Este servicio replica EXACTAMENTE la lógica del visualizador HTML:

```javascript
// front/src/services/SimulacionStompService.js

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class SimulacionStompService {
  constructor() {
    this.stompClient = null;
    this.currentSessionId = null;
    this.subscription = null;
    
    // Callbacks
    this.onConnect = null;
    this.onDisconnect = null;
    this.onMessage = null;
    this.onError = null;
  }

  /**
   * Conectar a WebSocket STOMP
   */
  async connect() {
    if (this.stompClient?.active) {
      console.log('⚠️ Ya está conectado');
      return;
    }

    return new Promise((resolve, reject) => {
      try {
        const socket = new SockJS('http://localhost:8000/ws');
        this.stompClient = new Client({
          webSocketFactory: () => socket,
          debug: (str) => {
            // Solo mostrar errores importantes
            if (str.includes('ERROR') || str.includes('CONNECTED')) {
              console.log('🔌 STOMP:', str);
            }
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          
          onConnect: () => {
            console.log('✅ WebSocket STOMP conectado');
            if (this.onConnect) this.onConnect();
            resolve();
          },
          
          onStompError: (frame) => {
            console.error('❌ Error STOMP:', frame.headers['message']);
            if (this.onError) this.onError(frame);
            reject(new Error(frame.headers['message']));
          },
          
          onWebSocketError: (error) => {
            console.error('❌ Error WebSocket:', error);
            if (this.onError) this.onError(error);
            reject(error);
          },
          
          onDisconnect: () => {
            console.log('🔌 WebSocket desconectado');
            if (this.onDisconnect) this.onDisconnect();
          }
        });

        this.stompClient.activate();
        
      } catch (error) {
        console.error('❌ Error al conectar:', error);
        reject(error);
      }
    });
  }

  /**
   * Iniciar simulación (REST + STOMP)
   * EXACTAMENTE como visualizador-ag.html
   */
  async iniciarSimulacion(params) {
    if (!this.stompClient?.active) {
      throw new Error('WebSocket no conectado');
    }

    try {
      console.log('🚀 Iniciando simulación:', params);
      
      // 1. Llamar REST API
      const response = await fetch('http://localhost:8000/api/simulations/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fecha: params.fecha,
          factorK: params.factorK || 5,
          tamanioPoblacion: params.tamanioPoblacion || 10,
          maxGeneraciones: params.maxGeneraciones || 10,
          limiteGeneracionesSinMejora: params.limiteGeneracionesSinMejora || 5
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      this.currentSessionId = data.sessionId;
      
      console.log('✅ Simulación iniciada con ID:', this.currentSessionId);

      // 2. Suscribirse al topic STOMP
      await this.suscribirseASimulacion(this.currentSessionId);
      
      return this.currentSessionId;
      
    } catch (error) {
      console.error('❌ Error al iniciar simulación:', error);
      throw error;
    }
  }

  /**
   * Suscribirse a los mensajes de la simulación
   */
  async suscribirseASimulacion(sessionId) {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    const topic = `/topic/simulations/${sessionId}`;
    console.log('📡 Suscribiéndose a:', topic);

    this.subscription = this.stompClient.subscribe(topic, (mensaje) => {
      try {
        const data = JSON.parse(mensaje.body);
        console.log('📥 Mensaje recibido:', data);
        
        // Procesar mensaje
        if (data.tipo === 'PROGRESO_AG') {
          console.log(`📊 Progreso AG - Gen ${data.generacion}/${data.maxGeneraciones}`);
          if (this.onMessage) this.onMessage(data);
        } 
        else if (data.status === 'COMPLETED') {
          console.log('🎉 Simulación completada');
          if (this.onMessage) this.onMessage(data);
        } 
        else if (data.status === 'CANCELLED') {
          console.log('🛑 Simulación cancelada');
          if (this.onMessage) this.onMessage(data);
        } 
        else if (data.status === 'ERROR') {
          console.error('❌ Error en simulación:', data.message);
          if (this.onMessage) this.onMessage(data);
        }
        
      } catch (error) {
        console.error('❌ Error al parsear mensaje:', error);
      }
    });
  }

  /**
   * Detener simulación actual
   */
  async detener() {
    if (!this.currentSessionId) {
      console.warn('⚠️ No hay simulación activa');
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8000/api/simulations/${this.currentSessionId}/cancel`,
        { method: 'POST' }
      );
      
      if (response.ok) {
        console.log('✅ Simulación detenida');
      }
    } catch (error) {
      console.error('❌ Error al detener:', error);
    }
  }

  /**
   * Desconectar WebSocket
   */
  disconnect() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }

    this.currentSessionId = null;
    console.log('🔌 WebSocket desconectado completamente');
  }

  /**
   * Verificar si está conectado
   */
  isConnected() {
    return this.stompClient?.active || false;
  }
}

export default new SimulacionStompService();
```

---

## 📝 Paso 2: Actualizar SimuladorSemanal.js

### Cambios Principales:

```javascript
// ANTES (Complejo - 2 sistemas)
import { conectarWebSocketPlanificacion } from '../../../config/websocket';
const wsPlanificacionRef = useRef(null);
const stompClientRef = useRef(null);

// DESPUÉS (Simple - 1 sistema)
import SimulacionStompService from '../../../services/SimulacionStompService';
```

### Implementación Simplificada:

```javascript
const SimuladorSemanal = () => {
  // Estado simple
  const [wsConectado, setWsConectado] = useState(false);
  const [simulacionActiva, setSimulacionActiva] = useState(false);
  const [progresoAG, setProgresoAG] = useState(null);
  const [vuelos, setVuelos] = useState([]);

  // Configurar callbacks del servicio
  useEffect(() => {
    SimulacionStompService.onConnect = () => {
      console.log('✅ Conectado');
      setWsConectado(true);
    };

    SimulacionStompService.onDisconnect = () => {
      console.log('🔌 Desconectado');
      setWsConectado(false);
    };

    SimulacionStompService.onMessage = (data) => {
      console.log('📩 Mensaje:', data);
      
      if (data.tipo === 'PROGRESO_AG') {
        setProgresoAG(data);
        
        // Procesar vuelos
        if (data.solucion?.vuelos) {
          const vuelosConvertidos = data.solucion.vuelos.map(convertirVuelo);
          setVuelos(prev => [...prev, ...vuelosConvertidos]);
        }
      }
      else if (data.status === 'COMPLETED') {
        setSimulacionActiva(false);
        alert('✅ Simulación completada');
      }
    };

    SimulacionStompService.onError = (error) => {
      console.error('❌ Error:', error);
      alert(`Error: ${error.message}`);
    };

    // Conectar automáticamente
    SimulacionStompService.connect()
      .then(() => console.log('✅ Servicio iniciado'))
      .catch(err => console.error('❌ Error al iniciar:', err));

    // Cleanup
    return () => {
      SimulacionStompService.disconnect();
    };
  }, []);

  // Iniciar simulación
  const handleIniciarSimulacion = async () => {
    if (!fechaInicioSimulacion) {
      alert('Selecciona una fecha');
      return;
    }

    try {
      setVuelos([]); // Limpiar vuelos anteriores
      setSimulacionActiva(true);
      
      await SimulacionStompService.iniciarSimulacion({
        fecha: fechaInicioSimulacion,
        factorK: 5,
        tamanioPoblacion: 10,
        maxGeneraciones: 10,
        limiteGeneracionesSinMejora: 5
      });
      
      console.log('✅ Simulación iniciada');
      
    } catch (error) {
      console.error('❌ Error:', error);
      setSimulacionActiva(false);
      alert(`Error: ${error.message}`);
    }
  };

  // Detener simulación
  const handleDetenerSimulacion = () => {
    SimulacionStompService.detener();
    setSimulacionActiva(false);
  };

  // ... resto del componente
};
```

---

## ✅ Ventajas de esta Unificación

1. **Simplicidad**: 1 solo sistema de WebSocket
2. **Consistencia**: Misma lógica que visualizador HTML (funciona)
3. **Mantenibilidad**: Servicio reutilizable
4. **Claridad**: Código más legible

---

## 🚀 Pasos para Implementar

1. Crear `front/src/services/SimulacionStompService.js`
2. Actualizar `SimuladorSemanal.js` para usar el servicio
3. Eliminar código obsoleto:
   - `conectarWebSocketPlanificacion`
   - `wsPlanificacionRef`
   - Lógica duplicada

4. Recargar navegador con `Ctrl + Shift + R`

---

**¿Quieres que implemente estos cambios en el código?** 🚀
