/**
 * ==================== SERVICIO DE SIMULACIÓN (REST + WebSocket STOMP) ====================
 * 
 * Propósito: Gestionar la comunicación con el backend de simulación de rutas
 * usando REST para control y WebSockets (STOMP) para datos en tiempo real.
 * 
 * 🔄 FLUJO CORRECTO (Backend Contract):
 * 1. connect()           -> Establece conexión WebSocket
 * 2. startSimulation()   -> POST /api/simulations -> Obtiene ID
 * 3. subscribe(id)       -> Se suscribe a /topic/simulations/{id}
 * 4. onMessage()         -> Recibe snapshots cada 500ms
 * 5. cancelSimulation()  -> POST /api/simulations/{id}/cancel
 * 6. disconnect()        -> Limpia conexión
 * 
 * ⚠️ IMPORTANTE: NO puedes suscribirte sin tener el ID primero
 * 
 * @author Senior Frontend Developer
 * @version 3.0 - Production Ready (Coordinación REST+WS)
 */

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL, WS_URL } from '../config/api';

// ==================== CONFIGURACIÓN ====================
const REST_BASE_URL = API_BASE_URL;
const WS_ENDPOINT = WS_URL;

// Estados de conexión
const ConnectionState = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  SUBSCRIBED: 'SUBSCRIBED',
  ERROR: 'ERROR'
};

// Estados de simulación
const SimulationStatus = {
  RUNNING: 'RUNNING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  ERROR: 'ERROR'
};

class SimulationService {
  constructor() {
    this.client = null;
    this.subscription = null;
    this.connectionState = ConnectionState.DISCONNECTED;
    this.simulationId = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000; // 3 segundos
    
    // Callbacks
    this.onMessageCallback = null;
    this.onErrorCallback = null;
    this.onConnectedCallback = null;
    this.onDisconnectedCallback = null;
    this.onSimulationCompleteCallback = null;
  }

  // ==================== GESTIÓN DE CALLBACKS ====================
  
  /**
   * Configura el callback para mensajes de simulación
   * @param {Function} callback - (data) => {}
   */
  onMessage(callback) {
    this.onMessageCallback = callback;
    return this;
  }

  /**
   * Configura el callback para errores
   * @param {Function} callback - (error) => {}
   */
  onError(callback) {
    this.onErrorCallback = callback;
    return this;
  }

  /**
   * Configura el callback cuando se conecta
   * @param {Function} callback - () => {}
   */
  onConnected(callback) {
    this.onConnectedCallback = callback;
    return this;
  }

  /**
   * Configura el callback cuando se desconecta
   * @param {Function} callback - () => {}
   */
  onDisconnected(callback) {
    this.onDisconnectedCallback = callback;
    return this;
  }

  /**
   * Configura el callback cuando la simulación termina
   * @param {Function} callback - (finalData) => {}
   */
  onSimulationComplete(callback) {
    this.onSimulationCompleteCallback = callback;
    return this;
  }

  // ==================== CONEXIÓN WEBSOCKET ====================
  
  /**
   * Establece conexión WebSocket con el backend
   * @returns {Promise<void>}
   */
  async connect() {
    if (this.connectionState === ConnectionState.CONNECTED || 
        this.connectionState === ConnectionState.CONNECTING) {
      console.log('⚠️ Ya existe una conexión activa o en progreso');
      return Promise.resolve();
    }

    this.connectionState = ConnectionState.CONNECTING;
    console.log('🔌 Conectando a WebSocket:', WS_ENDPOINT);

    return new Promise((resolve, reject) => {
      try {
        // Crear cliente STOMP con SockJS
        this.client = new Client({
          webSocketFactory: () => new SockJS(WS_ENDPOINT),
          debug: (str) => {
            if (process.env.NODE_ENV === 'development') {
              console.log('🐛 STOMP Debug:', str);
            }
          },
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          
          // Callback cuando se conecta
          onConnect: (frame) => {
            console.log('✅ WebSocket conectado exitosamente');
            this.connectionState = ConnectionState.CONNECTED;
            this.reconnectAttempts = 0;
            
            if (this.onConnectedCallback) {
              this.onConnectedCallback();
            }
            
            resolve();
          },
          
          // Callback cuando se desconecta
          onDisconnect: () => {
            console.log('🔌 WebSocket desconectado');
            this.connectionState = ConnectionState.DISCONNECTED;
            
            if (this.onDisconnectedCallback) {
              this.onDisconnectedCallback();
            }
          },
          
          // Callback de error
          onStompError: (frame) => {
            console.error('❌ Error STOMP:', frame.headers['message']);
            console.error('Detalles:', frame.body);
            this.connectionState = ConnectionState.ERROR;
            
            const error = new Error(frame.headers['message'] || 'Error STOMP desconocido');
            
            if (this.onErrorCallback) {
              this.onErrorCallback(error);
            }
            
            reject(error);
          },
          
          // Callback de error de conexión
          onWebSocketError: (event) => {
            console.error('❌ Error de WebSocket:', event);
            this.connectionState = ConnectionState.ERROR;
            
            const error = new Error('Error al conectar con el servidor WebSocket');
            
            if (this.onErrorCallback) {
              this.onErrorCallback(error);
            }
            
            // Intentar reconexión automática
            this.handleReconnect();
            
            reject(error);
          }
        });

        // Activar cliente
        this.client.activate();
        
      } catch (error) {
        console.error('❌ Error al inicializar cliente STOMP:', error);
        this.connectionState = ConnectionState.ERROR;
        
        if (this.onErrorCallback) {
          this.onErrorCallback(error);
        }
        
        reject(error);
      }
    });
  }

