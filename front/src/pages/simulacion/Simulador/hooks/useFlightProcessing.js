/**
 * Hook useFlightProcessing
 * Maneja el procesamiento de vuelos recibidos del WebSocket
 * Incluye procesamiento de segments de snapshot y vuelos directos
 */

import { useCallback, useRef } from 'react';
import { bearingDegrees } from '../utils/timeUtils';

/**
 * Hook para procesar vuelos del WebSocket
 * @param {Object} options - Opciones de configuración
 * @param {Array} options.airports - Lista de aeropuertos disponibles
 * @param {Function} options.setFlights - Setter para actualizar vuelos
 * @param {Function} options.onTiempoSimuladoInit - Callback cuando se inicializa el tiempo
 * @returns {Object} - Funciones de procesamiento
 */
export function useFlightProcessing(options = {}) {
  const { 
    airports = [], 
    setFlights,
    onTiempoSimuladoInit
  } = options;
  
  // Referencias para evitar stale closures
  const airportsRef = useRef(airports);
  const contadorVuelosRef = useRef(0);
  
  // Actualizar ref cuando cambian airports
  airportsRef.current = airports;

  /**
   * Procesar segments del snapshot (nueva estructura JSON del backend)
   * Extrae todos los segments de orderPlans → routes → segments
   * @param {Object} snapshot - Snapshot del backend
   */
  const procesarSegmentsSnapshot = useCallback((snapshot) => {
    const currentAirports = airportsRef.current;
    console.log(`\n[useFlightProcessing] procesarSegmentsSnapshot - Snapshot recibido`);
    console.log(`   Aeropuertos disponibles: ${currentAirports.length}`);
    console.log(`   Tiempo simulado: ${snapshot.generatedAt}`);
    
    if (!snapshot.orderPlans || snapshot.orderPlans.length === 0) {
      console.warn('[useFlightProcessing] No hay orderPlans en el snapshot');
      return [];
    }
    
    const nuevosVuelos = [];
    let segmentIndex = 0;
    
    // Iterar por cada orderPlan
    snapshot.orderPlans.forEach((orderPlan, orderIndex) => {
      const orderId = orderPlan.orderId;
      const orderSlackMinutes = orderPlan.slackMinutes;
      
      console.log(`\n   Pedido ${orderIndex + 1}/${snapshot.orderPlans.length}: ${orderId}`);
      console.log(`      Holgura: ${orderSlackMinutes} minutos ${orderSlackMinutes > 0 ? 'OK' : 'RETRASADO'}`);
      
      // Iterar por cada ruta del pedido
      orderPlan.routes.forEach((route, routeIndex) => {
        console.log(`      Ruta ${routeIndex + 1}: ${route.segments.length} segmentos`);
        
        // Iterar por cada segment (VUELO) de la ruta
        route.segments.forEach((segment, segIndex) => {
          segmentIndex++;
          
          // Buscar aeropuertos de origen y destino
          const origen = currentAirports.find(a => 
            a.code.toUpperCase() === segment.origin.toUpperCase()
          );
          const destino = currentAirports.find(a => 
            a.code.toUpperCase() === segment.destination.toUpperCase()
          );
          
          if (!origen) {
            console.error(`[useFlightProcessing] Aeropuerto ORIGEN no encontrado: "${segment.origin}"`);
            return;
          }
          
          if (!destino) {
            console.error(`[useFlightProcessing] Aeropuerto DESTINO no encontrado: "${segment.destination}"`);
            return;
          }
          
          // Calcular rotación del avión
          const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
          const rotation = (brg - 90 + 360) % 360;
          
          // Determinar estado basado en holgura
          let status = 'active';
          if (orderSlackMinutes <= 0) {
            status = 'retrasado';
          }
          
          // Crear ID único
          const uniqueId = `SNAP-${segment.flightId}-${orderId}-${routeIndex}-${segIndex}-${segmentIndex}-${Math.random().toString(36).substr(2, 6)}`;
          
          // Crear objeto de vuelo con timestamps del segment
          const nuevoVuelo = {
            id: uniqueId,
            flightId: segment.flightId,
            origin: {
              code: segment.origin,
              lat: origen.lat,
              lng: origen.lng,
              region: origen.region
            },
            destination: {
              code: segment.destination,
              lat: destino.lat,
              lng: destino.lng,
              region: destino.region
            },
            // TIMESTAMPS PARA INTERPOLACION HIBRIDA
            fechaInicial: segment.departureUtc,
            fechaFinal: segment.arrivalUtc,
            // Información del pedido
            pedidoId: orderId,
            slackMinutes: orderSlackMinutes,
            // Posición y estado inicial
            progress: 0,
            currentLat: origen.lat,
            currentLng: origen.lng,
            rotation,
            status,
            // Metadatos
            altitude: 35000,
            speed: 850,
            packageCapacity: segment.quantity,
            currentPackages: segment.quantity,
            packageType: 'SNAPSHOT',
            isSameContinentFlight: origen.region === destino.region,
            aircraftColor: orderSlackMinutes <= 0 ? '#ef4444' : '#3b82f6', // Rojo si retrasado
          };
          
          nuevosVuelos.push(nuevoVuelo);
        });
      });
    });
    
    console.log(`\n[useFlightProcessing] Total segments: ${segmentIndex}, vuelos creados: ${nuevosVuelos.length}`);
    
    if (nuevosVuelos.length > 0) {
      // Eliminar duplicados usando Map
      const flightsMap = new Map(nuevosVuelos.map(v => [v.id, v]));
      const vuelosUnicos = Array.from(flightsMap.values());
      
      if (vuelosUnicos.length < nuevosVuelos.length) {
        console.warn(`[useFlightProcessing] ${nuevosVuelos.length - vuelosUnicos.length} duplicados eliminados`);
      }
      
      // Actualizar estado
      if (setFlights) {
        setFlights(vuelosUnicos);
      }
      
      // Auto-inicializar tiempo si es necesario
      if (onTiempoSimuladoInit) {
        const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
        if (vuelosConFecha.length > 0) {
          const fechaMasTemprana = Math.min(
            ...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime())
          );
          onTiempoSimuladoInit(fechaMasTemprana);
        }
      }
      
      return vuelosUnicos;
    }
    
    return [];
  }, [setFlights, onTiempoSimuladoInit]);

  /**
   * Procesar vuelos directos (formato alternativo del backend)
   * Formato: {origenCodigoICAO, destinoCodigoICAO, fechaInicial, fechaFinal, totalPaquetes}
   * @param {Array} vuelos - Array de vuelos del backend
   */
  const procesarVuelosDirectos = useCallback((vuelos) => {
    const currentAirports = airportsRef.current;
    console.log(`\n[useFlightProcessing] procesarVuelosDirectos - Recibidos ${vuelos?.length || 0} vuelos`);
    console.log(`   Aeropuertos disponibles: ${currentAirports.length}`);
    
    if (!vuelos || vuelos.length === 0) {
      console.warn('[useFlightProcessing] No hay vuelos para procesar');
      return [];
    }
    
    const nuevosVuelos = [];
    const baseTimestamp = Date.now();

    vuelos.forEach((vuelo, index) => {
      // Buscar aeropuertos de origen y destino (case-insensitive)
      const origen = currentAirports.find(a => 
        a.code.toUpperCase() === vuelo.origenCodigoICAO.toUpperCase()
      );
      const destino = currentAirports.find(a => 
        a.code.toUpperCase() === vuelo.destinoCodigoICAO.toUpperCase()
      );

      if (!origen) {
        console.error(`[useFlightProcessing] Aeropuerto ORIGEN no encontrado: "${vuelo.origenCodigoICAO}"`);
        return;
      }

      if (!destino) {
        console.error(`[useFlightProcessing] Aeropuerto DESTINO no encontrado: "${vuelo.destinoCodigoICAO}"`);
        return;
      }

      // Calcular rotación del avión
      const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
      const rotation = (brg - 90 + 360) % 360;

      // Generar ID único
      contadorVuelosRef.current += 1;
      const uniqueId = `DIR-${index}-${baseTimestamp}-${contadorVuelosRef.current}-${Math.random().toString(36).substr(2, 6)}`;

      // Crear objeto de vuelo
      const nuevoVuelo = {
        id: uniqueId,
        origin: {
          code: vuelo.origenCodigoICAO,
          lat: origen.lat,
          lng: origen.lng,
          region: origen.region
        },
        destination: {
          code: vuelo.destinoCodigoICAO,
          lat: destino.lat,
          lng: destino.lng,
          region: destino.region
        },
        // TIMESTAMPS
        fechaInicial: vuelo.fechaInicial,
        fechaFinal: vuelo.fechaFinal,
        // Posición y estado inicial
        progress: 0,
        currentLat: origen.lat,
        currentLng: origen.lng,
        rotation,
        status: 'active',
        // Metadatos
        altitude: 35000,
        speed: 850,
        packageCapacity: vuelo.totalPaquetes || 0,
        currentPackages: vuelo.totalPaquetes || 0,
        packageType: 'DIRECTO',
        isSameContinentFlight: origen.region === destino.region,
        aircraftColor: '#3b82f6',
        // Info de pedidos
        pedidos: vuelo.pedidos || []
      };

      nuevosVuelos.push(nuevoVuelo);
    });

    console.log(`[useFlightProcessing] ${nuevosVuelos.length} vuelos directos creados`);

    if (nuevosVuelos.length > 0 && setFlights) {
      // Agregar a vuelos existentes o reemplazar
      setFlights(prev => {
        const existingIds = new Set(prev.map(v => v.id));
        const vuelosNuevos = nuevosVuelos.filter(v => !existingIds.has(v.id));
        return [...prev, ...vuelosNuevos];
      });
      
      // Auto-inicializar tiempo si es necesario
      if (onTiempoSimuladoInit) {
        const vuelosConFecha = nuevosVuelos.filter(v => v.fechaInicial);
        if (vuelosConFecha.length > 0) {
          const fechaMasTemprana = Math.min(
            ...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime())
          );
          onTiempoSimuladoInit(fechaMasTemprana);
        }
      }
    }

    return nuevosVuelos;
  }, [setFlights, onTiempoSimuladoInit]);

  /**
   * Limpiar todos los vuelos
   */
  const limpiarVuelos = useCallback(() => {
    if (setFlights) {
      setFlights([]);
    }
    contadorVuelosRef.current = 0;
    console.log('[useFlightProcessing] Vuelos limpiados');
  }, [setFlights]);

  /**
   * Resetear contador de IDs
   */
  const resetearContador = useCallback(() => {
    contadorVuelosRef.current = 0;
  }, []);

  return {
    // Funciones de procesamiento
    procesarSegmentsSnapshot,
    procesarVuelosDirectos,
    limpiarVuelos,
    resetearContador,
    
    // Referencias (para uso avanzado)
    airportsRef,
    contadorVuelosRef
  };
}

export default useFlightProcessing;
