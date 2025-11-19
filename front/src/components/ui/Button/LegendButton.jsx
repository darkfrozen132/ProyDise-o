import { IconButton } from "@mui/material";
import InfoIcon from "@mui/icons-material/Info";

export default function LegendButton({ onClick, style }) {
    return (
        <IconButton
            onClick={onClick}
            title="Mostrar Leyenda"
            sx={{
                position: "absolute",
                bottom: "20px",
                left: "20px",
                backgroundColor: "white",
                boxShadow: "0 2px 6px rgba(0,0,0,0.3)",
                zIndex: 1000,
                "&:hover": { backgroundColor: "#f5f5f5" },
                ...style,  // permite override desde otros lados
            }}
        >
            <InfoIcon sx={{ color: "#2c4a6b" }} />
        </IconButton>
    );
}