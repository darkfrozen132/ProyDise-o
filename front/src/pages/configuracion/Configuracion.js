import React, { useState } from 'react';
import './Configuracion.css';

const Configuracion = () => {
  const [config, setConfig] = useState({
    warehouseCapacity: 1000,
    intraContinentalDays: 2,
    interContinentalDays: 3,
    intraFlightTime: 0.5,
    interFlightTime: 1.0,
    intraCapacityMin: 200,
    intraCapacityMax: 300,
    interCapacityMin: 250,
    interCapacityMax: 400,
    intraFrequency: 'multiple',
    interFrequency: 'daily'
  });

  const handleChange = (field, value) => {
    setConfig(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSave = () => {
    // Guardar configuración
    alert('Configuración guardada exitosamente');
  };

  const handleReset = () => {
    setConfig({
      warehouseCapacity: 1000,
      intraContinentalDays: 2,
      interContinentalDays: 3,
      intraFlightTime: 0.5,
      interFlightTime: 1.0,
      intraCapacityMin: 200,
      intraCapacityMax: 300,
      interCapacityMin: 250,
      interCapacityMax: 400,
      intraFrequency: 'multiple',
      interFrequency: 'daily'
    });
  };

  return (
    <div className="configuracion-container">
      <div className="control-panel">
        <h2>Configuración Inicial del Sistema</h2>
        
        {/* Sedes y Aeropuertos */}
        <div className="config-group">
          <h3><i className="fas fa-map-marker-alt"></i> Sedes y Aeropuertos</h3>
          <div className="config-item">
            <label>Sedes Principales:</label>
            <div className="sede-list">
              <span className="sede-item">📍 Lima, Perú (Stock Ilimitado)</span>
              <span className="sede-item">📍 Bruselas, Bélgica (Stock Ilimitado)</span>
              <span className="sede-item">📍 Baku, Azerbaiyán (Stock Ilimitado)</span>
            </div>
          </div>
          <div className="config-item">
            <label>Capacidad de Almacenes:</label>
            <input 
              type="number" 
              value={config.warehouseCapacity} 
              min="600" 
              max="1500"
              onChange={(e) => handleChange('warehouseCapacity', parseInt(e.target.value))}
            />
            <span>paquetes</span>
          </div>
        </div>

        {/* Plazos y Tiempos */}
        <div className="config-group">
          <h3><i className="fas fa-clock"></i> Plazos de Entrega</h3>
          <div className="config-item">
            <label>Plazo Intracontinental:</label>
            <input 
              type="number" 
              value={config.intraContinentalDays} 
              min="1" 
              max="5"
              onChange={(e) => handleChange('intraContinentalDays', parseInt(e.target.value))}
            /> días
          </div>
          <div className="config-item">
            <label>Plazo Intercontinental:</label>
            <input 
              type="number" 
              value={config.interContinentalDays} 
              min="1" 
              max="7"
              onChange={(e) => handleChange('interContinentalDays', parseInt(e.target.value))}
            /> días
          </div>
          <div className="config-item">
            <label>Tiempo de Vuelo PACK (Intracontinental):</label>
            <input 
              type="number" 
              step="0.1" 
              value={config.intraFlightTime} 
              min="0.1" 
              max="2"
              onChange={(e) => handleChange('intraFlightTime', parseFloat(e.target.value))}
            /> días
          </div>
          <div className="config-item">
            <label>Tiempo de Vuelo PACK (Intercontinental):</label>
            <input 
              type="number" 
              step="0.1" 
              value={config.interFlightTime} 
              min="0.5" 
              max="3"
              onChange={(e) => handleChange('interFlightTime', parseFloat(e.target.value))}
            /> días
          </div>
        </div>

        {/* Capacidades de Vuelo */}
        <div className="config-group">
          <h3><i className="fas fa-plane"></i> Capacidades de Vuelo</h3>
          <div className="config-item">
            <label>Capacidad Intracontinental:</label>
            <input 
              type="number" 
              value={config.intraCapacityMin} 
              min="100" 
              max="500"
              onChange={(e) => handleChange('intraCapacityMin', parseInt(e.target.value))}
            /> - 
            <input 
              type="number" 
              value={config.intraCapacityMax} 
              min="200" 
              max="600"
              onChange={(e) => handleChange('intraCapacityMax', parseInt(e.target.value))}
            /> paquetes
          </div>
          <div className="config-item">
            <label>Capacidad Intercontinental:</label>
            <input 
              type="number" 
              value={config.interCapacityMin} 
              min="150" 
              max="600"
              onChange={(e) => handleChange('interCapacityMin', parseInt(e.target.value))}
            /> - 
            <input 
              type="number" 
              value={config.interCapacityMax} 
              min="300" 
              max="800"
              onChange={(e) => handleChange('interCapacityMax', parseInt(e.target.value))}
            /> paquetes
          </div>
        </div>

        {/* Frecuencias */}
        <div className="config-group">
          <h3><i className="fas fa-calendar-alt"></i> Frecuencias de Vuelo</h3>
          <div className="config-item">
            <label>Frecuencia Intracontinental:</label>
            <select 
              value={config.intraFrequency}
              onChange={(e) => handleChange('intraFrequency', e.target.value)}
            >
              <option value="multiple">Múltiples veces al día</option>
              <option value="daily">Una vez al día</option>
              <option value="alternate">Días alternos</option>
            </select>
          </div>
          <div className="config-item">
            <label>Frecuencia Intercontinental:</label>
            <select 
              value={config.interFrequency}
              onChange={(e) => handleChange('interFrequency', e.target.value)}
            >
              <option value="daily">Al menos una vez al día</option>
              <option value="alternate">Días alternos</option>
              <option value="weekly">Semanal</option>
            </select>
          </div>
        </div>

        <div className="config-actions">
          <button className="config-btn save-config" onClick={handleSave}>
            <i className="fas fa-save"></i> Guardar Configuración
          </button>
          <button className="config-btn reset-config" onClick={handleReset}>
            <i className="fas fa-undo"></i> Restaurar Valores por Defecto
          </button>
        </div>

        {/* Sección adicional para probar scroll */}
        <div className="config-group">
          <h3>
            <i className="fas fa-info-circle"></i>
            Información del Sistema
          </h3>
          <div className="info-grid">
            <div className="info-item">
              <h4>Cobertura Global</h4>
              <p>MoraPack opera en 15 países con 3 sedes principales estratégicamente ubicadas.</p>
            </div>
            <div className="info-item">
              <h4>Capacidad de Procesamiento</h4>
              <p>Nuestro sistema puede manejar hasta 10,000 paquetes MPE simultáneamente.</p>
            </div>
            <div className="info-item">
              <h4>Tiempo de Respuesta</h4>
              <p>Garantizamos tiempos de entrega optimizados con nuestro algoritmo de rutas inteligentes.</p>
            </div>
            <div className="info-item">
              <h4>Monitoreo 24/7</h4>
              <p>Sistema de seguimiento en tiempo real con alertas automáticas de saturación.</p>
            </div>
          </div>
        </div>

        <div className="config-group">
          <h3>
            <i className="fas fa-chart-line"></i>
            Métricas del Sistema
          </h3>
          <div className="metrics-grid">
            <div className="metric-card">
              <div className="metric-value">402</div>
              <div className="metric-label">Vuelos Activos</div>
            </div>
            <div className="metric-card">
              <div className="metric-value">90.74%</div>
              <div className="metric-label">Saturación de Aviones</div>
            </div>
            <div className="metric-card">
              <div className="metric-value">12</div>
              <div className="metric-label">Aeropuertos Conectados</div>
            </div>
            <div className="metric-card">
              <div className="metric-value">15,847</div>
              <div className="metric-label">Paquetes Procesados Hoy</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Configuracion;
