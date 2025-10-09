import React from 'react';
import Button from '@mui/material/Button';
import { IoArrowBackOutline } from 'react-icons/io5';
import { useNavigate } from 'react-router-dom';

const BackButton = ({ to, label = 'Regresar', width = 'auto' }) => {
    const navigate = useNavigate();

    const handleClick = () => {
        if (to) {
            navigate(to); // solo navega si existe una redireccion
        }
    };
    return (
        <Button
            variant="outlined"
            size="medium"
            startIcon={<IoArrowBackOutline />}
            onClick={handleClick}
            sx={{
                textTransform: 'none', 
                color: '#333',
                borderColor: '#b3b3b3',
                backgroundColor: '#f9f9f9',
                fontWeight: 600,
                borderRadius: '30px',
                px: 2,
                py: 0.5,
                '&:hover': { // estilos al pasar el mouse
                    backgroundColor: '#e0e0e0',
                    borderColor: '#999',
                },
                width, // puede ser 'auto' o un valor específico
                minWidth: 'unset', // para que el ancho sea solo el del contenido
                cursor: to ? 'pointer' : 'default', // cambia el cursor si no hay redireccion
            }}
        >
            {label}
        </Button>
    );
};

export default BackButton;
