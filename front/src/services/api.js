/**
 * ==================== API SERVICE ====================
 * 
 * Servicio para comunicación con el backend REST
 * Endpoints básicos para aeropuertos, pedidos, etc.
 * 
 * @author Frontend Developer
 * @version 1.0
 */

import axios from 'axios';

// Configuración base
const REST_BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8000';

/**
 * Cliente axios configurado
 */
const apiClient = axios.create({
  baseURL: REST_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  }
});

// ==================== AEROPUERTOS ====================

/**
 * Obtiene la lista de todos los aeropuertos
 * @returns {Promise<Array>} Lista de aeropuertos
 */
export const cargarAeropuertos = async () => {
  try {
    console.log('📍 Cargando aeropuertos desde:', `${REST_BASE_URL}/api/aeropuertos`);
    const response = await apiClient.get('/api/aeropuertos');
    return response.data;
  } catch (error) {
    console.error('❌ Error al cargar aeropuertos:', error);
    
    // Si falla, devolver datos mock para desarrollo
    if (process.env.NODE_ENV === 'development') {
      console.warn('⚠️ Usando datos mock de aeropuertos');
      return getMockAeropuertos();
    }
    
    throw error;
  }
};

/**
 * Obtiene un aeropuerto específico por código ICAO
 * @param {string} codigoICAO - Código ICAO del aeropuerto
 * @returns {Promise<Object>} Datos del aeropuerto
 */
export const obtenerAeropuerto = async (codigoICAO) => {
  try {
    const response = await apiClient.get(`/api/aeropuertos/${codigoICAO}`);
    return response.data;
  } catch (error) {
    console.error(`❌ Error al obtener aeropuerto ${codigoICAO}:`, error);
    throw error;
  }
};

// ==================== PEDIDOS ====================

/**
 * Obtiene la lista de pedidos
 * @returns {Promise<Array>} Lista de pedidos
 */
export const cargarPedidos = async () => {
  try {
    const response = await apiClient.get('/api/pedidos');
    return response.data;
  } catch (error) {
    console.error('❌ Error al cargar pedidos:', error);
    throw error;
  }
};

/**
 * Obtiene pedidos por fecha
 * @param {string} fecha - Fecha en formato YYYY-MM-DD
 * @returns {Promise<Array>} Lista de pedidos
 */
export const obtenerPedidosPorFecha = async (fecha) => {
  try {
    const response = await apiClient.get('/api/pedidos/fecha', {
      params: { fecha }
    });
    return response.data;
  } catch (error) {
    console.error(`❌ Error al obtener pedidos para fecha ${fecha}:`, error);
    throw error;
  }
};

// ==================== PLANIFICACIÓN ====================

/**
 * Ejecuta planificación simple (endpoint REST)
 * @param {Object} params - Parámetros de planificación
 * @returns {Promise<Object>} Resultado de la planificación
 */
export const ejecutarPlanificacionSimple = async (params = {}) => {
  try {
    const {
      fecha = new Date().toISOString().split('T')[0],
      factorK = 5,
      tamanioPoblacion = 20,
      maxGeneraciones = 20,
      limiteGeneracionesSinMejora = 10
    } = params;

    console.log('🚀 Ejecutando planificación simple:', params);

    const response = await apiClient.post(
      '/api/planificacion/ejecutar-simple',
      null,
      {
        params: {
          fecha,
          factorK,
          tamanioPoblacion,
          maxGeneraciones,
          limiteGeneracionesSinMejora
        }
      }
    );

    return response.data;
  } catch (error) {
    console.error('❌ Error al ejecutar planificación:', error);
    throw error;
  }
};

// ==================== DATOS MOCK ====================

/**
 * Datos mock de aeropuertos para desarrollo
 * @returns {Array} Lista de aeropuertos mock
 */
const getMockAeropuertos = () => {
  return [
    {
      codigoICAO: 'KJFK',
      nombre: 'John F. Kennedy International Airport',
      ciudad: 'New York',
      pais: 'United States',
      latitud: 40.6413,
      longitud: -73.7781
    },
    {
      codigoICAO: 'EGLL',
      nombre: 'London Heathrow Airport',
      ciudad: 'London',
      pais: 'United Kingdom',
      latitud: 51.4700,
      longitud: -0.4543
    },
    {
      codigoICAO: 'RJTT',
      nombre: 'Tokyo Haneda Airport',
      ciudad: 'Tokyo',
      pais: 'Japan',
      latitud: 35.5494,
      longitud: 139.7798
    },
    {
      codigoICAO: 'YSSY',
      nombre: 'Sydney Kingsford Smith Airport',
      ciudad: 'Sydney',
      pais: 'Australia',
      latitud: -33.9399,
      longitud: 151.1753
    },
    {
      codigoICAO: 'EDDF',
      nombre: 'Frankfurt Airport',
      ciudad: 'Frankfurt',
      pais: 'Germany',
      latitud: 50.0379,
      longitud: 8.5622
    },
    {
      codigoICAO: 'LFPG',
      nombre: 'Charles de Gaulle Airport',
      ciudad: 'Paris',
      pais: 'France',
      latitud: 49.0097,
      longitud: 2.5479
    },
    {
      codigoICAO: 'ZBAA',
      nombre: 'Beijing Capital International Airport',
      ciudad: 'Beijing',
      pais: 'China',
      latitud: 40.0799,
      longitud: 116.6031
    },
    {
      codigoICAO: 'WSSS',
      nombre: 'Singapore Changi Airport',
      ciudad: 'Singapore',
      pais: 'Singapore',
      latitud: 1.3644,
      longitud: 103.9915
    },
    {
      codigoICAO: 'OMDB',
      nombre: 'Dubai International Airport',
      ciudad: 'Dubai',
      pais: 'United Arab Emirates',
      latitud: 25.2532,
      longitud: 55.3657
    },
    {
      codigoICAO: 'KLAX',
      nombre: 'Los Angeles International Airport',
      ciudad: 'Los Angeles',
      pais: 'United States',
      latitud: 33.9416,
      longitud: -118.4085
    }
  ];
};

// ==================== INTERCEPTORS ====================

// Interceptor de respuesta para manejo global de errores
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Error de servidor (4xx, 5xx)
      console.error('❌ Error de servidor:', {
        status: error.response.status,
        mensaje: error.response.data?.mensaje || error.response.statusText,
        url: error.config?.url
      });
    } else if (error.request) {
      // Error de red (no hay respuesta)
      console.error('❌ Error de red:', {
        mensaje: 'No se pudo conectar con el servidor',
        url: error.config?.url
      });
    } else {
      // Error de configuración
      console.error('❌ Error de configuración:', error.message);
    }
    
    return Promise.reject(error);
  }
);

// ==================== EXPORTAR ====================

export default apiClient;
