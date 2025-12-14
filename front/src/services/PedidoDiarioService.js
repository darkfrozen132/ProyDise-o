/**
 * ==================== PEDIDO DIARIO SERVICE ====================
 * 
 * Servicio para gestionar pedidos diarios
 * Conecta con los endpoints de PedidoDiarioController.java
 * 
 * Base URL: /api/pedidos-diarios
 * 
 * @author Frontend Developer
 * @version 1.0
 */

import axios from 'axios';
import { API_BASE_URL } from '../config/api';

// Cliente axios configurado
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  }
});

const PEDIDOS_ENDPOINT = '/api/pedidos-diarios';

/**
 * Clase PedidoDiario - Representa un pedido diario
 */
export class PedidoDiario {
  constructor(data = {}) {
    this.id = data.id || null;
    this.clienteId = data.clienteId || '';
    this.aeropuertoDestinoId = data.aeropuertoDestinoId || '';
    this.cantidadProductos = data.cantidadProductos || 0;
    this.dia = data.dia || 1;
    this.mes = data.mes || 1;
    this.anio = data.anio || new Date().getFullYear();
    this.hora = data.hora || 0;
    this.minuto = data.minuto || 0;
  }

  /**
   * Convierte a objeto JSON para enviar al backend
   */
  toRequest() {
    return {
      clienteId: this.clienteId,
      aeropuertoDestinoId: this.aeropuertoDestinoId,
      cantidadProductos: this.cantidadProductos,
      dia: this.dia,
      mes: this.mes,
      anio: this.anio,
      hora: this.hora,
      minuto: this.minuto
    };
  }

  /**
   * Crea una instancia desde la respuesta del backend
   */
  static fromResponse(data) {
    return new PedidoDiario(data);
  }
}

/**
 * Servicio para gestionar pedidos diarios
 */
