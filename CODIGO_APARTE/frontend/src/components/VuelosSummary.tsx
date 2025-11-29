import { Plane, Building, Globe, BarChart2 } from "lucide-react";
import { useFlights } from "../hooks/useFlights";
import type { Vuelo } from "../types/vuelo";

export default function VuelosSummary() {
  // 1. Usa el hook de vuelos
  const { list } = useFlights();

  if (list.isLoading)
    return <div className="p-4 text-center">Cargando resumen de vuelos...</div>;

  if (list.isError)
    return <div className="p-4 text-center text-error">Error al cargar resumen.</div>;

  // 2. Lógica de cálculo de estadísticas
  const vuelos: Vuelo[] = list.data ?? [];
  const activos = vuelos.filter((v) => v.estado === "en vuelo").length;
  // ⚠️ Esta lógica asume que tu tipo 'Vuelo' incluye 'origenContinente' y 'destinoContinente'
  const intra = vuelos.filter((v) => v.origenContinente && v.destinoContinente && v.origenContinente === v.destinoContinente).length;
  const inter = vuelos.filter((v) => v.origenContinente && v.destinoContinente && v.origenContinente !== v.destinoContinente).length;
  const total = vuelos.length;

  // 3. Define las tarjetas
  const cards = [
    {
      label: "Vuelos Activos",
      value: activos,
      icon: <Plane className="text-primary" size={22} />,
    },
    {
      label: "Intracontinentales",
      value: intra,
      icon: <Building className="text-success" size={22} />,
    },
    {
      label: "Intercontinentales",
      value: inter,
      icon: <Globe className="text-info" size={22} />,
    },
    {
      label: "Total Vuelos",
      value: total,
      icon: <BarChart2 className="text-base-content/30" size={22} />,
    },
  ];

  // 4. Renderiza el JSX
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      {cards.map((c) => (
        <div
          key={c.label}
          className="card border border-base-300 bg-base-100 shadow-sm"
        >
          <div className="card-body flex flex-row items-center justify-between p-4">
            <div className="flex flex-col">
              <span className="text-sm opacity-70">{c.label}</span>
              <span className="text-2xl font-bold">{c.value}</span>
            </div>
            <div className="flex items-center">{c.icon}</div>
          </div>
        </div>
      ))}
    </div>
  );
}
