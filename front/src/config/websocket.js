// ==================== CONFIGURACIÓN WEBSOCKET ====================
import { useState, useEffect } from 'react';

const WS_BASE_URL = process.env.REACT_APP_API_URL 
  ? process.env.REACT_APP_API_URL.replace('http://', 'ws://').replace('https://', 'wss://')
  : 'ws://127.0.0.1:8000';

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

export default {
  conectarWebSocket,
  useWebSocket,
  WS_BASE_URL
};
