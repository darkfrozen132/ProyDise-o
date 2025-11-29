import { API } from "../api/api";
import type { SimulationStartResponse, SimulationStartRequest } from "../types/simulation";

const startSimulation = async (request: SimulationStartRequest): Promise<SimulationStartResponse> => {
  const res = await API.post<SimulationStartResponse>("/simulations", request);
  return res.data;
};

const cancelSimulation = async (simulationId: string): Promise<void> => {
  await API.delete(`/simulations/${simulationId}`);
};

export const simulacionService = {
  startSimulation,
  cancelSimulation,
};
