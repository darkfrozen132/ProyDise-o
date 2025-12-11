import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer } from 'react-leaflet';
import { Drawer, IconButton, Tabs, Tab, Box } from '@mui/material';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import vuelosSemana from '../../../assets/data/vuelosSemana.json';
import { API_BASE_URL, WS_URL } from '../../../config/api';
import './SimuladorSemanal.css';
import './WebSocketStomp.css';
import { getAirports } from '../../../config/api';
import LegendDialog from '../../../components/ui/Dialog/LegendDialog';
import LegendButton from '../../../components/ui/Button/LegendButton';
import MetricsPopper from '../../../components/ui/Dialog/MetricsPopper';
import MetricsButton from '../../../components/ui/Button/MetricsButton';
import ControlButton from '../../../components/ui/Button/ControlButton';
import ControlPopper from '../../../components/ui/Dialog/ControlPopper';

import { IoMdAirplane } from "react-icons/io";
import ReactDOMServer from "react-dom/server";

/* Constantes de configuracion de tiempo de simulacion */
const DESIRED_TIME_SCALE = 300; // Valor de K
const TIEMPO_RECOGIDA_MS = 2 * 60 * 60 * 1000; // 2 horas en milisegundos - tiempo para recoger paquetes del almacén

/* Reparar iconos por defecto de Leaflet */
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
	iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
	iconUrl: require('leaflet/dist/images/marker-icon.png'),
	shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

/* ======= Obtener color del avión según estado ======= */
const getAircraftColorByStatus = (flight) => {
	const capacidad = flight.packageCapacity || 1; // Evitar división por 0
	const cargaActual = flight.currentPackages || 0;
	const porcentajeCarga = (cargaActual / capacidad) * 100;

	// Colores según porcentaje de carga
	if (porcentajeCarga >= 80) {
		return '#dc3545';
	} else if (porcentajeCarga >= 50) {
		return '#f59e0b';
	} else {
		return '#28a745';
	}
};

/* ======= Obtener SVG del icono de avión con color dinámico ======= */
const getAirplaneSvg = (color, size = 0) => {
	return ReactDOMServer.renderToString(
		<IoMdAirplane color={color} size={size} />
	);
};

/* ======= Crear icono de avión personalizado en SVG ======= */
const createAirplaneIcon = (flight, rotation = 0) => {
	const color = getAircraftColorByStatus(flight);
	const iconSvg = getAirplaneSvg(color, 20);

	return L.divIcon({
		html: `<div style="transform: rotate(${rotation}deg); transform-origin: center center; display: flex; align-items: center; justify-content: center;">
        			${iconSvg}
      			</div>`,
		className: "airplane-icon-animated",
		iconSize: [20, 20],
		iconAnchor: [13, 13],
		popupAnchor: [0, -16],
	});
};

/* ======= Crear popup detallado para un vuelo (HTML string) ======= */
const createFlightPopup = (flight) => {
	const statusIcon =
		flight.status === 'completed' || flight.progress >= 100 ? '✅' :
			flight.status === 'delayed' || flight.retrasado ? '🔴' : '🔵';

	const statusText =
		flight.status === 'completed' || flight.progress >= 100 ? 'Completado' :
			flight.status === 'delayed' || flight.retrasado ? 'Retrasado' : 'En curso';

	const color = getAircraftColorByStatus(flight);

	return `
		<div style="min-width: 200px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
			<div style="background: linear-gradient(135deg, ${color}dd 0%, ${color}aa 100%); color: white; padding: 8px 12px; margin: -10px -10px 10px -10px; border-radius: 4px 4px 0 0;">
				<strong style="font-size: 15px;">✈️ ${flight.id}</strong>
			</div>
			
			<div style="padding: 4px 0;">
				<div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #e5e7eb;">
					<div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">
						<strong>Ruta:</strong>
					</div>
					<div style="font-size: 14px; font-weight: 600; color: #1f2937;">
						${flight.origin?.code || 'N/A'} → ${flight.destination?.code || 'N/A'}
					</div>
					${flight.origin?.region && flight.destination?.region ?
			`<div style="font-size: 11px; color: #9ca3af; margin-top: 2px;">
							${flight.isSameContinentFlight ? '🌍 Mismo continente' : '🌏 Intercontinental'}
						</div>` : ''
		}
				</div>
				
				<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
					<div>
						<div style="font-size: 11px; color: #6b7280;">Progreso</div>
						<div style="font-size: 14px; font-weight: 600; color: #1f2937;">
							${Math.round(flight.progress || 0)}%
						</div>
					</div>
					<div>
						<div style="font-size: 11px; color: #6b7280;">Estado</div>
						<div style="font-size: 13px; font-weight: 600;">
							${statusIcon} ${statusText}
						</div>
					</div>
				</div>
				
				${flight.speed ?
			`<div style="margin-bottom: 6px;">
						<div style="font-size: 11px; color: #6b7280;">Velocidad</div>
						<div style="font-size: 13px; color: #1f2937;">${flight.speed} km/h</div>
					</div>` : ''
		}
				
				${flight.altitude ?
			`<div style="margin-bottom: 6px;">
						<div style="font-size: 11px; color: #6b7280;">Altitud</div>
						<div style="font-size: 13px; color: #1f2937;">${flight.altitude.toLocaleString()} ft</div>
					</div>` : ''
		}
				
				${flight.packageType ?
			`<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #e5e7eb;">
						<div style="font-size: 11px; color: #6b7280;">Tipo de paquete</div>
						<div style="font-size: 13px; color: #1f2937; font-weight: 500;">
							${flight.packageType === 'AG' ? '📦 Algoritmo Genético' :
				flight.packageType === 'Inicial' ? '🎯 Planificación Inicial' :
					flight.packageType}
						</div>
						${flight.currentPackages !== undefined && flight.packageCapacity !== undefined ?
				`<div style="font-size: 12px; color: #6b7280; margin-top: 2px;">
								Capacidad: ${flight.currentPackages}/${flight.packageCapacity} paquetes
							</div>` : ''
			}
					</div>` : ''
		}
				
				${flight.pedidoId ?
			`<div style="margin-top: 6px; font-size: 11px; color: #6b7280;">
						Pedido: <span style="font-family: monospace; color: #1f2937;">${flight.pedidoId}</span>
					</div>` : ''
		}
			</div>
		</div>
	`;
};

/* ======= Crear icono de aeropuerto personalizado ======= */
const createAirportIcon = (name, saturation = 0) => {
	let size, color, borderColor, borderWidth, shadow;
	// Estilos especiales para sedes
	if (name == "Bruselas" || name == "Lima" || name == "Baku") {
		size = 23; color = '#4954b6ff'; borderColor = '#ffffffff'; borderWidth = 1.8; shadow = '0 2px 2px rgba(0,0,0,0.4)';
	} else {
		// Estilos para aeropuertos normales según saturación
		size = 18; borderColor = '#ffffff'; borderWidth = 1.5; shadow = '0 2px 2px rgba(0,0,0,0.4)';
		if (saturation >= 80) color = '#dc3545'; 
		else if (saturation >= 50) color = '#f59e0b'; 
		else color = '#28a745';
	}
	// Crear icono con estilos definidos
	return new L.DivIcon({
		className: 'airport-marker',
		html: `<div style="background: ${color}; border: ${borderWidth}px solid ${borderColor}; border-radius: 50%; width: ${size}px; height: ${size}px; display:flex;align-items:center;justify-content:center; box-shadow:${shadow}; position:relative; cursor:pointer; transition: all .3s ease;">
			<i class="fas fa-${(name == "Bruselas" || name == "Lima" || name == "Baku") ? 'building' : 'plane'}" style="color:white; font-size:${size * 0.4}px; ${(name == "Bruselas" || name == "Lima" || name == "Baku") ? '' : 'transform: rotate(45deg);'} text-shadow:0 1px 3px rgba(0,0,0,.5);"></i>
		</div>`,
		iconSize: [size, size], 
		iconAnchor: [size / 2, size / 2], 
		popupAnchor: [0, -size / 2]
	});
};

/* Crear popup detallado para un aeropuerto (HTML string) */
const createAirportPopup = (airport) => {
	const isUnlimited = airport.capacity === 'ILIMITADO';
	const capacityValue = isUnlimited ? null : (typeof airport.capacity === 'number' ? airport.capacity : (Number(airport.capacity) || null));
	const packages = airport.packages || 0;
	const pedidosCount = airport.pedidosCount || 0; // 🆕 Número de pedidos (diferente a paquetes)
	const tiempoRestante = airport.tiempoRestanteRecogida; // 🆕 Tiempo hasta próxima recogida (minutos)
	const saturation = isUnlimited || !capacityValue ? 0 : ((packages / capacityValue) * 100);

	// Formatear tiempo restante
	const formatearTiempoRestante = (minutos) => {
		if (!minutos || minutos === Infinity) return null;
		if (minutos < 60) return `${Math.round(minutos)} min`;
		const horas = Math.floor(minutos / 60);
		const mins = Math.round(minutos % 60);
		return `${horas}h ${mins}m`;
	};

	const tiempoRestanteStr = formatearTiempoRestante(tiempoRestante);

	// 🆕 Sección de paquetes esperando recogida
	const recogidaInfo = packages > 0 && tiempoRestanteStr ? `
		<div style="margin-top:8px; padding:8px; background:#fef3c7; border-radius:6px; border-left:3px solid #f59e0b;">
			<div style="font-size:11px; color:#92400e; font-weight:600;">⏰ Próxima recogida en ~${tiempoRestanteStr}</div>
			<div style="font-size:10px; color:#a16207; margin-top:2px;">Los paquetes se recogen 2h después de aterrizar</div>
		</div>
	` : '';

	// 🆕 Barra de progreso mejorada con más información
	const progressBar = isUnlimited || !capacityValue ? '' : `
		<div style="margin-top:12px; padding-top:12px; border-top: 1px solid #e5e7eb;">
			<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
				<span style="font-size:12px; font-weight:600; color:#374151;">📦 Ocupación del almacén</span>
				<span style="font-size:13px; font-weight:700; color:${saturation >= 80 ? '#dc3545' : saturation >= 50 ? '#f59e0b' : '#28a745'};">${saturation.toFixed(1)}%</span>
			</div>
			<div style="height:12px; background:#eef2f6; border-radius:8px; overflow:hidden; box-shadow: inset 0 1px 2px rgba(0,0,0,0.04);">
				<div style="width:${Math.min(100, Math.round(saturation))}%; height:100%; background: linear-gradient(90deg, ${saturation >= 80 ? '#dc3545' : saturation >= 50 ? '#f59e0b' : '#28a745'}, ${saturation >= 80 ? '#ef4444' : saturation >= 50 ? '#fbbf24' : '#22c55e'}); transition:width .35s ease;"></div>
			</div>
			<div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; margin-top:10px;">
				<div style="background:#f8fafc; padding:8px; border-radius:6px; text-align:center;">
					<div style="font-size:18px; font-weight:700; color:#1f2937;">${packages.toLocaleString()}</div>
					<div style="font-size:11px; color:#6b7280;">Paquetes actuales</div>
				</div>
				<div style="background:#f8fafc; padding:8px; border-radius:6px; text-align:center;">
					<div style="font-size:18px; font-weight:700; color:#6b7280;">${capacityValue.toLocaleString()}</div>
					<div style="font-size:11px; color:#6b7280;">Capacidad total</div>
				</div>
			</div>
			${pedidosCount > 0 ? `
				<div style="margin-top:8px; font-size:12px; color:#6b7280; text-align:center;">
					📋 ${pedidosCount} pedidos en almacén
				</div>
			` : ''}
			${recogidaInfo}
		</div>`;

	// 🆕 Para sedes con capacidad ilimitada
	const unlimitedSection = isUnlimited ? `
		<div style="margin-top:12px; padding-top:12px; border-top: 1px solid #e5e7eb;">
			<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
				<span style="font-size:12px; font-weight:600; color:#374151;">📦 Almacén</span>
				<span style="font-size:13px; font-weight:700; color:#6b7280;">♾️ ILIMITADO</span>
			</div>
			<div style="background:#f8fafc; padding:10px; border-radius:6px; text-align:center; margin-top:8px;">
				<div style="font-size:20px; font-weight:700; color:#1f2937;">${packages.toLocaleString()}</div>
				<div style="font-size:11px; color:#6b7280;">Paquetes actuales</div>
			</div>
			${pedidosCount > 0 ? `
				<div style="margin-top:8px; font-size:12px; color:#6b7280; text-align:center;">
					📋 ${pedidosCount} pedidos en almacén
				</div>
			` : ''}
			${recogidaInfo}
		</div>
	` : '';

	return `
		<div style="min-width:280px; padding:14px; border-radius:12px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: white; color: #111827; box-shadow: 0 6px 18px rgba(16,24,40,0.08);">
			<div style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px;">
				<div style="flex:1; padding-right:8px;">
					<div style="font-size:15px; font-weight:800; color:#0f172a; line-height:1.1;">${airport.name}</div>
					<div style="font-size:12px; color:#6b7280; margin-top:4px;">${airport.country || 'País desconocido'} • Código: ${airport.code || 'N/A'}</div>
				</div>
				<div style="text-align:right; font-size:12px; color:#6b7280; white-space:nowrap;">${airport.isSede ? '🏢 Sede' : '✈️ Aeropuerto'}</div>
			</div>
			<div style="margin-top:10px; font-size:13px; color:#374151;">${airport.region ? `Región: ${airport.region}` : ''}${airport.operationType ? ` • ${airport.operationType}` : ''}</div>
			${isUnlimited ? unlimitedSection : progressBar}
		</div>
	`;
};

/* ======= Calcular rumbo entre dos puntos ======= */
const toRad = (deg) => (deg * Math.PI) / 180;
const toDeg = (rad) => (rad * 180) / Math.PI;

const calculateBearing = (from, to) => {
	const lat1 = toRad(from.lat);
	const lat2 = toRad(to.lat);
	const dLon = toRad(to.lng - from.lng);

	const y = Math.sin(dLon) * Math.cos(lat2);
	const x =
		Math.cos(lat1) * Math.sin(lat2) -
		Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

	let brng = toDeg(Math.atan2(y, x)); // -180 .. 180
	brng = (brng + 360) % 360; // 0 .. 360

	return brng;
};

/* ======= Componente para manejar marcadores y líneas dinámicas ======= */
function DynamicMarkers({ flights, airports, activeView, showRoutes, vuelosEnMovimiento, showFlightLines }) {
	const map = (0, require('react-leaflet').useMap)();
	const markersRef = React.useRef({});
	const airportMarkersRef = React.useRef({});
	const flightLinesRef = React.useRef({});
	const lastLogRef = React.useRef({ count: 0, time: 0, activeCount: 0 });

	React.useEffect(() => {
		const now = Date.now();

		// DEBUG: Log cuando cambia el número de vuelos o cada 5 segundos
		if (flights.length !== lastLogRef.current.count || now - lastLogRef.current.time > 5000) {
			console.log(`🗺️ DynamicMarkers - Recibidos ${flights.length} vuelos, activeView: ${activeView}`);
			lastLogRef.current = { count: flights.length, time: now };
		}

		/***** AEROPUERTOS *****/
		const currentAirportCodes = new Set();
		airports.forEach(airport => {
			// Validar coordenadas
			if (!airport.lat || !airport.lng || isNaN(airport.lat) || isNaN(airport.lng)) {
				console.warn(`Aeropuerto ${airport.code} sin coordenadas válidas`);
				return;
			}

			// Añadir código al set actual
			currentAirportCodes.add(airport.code);
			const existingMarker = airportMarkersRef.current[airport.code];

			// Actualizar o crear marcador
			if (existingMarker) {
				existingMarker.setPopupContent(createAirportPopup(airport));
			} else {
				const isUnlimited = airport.capacity === 'ILIMITADO';
				const saturation = isUnlimited ? 0 : (airport.packages / airport.capacity) * 100;
				const icon = createAirportIcon(airport.name, saturation); // Crear icono con saturación
				const marker = L.marker([airport.lat, airport.lng],
					{ icon, isAirport: true}).bindPopup(createAirportPopup(airport), {
					closeOnClick: false,
					autoClose: false
				}); // crear popup detallado
				marker.addTo(map); // Agregar al mapa
				airportMarkersRef.current[airport.code] = marker; // Guardar referencia
			}
		});

		// Remover marcadores de aeropuertos que ya no existen
		Object.keys(airportMarkersRef.current).forEach(code => {
			if (!currentAirportCodes.has(code)) {
				map.removeLayer(airportMarkersRef.current[code]);
				delete airportMarkersRef.current[code];
			}
		});

		/***** VUELOS *****/
		if (activeView === 'flights' || activeView === 'routes') {
			const currentFlightIds = new Set();

			// Filtrar vuelos activos en movimiento
			const vuelosActivos = vuelosEnMovimiento.filter(v =>
				v.status === 'active' && v.progress > 0 && v.progress < 100
			);

			// Logs de vuelos excluidos
			const vuelosNoActivos = vuelosEnMovimiento.filter(v =>
				!(v.status === 'active' && v.progress > 0 && v.progress < 100)
			);
			if (vuelosNoActivos.length > 0 && vuelosNoActivos.length < 3) {
				console.log(`🔍 Vuelos excluidos:`, vuelosNoActivos.map(v =>
					`${v.id.substring(0, 8)}... (status=${v.status}, progress=${v.progress?.toFixed(1)}%)`
				));
			}
			if (vuelosActivos.length !== lastLogRef.current.activeCount) {
				console.log(`✈️ Aviones en vuelo: ${vuelosActivos.length}/${vuelosEnMovimiento.length}`);
				lastLogRef.current.activeCount = vuelosActivos.length;
			}

			// Procesar cada vuelo activo
			vuelosActivos.forEach((flight, index) => {
				// Validar coordenadas
				if (!flight.currentLat || !flight.currentLng ||
					isNaN(flight.currentLat) || isNaN(flight.currentLng)) {
					return;
				}

				// Añadir ID al set actual
				currentFlightIds.add(flight.id);
				const existingMarker = markersRef.current[flight.id];
				const position = { lat: flight.currentLat, lng: flight.currentLng };

				// Actualizar o crear marcador
				if (existingMarker) {
					// Actualizar posición con animación suave si la distancia es significativa
					const currentLatLng = existingMarker.getLatLng();
					const newLatLng = L.latLng(position.lat, position.lng);
					const distance = currentLatLng.distanceTo(newLatLng);

					// Calcular rotación según el movimiento actual
					const rotation = calculateBearing(
						{ lat: currentLatLng.lat, lng: currentLatLng.lng },
						{ lat: newLatLng.lat, lng: newLatLng.lng }
					);

					// Animar solo si la distancia es mayor a 100 metros
					if (distance > 100) {
						animateMarker(existingMarker, currentLatLng, newLatLng, 1000);
					} else {
						existingMarker.setLatLng(newLatLng);
					}
					// Actualizar icono y popup
					existingMarker.setIcon(createAirplaneIcon(flight, rotation));
					existingMarker.setPopupContent(createFlightPopup(flight));
				} else {
					// Calcular rotación inicial
					let rotation = 0;
					if (flight.origin && flight.destination) {
						rotation = calculateBearing(
							{ lat: flight.origin.lat, lng: flight.origin.lng },
							{ lat: flight.destination.lat, lng: flight.destination.lng }
						);
					}
					// Crear nuevo marcador
					const icon = createAirplaneIcon(flight, rotation);
					const popupContent = createFlightPopup(flight);
					const marker = L.marker([position.lat, position.lng], {
						icon,
						isFlight: true
					}).bindPopup(popupContent);
					marker.addTo(map);
					markersRef.current[flight.id] = marker;
				}

				// Líneas dinámicas - Solo crear si el botón está activado
				if (showFlightLines) {
					try {
						const lineKey = flight.id;
						const coords = [[flight.origin.lat, flight.origin.lng], [position.lat, position.lng]];
						const existingLine = flightLinesRef.current[lineKey];

						if (existingLine) {
							existingLine.setLatLngs(coords);
						} else {
							const colorAirplane = getAircraftColorByStatus(flight);
							const flightLine = L.polyline(coords, {
								color: colorAirplane,
								weight: 2,
								opacity: 1.0,
								dashArray: '3, 8',
								lineCap: 'round',
								lineJoin: 'round',
								interactive: false
							});
							flightLine.addTo(map);
							flightLinesRef.current[lineKey] = flightLine;
						}
					} catch (err) {
						console.warn('❌ No se pudo dibujar línea de vuelo para', flight.id, err);
					}
				}
			});

			// Remover marcadores de vuelos que ya no existen
			Object.keys(markersRef.current).forEach(flightId => {
				if (!currentFlightIds.has(flightId)) {
					map.removeLayer(markersRef.current[flightId]);
					delete markersRef.current[flightId];
				}
			});

			// Remover líneas de vuelos que ya no existen (SEPARADO)
			Object.keys(flightLinesRef.current).forEach(flightId => {
				if (!currentFlightIds.has(flightId)) {
					try {
						map.removeLayer(flightLinesRef.current[flightId]);
						delete flightLinesRef.current[flightId];
					} catch (e) {
						console.warn(`Error eliminando línea: ${e.message}`);
					}
				}
			});

			// Limpiar TODAS las líneas si el botón está desactivado
			if (!showFlightLines && Object.keys(flightLinesRef.current).length > 0) {
				console.log('🧹 Limpiando todas las líneas (botón desactivado)');
				Object.values(flightLinesRef.current).forEach(line => {
					try { map.removeLayer(line); } catch (e) { }
				});
				flightLinesRef.current = {};
			}

			console.log(`📊 Marcadores: ${Object.keys(markersRef.current).length} | Líneas: ${Object.keys(flightLinesRef.current).length}`);
		} else {
			// Limpiar todo si no estamos en vista de vuelos
			Object.values(markersRef.current).forEach(marker => map.removeLayer(marker));
			markersRef.current = {};
			Object.values(flightLinesRef.current).forEach(line => map.removeLayer(line));
			flightLinesRef.current = {};
		}

		return () => {
			Object.values(airportMarkersRef.current).forEach(m => map.removeLayer(m));
			airportMarkersRef.current = {};
			Object.values(markersRef.current).forEach(m => map.removeLayer(m));
			markersRef.current = {};
		};
	}, [vuelosEnMovimiento, airports, activeView, showRoutes, showFlightLines, map]);

	return null;
}

