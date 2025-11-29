// ==================== CONFIGURACIÓN WEBSOCKET ====================
import { useState, useEffect } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

// URL base del WebSocket (simulación antigua)
const WS_BASE_URL = process.env.REACT_APP_API_URL 
  ? process.env.REACT_APP_API_URL.replace('http://', 'ws://').replace('https://', 'wss://')
  : 'ws://127.0.0.1:8000';

// URL del backend para STOMP (sin ws://, es HTTP porque SockJS maneja la conexión)
const STOMP_BACKEND_URL = 'http://localhost:8000';

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
 * Conectar al WebSocket de Planificación usando STOMP
 * Usa SockJS + STOMP para conectarse al backend Spring Boot
 * @param {Function} onMessage - Callback cuando llegan datos: (data) => {}
 * @param {Function} onError - Callback cuando hay error: (error) => {}
 * @param {Function} onOpen - Callback cuando se conecta: () => {}
 * @param {Function} onClose - Callback cuando se desconecta: () => {}
 * @returns {Object} Objeto con métodos: enviar(), iniciarPlanificacion(), cerrar()
 */
export const conectarWebSocketPlanificacion = (onMessage, onError, onOpen, onClose) => {
  console.log('🔄 Conectando a STOMP en:', STOMP_BACKEND_URL + '/ws');
  
  // Crear cliente STOMP con SockJS
  const stompClient = new Client({
    webSocketFactory: () => new SockJS(STOMP_BACKEND_URL + '/ws'),
    debug: (str) => {
      // Solo mostrar mensajes importantes, no todo el tráfico STOMP
      if (str.includes('ERROR') || str.includes('CONNECTED')) {
        console.log('🔌 STOMP:', str);
      }
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });
  
  let currentSubscription = null;
  let currentSessionId = null;
  
  // Configurar callbacks
  stompClient.onConnect = (frame) => {
    console.log('✅ STOMP conectado exitosamente');
    console.log('🔌 Frame de conexión:', frame);
    if (onOpen) onOpen();
  };
  
  stompClient.onStompError = (frame) => {
    console.error('❌ Error STOMP:', frame.headers['message']);
    console.error('� Detalles:', frame.body);
    if (onError) onError(new Error(frame.headers['message']));
  };
  
  stompClient.onWebSocketError = (error) => {
    console.error('❌ Error en WebSocket:', error);
    if (onError) onError(error);
  };
  
  stompClient.onWebSocketClose = (event) => {
    console.log('🔌 WebSocket cerrado');
    if (onClose) onClose(event);
  };
  
  // Activar cliente
  try {
    stompClient.activate();
  } catch (error) {
    console.error('❌ Error al activar STOMP:', error);
    if (onError) onError(error);
    return null;
  }
  
  /**
   * Suscribirse a los mensajes de una simulación específica
   * @param {string} sessionId - ID de la sesión de simulación
   */
  const suscribirseASimulacion = (sessionId) => {
    if (!stompClient.connected) {
      console.warn('⚠️ No conectado a STOMP, esperando...');
      return;
    }
    
    // Desuscribirse del canal anterior si existe
    if (currentSubscription) {
      currentSubscription.unsubscribe();
    }
    
    currentSessionId = sessionId;
    const topic = `/topic/simulations/${sessionId}`;
    console.log('� Suscribiéndose a:', topic);
    
    currentSubscription = stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body);
        console.log('📩 Mensaje STOMP recibido:', data);
        
        // Clasificar mensaje por tipo
        if (data.tipo === 'PROGRESO_AG') {
          console.log(`📊 Progreso AG - Generación ${data.generacion}/${data.maxGeneraciones}`);
        } else if (data.status === 'COMPLETED') {
          console.log('🎉 Simulación completada');
        } else if (data.status === 'CANCELLED') {
          console.log('� Simulación cancelada');
        } else if (data.status === 'ERROR') {
          console.error('❌ Error en simulación:', data.message);
        }
        
        onMessage(data);
      } catch (error) {
        console.error('❌ Error al parsear mensaje STOMP:', error);
        onMessage(message.body);
      }
    });
  };
  
  /**
   * Iniciar planificación con parámetros
   * Llama al endpoint REST y luego se suscribe al canal WebSocket
   * @param {string} fecha - Fecha inicial (YYYY-MM-DD)
   * @param {number} factorK - Factor de amplificación temporal
   * @param {Object} opciones - Opciones del algoritmo genético
   */
  const iniciarPlanificacion = async (fecha, factorK, opciones = {}) => {
    console.log('🚀 Iniciando planificación vía REST...');
    
    try {
      const response = await fetch(`${STOMP_BACKEND_URL}/api/simulations/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fecha,
          factorK,
          tamanioPoblacion: opciones.tamanioPoblacion || 10,
          maxGeneraciones: opciones.maxGeneraciones || 10,
          limiteGeneracionesSinMejora: opciones.limiteGeneracionesSinMejora || 5
        })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      const sessionId = data.sessionId;
      
      console.log('✅ Simulación iniciada con ID:', sessionId);
      
      // Suscribirse al canal de esta simulación
      suscribirseASimulacion(sessionId);
      
      return sessionId;
    } catch (error) {
      console.error('❌ Error al iniciar planificación:', error);
      if (onError) onError(error);
      throw error;
    }
  };
  
  /**
   * Enviar mensaje personalizado (no usado en STOMP, mantenido para compatibilidad)
   */
  const enviar = (mensaje) => {
    console.warn('⚠️ enviar() no implementado para STOMP');
    // En STOMP normalmente no enviamos mensajes arbitrarios
    // Todo se maneja vía REST + suscripciones
  };
  
  /**
   * Verificar conexión
   */
  const estaConectado = () => stompClient.connected;
  
  /**
   * Cerrar conexión
   */
  const cerrar = () => {
    if (currentSubscription) {
      currentSubscription.unsubscribe();
      currentSubscription = null;
    }
    stompClient.deactivate();
    console.log('🔌 Cerrando conexión STOMP...');
  };
  
  // Retornar objeto con métodos públicos
  return {
    enviar,
    iniciarPlanificacion,
    suscribirseASimulacion,
    estaConectado,
    cerrar,
    stompClient
  };
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
  STOMP_BACKEND_URL
};
