import { API } from "../api/api";
import type { Order, OrderPage } from "../types/order";
import type { OrderRequest } from "../types/orderRequest";

const fetchPage = (
  scope: "REAL" | "PROJECTED",
  page: number,
  size: number
) => {
  const params = new URLSearchParams();
  params.append("scope", scope);
  params.append("page", String(page));
  params.append("size", String(size));
  const suffix = `?${params.toString()}`;
  return API.get<OrderPage>(`/orders${suffix}`).then((res) => res.data);
};

export const orderService = {
  getPage: fetchPage,
  getAll: (scope: "REAL" | "PROJECTED" = "REAL", size = 500) =>
    fetchPage(scope, 0, size).then((page) => page.items),
  count: (scope: 'REAL' | 'PROJECTED' = 'REAL') =>
    API.get<{ scope: 'REAL' | 'PROJECTED'; total: number }>(
      `/orders/count?scope=${scope}`
    ).then(res => res.data.total),
  getOne: (id: string) => API.get<Order>(`/orders/${id}`).then(res => res.data),
  create: (request: OrderRequest) => API.post<Order>("/orders", request).then(res => res.data),
  update: (id: string, request: OrderRequest) =>
    API.put<Order>(`/orders/${id}`, request).then(res => res.data),
  delete: (id: string) => API.delete(`/orders/${id}`),
  createProjectedBatch: async (orders: OrderRequest[]) => {
    if (!orders.length) return [];
    const payload = {
      orders: orders.map(order => ({ ...order, projected: true })),
    };
    const res = await API.post<Order[]>("/orders/batch", payload);
    return res.data;
  },
};
