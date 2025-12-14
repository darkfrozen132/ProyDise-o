import { IconButton } from "@mui/material";
import { HiAdjustments } from "react-icons/hi";
import { useEffect, useRef } from "react";

export default function ControlButton({ onClick, style, onMount }) {
    const buttonRef = useRef(null);
    
    useEffect(() => {
        // Notificar al padre que el botón se montó
        if (buttonRef.current && onMount) {
            onMount(buttonRef.current);
        }
    }, [onMount]);
    
    return (
        <IconButton
            ref={buttonRef}
            onClick={onClick}
            title="Controles de Simulación"
            sx={{
                position: "absolute",
                bottom: 20,
                right: 20,
                width: 36,
                height: 36,
                backgroundColor: "#ffffff",
                borderRadius: "12px",
                border: "1px solid rgba(148, 163, 184, 0.7)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                transition: "background-color 0.2s ease, box-shadow 0.2s ease",
                zIndex: 1000,
                "&:hover": {
                    backgroundColor: "#f8fafc",
                    boxShadow: "0 5px 10px rgba(15, 23, 42, 0.18)",
                },
                ...style,
            }}
        >
            <HiAdjustments sx={{ color: "#2c4a6b", fontSize: 30, stroke: "white", strokeWidth: 0.8 }} />
        </IconButton>
    );
}