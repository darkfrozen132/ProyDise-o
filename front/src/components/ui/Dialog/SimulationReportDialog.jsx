// src/components/ui/Dialog/SimulationReportDialog.jsx
import React, { useRef } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Box,
    Typography,
    Divider,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Chip,
    LinearProgress
} from '@mui/material';
import {
    PictureAsPdf as PdfIcon,
    Close as CloseIcon,
    CheckCircle as CheckIcon,
    Flight as FlightIcon,
    Inventory as PackageIcon,
    LocalShipping as DeliveryIcon,
    Warehouse as WarehouseIcon
} from '@mui/icons-material';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

/**
 * SimulationReportDialog - Popup de reporte de simulación completada
 * 
 * Props:
 * - open: boolean - si el dialog está abierto
 * - onClose: function - callback para cerrar el dialog
 * - reportData: object - datos del reporte
 *   - fechaInicio: string - fecha de inicio de la simulación
 *   - fechaFin: string - fecha de fin de la simulación
 *   - tiempoRealTranscurrido: number - segundos reales transcurridos
 *   - pedidosPorDestino: array - [{destino, cantidad, paquetes}]
 *   - metricas: object - {totalVuelos, totalPedidos, vuelosCompletados, pedidosEntregados}
 *   - saturacionAlmacenes: array - [{codigo, nombre, ocupacion, capacidad, porcentaje}]
 *   - vuelosGenerados: array - lista de todos los vuelos
 */
