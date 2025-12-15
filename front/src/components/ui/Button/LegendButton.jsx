import { Box, IconButton } from "@mui/material";
import InfoIcon from "@mui/icons-material/Info";

export default function LegendButton({ onClick, style }) {
    return (
        <IconButton
            onClick={onClick}
            title="Leyenda"
            sx={{
                position: "absolute",
                bottom: "20px",
                left: "20px",
                padding: 0,
                backgroundColor: "transparent",
                boxShadow: "none",
                zIndex: 1000,
                "&:hover": { boxShadow: "0 0 10px 4px #ddecfcff" },
                ...style,  // permite override desde otros lados
            }}
        >
            <InfoIcon sx={{ color: "#2c4a6b", fontSize: 30, stroke: "white", strokeWidth: 0.8,}} />
        </IconButton>
    );
}