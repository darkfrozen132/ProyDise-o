/**
 * ==================== COMPONENTE: WebSocketStatusIndicator ====================
 * 
 * Indicador visual del estado de conexión WebSocket.
 * Muestra:
 * - Estado de conexión (conectado/desconectado/reconectando)
 * - Calidad de conexión (latencia)
 * - Intentos de reconexión
 * 
 * @author Refactored for SimuladorSemanal
 */

import React from 'react';
import PropTypes from 'prop-types';

// ==================== ESTILOS ====================
const styles = {
  container: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '8px 12px',
    borderRadius: '8px',
    backgroundColor: '#1e293b',
    border: '1px solid #334155',
    fontSize: '12px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  indicator: {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    transition: 'all 0.3s ease',
  },
  text: {
    color: '#94a3b8',
    fontWeight: 500,
  },
  latency: {
    color: '#64748b',
    fontSize: '11px',
  },
  reconnecting: {
    color: '#fbbf24',
    fontSize: '11px',
    fontStyle: 'italic',
  },
};

// Colores según estado
const stateColors = {
  connected: '#22c55e',      // Verde
  connecting: '#3b82f6',     // Azul
  reconnecting: '#f59e0b',   // Amarillo
  disconnected: '#6b7280',   // Gris
  error: '#ef4444',          // Rojo
};

// Colores según calidad
const qualityColors = {
  excellent: '#22c55e',  // Verde
  good: '#84cc16',       // Verde claro
  fair: '#f59e0b',       // Amarillo
  poor: '#ef4444',       // Rojo
  unknown: '#6b7280',    // Gris
};

// Textos de estado
const stateTexts = {
  connected: 'Conectado',
  connecting: 'Conectando...',
  reconnecting: 'Reconectando...',
  disconnected: 'Desconectado',
  error: 'Error',
};

/**
 * Componente indicador de estado WebSocket
 */
const WebSocketStatusIndicator = ({
  connectionState = 'disconnected',
  connectionQuality = 'unknown',
  latency = null,
  reconnectAttempt = 0,
  maxReconnectAttempts = 10,
  compact = false,
  showQuality = true,
  className = '',
  style = {},
}) => {
  const indicatorColor = stateColors[connectionState] || stateColors.disconnected;
  const qualityColor = qualityColors[connectionQuality] || qualityColors.unknown;
  const stateText = stateTexts[connectionState] || 'Desconocido';
  
  // Animación de pulso para estados activos
  const isPulsing = connectionState === 'connecting' || connectionState === 'reconnecting';
  
  const indicatorStyle = {
    ...styles.indicator,
    backgroundColor: indicatorColor,
    boxShadow: isPulsing 
      ? `0 0 0 3px ${indicatorColor}40` 
      : `0 0 4px ${indicatorColor}80`,
    animation: isPulsing ? 'pulse 1.5s ease-in-out infinite' : 'none',
  };
  
  if (compact) {
    const titleText = latency ? `${stateText} (${latency}ms)` : stateText;
    return (
      <div 
        className={className}
        style={{ ...styles.container, padding: '4px 8px', ...style }}
        title={titleText}
      >
        <div style={indicatorStyle} />
        <span style={styles.text}>{stateText}</span>
      </div>
    );
  }
  
  return (
    <div className={className} style={{ ...styles.container, ...style }}>
      {/* Indicador de conexión */}
      <div style={indicatorStyle} />
      
      {/* Estado de conexión */}
      <span style={styles.text}>{stateText}</span>
      
      {/* Latencia y calidad */}
      {showQuality && connectionState === 'connected' && latency !== null && (
        <span style={{ ...styles.latency, color: qualityColor }}>
          ({latency}ms)
        </span>
      )}
      
      {/* Intentos de reconexión */}
      {connectionState === 'reconnecting' && reconnectAttempt > 0 && (
        <span style={styles.reconnecting}>
          Intento {reconnectAttempt}/{maxReconnectAttempts}
        </span>
      )}
      
      {/* Estilos CSS para animación */}
      <style>{`
        @keyframes pulse {
          0%, 100% {
            opacity: 1;
            transform: scale(1);
          }
          50% {
            opacity: 0.6;
            transform: scale(1.1);
          }
        }
      `}</style>
    </div>
  );
};

WebSocketStatusIndicator.propTypes = {
  connectionState: PropTypes.oneOf([
    'connected',
    'connecting',
    'reconnecting',
    'disconnected',
    'error',
  ]),
  connectionQuality: PropTypes.oneOf([
    'excellent',
    'good',
    'fair',
    'poor',
    'unknown',
  ]),
  latency: PropTypes.number,
  reconnectAttempt: PropTypes.number,
  maxReconnectAttempts: PropTypes.number,
  compact: PropTypes.bool,
  showQuality: PropTypes.bool,
  className: PropTypes.string,
  style: PropTypes.object,
};

export default WebSocketStatusIndicator;
