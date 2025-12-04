/**
 * Constantes de configuración para el Simulador Semanal
 */

// ==================== CONFIGURACIÓN DE VELOCIDAD ====================
// Factor K: Cuántos ms simulados pasan por cada ms real
// Ejemplo: K=500 significa que 1 segundo real = 500 segundos simulados (8.33 min)
export const DESIRED_TIME_SCALE = 450; // Factor K de aceleración del tiempo

// Intervalos de tiempo
export const REAL_TICK_MS = 1000; // Intervalo del reloj real en ms (1 segundo) - NO USADO
export const TICK_REAL_MS = 250; // Intervalo de actualización del reloj local (4 FPS)

// ==================== SISTEMA DE BUFFER INICIAL ====================
// Dar ventaja al backend para generar rutas antes de empezar animación
export const BUFFER_DELAY_MS = 20000; // 20 segundos de buffer inicial
export const USE_ADAPTIVE_SPEED = false; // false = velocidad CONSTANTE después del buffer

// ==================== SISTEMA ADAPTATIVO (OPCIONAL) ====================
// Solo se usa si USE_ADAPTIVE_SPEED = true
export const K_BASE = 450; // Factor K base
export const UMBRAL_COLA_BAJA = 3; // Si cola < 3, ralentizar
export const FACTOR_RALENTIZADO = 0.25; // K se reduce a 25% cuando cola baja

// Configuración del mapa
export const MAP_CENTER = [20, 0]; // Centro del mapa (lat, lng)
export const MAP_ZOOM = 2; // Zoom inicial del mapa

// Colores de aviones
export const AIRCRAFT_COLORS = {
	COMPLETED: '#3b82f6', // Azul - Completado
	DELAYED: '#ef4444',   // Rojo - Retrasado
	ARRIVING: '#22c55e',  // Verde claro - Casi llegando (>75%)
	IN_ROUTE: '#3b82f6',  // Azul - En ruta (>50%)
	STARTING: '#60a5fa',  // Azul claro - Iniciando (>25%)
	DEFAULT: '#3b82f6',   // Azul por defecto
	NORMAL: '#3b82f6',    // Azul - En curso normal
	WEBSOCKET: '#3b82f6', // Azul - Vuelos del WebSocket
	GROUNDED: '#9ca3af',  // Gris - En tierra
};

// Estados de simulación
export const SIMULATION_STATES = {
	DISCONNECTED: 'disconnected',
	CONNECTING: 'connecting',
	CONNECTED: 'connected',
	RUNNING: 'running',
	COMPLETED: 'completed',
	ERROR: 'error',
	IDLE: 'idle',
	WAITING: 'waiting',
};

// URLs del backend
export const BACKEND_URL = 'http://localhost:8000';
export const WEBSOCKET_URL = `${BACKEND_URL}/ws`; // WebSocket STOMP endpoint
export const WS_ENDPOINT = `${BACKEND_URL}/planificacion`;
export const API_SIMULATIONS = `${BACKEND_URL}/api/simulations`;
export const API_START_SIMULATION = `${API_SIMULATIONS}/start`;

// Configuración de animación
export const ANIMATION_DURATION_MS = 1000; // Duración de animación de marcadores
export const FLIGHT_ALTITUDE_DEFAULT = 35000; // Altitud por defecto en pies
export const FLIGHT_SPEED_DEFAULT = 850; // Velocidad por defecto en km/h

// Aeropuertos HUBS/SEDES a excluir en visualización especial
export const HUBS_SEDES = ['SPIM', 'EBCI', 'UBBB']; // Lima, Bruselas, Bakú
