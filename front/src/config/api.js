import axios from 'axios';


const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para requests
api.interceptors.request.use(
  (config) => {
    // Aquí puedes agregar tokens de autenticación si los necesitas
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para responses
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Manejo de errores global
    if (error.response) {
      // El servidor respondió con un código de error
      console.error('Error de respuesta:', error.response.status);
    } else if (error.request) {
      // La petición se hizo pero no hubo respuesta
      console.error('Error de conexión: No se pudo conectar al servidor');
    } else {
      // Algo sucedió al configurar la petición
      console.error('Error:', error.message);
    }
    return Promise.reject(error);
  }
);

// Función para transformar datos de aeropuertos del backend al formato del frontend
export const transformAirportData = (backendAirport) => {
  console.log('Transformando aeropuerto:', backendAirport);
  
  const transformed = {
    name: `${backendAirport.ciudad}`,
    code: backendAirport.codigoICAO,
    lat: backendAirport.latitud,
    lng: backendAirport.longitud,
    capacity: backendAirport.capacidadAlmacen,
    packages: backendAirport.capacidadAlmacen - backendAirport.capacidadDisponible,
    isSede: false, // Ajustar según lógica de negocio
    region: backendAirport.continente,
    country: backendAirport.pais,
    operationType: 'Aeropuerto Regional', // Ajustar según lógica de negocio
    timezone: backendAirport.husoHorario
  };
  
  console.log('Aeropuerto transformado:', transformed);
  return transformed;
};

// Función para obtener aeropuertos transformados
export const getAirports = async () => {
  try {
    console.log('Solicitando aeropuertos a:', API_BASE_URL + '/api/aeropuertos/listar');
    const response = await api.get('/api/aeropuertos/listar');
    console.log('Respuesta recibida:', response.data);
    const transformed = response.data.map(transformAirportData);
    console.log('Aeropuertos transformados:', transformed);
    return transformed;
  } catch (error) {
    console.error('Error al obtener aeropuertos:', error);
    throw error;
  }
};

// Función para transformar datos de vuelos del backend al formato del frontend
export const transformFlightData = (backendFlight, airports) => {
  console.log('Transformando vuelo:', backendFlight);
  
  // Buscar aeropuerto de origen por código
  const originAirport = airports.find(a => a.code === backendFlight.originCode);
  const destinationAirport = airports.find(a => a.code === backendFlight.destinationCode);
  
  if (!originAirport || !destinationAirport) {
    console.warn('⚠️ No se encontraron aeropuertos para el vuelo:', backendFlight);
    return null;
  }

  // Calcular progreso del vuelo (de 0.0 a 1.0)
  const progress = backendFlight.progress || 0.5; // Por defecto a la mitad si no viene

  // Calcular posición actual basado en el progreso
  const deltaLat = destinationAirport.lat - originAirport.lat;
  const deltaLng = destinationAirport.lng - originAirport.lng;
  const currentLat = originAirport.lat + (deltaLat * progress);
  const currentLng = originAirport.lng + (deltaLng * progress);

  // Calcular rotación del avión
  const rotation = Math.atan2(deltaLng, deltaLat) * (180 / Math.PI);

  // Determinar color según capacidad
  const loadPercentage = (backendFlight.totalPaquetes / backendFlight.capacidad) * 100;
  let aircraftColor;
  if (loadPercentage >= 80) aircraftColor = '#dc3545'; // Rojo
  else if (loadPercentage >= 50) aircraftColor = '#ffc107'; // Amarillo
  else aircraftColor = '#28a745'; // Verde

  // Mapear tipo de avión (ajustar según tu lógica)
  const aircraftTypeMap = {
    'EBCI': 'cargo',
    'default': 'boeing737'
  };
  const aircraftType = aircraftTypeMap[backendFlight.originCode] || 'boeing737';

  const transformed = {
    id: backendFlight.id,
    origin: originAirport,
    destination: destinationAirport,
    currentLat: currentLat,
    currentLng: currentLng,
    altitude: backendFlight.altitude || 35000,
    speed: backendFlight.speed || 500,
    progress: progress,
    aircraftType: aircraftType,
    aircraftColor: aircraftColor,
    rotation: rotation,
    packageCapacity: backendFlight.capacidad,
    currentPackages: backendFlight.totalPaquetes,
    status: 'active',
    departureTime: backendFlight.salida,
    arrivalTime: backendFlight.llegada,
    route: backendFlight.ruta,
    orders: backendFlight.orders || [],
    regionOrigin: backendFlight.regionOrigin,
    regionDestination: backendFlight.regionDestination,
    isSameContinentFlight: backendFlight.regionOrigin === backendFlight.regionDestination
  };
  
  console.log('Vuelo transformado:', transformed);
  return transformed;
};

