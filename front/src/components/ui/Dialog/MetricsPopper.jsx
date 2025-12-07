// MetricsPopper.jsx
import { Popper, Paper } from "@mui/material";
import { FaChartLine } from "react-icons/fa";
import { FaPlane } from "react-icons/fa";
import { BiSolidTachometer } from "react-icons/bi";
import { FaWarehouse } from "react-icons/fa";
import { FaBuilding } from "react-icons/fa";

export default function MetricsPopper({ open, anchorEl, flightsInAirCount, orderCount, flights, getSaturation }) {
    return (
        <Popper
            open={open}
            anchorEl={anchorEl}
            placement="bottom-end"
            modifiers={[
                {
                    name: "offset",
                    options: { offset: [0, 5] }, // separación vertical
                },
            ]}
            sx={{ zIndex: 1300 }}
        >
            <Paper
                sx={{
                    borderRadius: 2,
                    width: 200,
                    paddingTop: -2,
                    boxShadow: 4,
                    backgroundColor: "rgba(255,255,255,0.8)",
                }}
            >
                <div
                    style={{
                        backgroundColor: "#2c4a6b",
                        color: "white",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        padding: "8px 12px",
                        borderTopLeftRadius: "12px",
                        borderTopRightRadius: "12px",
                    }}
                >
                    <div style={{ display: "flex", alignItems: "center", fontSize: "15px", fontWeight: "800", gap: "8px"}}>
                        <FaChartLine />
                        Métricas
                    </div>
                </div>
                <div className="stats-section" style = {{padding: "0px 15px 12px 15px"}}>
                    <div className="metrics-grid">
                        <div className="metric-card">
                            <div className="metric-icon">
                                <FaPlane size={15}/>
                            </div>
                            <div className="metric-content">
                                <div className="metric-label">Número de aviones</div>
                                <div className="metric-value">{flightsInAirCount}</div>
                                <div className="metric-sublabel">en el aire</div>
                                {/*<div className="metric-sublabel">de 402 total</div>*/}
                            </div>
                        </div>

                        <div className="metric-card">
                            <div className="metric-icon">
                                <BiSolidTachometer size={20}/>
                            </div>
                            <div className="metric-content">
                                <div className="metric-label">Porcentaje de aviones</div>
                                <div className="metric-value">
                                    {((flightsInAirCount / 2866) * 100).toFixed(1)}%
                                </div>
                                <div className="metric-sublabel">en vuelo</div>
                            </div>
                        </div>

                        <div className="metric-card">
                            <div className="metric-icon">
                                <BiSolidTachometer size={20}/>
                            </div>
                            <div className="metric-content">
                                <div className="metric-label">Número de pedidos</div>
                                <div className="metric-value">{orderCount}</div>
                                <div className="metric-sublabel">realizados</div>
                            </div>
                        </div>

                        <div className="metric-card">
                            <div className="metric-icon">
                                <FaWarehouse size={15}/>
                            </div>
                            <div className="metric-content">
                                <div className="metric-label">Saturación aeropuertos</div>
                                <div className="metric-value">{getSaturation()}%</div>
                                <div className="metric-sublabel">almacenes regulares</div>
                            </div>
                        </div>
                    </div>
                </div>
            </Paper>
        </Popper>
    );
}
