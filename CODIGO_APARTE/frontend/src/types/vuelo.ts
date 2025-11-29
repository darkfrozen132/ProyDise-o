export type EstadoVuelo = "en vuelo" | "programado" | "completado" | "retrasado";

export interface Vuelo {
  id: number;
  codigoVuelo?: string;
  aeronave?: string;
  origen: string;
  destino: string;
  origenContinente?: string;
  destinoContinente?: string;
  salidaLocalMin: number;
  llegadaLocalMin: number;
  distanciaKm?: number;
  capacidad: number;
  ocupacionActual?: number;
  estado?: EstadoVuelo;
}
