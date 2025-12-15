// MetricsPopper.jsx
import { Popper, Paper } from "@mui/material";
import { FaChartLine, FaPlane, FaWarehouse, FaBox, FaShoppingCart } from "react-icons/fa";
import { useState } from "react";

// Total de aviones en la flota (para calcular % de utilización)
const TOTAL_FLEET_SIZE = 2866;

export default function MetricsPopper({ open, anchorEl, flightsInAirCount, orderCount, getCargoSaturation, flights, getSaturation }) {
    const [hoveredCard, setHoveredCard] = useState(null);

    // Obtener color del semáforo según el porcentaje
    const getSemaphoreColor = (percentage) => {
        const value = parseFloat(percentage) || 0;
        if (value >= 80) return "#dc3545"; // Rojo - Alta
        if (value >= 50) return "#ffc107"; // Amarillo - Media
        return "#28a745"; // Verde - Baja
    };

    // Obtener etiqueta del nivel
    const getSemaphoreLabel = (percentage) => {
        const value = parseFloat(percentage) || 0;
        if (value >= 80) return "Alta";
        if (value >= 50) return "Media";
        return "Baja";
    };

    // Calcular porcentaje de flota en vuelo
    const getFleetUsagePercent = () => {
        return ((flightsInAirCount / TOTAL_FLEET_SIZE) * 100).toFixed(1);
    };

    const getCardStyle = (cardId) => ({
        background: "#f8f9fa",
        borderRadius: "8px",
        padding: "10px",
        display: "flex",
        alignItems: "center",
        gap: "10px",
        boxShadow: hoveredCard === cardId 
            ? "0 0 12px rgba(0, 212, 255, 0.4), 0 2px 6px rgba(0, 0, 0, .05)"
            : "0 2px 6px rgba(0, 0, 0, .05)",
        transition: "all 0.3s ease",
        cursor: "default",
        border: hoveredCard === cardId ? "1px solid rgba(0, 212, 255, 0.3)" : "1px solid transparent"
    });
    return (
        <Popper
            open={open}
            anchorEl={anchorEl}
            placement="bottom-end"
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
                    width: 180,
                    boxShadow: 4,
                    backgroundColor: "rgba(255,255,255,0.95)",
                }}
            >
                {/* Header */}
                <div
                    style={{
                        backgroundColor: "#2c4a6b",
                        color: "white",
                        display: "flex",
                        alignItems: "center",
                        gap: "6px",
                        padding: "6px 10px",
                        borderTopLeftRadius: "8px",
                        borderTopRightRadius: "8px",
                        fontSize: "13px",
                        fontWeight: "700",
                    }}
                >
                    <FaChartLine size={12} />
                    Métricas
                </div>
                
                {/* Metrics */}
                <div style={{ padding: "0px 10px 10px 10px" }}>
                    <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "10px" }}>
                        {/* Aviones en el aire - Uso de flota */}
                        <div 
                            style={getCardStyle(1)}
                            onMouseEnter={() => setHoveredCard(1)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: getSemaphoreColor(getFleetUsagePercent()),
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <FaPlane size={11} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    Uso de flota
                                </div>
                                <div style={{ 
                                    fontSize: "15px", 
                                    fontWeight: "700", 
                                    color: getSemaphoreColor(getFleetUsagePercent()),
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "6px"
                                }}>
                                    {flightsInAirCount}
                                    <span style={{ 
                                        fontSize: "10px", 
                                        fontWeight: "600",
                                        backgroundColor: getSemaphoreColor(getFleetUsagePercent()),
                                        color: "#fff",
                                        padding: "2px 6px",
                                        borderRadius: "4px"
                                    }}>
                                        {getFleetUsagePercent()}%
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Pedidos */}
                        <div 
                            style={getCardStyle(2)}
                            onMouseEnter={() => setHoveredCard(2)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: "#6366f1",
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <FaShoppingCart size={11} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    Numero de Pedidos
                                </div>
                                <div style={{ 
                                    fontSize: "15px", 
                                    fontWeight: "700", 
                                    color: "#6366f1",
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "6px"
                                }}>
                                    {orderCount}
                                    <span style={{ 
                                        fontSize: "10px", 
                                        fontWeight: "600",
                                        backgroundColor: "#6366f1",
                                        color: "#fff",
                                        padding: "2px 6px",
                                        borderRadius: "4px"
                                    }}>
                                        total
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Nivel de llenado de flota */}
                        <div 
                            style={getCardStyle(3)}
                            onMouseEnter={() => setHoveredCard(3)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: getSemaphoreColor(getCargoSaturation()),
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <FaBox size={11} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    Nivel de llenado
                                </div>
                                <div style={{ 
                                    fontSize: "15px", 
                                    fontWeight: "700", 
                                    color: getSemaphoreColor(getCargoSaturation()),
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "6px"
                                }}>
                                    {getCargoSaturation()}%
                                    <span style={{ 
                                        fontSize: "10px", 
                                        fontWeight: "600",
                                        backgroundColor: getSemaphoreColor(getCargoSaturation()),
                                        color: "#fff",
                                        padding: "2px 6px",
                                        borderRadius: "4px"
                                    }}>
                                        {getSemaphoreLabel(getCargoSaturation())}
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Saturación aeropuertos */}
                        <div 
                            style={getCardStyle(4)}
                            onMouseEnter={() => setHoveredCard(4)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: getSemaphoreColor(getSaturation()),
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <FaWarehouse size={11} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    Saturación almacenes
                                </div>
                                <div style={{ 
                                    fontSize: "15px", 
                                    fontWeight: "700", 
                                    color: getSemaphoreColor(getSaturation()),
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "6px"
                                }}>
                                    {getSaturation()}%
                                    <span style={{ 
                                        fontSize: "10px", 
                                        fontWeight: "600",
                                        backgroundColor: getSemaphoreColor(getSaturation()),
                                        color: "#fff",
                                        padding: "2px 6px",
                                        borderRadius: "4px"
                                    }}>
                                        {getSemaphoreLabel(getSaturation())}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </Paper>
        </Popper>
    );
}