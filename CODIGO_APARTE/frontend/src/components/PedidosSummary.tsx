import { Package, MapPin, Truck, Info } from "lucide-react";
import type { OrderPage } from "../types/order";
import type { PedidoScope } from "../hooks/usePedidos";

interface Props {
  data?: OrderPage;
  scope: PedidoScope;
  pageSize: number;
}

export default function PedidosSummary({ data, scope, pageSize }: Props) {
  const items = data?.items ?? [];
  const totalQuantity = items.reduce((sum, order) => sum + order.quantity, 0);

  const firstOrder = items[items.length - 1];
  const lastOrder = items[0];

  const formatUtc = (value?: string) =>
    value
      ? new Date(value).toLocaleString("es-PE", {
          dateStyle: "medium",
          timeStyle: "short",
          timeZone: "UTC",
        })
      : "—";

  const cards = [
    {
      label: "Pedidos cargados (página)",
      value: items.length,
      icon: <Package className="text-primary" size={22} />,
    },
    {
      label: "Unidades totales (página)",
      value: totalQuantity,
      icon: <MapPin className="text-success" size={22} />,
    },
    {
      label: "Más reciente (UTC)",
      value: formatUtc(lastOrder?.creationUtc),
      icon: <Truck className="text-info" size={22} />,
    },
    {
      label: "Más antiguo (UTC)",
      value: formatUtc(firstOrder?.creationUtc),
      icon: <Info className="text-warning" size={22} />,
    },
  ];

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between text-sm">
        <span className="font-medium">
          Vista: {scope === "REAL" ? "Operativos" : "Proyectados"}
        </span>
        <span className="opacity-70">
          Total global: {(data?.totalElements ?? 0).toLocaleString("es-PE")} ·
          Página {(data?.page ?? 0) + 1} de {data?.totalPages ?? 0} · Tamaño de
          página: {pageSize}
        </span>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((card) => (
          <div
            key={card.label}
            className="card border border-base-300 bg-base-100 shadow-sm"
          >
            <div className="card-body flex flex-row items-center justify-between p-4">
              <div className="flex flex-col">
                <span className="text-sm opacity-70">{card.label}</span>
                <span className="text-xl font-bold">{card.value}</span>
              </div>
              <div className="flex items-center">{card.icon}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
