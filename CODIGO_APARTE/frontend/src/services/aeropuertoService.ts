import { API } from "../api/api";
import type { Airport } from "../types/airport";

export const aeropuertoService = {
  getAll: () => API.get<Airport[]>("/base/airports").then(res => res.data),
};