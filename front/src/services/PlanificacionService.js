/**
 * ==================== SERVICIO DE PLANIFICACIÓN (REST + WebSocket STOMP) ====================
 * 
 * Propósito: Gestionar la comunicación con el backend de planificación de rutas
 * usando WebSocket (STOMP) para control y datos en tiempo real.
 * 
 * 🔄 FLUJO CORRECTO (Backend Contract):
 * 1. connect()                    -> Conecta a /ws/planificacion
 * 2. iniciar(params)              -> Envía a /app/planificacion/iniciar
 * 3. subscribe(sessionId)         -> Se suscribe a /topic/planificacion/progreso/{sessionId}
 * 4. onProgreso()                 -> Recibe ProgresoAGDTO continuamente
 * 5. pausar(sessionId)            -> Envía a /app/planificacion/pausar/{sessionId}
 * 6. reanudar(sessionId)          -> Envía a /app/planificacion/reanudar/{sessionId}
 * 7. cancelar(sessionId)          -> Envía a /app/planificacion/cancelar/{sessionId}
 * 8. disconnect()                 -> Limpia conexión
 * 
 * @author Senior Frontend Developer
 * @version 4.0 - Específico para tu backend
 */

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL } from '../config/api';

// ==================== CONFIGURACIÓN ====================
const WS_ENDPOINT = `${API_BASE_URL}/ws/planificacion`;

// Estados de conexión
const ConnectionState = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  CONNECTED: 'CONNECTED',
  SUBSCRIBED: 'SUBSCRIBED',
  ERROR: 'ERROR'
};

// Estados de planificación
const PlanificacionStatus = {
  INICIANDO: 'INICIANDO',
  EN_PROGRESO: 'EN_PROGRESO',
  PAUSADO: 'PAUSADO',
  COMPLETADO: 'COMPLETADO',
  CANCELADO: 'CANCELADO',
  ERROR: 'ERROR'
};

/**
 * Servicio de Planificación con WebSocket
 */
class PlanificacionService {
  constructor() {
    this.client = null;
    this.subscription = null;
    this.connectionState = ConnectionState.DISCONNECTED;
    this.sessionId = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000; // 3 segundos
    
    // 🗺️ NUEVO: Mapa de aeropuertos con coordenadas
    this.aeropuertosMap = {};
    this.aeropuertosCargados = false;
    
    // Callbacks
    this.onProgresoCallback = null;
    this.onErrorCallback = null;
    this.onConnectedCallback = null;
    this.onDisconnectedCallback = null;
    this.onCompletadoCallback = null;
    this.onRutasProcesamCallback = null; // 🆕 Callback para rutas con coordenadas
  }

  // ==================== GESTIÓN DE CALLBACKS ====================
  
