/**
 * Utilidades relacionadas con la lógica de vuelos
 * para el Simulador Semanal
 */

import { AIRCRAFT_COLORS } from '../constants';

/**
 * Determina el color del icono del avión basado en el estado del vuelo
 * @param {Object} flight - Objeto vuelo con status, progress, retrasado, aircraftColor
 * @returns {string} - Color hexadecimal
 */
export function getAircraftColorByStatus(flight) {
  // Si el vuelo tiene un color personalizado (ej: del WebSocket), usarlo
  if (flight.aircraftColor && flight.aircraftColor !== AIRCRAFT_COLORS.DEFAULT) {
    return flight.aircraftColor;
  }
  
  // Lógica de estado basada en progreso y status
  if (flight.status === 'completed' || flight.progress >= 100) {
    return AIRCRAFT_COLORS.COMPLETED; // Azul - Completado
  }
  
  if (flight.status === 'delayed' || flight.retrasado) {
    return AIRCRAFT_COLORS.DELAYED; // Rojo - Retrasado
  }
  
  // Estado normal basado en progreso
  if (flight.progress > 75) {
    return AIRCRAFT_COLORS.ARRIVING; // Verde claro - Casi llegando
  } else if (flight.progress > 50) {
    return AIRCRAFT_COLORS.IN_ROUTE; // Azul - En ruta
  } else if (flight.progress > 25) {
    return AIRCRAFT_COLORS.STARTING; // Azul claro - Iniciando
  }
  
  return AIRCRAFT_COLORS.DEFAULT; // Azul por defecto
}

/**
 * Crear popup detallado para un vuelo
 * @param {Object} flight - Objeto vuelo con toda la información
 * @returns {string} - HTML del popup
 */
export function createFlightPopup(flight) {
  const statusIcon = 
    flight.status === 'completed' || flight.progress >= 100 ? '✅' :
    flight.status === 'delayed' || flight.retrasado ? '🔴' : '🔵';
  
  const statusText = 
    flight.status === 'completed' || flight.progress >= 100 ? 'Completado' :
    flight.status === 'delayed' || flight.retrasado ? 'Retrasado' : 'En curso';
  
  const color = getAircraftColorByStatus(flight);
  
  return `
    <div style="min-width: 200px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
      <div style="background: linear-gradient(135deg, ${color}dd 0%, ${color}aa 100%); color: white; padding: 8px 12px; margin: -10px -10px 10px -10px; border-radius: 4px 4px 0 0;">
        <strong style="font-size: 15px;">✈️ ${flight.id}</strong>
      </div>
      
      <div style="padding: 4px 0;">
        <div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #e5e7eb;">
          <div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">
            <strong>Ruta:</strong>
          </div>
          <div style="font-size: 14px; font-weight: 600; color: #1f2937;">
            ${flight.origin?.code || 'N/A'} → ${flight.destination?.code || 'N/A'}
          </div>
          ${flight.origin?.region && flight.destination?.region ? 
            `<div style="font-size: 11px; color: #9ca3af; margin-top: 2px;">
              ${flight.isSameContinentFlight ? '🌍 Mismo continente' : '🌏 Intercontinental'}
            </div>` : ''
          }
        </div>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
          <div>
            <div style="font-size: 11px; color: #6b7280;">Progreso</div>
            <div style="font-size: 14px; font-weight: 600; color: #1f2937;">
              ${Math.round(flight.progress || 0)}%
            </div>
          </div>
          <div>
            <div style="font-size: 11px; color: #6b7280;">Estado</div>
            <div style="font-size: 13px; font-weight: 600;">
              ${statusIcon} ${statusText}
            </div>
          </div>
        </div>
        
        ${flight.speed ? 
          `<div style="margin-bottom: 6px;">
            <div style="font-size: 11px; color: #6b7280;">Velocidad</div>
            <div style="font-size: 13px; color: #1f2937;">${flight.speed} km/h</div>
          </div>` : ''
        }
        
        ${flight.altitude ? 
          `<div style="margin-bottom: 6px;">
            <div style="font-size: 11px; color: #6b7280;">Altitud</div>
            <div style="font-size: 13px; color: #1f2937;">${flight.altitude.toLocaleString()} ft</div>
          </div>` : ''
        }
        
        ${flight.packageType ? 
          `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #e5e7eb;">
            <div style="font-size: 11px; color: #6b7280;">Tipo de paquete</div>
            <div style="font-size: 13px; color: #1f2937; font-weight: 500;">
              ${flight.packageType === 'AG' ? '📦 Algoritmo Genetico' : 
                flight.packageType === 'Inicial' ? '🎯 Planificacion Inicial' : 
                flight.packageType}
            </div>
            ${flight.currentPackages !== undefined && flight.packageCapacity !== undefined ?
              `<div style="font-size: 12px; color: #6b7280; margin-top: 2px;">
                Capacidad: ${flight.currentPackages}/${flight.packageCapacity} paquetes
              </div>` : ''
            }
          </div>` : ''
        }
        
        ${flight.pedidoId ? 
          `<div style="margin-top: 6px; font-size: 11px; color: #6b7280;">
            Pedido: <span style="font-family: monospace; color: #1f2937;">${flight.pedidoId}</span>
          </div>` : ''
        }
      </div>
    </div>
  `;
}

