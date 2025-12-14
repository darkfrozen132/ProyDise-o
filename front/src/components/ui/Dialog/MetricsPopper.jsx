// MetricsPopper.jsx
import { Popper, Paper } from "@mui/material";
import { FaChartLine, FaPlane, FaWarehouse } from "react-icons/fa";
import { BiSolidTachometer } from "react-icons/bi";
import { useState } from "react";

export default function MetricsPopper({ open, anchorEl, flightsInAirCount, orderCount, flights, getSaturation }) {
    const [hoveredCard, setHoveredCard] = useState(null);

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
                        {/* Aviones en el aire */}
                        <div 
                            style={getCardStyle(1)}
                            onMouseEnter={() => setHoveredCard(1)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: "#2c4a6b",
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
                                    Aviones en aire
                                </div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#333" }}>
                                    {flightsInAirCount}
                                </div>
                            </div>
                        </div>

                        {/* Porcentaje en vuelo */}
                        <div 
                            style={getCardStyle(2)}
                            onMouseEnter={() => setHoveredCard(2)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: "#2c4a6b",
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <BiSolidTachometer size={13} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    % en vuelo
                                </div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#333" }}>
                                    {((flightsInAirCount / 2866) * 100).toFixed(1)}%
                                </div>
                            </div>
                        </div>

                        {/* Pedidos */}
                        <div 
                            style={getCardStyle(3)}
                            onMouseEnter={() => setHoveredCard(3)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: "#2c4a6b",
                                color: "#fff",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                flexShrink: 0
                            }}>
                                <BiSolidTachometer size={13} />
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                                <div style={{ fontSize: "11px", color: "#6c757d", marginBottom: "2px" }}>
                                    Pedidos realizados
                                </div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#333" }}>
                                    {orderCount}
                                </div>
                            </div>
                        </div>

                        {/* Saturación */}
                        <div 
                            style={getCardStyle(4)}
                            onMouseEnter={() => setHoveredCard(4)}
                            onMouseLeave={() => setHoveredCard(null)}
                        >
                            <div style={{
                                width: "24px",
                                height: "24px",
                                borderRadius: "50%",
                                background: "#2c4a6b",
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
                                    Saturación aeropuertos
                                </div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#333" }}>
                                    {getSaturation()}%
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </Paper>
        </Popper>
    );
}