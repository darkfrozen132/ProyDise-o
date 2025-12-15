/**
 * 🚀 SERVICIO DE SIMULACIÓN LOGÍSTICA
 * 
 * Gestiona la comunicación con el backend para simulaciones en tiempo real:
 * - REST API para control (iniciar/cancelar/pausar/reanudar)
 * - WebSocket STOMP para recibir actualizaciones en tiempo real
 * 
 * Arquitectura:
 * - Singleton pattern para una única instancia
 * - Observer pattern para callbacks
 * - Reconexión automática
 */

import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { API_BASE_URL } from '../config/api';

class SimulacionLogisticaService {
  constructor() {
    this.stompClient = null;
    this.sessionId = null;
    this.isConnected = false;
    this.subscription = null;
    
    // Callbacks
    this.callbacks = {
      onProgresoAG: null,
      onSnapshot: null,
      onCompleted: null,
      onError: null,
      onConnected: null,
      onDisconnected: null,
    };

    // Configuración (usa configuración centralizada)
    this.config = {
      baseURL: API_BASE_URL,
      wsEndpoint: '/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      maxReconnectAttempts: 5,
    };

    this.reconnectAttempts = 0;
  }

  /**
   * 📡 CONECTAR WEBSOCKET
   */
  connect() {
    return new Promise((resolve, reject) => {
      if (this.isConnected) {
        console.log('✅ WebSocket ya está conectado');
        resolve();
        return;
      }

      console.log(`🔌 Conectando WebSocket a ${this.config.baseURL}${this.config.wsEndpoint}...`);

      const socket = new SockJS(`${this.config.baseURL}${this.config.wsEndpoint}`);

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: this.config.reconnectDelay,
        heartbeatIncoming: this.config.heartbeatIncoming,
        heartbeatOutgoing: this.config.heartbeatOutgoing,
        
        onConnect: () => {
          console.log('✅ WebSocket conectado exitosamente');
          this.isConnected = true;
          this.reconnectAttempts = 0;
          
          if (this.callbacks.onConnected) {
            this.callbacks.onConnected();
          }
          
          resolve();
        },
        
        onStompError: (frame) => {
          console.error('❌ Error STOMP:', frame.headers['message']);
          console.error('Detalles:', frame.body);
          
          const error = {
            type: 'STOMP_ERROR',
            message: frame.headers['message'],
            details: frame.body
          };
          
          if (this.callbacks.onError) {
            this.callbacks.onError(error);
          }
          
          reject(error);
        },
        
        onWebSocketError: (error) => {
          console.error('❌ Error WebSocket:', error);
          
          const wsError = {
            type: 'WEBSOCKET_ERROR',
            message: 'Error de conexión WebSocket',
            details: error
          };
          
          if (this.callbacks.onError) {
            this.callbacks.onError(wsError);
          }
        },

        onWebSocketClose: () => {
          console.log('🔌 WebSocket cerrado');
          this.isConnected = false;
          
          if (this.callbacks.onDisconnected) {
            this.callbacks.onDisconnected();
          }

          // Intentar reconectar
          if (this.reconnectAttempts < this.config.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`🔄 Intento de reconexión ${this.reconnectAttempts}/${this.config.maxReconnectAttempts}...`);
          }
        }
      });