// Función para obtener vuelos transformados
export const getFlights = async (airports) => {
  try {
    console.log('Solicitando vuelos a:', API_BASE_URL + '/vuelos/listar');
    const response = await api.get('/vuelos/listar');
    console.log('Respuesta de vuelos recibida:', response.data);
    
    if (!response.data || !response.data.vuelos) {
      console.warn('⚠️ No se encontraron vuelos en la respuesta');
      return [];
    }
    
    const transformed = response.data.vuelos
      .map(flight => transformFlightData(flight, airports))
      .filter(flight => flight !== null); // Filtrar vuelos inválidos
    
    console.log('Vuelos transformados:', transformed.length, 'vuelos');
    return transformed;
  } catch (error) {
    console.error('Error al obtener vuelos:', error);
    throw error;
  }
};

// ==================== FUNCIONES PARA SSE (SIMULACIÓN TIEMPO REAL) ====================

// Iniciar simulación
export const iniciarSimulacion = async () => {
  try {
    console.log('🚀 Iniciando simulación...');
    const response = await api.post('/simulacion/iniciar');
    console.log('✅ Simulación iniciada:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error al iniciar simulación:', error);
    throw error;
  }
};

// Pausar simulación
export const pausarSimulacion = async () => {
  try {
    console.log('⏸️ Pausando simulación...');
    const response = await api.post('/simulacion/pausar');
    console.log('✅ Simulación pausada:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error al pausar simulación:', error);
    throw error;
  }
};

// Reanudar simulación
export const reanudarSimulacion = async () => {
  try {
    console.log('▶️ Reanudando simulación...');
    const response = await api.post('/simulacion/reanudar');
    console.log('✅ Simulación reanudada:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error al reanudar simulación:', error);
    throw error;
  }
};

// Detener simulación
export const detenerSimulacion = async () => {
  try {
    console.log('⏹️ Deteniendo simulación...');
    const response = await api.post('/simulacion/detener');
    console.log('✅ Simulación detenida:', response.data);
    return response.data;
  } catch (error) {
    console.error('❌ Error al detener simulación:', error);
    throw error;
  }
};

// Obtener estado actual de la simulación (sin stream)
export const obtenerEstadoSimulacion = async () => {
  try {
    const response = await api.get('/simulacion/estado');
    return response.data;
  } catch (error) {
    console.error('❌ Error al obtener estado de simulación:', error);
    throw error;
  }
};

// Conectar al stream SSE de la simulación
export const conectarStreamSimulacion = (onMessage, onError) => {
  const eventSource = new EventSource(`${API_BASE_URL}/simulacion/stream`);
  
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('📡 SSE - Estado recibido:', data);
      onMessage(data);
    } catch (error) {
      console.error('❌ Error al parsear SSE:', error);
    }
  };
  
  eventSource.onerror = (error) => {
    console.error('❌ Error en SSE:', error);
    if (onError) onError(error);
  };
  
  return eventSource;
};

export default api;
export { API_BASE_URL };  
