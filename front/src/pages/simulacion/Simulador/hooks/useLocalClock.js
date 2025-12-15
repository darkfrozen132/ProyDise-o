/**
 * Hook useLocalClock
 * Maneja el reloj local de simulación con interpolación temporal
 * 
 * Este hook implementa el sistema híbrido de tiempo:
 * - El backend envía actualizaciones de tiempo cada cierto intervalo
 * - El frontend interpola suavemente entre actualizaciones
 * - El factor K controla la velocidad de simulación
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { 
  DESIRED_TIME_SCALE, 
  REAL_TICK_MS, 
  TICK_REAL_MS,
  BUFFER_DELAY_MS 
} from '../constants';

/**
 * Hook para manejar el reloj local de simulación
 * @param {Object} options - Opciones de configuración
 * @param {number} options.factorK - Factor de escala de tiempo (default: DESIRED_TIME_SCALE)
 * @param {boolean} options.autoStart - Si el reloj debe iniciar automáticamente
 * @returns {Object} - Estado y funciones del reloj
 */
export function useLocalClock(options = {}) {
  const { 
    factorK = DESIRED_TIME_SCALE,
    autoStart = false 
  } = options;

  // Estado del tiempo
  const [tiempoSimuladoBackend, setTiempoSimuladoBackend] = useState(null);
  const [ultimaActualizacionReal, setUltimaActualizacionReal] = useState(null);
  const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
  const [tiempoSimulacionActual, setTiempoSimulacionActual] = useState(null);
  const [clockActivo, setClockActivo] = useState(autoStart);
  
  // Estado del buffer
  const [bufferActivo, setBufferActivo] = useState(false);
  const [primerMensajeRecibido, setPrimerMensajeRecibido] = useState(false);
  
  // Referencias para evitar stale closures
  const tiempoSimuladoBackendRef = useRef(null);
  const ultimaActualizacionRealRef = useRef(null);
  const clockActivoRef = useRef(autoStart);
  const factorKRef = useRef(factorK);
  
  // Referencia del intervalo
  const intervalRef = useRef(null);
  
  /**
   * Calcula el tiempo simulado actual basándose en interpolación
   * tiempoSimulado = tiempoBackend + (tiempoRealTranscurrido * factorK)
   */
  const calcularTiempoSimulado = useCallback(() => {
    if (tiempoSimuladoBackendRef.current === null || ultimaActualizacionRealRef.current === null) {
      return null;
    }
    
    const tiempoRealTranscurrido = Date.now() - ultimaActualizacionRealRef.current;
    const tiempoSimuladoTranscurrido = tiempoRealTranscurrido * factorKRef.current;
    
    return tiempoSimuladoBackendRef.current + tiempoSimuladoTranscurrido;
  }, []);

  /**
   * Tick del reloj - actualiza el estado cada TICK_REAL_MS
   */
  const tick = useCallback(() => {
    if (!clockActivoRef.current) return;
    
    const tiempoActual = calcularTiempoSimulado();
    if (tiempoActual !== null) {
      setTiempoMovimiento(prev => prev + TICK_REAL_MS);
      
      // Actualizar el tiempo mostrado en UI
      setTiempoSimulacionActual(new Date(tiempoActual).toISOString());
    }
  }, [calcularTiempoSimulado]);

  /**
   * Iniciar el reloj local
   */
  const iniciarClock = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    
    clockActivoRef.current = true;
    setClockActivo(true);
    
    intervalRef.current = setInterval(tick, TICK_REAL_MS);
    console.log(`[useLocalClock] Reloj iniciado con factor K=${factorKRef.current}`);
  }, [tick]);

  /**
   * Detener el reloj local
   */
  const detenerClock = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    
    clockActivoRef.current = false;
    setClockActivo(false);
    console.log('[useLocalClock] Reloj detenido');
  }, []);

  /**
   * Reiniciar el reloj local
   */
  const reiniciarClock = useCallback(() => {
    setTiempoSimuladoBackend(null);
    setUltimaActualizacionReal(null);
    setTiempoMovimiento(0);
    setTiempoSimulacionActual(null);
    setPrimerMensajeRecibido(false);
    setBufferActivo(false);
    
    tiempoSimuladoBackendRef.current = null;
    ultimaActualizacionRealRef.current = null;
    
    detenerClock();
    console.log('[useLocalClock] Reloj reiniciado');
  }, [detenerClock]);

  /**
   * Actualizar tiempo desde el backend
   * @param {number|string} tiempoBackend - Timestamp o string ISO del backend
   */
  const actualizarTiempoBackend = useCallback((tiempoBackend) => {
    const timestamp = typeof tiempoBackend === 'string' 
      ? new Date(tiempoBackend).getTime()
      : tiempoBackend;
    
    if (isNaN(timestamp)) {
      console.warn('[useLocalClock] Tiempo de backend invalido:', tiempoBackend);
      return;
    }
    
    // Actualizar refs y estado
    tiempoSimuladoBackendRef.current = timestamp;
    ultimaActualizacionRealRef.current = Date.now();
    
    setTiempoSimuladoBackend(timestamp);
    setUltimaActualizacionReal(Date.now());
    setTiempoMovimiento(0);
    setTiempoSimulacionActual(new Date(timestamp).toISOString());
    
    // Manejar el buffer de 15 segundos para el primer mensaje
    if (!primerMensajeRecibido) {
      setPrimerMensajeRecibido(true);
      setBufferActivo(true);
      
      console.log(`[useLocalClock] Primer mensaje recibido. Activando buffer de ${BUFFER_DELAY_MS/1000}s...`);
      
      // Después del buffer, iniciar el reloj
      setTimeout(() => {
        setBufferActivo(false);
        console.log('[useLocalClock] Buffer completado. Iniciando reloj...');
        iniciarClock();
      }, BUFFER_DELAY_MS);
    }
    
    console.log(`[useLocalClock] Tiempo backend actualizado: ${new Date(timestamp).toISOString()}`);
  }, [primerMensajeRecibido, iniciarClock]);

  /**
   * Inicializar tiempo desde la fecha más temprana de vuelos
   * @param {Array} vuelos - Array de vuelos con fechaInicial
   */
  const autoInicializarDesdeVuelos = useCallback((vuelos) => {
    if (tiempoSimuladoBackendRef.current !== null) {
      console.log('[useLocalClock] Tiempo ya inicializado, ignorando auto-inicializacion');
      return;
    }
    
    const vuelosConFecha = vuelos.filter(v => v.fechaInicial);
    if (vuelosConFecha.length === 0) {
      console.warn('[useLocalClock] No hay vuelos con fecha para auto-inicializar');
      return;
    }
    
    const fechaMasTemprana = Math.min(
      ...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime())
    );
    
    console.log(`[useLocalClock] Auto-inicializando desde vuelos: ${new Date(fechaMasTemprana).toISOString()}`);
    actualizarTiempoBackend(fechaMasTemprana);
  }, [actualizarTiempoBackend]);

  /**
   * Actualizar factor K en tiempo de ejecución
   */
  const actualizarFactorK = useCallback((nuevoK) => {
    factorKRef.current = nuevoK;
    console.log(`[useLocalClock] Factor K actualizado a: ${nuevoK}`);
  }, []);

  // Limpiar intervalo al desmontar
  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  // Sync de ref cuando cambia factorK
  useEffect(() => {
    factorKRef.current = factorK;
  }, [factorK]);

  return {
    // Estado del tiempo
    tiempoSimuladoBackend,
    ultimaActualizacionReal,
    tiempoMovimiento,
    tiempoSimulacionActual,
    clockActivo,
    bufferActivo,
    primerMensajeRecibido,
    
    // Referencias (para uso avanzado)
    tiempoSimuladoBackendRef,
    
    // Funciones
    calcularTiempoSimulado,
    iniciarClock,
    detenerClock,
    reiniciarClock,
    actualizarTiempoBackend,
    autoInicializarDesdeVuelos,
    actualizarFactorK,
    
    // Setters directos (para compatibilidad)
    setTiempoSimuladoBackend,
    setUltimaActualizacionReal,
    setTiempoMovimiento,
    setTiempoSimulacionActual
  };
}

export default useLocalClock;
