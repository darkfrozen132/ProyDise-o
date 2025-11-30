import { IoArrowBackCircleOutline } from "react-icons/io5";
import { useNavigate } from "react-router-dom";

// <BackIconButton size={40} />

const BackIconButton = ({ size = 32 }) => {
    const navigate = useNavigate();

    return (
        <IoArrowBackCircleOutline
            onClick={() => navigate(-1)}
            size={size}                            // tamaño dinámico
            style={{
                color: "#7a7a7a",
                cursor: "pointer",
                transition: "all 0.2s ease-in-out",
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "#4e4e4e")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "#7a7a7a")}
            title="Regresar"
        />
    );
};

export default BackIconButton;