export default function SimulationReportDialog({ open, onClose, reportData }) {
    const reportRef = useRef(null);
    const [generating, setGenerating] = React.useState(false);

    // Valores por defecto si no hay datos
    const data = reportData || {
        fechaInicio: 'N/A',
        fechaFin: 'N/A',
        tiempoRealTranscurrido: 0,
        pedidosPorDestino: [],
        metricas: {
            totalVuelos: 0,
            totalPedidos: 0,
            vuelosCompletados: 0,
            pedidosEntregados: 0
        },
        saturacionAlmacenes: [],
        vuelosGenerados: []
    };

    // Formatear tiempo real
    const formatTiempoReal = (segundos) => {
        const mins = Math.floor(segundos / 60);
        const secs = segundos % 60;
        if (mins > 0) {
            return `${mins}m ${secs}s`;
        }
        return `${secs}s`;
    };

    // Generar PDF del reporte
    const handleGeneratePDF = async () => {
        if (!reportRef.current) return;

        setGenerating(true);
        try {
            // Capturar el contenido como imagen
            const canvas = await html2canvas(reportRef.current, {
                scale: 2,
                useCORS: true,
                logging: false,
                backgroundColor: '#ffffff'
            });

            const imgData = canvas.toDataURL('image/png');
            const pdf = new jsPDF('p', 'mm', 'a4');

            const pdfWidth = pdf.internal.pageSize.getWidth();
            const pdfHeight = pdf.internal.pageSize.getHeight();

            const imgWidth = canvas.width;
            const imgHeight = canvas.height;

            const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);
            const imgX = (pdfWidth - imgWidth * ratio) / 2;
            const imgY = 10;

            pdf.addImage(imgData, 'PNG', imgX, imgY, imgWidth * ratio, imgHeight * ratio);

            // Descargar el PDF
            const fechaStr = new Date().toISOString().slice(0, 10);
            pdf.save(`Reporte_Simulacion_${fechaStr}.pdf`);
        } catch (error) {
            console.error('Error generando PDF:', error);
            alert('Error al generar el PDF. Por favor intente nuevamente.');
        } finally {
            setGenerating(false);
        }
    };

    // Calcular porcentaje de saturación promedio
    const saturacionPromedio = data.saturacionAlmacenes.length > 0
        ? (data.saturacionAlmacenes.reduce((sum, a) => sum + (a.porcentaje || 0), 0) / data.saturacionAlmacenes.length).toFixed(1)
        : 0;

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
            PaperProps={{
                sx: {
                    borderRadius: 3,
                    maxHeight: '90vh'
                }
            }}
        >
            <DialogTitle
                sx={{
                    backgroundColor: '#2c4a6b',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    py: 2
                }}
            >
                <CheckIcon sx={{ fontSize: 28 }} />
                <Box>
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        ¡Simulación Completada!
                    </Typography>
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        Resumen de resultados de la simulación
                    </Typography>
                </Box>
            </DialogTitle>

            <DialogContent sx={{ p: 0 }}>
                <Box ref={reportRef} sx={{ p: 3, backgroundColor: '#fff' }}>
                    {/* Header del reporte */}
                    <Box sx={{ mb: 3, textAlign: 'center' }}>
                        <Typography variant="h5" sx={{ fontWeight: 700, color: '#2c4a6b', mb: 1 }}>
                            📊 Reporte de Simulación Semanal
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Fecha del reporte: {new Date().toLocaleDateString('es-PE', { 
                                year: 'numeric', 
                                month: 'long', 
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                            })}
                        </Typography>
                    </Box>

                    {/* Información de la simulación */}
                    <Paper elevation={0} sx={{ p: 2, mb: 3, backgroundColor: '#f8fafc', borderRadius: 2 }}>
                        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 2, textAlign: 'center' }}>
                            <Box>
                                <Typography variant="caption" color="text.secondary">Fecha Inicio</Typography>
                                <Typography variant="body1" sx={{ fontWeight: 600 }}>
                                    {data.fechaInicio}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="caption" color="text.secondary">Fecha Fin</Typography>
                                <Typography variant="body1" sx={{ fontWeight: 600 }}>
                                    {data.fechaFin}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="caption" color="text.secondary">Tiempo Real</Typography>
                                <Typography variant="body1" sx={{ fontWeight: 600 }}>
                                    {formatTiempoReal(data.tiempoRealTranscurrido)}
                                </Typography>
                            </Box>
                        </Box>
                    </Paper>

                    <Divider sx={{ my: 2 }} />

                    {/* Métricas Generales */}
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#2c4a6b', mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                        <FlightIcon /> Métricas Generales
                    </Typography>

                    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 2, mb: 3 }}>
                        <Paper elevation={1} sx={{ p: 2, borderRadius: 2, textAlign: 'center', borderLeft: '4px solid #3b82f6' }}>
                            <FlightIcon sx={{ fontSize: 32, color: '#3b82f6', mb: 1 }} />
                            <Typography variant="h4" sx={{ fontWeight: 700, color: '#1e293b' }}>
                                {data.metricas.totalVuelos}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Vuelos Generados
                            </Typography>
                            <Chip
                                label={`${data.metricas.vuelosCompletados} completados`}
                                size="small"
                                color="success"
                                sx={{ mt: 1 }}
                            />
                        </Paper>

                        <Paper elevation={1} sx={{ p: 2, borderRadius: 2, textAlign: 'center', borderLeft: '4px solid #10b981' }}>
                            <PackageIcon sx={{ fontSize: 32, color: '#10b981', mb: 1 }} />
                            <Typography variant="h4" sx={{ fontWeight: 700, color: '#1e293b' }}>
                                {data.metricas.totalPedidos}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Pedidos Procesados
                            </Typography>
                            <Chip
                                label={`${data.metricas.pedidosEntregados} entregados`}
                                size="small"
                                color="success"
                                sx={{ mt: 1 }}
                            />
                        </Paper>
                    </Box>

                    <Divider sx={{ my: 2 }} />

                    {/* Pedidos por Destino */}
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#2c4a6b', mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                        <DeliveryIcon /> Pedidos por Destino Final
                    </Typography>

                    {data.pedidosPorDestino.length > 0 ? (
                        <TableContainer component={Paper} elevation={0} sx={{ mb: 3, border: '1px solid #e2e8f0', borderRadius: 2 }}>
                            <Table size="small">
                                <TableHead>
                                    <TableRow sx={{ backgroundColor: '#f1f5f9' }}>
                                        <TableCell sx={{ fontWeight: 700 }}>Destino</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }} align="center">Pedidos</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }} align="center">Paquetes</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }} align="center">%</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {data.pedidosPorDestino.slice(0, 10).map((row, index) => (
                                        <TableRow key={index} hover>
                                            <TableCell>
                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                    <Box sx={{
                                                        width: 8,
                                                        height: 8,
                                                        borderRadius: '50%',
                                                        backgroundColor: index < 3 ? '#3b82f6' : '#94a3b8'
                                                    }} />
                                                    {row.destino || 'N/A'}
                                                </Box>
                                            </TableCell>
                                            <TableCell align="center">{row.cantidad}</TableCell>
                                            <TableCell align="center">{row.paquetes || 0}</TableCell>
                                            <TableCell align="center">
                                                <Chip
                                                    label={`${((row.cantidad / data.metricas.totalPedidos) * 100).toFixed(1)}%`}
                                                    size="small"
                                                    variant="outlined"
                                                    color={index < 3 ? 'primary' : 'default'}
                                                />
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                            {data.pedidosPorDestino.length > 10 && (
                                <Box sx={{ p: 1, textAlign: 'center', backgroundColor: '#f8fafc' }}>
                                    <Typography variant="caption" color="text.secondary">
                                        ... y {data.pedidosPorDestino.length - 10} destinos más
                                    </Typography>
                                </Box>
                            )}
                        </TableContainer>
                    ) : (
                        <Paper elevation={0} sx={{ p: 2, mb: 3, backgroundColor: '#f8fafc', borderRadius: 2, textAlign: 'center' }}>
                            <Typography variant="body2" color="text.secondary">
                                No hay datos de pedidos por destino
                            </Typography>
                        </Paper>
                    )}

                    <Divider sx={{ my: 2 }} />

                    {/* Saturación de Almacenes */}
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#2c4a6b', mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                        <WarehouseIcon /> Saturación de Almacenes
                    </Typography>

                    <Paper elevation={0} sx={{ p: 2, mb: 2, backgroundColor: '#f8fafc', borderRadius: 2 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                            <Typography variant="body2" color="text.secondary">
                                Saturación Promedio
                            </Typography>
                            <Typography variant="h6" sx={{ fontWeight: 700, color: saturacionPromedio > 80 ? '#dc2626' : saturacionPromedio > 50 ? '#f59e0b' : '#10b981' }}>
                                {saturacionPromedio}%
                            </Typography>
                        </Box>
                        <LinearProgress
                            variant="determinate"
                            value={Math.min(100, Number(saturacionPromedio))}
                            sx={{
                                height: 10,
                                borderRadius: 5,
                                backgroundColor: '#e2e8f0',
                                '& .MuiLinearProgress-bar': {
                                    backgroundColor: saturacionPromedio > 80 ? '#dc2626' : saturacionPromedio > 50 ? '#f59e0b' : '#10b981',
                                    borderRadius: 5
                                }
                            }}
                        />
                    </Paper>

                    {data.saturacionAlmacenes.length > 0 && (
                        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 1, mb: 2 }}>
                            {data.saturacionAlmacenes.slice(0, 9).map((almacen, index) => (
                                <Paper
                                    key={index}
                                    elevation={0}
                                    sx={{
                                        p: 1.5,
                                        borderRadius: 2,
                                        border: '1px solid #e2e8f0',
                                        textAlign: 'center'
                                    }}
                                >
                                    <Typography variant="caption" sx={{ fontWeight: 600, color: '#64748b' }}>
                                        {almacen.codigo}
                                    </Typography>
                                    <Typography
                                        variant="h6"
                                        sx={{
                                            fontWeight: 700,
                                            color: almacen.porcentaje > 80 ? '#dc2626' : almacen.porcentaje > 50 ? '#f59e0b' : '#10b981'
                                        }}
                                    >
                                        {almacen.porcentaje?.toFixed(0) || 0}%
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {almacen.ocupacion}/{almacen.capacidad}
                                    </Typography>
                                </Paper>
                            ))}
                        </Box>
                    )}

                    {/* Footer del reporte */}
                    <Box sx={{ mt: 3, pt: 2, borderTop: '1px dashed #e2e8f0', textAlign: 'center' }}>
                        <Typography variant="caption" color="text.secondary">
                            Reporte generado por MoraPack Dashboard • Sistema de Simulación Semanal
                        </Typography>
                    </Box>
                </Box>
            </DialogContent>

            <DialogActions sx={{ p: 2, backgroundColor: '#f8fafc', gap: 1 }}>
                <Button
                    onClick={onClose}
                    variant="outlined"
                    startIcon={<CloseIcon />}
                    sx={{ borderRadius: 2 }}
                >
                    Cerrar
                </Button>
                <Button
                    onClick={handleGeneratePDF}
                    variant="contained"
                    startIcon={<PdfIcon />}
                    disabled={generating}
                    sx={{
                        borderRadius: 2,
                        backgroundColor: '#dc2626',
                        '&:hover': { backgroundColor: '#b91c1c' }
                    }}
                >
                    {generating ? 'Generando PDF...' : 'Descargar PDF'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
