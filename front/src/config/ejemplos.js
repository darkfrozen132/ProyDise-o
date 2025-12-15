// ==================== EJEMPLO DE USO EN COMPONENTES ====================
// Este archivo muestra cómo usar api.js y websocket.js en tus componentes React

import React, { useState, useEffect, useRef } from 'react';

// ========== OPCIÓN 1: Usando HTTP + SSE (EventSource) ==========
import { 
  iniciarSimulacion, 
  detenerSimulacion,
  conectarStreamSimulacion,
  getAirports 
} from '../config/api';

function SimuladorConSSE() {
  const [simulacionActiva, setSimulacionActiva] = useState(false);
  const [tickActual, setTickActual] = useState(0);
  const [rutasSolucion, setRutasSolucion] = useState([]);
  const eventSourceRef = useRef(null);

  // Iniciar simulación con SSE
  const handleIniciar = async () => {
    try {
      // 1. Llamar al endpoint de iniciar
      const response = await iniciarSimulacion();
      console.log('✅ Simulación iniciada:', response);

      // 2. Conectar al stream SSE
      eventSourceRef.current = conectarStreamSimulacion(
        (estado) => {
          // Actualizar estado con los datos del SSE
          setTickActual(estado.tickActual);
          setRutasSolucion(estado.rutasSolucion || []);
          setSimulacionActiva(estado.activa);
        },
        (error) => {
          console.error('❌ Error en SSE:', error);
        }
      );

      setSimulacionActiva(true);
    } catch (error) {
      console.error('Error al iniciar:', error);
    }
  };

  // Detener simulación
  const handleDetener = async () => {
    try {
      // 1. Cerrar el stream SSE
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }

      // 2. Llamar al endpoint de detener
      await detenerSimulacion();
      setSimulacionActiva(false);
      console.log('✅ Simulación detenida');
    } catch (error) {
      console.error('Error al detener:', error);
    }
  };

  // Limpiar al desmontar
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  return (
    <div>
      <h2>Simulación con SSE</h2>
      <p>Tick actual: {tickActual}</p>
      <p>Rutas: {rutasSolucion.length}</p>
      <button onClick={handleIniciar} disabled={simulacionActiva}>
        Iniciar
      </button>
      <button onClick={handleDetener} disabled={!simulacionActiva}>
        Detener
      </button>
    </div>
  );
}


// ========== OPCIÓN 2: Usando WebSocket (Bidireccional) ==========
import { conectarWebSocket } from '../config/websocket';

function SimuladorConWebSocket() {
  const [isConnected, setIsConnected] = useState(false);
  const [tickActual, setTickActual] = useState(0);
  const [rutasSolucion, setRutasSolucion] = useState([]);
  const wsRef = useRef(null);

  useEffect(() => {
    // Conectar al WebSocket al montar el componente
    wsRef.current = conectarWebSocket(
      (data) => {
        // Manejar datos recibidos del servidor
        console.log('📡 Datos WebSocket:', data);
        
        if (data.tickActual !== undefined) {
          setTickActual(data.tickActual);
        }
        if (data.rutasSolucion) {
          setRutasSolucion(data.rutasSolucion);
        }
      },
      (error) => {
        console.error('❌ Error WebSocket:', error);
      },
      () => {
        // Cuando se conecta
        console.log('✅ WebSocket conectado');
        setIsConnected(true);
      },
      () => {
        // Cuando se desconecta
        console.log('❌ WebSocket desconectado');
        setIsConnected(false);
      }
    );

    // Limpiar al desmontar
    return () => {
      if (wsRef.current) {
        wsRef.current.cerrar();
      }
    };
  }, []);

  // Enviar comando de iniciar
  const handleIniciar = () => {
    if (wsRef.current && wsRef.current.estaConectado()) {
      wsRef.current.enviar({ comando: 'iniciar' });
    }
  };

  // Enviar comando de pausar
  const handlePausar = () => {
    if (wsRef.current && wsRef.current.estaConectado()) {
      wsRef.current.enviar({ comando: 'pausar' });
    }
  };

  // Enviar comando de detener
  const handleDetener = () => {
    if (wsRef.current && wsRef.current.estaConectado()) {
      wsRef.current.enviar({ comando: 'detener' });
    }
  };

  return (
    <div>
      <h2>Simulación con WebSocket</h2>
      <p>Estado: {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}</p>
      <p>Tick actual: {tickActual}</p>
      <p>Rutas: {rutasSolucion.length}</p>
      <button onClick={handleIniciar} disabled={!isConnected}>
        Iniciar
      </button>
      <button onClick={handlePausar} disabled={!isConnected}>
        Pausar
      </button>
      <button onClick={handleDetener} disabled={!isConnected}>
        Detener
      </button>
    </div>
  );
}


// ========== OPCIÓN 3: Mixto (HTTP para control + WebSocket para datos) ==========
function SimuladorMixto() {
  const [isConnected, setIsConnected] = useState(false);
  const [simulacionActiva, setSimulacionActiva] = useState(false);
  const [tickActual, setTickActual] = useState(0);
  const [airports, setAirports] = useState([]);
  const wsRef = useRef(null);

  // Cargar aeropuertos al montar (HTTP)
  useEffect(() => {
    const loadAirports = async () => {
      try {
        const data = await getAirports();
        setAirports(data);
        console.log('✅ Aeropuertos cargados:', data.length);
      } catch (error) {
        console.error('Error cargando aeropuertos:', error);
      }
    };
    
    loadAirports();
  }, []);

  // Conectar WebSocket al montar
  useEffect(() => {
    wsRef.current = conectarWebSocket(
      (data) => {
        setTickActual(data.tickActual || 0);
        setSimulacionActiva(data.activa || false);
      },
      (error) => console.error(error),
      () => setIsConnected(true),
      () => setIsConnected(false)
    );

    return () => {
      if (wsRef.current) {
        wsRef.current.cerrar();
      }
    };
  }, []);

  // Usar HTTP para iniciar la simulación
  const handleIniciar = async () => {
    try {
      await iniciarSimulacion();
      setSimulacionActiva(true);
      // El WebSocket recibirá las actualizaciones automáticamente
    } catch (error) {
      console.error('Error:', error);
    }
  };

  // Usar HTTP para detener
  const handleDetener = async () => {
    try {
      await detenerSimulacion();
      setSimulacionActiva(false);
    } catch (error) {
      console.error('Error:', error);
    }
  };

  return (
    <div>
      <h2>Simulación Mixta (HTTP + WS)</h2>
      <p>WebSocket: {isConnected ? '🟢' : '🔴'}</p>
      <p>Aeropuertos: {airports.length}</p>
      <p>Tick: {tickActual}</p>
      <button onClick={handleIniciar} disabled={!isConnected || simulacionActiva}>
        Iniciar
      </button>
      <button onClick={handleDetener} disabled={!simulacionActiva}>
        Detener
      </button>
    </div>
  );
}

export { SimuladorConSSE, SimuladorConWebSocket, SimuladorMixto };
