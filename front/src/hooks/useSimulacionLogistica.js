/**
 * 🎣 HOOK PERSONALIZADO: useSimulacionLogistica
 * 
 * Gestiona el estado completo de una simulación logística en tiempo real:
 * - Conexión/desconexión automática al WebSocket
 * - Estado de la simulación (idle, connecting, running, completed, error)
 * - Progreso del algoritmo genético
 * - Rutas y métricas de la solución
 * - Logs de eventos
 * 
 * Uso:
 * const { iniciar, cancelar, estado, progreso, rutas, logs } = useSimulacionLogistica();
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { simulacionService } from '../services/SimulacionLogisticaService';

export const useSimulacionLogistica = () => {
  // Estados
  const [estado, setEstado] = useState('IDLE'); // IDLE | CONNECTING | RUNNING | PAUSED | COMPLETED | CANCELLED | ERROR
  const [estaConectado, setEstaConectado] = useState(false);
  const [sessionId, setSessionId] = useState(null);
  
  // Progreso del Algoritmo Genético
  const [progreso, setProgreso] = useState({
    generacionActual: 0,
    maxGeneraciones: 0,
    porcentaje: 0,
    mejorFitness: 0,
    fitnessPromedio: 0,
    pedidosProcesados: 0,
    pedidosTotales: 0,
    fechaSimulada: null,
    tiempoTranscurrido: 0,
  });

  // Solución actual
  const [solucion, setSolucion] = useState({
    rutas: [],
    metricas: {
      totalRutas: 0,
      totalVuelos: 0,
      costoTotal: 0,
      tiempoPromedioEntrega: 0,
    }
  });

  // Snapshot de simulación
  const [snapshot, setSnapshot] = useState({
    iteration: 0,
    simulatedTime: null,
    processedOrders: 0,
    totalOrders: 0,
  });

  // Logs
  const [logs, setLogs] = useState([]);
  const maxLogs = 50; // Máximo de logs a mantener

  // Error
  const [error, setError] = useState(null);

  // Refs para limpieza
  const inicializadoRef = useRef(false);

  /**
   * 📝 AGREGAR LOG
   */
  const agregarLog = useCallback((tipo, mensaje) => {
    const nuevoLog = {
      id: Date.now(),
      tipo, // 'info' | 'success' | 'warning' | 'error'
      mensaje,
      timestamp: new Date().toLocaleTimeString(),
    };

    setLogs(prevLogs => {
      const nuevosLogs = [nuevoLog, ...prevLogs];
      return nuevosLogs.slice(0, maxLogs);
    });
  }, []);

  /**
   * 🔌 CONECTAR AL WEBSOCKET
   */
  const conectar = useCallback(async () => {
    try {
      setEstado('CONNECTING');
      agregarLog('info', 'Conectando al servidor...');
      
      await simulacionService.connect();
      
      setEstaConectado(true);
      agregarLog('success', 'Conectado exitosamente');
    } catch (error) {
      setEstaConectado(false);
      setEstado('ERROR');
      agregarLog('error', `Error de conexión: ${error.message}`);
      setError(error);
    }
  }, [agregarLog]);

  /**
   * 🚀 INICIAR SIMULACIÓN
   */
  const iniciar = useCallback(async (fecha) => {
    try {
      setError(null);
      setEstado('RUNNING');
      agregarLog('info', `Iniciando simulación para fecha: ${fecha}`);

      // Asegurar que estamos conectados
      if (!estaConectado) {
        await conectar();
      }

      // Iniciar simulación
      const sid = await simulacionService.iniciarSimulacion(fecha);
      setSessionId(sid);
      
      agregarLog('success', `Simulación iniciada (Session: ${sid.substring(0, 8)}...)`);

    } catch (error) {
      setEstado('ERROR');
      agregarLog('error', `Error al iniciar: ${error.message}`);
      setError(error);
      throw error;
    }
  }, [estaConectado, conectar, agregarLog]);

  /**
   * ⛔ CANCELAR SIMULACIÓN
   */
  const cancelar = useCallback(async () => {
    try {
      agregarLog('warning', 'Cancelando simulación...');
      
      await simulacionService.cancelarSimulacion();
      
      setEstado('CANCELLED');
      agregarLog('warning', 'Simulación cancelada');
    } catch (error) {
      agregarLog('error', `Error al cancelar: ${error.message}`);
      setError(error);
    }
  }, [agregarLog]);

  /**
   * ⏸️ PAUSAR SIMULACIÓN
   */
  const pausar = useCallback(async () => {
    try {
      agregarLog('info', 'Pausando simulación...');
      
      await simulacionService.pausarSimulacion();
      
      setEstado('PAUSED');
      agregarLog('info', 'Simulación pausada');
    } catch (error) {
      agregarLog('error', `Error al pausar: ${error.message}`);
      setError(error);
    }
  }, [agregarLog]);

  /**
   * ▶️ REANUDAR SIMULACIÓN
   */
  const reanudar = useCallback(async () => {
    try {
      agregarLog('info', 'Reanudando simulación...');
      
      await simulacionService.reanudarSimulacion();
      
      setEstado('RUNNING');
      agregarLog('success', 'Simulación reanudada');
    } catch (error) {
      agregarLog('error', `Error al reanudar: ${error.message}`);
      setError(error);
    }
  }, [agregarLog]);

  /**
   * 🔄 REINICIAR
   */
  const reiniciar = useCallback(() => {
    setEstado('IDLE');
    setSessionId(null);
    setProgreso({
      generacionActual: 0,
      maxGeneraciones: 0,
      porcentaje: 0,
      mejorFitness: 0,
      fitnessPromedio: 0,
      pedidosProcesados: 0,
      pedidosTotales: 0,
      fechaSimulada: null,
      tiempoTranscurrido: 0,
    });
    setSolucion({
      rutas: [],
      metricas: {
        totalRutas: 0,
        totalVuelos: 0,
        costoTotal: 0,
        tiempoPromedioEntrega: 0,
      }
    });
    setSnapshot({
      iteration: 0,
      simulatedTime: null,
      processedOrders: 0,
      totalOrders: 0,
    });
    setError(null);
    agregarLog('info', 'Sistema reiniciado');
  }, [agregarLog]);

  /**
   * 🧬 MANEJAR PROGRESO DEL AG
   */
  const manejarProgresoAG = useCallback((data) => {
    setProgreso({
      generacionActual: data.generacion,
      maxGeneraciones: data.maxGeneraciones,
      porcentaje: data.progreso,
      mejorFitness: data.mejorFitness,
      fitnessPromedio: data.fitnessPromedio || 0,
      pedidosProcesados: data.pedidosProcesados,
      pedidosTotales: data.pedidosTotales,
      fechaSimulada: data.fechaSimulada,
      tiempoTranscurrido: 0, // Se puede calcular si el backend envía timestamps
    });

    if (data.solucion) {
      setSolucion({
        rutas: data.solucion.rutas || [],
        metricas: data.solucion.metricas || {
          totalRutas: 0,
          totalVuelos: 0,
          costoTotal: 0,
          tiempoPromedioEntrega: 0,
        }
      });
    }

    agregarLog(
      'info',
      `Generación ${data.generacion}/${data.maxGeneraciones} - Fitness: ${data.mejorFitness.toFixed(2)}`
    );
  }, [agregarLog]);

  /**
   * 📊 MANEJAR SNAPSHOT
   */
  const manejarSnapshot = useCallback((data) => {
    setSnapshot({
      iteration: data.iteration,
      simulatedTime: data.simulatedTime,
      processedOrders: data.processedOrders,
      totalOrders: data.totalOrders,
    });

    // Actualizar progreso si no viene del AG
    if (data.totalOrders > 0) {
      const porcentaje = (data.processedOrders / data.totalOrders) * 100;
      setProgreso(prev => ({
        ...prev,
        porcentaje,
        pedidosProcesados: data.processedOrders,
        pedidosTotales: data.totalOrders,
      }));
    }
  }, []);

  /**
   * ✅ MANEJAR COMPLETADO
   */
  const manejarCompletado = useCallback((data) => {
    setEstado(data.status === 'CANCELLED' ? 'CANCELLED' : 'COMPLETED');
    
    if (data.status === 'COMPLETED') {
      agregarLog('success', '🎉 Simulación completada exitosamente');
      
      // Actualizar con datos finales
      if (data.solution) {
        setSolucion(prev => ({
          ...prev,
          rutas: data.solution.routes || prev.rutas,
          metricas: data.solution.metadata || prev.metricas,
        }));
      }
    } else {
      agregarLog('warning', 'Simulación cancelada por el usuario');
    }
  }, [agregarLog]);

  /**
   * ❌ MANEJAR ERROR
   */
  const manejarError = useCallback((error) => {
    setEstado('ERROR');
    setError(error);
    agregarLog('error', `Error: ${error.message}`);
  }, [agregarLog]);

  /**
   * 🎯 CONFIGURAR CALLBACKS DEL SERVICIO
   */
  useEffect(() => {
    if (inicializadoRef.current) return;
    inicializadoRef.current = true;

    // Registrar callbacks
    simulacionService
      .onProgresoAG(manejarProgresoAG)
      .onSnapshot(manejarSnapshot)
      .onCompleted(manejarCompletado)
      .onError(manejarError)
      .onConnected(() => {
        setEstaConectado(true);
        setEstado('IDLE');
      })
      .onDisconnected(() => {
        setEstaConectado(false);
        if (estado === 'RUNNING') {
          agregarLog('warning', 'Desconectado del servidor');
        }
      });

    // Conectar al montar
    conectar();

    // Cleanup al desmontar
    return () => {
      simulacionService.disconnect();
    };
  }, []); // Solo ejecutar una vez

  /**
   * 📊 DATOS CALCULADOS
   */
  const stats = {
    // Indicadores generales
    estaActivo: estado === 'RUNNING' || estado === 'PAUSED',
    puedeIniciar: estado === 'IDLE' || estado === 'COMPLETED' || estado === 'CANCELLED' || estado === 'ERROR',
    puedeCancelar: estado === 'RUNNING' || estado === 'PAUSED',
    puedePausar: estado === 'RUNNING',
    puedeReanudar: estado === 'PAUSED',
    
    // Progreso
    porcentajeCompleto: progreso.porcentaje,
    generaciones: `${progreso.generacionActual}/${progreso.maxGeneraciones}`,
    pedidos: `${progreso.pedidosProcesados}/${progreso.pedidosTotales}`,
    
    // Métricas
    totalRutas: solucion.metricas.totalRutas,
    totalVuelos: solucion.metricas.totalVuelos,
    costoTotal: solucion.metricas.costoTotal,
  };

  return {
    // Estado
    estado,
    estaConectado,
    sessionId,
    error,
    
    // Datos
    progreso,
    solucion,
    snapshot,
    logs,
    stats,
    
    // Acciones
    iniciar,
    cancelar,
    pausar,
    reanudar,
    reiniciar,
    conectar,
  };
};
