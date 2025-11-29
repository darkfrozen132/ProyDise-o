import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet';
import L from 'leaflet';
import type { LatLngExpression } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Airport } from '../../types/airport';
import type { SegmentoVuelo, VueloEnMovimiento } from '../../hooks/useSimulacion';
import MapResizer from './MapResizer';

const hubIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const PLANE_ICON_SIZE = 25;
const PLANE_ICON_ANCHOR = [PLANE_ICON_SIZE / 2, PLANE_ICON_SIZE / 2] as [number, number];
const PLANE_ICON_BASE_ROTATION = 45; // imagen apunta 45° hacia arriba por defecto

const planeIconPaths: Record<'completado' | 'retrasado' | 'default', string> = {
  completado: '/images/plane-icon-green.png',
  retrasado: '/images/plane-icon-red.png',
  default: '/images/plane-icon-blue.png',
};

const createRotatedPlaneIcon = (status: VueloEnMovimiento['estadoVisual'], rotation: number) => {
  const iconUrl = status === 'completado'
    ? planeIconPaths.completado
    : status === 'retrasado'
      ? planeIconPaths.retrasado
      : planeIconPaths.default;

  const adjustedRotation = rotation - PLANE_ICON_BASE_ROTATION;
  return L.divIcon({
    html: `<img src="${iconUrl}" style="transform: rotate(${adjustedRotation.toFixed(2)}deg); width: ${PLANE_ICON_SIZE}px; height: ${PLANE_ICON_SIZE}px;" alt="avión" />`,
    className: 'plane-marker-icon',
    iconSize: [PLANE_ICON_SIZE, PLANE_ICON_SIZE],
    iconAnchor: PLANE_ICON_ANCHOR,
  });
};

const calculateBearing = (fromLat: number, fromLon: number, toLat: number, toLon: number) => {
  const fromLatRad = (fromLat * Math.PI) / 180;
  const toLatRad = (toLat * Math.PI) / 180;
  const deltaLonRad = ((toLon - fromLon) * Math.PI) / 180;

  const y = Math.sin(deltaLonRad) * Math.cos(toLatRad);
  const x =
    Math.cos(fromLatRad) * Math.sin(toLatRad) -
    Math.sin(fromLatRad) * Math.cos(toLatRad) * Math.cos(deltaLonRad);

  const rawBearing = (Math.atan2(y, x) * 180) / Math.PI;
  if (Number.isNaN(rawBearing)) {
    return 0;
  }
  return (rawBearing + 360) % 360;
};

interface MapaVuelosProps {
  activeSegments: SegmentoVuelo[];
  aeropuertos: Airport[];
  isLoading: boolean;
  vuelosEnMovimiento: VueloEnMovimiento[];
  filtroHubActivo: string;
}