  /**
   * Maneja la reconexión automática
   */
  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 Intento de reconexión ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('❌ Fallo en reconexión:', error);
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('❌ Máximo de intentos de reconexión alcanzado');
      const error = new Error('No se pudo reconectar al servidor después de múltiples intentos');
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
    }
  }

  // ==================== GESTIÓN DE SIMULACIÓN ====================
  
  /**
   * Inicia una nueva simulación
   * @param {number} windowMinutes - Ventana de tiempo en minutos
   * @returns {Promise<string>} - ID de simulación
   */
  async startSimulation(windowMinutes = 60) {
    try {
      console.log('🚀 Iniciando simulación con ventana de', windowMinutes, 'minutos');
      
      const response = await fetch(`${REST_BASE_URL}/api/simulations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ windowMinutes })
      });

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status} - ${response.statusText}`);
      }

      const data = await response.json();
      
      // IMPORTANTE: El ID puede venir en diferentes campos
      const simulationId = data.simulationId || data.id || data.uuid;
      
      if (!simulationId) {
        throw new Error('No se recibió un ID de simulación válido del servidor');
      }

      this.simulationId = simulationId;
      console.log('✅ Simulación iniciada con ID:', this.simulationId);
      
      // Suscribirse automáticamente al tópico
      await this.subscribe(this.simulationId);
      
      return this.simulationId;
      
    } catch (error) {
      console.error('❌ Error al iniciar simulación:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Suscribirse al tópico de una simulación específica
   * @param {string} simulationId - ID de la simulación
   * @returns {Promise<void>}
   */
  async subscribe(simulationId) {
    if (!this.client || this.connectionState !== ConnectionState.CONNECTED) {
      throw new Error('Cliente no conectado. Llama a connect() primero.');
    }

    if (!simulationId) {
      throw new Error('ID de simulación requerido');
    }

    // Desuscribirse si ya existe una suscripción
    if (this.subscription) {
      console.log('🔄 Desuscribiendo de suscripción anterior...');
      this.subscription.unsubscribe();
    }

    const topic = `/topic/simulations/${simulationId}`;
    console.log('📡 Suscribiéndose al tópico:', topic);

    try {
      this.subscription = this.client.subscribe(topic, (message) => {
        try {
          const data = JSON.parse(message.body);
          console.log('📨 Snapshot recibido:', {
            status: data.status,
            progress: `${data.processedOrders}/${data.totalOrders}`,
            fitness: data.currentFitness,
            routes: data.routes?.length || 0
          });

          // Llamar callback de mensaje
          if (this.onMessageCallback) {
            this.onMessageCallback(data);
          }

          // Detectar simulación completada
          if (data.status === SimulationStatus.COMPLETED || 
              data.status === SimulationStatus.CANCELLED) {
            console.log(`🏁 Simulación ${data.status.toLowerCase()}`);
            
            if (this.onSimulationCompleteCallback) {
              this.onSimulationCompleteCallback(data);
            }
          }

        } catch (error) {
          console.error('❌ Error al procesar mensaje:', error);
          
          if (this.onErrorCallback) {
            this.onErrorCallback(error);
          }
        }
      });

      this.connectionState = ConnectionState.SUBSCRIBED;
      console.log('✅ Suscripción exitosa al tópico:', topic);
      
    } catch (error) {
      console.error('❌ Error al suscribirse:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Cancela la simulación actual
   * @returns {Promise<void>}
   */
  async cancelSimulation() {
    if (!this.simulationId) {
      console.warn('⚠️ No hay simulación activa para cancelar');
      return;
    }

    try {
      console.log('⏹️ Cancelando simulación:', this.simulationId);
      
      const response = await fetch(`${REST_BASE_URL}/api/simulations/${this.simulationId}/cancel`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status} - ${response.statusText}`);
      }

      console.log('✅ Simulación cancelada exitosamente');
      
    } catch (error) {
      console.error('❌ Error al cancelar simulación:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Desconecta y limpia todos los recursos
   */
  disconnect() {
    console.log('🔌 Desconectando y limpiando recursos...');
    
    // Desuscribirse
    if (this.subscription) {
      try {
        this.subscription.unsubscribe();
        console.log('✅ Desuscripción exitosa');
      } catch (error) {
        console.error('❌ Error al desuscribirse:', error);
      }
      this.subscription = null;
    }

    // Desactivar cliente
    if (this.client) {
      try {
        this.client.deactivate();
        console.log('✅ Cliente desactivado');
      } catch (error) {
        console.error('❌ Error al desactivar cliente:', error);
      }
      this.client = null;
    }

    // Resetear estado
    this.connectionState = ConnectionState.DISCONNECTED;
    this.simulationId = null;
    this.reconnectAttempts = 0;
    
    console.log('✅ Desconexión completa');
  }

  // ==================== UTILIDADES ====================
  
  /**
   * Verifica si está conectado
   * @returns {boolean}
   */
  isConnected() {
    return this.connectionState === ConnectionState.CONNECTED ||
           this.connectionState === ConnectionState.SUBSCRIBED;
  }

  /**
   * Verifica si está suscrito a una simulación
   * @returns {boolean}
   */
  isSubscribed() {
    return this.connectionState === ConnectionState.SUBSCRIBED && 
           this.subscription !== null;
  }

  /**
   * Obtiene el estado de conexión actual
   * @returns {string}
   */
  getConnectionState() {
    return this.connectionState;
  }

  /**
   * Obtiene el ID de simulación actual
   * @returns {string|null}
   */
  getSimulationId() {
    return this.simulationId;
  }
}

// ==================== EXPORTAR ====================

// Instancia singleton (opcional)
export const simulationService = new SimulationService();

// Exportar clase para crear múltiples instancias si es necesario
export default SimulationService;

// Exportar constantes
export { ConnectionState, SimulationStatus };
