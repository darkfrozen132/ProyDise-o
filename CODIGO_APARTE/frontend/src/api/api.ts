import axios from "axios";

export const API = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  timeout: 60000,
});

API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const msg = error.response.data?.message || "Error en el servidor";
      console.error("API Error:", error.response.status, msg);
      return Promise.reject(new Error(msg));
    } else if (error.request) {
      console.error("Sin respuesta del servidor:", error.request);
      return Promise.reject(new Error("No se recibió respuesta del servidor"));
    } else {
      console.error("Error desconocido:", error.message);
      return Promise.reject(new Error("Error desconocido en la petición"));
    }
  }
);