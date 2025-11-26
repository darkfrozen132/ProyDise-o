/**
 * ==================== EJEMPLO DE INTEGRACIÓN: SimuladorEnVivo ====================
 * 
 * Componente de ejemplo que demuestra cómo usar el hook useSimulation
 * para crear una interfaz de simulación en tiempo real.
 * 
 * Características implementadas:
 * ✅ Conexión automática al WebSocket
 * ✅ Botones de control (Iniciar, Detener)
 * ✅ Barra de progreso reactiva
 * ✅ Visualización de rutas en tiempo real
 * ✅ Manejo de errores
 * ✅ Indicadores de estado
 * ✅ Limpieza automática al desmontar
 * 
 * @author Senior Frontend Developer
 * @version 1.0
 */

import React, { useState } from 'react';
import { useSimulation } from '../hooks/useSimulation';
import './SimuladorEnVivo.css';

const SimuladorEnVivo = () => {
  // ==================== HOOKS ====================
  
  const {
    // Estado
    isConnected,
    isSubscribed,
    isLoading,
    error,
    
    // Datos de simulación
    simulationId,
    status,
    progress,
    routes,
    processedOrders,
    totalOrders,
    currentFitness,
    
    // Métodos
    connect,
    startSimulation,
    cancelSimulation,
    disconnect,
    reset
  } = useSimulation({
    autoConnect: true,
    windowMinutes: 60
  });

  // Estado local
  const [windowMinutes, setWindowMinutes] = useState(60);

  // ==================== HANDLERS ====================
  
  const handleStart = async () => {
    try {
      reset(); // Limpiar datos anteriores
      await startSimulation(windowMinutes);
    } catch (err) {
      console.error('Error al iniciar:', err);
    }
  };

  const handleStop = async () => {
    try {
      await cancelSimulation();
    } catch (err) {
      console.error('Error al detener:', err);
    }
  };

  const handleReconnect = async () => {
    try {
      disconnect();
      setTimeout(() => connect(), 500);
    } catch (err) {
      console.error('Error al reconectar:', err);
    }
  };

  // ==================== HELPERS ====================
  
  const getStatusBadge = () => {
    if (!status) return { text: 'Sin iniciar', class: 'status-idle' };
    
    const statusMap = {
      RUNNING: { text: 'En ejecución', class: 'status-running' },
      COMPLETED: { text: 'Completado', class: 'status-completed' },
      CANCELLED: { text: 'Cancelado', class: 'status-cancelled' },
      ERROR: { text: 'Error', class: 'status-error' }
    };
    
    return statusMap[status] || { text: status, class: 'status-unknown' };
  };

  const statusBadge = getStatusBadge();
  const isSimulationActive = status === 'RUNNING';
  const canStart = isConnected && !isSimulationActive && !isLoading;
  const canStop = isConnected && isSimulationActive && !isLoading;

  // ==================== RENDER ====================
  
  return (
    <div className="simulador-en-vivo">
      {/* Header */}
      <div className="simulador-header">
        <h2>🚚 Simulación de Rutas en Vivo</h2>
        <div className="connection-indicators">
          <span className={`indicator ${isConnected ? 'connected' : 'disconnected'}`}>
            {isConnected ? '🟢 Conectado' : '🔴 Desconectado'}
          </span>
          {isSubscribed && (
            <span className="indicator subscribed">
              📡 Suscrito a: {simulationId?.slice(0, 8)}...
            </span>
          )}
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="error-banner">
          <strong>⚠️ Error:</strong> {error}
          <button onClick={handleReconnect} className="btn-reconnect">
            Reconectar
          </button>
        </div>
      )}

      {/* Panel de Control */}
      <div className="control-panel">
        <div className="control-group">
          <label htmlFor="window-minutes">Ventana de tiempo (minutos):</label>
          <input
            id="window-minutes"
            type="number"
            min="1"
            max="1440"
            value={windowMinutes}
            onChange={(e) => setWindowMinutes(Number(e.target.value))}
            disabled={isSimulationActive}
            className="input-minutes"
          />
        </div>

        <div className="control-buttons">
          <button
            onClick={handleStart}
            disabled={!canStart}
            className={`btn btn-start ${canStart ? '' : 'disabled'}`}
          >
            {isLoading ? '⏳ Iniciando...' : '▶️ Iniciar Simulación'}
          </button>

          <button
            onClick={handleStop}
            disabled={!canStop}
            className={`btn btn-stop ${canStop ? '' : 'disabled'}`}
          >
            ⏹️ Detener
          </button>
        </div>
      </div>

      {/* Panel de Estado */}
      <div className="status-panel">
        <div className="status-row">
          <div className="status-item">
            <span className="status-label">Estado:</span>
            <span className={`status-badge ${statusBadge.class}`}>
              {statusBadge.text}
            </span>
          </div>

          {simulationId && (
            <div className="status-item">
              <span className="status-label">ID Simulación:</span>
              <code className="simulation-id">{simulationId}</code>
            </div>
          )}
        </div>

        {/* Barra de Progreso */}
        {isSubscribed && (
          <div className="progress-section">
            <div className="progress-header">
              <span className="progress-label">Progreso</span>
              <span className="progress-percentage">{progress.toFixed(1)}%</span>
            </div>
            <div className="progress-bar-container">
              <div 
                className="progress-bar-fill"
                style={{ width: `${progress}%` }}
              />
            </div>
            <div className="progress-details">
              <span>{processedOrders} / {totalOrders} pedidos procesados</span>
              <span>Fitness: {currentFitness.toFixed(2)}</span>
            </div>
          </div>
        )}
      </div>

      {/* Lista de Rutas */}
      {routes.length > 0 && (
        <div className="routes-panel">
          <h3>📍 Rutas Activas ({routes.length})</h3>
          <div className="routes-list">
            {routes.slice(0, 10).map((route, index) => (
              <div key={index} className="route-card">
                <div className="route-header">
                  <span className="route-number">Ruta #{index + 1}</span>
                  <span className="route-status">
                    {route.status || 'Activa'}
                  </span>
                </div>
                <div className="route-details">
                  {route.origin && route.destination && (
                    <>
                      <div className="route-location">
                        <span className="location-label">Origen:</span>
                        <span className="location-value">{route.origin}</span>
                      </div>
                      <div className="route-location">
                        <span className="location-label">Destino:</span>
                        <span className="location-value">{route.destination}</span>
                      </div>
                    </>
                  )}
                  {route.packages !== undefined && (
                    <div className="route-packages">
                      📦 {route.packages} paquetes
                    </div>
                  )}
                </div>
              </div>
            ))}
            {routes.length > 10 && (
              <div className="routes-more">
                + {routes.length - 10} rutas más...
              </div>
            )}
          </div>
        </div>
      )}

      {/* Mensaje cuando no hay datos */}
      {!isSubscribed && !error && (
        <div className="empty-state">
          <div className="empty-icon">🚚</div>
          <h3>Sin simulación activa</h3>
          <p>Haz clic en "Iniciar Simulación" para comenzar</p>
        </div>
      )}
    </div>
  );
};

export default SimuladorEnVivo;
