/**
 * Hook useWebSocketStomp
 * Maneja la conexión WebSocket STOMP para la simulación semanal
 */

import { useCallback, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WEBSOCKET_URL, SIMULATION_STATES } from '../constants';

/**
 * Hook para manejar conexión WebSocket STOMP
 * @returns {Object} - Estado y funciones de conexión
 */
export function useWebSocketStomp() {
  // Estado de conexión
  const [wsStompConectado, setWsStompConectado] = useState(false);
  const [estadoSimulacionStomp, setEstadoSimulacionStomp] = useState(SIMULATION_STATES.DISCONNECTED);
  const [sessionId, setSessionId] = useState(null);
  
  // Referencias
  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  
  // Callbacks de mensajes (se setean externamente)
  const onMessageCallbackRef = useRef(null);
  const onErrorCallbackRef = useRef(null);
  const onConnectedCallbackRef = useRef(null);
  
  /**
   * Conectar WebSocket STOMP
   */
  const conectar = useCallback(() => {
    if (stompClientRef.current && stompClientRef.current.active) {
      console.log('[useWebSocketStomp] WebSocket STOMP ya conectado');
      return;
    }

    console.log('[useWebSocketStomp] Conectando WebSocket STOMP...');
    setEstadoSimulacionStomp(SIMULATION_STATES.CONNECTING);

    const socket = new SockJS(WEBSOCKET_URL);
    
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        console.log('[useWebSocketStomp] WebSocket STOMP conectado');
        setWsStompConectado(true);
        setEstadoSimulacionStomp(SIMULATION_STATES.CONNECTED);
        if (onConnectedCallbackRef.current) {
          onConnectedCallbackRef.current();
        }
      },
      
      onStompError: (frame) => {
        console.error('[useWebSocketStomp] Error STOMP:', frame.headers['message']);
        setEstadoSimulacionStomp(SIMULATION_STATES.ERROR);
        if (onErrorCallbackRef.current) {
          onErrorCallbackRef.current(`Error STOMP: ${frame.headers['message']}`);
        }
      },
      
      onWebSocketError: (error) => {
        console.error('[useWebSocketStomp] Error WebSocket:', error);
        setWsStompConectado(false);
        setEstadoSimulacionStomp(SIMULATION_STATES.ERROR);
        if (onErrorCallbackRef.current) {
          onErrorCallbackRef.current('Error de conexion WebSocket');
        }
      },
      
      onDisconnect: () => {
        console.log('[useWebSocketStomp] WebSocket STOMP desconectado');
        setWsStompConectado(false);
        setEstadoSimulacionStomp(SIMULATION_STATES.DISCONNECTED);
      }
    });

    stompClient.activate();
    stompClientRef.current = stompClient;
  }, []);

  /**
   * Desconectar WebSocket STOMP
   */
  const desconectar = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    setWsStompConectado(false);
    setEstadoSimulacionStomp(SIMULATION_STATES.DISCONNECTED);
    setSessionId(null);
    console.log('[useWebSocketStomp] WebSocket STOMP desconectado completamente');
  }, []);

  /**
   * Suscribirse a un topic específico
   * @param {string} topicUrl - URL del topic (ej: /topic/simulations/123)
   * @param {Function} onMessage - Callback para mensajes recibidos
   */
  const suscribirse = useCallback((topicUrl, onMessage) => {
    if (!stompClientRef.current || !stompClientRef.current.active) {
      console.error('[useWebSocketStomp] No hay conexion activa para suscribirse');
      return null;
    }

    console.log(`[useWebSocketStomp] Suscribiendose a: ${topicUrl}`);

    const subscription = stompClientRef.current.subscribe(
      topicUrl,
      (message) => {
        try {
          const datos = JSON.parse(message.body);
          console.log('[useWebSocketStomp] Mensaje recibido:', datos);
          if (onMessage) {
            onMessage(datos);
          }
          if (onMessageCallbackRef.current) {
            onMessageCallbackRef.current(datos);
          }
        } catch (error) {
          console.error('[useWebSocketStomp] Error parseando mensaje:', error);
          if (onErrorCallbackRef.current) {
            onErrorCallbackRef.current('Error procesando mensaje del servidor');
          }
        }
      }
    );

    subscriptionRef.current = subscription;
    return subscription;
  }, []);

  /**
   * Desuscribirse del topic actual
   */
  const desuscribirse = useCallback(() => {
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
      console.log('[useWebSocketStomp] Desuscrito del topic');
    }
  }, []);

  /**
   * Configurar callbacks de eventos
   */
  const setOnMessage = useCallback((callback) => {
    onMessageCallbackRef.current = callback;
  }, []);

  const setOnError = useCallback((callback) => {
    onErrorCallbackRef.current = callback;
  }, []);

  const setOnConnected = useCallback((callback) => {
    onConnectedCallbackRef.current = callback;
  }, []);

  return {
    // Estado
    wsStompConectado,
    estadoSimulacionStomp,
    sessionId,
    setSessionId,
    setEstadoSimulacionStomp,
    
    // Referencias (para uso avanzado)
    stompClientRef,
    subscriptionRef,
    
    // Funciones
    conectar,
    desconectar,
    suscribirse,
    desuscribirse,
    
    // Configuración de callbacks
    setOnMessage,
    setOnError,
    setOnConnected
  };
}

export default useWebSocketStomp;