/**
 * 🆕 SISTEMA HÍBRIDO: Calcular posición interpolada basada en tiempo
 * Inspirado en MapaVuelos.tsx - Interpolación lineal continua
 */
function calculateInterpolatedPosition(vuelo, tiempoActualMs) {
	// Verificar que el vuelo tenga timestamps
	if (!vuelo.fechaInicial || !vuelo.fechaFinal) {
		console.warn(`⚠️ Vuelo ${vuelo.id} sin timestamps - usando fallback`);
		// Fallback: retornar posición actual
		return {
			lat: vuelo.currentLat || vuelo.origin?.lat,
			lng: vuelo.currentLng || vuelo.origin?.lng,
			progress: vuelo.progress || 0,
			status: vuelo.status || 'active'
		};
	}

	// 🆕 FIX: Forzar interpretación como UTC si no tiene 'Z'
	let fechaInicialStr = vuelo.fechaInicial;
	let fechaFinalStr = vuelo.fechaFinal;
	if (typeof fechaInicialStr === 'string' && !fechaInicialStr.endsWith('Z')) {
		fechaInicialStr = fechaInicialStr + 'Z';
	}
	if (typeof fechaFinalStr === 'string' && !fechaFinalStr.endsWith('Z')) {
		fechaFinalStr = fechaFinalStr + 'Z';
	}

	// Extraer timestamps
	const horaSalida = new Date(fechaInicialStr).getTime();
	const horaLlegada = new Date(fechaFinalStr).getTime();
	const duracionVuelo = horaLlegada - horaSalida;

	// 🐛 DEBUG: Log muy reducido para no saturar consola (🚀 Optimizado)
	// Deshabilitado en producción - descomentar para debug
	/*
	if (Math.random() < 0.005) { // 0.5% de probabilidad
		const tiempoActual = new Date(tiempoActualMs);
		const enVuelo = tiempoActualMs >= horaSalida && tiempoActualMs < horaLlegada;
		console.log(`🔍 ${vuelo.id}: ${enVuelo ? '✈️' : '⏸️'} ${(duracionVuelo / 1000 / 60).toFixed(0)}min`);
	}
	*/

	// Si el vuelo no ha empezado, está en origen
	if (tiempoActualMs < horaSalida) {
		return {
			lat: vuelo.origin.lat,
			lng: vuelo.origin.lng,
			progress: 0,
			status: 'waiting'
		};
	}

	// Si el vuelo ya terminó, está en destino
	if (tiempoActualMs >= horaLlegada) {
		return {
			lat: vuelo.destination.lat,
			lng: vuelo.destination.lng,
			progress: 100,
			status: 'completed'
		};
	}

	// Calcular progreso (0-100%)
	const tiempoTranscurrido = tiempoActualMs - horaSalida;
	const progreso = (tiempoTranscurrido / duracionVuelo) * 100;
	const ratio = progreso / 100;

	// Interpolación lineal entre origen y destino
	const lat = vuelo.origin.lat + (vuelo.destination.lat - vuelo.origin.lat) * ratio;
	const lng = vuelo.origin.lng + (vuelo.destination.lng - vuelo.origin.lng) * ratio;

	// 🐛 DEBUG deshabilitado para rendimiento
	// if (Math.random() < 0.02) { console.log(`   ✈️ Progreso: ${progreso.toFixed(1)}%`); }

	return {
		lat,
		lng,
		progress: progreso,
		status: 'active'
	};
}

/**
 * Animar movimiento suave de marcador entre dos posiciones
 * También actualiza la rotación del avión en la dirección del movimiento
 */
function animateMarker(marker, startLatLng, endLatLng, duration = 1000) {
	const startTime = Date.now();
	const startLat = startLatLng.lat;
	const startLng = startLatLng.lng;
	const endLat = endLatLng.lat;
	const endLng = endLatLng.lng;

	// Calcular el ángulo de rotación basado en la dirección del movimiento
	const rotation = bearingDegrees(startLat, startLng, endLat, endLng);

	function frame() {
		const elapsed = Date.now() - startTime;
		const progress = Math.min(elapsed / duration, 1);

		// Easing suave (ease-out cúbico) - más realista para movimiento de aviones
		const eased = 1 - Math.pow(1 - progress, 3);

		const currentLat = startLat + (endLat - startLat) * eased;
		const currentLng = startLng + (endLng - startLng) * eased;

		marker.setLatLng([currentLat, currentLng]);

		if (progress < 1) {
			requestAnimationFrame(frame);
		}
	}

	requestAnimationFrame(frame);
}

/* Función para parsear fecha simulada en UTC */
const parseSimDateUTC = (str) => str ? new Date(str.replace(' ', 'T') + ':00Z') : null;

