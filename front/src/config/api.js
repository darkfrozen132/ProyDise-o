import axios from 'axios';

const API_BASE_URL = 'http://127.0.0.1:8080/api';

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
    console.log('Solicitando aeropuertos a:', API_BASE_URL + '/aeropuertos/listar');
    const response = await api.get('/aeropuertos/listar');
    console.log('Respuesta recibida:', response.data);
    const transformed = response.data.map(transformAirportData);
    console.log('Aeropuertos transformados:', transformed);
    return transformed;
  } catch (error) {
    console.error('Error al obtener aeropuertos:', error);
    throw error;
  }
};

export default api;
export { API_BASE_URL };