/**
 * Normaliza un vuelo recibido del WebSocket al formato interno del simulador
 * @param {Object} vueloWs - Vuelo en formato WebSocket
 * @param {Object} aeropuertosMap - Mapa de aeropuertos por código
 * @returns {Object|null} - Vuelo normalizado o null si inválido
 */
export function normalizeFlightFromWebSocket(vueloWs, aeropuertosMap) {
  const origen = aeropuertosMap[vueloWs.aeropuertoOrigen];
  const destino = aeropuertosMap[vueloWs.aeropuertoDestino];
  
  if (!origen || !destino) {
    console.warn(`[flightUtils] Aeropuerto no encontrado: ${vueloWs.aeropuertoOrigen} -> ${vueloWs.aeropuertoDestino}`);
    return null;
  }
  
  return {
    id: vueloWs.codigoVuelo || `WS-${Date.now()}`,
    origin: {
      code: vueloWs.aeropuertoOrigen,
      lat: origen.latitud,
      lng: origen.longitud,
      name: origen.nombre
    },
    destination: {
      code: vueloWs.aeropuertoDestino,
      lat: destino.latitud,
      lng: destino.longitud,
      name: destino.nombre
    },
    fechaInicial: vueloWs.horaSalida || vueloWs.fechaInicial,
    fechaFinal: vueloWs.horaLlegada || vueloWs.fechaFinal,
    progress: 0,
    status: 'waiting',
    currentLat: origen.latitud,
    currentLng: origen.longitud,
    packageType: vueloWs.tipoEnvio || 'Normal',
    pedidoId: vueloWs.pedidoId
  };
}

/**
 * Verifica si un vuelo está activo en un momento dado
 * @param {Object} vuelo - Objeto vuelo
 * @param {number} tiempoMs - Tiempo actual en milisegundos
 * @returns {boolean} - true si el vuelo está activo
 */
export function isFlightActive(vuelo, tiempoMs) {
  if (!vuelo.fechaInicial || !vuelo.fechaFinal) return false;
  
  const inicio = new Date(vuelo.fechaInicial).getTime();
  const fin = new Date(vuelo.fechaFinal).getTime();
  
  return tiempoMs >= inicio && tiempoMs <= fin;
}

/**
 * Filtra vuelos que están visibles en el tiempo actual
 * @param {Array} vuelos - Array de vuelos
 * @param {number} tiempoMs - Tiempo actual en milisegundos
 * @param {number} ventanaMs - Ventana de tiempo para incluir vuelos próximos (default 30 min)
 * @returns {Array} - Vuelos visibles
 */
export function filterVisibleFlights(vuelos, tiempoMs, ventanaMs = 30 * 60 * 1000) {
  return vuelos.filter(vuelo => {
    if (!vuelo.fechaInicial || !vuelo.fechaFinal) return false;
    
    const inicio = new Date(vuelo.fechaInicial).getTime();
    const fin = new Date(vuelo.fechaFinal).getTime();
    
    // Incluir vuelos activos y próximos a salir
    return tiempoMs >= inicio - ventanaMs && tiempoMs <= fin;
  });
}
