/**
 * ==================== SIMULADOR EN VIVO (REST + WebSocket) ====================
 * 
 * Componente principal que orquesta todo el ciclo de vida de la simulación:
 * 1. Conecta al WebSocket al montar
 * 2. Inicia simulación por REST (obtiene ID)
 * 3. Se suscribe al tópico específico con el ID
 * 4. Recibe snapshots en tiempo real y actualiza UI
 * 5. Limpia conexión al desmontar
 * 
 * @author Senior React Developer
 * @version 3.0 - Production Ready
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import { simulationService, SimulationStatus } from '../../../services/SimulationService';
import 'leaflet/dist/leaflet.css';
import './Simulador.css';

// Fix para iconos de Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

// ==================== ICONOS PERSONALIZADOS ====================

const createAirplaneIcon = (color = '#007bff', rotation = 0) => {
  return L.divIcon({
    html: `<div style="transform: rotate(${rotation}deg); color: ${color}; font-size: 24px;">
             ✈️
           </div>`,
    className: 'airplane-icon',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    popupAnchor: [0, -12]
  });
};

const createAirportIcon = (color = '#28a745') => {
  return L.divIcon({
    html: `<div style="
        background: ${color};
        border: 3px solid white;
        border-radius: 50%;
        width: 20px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
      ">
        <span style="color: white; font-size: 10px;">📍</span>
      </div>`,
    className: 'airport-marker',
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [0, -10]
  });
};

// ==================== COMPONENTE PRINCIPAL ====================

const Simulador = () => {
  // ==================== ESTADO ====================
  
  // Conexión y simulación
  const [connected, setConnected] = useState(false);
  const [simulationId, setSimulationId] = useState(null);
  const [status, setStatus] = useState('IDLE'); // IDLE, CONNECTING, RUNNING, COMPLETED, CANCELLED, ERROR
  
  // Datos de simulación
  const [progress, setProgress] = useState(0);
  const [stats, setStats] = useState({
    processedOrders: 0,
    totalOrders: 0,
    currentFitness: 0,
    elapsedTime: 0
  });
  
  // Rutas y logs
  const [routes, setRoutes] = useState([]);
  const [logs, setLogs] = useState([]);
  
  // Configuración
  const [windowMinutes, setWindowMinutes] = useState(60);
  
  // Referencia para cleanup
  const isMounted = useRef(true);
  
  // ==================== CALLBACKS DE SIMULACIÓN ====================
  
  /**
   * Manejador de mensajes del WebSocket
   */
  const handleMessage = useCallback((data) => {
    if (!isMounted.current) return;
    
    console.log('📨 Snapshot recibido:', data);
    
    // Actualizar estado
    setStatus(data.status || 'RUNNING');
    
    // Actualizar progreso
    if (data.totalOrders > 0) {
      const progressPercent = (data.processedOrders / data.totalOrders) * 100;
      setProgress(progressPercent);
    }
    
    // Actualizar stats
    setStats({
      processedOrders: data.processedOrders || 0,
      totalOrders: data.totalOrders || 0,
      currentFitness: data.currentFitness || 0,
      elapsedTime: data.elapsedTime || 0
    });
    
    // Actualizar rutas
    if (data.routes && Array.isArray(data.routes)) {
      setRoutes(data.routes);
    }
    
    // Agregar log
    const logMessage = `[${new Date().toLocaleTimeString()}] Status: ${data.status} | Progress: ${data.processedOrders}/${data.totalOrders}`;
    setLogs(prevLogs => [...prevLogs.slice(-20), logMessage]); // Mantener solo últimos 20 logs
  }, []);
  
  /**
   * Manejador de errores
   */
  const handleError = useCallback((error) => {
    if (!isMounted.current) return;
    
    console.error('❌ Error en simulación:', error);
    setStatus('ERROR');
    setLogs(prevLogs => [...prevLogs, `[ERROR] ${error.message}`]);
    alert(`Error: ${error.message}`);
  }, []);
  
  /**
   * Manejador cuando se completa la simulación
   */
  const handleSimulationComplete = useCallback((finalData) => {
    if (!isMounted.current) return;
    
    console.log('🏁 Simulación completada:', finalData);
    setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] Simulación ${finalData.status}`]);
    
    // Opcional: Mostrar notificación
    if (finalData.status === SimulationStatus.COMPLETED) {
      alert('✅ Simulación completada exitosamente');
    } else if (finalData.status === SimulationStatus.CANCELLED) {
      alert('⏹️ Simulación cancelada');
    }
  }, []);
  
  /**
   * Manejador cuando se conecta
   */
  const handleConnected = useCallback(() => {
    if (!isMounted.current) return;
    
    console.log('✅ WebSocket conectado');
    setConnected(true);
    setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] WebSocket conectado`]);
  }, []);
  
  /**
   * Manejador cuando se desconecta
   */
  const handleDisconnected = useCallback(() => {
    if (!isMounted.current) return;
    
    console.log('🔌 WebSocket desconectado');
    setConnected(false);
    setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] WebSocket desconectado`]);
  }, []);
  
  // ==================== EFECTOS ====================
  
  /**
   * Efecto de inicialización: Conectar al WebSocket
   */
  useEffect(() => {
    console.log('🚀 Montando componente Simulador');
    isMounted.current = true;
    
    // Configurar callbacks
    simulationService
      .onMessage(handleMessage)
      .onError(handleError)
      .onConnected(handleConnected)
      .onDisconnected(handleDisconnected)
      .onSimulationComplete(handleSimulationComplete);
    
    // Conectar
    setStatus('CONNECTING');
    simulationService.connect()
      .then(() => {
        console.log('✅ Conexión establecida');
        setStatus('IDLE');
      })
      .catch((error) => {
        console.error('❌ Error al conectar:', error);
        setStatus('ERROR');
        alert('Error al conectar con el servidor. Verifica que el backend esté corriendo.');
      });
    
    // Cleanup al desmontar
    return () => {
      console.log('🧹 Desmontando componente Simulador');
      isMounted.current = false;
      simulationService.disconnect();
    };
  }, []); // Solo ejecutar al montar
  
  // ==================== MANEJADORES DE ACCIONES ====================
  
  /**
   * Iniciar nueva simulación
   */
  const handleStart = async () => {
    if (!connected) {
      alert('No hay conexión WebSocket. Espera a que se conecte.');
      return;
    }
    
    if (status === 'RUNNING') {
      alert('Ya hay una simulación en curso');
      return;
    }
    
    try {
      console.log('🚀 Iniciando simulación...');
      setStatus('RUNNING');
      setProgress(0);
      setStats({ processedOrders: 0, totalOrders: 0, currentFitness: 0, elapsedTime: 0 });
      setRoutes([]);
      setLogs([`[${new Date().toLocaleTimeString()}] Iniciando simulación...`]);
      
      // Llamar al servicio (esto hace POST y se suscribe automáticamente)
      const id = await simulationService.startSimulation(windowMinutes);
      
      setSimulationId(id);
      console.log('✅ Simulación iniciada con ID:', id);
      setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] Simulación iniciada (ID: ${id})`]);
      
    } catch (error) {
      console.error('❌ Error al iniciar simulación:', error);
      setStatus('ERROR');
      setLogs(prevLogs => [...prevLogs, `[ERROR] No se pudo iniciar: ${error.message}`]);
      
      // Verificar si es problema de ID
      if (error.message.includes('ID')) {
        alert('Error: El backend no devolvió un ID válido. Verifica que el endpoint /api/simulations esté funcionando correctamente.');
      } else {
        alert(`Error al iniciar simulación: ${error.message}`);
      }
    }
  };
  
  /**
   * Cancelar simulación actual
   */
  const handleStop = async () => {
    if (!simulationId) {
      alert('No hay simulación activa');
      return;
    }
    
    try {
      console.log('⏹️ Cancelando simulación...');
      setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] Cancelando simulación...`]);
      
      await simulationService.cancelSimulation();
      
      setStatus('CANCELLED');
      console.log('✅ Simulación cancelada');
      setLogs(prevLogs => [...prevLogs, `[${new Date().toLocaleTimeString()}] Simulación cancelada`]);
      
    } catch (error) {
      console.error('❌ Error al cancelar simulación:', error);
      alert(`Error al cancelar: ${error.message}`);
    }
  };
  
  /**
   * Reset para nueva simulación
   */
  const handleReset = () => {
    setSimulationId(null);
    setStatus('IDLE');
    setProgress(0);
    setStats({ processedOrders: 0, totalOrders: 0, currentFitness: 0, elapsedTime: 0 });
    setRoutes([]);
    setLogs([]);
    console.log('🔄 Reset completado');
  };
  
  // ==================== RENDER ====================
  
  return (
    <div className="simulador-container">
      <h1>🎮 Simulador de Rutas en Tiempo Real</h1>
      
      {/* Panel de Control */}
      <div className="control-panel">
        <div className="status-badge" data-status={status.toLowerCase()}>
          {status === 'CONNECTING' && '🔄 Conectando...'}
          {status === 'IDLE' && '⏸️ Listo'}
          {status === 'RUNNING' && '🚀 Simulando'}
          {status === 'COMPLETED' && '✅ Completado'}
          {status === 'CANCELLED' && '⏹️ Cancelado'}
          {status === 'ERROR' && '❌ Error'}
        </div>
        
        <div className="connection-status">
          <span className={connected ? 'connected' : 'disconnected'}>
            {connected ? '🟢 Conectado' : '🔴 Desconectado'}
          </span>
          {simulationId && <span className="simulation-id">ID: {simulationId}</span>}
        </div>
        
        <div className="controls">
          <div className="input-group">
            <label>Ventana de tiempo (minutos):</label>
            <input
              type="number"
              value={windowMinutes}
              onChange={(e) => setWindowMinutes(Number(e.target.value))}
              min="10"
              max="180"
              disabled={status === 'RUNNING'}
            />
          </div>
          
          <button
            onClick={handleStart}
            disabled={!connected || status === 'RUNNING' || status === 'CONNECTING'}
            className="btn btn-start"
          >
            🚀 Iniciar Simulación
          </button>
          
          <button
            onClick={handleStop}
            disabled={status !== 'RUNNING'}
            className="btn btn-stop"
          >
            ⏹️ Detener
          </button>
          
          <button
            onClick={handleReset}
            disabled={status === 'RUNNING'}
            className="btn btn-reset"
          >
            🔄 Reset
          </button>
        </div>
      </div>
      
      {/* Barra de Progreso */}
      <div className="progress-section">
        <div className="progress-header">
          <span>Progreso: {progress.toFixed(1)}%</span>
          <span>{stats.processedOrders} / {stats.totalOrders} pedidos</span>
        </div>
        <div className="progress-bar-container">
          <div
            className="progress-bar"
            style={{
              width: `${Math.min(progress, 100)}%`,
              backgroundColor: progress === 100 ? '#28a745' : '#007bff'
            }}
          />
        </div>
      </div>
      
      {/* Estadísticas */}
      <div className="stats-panel">
        <div className="stat-card">
          <div className="stat-label">Fitness Actual</div>
          <div className="stat-value">{stats.currentFitness.toFixed(2)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Tiempo Transcurrido</div>
          <div className="stat-value">{(stats.elapsedTime / 1000).toFixed(1)}s</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Rutas Generadas</div>
          <div className="stat-value">{routes.length}</div>
        </div>
      </div>
      
      {/* Mapa de Rutas */}
      <div className="map-section">
        <h3>🗺️ Mapa de Rutas ({routes.length} rutas activas)</h3>
        <MapContainer
          center={[0, 0]}
          zoom={2}
          style={{ height: '500px', width: '100%' }}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; OpenStreetMap contributors'
          />
          
          {/* Renderizar rutas */}
          {routes.map((route, index) => {
            // Asumiendo que cada ruta tiene stops con coordenadas
            const stops = route.stops || [];
            
            if (stops.length < 2) return null;
            
            const positions = stops.map(stop => [
              stop.latitude || stop.lat || 0,
              stop.longitude || stop.lng || 0
            ]);
            
            const colors = ['#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF'];
            const color = colors[index % colors.length];
            
            return (
              <React.Fragment key={route.id || index}>
                <Polyline
                  positions={positions}
                  color={color}
                  weight={3}
                  opacity={0.7}
                >
                  <Popup>
                    <strong>Ruta #{index + 1}</strong><br />
                    ID: {route.id}<br />
                    Paradas: {stops.length}<br />
                    Fitness: {route.fitness?.toFixed(2) || 'N/A'}
                  </Popup>
                </Polyline>
                
                {/* Marcadores en cada parada */}
                {stops.map((stop, stopIndex) => {
                  const lat = stop.latitude || stop.lat || 0;
                  const lng = stop.longitude || stop.lng || 0;
                  
                  if (lat === 0 && lng === 0) return null;
                  
                  return (
                    <Marker
                      key={`${index}-${stopIndex}`}
                      position={[lat, lng]}
                      icon={createAirportIcon(color)}
                    >
                      <Popup>
                        <strong>Parada #{stopIndex + 1}</strong><br />
                        {stop.airportCode || stop.code || 'N/A'}<br />
                        Lat: {lat.toFixed(4)}<br />
                        Lng: {lng.toFixed(4)}
                      </Popup>
                    </Marker>
                  );
                })}
              </React.Fragment>
            );
          })}
        </MapContainer>
      </div>
      
      {/* Logs */}
      <div className="logs-section">
        <h3>📋 Logs del Sistema</h3>
        <div className="logs-container">
          {logs.length === 0 ? (
            <p className="no-logs">Sin logs aún...</p>
          ) : (
            logs.map((log, index) => (
              <div key={index} className="log-entry">{log}</div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default Simulador;
