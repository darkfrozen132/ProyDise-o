// MetricsPopper.jsx
import React, { useState, useEffect } from 'react';
import { Popper, Paper } from "@mui/material";
import { FaRoad } from "react-icons/fa";
import BackIconButton from '../../../components/ui/Button/BackIconButton';
import { FaStop } from "react-icons/fa6";
import { FaPlay } from "react-icons/fa6";

export default function ControlPopper({
    open,
    anchorEl,
    fechaInicioSimulacion,
    setFechaInicioSimulacion,
    horaInicioSimulacion,
    setHoraInicioSimulacion,
    tiempoSimulacionActual,
    tiempoRealTranscurrido,
    simulacionActiva,
    estadoPlanificacion,
    handleIniciarSimulacion,
    handleDetenerSimulacion,
    startButtonLabel,
    showFlightLines,
    setShowFlightLines, }) {

    const [now, setNow] = useState(new Date());
    
    /* ======= Calcular tiempo de simulación transcurrido ======= */
    const tiempoSimulacionTranscurrido = (() => {
        if (!tiempoSimulacionActual || !fechaInicioSimulacion) return 0;
        try {
            const tiempoActualMs = new Date(tiempoSimulacionActual).getTime();
            // Construir ISO string: YYYY-MM-DDTHH:mm:00.000Z
            const isoString = `${fechaInicioSimulacion}T${horaInicioSimulacion}:00.000Z`;
            const tiempoInicioMs = new Date(isoString).getTime();
            if (isNaN(tiempoActualMs) || isNaN(tiempoInicioMs)) {
                return 0;
            }
            
            // Calcular diferencia en segundos
            const diferencia = Math.floor((tiempoActualMs - tiempoInicioMs) / 1000);
            
            // Asegurar que el resultado sea positivo o cero
            return Math.max(0, diferencia);
        } catch (error) {
            console.error('Error calculando tiempo simulación transcurrido:', error);
            return 0;
        }
    })();
    /* ======= Actualizar hora real cada segundo ======= */
    useEffect(() => {
        // Solo actualizar la hora real si la simulación está activa
        if (!fechaInicioSimulacion || !simulacionActiva) {
            return;
        }
        
        const interval = setInterval(() => {
            setNow(new Date());
        }, 1000);

        return () => clearInterval(interval); // limpieza
    }, [fechaInicioSimulacion, simulacionActiva]);

    return (
        <Popper
            open={open}
            anchorEl={anchorEl}
            placement="left-start"
            modifiers={[
                {
                    name: "offset",
                    options: { offset: [0, 5] },
                },
            ]}
            sx={{ zIndex: 1300 }}
        >
            <Paper
                sx={{
                    borderRadius: 2,
                    width: { xs: '80vw', sm: '80vw', md: 550, lg: 750 },
                    maxWidth: '85vw',
                    padding: { xs: 0.8, sm: 1 },
                    boxShadow: 4,
                    backgroundColor: "rgba(255,255,255,0.8)",
                }}
            >
                <div style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '10px',
                    alignItems: 'center',
                    width: '100%'
                }}>
                    {/* Botón de retroceso */}
                    <div style={{ flexShrink: 0 }}>
                        <BackIconButton size={28} />
                    </div>

                    {/* Selector de fecha de inicio */}
                    <div style={{ 
                        display: 'flex', 
                        flexDirection: 'column',
                        gap: '4px',
                        minWidth: '140px'
                    }}>
                        <label style={{ 
                            fontSize: '12px', 
                            fontWeight: '500',
                            color: '#495057',
                            fontWeight: '600'
                        }}>
                            Fecha de Inicio:
                        </label>
                        <input
                            type="date"
                            id="fecha-inicio"
                            className="date-input"
                            value={fechaInicioSimulacion}
                            onChange={(e) => setFechaInicioSimulacion(e.target.value)}
                            style={{
                                padding: '6px 8px',
                                fontSize: '13px',
                                borderRadius: '4px',
                                border: '1px solid #ced4da'
                            }}
                        />
                    </div>

                    {/* Selector de hora de inicio */}
                    <div style={{ 
                        display: 'flex', 
                        flexDirection: 'column',
                        gap: '4px',
                        minWidth: '110px'
                    }}>
                        <label style={{ 
                            fontSize: '12px', 
                            fontWeight: '500',
                            color: '#495057',
                            fontWeight: '600'
                        }}>
                            Hora de Inicio:
                        </label>
                        <input
                            type="time"
                            id="hora-inicio"
                            className="date-input"
                            value={horaInicioSimulacion}
                            onChange={(e) => setHoraInicioSimulacion(e.target.value)}
                            style={{
                                padding: '6px 8px',
                                fontSize: '13px',
                                borderRadius: '4px',
                                border: '1px solid #ced4da',
                                width: '100%'
                            }}
                        />
                    </div>

                    {/* Panel de información de tiempo */}
                    <div style={{
                        background: '#f8f9fa',
                        borderRadius: '6px',
                        padding: '10px 12px',
                        border: '1px solid #dee2e6',
                        flex: '1 1 280px',
                        minWidth: '200px'
                    }}>
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '8px'
                        }}>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', alignItems: 'baseline' }}>
                                <span style={{ fontSize: '12px', fontWeight: '600', color: '#495057', whiteSpace: 'nowrap' }}>
                                    Simulación:
                                </span>
                                <span style={{ fontSize: '12px', fontWeight: '500', color: '#212529' }}>
                                    {tiempoSimulacionActual ?
                                        new Date(tiempoSimulacionActual).toLocaleString('es-ES', {
                                            timeZone: 'UTC',
                                            day: '2-digit',
                                            month: '2-digit',
                                            year: 'numeric',
                                            hour: '2-digit',
                                            minute: '2-digit',
                                            second: '2-digit',
                                            hour12: false
                                        })
                                        : fechaInicioSimulacion ?
                                            new Date(fechaInicioSimulacion).toLocaleString('es-ES', {
                                                timeZone: 'UTC',
                                                day: '2-digit',
                                                month: '2-digit',
                                                year: 'numeric',
                                                hour: '2-digit',
                                                minute: '2-digit',
                                                second: '2-digit',
                                                hour12: false
                                            })
                                            : '--:--:--'
                                    }
                                </span>
                                <span style={{ fontSize: '12px', color: '#6c757d', fontFamily: 'monospace' }}>
                                    {(() => {
                                        const total = tiempoSimulacionTranscurrido;

                                        const dias = Math.floor(total / 86400);
                                        const horasRestantes = Math.floor((total % 86400) / 3600);
                                        const minutos = Math.floor((total % 3600) / 60);
                                        const segundos = total % 60;

                                        return `(${String(dias).padStart(2, '0')}d ${String(horasRestantes).padStart(2, '0')}:${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')})`;
                                    })()}
                                </span>

                            </div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', alignItems: 'baseline' }}>
                                <span style={{ fontSize: '12px', fontWeight: '600', color: '#495057', whiteSpace: 'nowrap' }}>
                                    Tiempo real:
                                </span>
                                <span style={{ fontSize: '12px', fontWeight: '500', color: '#212529' }}>
                                    {!fechaInicioSimulacion
                                        ? '--:--:--'
                                        : now.toLocaleString('es-ES', {
                                            day: '2-digit',
                                            month: '2-digit',
                                            year: 'numeric',
                                            hour: '2-digit',
                                            minute: '2-digit',
                                            second: '2-digit',
                                            hour12: false
                                        })
                                    }
                                </span>
                                <span style={{ fontSize: '12px', color: '#6c757d', fontFamily: 'monospace' }}>
                                    {(() => {
                                        const horas = Math.floor(tiempoRealTranscurrido / 3600);
                                        const minutos = Math.floor((tiempoRealTranscurrido % 3600) / 60);
                                        const segundos = tiempoRealTranscurrido % 60;
                                        return `(${String(horas).padStart(2, '0')}:${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')})`;
                                    })()}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Estado y Botones de control */}
                    <div style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: '10px',
                        flexWrap: 'wrap',
                        flex: '1 1 auto',
                        justifyContent: 'center'
                    }}>
                        {/* Botones de control */}
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            {/* Botón de iniciar simulación */}
                            <button
                                onClick={handleIniciarSimulacion}
                                disabled={simulacionActiva}
                                style={{
                                    padding: '6px 12px',
                                    borderRadius: '4px',
                                    border: simulacionActiva ? '1px solid #e9ecef' : '1px solid #28a745',
                                    background: simulacionActiva ? '#e9ecef' : '#28a745',
                                    color: simulacionActiva ? '#6c757d' : 'white',
                                    fontSize: '13px',
                                    fontWeight: '500',
                                    cursor: simulacionActiva ? 'not-allowed' : 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    transition: 'all 0.2s',
                                    boxShadow: simulacionActiva ? 'none' : '0 2px 6px rgba(40, 167, 69, 0.3)',
                                    whiteSpace: 'nowrap'
                                }}
                            >
                                <FaPlay size={14} />
                                {startButtonLabel}
                            </button>
                            {/* Botón para detener la simulación */}
                            <button
                                onClick={handleDetenerSimulacion}
                                disabled={!simulacionActiva}
                                style={{
                                    padding: '6px 12px',
                                    borderRadius: '4px',
                                    border: simulacionActiva ? '1px solid #dc3545' : '1px solid #e9ecef',
                                    background: simulacionActiva ? '#dc3545' : '#e9ecef',
                                    color: simulacionActiva ? 'white' : '#6c757d',
                                    fontSize: '13px',
                                    fontWeight: '500',
                                    cursor: simulacionActiva ? 'pointer' : 'not-allowed',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    transition: 'all 0.2s',
                                    boxShadow: simulacionActiva ? '0 2px 6px rgba(220, 53, 69, 0.3)' : 'none',
                                    whiteSpace: 'nowrap'
                                }}
                            >
                                <FaStop size={14} />
                                Detener
                            </button>
                            {/* Botón para mostrar/ocultar líneas de rutas */}
                            <button
                                onClick={() => setShowFlightLines(!showFlightLines)}
                                style={{
                                    padding: '6px 12px',
                                    borderRadius: '4px',
                                    border: showFlightLines ? '1px solid #0d6efd' : '1px solid #e9ecef',
                                    background: showFlightLines ? '#0d6efd' : '#e9ecef',
                                    color: showFlightLines ? 'white' : '#6c757d',
                                    fontSize: '13px',
                                    fontWeight: '500',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    transition: 'all 0.2s',
                                    boxShadow: showFlightLines ? '0 2px 6px rgba(13, 110, 253, 0.3)' : 'none',
                                    whiteSpace: 'nowrap'
                                }}
                                title={showFlightLines ? 'Ocultar líneas de rutas' : 'Mostrar líneas de rutas'}
                            >
                                <FaRoad size={14} />
                                Rutas
                            </button>
                        </div>
                    </div>
                </div>
            </Paper>
        </Popper>
    );
}