  /**
   * Configura el callback para mensajes de progreso
   * @param {Function} callback - (progresoDTO) => {}
   */
  onProgreso(callback) {
    this.onProgresoCallback = callback;
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
   * Configura el callback cuando la planificación se completa
   * @param {Function} callback - (finalData) => {}
   */
  onCompletado(callback) {
    this.onCompletadoCallback = callback;
    return this;
  }

  /**
   * 🆕 Configura el callback para rutas procesadas con coordenadas
   * @param {Function} callback - (rutasProcesadas) => {}
   */
  onRutasProcesadas(callback) {
    this.onRutasProcesamCallback = callback;
    return this;
  }

  // ==================== CARGA DE AEROPUERTOS ====================
  
  /**
   * 🆕 Carga los aeropuertos desde el backend y crea el mapa de coordenadas
   * @returns {Promise<Object>} - Diccionario {codigoICAO: {lat, lon, ciudad, pais}}
   */
  async cargarAeropuertos() {
    if (this.aeropuertosCargados) {
      console.log('✅ Aeropuertos ya cargados en caché');
      return this.aeropuertosMap;
    }

    try {
      console.log('📍 Cargando aeropuertos desde API...');
      
      const response = await fetch(`${API_BASE_URL}/api/aeropuertos/listar`);
      
      if (!response.ok) {
        throw new Error(`Error HTTP: ${response.status}`);
      }

      const aeropuertos = await response.json();
      
      // Crear diccionario: codigo ICAO -> coordenadas
      this.aeropuertosMap = {};
      aeropuertos.forEach(aero => {
        this.aeropuertosMap[aero.codigoICAO] = {
          lat: aero.latitud,
          lon: aero.longitud,
          ciudad: aero.ciudad || aero.nombre,
          pais: aero.pais,
          nombre: aero.nombre,
          iata: aero.codigoIATA
        };
      });

      this.aeropuertosCargados = true;
      console.log(`✅ ${aeropuertos.length} aeropuertos cargados:`, Object.keys(this.aeropuertosMap));
      
      return this.aeropuertosMap;
      
    } catch (error) {
      console.error('❌ Error al cargar aeropuertos:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * 🆕 Procesa rutas del backend y las enriquece con coordenadas
   * @param {Array} rutas - Array de rutas del backend
   * @returns {Array} - Array de rutas con coordenadas completas
   */
  procesarRutasConCoordenadas(rutas) {
    if (!rutas || rutas.length === 0) {
      console.warn('⚠️ No hay rutas para procesar');
      return [];
    }

    if (!this.aeropuertosCargados) {
      console.error('❌ Aeropuertos no cargados. Llama a cargarAeropuertos() primero.');
      return [];
    }

    console.log(`🗺️ Procesando ${rutas.length} rutas con coordenadas...`);
    
    const rutasProcesadas = [];

    rutas.forEach((ruta, index) => {
      const origen = this.aeropuertosMap[ruta.origen];
      const destino = this.aeropuertosMap[ruta.destino];

      if (!origen) {
        console.warn(`⚠️ Coordenadas no encontradas para origen: ${ruta.origen}`);
        return;
      }

      if (!destino) {
        console.warn(`⚠️ Coordenadas no encontradas para destino: ${ruta.destino}`);
        return;
      }

      // Crear ruta enriquecida con coordenadas
      const rutaProcesada = {
        pedidoId: ruta.pedidoId,
        vueloId: ruta.vueloId || `${ruta.origen}-${ruta.destino}-${index}`,
        origen: {
          codigo: ruta.origen,
          lat: origen.lat,
          lon: origen.lon,
          ciudad: origen.ciudad,
          pais: origen.pais,
          nombre: origen.nombre
        },
        destino: {
          codigo: ruta.destino,
          lat: destino.lat,
          lon: destino.lon,
          ciudad: destino.ciudad,
          pais: destino.pais,
          nombre: destino.nombre
        },
        salida: ruta.salida,
        llegada: ruta.llegada,
        duracionHoras: ruta.duracionHoras,
        distanciaKm: ruta.distanciaKm
      };

      rutasProcesadas.push(rutaProcesada);
    });

    console.log(`✅ ${rutasProcesadas.length} rutas procesadas con coordenadas`);
    
    return rutasProcesadas;
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

  // ==================== GESTIÓN DE PLANIFICACIÓN ====================
  
  /**
   * Inicia una nueva planificación
   * @param {Object} params - Parámetros de planificación
   * @param {string} params.tiempoActualSimulacion - Fecha en formato ISO
   * @param {number} params.factorK - Factor K
   * @param {number} [params.tamanioPoblacion] - Tamaño de población
   * @param {number} [params.maxGeneraciones] - Máximo de generaciones
   * @param {number} [params.limiteGeneracionesSinMejora] - Límite sin mejora
   * @returns {Promise<void>}
   */
  async iniciar(params) {
    if (!this.client || this.connectionState !== ConnectionState.CONNECTED) {
      throw new Error('Cliente no conectado. Llama a connect() primero.');
    }

    try {
      console.log('🚀 Iniciando planificación con parámetros:', params);
      
      const request = {
        tiempoActualSimulacion: params.tiempoActualSimulacion,
        factorK: params.factorK || 5,
        tamanioPoblacion: params.tamanioPoblacion || 20,
        maxGeneraciones: params.maxGeneraciones || 20,
        limiteGeneracionesSinMejora: params.limiteGeneracionesSinMejora || 10
      };

      // Enviar comando de inicio
      this.client.publish({
        destination: '/app/planificacion/iniciar',
        body: JSON.stringify(request)
      });

      console.log('✅ Comando de inicio enviado');
      
    } catch (error) {
      console.error('❌ Error al iniciar planificación:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Suscribirse al tópico de progreso de una sesión específica
   * @param {string} sessionId - ID de la sesión
   * @returns {Promise<void>}
   */
  async subscribe(sessionId) {
    if (!this.client || this.connectionState !== ConnectionState.CONNECTED) {
      throw new Error('Cliente no conectado. Llama a connect() primero.');
    }

    if (!sessionId) {
      throw new Error('ID de sesión requerido');
    }

    // Desuscribirse si ya existe una suscripción
    if (this.subscription) {
      console.log('🔄 Desuscribiendo de suscripción anterior...');
      this.subscription.unsubscribe();
    }

    const topic = `/topic/planificacion/progreso/${sessionId}`;
    console.log('📡 Suscribiéndose al tópico:', topic);

    try {
      this.subscription = this.client.subscribe(topic, (message) => {
        try {
          const data = JSON.parse(message.body);
          console.log('📨 Progreso recibido:', {
            generacion: `${data.generacionActual}/${data.totalGeneraciones}`,
            progreso: `${data.porcentaje?.toFixed(1)}%`,
            fitness: data.mejorFitness,
            eta: `${(data.etaMs / 1000).toFixed(1)}s`
          });

          // Llamar callback de progreso
          if (this.onProgresoCallback) {
            this.onProgresoCallback(data);
          }

          // 🗺️ NUEVO: Procesar rutas si vienen en la solución
          if (data.solucion && data.solucion.rutas && data.solucion.rutas.length > 0) {
            console.log(`🗺️ Procesando ${data.solucion.rutas.length} rutas para el mapa...`);
            
            const rutasProcesadas = this.procesarRutasConCoordenadas(data.solucion.rutas);
            
            // Llamar callback de rutas procesadas
            if (this.onRutasProcesamCallback && rutasProcesadas.length > 0) {
              this.onRutasProcesamCallback(rutasProcesadas);
            }
            
            // Guardar en window para debugging (opcional)
            if (typeof window !== 'undefined') {
              window.ultimasRutas = rutasProcesadas;
              console.log('💡 TIP: Accede a las rutas con window.ultimasRutas');
            }
          }

          // Detectar si se completó (100% o última generación)
          if (data.porcentaje >= 100 || data.generacionActual >= data.totalGeneraciones) {
            console.log('🏁 Planificación completada');
            
            if (this.onCompletadoCallback) {
              this.onCompletadoCallback(data);
            }
          }

        } catch (error) {
          console.error('❌ Error al procesar mensaje de progreso:', error);
          
          if (this.onErrorCallback) {
            this.onErrorCallback(error);
          }
        }
      });

      this.sessionId = sessionId;
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
   * Pausa la planificación actual
   * @returns {Promise<void>}
   */
  async pausar() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay sesión activa para pausar');
      return;
    }

    try {
      console.log('⏸️ Pausando planificación:', this.sessionId);
      
      this.client.publish({
        destination: `/app/planificacion/pausar/${this.sessionId}`,
        body: ''
      });

      console.log('✅ Comando de pausa enviado');
      
    } catch (error) {
      console.error('❌ Error al pausar planificación:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Reanuda la planificación pausada
   * @returns {Promise<void>}
   */
  async reanudar() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay sesión activa para reanudar');
      return;
    }

    try {
      console.log('▶️ Reanudando planificación:', this.sessionId);
      
      this.client.publish({
        destination: `/app/planificacion/reanudar/${this.sessionId}`,
        body: ''
      });

      console.log('✅ Comando de reanudación enviado');
      
    } catch (error) {
      console.error('❌ Error al reanudar planificación:', error);
      
      if (this.onErrorCallback) {
        this.onErrorCallback(error);
      }
      
      throw error;
    }
  }

  /**
   * Cancela la planificación actual
   * @returns {Promise<void>}
   */
  async cancelar() {
    if (!this.sessionId) {
      console.warn('⚠️ No hay sesión activa para cancelar');
      return;
    }

    try {
      console.log('⏹️ Cancelando planificación:', this.sessionId);
      
      this.client.publish({
        destination: `/app/planificacion/cancelar/${this.sessionId}`,
        body: ''
      });

      console.log('✅ Comando de cancelación enviado');
      
    } catch (error) {
      console.error('❌ Error al cancelar planificación:', error);
      
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
    this.sessionId = null;
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
   * Verifica si está suscrito a una sesión
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
   * Obtiene el ID de sesión actual
   * @returns {string|null}
   */
  getSessionId() {
    return this.sessionId;
  }

  /**
   * 🆕 Verifica si los aeropuertos están cargados
   * @returns {boolean}
   */
  aeropuertosEstanCargados() {
    return this.aeropuertosCargados;
  }

  /**
   * 🆕 Obtiene el mapa de aeropuertos
   * @returns {Object}
   */
  getAeropuertosMap() {
    return this.aeropuertosMap;
  }

  /**
   * 🆕 Obtiene las coordenadas de un aeropuerto por código ICAO
   * @param {string} codigoICAO - Código ICAO del aeropuerto
   * @returns {Object|null} - {lat, lon, ciudad, pais} o null si no existe
   */
  getCoordenadas(codigoICAO) {
    return this.aeropuertosMap[codigoICAO] || null;
  }
}

// ==================== EXPORTAR ====================

// Instancia singleton (opcional)
export const planificacionService = new PlanificacionService();

// Exportar clase para crear múltiples instancias si es necesario
export default PlanificacionService;

// Exportar constantes
export { ConnectionState, PlanificacionStatus };
