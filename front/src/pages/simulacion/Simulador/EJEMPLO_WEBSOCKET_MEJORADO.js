/**
 * ==================== EJEMPLO: Uso del Hook usePlanificacionWebSocket ====================
 * 
 * Este archivo muestra cómo integrar el nuevo hook mejorado en SimuladorSemanal.
 * 
 * MEJORAS IMPLEMENTADAS:
 * 1. ✅ Reconexión automática con backoff exponencial
 * 2. ✅ Indicador visual de estado de conexión
 * 3. ✅ Medición de latencia y calidad de conexión
 * 4. ✅ Heartbeat para detectar desconexiones silenciosas
 * 5. ✅ Timeout de conexión configurable
 * 6. ✅ Estado unificado y limpio (sin refs duplicados)
 * 7. ✅ Componente de indicador reutilizable
 * 
 * CÓMO MIGRAR:
 * 
 * 1. Reemplazar las importaciones:
 * 
 *    ANTES:
 *    import { conectarWebSocketPlanificacion } from '../../../config/websocket';
 * 
 *    DESPUÉS:
 *    import usePlanificacionWebSocket from '../../../hooks/usePlanificacionWebSocket';
 *    import WebSocketStatusIndicator from '../../../components/ui/WebSocketStatusIndicator';
 * 
 * 
 * 2. Reemplazar el estado y refs del WebSocket:
 * 
 *    ANTES:
 *    const [wsPlanificacion, setWsPlanificacion] = useState(null);
 *    const [wsConectado, setWsConectado] = useState(false);
 *    const [iteracionesPlanificacion, setIteracionesPlanificacion] = useState([]);
 *    const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle');
 *    const wsPlanificacionRef = useRef(null);
 * 
 *    DESPUÉS:
 *    const {
 *      isConnected,
 *      connectionState,
 *      connectionQuality,
 *      latency,
 *      reconnectAttempt,
 *      iteraciones,
 *      estadoPlanificacion,
 *      connect,
 *      disconnect,
 *      iniciarPlanificacion,
 *      limpiarIteraciones,
 *      onMessage,
 *    } = usePlanificacionWebSocket();
 * 
 * 
 * 3. Agregar el indicador visual en el JSX:
 * 
 *    <WebSocketStatusIndicator
 *      connectionState={connectionState}
 *      connectionQuality={connectionQuality}
 *      latency={latency}
 *      reconnectAttempt={reconnectAttempt}
 *    />
 * 
 * 
 * 4. Reemplazar handleConectarWsPlanificacion:
 * 
 *    ANTES:
 *    const handleConectarWsPlanificacion = () => { ... código largo ... };
 * 
 *    DESPUÉS:
 *    // El hook auto-conecta, pero puedes reconectar manualmente:
 *    const handleReconectar = () => connect();
 * 
 * 
 * 5. Reemplazar handleIniciarPlanificacion:
 * 
 *    ANTES:
 *    wsPlanificacionRef.current.iniciarPlanificacion(fecha, factorK, opciones);
 * 
 *    DESPUÉS:
 *    iniciarPlanificacion(fecha, factorK, opciones);
 * 
 * 
 * 6. Registrar callback para procesar vuelos:
 * 
 *    useEffect(() => {
 *      onMessage((data) => {
 *        if (data.solucion?.vuelos) {
 *          // Procesar vuelos igual que antes
 *          const vuelosParaMapa = data.solucion.vuelos
 *            .map(convertirVueloPlanificacionAMapa)
 *            .filter(v => v !== null);
 *          setFlights(prev => [...prev, ...vuelosParaMapa]);
 *        }
 *        
 *        // Actualizar tiempo simulado
 *        if (data.datos?.tiempoSimulacionActual) {
 *          setTiempoSimulado(new Date(data.datos.tiempoSimulacionActual).getTime());
 *        }
 *      });
 *    }, [onMessage]);
 * 
 */

import React, { useEffect } from 'react';
import usePlanificacionWebSocket from '../../../hooks/usePlanificacionWebSocket';
import WebSocketStatusIndicator from '../../../components/ui/WebSocketStatusIndicator';