// Rumbo geodésico en grados (0°=N, 90°=E)
// Si tu SVG “mira a la derecha”, usa (brg - 90 + 360) % 360
function bearingDegrees(lat1, lon1, lat2, lon2) {
	const toRad = d => d * Math.PI / 180;
	const toDeg = r => r * 180 / Math.PI;
	const φ1 = toRad(lat1), φ2 = toRad(lat2);
	const Δλ = toRad(lon2 - lon1);
	const y = Math.sin(Δλ) * Math.cos(φ2);
	const x = Math.cos(φ1) * Math.cos(φ2) - Math.sin(φ1) * Math.sin(φ2) * Math.cos(Δλ);
	return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

const SimuladorSemanal = () => {
	const [fechaInicioSimulacion, setFechaInicioSimulacion] = useState(""); // la fecha que envías
	const [horaInicioSimulacion, setHoraInicioSimulacion] = useState("00:00"); // la hora que envías (formato HH:mm)
	const [planFixed, setPlanFixed] = useState([]);  // lista de vuelos del JSON local
	const [simClock, setSimClock] = useState(null);  // reloj simulado (Date)

	const simStartRef = useRef(null);

	const navigate = useNavigate();
	const [flights, setFlights] = useState([]);
	const [flightsInAir, setFlightsInAir] = useState(0);
	const [currentTime, setCurrentTime] = useState(new Date(2024, 7, 27, 8, 0, 0));
	const [elapsedTime, setElapsedTime] = useState({ days: 0, hours: 0, minutes: 0 });
	const [isRunning, setIsRunning] = useState(true);
	const [speed, setSpeed] = useState(1);
	const [simulationStatus, setSimulationStatus] = useState('Monitoreo semanal activo');
	const [activeView, setActiveView] = useState('flights');
	const [showRoutes, setShowRoutes] = useState(true);
	const [showLegend, setShowLegend] = useState(false);
	const [showFlightLines, setShowFlightLines] = useState(true); // 🆕 Toggle para líneas dinámicas de vuelos

	// ===================== ESTADO BOTONES FLOTANTES ==================== 
	const controlButtonRef = useRef(null);

	const [legendAnchorEl, setLegendAnchorEl] = useState(null);
	const [isMetricsPanelOpen, setIsMetricsPanelOpen] = useState(false);
	const [isControlPanelOpen, setIsControlPanelOpen] = useState(false);

	//const [isMetricsPopperOpen, setIsMetricsPopperOpen] = useState(false);
	//const [isControlPopperOpen, setIsControlPopperOpen] = useState(false);
	const [metricsAnchorEl, setMetricsAnchorEl] = useState(null);
	const [controlAnchorEl, setControlAnchorEl] = useState(null);
	const [hasAutoOpened, setHasAutoOpened] = useState(false);

	const isControlPopperOpen = Boolean(controlAnchorEl);
	const isMetricsPopperOpen = Boolean(metricsAnchorEl);
	
	/* Datos de aeropuertos - se cargarán desde la API */
	const [airports, setAirports] = useState([]);
	const [loadingAirports, setLoadingAirports] = useState(true);
	const airportsRef = useRef([]); // 🔥 Ref para tener siempre el valor actual de airports
	const intervalRef = useRef(); /* Referencia para el intervalo de simulación */

	// ==================== ESTADO SSE (TIEMPO DE SIMULACIÓN Y RUTAS) ====================
	const [simulacionActiva, setSimulacionActiva] = useState(false);
	const [horaSimulada, setHoraSimulada] = useState(null);
	const [tiempoRealMs, setTiempoRealMs] = useState(0);
	const [tickActual, setTickActual] = useState(0);
	const [timeScale, setTimeScale] = useState(10.0);
	const [rutasSolucion, setRutasSolucion] = useState([]); // Rutas que vienen del SSE
	const eventSourceRef = useRef(null); /* Referencia para el EventSource SSE */

	// ==================== ESTADO WEBSOCKET PLANIFICACIÓN ====================
	// ⚠️ NOTA: El hook usePlanificacionWebSocket está DESHABILITADO porque el backend
	// no tiene el endpoint /ws/planificacion registrado. La simulación usa STOMP en /ws.
	// Para habilitar, registrar PlanificacionWebSocketHandler en WebSocketConfig.java

	// Variables placeholder para compatibilidad (el hook no se usa)
	const wsConnected = false;
	const iniciarPlanificacion = async () => { console.warn('⚠️ WebSocket planificación no disponible'); };
	const wsLimpiarIteraciones = () => { };
	const wsDetenerPlanificacion = () => { };
	const wsOnMessage = () => { };

	// Estados locales de planificación (para control local de UI)
	const [iteracionesPlanificacion, setIteracionesPlanificacion] = useState([]);
	const [estadoPlanificacion, setEstadoPlanificacion] = useState('idle');

	// Estados adicionales que no están en el hook
	const [autoInicioIntentado, setAutoInicioIntentado] = useState(false);
	const [intentosRealizados, setIntentosRealizados] = useState(0); // Contador de reintentos
	const [tiempoRealTranscurrido, setTiempoRealTranscurrido] = useState(0); // Tiempo real en segundos
	const tiempoInicioRef = useRef(null); // Momento en que se inició la planificación
	const intervalTiempoRealRef = useRef(null); // Intervalo para actualizar tiempo real
	const [tiempoSimulacionActual, setTiempoSimulacionActual] = useState(null); // Hora de simulación del backend

	// ==================== NUEVO: ESTADO WEBSOCKET STOMP (SIMULACIÓN SEMANAL) ====================
	const [wsStompConectado, setWsStompConectado] = useState(false);
	const [sessionId, setSessionId] = useState(null);
	const [progresoAG, setProgresoAG] = useState(null);
	const [mensajesSimulacion, setMensajesSimulacion] = useState([]);
	const [estadoSimulacionStomp, setEstadoSimulacionStomp] = useState('disconnected'); // disconnected, connecting, connected, running, completed, error
	const stompClientRef = useRef(null);
	const subscriptionRef = useRef(null);

	// ==================== SISTEMA DE COLA Y RELOJ LOCAL ====================
	const [colaVuelos, setColaVuelos] = useState([]);           // Buffer de vuelos pendientes del WebSocket
	const [vuelosEnAire, setVuelosEnAire] = useState([]);       // Vuelos activos (procesándose en animación)
	const [pedidosCompletados, setPedidosCompletados] = useState([]);
	const [contadorPedidosTotal, setContadorPedidosTotal] = useState(0); // 🆕 Contador de todos los pedidos en pantalla
	const [relojLocal, setRelojLocal] = useState(null);         // Reloj de simulación local (independiente)
	const [kActual, setKActual] = useState(500);                // Factor K actual (adaptable)
	const [kBase] = useState(500);                               // Factor K base (constante)
	const [modoRalentizado, setModoRalentizado] = useState(false); // Indica si la simulación está ralentizada
	const [simulacionLocalActiva, setSimulacionLocalActiva] = useState(false); // Si la animación local corre
	const relojLocalRef = useRef(null);                          // Ref para el reloj local (evita closures)
	const colaVuelosRef = useRef([]);                            // Ref para la cola (evita closures)
	const TICK_REAL_MS = 250;                                    // Intervalo de actualización en ms (🚀 Optimizado: 4 FPS)

	// ========== BUFFER DE 20 SEGUNDOS PARA DAR VENTAJA AL BACKEND ==========
	const [bufferActivo, setBufferActivo] = useState(false);     // Si el buffer está activo (primeros 20 segundos)
	const vuelosBufferRef = useRef([]);                          // Vuelos acumulados durante el buffer
	const bufferTimeoutRef = useRef(null);                       // Timeout para finalizar buffer
	const BUFFER_DELAY_MS = 20000;                               // 🆕 20 segundos de buffer inicial
	const UMBRAL_COLA_BAJA = 3;                                  // Si cola < 3, ralentizar (no usado en modo constante)
	const FACTOR_RALENTIZADO = 0.25;                             // K se reduce a 25% cuando cola baja (no usado)

	// ========== SISTEMA DE TIEMPO SIMULADO (REACTIVO) ==========
	const [tiempoSimuladoBackend, setTiempoSimuladoBackend] = useState(null);
	const [ultimaActualizacionReal, setUltimaActualizacionReal] = useState(null);
	const [tiempoMovimiento, setTiempoMovimiento] = useState(0);
	const [tiempoSimulado, setTiempoSimulado] = useState(Date.now());
	const [speedMultiplier, setSpeedMultiplier] = useState(DESIRED_TIME_SCALE); // 500x
	const tiempoSimuladoBackendRef = useRef(null); // 🆕 Ref para verificar si ya se inicializó

	// 🔥 Refs para las funciones de procesamiento (evitan circular dependencies)
	const procesarVuelosDirectosRef = useRef(null);
	const procesarRutasSimulacionRef = useRef(null);

	// 🆔 Contador único para generar IDs de vuelos (evita duplicados)
	const contadorVuelosRef = useRef(0);
	const procesarRutasSnapshotRef = useRef(null);
	const procesarSegmentsSnapshotRef = useRef(null); // 🆕 Para nueva estructura JSON

	// ✅ ELIMINADO: Refs de tiempo movidas a estado reactivo (ver línea ~215)

	// ==================== 📊 MÉTRICAS DE VUELOS ====================
	// Para diagnosticar si el backend envía vuelos tarde o si hay problemas de graficación
	const [metricasVuelos, setMetricasVuelos] = useState({
		totalRecibidosBackend: 0,      // Total de vuelos únicos recibidos del backend
		totalEnEstadoFlights: 0,       // Vuelos en el estado flights[] actual
		totalGraficados: 0,            // Vuelos que se están mostrando en el mapa
		totalPerdidosAntesDeTiempo: 0, // Vuelos que llegaron y ya terminaron antes de graficarse
		totalPerdidosDespuesDeTiempo: 0, // Vuelos que no empezaron porque el tiempo simulado ya pasó
		ultimaActualizacion: null      // Timestamp de última actualización
	});
	const vuelosRecibidosIdsRef = useRef(new Set()); // IDs únicos recibidos (para evitar contar duplicados)



	/* ==================== FUNCIÓN PARA CONVERTIR RUTA DEL BACKEND A VUELO ==================== */
	// NOTA: Esta función se usaba para convertir rutas SSE del backend
	// en vuelos compatibles con el mapa. En el modo actual (simClock + planFixed)
	// la simulación construye los flights directamente en el useEffect de simClock.
	// La dejamos por si volvemos a usar SSE en el futuro.
	const convertirRutaAVuelo = (ruta) => {
		// Determinar color según progreso y estado
		let aircraftColor;
		if (!ruta.enVuelo) {
			aircraftColor = '#6c757d'; // Gris si no está en vuelo
		} else if (ruta.progress >= 0.8) {
			aircraftColor = '#28a745'; // Verde cerca del destino
		} else if (ruta.progress >= 0.5) {
			aircraftColor = '#ffc107'; // Amarillo a mitad de camino
		} else {
			aircraftColor = '#007bff'; // Azul al inicio
		}

		// Calcular rotación basada en dirección
		const deltaLat = ruta.destinoLatitud - ruta.origenLatitud;
		const deltaLng = ruta.destinoLongitud - ruta.origenLongitud;
		const rotation = Math.atan2(deltaLng, deltaLat) * (180 / Math.PI);

		// Crear objeto de vuelo compatible con el mapa
		return {
			id: `RT${String(ruta.id).padStart(4, '0')}`,
			origin: {
				code: ruta.originCode,
				lat: ruta.origenLatitud,
				lng: ruta.origenLongitud,
				region: ruta.regionOrigen
			},
			destination: {
				code: ruta.destinationCode,
				lat: ruta.destinoLatitud,
				lng: ruta.destinoLongitud,
				region: ruta.regionDestino
			},
			progress: ruta.progress,
			altitude: ruta.altitude,
			speed: ruta.speed,
			status: ruta.enVuelo ? 'active' : 'grounded',
			packageCapacity: ruta.totalPackages,
			currentPackages: ruta.totalPackages,
			packageType: 'MPE',
			isSameContinentFlight: ruta.regionOrigen === ruta.regionDestino,
			currentLat: ruta.currentLatitude,
			currentLng: ruta.currentLongitude,
			aircraftColor,
			rotation
		};
	};

	/* ==================== DEBUG: MONITOREAR CAMBIOS EN tiempoRealMs ==================== */
	useEffect(() => {
		console.log('tiempoRealMs actualizado a:', tiempoRealMs, 'ms =', {
			horas: Math.floor(tiempoRealMs / 3600000),
			minutos: Math.floor((tiempoRealMs % 3600000) / 60000),
			segundos: Math.floor((tiempoRealMs % 60000) / 1000)
		});
	}, [tiempoRealMs]);

	/* ==================== Cargar aeropuertos desde API al montar ==================== */
	useEffect(() => {
		const fetchAirports = async () => {
			try {
				setLoadingAirports(true);
				console.log('Iniciando carga de aeropuertos...');
				const data = await getAirports();
				console.log('Aeropuertos cargados desde API:', data.length, 'aeropuertos');
				setAirports(data);
				airportsRef.current = data; // 🔥 Actualizar ref
			} catch (error) {
				console.error('Error al cargar aeropuertos desde API:', error);
				console.log('Usando datos de fallback...');
				/* Fallback a datos estáticos en caso de error */
				/* 🆕 TODOS los aeropuertos empiezan VACÍOS (packages: 0) */
				const fallbackData = [
					{ name: 'Bruselas-Charleroi', code: 'EBCI', lat: 50.4592, lng: 4.4538, capacity: 'ILIMITADO', packages: 0, pedidosCount: 0, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Lima-Jorge Chávez', code: 'SPIM', lat: -12.0219, lng: -77.1143, capacity: 'ILIMITADO', packages: 0, pedidosCount: 0, isSede: true, region: 'América del Sur', country: 'Perú', operationType: 'Sede Principal - Hub Sudamericano' },
					{ name: 'Bogotá', code: 'SKBO', lat: 4.7016, lng: -74.1469, capacity: 900, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Colombia', operationType: 'Aeropuerto Regional' },
					{ name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 'ILIMITADO', packages: 0, pedidosCount: 0, isSede: true, region: 'Europa', country: 'Bélgica', operationType: 'Sede Principal - Hub Europeo' },
					{ name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
					// Agregar todos los aeropuertos que el backend envía
					{ name: 'Muscat', code: 'OOMS', lat: 23.5933, lng: 58.2844, capacity: 800, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Omán', operationType: 'Aeropuerto Regional' },
					{ name: 'Brasilia', code: 'SBBR', lat: -15.8697, lng: -47.9206, capacity: 950, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Brasil', operationType: 'Aeropuerto Regional' },
					{ name: 'Quito', code: 'SEQM', lat: -0.1277, lng: -78.3575, capacity: 750, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Ecuador', operationType: 'Aeropuerto Regional' },
					{ name: 'New Delhi', code: 'VIDP', lat: 28.5562, lng: 77.1000, capacity: 1100, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'India', operationType: 'Aeropuerto Regional' },
					{ name: 'Amman', code: 'OJAI', lat: 31.7226, lng: 35.9932, capacity: 700, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Jordania', operationType: 'Aeropuerto Regional' },
					{ name: 'Amsterdam-Schiphol', code: 'EHAM', lat: 52.3105, lng: 4.7683, capacity: 1200, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Países Bajos', operationType: 'Aeropuerto Regional' },
					{ name: 'Prague', code: 'LKPR', lat: 50.1008, lng: 14.2600, capacity: 850, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'República Checa', operationType: 'Aeropuerto Regional' },
					{ name: 'Sana\'a', code: 'OYSN', lat: 15.4762, lng: 44.2189, capacity: 600, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Yemen', operationType: 'Aeropuerto Regional' },
					{ name: 'Minsk', code: 'UMMS', lat: 53.8824, lng: 28.0307, capacity: 750, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Bielorrusia', operationType: 'Aeropuerto Regional' },
					{ name: 'Porto Alegre', code: 'SGAS', lat: -29.9944, lng: -51.1714, capacity: 800, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Brasil', operationType: 'Aeropuerto Regional' },
					{ name: 'Sofia', code: 'LBSF', lat: 42.6950, lng: 23.4114, capacity: 700, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Bulgaria', operationType: 'Aeropuerto Regional' },
					{ name: 'Berlin-Tempelhof', code: 'EDDI', lat: 52.4726, lng: 13.4040, capacity: 900, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Alemania', operationType: 'Aeropuerto Regional' },
					{ name: 'La Paz', code: 'SLLP', lat: -16.5133, lng: -68.1925, capacity: 650, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Bolivia', operationType: 'Aeropuerto Regional' },
					{ name: 'Dubai', code: 'OMDB', lat: 25.2528, lng: 55.3644, capacity: 1300, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'EAU', operationType: 'Aeropuerto Regional' },
					{ name: 'Buenos Aires-Ezeiza', code: 'SABE', lat: -34.8222, lng: -58.5358, capacity: 1000, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Argentina', operationType: 'Aeropuerto Regional' },
					{ name: 'Riyadh', code: 'OERK', lat: 24.9578, lng: 46.6987, capacity: 950, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Arabia Saudita', operationType: 'Aeropuerto Regional' },
					{ name: 'Karachi', code: 'OPKC', lat: 24.9056, lng: 67.1608, capacity: 900, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Pakistán', operationType: 'Aeropuerto Regional' },
					{ name: 'Vienna', code: 'LOWW', lat: 48.1103, lng: 16.5697, capacity: 950, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Austria', operationType: 'Aeropuerto Regional' },
					{ name: 'Asunción', code: 'SUAA', lat: -25.2400, lng: -57.5194, capacity: 600, packages: 0, pedidosCount: 0, isSede: false, region: 'América del Sur', country: 'Paraguay', operationType: 'Aeropuerto Regional' },
					{ name: 'Tirana', code: 'LATI', lat: 41.4147, lng: 19.7206, capacity: 550, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Albania', operationType: 'Aeropuerto Regional' },
					{ name: 'Zagreb', code: 'LDZA', lat: 45.7429, lng: 16.0688, capacity: 700, packages: 0, pedidosCount: 0, isSede: false, region: 'Europa', country: 'Croacia', operationType: 'Aeropuerto Regional' },
					{ name: 'Damascus', code: 'OSDI', lat: 33.4114, lng: 36.5156, capacity: 650, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Siria', operationType: 'Aeropuerto Regional' },
					{ name: 'Baku', code: 'UBBB', lat: 40.4675, lng: 50.0467, capacity: 800, packages: 0, pedidosCount: 0, isSede: false, region: 'Asia', country: 'Azerbaiyán', operationType: 'Aeropuerto Regional' }
				];
				setAirports(fallbackData);
				airportsRef.current = fallbackData; // 🔥 Actualizar ref con fallback
				console.log('✅ Datos de fallback cargados:', fallbackData.length, 'aeropuertos');
			} finally {
				setLoadingAirports(false);
				console.log('Carga de aeropuertos finalizada');
			}
		};

		fetchAirports();
	}, []);

	/* ==================== Cargar plan fijo desde JSON local al montar ==================== */
	useEffect(() => {
		if (vuelosSemana?.vuelos) {
			setPlanFixed(vuelosSemana.vuelos);
		}
	}, [vuelosSemana]);

	// ========== INTERVALO: Incrementar tiempoMovimiento ==========
	// ❌ DESACTIVADO: Ahora el reloj local controla todo el tiempo
	// Este sistema creaba conflictos con el reloj local
	/*
	useEffect(() => {
		if (!ultimaActualizacionReal || !tiempoSimuladoBackend) return;

		console.log('🕐 Iniciando intervalo de tiempo simulado');

		const interval = setInterval(() => {
			const tiempoReal = Date.now() - ultimaActualizacionReal;
			setTiempoMovimiento(tiempoReal);
		}, 50); // 20 FPS (cada 50ms)

		return () => {
			console.log('🛑 Deteniendo intervalo de tiempo simulado');
			clearInterval(interval);
		};
	}, [ultimaActualizacionReal, tiempoSimuladoBackend]);
	*/

	// ========== CALCULAR: Tiempo Simulado ==========
	// ❌ DESACTIVADO: Ahora el reloj local actualiza tiempoSimulado directamente
	// Este sistema creaba conflictos con el reloj local
	/*
	useEffect(() => {
		if (!tiempoSimuladoBackend) {
			setTiempoSimulado(Date.now());
			return;
		}

		// Calcular tiempo simulado: base + (tiempo_real × velocidad)
		const msSimuladosPasados = tiempoMovimiento * speedMultiplier;
		const nuevoTiempoSimulado = tiempoSimuladoBackend + msSimuladosPasados;
		
		setTiempoSimulado(nuevoTiempoSimulado);

		// Debug cada 2 segundos (solo algunos frames)
		if (Math.random() < 0.02) {
			const fechaSimulada = new Date(nuevoTiempoSimulado);
			console.log(`⏰ Tiempo simulado: ${fechaSimulada.toISOString()}`);
			console.log(`   Base: ${new Date(tiempoSimuladoBackend).toISOString()}`);
			console.log(`   Δ Real: ${(tiempoMovimiento / 1000).toFixed(1)}s`);
			console.log(`   Velocidad: ${speedMultiplier}x`);
		}
	}, [tiempoSimuladoBackend, tiempoMovimiento, speedMultiplier]);
	*/

	// ========== CALCULAR: Vuelos en Movimiento (REACTIVO) ==========
	const vuelosEnMovimiento = useMemo(() => {
		if (!tiempoSimulado || flights.length === 0) {
			return [];
		}

		// 🚀 Debug deshabilitado para rendimiento
		// if (Math.random() < 0.02) { console.log(`✈️ Re-calculando ${flights.length} vuelos`); }

		return flights.map(flight => {
			// Calcular posición interpolada basada en tiempo simulado
			const interpolated = calculateInterpolatedPosition(flight, tiempoSimulado);

			// Calcular rotación del avión
			const bearing = bearingDegrees(
				interpolated.lat,
				interpolated.lng,
				flight.destination.lat,
				flight.destination.lng
			);
			const rotation = (bearing - 90 + 360) % 360;

			return {
				...flight,
				currentLat: interpolated.lat,
				currentLng: interpolated.lng,
				progress: interpolated.progress,
				status: interpolated.status,
				rotation: rotation
			};
		});
	}, [flights, tiempoSimulado]); // 🎯 DEPENDENCIAS REACTIVAS

	// Debug: Cantidad de vuelos en el aire (🚀 log reducido para rendimiento)
	useEffect(() => {
		const enAire = vuelosEnMovimiento.filter(v =>
			v.status === 'active' && v.progress > 0 && v.progress < 100
		).length;

		// 📊 Contar por estado para diagnóstico
		const waiting = vuelosEnMovimiento.filter(v => v.status === 'waiting').length;
		const completed = vuelosEnMovimiento.filter(v => v.status === 'completed').length;
		const active = vuelosEnMovimiento.filter(v => v.status === 'active').length;

		if (enAire !== flightsInAir) {
			setFlightsInAir(enAire);
			// Log deshabilitado - la información ya se muestra en el UI
			// console.log(`🛫 Aviones: ${enAire}/${vuelosEnMovimiento.length}`);
		}

		// 📊 ACTUALIZAR MÉTRICAS de vuelos graficados con desglose por estado
		setMetricasVuelos(prev => ({
			...prev,
			totalEnEstadoFlights: flights.length,
			totalGraficados: enAire,
			// 🆕 Desglose por estado
			vuelosWaiting: waiting,
			vuelosCompleted: completed,
			vuelosActive: active
		}));
	}, [vuelosEnMovimiento, flightsInAir, flights.length]);

	// 🆕 EFECTO: Guardar pedidos de vuelos completados
	const vuelosCompletadosRef = useRef(new Set()); // Para rastrear vuelos ya procesados

	useEffect(() => {
		// Buscar vuelos que acaban de completarse (status = 'completed' o 'arrived')
		const vuelosTerminados = vuelosEnMovimiento.filter(v =>
			(v.status === 'completed' || v.status === 'arrived' || v.progress >= 1) &&
			!vuelosCompletadosRef.current.has(v.id) // No procesados aún
		);

		if (vuelosTerminados.length > 0) {
			// Extraer pedidos de vuelos completados
			const nuevosPedidosCompletados = [];

			vuelosTerminados.forEach(vuelo => {
				// Marcar vuelo como procesado
				vuelosCompletadosRef.current.add(vuelo.id);

				// Si tiene pedidos, agregarlos a la lista de completados
				if (vuelo.pedidos && vuelo.pedidos.length > 0) {
					vuelo.pedidos.forEach(pedido => {
						nuevosPedidosCompletados.push({
							...pedido,
							vueloId: vuelo.id,
							origen: pedido.origen || vuelo.origin?.code,
							destino: pedido.destino || vuelo.destination?.code,
							completadoEn: new Date().toISOString(),
							status: 'completed'
						});
					});
				} else if (vuelo.pedidoId) {
					// Vuelo con pedidoId individual
					nuevosPedidosCompletados.push({
						idPedido: vuelo.pedidoId,
						vueloId: vuelo.id,
						origen: vuelo.origin?.code,
						destino: vuelo.destination?.code,
						cantidad: vuelo.currentPackages || 1,
						completadoEn: new Date().toISOString(),
						status: 'completed'
					});
				}
			});

			// 🆕 ACTUALIZAR paquetes en aeropuertos destino al completar vuelos
			// Sumamos los paquetes entregados al aeropuerto destino
			try {
				setAirports(prev => {
					const updated = prev.map(a => ({ ...a }));
					vuelosTerminados.forEach(v => {
						const destCode = v.destination?.code;
						if (!destCode) return;
						const cantidadEntregada = (v.pedidos && v.pedidos.length > 0)
							? v.pedidos.reduce((s, p) => s + (p.cantidad || 0), 0)
							: (v.currentPackages || 0);
						for (let i = 0; i < updated.length; i++) {
							if (String(updated[i].code || '').toUpperCase() === String(destCode).toUpperCase()) {
								updated[i].packages = (updated[i].packages || 0) + (cantidadEntregada || 0);
								break;
							}
						}
					});
					airportsRef.current = updated; // mantener ref sincronizada
					return updated;
				});
			} catch (err) {
				console.error('Error actualizando paquetes en aeropuertos:', err);
			}

			// Agregar a la lista de pedidos completados (máximo 100 para no consumir memoria)
			if (nuevosPedidosCompletados.length > 0) {
				setPedidosCompletados(prev => {
					const nuevos = [...nuevosPedidosCompletados, ...prev];
					return nuevos.slice(0, 100); // Mantener solo los últimos 100
				});
				console.log(`📦 ${nuevosPedidosCompletados.length} pedido(s) completado(s) guardado(s)`);
			}
		}
	}, [vuelosEnMovimiento]);

	// 🆕 EFECTO: Actualizar el contador total de pedidos en pantalla
	useEffect(() => {
		let totalPedidos = 0;

		// Contar todos los pedidos de los vuelos que se están mostrando
		(flights || []).forEach(f => {
			if (f.pedidos && Array.isArray(f.pedidos)) {
				totalPedidos += f.pedidos.length;
			} else if (f.pedidoId) {
				totalPedidos += 1;
			}
		});

		setContadorPedidosTotal(totalPedidos);
	}, [flights]);

	// 📊 EFECTO: Mostrar métricas cada 10 segundos en consola
	useEffect(() => {
		if (!simulacionLocalActiva) return;

		const interval = setInterval(() => {
			const m = metricasVuelos;

			console.log(`\n📊 ========== MÉTRICAS DE VUELOS ==========`);
			console.log(`📨 Recibidos del backend:     ${m.totalRecibidosBackend}`);
			console.log(`📋 En estado flights[]:       ${m.totalEnEstadoFlights}`);
			console.log(`✈️  Graficados en mapa:        ${m.totalGraficados}`);
			console.log(`─────────────────────────────────────────`);
			console.log(`⏳ En espera (waiting):       ${m.vuelosWaiting || 0} ← no han despegado aún`);
			console.log(`� En vuelo (active):         ${m.vuelosActive || 0}`);
			console.log(`✅ Completados (completed):   ${m.vuelosCompleted || 0} ← ya aterrizaron`);
			console.log(`⚠️  Llegaron tarde:            ${m.totalPerdidosAntesDeTiempo}`);
			console.log(`📊 ==========================================\n`);

			// 🔴 DIAGNÓSTICO: Si hay vuelos pero ninguno graficado
			if (m.totalEnEstadoFlights > 0 && m.totalGraficados === 0) {
				if (m.vuelosWaiting > 0) {
					console.warn(`🔴 DIAGNÓSTICO: ${m.vuelosWaiting} vuelos esperando despegar. El tiempo simulado está ANTES de que empiecen los vuelos.`);
				}
				if (m.vuelosCompleted > 0) {
					console.warn(`🔴 DIAGNÓSTICO: ${m.vuelosCompleted} vuelos ya aterrizaron. El tiempo simulado está DESPUÉS de que terminaron.`);
				}
			}
		}, 10000); // Cada 10 segundos

		return () => clearInterval(interval);
	}, [simulacionLocalActiva, metricasVuelos]);

	// ==================== 🆕 CÁLCULO DE PAQUETES EN ALMACÉN POR AEROPUERTO ====================
	// Los paquetes en almacén = vuelos que aterrizaron hace menos de 2 horas
	// Después de 2 horas, los paquetes se "recogen" y se eliminan del almacén
	const paquetesEnAlmacen = useMemo(() => {
		if (!tiempoSimulado || vuelosEnMovimiento.length === 0) {
			return {}; // Objeto vacío: { codigoAeropuerto: { paquetes, pedidos } }
		}

		const almacenPorAeropuerto = {}; // { codigo: { paquetes: number, pedidos: Set<string> } }

		vuelosEnMovimiento.forEach(vuelo => {
			// Solo considerar vuelos que ya aterrizaron (completados)
			if (vuelo.status !== 'completed' || vuelo.progress < 100) {
				return;
			}

			// Obtener hora de llegada del vuelo
			let horaLlegada = null;
			if (vuelo.fechaFinal) {
				let fechaStr = vuelo.fechaFinal;
				if (typeof fechaStr === 'string' && !fechaStr.endsWith('Z')) {
					fechaStr = fechaStr + 'Z';
				}
				horaLlegada = new Date(fechaStr).getTime();
			}

			if (!horaLlegada) return;

			// Calcular tiempo transcurrido desde que aterrizó
			const tiempoDesdeAterrizaje = tiempoSimulado - horaLlegada;

			// Si han pasado más de 2 horas, los paquetes ya fueron recogidos
			if (tiempoDesdeAterrizaje >= TIEMPO_RECOGIDA_MS) {
				return; // Paquetes ya recogidos, no contar
			}

			// Paquetes aún en almacén (esperando recogida)
			const codigoDestino = vuelo.destination?.code;
			if (!codigoDestino) return;

			// Inicializar si no existe
			if (!almacenPorAeropuerto[codigoDestino]) {
				almacenPorAeropuerto[codigoDestino] = {
					paquetes: 0,
					pedidos: new Set(),
					tiempoRestanteMin: Infinity // Tiempo mínimo para la próxima recogida
				};
			}

			// Sumar paquetes de este vuelo
			const cantidadPaquetes = vuelo.currentPackages || vuelo.pedidos?.reduce((sum, p) => sum + (p.cantidad || 0), 0) || 0;
			almacenPorAeropuerto[codigoDestino].paquetes += cantidadPaquetes;

			// Agregar IDs de pedidos
			if (vuelo.pedidoId) {
				almacenPorAeropuerto[codigoDestino].pedidos.add(vuelo.pedidoId);
			}
			if (vuelo.pedidos) {
				vuelo.pedidos.forEach(p => {
					if (p.idPedido) almacenPorAeropuerto[codigoDestino].pedidos.add(p.idPedido);
				});
			}

			// Calcular tiempo restante para recogida (en minutos)
			const tiempoRestante = (TIEMPO_RECOGIDA_MS - tiempoDesdeAterrizaje) / 60000;
			if (tiempoRestante < almacenPorAeropuerto[codigoDestino].tiempoRestanteMin) {
				almacenPorAeropuerto[codigoDestino].tiempoRestanteMin = tiempoRestante;
			}
		});

		// Convertir Sets a conteo
		Object.keys(almacenPorAeropuerto).forEach(codigo => {
			almacenPorAeropuerto[codigo].pedidosCount = almacenPorAeropuerto[codigo].pedidos.size;
			delete almacenPorAeropuerto[codigo].pedidos; // Eliminar Set, solo mantener conteo
		});

		return almacenPorAeropuerto;
	}, [vuelosEnMovimiento, tiempoSimulado]);

	// 🆕 EFECTO: Actualizar estado de aeropuertos con paquetes calculados
	useEffect(() => {
		if (Object.keys(paquetesEnAlmacen).length === 0) return;

		setAirports(prevAirports => {
			let cambios = false;
			const nuevosAirports = prevAirports.map(airport => {
				const datosAlmacen = paquetesEnAlmacen[airport.code];
				const nuevosPaquetes = datosAlmacen?.paquetes || 0;
				const nuevosPedidos = datosAlmacen?.pedidosCount || 0;

				// Solo actualizar si cambió
				if (airport.packages !== nuevosPaquetes || airport.pedidosCount !== nuevosPedidos) {
					cambios = true;
					return {
						...airport,
						packages: nuevosPaquetes,
						pedidosCount: nuevosPedidos,
						tiempoRestanteRecogida: datosAlmacen?.tiempoRestanteMin
					};
				}
				return airport;
			});

			return cambios ? nuevosAirports : prevAirports;
		});
	}, [paquetesEnAlmacen]);

	// ==================== 🆕 RELOJ LOCAL CON VELOCIDAD CONSTANTE ====================
	// Sistema simplificado: Buffer inicial de 20 segundos, luego velocidad constante K=500
	// SIN sistema adaptativo - avance uniforme del tiempo
	const flightsInAirRef = useRef(0);

	// Actualizar ref cuando cambia flightsInAir
	useEffect(() => {
		flightsInAirRef.current = flightsInAir;
	}, [flightsInAir]);

	// 🆕 CONSTANTE: Duración de la simulación semanal (7 días en milisegundos)
	const DURACION_SIMULACION_MS = 7 * 24 * 60 * 60 * 1000; // 604,800,000 ms = 7 días

	useEffect(() => {
		if (!simulacionLocalActiva || !relojLocalRef.current || !simStartRef.current) return;

		// 🎯 VELOCIDAD CONSTANTE: Siempre usa K=300 (DESIRED_TIME_SCALE)
		// Sin adaptación basada en aviones visibles
		const K_CONSTANTE = DESIRED_TIME_SCALE; // 300x

		const interval = setInterval(() => {
			// Calcular milisegundos simulados por tick
			// Fórmula: msSimulados = (TICK_REAL_MS / 1000) * K * 1000 = TICK_REAL_MS * K
			const msSimulados = TICK_REAL_MS * K_CONSTANTE;

			// Avanzar el reloj local
			const nuevoTiempo = new Date(relojLocalRef.current.getTime() + msSimulados);

			// 🆕 VERIFICAR LÍMITE DE 7 DÍAS
			const tiempoTranscurridoSimulado = nuevoTiempo.getTime() - simStartRef.current.getTime();
			if (tiempoTranscurridoSimulado >= DURACION_SIMULACION_MS) {
				console.log('✅ SIMULACIÓN SEMANAL COMPLETADA - 7 días simulados');
				console.log(`   Inicio: ${simStartRef.current.toISOString()}`);
				console.log(`   Fin: ${nuevoTiempo.toISOString()}`);

				// Detener la simulación local
				setSimulacionLocalActiva(false);
				setSimulacionActiva(false);

				// Limpiar intervalo de tiempo real
				if (intervalTiempoRealRef.current) {
					clearInterval(intervalTiempoRealRef.current);
					intervalTiempoRealRef.current = null;
				}

				alert('✅ Simulación semanal completada (7 días)');
				return;
			}

			relojLocalRef.current = nuevoTiempo;
			setRelojLocal(nuevoTiempo);
			setSimClock(nuevoTiempo);
			setTiempoSimulacionActual(nuevoTiempo.toISOString());

			// 🆕 IMPORTANTE: Actualizar tiempoSimulado para la interpolación de vuelos
			setTiempoSimulado(nuevoTiempo.getTime());

			// Debug cada 20 segundos aproximadamente (80 ticks @ 250ms)
			if (Math.random() < 0.0125) {
				const avionesEnPantalla = flightsInAirRef.current;
				const diasTranscurridos = (tiempoTranscurridoSimulado / (24 * 60 * 60 * 1000)).toFixed(2);
				console.log(`⏰ Reloj: ${nuevoTiempo.toISOString().slice(11, 19)} | Día ${diasTranscurridos}/7 | K=${K_CONSTANTE} | Aviones=${avionesEnPantalla}`);
			}
		}, TICK_REAL_MS);

		return () => clearInterval(interval);
	}, [simulacionLocalActiva]);

	// ==================== FUNCIONES PARA CONTROLAR SIMULACIÓN ====================
	const handleIniciarSimulacion = async () => {
		console.log("Iniciando simulación desde:", fechaInicioSimulacion, "a las", horaInicioSimulacion);

		// Validar fecha
		if (!fechaInicioSimulacion) {
			alert("Por favor, selecciona una fecha de inicio.");
			return;
		}

		// 1) Si ya está corriendo, no hacemos nada
		if (simulacionActiva) {
			console.log("La simulación ya está activa.");
			return;
		}

		// 2) Si ya hubo una simulación (simClock existe) y solo estaba pausada → reanudar
		if (simClock) {
			console.log("Reanudando simulación en:", simClock);
			setSimulacionActiva(true);
			return;
		}

		// 🔥 NUEVA LÓGICA: Usar WebSocket STOMP para la simulación
		try {
			// Conectar WebSocket si no está conectado
			if (!wsStompConectado) {
				console.log('🔌 WebSocket no conectado. Conectando automáticamente...');
				conectarWebSocketStomp();
				// Esperar un momento para que se conecte
				await new Promise(resolve => setTimeout(resolve, 1500));
			}

			// Verificar conexión nuevamente
			if (!stompClientRef.current?.connected) {
				console.log('⏳ Esperando conexión WebSocket...');
				await new Promise(resolve => setTimeout(resolve, 1000));
			}

			// Limpiar estado anterior
			console.log('🧹 Limpiando estado anterior...');
			setFlights([]);
			setFlightsInAir(0);
			setProgresoAG(null);
			contadorVuelosRef.current = 0;

			// Iniciar reloj local
			const inicioUTC = new Date(`${fechaInicioSimulacion}T${horaInicioSimulacion}:00Z`);
			setSimClock(inicioUTC);
			simStartRef.current = inicioUTC;
			setTiempoRealMs(0);
			setTickActual(0);
			setTimeScale(DESIRED_TIME_SCALE);

			// 🆕 INICIAR SISTEMA DE COLA Y RELOJ LOCAL (pero NO activar aún)
			setRelojLocal(inicioUTC);
			relojLocalRef.current = inicioUTC;
			setColaVuelos([]);
			colaVuelosRef.current = [];
			setVuelosEnAire([]);
			setKActual(kBase);
			setModoRalentizado(false);
			// ⏸️ NO ACTIVAR RELOJ AÚN - Se activa después del buffer de 15 segundos
			setSimulacionLocalActiva(false);
			setTiempoSimulacionActual(inicioUTC.toISOString()); // Mostrar en UI

			// 🆕 INICIALIZAR tiempoSimulado con la fecha de inicio (para interpolación)
			setTiempoSimulado(inicioUTC.getTime());
			// Reiniciar refs de tiempo del backend
			tiempoSimuladoBackendRef.current = null;
			setTiempoSimuladoBackend(null);

			// 🆕 ACTIVAR BUFFER DE 15 SEGUNDOS para acumular vuelos iniciales
			console.log(`⏳ ACTIVANDO BUFFER DE ${BUFFER_DELAY_MS / 1000} SEGUNDOS para acumular vuelos iniciales...`);
			setBufferActivo(true);
			vuelosBufferRef.current = [];

			// Limpiar timeout anterior si existe
			if (bufferTimeoutRef.current) {
				clearTimeout(bufferTimeoutRef.current);
			}

			// Después de 15 segundos, finalizar buffer y activar animación
			bufferTimeoutRef.current = setTimeout(() => {
				console.log(`✅ BUFFER COMPLETADO - ${vuelosBufferRef.current.length} vuelos acumulados`);
				setBufferActivo(false);

				// Procesar todos los vuelos acumulados
				if (vuelosBufferRef.current.length > 0) {
					console.log(`🚀 Procesando ${vuelosBufferRef.current.length} vuelos del buffer...`);
					// Combinar todos los vuelos en un solo array y setear flights
					setFlights(vuelosBufferRef.current);
					setFlightsInAir(vuelosBufferRef.current.filter(v => v.status === 'active').length);

					// 🚀 ACTIVAR RELOJ LOCAL
					setSimulacionLocalActiva(true);
					console.log(`🚀 Reloj local ACTIVADO - ${vuelosBufferRef.current.length} vuelos listos para animar`);
				} else {
					console.warn(`⚠️ Buffer vacío - no hay vuelos para animar, esperando más datos...`);
				}

				// Limpiar buffer
				vuelosBufferRef.current = [];
			}, BUFFER_DELAY_MS);

			// 🔄 Llamar al endpoint REST para iniciar simulación
			console.log('🚀 Llamando al backend para iniciar simulación...');
			const response = await fetch(`${API_BASE_URL}/api/simulations/start`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					fecha: fechaInicioSimulacion,
					hora: horaInicioSimulacion,
					factorK: 5
				})
			});

			if (!response.ok) {
				throw new Error(`Error HTTP: ${response.status}`);
			}

			const data = await response.json();
			console.log('📨 Respuesta del servidor:', data);

			if (data.sessionId) {
				setSessionId(data.sessionId);

				// Suscribirse al topic si hay conexión STOMP
				if (stompClientRef.current?.connected) {
					const topicUrl = `/topic/simulations/${data.sessionId}`;
					console.log(`📡 Suscribiéndose a: ${topicUrl}`);

					const subscription = stompClientRef.current.subscribe(
						topicUrl,
						(message) => {
							try {
								const datos = JSON.parse(message.body);
								console.log('📨 Mensaje WebSocket:', datos);
								procesarMensajeSimulacion(datos);
							} catch (error) {
								console.error('❌ Error parseando mensaje:', error);
							}
						}
					);
					subscriptionRef.current = subscription;
				}
			}

			// 🕐 INICIAR CRONÓMETRO DE TIEMPO REAL (para UI)
			tiempoInicioRef.current = Date.now();
			setTiempoRealTranscurrido(0);

			// Limpiar intervalo anterior si existe
			if (intervalTiempoRealRef.current) {
				clearInterval(intervalTiempoRealRef.current);
			}

			// Actualizar tiempo real cada segundo
			intervalTiempoRealRef.current = setInterval(() => {
				if (tiempoInicioRef.current) {
					const transcurrido = Math.floor((Date.now() - tiempoInicioRef.current) / 1000);
					setTiempoRealTranscurrido(transcurrido);
				}
			}, 1000);

			setSimulacionActiva(true);
			console.log('✅ Simulación iniciada correctamente');

		} catch (error) {
			console.error('❌ Error al iniciar simulación:', error);
			alert(`Error al iniciar simulación: ${error.message}`);
		}
	};

	const handleDetenerSimulacion = async () => {
		console.log("🛑 Deteniendo simulación...");
		setSimulacionActiva(false);

		// 🕐 DETENER CRONÓMETRO
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
			intervalTiempoRealRef.current = null;
		}

		// Cancelar simulación en el backend si hay sessionId
		if (sessionId) {
			try {
				const response = await fetch(`${API_BASE_URL}/api/simulations/${sessionId}/cancel`, {
					method: 'POST'
				});
				if (response.ok) {
					console.log('✅ Simulación cancelada en el servidor');
				}
			} catch (error) {
				console.error('⚠️ Error al cancelar en servidor:', error);
			}
		}

		// Desuscribirse del topic WebSocket
		if (subscriptionRef.current) {
			try {
				subscriptionRef.current.unsubscribe();
				subscriptionRef.current = null;
				console.log('📡 Desuscrito del topic');
			} catch (error) {
				console.error('⚠️ Error al desuscribirse:', error);
			}
		}

		// Limpiar estado local de animación
		setSimulacionLocalActiva(false);
		setColaVuelos([]);
		setVuelosEnAire([]);
		colaVuelosRef.current = [];
	};

	const handleResetSimulacion = () => {
		console.log("Simulación reiniciada completamente.");

		setSimulacionActiva(false);
		setSimClock(null);
		setTiempoRealMs(0);
		setTickActual(0);
		setFlights([]);
		setFlightsInAir(0);
		contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos

		// 🕐 RESETEAR Y DETENER CRONÓMETRO
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
			intervalTiempoRealRef.current = null;
		}
		tiempoInicioRef.current = null;
		setTiempoRealTranscurrido(0);

		// 📊 RESETEAR MÉTRICAS DE VUELOS
		setMetricasVuelos({
			totalRecibidosBackend: 0,
			totalEnEstadoFlights: 0,
			totalGraficados: 0,
			totalPerdidosAntesDeTiempo: 0,
			totalPerdidosDespuesDeTiempo: 0,
			ultimaActualizacion: null
		});
		vuelosRecibidosIdsRef.current = new Set();

		// Si deseas también resetear planificaciones recibidas:
		// setPlanFixed([]);
	};

	const handleToggleLegend = (event) => {
		// Si ya está abierto (anchorEl tiene valor), lo cierra
		// Si está cerrado (anchorEl es null), lo abre
		setLegendAnchorEl(legendAnchorEl ? null : event.currentTarget);
	};

	// Callback que se ejecuta cuando el botón se monta
	const handleControlButtonMount = (buttonElement) => {
		// Solo abrir automáticamente la primera vez
		if (!hasAutoOpened) {
			setTimeout(() => {
				setControlAnchorEl(buttonElement);
				setHasAutoOpened(true);
			}, 100);
		}
	};

	// Callback que se ejecuta cuando el botón se monta
	const handleMetricButtonMount = (buttonElement) => {
		// Solo abrir automáticamente la primera vez
		if (!hasAutoOpened) {
			setTimeout(() => {
				setMetricsAnchorEl(buttonElement);
				setHasAutoOpened(true);
			}, 100);
		}
	};

	const handleControlButtonClick = (event) => {
		if (controlAnchorEl) {
			setControlAnchorEl(null);
		} else {
			setControlAnchorEl(event.currentTarget);
		}
	};

	const handleMetricsButtonClick = (event) => {
		if (metricsAnchorEl) {
			setMetricsAnchorEl(null);
		} else {
			setMetricsAnchorEl(event.currentTarget);
		}
	};

	// ==================== FUNCIONES WEBSOCKET DE PLANIFICACIÓN ====================

	/**
	 * Convertir vuelo de planificación al formato del mapa
	 */
	const convertirVueloPlanificacionAMapa = (vuelo) => {
		// Buscar coordenadas de aeropuertos (comparación case-insensitive)
		const origen = airports.find(a =>
			String(a.code).toUpperCase() === String(vuelo.origenCodigoICAO).toUpperCase()
		);
		const destino = airports.find(a =>
			String(a.code).toUpperCase() === String(vuelo.destinoCodigoICAO).toUpperCase()
		);

		if (!origen || !destino) {
			console.warn(`⚠️ Aeropuertos no encontrados: ${vuelo.origenCodigoICAO} o ${vuelo.destinoCodigoICAO}`);
			console.log('📍 Aeropuertos disponibles:', airports.map(a => a.code));
			return null;
		}

		// Calcular progreso basado en fechas
		let progress = 0.0;
		try {
			const fechaInicio = new Date(vuelo.fechaInicial);
			const fechaFin = new Date(vuelo.fechaFinal);
			const ahora = new Date();

			const totalDuracion = fechaFin - fechaInicio;
			const transcurrido = ahora - fechaInicio;

			if (totalDuracion > 0 && transcurrido > 0) {
				progress = Math.max(0, Math.min(1, transcurrido / totalDuracion));
			} else {
				progress = 0.1; // 10% del trayecto por defecto
			}
		} catch (e) {
			console.warn('⚠️ Error calculando progreso:', e);
			progress = 0.1;
		}

		// Interpolación de posición
		const currentLat = origen.lat + (destino.lat - origen.lat) * progress;
		const currentLng = origen.lng + (destino.lng - origen.lng) * progress;

		// Calcular rotación usando bearingDegrees
		const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
		const rotation = (brg - 90 + 360) % 360;

		// Determinar estado del vuelo
		const status = progress >= 1 ? 'arrived' : (progress <= 0 ? 'scheduled' : 'active');

		// Calcular total de paquetes
		const totalPaquetes = vuelo.pedidos?.reduce((sum, p) => sum + (p.cantidad || 0), 0) || 0;

		// Generar ID único con contador incremental (SIN Date.now() para evitar duplicados)
		contadorVuelosRef.current += 1;
		const idUnico = `PL-${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${contadorVuelosRef.current}`;

		return {
			id: idUnico,
			origin: {
				code: vuelo.origenCodigoICAO,
				lat: origen.lat,
				lng: origen.lng,
				region: origen.region
			},
			destination: {
				code: vuelo.destinoCodigoICAO,
				lat: destino.lat,
				lng: destino.lng,
				region: destino.region
			},
			progress,
			altitude: progress > 0 && progress < 1 ? 35000 : 0,
			speed: progress > 0 && progress < 1 ? 850 : 0,
			status,
			currentLat,
			currentLng,
			aircraftColor: '#3b82f6', // Azul para vuelos de planificación
			rotation,
			packageCapacity: totalPaquetes,
			currentPackages: totalPaquetes,
			packageType: 'MPE',
			isSameContinentFlight: origen.region === destino.region,
			// Datos adicionales de planificación
			fechaInicial: vuelo.fechaInicial,
			fechaFinal: vuelo.fechaFinal,
			pedidos: vuelo.pedidos || []
		};
	};

	// ==================== FUNCIONES DE PLANIFICACIÓN ====================
	// Nota: handleConectarWsPlanificacion fue eliminada - ahora usamos el hook usePlanificacionWebSocket
	// que maneja la conexión automáticamente con reconexión y heartbeat

	const handleIniciarPlanificacion = () => {
		// ✨ Usar el nuevo hook para verificar conexión
		if (!wsConnected) {
			console.warn('⚠️ WebSocket no conectado, esperando...');
			alert('WebSocket no conectado. Espera un momento...');
			return;
		}

		if (!fechaInicioSimulacion) {
			console.warn('⚠️ No hay fecha de inicio seleccionada');
			alert('Por favor, selecciona una fecha de inicio');
			return;
		}

		// No iniciar si ya hay una planificación en curso
		if (estadoPlanificacion === 'running') {
			console.warn('⚠️ Ya hay una planificación en curso');
			return;
		}

		// 🧹 LIMPIAR TODO para nueva planificación
		console.log('🧹 Limpiando estado para nueva planificación...');
		setIteracionesPlanificacion([]);
		wsLimpiarIteraciones(); // ✨ También limpiar en el hook
		setFlights([]); // Limpiar vuelos del mapa
		setFlightsInAir(0);
		setIntentosRealizados(0);
		setEstadoPlanificacion('running');
		contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos

		// 🕐 INICIAR TIEMPO DE SIMULACIÓN (basado en fecha y hora elegida por usuario)
		const fechaSimulacion = new Date(`${fechaInicioSimulacion}T${horaInicioSimulacion}:00Z`);
		simStartRef.current = fechaSimulacion;
		setSimClock(fechaSimulacion);
		console.log('🕐 Tiempo de simulación iniciado en:', fechaSimulacion.toISOString());

		// 🕐 INICIAR CRONÓMETRO DE TIEMPO REAL (para UI)
		tiempoInicioRef.current = Date.now();
		setTiempoRealTranscurrido(0);

		// Actualizar tiempo real cada segundo
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
		}
		intervalTiempoRealRef.current = setInterval(() => {
			if (tiempoInicioRef.current) {
				const transcurrido = Math.floor((Date.now() - tiempoInicioRef.current) / 1000);
				setTiempoRealTranscurrido(transcurrido);
			}
		}, 1000);

		console.log('🚀 Iniciando planificación para fecha:', fechaInicioSimulacion);

		// ✨ Usar la función del hook para iniciar planificación
		iniciarPlanificacion(
			fechaInicioSimulacion,
			5, // Factor K para simulación semanal
			{
				tamanioPoblacion: 20,
				maxGeneraciones: 20,
				limiteGeneracionesSinMejora: 10,
				hora: horaInicioSimulacion
			}
		).then(sessionId => {
			console.log('✅ Planificación iniciada con ID:', sessionId);
		}).catch(error => {
			console.error('❌ Error al iniciar planificación:', error);
			setEstadoPlanificacion('error');
		});
	};

	/**
	 * Limpiar iteraciones de planificación
	 */
	const handleLimpiarIteraciones = () => {
		setIteracionesPlanificacion([]);
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false); // Permitir nuevo auto-inicio
		wsLimpiarIteraciones(); // También limpiar en el hook
	};

	/**
	 * Detener planificación continua y limpiar mapa
	 */
	const handleDetenerPlanificacion = () => {
		console.log('🛑 Deteniendo planificación continua...');

		// Detener cronómetro
		if (intervalTiempoRealRef.current) {
			clearInterval(intervalTiempoRealRef.current);
			intervalTiempoRealRef.current = null;
		}

		// Detener planificación en el hook
		wsDetenerPlanificacion();

		// Resetear estado
		setEstadoPlanificacion('idle');
		setAutoInicioIntentado(false);

		console.log('✅ Planificación detenida. Vuelos permanecen en el mapa.');
	};

	/**
	 * Limpiar todos los vuelos del mapa
	 */
	const handleLimpiarMapa = () => {
		console.log('🧹 Limpiando mapa y reseteando simulación...');

		// Limpiar vuelos
		setFlights([]);
		setFlightsInAir(0);
		setIteracionesPlanificacion([]);
		setIntentosRealizados(0);
		contadorVuelosRef.current = 0;

		// ✅ RESETEAR TIEMPO SIMULADO
		setTiempoSimuladoBackend(null);
		tiempoSimuladoBackendRef.current = null; // 🆕 También resetear la ref
		setUltimaActualizacionReal(null);
		setTiempoMovimiento(0);
		setTiempoSimulado(Date.now());

		// Limpiar progreso
		setProgresoAG(null);
		setMensajesSimulacion([]);

		console.log('✅ Mapa limpiado y simulación reseteada');
	};

	// 🆕 EFECTO: Registrar callback para procesar mensajes del WebSocket mejorado
	useEffect(() => {
		wsOnMessage((data) => {
			console.log('📩 [Hook] Mensaje de planificación:', data);

			// Procesar tiempo simulado
			if (data.datos?.tiempoSimulacionActual) {
				let timestampStr = data.datos.tiempoSimulacionActual;
				if (typeof timestampStr === 'string' && !timestampStr.endsWith('Z')) {
					timestampStr = timestampStr + 'Z';
				}
				const timestampMs = typeof timestampStr === 'string'
					? new Date(timestampStr).getTime()
					: timestampStr;
				setTiempoSimulacionActual(data.datos.tiempoSimulacionActual);
				setTiempoSimuladoBackend(timestampMs);
				setTiempoSimulado(timestampMs);
				tiempoSimuladoBackendRef.current = timestampMs;
				setUltimaActualizacionReal(Date.now());
				setTiempoMovimiento(0);
			}

			// Procesar vuelos si vienen en la solución
			if (data.solucion?.vuelos && data.solucion.vuelos.length > 0) {
				console.log(`✈️ [Hook] Procesando ${data.solucion.vuelos.length} vuelos`);

				const vuelosParaMapa = data.solucion.vuelos
					.map(convertirVueloPlanificacionAMapa)
					.filter(v => v !== null);

				if (vuelosParaMapa.length > 0) {
					setFlights(prevFlights => {
						const flightsMap = new Map(prevFlights.map(f => [f.id, f]));
						vuelosParaMapa.forEach(vuelo => flightsMap.set(vuelo.id, vuelo));
						return Array.from(flightsMap.values());
					});
					setFlightsInAir(prev => prev + vuelosParaMapa.length);
				}

				// Si es tipo completado, solicitar siguiente iteración
				if (data.tipo === 'completado') {
					const nuevoIntento = intentosRealizados + 1;
					if (nuevoIntento < 1000) {
						setIntentosRealizados(nuevoIntento);
						setTimeout(() => {
							iniciarPlanificacion(fechaInicioSimulacion, 5, {
								tamanioPoblacion: 20,
								maxGeneraciones: 20,
								hora: horaInicioSimulacion
							});
						}, 100);
					}
				}
			}
		});
	}, [wsOnMessage, fechaInicioSimulacion, horaInicioSimulacion, intentosRealizados, iniciarPlanificacion]);

	// 🆕 Limpiar intervalo de tiempo real al desmontar
	useEffect(() => {
		return () => {
			if (intervalTiempoRealRef.current) {
				clearInterval(intervalTiempoRealRef.current);
			}
		};
	}, []);

	// Auto-iniciar planificación cuando se selecciona fecha y WebSocket está conectado
	useEffect(() => {
		// Solo auto-iniciar si:
		// 1. WebSocket está conectado
		// 2. Hay fecha seleccionada
		// 3. No se ha intentado auto-iniciar aún
		// 4. No hay planificación en curso (estado idle)
		if (wsConnected && fechaInicioSimulacion && !autoInicioIntentado && estadoPlanificacion === 'idle') {
			console.log('🚀 Auto-iniciando planificación...');
			setAutoInicioIntentado(true);

			// Pequeño delay para asegurar que la conexión esté estable
			setTimeout(() => {
				handleIniciarPlanificacion();
			}, 500);
		}
	}, [wsConnected, fechaInicioSimulacion, autoInicioIntentado, estadoPlanificacion]);

	// Resetear flag cuando cambia la fecha
	useEffect(() => {
		setAutoInicioIntentado(false);
	}, [fechaInicioSimulacion]);

	// ==================== FUNCIONES WEBSOCKET STOMP (SIMULACIÓN SEMANAL) ====================

	/**
	 * Conectar WebSocket STOMP
	 */
	const conectarWebSocketStomp = useCallback(() => {
		if (stompClientRef.current && stompClientRef.current.active) {
			console.log('⚠️ WebSocket STOMP ya está conectado');
			return;
		}

		console.log('📡 Conectando WebSocket STOMP...');
		setEstadoSimulacionStomp('connecting');

		const socket = new SockJS(WS_URL);

		const stompClient = new Client({
			webSocketFactory: () => socket,
			reconnectDelay: 5000,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,

			onConnect: () => {
				console.log('✅ WebSocket STOMP conectado');
				setWsStompConectado(true);
				setEstadoSimulacionStomp('connected');
				agregarMensaje('✅ Conexión WebSocket establecida', 'success');
			},

			onStompError: (frame) => {
				console.error('❌ Error STOMP:', frame.headers['message']);
				console.error('Detalles:', frame.body);
				setEstadoSimulacionStomp('error');
				agregarMensaje(`❌ Error STOMP: ${frame.headers['message']}`, 'error');
			},

			onWebSocketError: (error) => {
				console.error('❌ Error WebSocket:', error);
				setWsStompConectado(false);
				setEstadoSimulacionStomp('error');
				agregarMensaje('❌ Error de conexión WebSocket', 'error');
			},

			onDisconnect: () => {
				console.log('🔌 WebSocket STOMP desconectado');
				setWsStompConectado(false);
				setEstadoSimulacionStomp('disconnected');
				agregarMensaje('🔌 WebSocket desconectado', 'warning');
			}
		});

		stompClient.activate();
		stompClientRef.current = stompClient;
	}, []);

	/**
	 * Desconectar WebSocket STOMP
	 */
	const desconectarWebSocketStomp = useCallback(() => {
		if (subscriptionRef.current) {
			subscriptionRef.current.unsubscribe();
			subscriptionRef.current = null;
		}

		if (stompClientRef.current) {
			stompClientRef.current.deactivate();
			stompClientRef.current = null;
		}

		setWsStompConectado(false);
		setEstadoSimulacionStomp('disconnected');
		console.log('🔌 WebSocket STOMP desconectado completamente');
	}, []);

	// 🆔 Contador para IDs únicos de mensajes
	const contadorMensajesRef = useRef(0);

	/**
	 * Agregar mensaje al log
	 */
	const agregarMensaje = useCallback((mensaje, tipo = 'info') => {
		contadorMensajesRef.current += 1;
		const nuevoMensaje = {
			id: `msg-${Date.now()}-${contadorMensajesRef.current}-${Math.random().toString(36).substr(2, 9)}`,
			texto: mensaje,
			tipo, // success, error, warning, info
			timestamp: new Date().toLocaleTimeString('es-ES')
		};
		setMensajesSimulacion(prev => [nuevoMensaje, ...prev].slice(0, 50));
	}, []);

	/**
	 * Iniciar simulación semanal con WebSocket STOMP
	 */
	const iniciarSimulacionWebSocketStomp = useCallback(async () => {
		if (!fechaInicioSimulacion) {
			alert('Por favor, selecciona una fecha de inicio');
			return;
		}

		if (!wsStompConectado) {
			alert('WebSocket no conectado. Conectando...');
			conectarWebSocketStomp();
			return;
		}

		try {
			console.log('🚀 Iniciando simulación semanal con WebSocket STOMP...');

			// 🧹 LIMPIAR VUELOS ANTERIORES antes de iniciar nueva simulación
			console.log('🧹 Limpiando vuelos anteriores...');
			setFlights([]);
			setFlightsInAir(0);
			setProgresoAG(null);
			setMensajesSimulacion([]);
			contadorVuelosRef.current = 0; // 🆔 Resetear contador de IDs únicos

			// 📊 RESETEAR MÉTRICAS DE VUELOS
			setMetricasVuelos({
				totalRecibidosBackend: 0,
				totalEnEstadoFlights: 0,
				totalGraficados: 0,
				totalPerdidosAntesDeTiempo: 0,
				totalPerdidosDespuesDeTiempo: 0,
				ultimaActualizacion: null
			});
			vuelosRecibidosIdsRef.current = new Set();

			setEstadoSimulacionStomp('running');
			agregarMensaje(`🚀 Iniciando simulación para ${fechaInicioSimulacion} a las ${horaInicioSimulacion}`, 'info');

			// 1. Llamar al endpoint REST para iniciar
			const response = await fetch(`${API_BASE_URL}/api/simulations/start`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					fecha: fechaInicioSimulacion,
					hora: horaInicioSimulacion, // Hora de inicio (formato HH:mm)
					factorK: 5 // Factor K fijo para simulación semanal
				})
			});

			if (!response.ok) {
				throw new Error(`Error HTTP: ${response.status}`);
			}

			const data = await response.json();
			console.log('📨 Respuesta del servidor:', data);

			if (!data.sessionId) {
				throw new Error('No se recibió sessionId del servidor');
			}

			setSessionId(data.sessionId);
			agregarMensaje(`✅ Simulación iniciada - Session ID: ${data.sessionId}`, 'success');

			// 2. Suscribirse al topic del WebSocket
			const topicUrl = `/topic/simulations/${data.sessionId}`;
			console.log(`📡 Suscribiéndose a: ${topicUrl}`);

			const subscription = stompClientRef.current.subscribe(
				topicUrl,
				(message) => {
					try {
						const datos = JSON.parse(message.body);
						console.log('📨 Mensaje recibido:', datos);
						procesarMensajeSimulacion(datos);
					} catch (error) {
						console.error('❌ Error parseando mensaje:', error);
						agregarMensaje('❌ Error procesando mensaje del servidor', 'error');
					}
				}
			);

			subscriptionRef.current = subscription;
			agregarMensaje(`📡 Suscrito a: ${topicUrl}`, 'success');

		} catch (error) {
			console.error('❌ Error iniciando simulación:', error);
			setEstadoSimulacionStomp('error');
			agregarMensaje(`❌ Error: ${error.message}`, 'error');
			alert(`Error al iniciar simulación: ${error.message}`);
		}
	}, [fechaInicioSimulacion, wsStompConectado, conectarWebSocketStomp, agregarMensaje]);

	/**
	 * Procesar mensajes recibidos del WebSocket
	 */
	const procesarMensajeSimulacion = useCallback((datos) => {
		// Determinar tipo de mensaje
		if (datos.tipo === 'PROGRESO_AG') {
			// Mensaje de progreso del Algoritmo Genético
			console.log(`🧬 Progreso AG - Generación ${datos.generacion}/${datos.maxGeneraciones}`);

			// 🕐 ACTUALIZAR TIEMPO SIMULADO DEL BACKEND (convertir a timestamp)
			if (datos.fechaSimulada) {
				// 🆕 FIX: Forzar interpretación como UTC si no tiene 'Z'
				let fechaStr = datos.fechaSimulada;
				if (typeof fechaStr === 'string' && !fechaStr.endsWith('Z')) {
					fechaStr = fechaStr + 'Z';
				}
				const timestampSimulado = new Date(fechaStr).getTime();

				// ✅ ACTUALIZAR ESTADO Y REF
				setTiempoSimuladoBackend(timestampSimulado);
				setTiempoSimulado(timestampSimulado); // 🆕 CRÍTICO: Sincronizar tiempoSimulado para interpolación
				tiempoSimuladoBackendRef.current = timestampSimulado; // 🆕 Sincronizar ref
				setUltimaActualizacionReal(Date.now());
				setTiempoMovimiento(0); // Resetear movimiento

				// 🆕 ACTUALIZAR tiempoSimulacionActual para mostrar en UI
				setTiempoSimulacionActual(datos.fechaSimulada);

				console.log(`⏰ Backend - Tiempo simulado actualizado: ${fechaStr} -> ${new Date(timestampSimulado).toISOString()}`);
			}

			setProgresoAG({
				generacion: datos.generacion,
				maxGeneraciones: datos.maxGeneraciones,
				progreso: datos.progreso,
				mejorFitness: datos.mejorFitness,
				fitnessPromedio: datos.fitnessPromedio,
				pedidosProcesados: datos.pedidosProcesados,
				pedidosTotales: datos.pedidosTotales,
				fechaSimulada: datos.fechaSimulada
			});

			agregarMensaje(
				`🧬 Generación ${datos.generacion}/${datos.maxGeneraciones} - Fitness: ${datos.mejorFitness?.toFixed(2)} - Progreso: ${datos.progreso?.toFixed(1)}%`,
				'info'
			);

			// 🔥 NUEVO: Procesar vuelos directos (no rutas con subRutas)
			// Usar refs para llamar a las funciones más recientes (evita closure problem)
			if (datos.solucion?.vuelos && datos.solucion.vuelos.length > 0) {
				console.log(`✈️ Procesando ${datos.solucion.vuelos.length} vuelos directos...`);
				if (procesarVuelosDirectosRef.current) {
					procesarVuelosDirectosRef.current(datos.solucion.vuelos);
				}
			}
			// Fallback: Si viene en formato antiguo (rutas con subRutas)
			else if (datos.solucion?.rutas) {
				console.log(`✈️ Procesando ${datos.solucion.rutas.length} rutas...`);
				if (procesarRutasSimulacionRef.current) {
					procesarRutasSimulacionRef.current(datos.solucion.rutas);
				}
			}

			// 🆕 ACTUALIZAR AEROPUERTOS con datos de ocupación del backend
			if (datos.solucion?.aeropuertos && datos.solucion.aeropuertos.length > 0) {
				console.log(`🏢 Actualizando ocupación de ${datos.solucion.aeropuertos.length} aeropuertos...`);
				setAirports(prevAirports => {
					return prevAirports.map(airport => {
						// Buscar datos actualizados del backend por código
						const backendData = datos.solucion.aeropuertos.find(
							a => a.code === airport.code || a.codigo === airport.code
						);
						if (backendData) {
							return {
								...airport,
								packages: backendData.packages || backendData.ocupacionActual || 0,
								// Actualizar capacidad si viene (puede ser string "ILIMITADO" o número)
								capacity: backendData.capacity !== undefined ? backendData.capacity : airport.capacity
							};
						}
						return airport;
					});
				});
			}

		} else if (datos.type === 'PROGRESS' || datos.status === 'RUNNING') {
			// 🆕 NUEVA ESTRUCTURA: SimulationMessage con snapshot
			console.log(`🎮 Simulación corriendo - Snapshot recibido`);

			if (datos.snapshot) {
				const snapshot = datos.snapshot;
				console.log(`📊 Snapshot: ${snapshot.processedOrders}/${snapshot.totalOrders} pedidos procesados`);
				console.log(`📈 Fitness: ${snapshot.fitness?.toFixed(4)}`);

				agregarMensaje(
					`🎮 Progreso: ${snapshot.processedOrders}/${snapshot.totalOrders} pedidos - Fitness: ${snapshot.fitness?.toFixed(4)}`,
					'info'
				);

				// 🆕 PROCESAR SEGMENTS del snapshot (nueva estructura)
				if (snapshot.orderPlans && snapshot.orderPlans.length > 0) {
					console.log(`✈️ Procesando segments del snapshot...`);
					if (procesarSegmentsSnapshotRef.current) {
						procesarSegmentsSnapshotRef.current(snapshot);
					}
				}

				// 🆕 ACTUALIZAR AEROPUERTOS con datos de ocupación del snapshot
				if (snapshot.aeropuertos && snapshot.aeropuertos.length > 0) {
					console.log(`🏢 [Snapshot] Actualizando ocupación de ${snapshot.aeropuertos.length} aeropuertos...`);
					setAirports(prevAirports => {
						return prevAirports.map(airport => {
							// Buscar datos actualizados del snapshot por código
							const backendData = snapshot.aeropuertos.find(
								a => a.codigo === airport.code || a.code === airport.code
							);
							if (backendData) {
								return {
									...airport,
									// ocupacionActual = paquetes actuales en el almacén
									packages: backendData.ocupacionActual || backendData.packages || 0,
									// pedidosAlmacenados = número de pedidos (diferente a cantidad de paquetes)
									pedidosCount: backendData.pedidosAlmacenados || 0,
									// capacidadAlmacen = capacidad total
									capacity: backendData.capacidadAlmacen || airport.capacity
								};
							}
							return airport;
						});
					});
				}
			}
			// Fallback para estructura antigua
			else if (datos.solution?.routes) {
				console.log(`✈️ [Formato antiguo] Procesando ${datos.solution.routes.length} rutas...`);
				if (procesarRutasSnapshotRef.current) {
					procesarRutasSnapshotRef.current(datos.solution.routes);
				}
			}

		} else if (datos.type === 'COMPLETED' || datos.status === 'COMPLETED') {
			// Simulación completada
			console.log('🎉 Simulación completada exitosamente');
			setEstadoSimulacionStomp('completed');
			agregarMensaje('🎉 Simulación completada exitosamente', 'success');

			// 🆕 Procesar snapshot final si existe
			if (datos.snapshot?.orderPlans) {
				console.log(`✈️ Procesando snapshot final...`);
				if (procesarSegmentsSnapshotRef.current) {
					procesarSegmentsSnapshotRef.current(datos.snapshot);
				}
			}
			// Fallback para formato antiguo
			else if (datos.solution?.routes) {
				if (procesarRutasSnapshotRef.current) {
					procesarRutasSnapshotRef.current(datos.solution.routes);
				}
			}

			// Desuscribirse del topic
			if (subscriptionRef.current) {
				subscriptionRef.current.unsubscribe();
				subscriptionRef.current = null;
			}

		} else if (datos.tipo === 'ERROR') {
			// Error en la simulación
			console.error('❌ Error en simulación:', datos.mensaje);
			setEstadoSimulacionStomp('error');
			agregarMensaje(`❌ Error: ${datos.mensaje}`, 'error');
		}
	}, [agregarMensaje]); // 🔥 No incluir funciones de procesamiento (causa circular reference)

	/**
	 * 🆕 Procesar vuelos DIRECTOS (formato nuevo del backend)
	 * Formato: {origenCodigoICAO, destinoCodigoICAO, fechaInicial, fechaFinal, pedidos, totalPaquetes}
	 */
	/**
	 * 🆕 Procesar segments del snapshot (nueva estructura JSON)
	 * Extrae todos los segments de orderPlans → routes → segments
	 */
	const procesarSegmentsSnapshot = useCallback((snapshot) => {
		const currentAirports = airportsRef.current;
		console.log(`\n🔍 procesarSegmentsSnapshot - Snapshot recibido`);
		console.log(`📍 Aeropuertos disponibles: ${currentAirports.length}`);
		console.log(`⏰ Tiempo simulado: ${snapshot.generatedAt}`);

		if (!snapshot.orderPlans || snapshot.orderPlans.length === 0) {
			console.warn('⚠️ No hay orderPlans en el snapshot');
			return;
		}

		const nuevosVuelos = [];
		let segmentIndex = 0;

		// Iterar por cada orderPlan
		snapshot.orderPlans.forEach((orderPlan, orderIndex) => {
			const orderId = orderPlan.orderId;
			const orderSlackMinutes = orderPlan.slackMinutes;

			console.log(`\n📦 Pedido ${orderIndex + 1}/${snapshot.orderPlans.length}: ${orderId}`);
			console.log(`   Holgura: ${orderSlackMinutes} minutos ${orderSlackMinutes > 0 ? '✅' : '⚠️'}`);

			// Iterar por cada ruta del pedido
			orderPlan.routes.forEach((route, routeIndex) => {
				console.log(`   📍 Ruta ${routeIndex + 1}: ${route.segments.length} segmentos`);

				// Iterar por cada segment (VUELO) de la ruta
				route.segments.forEach((segment, segIndex) => {
					segmentIndex++;

					console.log(`\n   ✈️ Segment ${segmentIndex}: ${segment.flightId}`);
					console.log(`      ${segment.origin} → ${segment.destination}`);
					console.log(`      Despegue: ${segment.departureUtc}`);
					console.log(`      Llegada:  ${segment.arrivalUtc}`);
					console.log(`      Cantidad: ${segment.quantity} paquetes`);

					// Buscar aeropuertos de origen y destino
					const origen = currentAirports.find(a =>
						a.code.toUpperCase() === segment.origin.toUpperCase()
					);
					const destino = currentAirports.find(a =>
						a.code.toUpperCase() === segment.destination.toUpperCase()
					);

					if (!origen) {
						console.error(`      ❌ Aeropuerto ORIGEN no encontrado: "${segment.origin}"`);
						return;
					}

					if (!destino) {
						console.error(`      ❌ Aeropuerto DESTINO no encontrado: "${segment.destination}"`);
						return;
					}

					console.log(`      ✅ Origen: ${origen.code} [${origen.lat.toFixed(2)}, ${origen.lng.toFixed(2)}]`);
					console.log(`      ✅ Destino: ${destino.code} [${destino.lat.toFixed(2)}, ${destino.lng.toFixed(2)}]`);

					// Calcular rotación del avión
					const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
					const rotation = (brg - 90 + 360) % 360;

					// Determinar estado basado en holgura
					let status = 'active';
					if (orderSlackMinutes <= 0) {
						status = 'retrasado';
					}

					// Posición inicial (será calculada por interpolación)
					const progress = 0;
					const currentLat = origen.lat;
					const currentLng = origen.lng;

					// 🆕 Crear ID único combinando múltiples factores para evitar duplicados
					const uniqueId = `SNAP-${segment.flightId}-${orderId}-${routeIndex}-${segIndex}-${segmentIndex}-${Math.random().toString(36).substr(2, 6)}`;

					// 🆕 Crear objeto de pedido para este segment
					const pedidoSegment = {
						idPedido: orderId,
						cantidad: segment.quantity,
						slackMinutes: orderSlackMinutes,
						origen: segment.origin,
						destino: segment.destination
					};

					// 🆕 Crear objeto de vuelo con timestamps del segment
					const nuevoVuelo = {
						id: uniqueId,
						flightId: segment.flightId,
						origin: {
							code: segment.origin,
							lat: origen.lat,
							lng: origen.lng,
							region: origen.region
						},
						destination: {
							code: segment.destination,
							lat: destino.lat,
							lng: destino.lng,
							region: destino.region
						},
						// ⏰ TIMESTAMPS PARA INTERPOLACIÓN HÍBRIDA
						fechaInicial: segment.departureUtc,
						fechaFinal: segment.arrivalUtc,
						// Información del pedido
						pedidoId: orderId,
						pedidos: [pedidoSegment], // 🆕 ARRAY DE PEDIDOS
						slackMinutes: orderSlackMinutes,
						// Posición y estado
						progress,
						currentLat,
						currentLng,
						rotation,
						status,
						// Metadatos
						altitude: 35000,
						speed: 850,
						packageCapacity: segment.quantity,
						currentPackages: segment.quantity,
						packageType: 'SNAPSHOT',
						isSameContinentFlight: origen.region === destino.region,
						aircraftColor: orderSlackMinutes <= 0 ? '#ef4444' : '#3b82f6', // Rojo si retrasado
					};

					console.log(`      ✅ Vuelo creado con interpolación temporal`);
					console.log(`      🎬 Sistema híbrido ACTIVADO`);

					nuevosVuelos.push(nuevoVuelo);
				});
			});
		});

		console.log(`\n📊 ============================================`);
		console.log(`📊 Total de segments procesados: ${segmentIndex}`);
		console.log(`📊 Total de vuelos creados: ${nuevosVuelos.length}`);
		const vuelosConInterpolacion = nuevosVuelos.filter(v => v.fechaInicial && v.fechaFinal).length;
		console.log(`🎬 Vuelos con interpolación temporal: ${vuelosConInterpolacion}/${nuevosVuelos.length}`);
		console.log(`📊 ============================================\n`);

		if (nuevosVuelos.length > 0) {
			console.log(`🗺️ Primer vuelo (ejemplo):`, {
				id: nuevosVuelos[0].id,
				flightId: nuevosVuelos[0].flightId,
				from: nuevosVuelos[0].origin.code,
				to: nuevosVuelos[0].destination.code,
				departure: nuevosVuelos[0].fechaInicial,
				arrival: nuevosVuelos[0].fechaFinal,
				orderId: nuevosVuelos[0].pedidoId,
				status: nuevosVuelos[0].status
			});

			// 🔥 ELIMINAR DUPLICADOS usando Map
			const flightsMap = new Map(nuevosVuelos.map(v => [v.id, v]));
			const vuelosUnicos = Array.from(flightsMap.values());

			if (vuelosUnicos.length < nuevosVuelos.length) {
				console.warn(`⚠️ Se encontraron ${nuevosVuelos.length - vuelosUnicos.length} vuelos duplicados, eliminados`);
			}

			// 🔥 REEMPLAZAR todos los vuelos (sin duplicados)
			console.log(`🔄 Reemplazando flights array con ${vuelosUnicos.length} vuelos únicos`);
			setFlights(vuelosUnicos);

			// 🆕 AUTO-INICIALIZAR TIEMPO SIMULADO basado en los vuelos
			// Si el backend no envía tiempoSimulacionActual, usar la fecha más temprana de los vuelos
			const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
			if (vuelosConFecha.length > 0) {
				const fechaMasTemprana = Math.min(
					...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime())
				);

				// Verificar si tiempoSimuladoBackend aún no está establecido usando ref
				if (tiempoSimuladoBackendRef.current === null) {
					console.log(`🕐 AUTO-INICIALIZANDO tiempo simulado a: ${new Date(fechaMasTemprana).toISOString()}`);
					tiempoSimuladoBackendRef.current = fechaMasTemprana; // Marcar como inicializado
					setTiempoSimuladoBackend(fechaMasTemprana);
					setTiempoSimulado(fechaMasTemprana); // 🆕 CRÍTICO: Sincronizar tiempoSimulado para interpolación
					setTiempoSimulacionActual(new Date(fechaMasTemprana).toISOString()); // 🆕 Actualizar UI
					setUltimaActualizacionReal(Date.now());
					setTiempoMovimiento(0);
				}
			}
		}
	}, []);

	procesarSegmentsSnapshotRef.current = procesarSegmentsSnapshot;

	const procesarVuelosDirectos = useCallback((vuelos) => {
		const currentAirports = airportsRef.current; // 🔥 Usar ref para tener valor actual
		console.log(`\n🔍 procesarVuelosDirectos - Recibidos ${vuelos?.length || 0} vuelos`);
		console.log(`📍 Aeropuertos disponibles: ${currentAirports.length}`);

		if (!vuelos || vuelos.length === 0) {
			console.warn('⚠️ No hay vuelos para procesar');
			return;
		}

		const nuevosVuelos = [];
		const baseTimestamp = Date.now(); // 🆕 Timestamp base para todo el lote

		vuelos.forEach((vuelo, index) => {
			console.log(`\n✈️ Vuelo ${index + 1}/${vuelos.length}`);
			console.log(`   Origen: ${vuelo.origenCodigoICAO} → Destino: ${vuelo.destinoCodigoICAO}`);
			console.log(`   Paquetes: ${vuelo.totalPaquetes}`);

			// Buscar aeropuertos de origen y destino (case-insensitive)
			const origen = currentAirports.find(a =>
				a.code.toUpperCase() === vuelo.origenCodigoICAO.toUpperCase()
			);
			const destino = currentAirports.find(a =>
				a.code.toUpperCase() === vuelo.destinoCodigoICAO.toUpperCase()
			);

			if (!origen) {
				console.error(`   ❌ Aeropuerto ORIGEN no encontrado: "${vuelo.origenCodigoICAO}"`);
				console.log(`   📋 Aeropuertos disponibles (primeros 5):`, currentAirports.slice(0, 5).map(a => a.code));
				return;
			}

			if (!destino) {
				console.error(`   ❌ Aeropuerto DESTINO no encontrado: "${vuelo.destinoCodigoICAO}"`);
				console.log(`   📋 Aeropuertos disponibles (primeros 5):`, currentAirports.slice(0, 5).map(a => a.code));
				return;
			}

			console.log(`   ✅ Origen: ${origen.code} [${origen.lat}, ${origen.lng}]`);
			console.log(`   ✅ Destino: ${destino.code} [${destino.lat}, ${destino.lng}]`);

			// 🆕 USAR FECHA DE SIMULACIÓN (no fecha actual del sistema)
			const tieneFechas = vuelo.fechaInicial && vuelo.fechaFinal;
			if (tieneFechas) {
				const salida = new Date(vuelo.fechaInicial).getTime();
				const llegada = new Date(vuelo.fechaFinal).getTime();

				console.log(`   ⏰ Fechas: ${vuelo.fechaInicial} → ${vuelo.fechaFinal}`);
				console.log(`   ✈️ Vuelo configurado correctamente para animación`);
			} else {
				console.log(`   ⚠️ Sin fechas de vuelo - usando posición estática`);
			}

			// 🔥 Usar progreso fijo para visualización inmediata (si no hay fechas)
			const progress = 0.5; // Mitad del recorrido

			// Interpolación de posición inicial
			const currentLat = origen.lat + (destino.lat - origen.lat) * progress;
			const currentLng = origen.lng + (destino.lng - origen.lng) * progress;

			// Calcular rotación
			const brg = bearingDegrees(origen.lat, origen.lng, destino.lat, destino.lng);
			const rotation = (brg - 90 + 360) % 360;

			// 🆕 ID único ESTABLE: origen + destino + fechaInicial (sin timestamps aleatorios)
			// Esto evita crear duplicados cuando el backend envía el mismo vuelo múltiples veces
			const vueloKey = `${vuelo.origenCodigoICAO}-${vuelo.destinoCodigoICAO}-${vuelo.fechaInicial || index}`;
			const uniqueId = `WS-${vueloKey}`;

			// 🆕 MAPEAR PEDIDOS: Normalizar estructura de pedidos del backend
			const pedidosMapeados = (vuelo.pedidos || []).map(p => ({
				idPedido: p.idPedido || p.orderId || p.id,
				cantidad: p.cantidad || p.quantity || 1,
				destino: p.destino || vuelo.destinoCodigoICAO,
				origen: p.origen || vuelo.origenCodigoICAO
			}));

			// Crear objeto de vuelo (con timestamps para interpolación híbrida)
			const nuevoVuelo = {
				id: uniqueId,
				origin: {
					code: vuelo.origenCodigoICAO,
					lat: origen.lat,
					lng: origen.lng,
					region: origen.region
				},
				destination: {
					code: vuelo.destinoCodigoICAO,
					lat: destino.lat,
					lng: destino.lng,
					region: destino.region
				},
				progress,
				altitude: 35000, // Siempre en vuelo
				speed: 850, // Siempre con velocidad
				status: 'active', // Siempre activo para visualización
				currentLat,
				currentLng,
				aircraftColor: '#3b82f6', // 🔵 Azul para vuelos del WebSocket
				rotation,
				packageCapacity: vuelo.totalPaquetes || 1,
				currentPackages: vuelo.totalPaquetes || 1,
				packageType: 'WS',
				isSameContinentFlight: origen.region === destino.region,
				vuelo: `WS-${vuelo.pedidos?.[0]?.idPedido || index}`,
				pedidoId: vuelo.pedidos?.[0]?.idPedido,
				pedidos: pedidosMapeados, // 🆕 INCLUIR TODOS LOS PEDIDOS
				fechaInicial: vuelo.fechaInicial,
				fechaFinal: vuelo.fechaFinal
			};

			console.log(`   ✅ Vuelo creado en posición: [${currentLat.toFixed(2)}, ${currentLng.toFixed(2)}]`);
			if (nuevoVuelo.fechaInicial && nuevoVuelo.fechaFinal) {
				console.log(`   🎬 Sistema híbrido ACTIVADO para este vuelo`);
			}
			nuevosVuelos.push(nuevoVuelo);
		});

		console.log(`\n📊 ============================================`);
		console.log(`📊 Total de vuelos WebSocket creados: ${nuevosVuelos.length}`);
		const vuelosConInterpolacion = nuevosVuelos.filter(v => v.fechaInicial && v.fechaFinal).length;
		console.log(`🎬 Vuelos con interpolación temporal: ${vuelosConInterpolacion}/${nuevosVuelos.length}`);
		console.log(`📊 ============================================\n`);

		if (nuevosVuelos.length > 0) {
			console.log(`🗺️ Primer vuelo (ejemplo):`, {
				id: nuevosVuelos[0].id,
				from: nuevosVuelos[0].origin.code,
				to: nuevosVuelos[0].destination.code,
				position: [nuevosVuelos[0].currentLat, nuevosVuelos[0].currentLng],
				color: nuevosVuelos[0].aircraftColor,
				packages: nuevosVuelos[0].packageCapacity
			});

			// 🔥 ELIMINAR DUPLICADOS usando Map
			const flightsMap = new Map(nuevosVuelos.map(v => [v.id, v]));
			const vuelosUnicos = Array.from(flightsMap.values());

			if (vuelosUnicos.length < nuevosVuelos.length) {
				console.warn(`⚠️ Se encontraron ${nuevosVuelos.length - vuelosUnicos.length} vuelos duplicados, eliminados`);
			}

			// 📊 ACTUALIZAR MÉTRICAS - Contar vuelos nuevos recibidos del backend
			const tiempoActualParaMetricas = relojLocalRef.current ? relojLocalRef.current.getTime() : Date.now();
			let perdidosAntes = 0;
			let perdidosDespues = 0;

			vuelosUnicos.forEach(vuelo => {
				// Solo contar si es un ID nuevo (no duplicado de entregas anteriores)
				if (!vuelosRecibidosIdsRef.current.has(vuelo.id)) {
					vuelosRecibidosIdsRef.current.add(vuelo.id);

					// Verificar si el vuelo llegó "tarde" (ya terminó según el tiempo simulado)
					if (vuelo.fechaFinal) {
						const fechaFin = new Date(vuelo.fechaFinal).getTime();
						if (fechaFin < tiempoActualParaMetricas) {
							perdidosAntes++;
						}
					}
					// Verificar si el vuelo llegó "muy tarde" (ni siquiera empezó)
					if (vuelo.fechaInicial) {
						const fechaInicio = new Date(vuelo.fechaInicial).getTime();
						if (fechaInicio > tiempoActualParaMetricas + (60 * 60 * 1000)) { // 1 hora en el futuro, posible error
							perdidosDespues++;
						}
					}
				}
			});

			// Actualizar métricas
			setMetricasVuelos(prev => ({
				...prev,
				totalRecibidosBackend: vuelosRecibidosIdsRef.current.size,
				totalPerdidosAntesDeTiempo: prev.totalPerdidosAntesDeTiempo + perdidosAntes,
				totalPerdidosDespuesDeTiempo: prev.totalPerdidosDespuesDeTiempo + perdidosDespues,
				ultimaActualizacion: new Date().toISOString()
			}));

			if (perdidosAntes > 0 || perdidosDespues > 0) {
				console.warn(`📊 MÉTRICAS: ${perdidosAntes} vuelos llegaron tarde (ya aterrizaron), ${perdidosDespues} vuelos muy adelantados`);
			}

			// 🆕 LOG: Mostrar rango de fechas de los vuelos
			const vuelosConFecha = vuelosUnicos.filter(v => v.fechaInicial);
			if (vuelosConFecha.length > 0) {
				const fechaMasTemprana = Math.min(...vuelosConFecha.map(v => new Date(v.fechaInicial).getTime()));
				const fechaMasTardia = Math.max(...vuelosConFecha.map(v => new Date(v.fechaFinal).getTime()));
				console.log(`�� Rango de vuelos: ${new Date(fechaMasTemprana).toISOString()} → ${new Date(fechaMasTardia).toISOString()}`);
				console.log(`⏰ Tiempo simulado actual: ${new Date(relojLocalRef.current).toISOString()}`);
			}

			// 🆕 SISTEMA DE BUFFER DE 15 SEGUNDOS
			// Si el buffer está activo, acumular vuelos en lugar de activar animación
			if (bufferActivo) {
				console.log(`⏳ BUFFER ACTIVO - Acumulando ${vuelosUnicos.length} vuelos (total: ${vuelosBufferRef.current.length + vuelosUnicos.length})`);

				// Agregar vuelos únicos al buffer (evitar duplicados)
				const idsExistentes = new Set(vuelosBufferRef.current.map(v => v.id));
				const nuevosParaBuffer = vuelosUnicos.filter(v => !idsExistentes.has(v.id));
				vuelosBufferRef.current = [...vuelosBufferRef.current, ...nuevosParaBuffer];

				console.log(`📦 Buffer ahora tiene ${vuelosBufferRef.current.length} vuelos`);
				// NO activar reloj durante el buffer - se activa cuando termina el timeout
				return;
			}

			// 🔥 COMBINAR con vuelos existentes (sin duplicados) - Solo si buffer NO está activo
			console.log(`🔄 Combinando ${vuelosUnicos.length} vuelos nuevos con existentes`);

			setFlights(prevFlights => {
				// 🆕 LIMPIAR vuelos que ya aterrizaron (fechaFinal < tiempoSimulado - 30min margen)
				const relojActual = relojLocalRef.current;
				const tiempoActualMs = relojActual instanceof Date ? relojActual.getTime() : (relojActual || Date.now());
				const margenMs = 30 * 60 * 1000; // 30 minutos de margen después de aterrizar
				const vuelosActivos = prevFlights.filter(v => {
					if (!v.fechaFinal) return true; // Mantener si no tiene fecha
					const fechaAterrizaje = new Date(v.fechaFinal).getTime();
					return fechaAterrizaje > (tiempoActualMs - margenMs);
				});

				if (vuelosActivos.length < prevFlights.length) {
					console.log(`🧹 Limpiados ${prevFlights.length - vuelosActivos.length} vuelos que ya aterrizaron`);
				}

				const existingIds = new Set(vuelosActivos.map(v => v.id));
				const nuevosNoRepetidos = vuelosUnicos.filter(v => !existingIds.has(v.id));
				const combinados = [...vuelosActivos, ...nuevosNoRepetidos];
				console.log(`📊 Total vuelos después de combinar: ${combinados.length} (activos: ${vuelosActivos.length}, nuevos: ${nuevosNoRepetidos.length})`);
				return combinados;
			});

			// 🚀 ACTIVAR RELOJ LOCAL (solo si no está ya activo)
			if (!simulacionLocalActiva) {
				setSimulacionLocalActiva(true);
				console.log(`🚀 Reloj local ACTIVADO - ${vuelosUnicos.length} vuelos listos para animar`);
			}

			// ✅ Verificar que el estado se actualizó
			setTimeout(() => {
				console.log(`✅ Verificación: nuevos vuelos procesados = ${vuelosUnicos.length}`);
			}, 100);

			const vuelosActivos = vuelosUnicos.filter(v => v.status === 'active').length;
			setFlightsInAir(prev => prev + vuelosActivos);
			console.log(`Vuelos activos anadidos: ${vuelosActivos}`);
		} else {
			console.error(`NO SE CREARON VUELOS!`);
			console.error(`   Vuelos recibidos:`, vuelos);
			console.error(`   Aeropuertos disponibles:`, currentAirports.length);
		}
	}, []);

	procesarVuelosDirectosRef.current = procesarVuelosDirectos;

	/**
	 * Procesar rutas del snapshot de simulación
	 */
	const procesarRutasSnapshot = useCallback((routes) => {
		// Similar a procesarRutasSimulacion pero para formato de snapshot
		// Implementar según el formato específico de tu backend
		console.log(`📊 Procesando ${routes.length} rutas del snapshot`);
	}, []); // 🔥 Sin dependencias
	procesarRutasSnapshotRef.current = procesarRutasSnapshot;

	/**
	 * Cancelar simulación en curso
	 */
	const cancelarSimulacionStomp = useCallback(async () => {
		if (!sessionId) {
			alert('No hay simulación activa para cancelar');
			return;
		}

		try {
			console.log(`🛑 Cancelando simulación ${sessionId}...`);

			const response = await fetch(`${API_BASE_URL}/api/simulations/${sessionId}/cancel`, {
				method: 'POST'
			});

			if (response.ok) {
				agregarMensaje('🛑 Simulación cancelada', 'warning');
				setEstadoSimulacionStomp('cancelled');

				// Desuscribirse
				if (subscriptionRef.current) {
					subscriptionRef.current.unsubscribe();
					subscriptionRef.current = null;
				}
			} else {
				throw new Error('Error al cancelar simulación');
			}
		} catch (error) {
			console.error('❌ Error cancelando simulación:', error);
			agregarMensaje(`❌ Error: ${error.message}`, 'error');
		}
	}, [sessionId, agregarMensaje]);

	/**
	 * Limpiar todo (mensajes, vuelos, estado)
	 */
	const limpiarTodoStomp = useCallback(() => {
		setMensajesSimulacion([]);
		setProgresoAG(null);
		setFlights([]);
		setFlightsInAir(0);
		setSessionId(null);
		setEstadoSimulacionStomp('connected');
		console.log('🧹 Todo limpiado');
	}, []);

	// Auto-conectar WebSocket STOMP al montar (OPCIONAL)
	useEffect(() => {
		// Comentar si no quieres auto-conexión
		// conectarWebSocketStomp();

		return () => {
			desconectarWebSocketStomp();
		};
	}, [conectarWebSocketStomp, desconectarWebSocketStomp]);

	/* Calcular métricas de saturación de aeropuertos */
	const getSaturation = () => {
		const regularAirports = airports.filter(airport => !airport.isSede);
		if (regularAirports.length === 0) return "0.00";
		const totalCapacity = regularAirports.reduce((sum, airport) => sum + (typeof airport.capacity === 'number' ? airport.capacity : 0), 0);
		const totalPackages = regularAirports.reduce((sum, airport) => sum + airport.packages, 0);
		if (totalCapacity === 0) return "0.00";
		return ((totalPackages / totalCapacity) * 100).toFixed(2);
	};
	/* Encontrar aeropuerto más saturado */
	const getMostSaturatedAirport = () => {
		const regularAirports = airports.filter(airport => !airport.isSede && typeof airport.capacity === 'number');
		if (regularAirports.length === 0) return { name: 'N/A', capacity: 0, packages: 0, saturation: 0 };
		return regularAirports.reduce((max, airport) => {
			const saturation = (airport.packages / airport.capacity) * 100;
			const maxSaturation = (max.packages / max.capacity) * 100;
			return saturation > maxSaturation ? airport : max;
		});
	};
	/* Obtener aeropuerto más saturado */
	const mostSaturatedAirport = getMostSaturatedAirport();
	const getFlightsByAltitude = () => flightsInAir;

	/* 🆕 Filtrar vuelos en movimiento (progress > 0 y progress < 1) para métricas */
	const flightsInMovement = useMemo(() => {
		return flights.filter(f => f.progress && f.progress > 0 && f.progress < 1);
	}, [flights]);

	/* 🆕 Estado para actualizar métricas en tiempo real */
	const [flightsInAirCount, setFlightsInAirCount] = useState(0);

	/* 🆕 Efecto: Actualizar contador de vuelos en el aire con la MISMA LÓGICA que el sidebar */
	useEffect(() => {
		const enAire = (vuelosEnMovimiento || []).filter(f => (f.status === 'active' || (f.progress && f.progress > 0 && f.progress < 1))).length;
		setFlightsInAirCount(enAire);
		console.log(`📊 Vuelos en el aire (sidebar logic): ${enAire}`);
	}, [vuelosEnMovimiento]);

	/* Efecto: Actualizar la cantidad de pedidos */
	const [ordersCount, setOrdersCount] = useState(0);

	useEffect(() => {
		const list = [];

		(flights || []).forEach(f => {
			if (Array.isArray(f.pedidos)) {
				f.pedidos.forEach(p => list.push({ ...p, flightId: f.id }));
			} else if (f.pedidoId) {
				list.push({ idPedido: f.pedidoId, flightId: f.id });
			}
		});

		// Actualizar el estado global de pedidos
		setOrdersCount(list.length);
		console.log(`📦 Total pedidos en simulación: ${list.length}`);
	}, [flights]);

	/* Estado y lógica para el drawer lateral */
	const drawerWidth = 300; // ancho del drawer
	const [open, setOpen] = useState(false);

	// Estados para el sidebar: pestañas y buscadores
	const [sidebarTab, setSidebarTab] = useState('flights'); // 'flights' | 'airports' | 'orders'
	const [searchFlights, setSearchFlights] = useState('');
	const [searchAirports, setSearchAirports] = useState('');
	const [searchOrders, setSearchOrders] = useState('');
	const [expandedFlightIds, setExpandedFlightIds] = useState({});
	const [selectedAirport, setSelectedAirport] = useState(null); // Aeropuerto seleccionado para ver detalles
	const [selectedFlight, setSelectedFlight] = useState(null); // Vuelo seleccionado para ver detalles
	const tabsRef = useRef(null); // Referencia para scroll de tabs

	/* Accion de boton de Regresar */
	const goBack = () => {
		window.history.back(); // retrocede una página
	};

	/* Mostrar indicador de carga mientras se obtienen los aeropuertos */
	if (loadingAirports) {
		return (
			<div className="section-content" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
				<div style={{ textAlign: 'center', color: '#fff' }}>
					<i className="fas fa-spinner fa-spin" style={{ fontSize: '48px', marginBottom: '20px' }}></i>
					<p style={{ fontSize: '18px' }}>Cargando aeropuertos...</p>
				</div>
			</div>
		);
	}

	/* ==================== Texto dinamico para los botones ==================== */
	let startButtonLabel = "Iniciar";

	if (simulacionActiva) {
		startButtonLabel = "Iniciar";
	} else if (simClock) {
		// hubo simulación antes y ahora está pausada
		startButtonLabel = "Reanudar";
	}

	return (
		<div className="section-content" id="simulationSection">
			{/* Botón semicircular pegado al borde */}
			<IconButton
				onClick={() => setOpen(v => !v)}
				aria-label="toggle drawer"
				sx={{
					position: 'fixed',
					top: '50%',
					transform: 'translateY(-50%)',
					left: open ? drawerWidth - 15 : 0,
					zIndex: 1201,
					width: 36,
					height: 72,
					borderRadius: open ? '36px 0 0 36px' : '0 36px 36px 0',
					backgroundColor: '#2c4a6b',
					color: '#fff',
					boxShadow: '0 6px 18px rgba(0,0,0,0.25)',
					transition: 'left .35s ease, border-radius .35s ease, background-color .2s ease',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					'&:hover': { backgroundColor: '#496c92ff' },
				}}
			>
				{open ? <ChevronLeftIcon /> : <ChevronRightIcon />}
			</IconButton>
			{/* Drawer lateral */}
			<Drawer
				variant="persistent"
				anchor="left"
				open={open}
				sx={{
					width: open ? drawerWidth : 0,
					flexShrink: 0,
					'& .MuiDrawer-paper': {
						width: drawerWidth,
						boxSizing: 'border-box',
						padding: 0,
						position: 'relative',
						overflow: 'visible',
					},
				}}
			>
				{/* Barra lateral con información y controles */}
				<div className="simulation-sidebar">
					<div className="modes-sidebar-container"
						style={{
							display: 'flex',
							justifyContent: 'center',
							alignItems: 'flex-start',
							width: '100%',
							paddingTop: '2px',
							paddingBottom: '10px',
						}}>
					</div>
					<div className="sidebar-header">
						<h2>Simulación Semanal</h2>
					</div>
					<div className="sidebar-content" style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden', gap: '4px' }}>
						{/* Material-UI Tabs */}
						<Box sx={{ borderBottom: 1, borderColor: '#dee2e6', marginBottom: 1 }}>
							<Tabs
								value={sidebarTab === 'flights' ? 0 : sidebarTab === 'airports' ? 1 : 2}
								onChange={(e, newValue) => setSidebarTab(['flights', 'airports', 'orders'][newValue])}
								sx={{
									'& .MuiTabs-indicator': { background: '#2c4a6b', height: 3 },
									'& .MuiTab-root': {
										color: '#6c757d',
										fontWeight: 500,
										fontSize: '0.9rem',
										textTransform: 'none',
										minHeight: 44,
										padding: '8px 16px',
										'&.Mui-selected': { color: '#2c4a6b', fontWeight: 600 }
									}
								}}
							>
								<Tab label=" Vuelos" />
								<Tab label=" Aeropuertos" />
								<Tab label={` Pedidos ${contadorPedidosTotal > 0 ? `(${contadorPedidosTotal})` : ''}`} />
							</Tabs>
						</Box>

						{/* Buscador */}
						<Box sx={{ padding: '8px 12px', marginBottom: '8px' }}>
							<input
								placeholder={
									sidebarTab === 'flights' ? 'Buscar vuelo, origen o destino...' :
										sidebarTab === 'airports' ? 'Buscar aeropuerto...' :
											'Buscar pedido...'
								}
								value={sidebarTab === 'flights' ? searchFlights : sidebarTab === 'airports' ? searchAirports : searchOrders}
								onChange={(e) => {
									if (sidebarTab === 'flights') setSearchFlights(e.target.value);
									else if (sidebarTab === 'airports') setSearchAirports(e.target.value);
									else setSearchOrders(e.target.value);
								}}
								style={{
									width: '100%',
									padding: '10px 12px',
									borderRadius: '8px',
									border: '1px solid #dee2e6',
									fontFamily: 'inherit',
									fontSize: '0.9rem',
									boxSizing: 'border-box'
								}}
							/>
						</Box>

						{/* Contenido de cada pestaña - con espacio para detalles */}
						{/* Contenido de cada pestaña - con espacio para detalles */}
						<Box sx={{
							display: 'flex',
							flex: 1,
							flexDirection: 'column',
							overflowY: 'hidden', // Cambiar de 'auto' a 'hidden'
							paddingX: '12px',
							paddingY: '8px',
							minHeight: 0 // Importante
						}}>
							{/* Listado principal - se ajusta cuando aparece el detalle */}
							<Box sx={{
								flex: selectedAirport || selectedFlight ? 0.6 : 1, // 60% si hay detalle, 100% si no
								overflowY: 'auto',
								marginBottom: '12px',
								minHeight: 0, // Importante para el scroll
								transition: 'flex 0.3s ease' // Transición suave
							}}>
								{sidebarTab === 'flights' && (
									<Box>
										{(vuelosEnMovimiento || []).filter(f => (f.status === 'active' || (f.progress && f.progress > 0 && f.progress < 1))).filter(f => {
											const q = searchFlights.trim().toLowerCase();
											if (!q) return true;
											// 🆕 Buscar también por pedidoId
											const matchId = String(f.id || '').toLowerCase().includes(q);
											const matchOrigin = String(f.origin?.code || '').toLowerCase().includes(q);
											const matchDest = String(f.destination?.code || '').toLowerCase().includes(q);
											const matchPedido = String(f.pedidoId || '').toLowerCase().includes(q);
											const matchPedidos = (f.pedidos || []).some(p =>
												String(p.idPedido || p.id || '').toLowerCase().includes(q)
											);
											return matchId || matchOrigin || matchDest || matchPedido || matchPedidos;
										}).map(flight => {
											// 🆕 Calcular cantidad de pedidos
											const cantidadPedidos = (flight.pedidos?.length || 0) || (flight.pedidoId ? 1 : 0);
											return (
												<Box key={flight.id} onClick={() => setSelectedFlight(flight)} sx={{ border: '1px solid #dee2e6', padding: '10px', borderRadius: '8px', marginBottom: '10px', background: selectedFlight?.id === flight.id ? '#e8f4f8' : '#f8f9fa', cursor: 'pointer', transition: 'all 0.2s ease', '&:hover': { borderColor: '#2c4a6b', boxShadow: '0 2px 8px rgba(44, 74, 107, 0.15)' } }}>
													<Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
														<Box sx={{ flex: 1 }}>
															<Box sx={{ fontWeight: 700, fontSize: '0.95rem', color: '#2c4a6b' }}>{flight.id}</Box>
															<Box sx={{ fontSize: '0.85rem', color: '#6c757d', marginTop: '2px' }}>{flight.origin?.code || 'N/A'} → {flight.destination?.code || 'N/A'}</Box>
														</Box>
														{/* 🆕 Badge de cantidad de pedidos */}
														{cantidadPedidos > 0 && (
															<Box sx={{
																background: '#2c4a6b',
																color: '#fff',
																padding: '2px 8px',
																borderRadius: '12px',
																fontSize: '0.75rem',
																fontWeight: 600,
																whiteSpace: 'nowrap'
															}}>
																📦 {cantidadPedidos}
															</Box>
														)}
													</Box>
													{expandedFlightIds[flight.id] && (
														<Box sx={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px solid #dee2e6' }}>
															{flight.pedidos && flight.pedidos.length > 0 ? (
																flight.pedidos.map(p => (
																	<Box key={p.idPedido || p.id} sx={{ padding: '6px 8px', borderRadius: '4px', border: '1px dashed #dee2e6', marginBottom: '6px', background: '#fff' }}>
																		<Box sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#2c4a6b' }}>{p.idPedido || p.id}</Box>
																		<Box sx={{ fontSize: '0.8rem', color: '#6c757d', marginTop: '2px' }}>{p.descripcion || p.info || `Cantidad: ${p.cantidad || 1}`}</Box>
																	</Box>
																))
															) : flight.pedidoId ? (
																<Box sx={{ padding: '6px 8px', borderRadius: '4px', border: '1px dashed #dee2e6', background: '#fff', fontSize: '0.85rem', fontWeight: 600, color: '#2c4a6b' }}>📦 {flight.pedidoId}</Box>
															) : (
																<Box sx={{ fontSize: '0.85rem', color: '#6c757d' }}>Sin pedidos en este vuelo</Box>
															)}
														</Box>
													)}
												</Box>
											)
										})}
										{(vuelosEnMovimiento || []).filter(f => (f.status === 'active' || (f.progress && f.progress > 0 && f.progress < 1))).length === 0 && (
											<Box sx={{ color: '#6c757d', fontSize: '0.9rem', textAlign: 'center', padding: '20px 10px' }}>No hay vuelos en vuelo</Box>
										)}
									</Box>
								)}								{sidebarTab === 'airports' && (
									<Box>
										{(airports || []).filter(a => {
											const q = searchAirports.trim().toLowerCase();
											if (!q) return true;
											return (String(a.name || '').toLowerCase().includes(q) || String(a.code || '').toLowerCase().includes(q));
										}).map(airport => (
											<Box key={airport.code || airport.name} onClick={() => setSelectedAirport(airport)} sx={{ border: '1px solid #dee2e6', padding: '10px', borderRadius: '8px', marginBottom: '10px', background: selectedAirport?.code === airport.code ? '#e8f4f8' : (airport.isSede ? '#fff3cd' : '#f8f9fa'), cursor: 'pointer', transition: 'all 0.2s ease', '&:hover': { borderColor: '#2c4a6b', boxShadow: '0 2px 8px rgba(44, 74, 107, 0.15)' } }}>
												<Box sx={{ fontWeight: 700, fontSize: '0.95rem', color: airport.isSede ? '#FF6B35' : '#2c4a6b' }}>
													{airport.isSede && '🏢'} {airport.name} <span style={{ fontSize: '0.85rem', color: '#6c757d', fontWeight: 400 }}>({airport.code})</span>
												</Box>
												<Box sx={{ fontSize: '0.85rem', color: '#6c757d', marginTop: '4px' }}>{airport.operationType || airport.region || ''}</Box>
												<Box sx={{ marginTop: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem' }}>
													<Box sx={{ fontWeight: 600, color: '#2c4a6b' }}>{airport.packages || 0} 📦</Box>
													<Box sx={{ color: '#6c757d' }}>{typeof airport.capacity === 'number' ? `${airport.capacity}` : airport.capacity}</Box>
												</Box>
												{airport.pedidos && airport.pedidos.length > 0 && (
													<Box sx={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px solid #dee2e6' }}>
														<Box sx={{ fontSize: '0.8rem', fontWeight: 700, color: '#2c4a6b', marginBottom: '6px' }}>Pedidos en aeropuerto:</Box>
														{airport.pedidos.map(p => (
															<Box key={p.idPedido || p.id} sx={{ padding: '4px 6px', borderRadius: '4px', border: '1px dashed #dee2e6', marginBottom: '4px', fontSize: '0.8rem', background: '#fff' }}>
																{p.idPedido || p.id}
															</Box>
														))}
													</Box>
												)}
											</Box>
										))}
										{(airports || []).length === 0 && (
											<Box sx={{ color: '#6c757d', fontSize: '0.9rem', textAlign: 'center', padding: '20px 10px' }}>No hay aeropuertos cargados</Box>
										)}
									</Box>
								)}

								{sidebarTab === 'orders' && (
									<Box>
										{(() => {
											const list = [];
											// 🆕 Extraer pedidos de flights (vuelos activos)
											(flights || []).forEach(f => {
												if (f.pedidos && Array.isArray(f.pedidos)) {
													f.pedidos.forEach(p => list.push({
														...(p),
														flightId: f.id,
														flightData: f, // 🆕 Referencia al vuelo completo
														origin: p.origen || f.origin?.code,
														destination: p.destino || f.destination?.code,
														cantidad: p.cantidad || 1,
														status: f.status
													}));
												} else if (f.pedidoId) {
													list.push({
														idPedido: f.pedidoId,
														flightId: f.id,
														flightData: f, // 🆕 Referencia al vuelo completo
														origin: f.origin?.code,
														destination: f.destination?.code,
														cantidad: f.currentPackages || 1,
														status: f.status
													});
												}
											});

											const totalOrders = list.length;

											const q = searchOrders.trim().toLowerCase();
											return list.filter(o => {
												if (!q) return true;
												return String(o.idPedido || o.id || o.flightId || '').toLowerCase().includes(q) ||
													String(o.origin || '').toLowerCase().includes(q) ||
													String(o.destination || '').toLowerCase().includes(q) ||
													String(o.cliente || '').toLowerCase().includes(q);
											});
										})().map(order => (
											<Box
												key={order.idPedido || order.id || `${order.flightId}-${order.origin}-${order.destination}-${Math.random()}`}
												onClick={() => order.flightData && setSelectedFlight(order.flightData)}
												sx={{
													border: '1px solid #dee2e6',
													padding: '10px',
													borderRadius: '8px',
													marginBottom: '10px',
													background: order.status === 'active' ? '#e8f8e8' : '#f8f9fa',
													cursor: 'pointer',
													transition: 'all 0.2s ease',
													'&:hover': { borderColor: '#2c4a6b', boxShadow: '0 2px 8px rgba(44, 74, 107, 0.15)' }
												}}>
												<Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
													<Box sx={{ fontWeight: 700, fontSize: '0.95rem', color: '#2c4a6b' }}>📦 {order.idPedido || order.id}</Box>
													{order.cantidad && (
														<Box sx={{
															background: '#e3f2fd',
															color: '#1976d2',
															padding: '2px 8px',
															borderRadius: '12px',
															fontSize: '0.75rem',
															fontWeight: 600
														}}>
															{order.cantidad} uds
														</Box>
													)}
												</Box>
												<Box sx={{ fontSize: '0.85rem', color: '#6c757d', marginTop: '4px' }}>
													{order.origin || '?'} → {order.destination || '?'}
												</Box>
												<Box sx={{ marginTop: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
													<Box sx={{ fontSize: '0.8rem', color: '#495057', fontWeight: 500 }}>
														✈️ {order.flightId || 'N/A'}
													</Box>
													{order.status === 'active' && (
														<Box sx={{
															background: '#28a745',
															color: '#fff',
															padding: '1px 6px',
															borderRadius: '8px',
															fontSize: '0.7rem',
															fontWeight: 600
														}}>
															En vuelo
														</Box>
													)}
												</Box>
												{order.cliente && (
													<Box sx={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '4px' }}>
														Cliente: {order.cliente}
													</Box>
												)}
											</Box>
										))}
										{(() => {
											const anyOrders = (flights || []).some(f => (f.pedidos && f.pedidos.length) || f.pedidoId);
											if (!anyOrders) return <Box sx={{ color: '#6c757d', fontSize: '0.9rem', textAlign: 'center', padding: '20px 10px' }}>Sin pedidos disponibles</Box>;
											return null;
										})()}
									</Box>
								)}
							</Box>

							{/* Panel lateral de detalles del aeropuerto seleccionado */}
							{selectedAirport && (
								<Box sx={{
									borderTop: '2px solid #dee2e6',
									paddingY: '12px',
									flex: 0.4, // Ocupa 40% del espacio
									display: 'flex',
									flexDirection: 'column',
									overflow: 'hidden',
									minHeight: 0
								}}>
									<Box sx={{
										display: 'flex',
										justifyContent: 'space-between',
										alignItems: 'center',
										paddingX: '12px',
										marginBottom: '12px',
										flexShrink: 0
									}}>
										<Box sx={{ fontWeight: 700, fontSize: '0.95rem', color: '#2c4a6b' }}>{selectedAirport.name}</Box>
										<button onClick={() => setSelectedAirport(null)} style={{ padding: '2px 6px', background: '#e8e8e8', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: 700 }}>×</button>
									</Box>
									<Box sx={{
										paddingX: '12px',
										overflowY: 'auto',
										flex: 1,
										minHeight: 0
									}}>
										{selectedAirport.pedidos && selectedAirport.pedidos.length > 0 ? (
											selectedAirport.pedidos.map(p => (
												<Box key={p.idPedido || p.id} sx={{ padding: '6px 8px', borderRadius: '4px', border: '1px solid #dee2e6', marginBottom: '6px', fontSize: '0.8rem', background: '#fff', color: '#495057' }}>
													📦 {p.idPedido || p.id}
												</Box>
											))
										) : (
											<Box sx={{ fontSize: '0.8rem', color: '#6c757d', fontStyle: 'italic' }}>Sin pedidos</Box>
										)}
									</Box>
								</Box>
							)}

							{/* Panel lateral de detalles del vuelo seleccionado */}
							{selectedFlight && (
								<Box sx={{
									borderTop: '2px solid #dee2e6',
									paddingY: '12px',
									flex: 0.4, // Ocupa 40% del espacio
									display: 'flex',
									flexDirection: 'column',
									overflow: 'hidden',
									minHeight: 0
								}}>
									<Box sx={{
										display: 'flex',
										justifyContent: 'space-between',
										alignItems: 'center',
										paddingX: '12px',
										marginBottom: '8px',
										flexShrink: 0
									}}>
										<Box sx={{ fontWeight: 700, fontSize: '0.95rem', color: '#2c4a6b' }}>
											Vuelo {selectedFlight.id} • {selectedFlight.origin?.code || 'N/A'} → {selectedFlight.destination?.code || 'N/A'}
										</Box>
										<button
											onClick={() => setSelectedFlight(null)}
											style={{
												padding: '2px 6px',
												background: '#e8e8e8',
												border: 'none',
												borderRadius: '4px',
												cursor: 'pointer',
												fontSize: '12px',
												fontWeight: 700
											}}>
											×
										</button>
									</Box>
									<Box sx={{
										paddingX: '12px',
										overflowY: 'auto',
										flex: 1,
										minHeight: 0
									}}>
										{selectedFlight.pedidos && selectedFlight.pedidos.length > 0 ? (
											selectedFlight.pedidos.map(p => (
												<Box
													key={p.idPedido || p.id}
													sx={{
														padding: '8px 10px',
														borderRadius: '6px',
														border: '1px solid #dee2e6',
														marginBottom: '8px',
														fontSize: '0.85rem',
														background: '#fff',
														color: '#495057'
													}}>
													<Box sx={{ fontWeight: 600, color: '#2c4a6b', marginBottom: '4px' }}>
														📦 {p.idPedido || p.id}
													</Box>
													<Box sx={{ fontSize: '0.8rem', color: '#6c757d' }}>
														{p.cantidad && <span>Cantidad: {p.cantidad} • </span>}
														{p.origen && p.destino && <span>{p.origen} → {p.destino}</span>}
													</Box>
													{p.cliente && (
														<Box sx={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '2px' }}>
															Cliente: {p.cliente}
														</Box>
													)}
												</Box>
											))
										) : selectedFlight.pedidoId ? (
											<Box
												sx={{
													padding: '8px 10px',
													borderRadius: '6px',
													border: '1px solid #dee2e6',
													marginBottom: '8px',
													fontSize: '0.85rem',
													background: '#fff',
													color: '#495057'
												}}>
												<Box sx={{ fontWeight: 600, color: '#2c4a6b' }}>
													📦 {selectedFlight.pedidoId}
												</Box>
												<Box sx={{ fontSize: '0.8rem', color: '#6c757d', marginTop: '4px' }}>
													{selectedFlight.origin?.code} → {selectedFlight.destination?.code}
												</Box>
												{selectedFlight.currentPackages && (
													<Box sx={{ fontSize: '0.75rem', color: '#9ca3af', marginTop: '2px' }}>
														Paquetes: {selectedFlight.currentPackages}
													</Box>
												)}
											</Box>
										) : (
											<Box sx={{ fontSize: '0.8rem', color: '#6c757d', fontStyle: 'italic' }}>
												Sin pedidos
											</Box>
										)}
									</Box>
								</Box>
							)}
						</Box>
					</div>
				</div>
			</Drawer>

			{/* Contenedor principal que se ajusta al ancho del drawer */}
			<div
				style={{
					flexGrow: 1,
					minWidth: 0,
					transition: 'margin 0.2s ease', // transición suave al abrir/cerrar el drawer
					marginLeft: 0,
					paddingLeft: 0,
					boxSizing: 'border-box',
				}}
			>
				{/* Contenido principal con mapa*/}
				<div className="simulation-main-content">
					<div className="content-wrapper">
						{/* Mapa interactivo */}
						<div className="map-container">
							<MapContainer center={[13.0, 10.0]} zoom={3} className="flight-map" scrollWheelZoom={true} minZoom={2} maxZoom={10} zoomControl={true} zoomSnap={0.5} zoomDelta={0.5} doubleClickZoom={false} boxZoom={true} keyboard={true} touchZoom={true} worldCopyJump={false} maxBoundsViscosity={0.8} maxBounds={[[-90, -180], [90, 180]]}>
								<TileLayer url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png" attribution='&copy; OpenStreetMap contributors &copy; CARTO' noWrap={true} bounds={[[-90, -180], [90, 180]]}/>
								<DynamicMarkers
									flights={flights}
									airports={airports}
									activeView={activeView}
									showRoutes={showRoutes}
									vuelosEnMovimiento={vuelosEnMovimiento}
									showFlightLines={showFlightLines}
								/>
							</MapContainer>
							{/* Botón de Metricas */}
							<MetricsButton onClick={handleMetricsButtonClick} onMount={handleMetricButtonMount}/>
							{/* Botón de leyenda flotante */}
							<LegendButton onClick={handleToggleLegend} />
							{/* Controles de simulación */} 
							<ControlButton onClick={handleControlButtonClick} onMount={handleControlButtonMount}/>
						</div>
					</div>
				</div>
			</div>
			{/* Diálogo de Leyenda */}
			<LegendDialog
				anchorEl={legendAnchorEl}
				open={Boolean(legendAnchorEl)}
				onClose={handleToggleLegend}
			/>
			<MetricsPopper
				open={isMetricsPopperOpen}
				anchorEl={metricsAnchorEl}
				flightsInAirCount={flightsInAirCount}
				orderCount={ordersCount}
				flights={flightsInMovement}
				getSaturation={getSaturation}
			/>
			<ControlPopper
				open={isControlPopperOpen}
				anchorEl={controlAnchorEl}
				fechaInicioSimulacion={fechaInicioSimulacion}
				setFechaInicioSimulacion={setFechaInicioSimulacion}
				horaInicioSimulacion={horaInicioSimulacion}
				setHoraInicioSimulacion={setHoraInicioSimulacion}
				tiempoSimulacionActual={tiempoSimulacionActual}
				tiempoRealTranscurrido={tiempoRealTranscurrido}
				simulacionActiva={simulacionActiva}
				estadoPlanificacion={estadoPlanificacion}
				handleIniciarSimulacion={handleIniciarSimulacion}
				handleDetenerSimulacion={handleDetenerSimulacion}
				startButtonLabel={startButtonLabel}
				showFlightLines={showFlightLines}
				setShowFlightLines={setShowFlightLines}
			/>
		</div>
	);
};

export default SimuladorSemanal;