export function MapaVuelos({ activeSegments, aeropuertos, isLoading, vuelosEnMovimiento, filtroHubActivo }: MapaVuelosProps) {
  const initialPosition: LatLngExpression = [20, 0];

  //console.log('🗺️ OrderPlans recibidos:', orderPlans);
  //console.log('🗺️ Aeropuertos recibidos:', aeropuertos);
  //console.log('🗺️ Vuelos en movimiento recibidos:', vuelosEnMovimiento);
  //console.log('🗺️ CANTIDAD de vuelos en movimiento:', vuelosEnMovimiento.length);

  // Crea el directorio de coordenadas
  const coordsAeropuertos = new Map<string, LatLngExpression>(
    aeropuertos
      .filter((a) => typeof a.latitude === 'number' && typeof a.longitude === 'number')
      .map((a) => [a.id, [a.latitude, a.longitude]])
  );

  return (
    <MapContainer
      center={initialPosition}
      zoom={3}
      className="w-full h-full z-0"
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        noWrap={true}
      />

      <MapResizer isLoading={isLoading} />

      {/* LEYENDA */}
      <div className="absolute bottom-4 left-4 z-[1000] card bg-base-100 p-2 shadow-lg">
        <h4 className="font-bold mb-1">Leyenda</h4>
        <ul className="text-sm space-y-1">
          <li className="flex items-center gap-2">
            <img src="/images/plane-icon-blue.png" alt="avión azul" className="w-4 h-4" /> En curso
          </li>
          <li className="flex items-center gap-2">
            <img src="/images/plane-icon-green.png" alt="avión verde" className="w-4 h-4" /> Finalizado
          </li>
          <li className="flex items-center gap-2">
            <img src="/images/plane-icon-red.png" alt="avión rojo" className="w-4 h-4" /> Retrasado
          </li>
        </ul>
      </div>

      {/* Dibuja las sedes principales */}
      {aeropuertos
        .slice(0, 30)
        .map(aeropuerto => (
          <Marker
            key={aeropuerto.id}
            position={[aeropuerto.latitude, aeropuerto.longitude]}
            icon={hubIcon}
            opacity={!filtroHubActivo || filtroHubActivo === aeropuerto.id ? 1.0 : 0.3}
          >
            <Popup>
              <strong>{aeropuerto.id}</strong><br />
              {aeropuerto.name}<br />
              Sede Principal
            </Popup>
          </Marker>
        ))}

      {/* Dibuja las RUTAS EN CURSO */}
      {activeSegments.map((segmento) => {
        const origenCoords = coordsAeropuertos.get(segmento.origin);
        const destinoCoords = coordsAeropuertos.get(segmento.destination);
        if (!origenCoords || !destinoCoords) return null;

        const esRetrasado = segmento.retrasado;
        let colorRuta = esRetrasado ? "#FF0000" : "#3b82f6";
        let opacidadRuta = 0.7;
        let dashRuta: string | undefined = "5, 10";

        if (filtroHubActivo) {
          if (segmento.origin === filtroHubActivo) {
            colorRuta = "#FFA500";
            opacidadRuta = 1.0;
            dashRuta = undefined;
          } else {
            colorRuta = "#6b7280";
            opacidadRuta = 0.2;
          }
        }

        return (
          <Polyline
            key={segmento.id}
            positions={[origenCoords, destinoCoords]}
            color={colorRuta}
            weight={filtroHubActivo && segmento.origin === filtroHubActivo ? 3 : 2}
            opacity={opacidadRuta}
            dashArray={dashRuta}
          />
        );
      })}

      {/* Dibuja los AVIONES en movimiento usando ID ÚNICO */}
      {vuelosEnMovimiento?.map((vuelo) => {
        const origenCoords = coordsAeropuertos.get(vuelo.origen);
        const destinoCodigo = vuelo.destinoActual ?? vuelo.destino;
        const destinoCoords = coordsAeropuertos.get(destinoCodigo);

        if (!origenCoords || !destinoCoords) {
          return null;
        }

        if (isNaN(vuelo.latActual) || isNaN(vuelo.lonActual)) {
          return null;
        }

        // Atenúa los aviones que NO salieron del hub seleccionado
        let opacidadAvion = 1.0;
        if (filtroHubActivo && vuelo.origen !== filtroHubActivo) {
          opacidadAvion = 0.2;
        }

        const [destinoLat, destinoLon] = destinoCoords as [number, number];
        const bearing = calculateBearing(vuelo.latActual, vuelo.lonActual, destinoLat, destinoLon);

        return (
          <Marker
            key={vuelo.id}
            position={[vuelo.latActual, vuelo.lonActual]}
            icon={createRotatedPlaneIcon(vuelo.estadoVisual, bearing)}
            opacity={opacidadAvion}
          >
            <Popup>
              <div className="text-sm">
                <strong>Vuelo: {vuelo.flightId}</strong><br/>
                Ruta: {vuelo.origen} → {vuelo.destinoActual ?? vuelo.destino}<br/>
                {vuelo.departureTime && (
                  <>Salida: {new Date(vuelo.departureTime).toLocaleTimeString('es-PE', {
                    hour: '2-digit',
                    minute: '2-digit',
                    timeZone: 'UTC'
                  })}<br/></>
                )}
                {vuelo.arrivalTime && (
                  <>Llegada: {new Date(vuelo.arrivalTime).toLocaleTimeString('es-PE', {
                    hour: '2-digit',
                    minute: '2-digit',
                    timeZone: 'UTC'
                  })}<br/></>
                )}
                Progreso: {Math.round(vuelo.progreso)}%<br/>
                Estado: {vuelo.estadoVisual === 'completado' ? '✅ Completado' :
                         vuelo.estadoVisual === 'retrasado' ? '🔴 Retrasado' : '🔵 En curso'}<br/>
                <br/>
                <strong>Pedidos transportados ({vuelo.orderId.split(',').length}):</strong><br/>
                <div className="text-xs max-h-20 overflow-y-auto">
                  {vuelo.orderId.split(',').map(pedido => (
                    <div key={pedido}>• {pedido}</div>
                  ))}
                </div>
              </div>
            </Popup>
          </Marker>
        );
      })}
    </MapContainer>
  );
}
