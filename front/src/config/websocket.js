// ==================== CONFIGURACIÓN WEBSOCKET ====================
import { useState, useEffect } from 'react';

// URL base del WebSocket (simulación antigua)
const WS_BASE_URL = process.env.REACT_APP_API_URL 
  ? process.env.REACT_APP_API_URL.replace('http://', 'ws://').replace('https://', 'wss://')
  : 'ws://127.0.0.1:8000';

// URL del WebSocket de planificación (NUEVA - según websocket.md)
const WS_PLANIFICACION_URL = 'ws://localhost:8000/ws/planificacion';

/**
 * Conectar al WebSocket de la simulación   
 * @param {Function} onMessage - Callback cuando llegan datos: (data) => {}
 * @param {Function} onError - Callback cuando hay error: (error) => {}
 * @param {Function} onOpen - Callback cuando se conecta: () => {}
 * @param {Function} onClose - Callback cuando se desconecta: () => {}
 * @returns {WebSocket} Instancia de WebSocket con método enviar()
 */
export const conectarWebSocket = (onMessage, onError, onOpen, onClose) => {
  const ws = new WebSocket(`${WS_BASE_URL}/api/websocket/conectar`);
  
  ws.onopen = () => {
    console.log('🔌 WebSocket conectado a:', `${WS_BASE_URL}/api/websocket/conectar`);
    if (onOpen) onOpen();
  };
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('📡 WebSocket - Datos recibidos:', data);
      onMessage(data);
    } catch (error) {
      console.error('❌ Error al parsear mensaje WebSocket:', error);
      // Si no es JSON, pasar el dato raw
      onMessage(event.data);
    }
  };
  
  ws.onerror = (error) => {
    console.error('❌ Error en WebSocket:', error);
    if (onError) onError(error);
  };
  
  ws.onclose = (event) => {
    console.log('🔌 WebSocket desconectado. Código:', event.code, 'Razón:', event.reason);
    if (onClose) onClose(event);
  };
  
  /**
   * Método personalizado para enviar mensajes
   * @param {Object|string} mensaje - Mensaje a enviar (se convierte a JSON automáticamente)
   */
  ws.enviar = (mensaje) => {
    if (ws.readyState === WebSocket.OPEN) {
      const data = typeof mensaje === 'string' ? mensaje : JSON.stringify(mensaje);
      ws.send(data);
      console.log('📤 WebSocket - Mensaje enviado:', mensaje);
    } else {
      console.warn('⚠️ WebSocket no está conectado. Estado:', ws.readyState);
    }
  };

  /**
   * Método para verificar si está conectado
   */
  ws.estaConectado = () => {
    return ws.readyState === WebSocket.OPEN;
  };

  /**
   * Método para cerrar la conexión
   */
  ws.cerrar = () => {
    if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
      ws.close();
      console.log('🔌 Cerrando conexión WebSocket...');
    }
  };
  
  return ws;
};

/**
 * Hook de React para usar WebSocket (opcional)
 * Uso: const { ws, isConnected } = useWebSocket(handleMessage, handleError);
 */
export const useWebSocket = (onMessage, onError) => {
  const [ws, setWs] = useState(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const websocket = conectarWebSocket(
      onMessage,
      onError,
      () => setIsConnected(true),
      () => setIsConnected(false)
    );

    setWs(websocket);

    return () => {
      if (websocket && websocket.readyState === WebSocket.OPEN) {
        websocket.close();
      }
    };
  }, [onMessage, onError]);

  return { ws, isConnected };
};

// ==================== WEBSOCKET DE PLANIFICACIÓN ====================

/**
 * Conectar al WebSocket de Planificación en Tiempo Real
 * Según especificación en websocket.md
 * @param {Function} onMessage - Callback cuando llegan datos: (data) => {}
 * @param {Function} onError - Callback cuando hay error: (error) => {}
 * @param {Function} onOpen - Callback cuando se conecta: () => {}
 * @param {Function} onClose - Callback cuando se desconecta: () => {}
 * @returns {WebSocket} Instancia con métodos: enviar(), iniciarPlanificacion(), cerrar()
 */
