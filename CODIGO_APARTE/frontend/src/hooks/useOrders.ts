import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { orderService } from "../services/orderService";
import type { Order } from "../types/order";
import type { OrderRequest } from "../types/orderRequest";

// Claves de caché para orders
const keys = {
  all: ["orders"] as const,
  one: (id: string) => ["orders", id] as const,
};

export function useOrders() {
  const queryClient = useQueryClient();

  // Obtener todos los orders
  const list = useQuery<Order[]>({
    queryKey: keys.all,
    queryFn: () => orderService.getAll(),
  });

  //Crear order
  const create = useMutation({
    mutationFn: orderService.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.all });
    },
  });

  //Actualizar order
  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: OrderRequest }) =>
      orderService.update(id, data),
    onSuccess: (_, variables) => {
      // Refresca tanto la lista como el detalle editado
      queryClient.invalidateQueries({ queryKey: keys.all });
      queryClient.invalidateQueries({ queryKey: keys.one(variables.id) });
    },
  });

  //Eliminar order
  const remove = useMutation({
    mutationFn: orderService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.all });
    },
  });

  return { list, create, update, remove };
}

// Hook para un solo pedido a "useOrder"
export function useOrder(id: string) {
  return useQuery<Order>({
    queryKey: keys.one(id),
    queryFn: () => orderService.getOne(id),
    enabled: !!id,
  });
}