      this.stompClient.activate();
    });
  }

  /**
   * 🚀 INICIAR SIMULACIÓN
   * 
   * @param {string} fecha - Fecha de inicio (formato: YYYY-MM-DD)
   * @returns {Promise<string>} sessionId
   */
  async iniciarSimulacion(fecha) {
    try {
      console.log('🚀 Iniciando simulación...');
      console.log('📅 Fecha:', fecha);

      // 1. Hacer POST a /api/simulations/start
      const response = await fetch(`${this.config.baseURL}/api/simulations/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fecha: fecha,
          factorK: 5  // Factor K siempre es 5
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      console.log('📥 Respuesta del servidor:', data);

      if (!data.sessionId) {
        throw new Error(data.mensaje || 'No se recibió un sessionId válido');
      }

      this.sessionId = data.sessionId;

      // 2. Suscribirse al topic de la simulación
      await this.suscribirseASimulacion(data.sessionId);

      return data.sessionId;

    } catch (error) {
      console.error('❌ Error al iniciar simulación:', error);
      
      if (this.callbacks.onError) {
        this.callbacks.onError({
          type: 'INIT_ERROR',
          message: 'Error al iniciar la simulación',
          details: error.message
        });
      }
      
      throw error;
    }
  }

  /**
   * 📡 SUSCRIBIRSE AL TOPIC DE LA SIMULACIÓN
   */
  suscribirseASimulacion(sessionId) {
    return new Promise((resolve, reject) => {
      if (!this.isConnected) {
        reject(new Error('WebSocket no está conectado'));
        return;
      }

      const topic = `/topic/simulations/${sessionId}`;
      console.log(`📡 Suscribiéndose al topic: ${topic}`);

      try {
        this.subscription = this.stompClient.subscribe(topic, (message) => {
          this.manejarMensaje(message);
        });

        console.log('✅ Suscripción exitosa');
        resolve();
      } catch (error) {
        console.error('❌ Error al suscribirse:', error);
        reject(error);
      }
    });
  }

  /**
   * 📨 MANEJAR MENSAJE RECIBIDO
   */
  manejarMensaje(message) {
    try {
      const data = JSON.parse(message.body);
      console.log('📨 Mensaje recibido:', data);

      // Identificar tipo de mensaje
      if (data.tipo === 'PROGRESO_AG') {
        // Mensaje de progreso del Algoritmo Genético
        this.manejarProgresoAG(data);
      } else if (data.tipo === 'ERROR') {
        // Mensaje de error
        this.manejarError(data);
      } else if (data.status === 'RUNNING') {
        // Snapshot de simulación en ejecución
        this.manejarSnapshot(data);
      } else if (data.status === 'COMPLETED') {
        // Simulación completada
        this.manejarCompletado(data);
      } else if (data.status === 'CANCELLED') {
        // Simulación cancelada
        this.manejarCancelado(data);
      }

    } catch (error) {
      console.error('❌ Error al parsear mensaje:', error);
    }
  }

  /**
   * 🧬 MANEJAR PROGRESO DEL ALGORITMO GENÉTICO
   */
  manejarProgresoAG(data) {
    console.log(`🧬 Generación ${data.generacion}/${data.maxGeneraciones}`);
    console.log(`📊 Progreso: ${data.progreso.toFixed(2)}%`);
    console.log(`💯 Mejor fitness: ${data.mejorFitness}`);
    console.log(`📦 Pedidos: ${data.pedidosProcesados}/${data.pedidosTotales}`);

    if (this.callbacks.onProgresoAG) {
      this.callbacks.onProgresoAG(data);
    }
  }

  /**
   * 📊 MANEJAR SNAPSHOT DE SIMULACIÓN
   */
  manejarSnapshot(data) {
    console.log(`📊 Snapshot - Iteración: ${data.iteration}`);
    console.log(`⏱️  Tiempo simulado: ${data.simulatedTime}`);

    if (this.callbacks.onSnapshot) {
      this.callbacks.onSnapshot(data);
    }
  }

  /**
   * ✅ MANEJAR SIMULACIÓN COMPLETADA
   */
  manejarCompletado(data) {
    console.log('✅ Simulación completada exitosamente');
    console.log(`📦 Pedidos procesados: ${data.processedOrders}/${data.totalOrders}`);

    if (this.callbacks.onCompleted) {
      this.callbacks.onCompleted(data);
    }

    // Desuscribirse del topic
    this.desuscribirse();
  }

  /**
   * ⚠️ MANEJAR SIMULACIÓN CANCELADA
   */
  manejarCancelado(data) {
    console.log('⚠️ Simulación cancelada');

    if (this.callbacks.onCompleted) {
      this.callbacks.onCompleted(data);
    }

    this.desuscribirse();
  }

  /**
   * ❌ MANEJAR ERROR
   */
  manejarError(data) {
    console.error('❌ Error en simulación:', data.mensaje);

    if (this.callbacks.onError) {
      this.callbacks.onError({
        type: 'SIMULATION_ERROR',
        message: data.mensaje,
        timestamp: data.timestamp
      });
    }
  }

  /**
   * ⛔ CANCELAR SIMULACIÓN
   */
  async cancelarSimulacion() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay simulación activa para cancelar');
      return;
    }

    try {
      console.log(`⛔ Cancelando simulación ${this.sessionId}...`);

      const response = await fetch(
        `${this.config.baseURL}/api/simulations/${this.sessionId}/cancel`,
        { method: 'POST' }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      console.log('✅ Simulación cancelada:', data);

      this.desuscribirse();

    } catch (error) {
      console.error('❌ Error al cancelar simulación:', error);
      
      if (this.callbacks.onError) {
        this.callbacks.onError({
          type: 'CANCEL_ERROR',
          message: 'Error al cancelar la simulación',
          details: error.message
        });
      }
    }
  }

  /**
   * ⏸️ PAUSAR SIMULACIÓN
   */
  async pausarSimulacion() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay simulación activa para pausar');
      return;
    }

    try {
      console.log(`⏸️ Pausando simulación ${this.sessionId}...`);

      const response = await fetch(
        `${this.config.baseURL}/api/simulations/${this.sessionId}/pause`,
        { method: 'POST' }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      console.log('✅ Simulación pausada');

    } catch (error) {
      console.error('❌ Error al pausar simulación:', error);
      
      if (this.callbacks.onError) {
        this.callbacks.onError({
          type: 'PAUSE_ERROR',
          message: 'Error al pausar la simulación',
          details: error.message
        });
      }
    }
  }

  /**
   * ▶️ REANUDAR SIMULACIÓN
   */
  async reanudarSimulacion() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay simulación activa para reanudar');
      return;
    }

    try {
      console.log(`▶️ Reanudando simulación ${this.sessionId}...`);

      const response = await fetch(
        `${this.config.baseURL}/api/simulations/${this.sessionId}/resume`,
        { method: 'POST' }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      console.log('✅ Simulación reanudada');

    } catch (error) {
      console.error('❌ Error al reanudar simulación:', error);
      
      if (this.callbacks.onError) {
        this.callbacks.onError({
          type: 'RESUME_ERROR',
          message: 'Error al reanudar la simulación',
          details: error.message
        });
      }
    }
  }

  /**
   * 🔕 DESUSCRIBIRSE DEL TOPIC
   */
  desuscribirse() {
    if (this.subscription) {
      console.log('🔕 Desuscribiéndose del topic...');
      this.subscription.unsubscribe();
      this.subscription = null;
      this.sessionId = null;
    }
  }

  /**
   * 🔌 DESCONECTAR WEBSOCKET
   */
  disconnect() {
    console.log('🔌 Desconectando WebSocket...');
    
    this.desuscribirse();
    
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    
    this.isConnected = false;
    this.reconnectAttempts = 0;
  }

  /**
   * 🎯 REGISTRAR CALLBACKS
   */
  onProgresoAG(callback) {
    this.callbacks.onProgresoAG = callback;
    return this;
  }

  onSnapshot(callback) {
    this.callbacks.onSnapshot = callback;
    return this;
  }

  onCompleted(callback) {
    this.callbacks.onCompleted = callback;
    return this;
  }

  onError(callback) {
    this.callbacks.onError = callback;
    return this;
  }

  onConnected(callback) {
    this.callbacks.onConnected = callback;
    return this;
  }

  onDisconnected(callback) {
    this.callbacks.onDisconnected = callback;
    return this;
  }

  /**
   * 📊 OBTENER ESTADO
   */
  getEstado() {
    return {
      isConnected: this.isConnected,
      sessionId: this.sessionId,
      reconnectAttempts: this.reconnectAttempts,
    };
  }
}

// Exportar instancia única (Singleton)
export const simulacionService = new SimulacionLogisticaService();