// Ejemplo de componente simplificado
const EjemploSimuladorSemanalMejorado = ({ fechaInicioSimulacion, horaInicioSimulacion }) => {
  // ==================== HOOK DE WEBSOCKET MEJORADO ====================
  const {
    isConnected,
    connectionState,
    connectionQuality,
    latency,
    reconnectAttempt,
    iteraciones,
    estadoPlanificacion,
    iniciarPlanificacion,
    limpiarIteraciones,
    onMessage,
  } = usePlanificacionWebSocket({
    // Configuración personalizable (opcional)
    reconnectDelay: 1000,
    maxReconnectAttempts: 10,
    heartbeatInterval: 5000,
  });

  // Registrar callback para procesar mensajes
  useEffect(() => {
    onMessage((data) => {
      console.log('📨 Mensaje recibido:', data);
      
      // Aquí procesas los vuelos como antes
      if (data.solucion?.vuelos && data.solucion.vuelos.length > 0) {
        console.log(`✈️ ${data.solucion.vuelos.length} vuelos recibidos`);
        // setFlights(...) - tu lógica existente
      }
      
      // Actualizar tiempo simulado
      if (data.datos?.tiempoSimulacionActual) {
        console.log('⏰ Tiempo simulado:', data.datos.tiempoSimulacionActual);
        // setTiempoSimulado(...) - tu lógica existente
      }
    });
  }, [onMessage]);

  // Handler para iniciar planificación
  const handleIniciar = () => {
    if (!fechaInicioSimulacion) {
      alert('Selecciona una fecha de inicio');
      return;
    }

    if (!isConnected) {
      console.warn('⚠️ WebSocket no conectado');
      return;
    }

    // Limpiar estado anterior
    limpiarIteraciones();
    
    // Iniciar planificación
    iniciarPlanificacion(
      fechaInicioSimulacion,
      5, // factorK
      {
        tamanioPoblacion: 20,
        maxGeneraciones: 20,
        hora: horaInicioSimulacion
      }
    );
  };

  return (
    <div>
      {/* ==================== INDICADOR DE ESTADO ==================== */}
      <WebSocketStatusIndicator
        connectionState={connectionState}
        connectionQuality={connectionQuality}
        latency={latency}
        reconnectAttempt={reconnectAttempt}
        style={{ marginBottom: '16px' }}
      />

      {/* ==================== CONTROLES ==================== */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
        <button
          onClick={handleIniciar}
          disabled={!isConnected || estadoPlanificacion === 'running'}
          style={{
            padding: '8px 16px',
            backgroundColor: isConnected ? '#22c55e' : '#6b7280',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: isConnected ? 'pointer' : 'not-allowed',
          }}
        >
          {estadoPlanificacion === 'running' ? 'Ejecutando...' : 'Iniciar Planificación'}
        </button>

        <button
          onClick={limpiarIteraciones}
          style={{
            padding: '8px 16px',
            backgroundColor: '#64748b',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
        >
          Limpiar
        </button>
      </div>

      {/* ==================== LISTA DE ITERACIONES ==================== */}
      <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
        <h4>Iteraciones ({iteraciones.length})</h4>
        {iteraciones.map((it, index) => (
          <div 
            key={`iter-${index}`}
            style={{
              padding: '8px',
              marginBottom: '4px',
              backgroundColor: '#1e293b',
              borderRadius: '4px',
              fontSize: '12px',
            }}
          >
            <strong>#{it.datos?.ejecucionNumero || index + 1}</strong> - 
            {it.tipo === 'completado' ? ' ✅ Completado' : ' 📊 Progreso'} - 
            {it.solucion?.vuelos?.length || 0} vuelos
          </div>
        ))}
      </div>
    </div>
  );
};

export default EjemploSimuladorSemanalMejorado;

/**
 * ==================== BENEFICIOS DE ESTA MIGRACIÓN ====================
 * 
 * 1. CÓDIGO MÁS LIMPIO:
 *    - Eliminas ~200 líneas de código del componente principal
 *    - Estado centralizado en el hook
 *    - Sin refs duplicados ni código disperso
 * 
 * 2. RECONEXIÓN ROBUSTA:
 *    - Backoff exponencial (1s, 2s, 4s, 8s... hasta 30s máx)
 *    - Máximo de intentos configurable
 *    - Reconexión automática ante desconexiones inesperadas
 * 
 * 3. HEARTBEAT:
 *    - Detecta desconexiones silenciosas del servidor
 *    - Mide latencia de la conexión
 *    - Trigger de reconexión si no hay respuesta
 * 
 * 4. CALIDAD DE CONEXIÓN:
 *    - Indicador visual de latencia
 *    - Colores: Verde (< 100ms) → Amarillo → Rojo (> 500ms)
 *    - El usuario sabe si hay problemas de red
 * 
 * 5. REUTILIZABLE:
 *    - El hook se puede usar en otros componentes
 *    - El indicador es un componente independiente
 *    - Configuración flexible
 */
