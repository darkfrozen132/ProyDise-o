/**
 * ==================== HOOK DE REACT: useSimulation ====================
 * 
 * Hook personalizado para integrar fácilmente el servicio de simulación
 * en componentes React.
 * 
 * Características:
 * - Gestión automática del ciclo de vida (connect/disconnect)
 * - Estado reactivo de la simulación
 * - Limpieza automática al desmontar el componente
 * - API simple y declarativa
 * 
 * @author Senior Frontend Developer
 * @version 1.0
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import SimulationService from '../services/SimulationService';

/**
 * Hook para gestionar simulaciones en tiempo real
 * 
 * @param {Object} options - Opciones de configuración
 * @param {boolean} options.autoConnect - Conectar automáticamente al montar (default: true)
 * @param {number} options.windowMinutes - Ventana de tiempo por defecto (default: 60)
 * @returns {Object} Estado y métodos de la simulación
 */
export const useSimulation = ({ autoConnect = true, windowMinutes = 60 } = {}) => {
  // ==================== ESTADO ====================
  const [isConnected, setIsConnected] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [simulationId, setSimulationId] = useState(null);
  const [simulationData, setSimulationData] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  
  // Estado derivado de la simulación
  const [status, setStatus] = useState(null);
  const [progress, setProgress] = useState(0);
  const [routes, setRoutes] = useState([]);
  const [processedOrders, setProcessedOrders] = useState(0);
  const [totalOrders, setTotalOrders] = useState(0);
  const [currentFitness, setCurrentFitness] = useState(0);

  // Referencia al servicio (persiste entre renders)
  const serviceRef = useRef(null);

  // ==================== INICIALIZACIÓN ====================
  
  useEffect(() => {
    // Crear instancia del servicio
    serviceRef.current = new SimulationService();

    // Configurar callbacks
    serviceRef.current
      .onConnected(() => {
        console.log('🟢 Hook: Conectado');
        setIsConnected(true);
        setError(null);
      })
      .onDisconnected(() => {
        console.log('🔴 Hook: Desconectado');
        setIsConnected(false);
        setIsSubscribed(false);
      })
      .onMessage((data) => {
        console.log('📨 Hook: Datos recibidos:', data);
        setSimulationData(data);
        
        // Actualizar estado derivado
        if (data.status) setStatus(data.status);
        if (data.routes) setRoutes(data.routes);
        if (data.processedOrders !== undefined) setProcessedOrders(data.processedOrders);
        if (data.totalOrders !== undefined) {
          setTotalOrders(data.totalOrders);
          // Calcular progreso
          if (data.totalOrders > 0) {
            setProgress((data.processedOrders / data.totalOrders) * 100);
          }
        }
        if (data.currentFitness !== undefined) setCurrentFitness(data.currentFitness);
      })
      .onError((err) => {
        console.error('❌ Hook: Error:', err);
        setError(err.message || 'Error desconocido');
        setIsLoading(false);
      })
      .onSimulationComplete((finalData) => {
        console.log('🏁 Hook: Simulación completada:', finalData);
        setIsLoading(false);
      });

    // Conectar automáticamente si está habilitado
    if (autoConnect) {
      handleConnect();
    }

    // Cleanup al desmontar
    return () => {
      console.log('🧹 Hook: Limpiando recursos...');
      if (serviceRef.current) {
        serviceRef.current.disconnect();
      }
    };
  }, []); // Solo ejecutar una vez al montar

  // ==================== MÉTODOS ====================

  /**
   * Conectar al WebSocket
   */
  const handleConnect = useCallback(async () => {
    if (!serviceRef.current) return;
    
    try {
      setIsLoading(true);
      setError(null);
      await serviceRef.current.connect();
      setIsLoading(false);
    } catch (err) {
      console.error('❌ Error al conectar:', err);
      setError(err.message);
      setIsLoading(false);
    }
  }, []);

  /**
   * Iniciar simulación
   * @param {number} customWindowMinutes - Ventana de tiempo personalizada
   */
  const startSimulation = useCallback(async (customWindowMinutes) => {
    if (!serviceRef.current) return;
    
    try {
      setIsLoading(true);
      setError(null);
      
      const minutes = customWindowMinutes || windowMinutes;
      const id = await serviceRef.current.startSimulation(minutes);
      
      setSimulationId(id);
      setIsSubscribed(true);
      setIsLoading(false);
      
      return id;
    } catch (err) {
      console.error('❌ Error al iniciar simulación:', err);
      setError(err.message);
      setIsLoading(false);
      throw err;
    }
  }, [windowMinutes]);

  /**
   * Cancelar simulación actual
   */
  const cancelSimulation = useCallback(async () => {
    if (!serviceRef.current) return;
    
    try {
      setIsLoading(true);
      await serviceRef.current.cancelSimulation();
      setIsLoading(false);
    } catch (err) {
      console.error('❌ Error al cancelar simulación:', err);
      setError(err.message);
      setIsLoading(false);
      throw err;
    }
  }, []);

  /**
   * Desconectar del WebSocket
   */
  const disconnect = useCallback(() => {
    if (!serviceRef.current) return;
    
    serviceRef.current.disconnect();
    setIsConnected(false);
    setIsSubscribed(false);
    setSimulationId(null);
    setSimulationData(null);
    setStatus(null);
    setProgress(0);
    setRoutes([]);
  }, []);

  /**
   * Reiniciar hook (útil para múltiples simulaciones)
   */
  const reset = useCallback(() => {
    setSimulationData(null);
    setError(null);
    setStatus(null);
    setProgress(0);
    setRoutes([]);
    setProcessedOrders(0);
    setTotalOrders(0);
    setCurrentFitness(0);
  }, []);

  // ==================== RETORNO ====================

  return {
    // Estado de conexión
    isConnected,
    isSubscribed,
    isLoading,
    error,
    
    // Datos de simulación
    simulationId,
    simulationData,
    status,
    progress,
    routes,
    processedOrders,
    totalOrders,
    currentFitness,
    
    // Métodos
    connect: handleConnect,
    startSimulation,
    cancelSimulation,
    disconnect,
    reset,
    
    // Servicio raw (por si necesitas acceso directo)
    service: serviceRef.current
  };
};

export default useSimulation;
