import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { flightService } from "../services/flightService";
import type { Vuelo } from "../types/vuelo";

// 1. Claves de caché para gestionar los datos de vuelos
const flightKeys = {
  all: ["flights"] as const,
};

// 2. Hook principal para gestionar la lista y la creación de vuelos
export function useFlights() {
  const queryClient = useQueryClient();

  // Obtener todos los vuelos
  const list = useQuery<Vuelo[]>({
    queryKey: flightKeys.all,
    queryFn: flightService.getAll,
  });

  // Crear un nuevo vuelo
  const create = useMutation({
    mutationFn: flightService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: flightKeys.all });
    },
  });

  return { list, create };
}
