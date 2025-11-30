// src/components/ui/Dialog/LegendDialog.jsx
import { Popper, Paper, IconButton } from "@mui/material";
import { IoClose } from "react-icons/io5";

export default function LegendDialog({ anchorEl, open, onClose }) {
    return (
        <Popper
            open={open}
            anchorEl={anchorEl}
            placement="bottom-start"
            style={{ zIndex: 1300 }}
            modifiers={[
                {
                    name: 'offset',
                    options: {
                        offset: [2, 5], // ajusta la posición (horizontal, vertical)
                    },
                },
            ]}
        >
            <Paper
                sx={{
                    borderRadius: 3,
                    boxShadow: 3,
                    width: 200,
                    backgroundColor: "rgba(255,255,255,0.8)",
                }}
            >
                {/* HEADER */}
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
                    <div style={{ display: "flex", alignItems: "center", fontSize: "15px", fontWeight: "800"}}>
                        Leyenda del Mapa
                    </div>
                    <IconButton
                        onClick={onClose}
                        sx={{
                            color: "white",
                            "&:hover": { backgroundColor: "rgba(255,255,255,0.1)" },
                        }}
                        size="small"
                    >
                        <IoClose />
                    </IconButton>
                </div>

                {/* CONTENIDO */}
                <div style={{ padding: "16px 20px" }}>
                    <section style={{ marginBottom: "16px" }}>
                        <h5 style={{ marginBottom: "10px", color: "#2c4a6b" }}>
                            Sedes Principales
                        </h5>
                        <div style={{ display: "flex", alignItems: "center", marginBottom: "8px" }}>
                            <div
                                style={{
                                    width: 12,
                                    height: 12,
                                    backgroundColor: "#ff6b35",
                                    borderRadius: "80%",
                                    marginRight: 10,
                                }}
                            />
                            <span style={{ fontSize: "12px" }}>Lima, Bruselas, Baku</span>
                        </div>
                    </section>

                    <section>
                        <h5 style={{ marginBottom: "10px", color: "#2c4a6b" }}>
                            Almacenamiento
                        </h5>
                        <div>
                            <div style={{ display: "flex", alignItems: "center", marginBottom: 6 }}>
                                <div
                                    style={{
                                        width: 12,
                                        height: 12,
                                        backgroundColor: "#28a745",
                                        borderRadius: "50%",
                                        marginRight: 10,
                                    }}
                                />
                                <span style={{ fontSize: "12px" }}>Baja (0 - 49%)</span>
                            </div>
                            <div style={{ display: "flex", alignItems: "center", marginBottom: 6 }}>
                                <div
                                    style={{
                                        width: 12,
                                        height: 12,
                                        backgroundColor: "#ffc107",
                                        borderRadius: "50%",
                                        marginRight: 10,
                                    }}
                                />
                                <span style={{ fontSize: "12px" }}>Media (50 - 79%)</span>
                            </div>
                            <div style={{ display: "flex", alignItems: "center" }}>
                                <div
                                    style={{
                                        width: 12,
                                        height: 12,
                                        backgroundColor: "#dc3545",
                                        borderRadius: "50%",
                                        marginRight: 10,
                                    }}
                                />
                                <span style={{ fontSize: "12px" }}>Alta (80% +)</span>
                            </div>
                        </div>
                    </section>
                </div>
            </Paper>
        </Popper>
    );
}