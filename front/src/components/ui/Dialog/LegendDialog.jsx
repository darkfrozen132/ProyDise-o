// src/components/ui/Dialog/LegendDialog.jsx
import { Dialog, DialogTitle, DialogContent, IconButton } from "@mui/material";
import InfoIcon from "@mui/icons-material/Info";

export default function LegendDialog({ open, onClose }) {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle
                sx={{
                    bgcolor: '#2c4a6b',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                }}
            >
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <InfoIcon sx={{ mr: 1 }} />
                    Leyenda del Mapa
                </div>
                <IconButton
                    onClick={() => onClose}
                    sx={{
                        color: 'white',
                        '&:hover': {
                            backgroundColor: 'rgba(255, 255, 255, 0.1)'
                        }
                    }}
                >
                    <i className="fas fa-times" />
                </IconButton>
            </DialogTitle>
            <DialogContent sx={{ mt: 2 }}>
                <div style={{ padding: '20px' }}>
                    <section style={{ marginBottom: '24px' }}>
                        <h3 style={{ marginBottom: '15px', color: '#2c4a6b' }}>Sedes Principales</h3>
                        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '10px' }}>
                            <div style={{ width: '12px', height: '12px', backgroundColor: '#ff6b35', borderRadius: '50%', marginRight: '10px' }}></div>
                            <span>Lima, Bruselas, Baku</span>
                        </div>
                        <div style={{ color: '#666', fontSize: '0.9em', marginLeft: '22px' }}>
                            Capacidad limitada
                        </div>
                    </section>

                    <section style={{ marginBottom: '24px' }}>
                        <h3 style={{ marginBottom: '15px', color: '#2c4a6b' }}>Aeropuertos</h3>
                        <div>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#28a745', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Baja (0-49%)</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#ffc107', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Media (50-79%)</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#dc3545', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Alta (80%+)</span>
                            </div>
                        </div>
                    </section>

                    <section>
                        <h3 style={{ marginBottom: '15px', color: '#2c4a6b' }}>Aviones</h3>
                        <div>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#28a745', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Poca carga (0-49%)</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#ffc107', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Media (50-79%)</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center' }}>
                                <div style={{ width: '12px', height: '12px', backgroundColor: '#dc3545', borderRadius: '50%', marginRight: '10px' }}></div>
                                <span>Mucha (80%+)</span>
                            </div>
                        </div>
                    </section>
                </div>
            </DialogContent>
        </Dialog>
    );
}