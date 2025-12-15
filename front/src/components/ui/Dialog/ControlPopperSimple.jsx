// ControlPopperSimple.jsx - Panel simplificado con reloj UTC y botones de control
import React, { useState, useEffect } from 'react';
import { Popper, Paper } from "@mui/material";
import { FaRoad } from "react-icons/fa";
import { FaStop, FaPlay } from "react-icons/fa6";
import { FaClock } from "react-icons/fa";

export default function ControlPopperSimple({
    open,
    anchorEl,
    simulacionActiva,
    handleIniciarSimulacion,
    handleDetenerSimulacion,
    startButtonLabel,
    showFlightLines,
    setShowFlightLines,
}) {
    const [utcTime, setUtcTime] = useState(new Date());

    // Actualizar reloj UTC cada segundo
    useEffect(() => {
        const interval = setInterval(() => {
            setUtcTime(new Date());
        }, 1000);

        return () => clearInterval(interval);
    }, []);

    // Formatear hora UTC
    const formatUTC = (date) => {
        return date.toISOString().slice(11, 19); // HH:MM:SS
    };

    // Formatear fecha UTC
    const formatDateUTC = (date) => {
        const day = String(date.getUTCDate()).padStart(2, '0');
        const month = String(date.getUTCMonth() + 1).padStart(2, '0');
        const year = date.getUTCFullYear();
        return `${day}/${month}/${year}`;
    };

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
                    padding: { xs: 1, sm: 1.5 },
                    boxShadow: 4,
                    backgroundColor: "rgba(255,255,255,0.95)",
                }}
            >
                <div style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '12px',
                    alignItems: 'center',
                }}>
                    {/* Reloj UTC */}
                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        background: 'linear-gradient(135deg, #1a237e 0%, #283593 100%)',
                        borderRadius: '8px',
                        padding: '10px 16px',
                        boxShadow: '0 2px 8px rgba(26, 35, 126, 0.3)',
                    }}>
                        <FaClock size={18} color="#fff" />
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                            <span style={{ 
                                fontSize: '10px', 
                                color: 'rgba(255,255,255,0.8)', 
                                fontWeight: '500',
                                letterSpacing: '1px'
                            }}>
                                UTC
                            </span>
                            <span style={{ 
                                fontSize: '22px', 
                                fontWeight: '700', 
                                color: '#fff',
                                fontFamily: 'monospace',
                                letterSpacing: '2px'
                            }}>
                                {formatUTC(utcTime)}
                            </span>
                            <span style={{ 
                                fontSize: '11px', 
                                color: 'rgba(255,255,255,0.7)',
                                fontWeight: '400'
                            }}>
                                {formatDateUTC(utcTime)}
                            </span>
                        </div>
                    </div>

                    {/* Botones de control */}
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                        {/* Botón Replanificar */}
                        <button
                            onClick={handleIniciarSimulacion}
                            disabled={simulacionActiva}
                            style={{
                                padding: '8px 14px',
                                borderRadius: '6px',
                                border: simulacionActiva ? '1px solid #e9ecef' : '1px solid #28a745',
                                background: simulacionActiva ? '#e9ecef' : '#28a745',
                                color: simulacionActiva ? '#6c757d' : 'white',
                                fontSize: '13px',
                                fontWeight: '600',
                                cursor: simulacionActiva ? 'not-allowed' : 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                transition: 'all 0.2s',
                                boxShadow: simulacionActiva ? 'none' : '0 2px 6px rgba(40, 167, 69, 0.3)',
                                whiteSpace: 'nowrap'
                            }}
                        >
                            <FaPlay size={12} />
                            {startButtonLabel}
                        </button>

                        {/* Botón Detener Replanificación */}
                        <button
                            onClick={handleDetenerSimulacion}
                            disabled={!simulacionActiva}
                            style={{
                                padding: '8px 14px',
                                borderRadius: '6px',
                                border: simulacionActiva ? '1px solid #dc3545' : '1px solid #e9ecef',
                                background: simulacionActiva ? '#dc3545' : '#e9ecef',
                                color: simulacionActiva ? 'white' : '#6c757d',
                                fontSize: '13px',
                                fontWeight: '600',
                                cursor: simulacionActiva ? 'pointer' : 'not-allowed',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '6px',
                                transition: 'all 0.2s',
                                boxShadow: simulacionActiva ? '0 2px 6px rgba(220, 53, 69, 0.3)' : 'none',
                                whiteSpace: 'nowrap'
                            }}
                        >
                            <FaStop size={12} />
                            Detener Replanificación
                        </button>

                        {/* Botón Rutas */}
                        <button
                            onClick={() => setShowFlightLines(!showFlightLines)}
                            style={{
                                padding: '8px 14px',
                                borderRadius: '6px',
                                border: showFlightLines ? '1px solid #0d6efd' : '1px solid #e9ecef',
                                background: showFlightLines ? '#0d6efd' : '#e9ecef',
                                color: showFlightLines ? 'white' : '#6c757d',
                                fontSize: '13px',
                                fontWeight: '600',
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
                            <FaRoad size={12} />
                            Rutas
                        </button>
                    </div>
                </div>
            </Paper>
        </Popper>
    );
}
