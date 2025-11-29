import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query";
import { orderService } from "../services/orderService";
import type { OrderRequest } from "../types/orderRequest";
import type { OrderPage } from "../types/order";

export type PedidoScope = "REAL" | "PROJECTED";

const keys = {
  list: (scope: PedidoScope, page: number, size: number) =>
    ["pedidos", scope, page, size] as const,
};

export function usePedidos(scope: PedidoScope, page: number, size: number) {
  const queryClient = useQueryClient();
  const queryKey = keys.list(scope, page, size);

  const list = useQuery<OrderPage>({
    queryKey,
    queryFn: () => orderService.getPage(scope, page, size),
    placeholderData: keepPreviousData,
  });

  const create = useMutation({
    mutationFn: (request: OrderRequest) => orderService.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const remove = useMutation({
    mutationFn: orderService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return { list, create, remove };
}
