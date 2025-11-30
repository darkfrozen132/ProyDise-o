import { IconButton } from "@mui/material";
import { BiBarChartAlt2 } from "react-icons/bi";

export default function MetricsButton({ onClick, style }) {
    return (
        <IconButton
            onClick={onClick}
            title="Métricas"
            sx={{
                position: "absolute",
                top: 20,
                right: 20,

                width: 36,
                height: 36,

                backgroundColor: "#ffffff",
                borderRadius: "12px",
                border: "1px solid rgba(148, 163, 184, 0.7)", // gris suave

                display: "flex",
                alignItems: "center",
                justifyContent: "center",

                transition: "background-color 0.2s ease, box-shadow 0.2s ease",
                zIndex: 1000,
                "&:hover": {
                    backgroundColor: "#f8fafc",
                    boxShadow: "0 5px 10px rgba(15, 23, 42, 0.18)",
                },

                ...style, // por si quieres sobrescribir algo desde afuera
            }}
        >
            {/* Ícono de barras tipo métricas */}
            <BiBarChartAlt2 sx={{ color: "#2c4a6b", fontSize: 30, stroke: "white", strokeWidth: 0.8,}} />
        </IconButton>
    );
}