const PedidoDiarioService = {

  /**
   * Crea un nuevo pedido diario
   * POST /api/pedidos-diarios
   * 
   * @param {Object} pedidoData - Datos del pedido
   * @param {string} pedidoData.clienteId - ID del cliente
   * @param {string} pedidoData.aeropuertoDestinoId - ID del aeropuerto destino
   * @param {number} pedidoData.cantidadProductos - Cantidad de productos
   * @param {number} pedidoData.dia - Día del pedido
   * @param {number} pedidoData.mes - Mes del pedido
   * @param {number} pedidoData.anio - Año del pedido
   * @param {number} pedidoData.hora - Hora del pedido
   * @param {number} pedidoData.minuto - Minuto del pedido
   * @returns {Promise<Object>} Respuesta con mensaje y pedido creado
   */
  async crearPedido(pedidoData) {
    try {
      console.log('📦 Creando pedido diario:', pedidoData);
      
      const pedido = pedidoData instanceof PedidoDiario 
        ? pedidoData.toRequest() 
        : pedidoData;

      const response = await apiClient.post(PEDIDOS_ENDPOINT, pedido);
      
      console.log('✅ Pedido creado exitosamente:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error al crear pedido diario:', error);
      throw this._handleError(error);
    }
  },

  /**
   * Obtiene todos los pedidos diarios
   * GET /api/pedidos-diarios
   * 
   * @returns {Promise<Array<PedidoDiario>>} Lista de pedidos
   */
  async obtenerTodos() {
    try {
      console.log('📋 Obteniendo todos los pedidos diarios...');
      const response = await apiClient.get(PEDIDOS_ENDPOINT);
      
      const pedidos = response.data.map(p => PedidoDiario.fromResponse(p));
      console.log(`✅ Se obtuvieron ${pedidos.length} pedidos`);
      return pedidos;
    } catch (error) {
      console.error('❌ Error al obtener pedidos:', error);
      throw this._handleError(error);
    }
  },

  /**
   * Obtiene un pedido por su ID
   * GET /api/pedidos-diarios/{id}
   * 
   * @param {number} id - ID del pedido
   * @returns {Promise<PedidoDiario>} Pedido encontrado
   */
  async obtenerPorId(id) {
    try {
      console.log(`🔍 Buscando pedido con ID: ${id}`);
      const response = await apiClient.get(`${PEDIDOS_ENDPOINT}/${id}`);
      
      const pedido = PedidoDiario.fromResponse(response.data);
      console.log('✅ Pedido encontrado:', pedido);
      return pedido;
    } catch (error) {
      console.error(`❌ Error al obtener pedido ${id}:`, error);
      throw this._handleError(error);
    }
  },

  /**
   * Busca pedidos por aeropuerto destino
   * GET /api/pedidos-diarios/destino/{aeropuertoId}
   * 
   * @param {string} aeropuertoId - ID del aeropuerto destino
   * @returns {Promise<Array<PedidoDiario>>} Lista de pedidos
   */
  async buscarPorDestino(aeropuertoId) {
    try {
      console.log(`🛬 Buscando pedidos con destino: ${aeropuertoId}`);
      const response = await apiClient.get(`${PEDIDOS_ENDPOINT}/destino/${aeropuertoId}`);
      
      const pedidos = response.data.map(p => PedidoDiario.fromResponse(p));
      console.log(`✅ Se encontraron ${pedidos.length} pedidos con destino ${aeropuertoId}`);
      return pedidos;
    } catch (error) {
      console.error(`❌ Error al buscar pedidos por destino ${aeropuertoId}:`, error);
      throw this._handleError(error);
    }
  },

  /**
   * Busca pedidos por cliente
   * GET /api/pedidos-diarios/cliente/{clienteId}
   * 
   * @param {string} clienteId - ID del cliente
   * @returns {Promise<Array<PedidoDiario>>} Lista de pedidos del cliente
   */
  async buscarPorCliente(clienteId) {
    try {
      console.log(`👤 Buscando pedidos del cliente: ${clienteId}`);
      const response = await apiClient.get(`${PEDIDOS_ENDPOINT}/cliente/${clienteId}`);
      
      const pedidos = response.data.map(p => PedidoDiario.fromResponse(p));
      console.log(`✅ Se encontraron ${pedidos.length} pedidos del cliente ${clienteId}`);
      return pedidos;
    } catch (error) {
      console.error(`❌ Error al buscar pedidos del cliente ${clienteId}:`, error);
      throw this._handleError(error);
    }
  },

  /**
   * Busca pedidos por día
   * GET /api/pedidos-diarios/dia/{dia}
   * 
   * @param {number} dia - Día a buscar
   * @returns {Promise<Array<PedidoDiario>>} Lista de pedidos del día
   */
  async buscarPorDia(dia) {
    try {
      console.log(`📅 Buscando pedidos del día: ${dia}`);
      const response = await apiClient.get(`${PEDIDOS_ENDPOINT}/dia/${dia}`);
      
      const pedidos = response.data.map(p => PedidoDiario.fromResponse(p));
      console.log(`✅ Se encontraron ${pedidos.length} pedidos del día ${dia}`);
      return pedidos;
    } catch (error) {
      console.error(`❌ Error al buscar pedidos del día ${dia}:`, error);
      throw this._handleError(error);
    }
  },

  /**
   * Elimina un pedido por su ID
   * DELETE /api/pedidos-diarios/{id}
   * 
   * @param {number} id - ID del pedido a eliminar
   * @returns {Promise<Object>} Respuesta con mensaje de confirmación
   */
  async eliminarPedido(id) {
    try {
      console.log(`🗑️ Eliminando pedido con ID: ${id}`);
      const response = await apiClient.delete(`${PEDIDOS_ENDPOINT}/${id}`);
      
      console.log('✅ Pedido eliminado exitosamente:', response.data);
      return response.data;
    } catch (error) {
      console.error(`❌ Error al eliminar pedido ${id}:`, error);
      throw this._handleError(error);
    }
  },

  /**
   * Elimina todos los pedidos diarios
   * DELETE /api/pedidos-diarios/limpiar
   * 
   * @returns {Promise<Object>} Respuesta con mensaje de confirmación
   */
  async limpiarPedidos() {
    try {
      console.log('🧹 Eliminando todos los pedidos diarios...');
      const response = await apiClient.delete(`${PEDIDOS_ENDPOINT}/limpiar`);
      
      console.log('✅ Todos los pedidos han sido eliminados:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error al limpiar pedidos:', error);
      throw this._handleError(error);
    }
  },

  /**
   * Obtiene estadísticas de pedidos diarios
   * GET /api/pedidos-diarios/estadisticas
   * 
   * @returns {Promise<Object>} Estadísticas de pedidos
   */
  async obtenerEstadisticas() {
    try {
      console.log('📊 Obteniendo estadísticas de pedidos...');
      const response = await apiClient.get(`${PEDIDOS_ENDPOINT}/estadisticas`);
      
      console.log('✅ Estadísticas obtenidas:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error al obtener estadísticas:', error);
      throw this._handleError(error);
    }
  },

  /**
   * Maneja errores de las peticiones HTTP
   * @private
   */
  _handleError(error) {
    if (error.response) {
      // El servidor respondió con un código de error
      const { status, data } = error.response;
      return {
        status,
        error: data.error || 'Error del servidor',
        detalle: data.detalle || error.message
      };
    } else if (error.request) {
      // La petición fue hecha pero no se recibió respuesta
      return {
        status: 0,
        error: 'Error de conexión',
        detalle: 'No se pudo conectar con el servidor'
      };
    } else {
      // Error al configurar la petición
      return {
        status: -1,
        error: 'Error de configuración',
        detalle: error.message
      };
    }
  }
};

export default PedidoDiarioService;
