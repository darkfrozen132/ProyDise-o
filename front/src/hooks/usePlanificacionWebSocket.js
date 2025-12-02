/**
 * ==================== HOOK: usePlanificacionWebSocket ====================
 * 
 * Hook personalizado para gestionar el WebSocket de planificación con:
 * - Reconexión automática con backoff exponencial
 * - Indicador de calidad de conexión (latencia)
 * - Heartbeat para detectar desconexiones
 * - Estado unificado y limpio
 * - Timeout configurable
 * 
 * @author Refactored for SimuladorSemanal
 * @version 2.0
 */

import { useState, useEffect, useRef, useCallback } from 'react';

// ==================== CONFIGURACIÓN ====================
const WS_URL = 'ws://localhost:8000/ws/planificacion';

const DEFAULT_CONFIG = {
  reconnectDelay: 1000,        // Delay inicial de reconexión (1s)
  maxReconnectDelay: 30000,    // Delay máximo de reconexión (30s)
  maxReconnectAttempts: 10,    // Máximo de intentos de reconexión
  heartbeatInterval: 5000,     // Intervalo de heartbeat (5s)
  heartbeatTimeout: 10000,     // Timeout de heartbeat (10s)
  connectionTimeout: 5000,     // Timeout de conexión inicial (5s)
};

// Estados de conexión
const ConnectionState = {
  DISCONNECTED: 'disconnected',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  ERROR: 'error',
};

// Calidad de conexión
const ConnectionQuality = {
  EXCELLENT: 'excellent',  // < 100ms
  GOOD: 'good',            // 100-300ms
  FAIR: 'fair',            // 300-500ms
  POOR: 'poor',            // > 500ms
  UNKNOWN: 'unknown',
};

/**
 * Hook para gestionar WebSocket de planificación
 * @param {Object} options - Opciones de configuración
 * @returns {Object} Estado y métodos del WebSocket
 */