export const conectarWebSocketPlanificacion = (onMessage, onError, onOpen, onClose) => {
  console.log('🔄 Intentando conectar a:', WS_PLANIFICACION_URL);
  
  let ws;
  try {
    ws = new WebSocket(WS_PLANIFICACION_URL);
  } catch (error) {
    console.error('❌ Error al crear WebSocket:', error);
    if (onError) onError(error);
    return null;
  }
  
  ws.onopen = () => {
    console.log('🔌 WebSocket Planificación conectado:', WS_PLANIFICACION_URL);
    if (onOpen) onOpen();
  };
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('📩 Planificación - Mensaje recibido:', data);
      
      // Clasificar mensaje por tipo
      switch(data.tipo) {
        case 'conexion':
          console.log('✅ Conexión establecida:', data.mensaje);
          break;
        case 'progreso':
          console.log(`📊 Iteración #${data.datos?.ejecucionNumero || data.ejecucionNumero}: ${data.datos?.duracionRealMs || data.duracionRealMs}ms`);
          break;
        case 'completado':
          console.log('🎉 Planificación completada:', data.solucion?.totalVuelos, 'vuelos');
          break;
        case 'error':
          console.error('❌ Error en planificación:', data.mensaje);
          break;
        default:
          console.log('📨 Mensaje tipo:', data.tipo);
      }
      
      onMessage(data);
    } catch (error) {
      console.error('❌ Error al parsear mensaje:', error);
      onMessage(event.data);
    }
  };
  
  ws.onerror = (error) => {
    console.error('❌ Error en WebSocket Planificación:', error);
    console.error('⚠️ Verifica que el servidor esté corriendo en:', WS_PLANIFICACION_URL);
    if (onError) onError(error);
  };
  
  ws.onclose = (event) => {
    if (event.wasClean) {
      console.log('🔌 WebSocket Planificación cerrado limpiamente. Código:', event.code);
    } else {
      console.warn('⚠️ WebSocket Planificación cerrado inesperadamente. Código:', event.code, 'Razón:', event.reason);
    }
    if (onClose) onClose(event);
  };
  
  /**
   * Iniciar planificación con parámetros
   * @param {string} fecha - Fecha inicial (YYYY-MM-DD)
   * @param {number} factorK - Factor de amplificación temporal (recomendado: 14)
   * @param {Object} opciones - Opciones del algoritmo genético
   */
  ws.iniciarPlanificacion = (fecha, factorK, opciones = {}) => {
    const request = {
      accion: "iniciar",
      fecha,
      factorK,
      ...opciones
    };
    
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(request));
      console.log('📤 Planificación iniciada:', request);
    } else {
      console.warn('⚠️ WebSocket no conectado. Estado:', ws.readyState);
    }
  };
  
  /**
   * Enviar mensaje personalizado
   */
  ws.enviar = (mensaje) => {
    if (ws.readyState === WebSocket.OPEN) {
      const data = typeof mensaje === 'string' ? mensaje : JSON.stringify(mensaje);
      ws.send(data);
      console.log('📤 Mensaje enviado:', mensaje);
    } else {
      console.warn('⚠️ WebSocket no conectado');
    }
  };
  
  /**
   * Verificar conexión
   */
  ws.estaConectado = () => ws.readyState === WebSocket.OPEN;
  
  /**
   * Cerrar conexión
   */
  ws.cerrar = () => {
    if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
      ws.close();
      console.log('🔌 Cerrando WebSocket Planificación...');
    }
  };
  
  return ws;
};

/**
 * Hook React para WebSocket de Planificación
 * @param {Function} onMessage - Callback para mensajes
 * @param {Function} onError - Callback para errores
 * @returns {{ ws: WebSocket, isConnected: boolean, iteraciones: Array }}
 */
export const useWebSocketPlanificacion = (onMessage, onError) => {
  const [ws, setWs] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [iteraciones, setIteraciones] = useState([]);

  useEffect(() => {
    const websocket = conectarWebSocketPlanificacion(
      (data) => {
        // Agregar iteración a la lista
        if (data.tipo === 'progreso' || data.tipo === 'completado') {
          setIteraciones(prev => [data, ...prev].slice(0, 50)); // Mantener últimas 50
        }
        onMessage(data);
      },
      onError,
      () => setIsConnected(true),
      () => {
        setIsConnected(false);
        setIteraciones([]); // Limpiar al desconectar
      }
    );

    setWs(websocket);

    return () => {
      if (websocket && websocket.readyState === WebSocket.OPEN) {
        websocket.close();
      }
    };
  }, []); // Solo montar una vez

  return { ws, isConnected, iteraciones };
};

export default {
  conectarWebSocket,
  useWebSocket,
  conectarWebSocketPlanificacion,
  useWebSocketPlanificacion,
  WS_BASE_URL,
  WS_PLANIFICACION_URL
};
