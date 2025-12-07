import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import { cargarAeropuertos } from '../services/api';
import { API_BASE_URL } from '../config/api';
import axios from 'axios';
import 'leaflet/dist/leaflet.css';
import './SimuladorSimple.css';

// Iconos de avión
const iconoAvion = new L.Icon({
  iconUrl: '/avion.png',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

const iconoAeropuerto = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

/**
 * Componente para ajustar el zoom del mapa automáticamente
 */
function AjustarZoom({ aeropuertos, vuelos }) {
  const map = useMap();
  
  useEffect(() => {
    const puntos = [];
    
    // Agregar aeropuertos
    aeropuertos.forEach(a => {
      if (a.latitud && a.longitud) {
        puntos.push([a.latitud, a.longitud]);
      }
    });
    
    // Agregar puntos de vuelos
    vuelos.forEach(v => {
      if (v.origenLatitud && v.origenLongitud) {
        puntos.push([v.origenLatitud, v.origenLongitud]);
      }
      if (v.destinoLatitud && v.destinoLongitud) {
        puntos.push([v.destinoLatitud, v.destinoLongitud]);
      }
    });
    
    if (puntos.length > 0) {
      const bounds = L.latLngBounds(puntos);
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [map, aeropuertos, vuelos]);
  
  return null;
}

/**
 * SIMULADOR SEMANAL SIMPLIFICADO
 * 
 * Usa endpoint REST en lugar de WebSocket
 * Proceso:
 * 1. Click en "Iniciar Simulación"
 * 2. Seleccionar fecha
 * 3. Esperar (loading)
 * 4. Mostrar TODOS los vuelos en el mapa de una vez
 */
export default function SimuladorSimple() {
  
  const [aeropuertos, setAeropuertos] = useState([]);
  const [vuelos, setVuelos] = useState([]);
  const [estadoSimulacion, setEstadoSimulacion] = useState('DETENIDO'); // DETENIDO, CARGANDO, COMPLETADO, ERROR
  const [mensaje, setMensaje] = useState('');
  const [fechaSeleccionada, setFechaSeleccionada] = useState('2025-01-15');
  
  const [estadisticas, setEstadisticas] = useState({
    totalVuelos: 0,
    totalPedidos: 0,
    duracionMs: 0
  });
  
  // Cargar aeropuertos al inicio
  useEffect(() => {
    cargarAeropuertosMapa();
  }, []);
  
  const cargarAeropuertosMapa = async () => {
    try {
      console.log('📍 Cargando aeropuertos...');
      const aeropuertosData = await cargarAeropuertos();
      console.log(`✅ ${aeropuertosData.length} aeropuertos cargados`);
      setAeropuertos(aeropuertosData);
    } catch (error) {
      console.error('❌ Error cargando aeropuertos:', error);
    }
  };
  
  /**
   * Buscar aeropuerto por código ICAO (insensible a mayúsculas)
   */
  const buscarAeropuerto = (codigoICAO) => {
    if (!codigoICAO) return null;
    return aeropuertos.find(a => 
      a.codigoICAO && a.codigoICAO.toLowerCase() === codigoICAO.toLowerCase()
    );
  };
  
  /**
   * Iniciar simulación (llama al endpoint REST)
   */
  const handleIniciarSimulacion = async () => {
    if (!fechaSeleccionada) {
      alert('Por favor selecciona una fecha');
      return;
    }
    
    console.log('═══════════════════════════════════════════════════════════');
    console.log('🚀 INICIANDO SIMULACIÓN SIMPLE');
    console.log('📅 Fecha:', fechaSeleccionada);
    console.log('═══════════════════════════════════════════════════════════');
    
    setEstadoSimulacion('CARGANDO');
    setMensaje('Ejecutando planificación semanal...');
    setVuelos([]); // Limpiar vuelos anteriores
    setEstadisticas({ totalVuelos: 0, totalPedidos: 0, duracionMs: 0 });
    
    try {
      const inicio = Date.now();
      
      // Llamar al endpoint REST
      const response = await axios.post(
        `${API_BASE_URL}/api/planificacion/ejecutar-simple`,
        null,
        {
          params: {
            fecha: fechaSeleccionada,
            factorK: 5,
            tamanioPoblacion: 20,
            maxGeneraciones: 20,
            limiteGeneracionesSinMejora: 10
          }
        }
      );
      
      const duracion = Date.now() - inicio;
      
      console.log('✅ Respuesta recibida en', duracion, 'ms');
      console.log('📦 Datos:', response.data);
      
      // Procesar vuelos
      const vuelosConCoordenadas = procesarVuelos(response.data.vuelos || []);
      
      setVuelos(vuelosConCoordenadas);
      setEstadoSimulacion('COMPLETADO');
      setMensaje(`✅ ${vuelosConCoordenadas.length} vuelos planificados`);
      
      setEstadisticas({
        totalVuelos: response.data.vuelos?.length || 0,
        totalPedidos: response.data.vuelos?.reduce((sum, v) => sum + (v.pedidos?.length || 0), 0) || 0,
        duracionMs: duracion
      });
      
      console.log('═══════════════════════════════════════════════════════════');
      console.log('✅ SIMULACIÓN COMPLETADA');
      console.log('✈️ Vuelos:', vuelosConCoordenadas.length);
      console.log('⏱️ Duración:', duracion, 'ms');
      console.log('═══════════════════════════════════════════════════════════');
      
    } catch (error) {
      console.error('❌ Error en simulación:', error);
      setEstadoSimulacion('ERROR');
      setMensaje(`❌ Error: ${error.response?.data?.mensaje || error.message}`);
    }
  };
  
  /**
   * Procesar vuelos: agregar coordenadas de aeropuertos
   */
  const procesarVuelos = (vuelosRaw) => {
    console.log(`🔄 Procesando ${vuelosRaw.length} vuelos...`);
    
    const vuelosConDatos = vuelosRaw.map((vuelo, index) => {
      const origen = buscarAeropuerto(vuelo.origenCodigoICAO);
      const destino = buscarAeropuerto(vuelo.destinoCodigoICAO);
      
      if (!origen || !destino) {
        console.warn(`⚠️ Vuelo ${index}: No se encontró aeropuerto`, {
          origen: vuelo.origenCodigoICAO,
          destino: vuelo.destinoCodigoICAO
        });
        return null;
      }
      
      return {
        ...vuelo,
        id: `vuelo-${index}`,
        origenLatitud: origen.latitud,
        origenLongitud: origen.longitud,
        origenNombre: origen.nombre,
        destinoLatitud: destino.latitud,
        destinoLongitud: destino.longitud,
        destinoNombre: destino.nombre,
        color: obtenerColorAleatorio()
      };
    });
    
    const vuelosValidos = vuelosConDatos.filter(v => v !== null);
    console.log(`✅ ${vuelosValidos.length} vuelos con coordenadas válidas`);
    
    return vuelosValidos;
  };
  
  /**
   * Generar color aleatorio para vuelo
   */
  const obtenerColorAleatorio = () => {
    const colores = ['#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF', '#FFA500', '#800080'];
    return colores[Math.floor(Math.random() * colores.length)];
  };
  
  /**
   * Limpiar mapa
   */
  const handleLimpiar = () => {
    setVuelos([]);
    setEstadoSimulacion('DETENIDO');
    setMensaje('');
    setEstadisticas({ totalVuelos: 0, totalPedidos: 0, duracionMs: 0 });
    console.log('🧹 Mapa limpiado');
  };
  
  return (
    <div className="simulador-simple-container">
      <h1>✈️ Simulador Semanal Simple</h1>
      
      {/* Panel de control */}
      <div className="panel-control">
        <div className="input-group">
          <label>📅 Fecha inicial:</label>
          <input 
            type="date" 
            value={fechaSeleccionada}
            onChange={(e) => setFechaSeleccionada(e.target.value)}
            disabled={estadoSimulacion === 'CARGANDO'}
          />
        </div>
        
        <button 
          onClick={handleIniciarSimulacion}
          disabled={estadoSimulacion === 'CARGANDO'}
          className="btn-iniciar"
        >
          {estadoSimulacion === 'CARGANDO' ? '⏳ Cargando...' : '🚀 Iniciar Simulación'}
        </button>
        
        <button 
          onClick={handleLimpiar}
          className="btn-limpiar"
          disabled={estadoSimulacion === 'CARGANDO'}
        >
          🧹 Limpiar
        </button>
      </div>
      
      {/* Mensaje de estado */}
      {mensaje && (
        <div className={`mensaje-estado ${estadoSimulacion.toLowerCase()}`}>
          {mensaje}
        </div>
      )}
      
      {/* Estadísticas */}
      {estadisticas.totalVuelos > 0 && (
        <div className="estadisticas">
          <div>✈️ Vuelos: {estadisticas.totalVuelos}</div>
          <div>📦 Pedidos: {estadisticas.totalPedidos}</div>
          <div>⏱️ Duración: {(estadisticas.duracionMs / 1000).toFixed(2)}s</div>
        </div>
      )}
      
      {/* Mapa */}
      <div className="mapa-container">
        <MapContainer 
          center={[0, 0]} 
          zoom={2} 
          style={{ height: '600px', width: '100%' }}
        >
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          />
          
          {/* Ajustar zoom automáticamente */}
          <AjustarZoom aeropuertos={aeropuertos} vuelos={vuelos} />
          
          {/* Aeropuertos */}
          {aeropuertos.map((aeropuerto) => (
            <Marker
              key={aeropuerto.codigoICAO}
              position={[aeropuerto.latitud, aeropuerto.longitud]}
              icon={iconoAeropuerto}
            >
              <Popup>
                <strong>{aeropuerto.nombre}</strong><br />
                <em>{aeropuerto.codigoICAO}</em><br />
                {aeropuerto.ciudad}, {aeropuerto.pais}
              </Popup>
            </Marker>
          ))}
          
          {/* Vuelos */}
          {vuelos.map((vuelo) => (
            <React.Fragment key={vuelo.id}>
              {/* Línea de ruta */}
              <Polyline
                positions={[
                  [vuelo.origenLatitud, vuelo.origenLongitud],
                  [vuelo.destinoLatitud, vuelo.destinoLongitud]
                ]}
                color={vuelo.color}
                weight={3}
                opacity={0.7}
              >
                <Popup>
                  <strong>✈️ {vuelo.origenCodigoICAO} → {vuelo.destinoCodigoICAO}</strong><br />
                  🛫 {vuelo.fechaInicial}<br />
                  🛬 {vuelo.fechaFinal}<br />
                  📦 {vuelo.pedidos?.length || 0} pedidos
                </Popup>
              </Polyline>
            </React.Fragment>
          ))}
        </MapContainer>
      </div>
    </div>
  );
}
