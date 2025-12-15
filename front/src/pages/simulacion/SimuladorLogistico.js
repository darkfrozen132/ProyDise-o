/**
 * 🚀 SIMULADOR LOGÍSTICO EN TIEMPO REAL
 * 
 * Interfaz completa para gestionar simulaciones logísticas con algoritmo genético:
 * - Panel de control para iniciar/pausar/cancelar simulaciones
 * - Visualización en tiempo real del progreso
 * - Métricas del algoritmo genético
 * - Lista de rutas generadas
 * - Consola de logs
 */

import React, { useState } from 'react';
import { useSimulacionLogistica } from '../../hooks/useSimulacionLogistica';
import './SimuladorLogistico.css';

const SimuladorLogistico = () => {
  const {
    estado,
    estaConectado,
    sessionId,
    error,
    progreso,
    solucion,
    logs,
    stats,
    iniciar,
    cancelar,
    pausar,
    reanudar,
    reiniciar,
  } = useSimulacionLogistica();

  // Estado local del formulario
  const [fecha, setFecha] = useState(
    new Date().toISOString().split('T')[0] // Fecha actual por defecto
  );

  /**
   * 🚀 MANEJAR INICIO DE SIMULACIÓN
   */
  const handleIniciar = async () => {
    try {
      await iniciar(fecha);
    } catch (error) {
      console.error('Error al iniciar simulación:', error);
    }
  };

  /**
   * 🎨 OBTENER COLOR DEL ESTADO
   */
  const getEstadoColor = () => {
    const colors = {
      IDLE: 'text-gray-600 bg-gray-100',
      CONNECTING: 'text-blue-600 bg-blue-100',
      RUNNING: 'text-green-600 bg-green-100 animate-pulse',
      PAUSED: 'text-yellow-600 bg-yellow-100',
      COMPLETED: 'text-emerald-600 bg-emerald-100',
      CANCELLED: 'text-orange-600 bg-orange-100',
      ERROR: 'text-red-600 bg-red-100',
    };
    return colors[estado] || colors.IDLE;
  };

  /**
   * 🎨 OBTENER EMOJI DEL ESTADO
   */
  const getEstadoEmoji = () => {
    const emojis = {
      IDLE: '⚪',
      CONNECTING: '🔵',
      RUNNING: '🟢',
      PAUSED: '🟡',
      COMPLETED: '✅',
      CANCELLED: '🟠',
      ERROR: '🔴',
    };
    return emojis[estado] || '⚪';
  };

  /**
   * 🎨 OBTENER TEXTO DEL ESTADO
   */
  const getEstadoTexto = () => {
    const textos = {
      IDLE: 'Listo para iniciar',
      CONNECTING: 'Conectando...',
      RUNNING: 'En ejecución',
      PAUSED: 'Pausado',
      COMPLETED: 'Completado',
      CANCELLED: 'Cancelado',
      ERROR: 'Error',
    };
    return textos[estado] || 'Desconocido';
  };

  return (
    <div className="simulador-container">
      {/* HEADER */}
      <div className="simulador-header">
        <div>
          <h1 className="simulador-title">
            🚀 Simulador Logístico en Tiempo Real
          </h1>
          <p className="simulador-subtitle">
            Planificación de rutas con Algoritmo Genético
          </p>
        </div>
        
        <div className="connection-status">
          <span className={`status-indicator ${estaConectado ? 'connected' : 'disconnected'}`}>
            {estaConectado ? '🟢' : '🔴'}
          </span>
          <span className="status-text">
            {estaConectado ? 'Conectado' : 'Desconectado'}
          </span>
        </div>
      </div>

      {/* LAYOUT PRINCIPAL */}
      <div className="simulador-layout">
        
        {/* COLUMNA IZQUIERDA: CONTROLES Y PROGRESO */}
        <div className="columna-izquierda">
          
          {/* PANEL DE CONTROL */}
          <div className="panel card">
            <div className="panel-header">
              <h2 className="panel-title">⚙️ Panel de Control</h2>
              <span className={`estado-badge ${getEstadoColor()}`}>
                {getEstadoEmoji()} {getEstadoTexto()}
              </span>
            </div>

            <div className="panel-body">
              {/* Input de Fecha */}
              <div className="form-group">
                <label htmlFor="fecha" className="form-label">
                  📅 Fecha de Simulación
                </label>
                <input
                  id="fecha"
                  type="date"
                  value={fecha}
                  onChange={(e) => setFecha(e.target.value)}
                  disabled={!stats.puedeIniciar}
                  className="form-input"
                />
                <p className="form-hint">
                  El Factor K se configura automáticamente en 5
                </p>
              </div>

              {/* Botones de Control */}
              <div className="button-group">
                <button
                  onClick={handleIniciar}
                  disabled={!stats.puedeIniciar}
                  className="btn btn-primary"
                >
                  🚀 Iniciar Simulación
                </button>

                <button
                  onClick={pausar}
                  disabled={!stats.puedePausar}
                  className="btn btn-warning"
                >
                  ⏸️ Pausar
                </button>

                <button
                  onClick={reanudar}
                  disabled={!stats.puedeReanudar}
                  className="btn btn-success"
                >
                  ▶️ Reanudar
                </button>

                <button
                  onClick={cancelar}
                  disabled={!stats.puedeCancelar}
                  className="btn btn-danger"
                >
                  ⛔ Cancelar
                </button>
              </div>

              {/* Session ID */}
              {sessionId && (
                <div className="session-info">
                  <span className="session-label">Session ID:</span>
                  <code className="session-id">{sessionId.substring(0, 8)}...</code>
                </div>
              )}

              {/* Error */}
              {error && (
                <div className="error-message">
                  <span className="error-icon">⚠️</span>
                  <div>
                    <div className="error-title">{error.type}</div>
                    <div className="error-details">{error.message}</div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* PANEL DE PROGRESO */}
          <div className="panel card">
            <div className="panel-header">
              <h2 className="panel-title">📊 Progreso de la Simulación</h2>
            </div>

            <div className="panel-body">
              {/* Barra de Progreso */}
              <div className="progress-section">
                <div className="progress-header">
                  <span className="progress-label">Progreso General</span>
                  <span className="progress-value">{progreso.porcentaje.toFixed(1)}%</span>
                </div>
                <div className="progress-bar-container">
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${progreso.porcentaje}%` }}
                  >
                    <div className="progress-bar-shine"></div>
                  </div>
                </div>
              </div>

              {/* Métricas del Algoritmo Genético */}
              <div className="metrics-grid">
                <div className="metric-card">
                  <div className="metric-icon">🧬</div>
                  <div className="metric-content">
                    <div className="metric-label">Generación</div>
                    <div className="metric-value">
                      {progreso.generacionActual} / {progreso.maxGeneraciones}
                    </div>
                  </div>
                </div>

                <div className="metric-card">
                  <div className="metric-icon">💯</div>
                  <div className="metric-content">
                    <div className="metric-label">Mejor Fitness</div>
                    <div className="metric-value">
                      {progreso.mejorFitness.toFixed(2)}
                    </div>
                  </div>
                </div>

                <div className="metric-card">
                  <div className="metric-icon">📦</div>
                  <div className="metric-content">
                    <div className="metric-label">Pedidos</div>
                    <div className="metric-value">
                      {progreso.pedidosProcesados} / {progreso.pedidosTotales}
                    </div>
                  </div>
                </div>

                <div className="metric-card">
                  <div className="metric-icon">📈</div>
                  <div className="metric-content">
                    <div className="metric-label">Fitness Promedio</div>
                    <div className="metric-value">
                      {progreso.fitnessPromedio.toFixed(2)}
                    </div>
                  </div>
                </div>
              </div>

              {/* Fecha Simulada */}
              {progreso.fechaSimulada && (
                <div className="simulated-time">
                  <span className="simulated-time-icon">🕐</span>
                  <div>
                    <div className="simulated-time-label">Tiempo Simulado</div>
                    <div className="simulated-time-value">
                      {new Date(progreso.fechaSimulada).toLocaleString('es-ES')}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* PANEL DE MÉTRICAS DE LA SOLUCIÓN */}
          {solucion.rutas.length > 0 && (
            <div className="panel card">
              <div className="panel-header">
                <h2 className="panel-title">📊 Métricas de la Solución</h2>
              </div>

              <div className="panel-body">
                <div className="solution-metrics">
                  <div className="solution-metric">
                    <span className="solution-metric-icon">🗺️</span>
                    <div>
                      <div className="solution-metric-label">Total Rutas</div>
                      <div className="solution-metric-value">
                        {solucion.metricas.totalRutas}
                      </div>
                    </div>
                  </div>

                  <div className="solution-metric">
                    <span className="solution-metric-icon">✈️</span>
                    <div>
                      <div className="solution-metric-label">Total Vuelos</div>
                      <div className="solution-metric-value">
                        {solucion.metricas.totalVuelos}
                      </div>
                    </div>
                  </div>

                  <div className="solution-metric">
                    <span className="solution-metric-icon">💰</span>
                    <div>
                      <div className="solution-metric-label">Costo Total</div>
                      <div className="solution-metric-value">
                        ${solucion.metricas.costoTotal.toLocaleString()}
                      </div>
                    </div>
                  </div>

                  <div className="solution-metric">
                    <span className="solution-metric-icon">⏱️</span>
                    <div>
                      <div className="solution-metric-label">Tiempo Promedio</div>
                      <div className="solution-metric-value">
                        {solucion.metricas.tiempoPromedioEntrega.toFixed(1)}h
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* COLUMNA DERECHA: RUTAS Y LOGS */}
        <div className="columna-derecha">
          
          {/* PANEL DE RUTAS */}
          <div className="panel card">
            <div className="panel-header">
              <h2 className="panel-title">🗺️ Rutas Generadas</h2>
              <span className="badge">{solucion.rutas.length} rutas</span>
            </div>

            <div className="panel-body">
              {solucion.rutas.length === 0 ? (
                <div className="empty-state">
                  <span className="empty-icon">📭</span>
                  <p className="empty-text">
                    No hay rutas generadas aún
                  </p>
                  <p className="empty-hint">
                    Inicia una simulación para ver las rutas
                  </p>
                </div>
              ) : (
                <div className="rutas-list">
                  {solucion.rutas.slice(0, 10).map((ruta, index) => (
                    <div key={index} className="ruta-card">
                      <div className="ruta-header">
                        <span className="ruta-id">Ruta #{ruta.pedidoId}</span>
                        <span className="ruta-airports">
                          {ruta.origen} → {ruta.destino}
                        </span>
                      </div>
                      
                      {ruta.subRutas && ruta.subRutas.length > 0 && (
                        <div className="subrutas-list">
                          {ruta.subRutas.map((subRuta, idx) => (
                            <div key={idx} className="subruta-item">
                              <div className="subruta-icon">✈️</div>
                              <div className="subruta-details">
                                <div className="subruta-flight">
                                  <strong>{subRuta.vuelo}</strong>
                                  <span className="subruta-route">
                                    {subRuta.origen} → {subRuta.destino}
                                  </span>
                                </div>
                                <div className="subruta-times">
                                  <span>
                                    🛫 {new Date(subRuta.horaSalida).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}
                                  </span>
                                  <span>
                                    🛬 {new Date(subRuta.horaLlegada).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}
                                  </span>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}

                  {solucion.rutas.length > 10 && (
                    <div className="rutas-more">
                      Y {solucion.rutas.length - 10} rutas más...
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* PANEL DE LOGS */}
          <div className="panel card">
            <div className="panel-header">
              <h2 className="panel-title">📝 Consola de Eventos</h2>
              <button
                onClick={() => {}}
                className="btn-icon"
                title="Limpiar logs"
              >
                🗑️
              </button>
            </div>

            <div className="panel-body">
              <div className="logs-container">
                {logs.length === 0 ? (
                  <div className="empty-state-small">
                    <span className="empty-icon-small">📋</span>
                    <p className="empty-text-small">No hay eventos aún</p>
                  </div>
                ) : (
                  logs.map((log) => (
                    <div key={log.id} className={`log-item log-${log.tipo}`}>
                      <span className="log-time">{log.timestamp}</span>
                      <span className="log-icon">
                        {log.tipo === 'success' && '✅'}
                        {log.tipo === 'error' && '❌'}
                        {log.tipo === 'warning' && '⚠️'}
                        {log.tipo === 'info' && 'ℹ️'}
                      </span>
                      <span className="log-message">{log.mensaje}</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* BOTÓN DE REINICIO (Solo visible cuando está completado) */}
      {(estado === 'COMPLETED' || estado === 'CANCELLED') && (
        <div className="reiniciar-container">
          <button onClick={reiniciar} className="btn btn-primary btn-large">
            🔄 Iniciar Nueva Simulación
          </button>
        </div>
      )}
    </div>
  );
};

export default SimuladorLogistico;