const usePlanificacionWebSocket = (options = {}) => {
  const config = { ...DEFAULT_CONFIG, ...options };
  
  // ==================== ESTADOS ====================
  const [connectionState, setConnectionState] = useState(ConnectionState.DISCONNECTED);
  const [connectionQuality, setConnectionQuality] = useState(ConnectionQuality.UNKNOWN);
  const [latency, setLatency] = useState(null);
  const [lastMessage, setLastMessage] = useState(null);
  const [error, setError] = useState(null);
  const [reconnectAttempt, setReconnectAttempt] = useState(0);
  const [iteraciones, setIteraciones] = useState([]);
  const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle');
  
  // ==================== REFS ====================
  const wsRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const heartbeatIntervalRef = useRef(null);
  const heartbeatTimeoutRef = useRef(null);
  const lastHeartbeatRef = useRef(null);
  const connectionTimeoutRef = useRef(null);
  const isUnmountedRef = useRef(false);
  
  // ==================== CALLBACKS ====================
  const onMessageRef = useRef(null);
  const onErrorRef = useRef(null);
  const onConnectedRef = useRef(null);
  const onDisconnectedRef = useRef(null);
  
  /**
   * Calcular calidad de conexión basada en latencia
   */
  const calculateConnectionQuality = useCallback((latencyMs) => {
    if (latencyMs === null) return ConnectionQuality.UNKNOWN;
    if (latencyMs < 100) return ConnectionQuality.EXCELLENT;
    if (latencyMs < 300) return ConnectionQuality.GOOD;
    if (latencyMs < 500) return ConnectionQuality.FAIR;
    return ConnectionQuality.POOR;
  }, []);
  
  /**
   * Limpiar todos los timeouts y intervalos
   */
  const cleanup = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
    if (heartbeatTimeoutRef.current) {
      clearTimeout(heartbeatTimeoutRef.current);
      heartbeatTimeoutRef.current = null;
    }
    if (connectionTimeoutRef.current) {
      clearTimeout(connectionTimeoutRef.current);
      connectionTimeoutRef.current = null;
    }
  }, []);
  
  /**
   * Cerrar WebSocket actual
   */
  const closeWebSocket = useCallback(() => {
    if (wsRef.current) {
      // Remover listeners antes de cerrar
      wsRef.current.onopen = null;
      wsRef.current.onmessage = null;
      wsRef.current.onerror = null;
      wsRef.current.onclose = null;
      
      if (wsRef.current.readyState === WebSocket.OPEN || 
          wsRef.current.readyState === WebSocket.CONNECTING) {
        wsRef.current.close(1000, 'Cierre limpio');
      }
      wsRef.current = null;
    }
    cleanup();
  }, [cleanup]);
  
  /**
   * Iniciar heartbeat para detectar desconexiones
   */
  const startHeartbeat = useCallback(() => {
    // Limpiar heartbeat anterior
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }
    if (heartbeatTimeoutRef.current) {
      clearTimeout(heartbeatTimeoutRef.current);
    }
    
    heartbeatIntervalRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        const now = Date.now();
        lastHeartbeatRef.current = now;
        
        // Enviar ping
        try {
          wsRef.current.send(JSON.stringify({ 
            accion: 'ping', 
            timestamp: now 
          }));
        } catch (e) {
          console.warn('⚠️ Error enviando heartbeat:', e);
        }
        
        // Timeout para respuesta
        heartbeatTimeoutRef.current = setTimeout(() => {
          console.warn('⚠️ Heartbeat timeout - reconectando...');
          handleReconnect();
        }, config.heartbeatTimeout);
      }
    }, config.heartbeatInterval);
  }, [config.heartbeatInterval, config.heartbeatTimeout]);
  
  /**
   * Manejar reconexión con backoff exponencial
   */
  const handleReconnect = useCallback(() => {
    if (isUnmountedRef.current) return;
    
    const nextAttempt = reconnectAttempt + 1;
    
    if (nextAttempt > config.maxReconnectAttempts) {
      console.error('❌ Máximo de intentos de reconexión alcanzado');
      setConnectionState(ConnectionState.ERROR);
      setError(new Error('No se pudo reconectar después de múltiples intentos'));
      return;
    }
    
    // Calcular delay con backoff exponencial
    const delay = Math.min(
      config.reconnectDelay * Math.pow(2, reconnectAttempt),
      config.maxReconnectDelay
    );
    
    console.log(`🔄 Reconectando en ${delay/1000}s... (intento ${nextAttempt}/${config.maxReconnectAttempts})`);
    setConnectionState(ConnectionState.RECONNECTING);
    setReconnectAttempt(nextAttempt);
    
    closeWebSocket();
    
    reconnectTimeoutRef.current = setTimeout(() => {
      if (!isUnmountedRef.current) {
        connect();
      }
    }, delay);
  }, [reconnectAttempt, config, closeWebSocket]);
  
  /**
   * Conectar al WebSocket
   */
  const connect = useCallback(() => {
    if (isUnmountedRef.current) return;
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      console.log('⚠️ WebSocket ya está conectado');
      return;
    }
    
    console.log('🔌 Conectando a WebSocket:', WS_URL);
    setConnectionState(ConnectionState.CONNECTING);
    setError(null);
    
    try {
      const ws = new WebSocket(WS_URL);
      wsRef.current = ws;
      
      // Timeout de conexión
      connectionTimeoutRef.current = setTimeout(() => {
        if (ws.readyState === WebSocket.CONNECTING) {
          console.warn('⚠️ Timeout de conexión');
          ws.close();
          handleReconnect();
        }
      }, config.connectionTimeout);
      
      ws.onopen = () => {
        if (isUnmountedRef.current) return;
        
        clearTimeout(connectionTimeoutRef.current);
        console.log('✅ WebSocket conectado');
        
        setConnectionState(ConnectionState.CONNECTED);
        setReconnectAttempt(0);
        setError(null);
        
        // Iniciar heartbeat
        startHeartbeat();
        
        // Callback
        if (onConnectedRef.current) {
          onConnectedRef.current();
        }
      };
      
      ws.onmessage = (event) => {
        if (isUnmountedRef.current) return;
        
        try {
          const data = JSON.parse(event.data);
          
          // Manejar pong (respuesta a heartbeat)
          if (data.tipo === 'pong' && lastHeartbeatRef.current) {
            const currentLatency = Date.now() - lastHeartbeatRef.current;
            setLatency(currentLatency);
            setConnectionQuality(calculateConnectionQuality(currentLatency));
            
            // Cancelar timeout de heartbeat
            if (heartbeatTimeoutRef.current) {
              clearTimeout(heartbeatTimeoutRef.current);
            }
            return;
          }
          
          // Manejar mensaje de conexión (no tiene 'pong' explícito)
          if (data.tipo === 'conexion') {
            console.log('✅ Confirmación de conexión:', data.mensaje);
            // Cancelar timeout de heartbeat si existe
            if (heartbeatTimeoutRef.current) {
              clearTimeout(heartbeatTimeoutRef.current);
            }
          }
          
          setLastMessage(data);
          
          // Procesar mensaje según tipo
          processMessage(data);
          
          // Callback externo
          if (onMessageRef.current) {
            onMessageRef.current(data);
          }
          
        } catch (e) {
          console.error('❌ Error parseando mensaje:', e);
        }
      };
      
      ws.onerror = (error) => {
        console.error('❌ Error en WebSocket:', error);
        setError(error);
        
        if (onErrorRef.current) {
          onErrorRef.current(error);
        }
      };
      
      ws.onclose = (event) => {
        if (isUnmountedRef.current) return;
        
        console.log(`🔌 WebSocket cerrado. Código: ${event.code}, Limpio: ${event.wasClean}`);
        
        cleanup();
        setConnectionQuality(ConnectionQuality.UNKNOWN);
        setLatency(null);
        
        if (onDisconnectedRef.current) {
          onDisconnectedRef.current(event);
        }
        
        // Reconectar solo si no fue cierre limpio
        if (!event.wasClean && connectionState !== ConnectionState.DISCONNECTED) {
          handleReconnect();
        } else {
          setConnectionState(ConnectionState.DISCONNECTED);
        }
      };
      
    } catch (e) {
      console.error('❌ Error creando WebSocket:', e);
      setError(e);
      setConnectionState(ConnectionState.ERROR);
    }
  }, [config, handleReconnect, startHeartbeat, calculateConnectionQuality, cleanup, connectionState]);
  
  /**
   * Procesar mensaje recibido
   */
  const processMessage = useCallback((data) => {
    switch (data.tipo) {
      case 'conexion':
        console.log('✅ Conexión establecida:', data.mensaje);
        break;
        
      case 'progreso':
        setIteraciones(prev => [data, ...prev].slice(0, 20));
        setEstadoPlanificacion('running');
        console.log(`📊 Progreso - Iteración #${data.datos?.ejecucionNumero || '?'}`);
        break;
        
      case 'completado':
        setIteraciones(prev => [data, ...prev].slice(0, 50));
        // No cambiar a 'completed' aquí para permitir continuar
        console.log('🎉 Iteración completada');
        break;
        
      case 'error':
        console.error('❌ Error en planificación:', data.mensaje);
        if (!data.mensaje?.includes('Ya hay una planificación en curso')) {
          setEstadoPlanificacion('error');
        }
        break;
        
      default:
        console.log('📨 Mensaje:', data.tipo);
    }
  }, []);
  
  /**
   * Desconectar del WebSocket
   */
  const disconnect = useCallback(() => {
    console.log('🔌 Desconectando WebSocket...');
    setConnectionState(ConnectionState.DISCONNECTED);
    closeWebSocket();
  }, [closeWebSocket]);
  
  /**
   * Enviar mensaje por WebSocket
   */
  const send = useCallback((message) => {
    if (wsRef.current?.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket no está conectado');
      return false;
    }
    
    try {
      const data = typeof message === 'string' ? message : JSON.stringify(message);
      wsRef.current.send(data);
      return true;
    } catch (e) {
      console.error('❌ Error enviando mensaje:', e);
      return false;
    }
  }, []);
  
  /**
   * Iniciar planificación
   */
  const iniciarPlanificacion = useCallback((fecha, factorK = 5, opciones = {}) => {
    if (wsRef.current?.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket no conectado');
      return false;
    }
    
    const request = {
      accion: 'iniciar',
      fecha,
      factorK,
      ...opciones
    };
    
    console.log('📤 Iniciando planificación:', request);
    setEstadoPlanificacion('running');
    
    return send(request);
  }, [send]);
  
  /**
   * Registrar callbacks
   */
  const onMessage = useCallback((callback) => {
    onMessageRef.current = callback;
  }, []);
  
  const onError = useCallback((callback) => {
    onErrorRef.current = callback;
  }, []);
  
  const onConnected = useCallback((callback) => {
    onConnectedRef.current = callback;
  }, []);
  
  const onDisconnected = useCallback((callback) => {
    onDisconnectedRef.current = callback;
  }, []);
  
  /**
   * Limpiar iteraciones
   */
  const limpiarIteraciones = useCallback(() => {
    setIteraciones([]);
    setEstadoPlanificacion('idle');
  }, []);
  
  /**
   * Detener planificación
   */
  const detenerPlanificacion = useCallback(() => {
    setEstadoPlanificacion('idle');
    // Podría enviar comando de cancelación al servidor
    send({ accion: 'cancelar' });
  }, [send]);
  
  // ==================== EFECTOS ====================
  
  // Auto-conectar al montar
  useEffect(() => {
    isUnmountedRef.current = false;
    
    console.log('🔌 usePlanificacionWebSocket: Montando hook, intentando conectar...');
    
    // Delay para evitar problemas con StrictMode
    const timer = setTimeout(() => {
      if (!isUnmountedRef.current) {
        console.log('🔌 usePlanificacionWebSocket: Llamando a connect()');
        connect();
      }
    }, 100);
    
    return () => {
      console.log('🔌 usePlanificacionWebSocket: Desmontando hook');
      isUnmountedRef.current = true;
      clearTimeout(timer);
      closeWebSocket();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  
  // ==================== RETORNO ====================
  return {
    // Estado
    connectionState,
    isConnected: connectionState === ConnectionState.CONNECTED,
    isConnecting: connectionState === ConnectionState.CONNECTING,
    isReconnecting: connectionState === ConnectionState.RECONNECTING,
    connectionQuality,
    latency,
    error,
    reconnectAttempt,
    
    // Estado de planificación
    iteraciones,
    estadoPlanificacion,
    lastMessage,
    
    // Métodos
    connect,
    disconnect,
    send,
    iniciarPlanificacion,
    limpiarIteraciones,
    detenerPlanificacion,
    
    // Registrar callbacks
    onMessage,
    onError,
    onConnected,
    onDisconnected,
    
    // Constantes exportadas
    ConnectionState,
    ConnectionQuality,
  };
};

export default usePlanificacionWebSocket;
export { ConnectionState, ConnectionQuality };
