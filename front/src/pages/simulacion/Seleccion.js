import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import typeOperation from '../../assets/images/type_operation.jpg';
import { FiActivity, FiCalendar, FiAlertTriangle, FiSunrise } from "react-icons/fi";
import { FaChevronRight } from "react-icons/fa";

const Seleccion = () => {
    const [selectedMode, setSelectedMode] = useState(null);
    const navigate = useNavigate();

    /* Operaciones con caracteristicas */
    const operationModes = [
        {
            id: 'realtime',
            title: 'Monitoreo en Tiempo Real',
            description: 'Control y seguimiento activo de operaciones',
            icon: FiActivity,
            color: '#00d4ff',
            path: 'simulador-diario'
        },        
        {
            id: 'weekly',
            title: 'Simulación Semanal',
            description: 'Planificación operativa de 7 días',
            icon: FiCalendar,
            color: '#10b981',
            path: 'simulador-semanal'
        },
        {
            id: 'collapse',
            title: 'Simulación de Colapso',
            description: 'Análisis de escenarios críticos',
            icon: FiAlertTriangle,
            color: '#ef4444',
            path: 'simulador-colapso'
        }
    ];

    /* Evento OnClick */
    const handleModeClick = (mode) => {
        setSelectedMode(mode.id);
        /* Navegar a la ruta especificada */
        setTimeout(() => {
            navigate(mode.path);
        }, 300);
    };

    return (
        /* Congifuración del fondo */
        <div style={{
            minHeight: '100%',
            display: 'flex',
            flexDirection: 'column',
            backgroundImage: `
                linear-gradient(
                rgba(44, 74, 107, 0.7),
                rgba(30, 58, 87, 0.7)
                ),
                url(${typeOperation})
            `,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundRepeat: 'no-repeat'
        }}>

            {/* Content */}
            <div style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '2rem'
            }}>
                <div style={{ width: '100%', maxWidth: '1000px' }}>
                    <h2 style={{
                        color: 'white',
                        fontSize: '1.75rem',
                        fontWeight: '700',
                        textAlign: 'center',
                        marginBottom: '0.5rem'
                    }}>
                        Modos de Operación
                    </h2>
                    <p style={{
                        color: 'rgba(255, 255, 255, 0.7)',
                        textAlign: 'center',
                        marginBottom: '2.5rem',
                        fontSize: '0.95rem'
                    }}>
                        Seleccione el modo según sus necesidades operativas
                    </p>

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                        gap: '1.25rem'
                    }}>
                        {/* Tarjetas de Modo de Operación */}
                        {operationModes.map((mode) => {
                            const Icon = mode.icon;
                            const isSelected = selectedMode === mode.id;
                            /* Tarjeta individual */
                            return (
                                /* Botón que envuelve la tarjeta */
                                <button
                                    key={mode.id}
                                    onClick={() => handleModeClick(mode)}
                                    /* Efectos hover */
                                    onMouseEnter={(e) => {
                                        e.currentTarget.style.transform = 'translateY(-6px)';
                                        e.currentTarget.style.boxShadow = `0 12px 32px -4px ${mode.color}`;
                                    }}
                                    /* Revertir efectos hover */
                                    onMouseLeave={(e) => {
                                        if (!isSelected) {
                                            e.currentTarget.style.transform = 'translateY(0)';
                                            e.currentTarget.style.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.3)';
                                        }
                                    }}
                                    /* Estilos de la tarjeta */
                                    style={{
                                        background: 'white',
                                        border: isSelected ? `2px solid ${mode.color}` : '2px solid transparent',
                                        borderRadius: '0.875rem',
                                        padding: '1.5rem',
                                        cursor: 'pointer',
                                        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                                        boxShadow: isSelected
                                            ? `0 12px 32px -4px ${mode.color}`
                                            : '0 4px 16px rgba(0, 0, 0, 0.3)',
                                        textAlign: 'left',
                                        position: 'relative',
                                        transform: isSelected ? 'translateY(-6px)' : 'translateY(0)'
                                    }}
                                >
                                    {/* Contenido de la tarjeta */}
                                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem' }}>
                                        {/* Icono */}
                                        <div style={{
                                            width: '48px',
                                            height: '48px',
                                            background: mode.color,
                                            borderRadius: '0.75rem',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            flexShrink: 0,
                                            boxShadow: `0 4px 12px ${mode.color}60`
                                        }}>
                                            <Icon style={{ width: '24px', height: '24px', color: 'white' }} />
                                        </div>

                                        {/* Textos */}
                                        <div style={{ flex: 1 }}>
                                            <h3 style={{
                                                fontSize: '1.125rem',
                                                fontWeight: '700',
                                                color: '#1e293b',
                                                marginBottom: '0.375rem',
                                                lineHeight: '1.3'
                                            }}>
                                                {mode.title}
                                            </h3>
                                            <p style={{
                                                fontSize: '0.875rem',
                                                color: '#64748b',
                                                lineHeight: '1.4',
                                                margin: 0
                                            }}>
                                                {mode.description}
                                            </p>
                                        </div>

                                        {/* Flecha */}
                                        <FaChevronRight style={{
                                            width: '20px',
                                            height: '20px',
                                            color: mode.color,
                                            flexShrink: 0,
                                            marginTop: '0.25rem'
                                        }} />
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Seleccion;