import React, { useState, useMemo } from 'react';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Box,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Tabs,
    Tab,
    Chip,
    LinearProgress,
    IconButton,
    Divider,
    Card,
    CardContent,
    Grid
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import DownloadIcon from '@mui/icons-material/Download';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import AccessTimeIcon from '@mui/icons-material/AccessTime';

/**
 * Modal de Reporte de Simulación
 * Muestra los resultados finales de la simulación con:
 * - Lista de pedidos con sus vuelos asignados
 * - Métricas finales (pedidos entregados, en tránsito, pendientes)
 * - Opción de exportar a PDF
 */
const ReporteSimulacionModal = ({
    open,
    onClose,
    pedidos = [],           // Lista de todos los pedidos con sus vuelos
    vuelos = [],            // Lista de todos los vuelos
    metricas = {},          // Métricas de la simulación
    fechaInicio = '',       // Fecha de inicio de simulación
    fechaFin = '',          // Fecha de fin de simulación
    tiempoRealTranscurrido = 0  // Tiempo real en segundos
}) => {
    const [tabActual, setTabActual] = useState(0);

    // Calcular métricas de pedidos
    const metricasPedidos = useMemo(() => {
        const entregados = pedidos.filter(p => p.status === 'Entregado' || p.estado === 'Entregado').length;
        const enTransito = pedidos.filter(p => 
            p.status === 'En vuelo' || p.estado === 'En vuelo' ||
            p.status === 'En escala' || p.estado === 'En escala'
        ).length;
        const pendientes = pedidos.filter(p => p.status === 'Planificado' || p.estado === 'Planificado' || !p.status).length;
        const total = pedidos.length;
        const porcentajeEntregados = total > 0 ? ((entregados / total) * 100).toFixed(1) : 0;
        
        return {
            total,
            entregados,
            enTransito,
            pendientes,
            porcentajeEntregados
        };
    }, [pedidos]);

    // Calcular métricas de vuelos
    const metricasVuelos = useMemo(() => {
        const completados = vuelos.filter(v => v.status === 'completed' || v.progress >= 100).length;
        const enVuelo = vuelos.filter(v => v.status === 'active' && v.progress > 0 && v.progress < 100).length;
        const pendientes = vuelos.filter(v => v.status === 'waiting' || v.progress === 0).length;
        const total = vuelos.length;
        
        // Calcular carga total transportada
        const cargaTotal = vuelos.reduce((sum, v) => sum + (v.currentPackages || v.pedidos?.length || 0), 0);
        
        return {
            total,
            completados,
            enVuelo,
            pendientes,
            cargaTotal
        };
    }, [vuelos]);

    // Formatear tiempo transcurrido
    const formatearTiempo = (segundos) => {
        const horas = Math.floor(segundos / 3600);
        const minutos = Math.floor((segundos % 3600) / 60);
        const segs = segundos % 60;
        
        if (horas > 0) {
            return `${horas}h ${minutos}m ${segs}s`;
        } else if (minutos > 0) {
            return `${minutos}m ${segs}s`;
        }
        return `${segs}s`;
    };

    // Generar contenido HTML para PDF
    const generarContenidoPDF = () => {
        const fechaGeneracion = new Date().toLocaleString('es-ES');
        
        return `
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reporte de Simulación - MoraPack</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            padding: 40px; 
            color: #333;
            line-height: 1.6;
        }
        .header { 
            text-align: center; 
            margin-bottom: 30px; 
            padding-bottom: 20px;
            border-bottom: 3px solid #1a237e;
        }
        .header h1 { 
            color: #1a237e; 
            font-size: 28px;
            margin-bottom: 10px;
        }
        .header .subtitle {
            color: #666;
            font-size: 14px;
        }
        .metricas-container {
            display: flex;
            justify-content: space-around;
            margin: 30px 0;
            flex-wrap: wrap;
            gap: 20px;
        }
        .metrica-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 12px;
            text-align: center;
            min-width: 150px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        .metrica-card.success { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
        .metrica-card.warning { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .metrica-card.info { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .metrica-valor { font-size: 32px; font-weight: bold; }
        .metrica-label { font-size: 12px; opacity: 0.9; margin-top: 5px; }
        .seccion { margin: 30px 0; }
        .seccion h2 { 
            color: #1a237e; 
            font-size: 20px;
            margin-bottom: 15px;
            padding-bottom: 10px;
            border-bottom: 2px solid #e0e0e0;
        }
        table { 
            width: 100%; 
            border-collapse: collapse; 
            margin-top: 15px;
            font-size: 12px;
        }
        th { 
            background: #1a237e; 
            color: white; 
            padding: 12px 8px;
            text-align: left;
            font-weight: 600;
        }
        td { 
            padding: 10px 8px; 
            border-bottom: 1px solid #e0e0e0;
        }
        tr:nth-child(even) { background: #f8f9fa; }
        tr:hover { background: #e3f2fd; }
        .status-chip {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 600;
        }
        .status-entregado { background: #c8e6c9; color: #2e7d32; }
        .status-envuelo { background: #bbdefb; color: #1565c0; }
        .status-escala { background: #e1bee7; color: #7b1fa2; }
        .status-pendiente { background: #fff3e0; color: #ef6c00; }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #e0e0e0;
            text-align: center;
            color: #666;
            font-size: 12px;
        }
        .resumen-box {
            background: #f5f5f5;
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .resumen-item {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px dashed #ddd;
        }
        .resumen-item:last-child { border-bottom: none; }
        @media print {
            body { padding: 20px; }
            .metrica-card { break-inside: avoid; }
            table { page-break-inside: auto; }
            tr { page-break-inside: avoid; }
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>📊 Reporte de Simulación</h1>
        <p class="subtitle">Sistema de Gestión Logística - MoraPack</p>
        <p class="subtitle">Generado: ${fechaGeneracion}</p>
    </div>

    <div class="resumen-box">
        <div class="resumen-item">
            <span><strong>📅 Fecha de inicio:</strong></span>
            <span>${fechaInicio || 'No especificada'}</span>
        </div>
        <div class="resumen-item">
            <span><strong>📅 Fecha de fin:</strong></span>
            <span>${fechaFin || 'No especificada'}</span>
        </div>
        <div class="resumen-item">
            <span><strong>⏱️ Tiempo real transcurrido:</strong></span>
            <span>${formatearTiempo(tiempoRealTranscurrido)}</span>
        </div>
    </div>

    <div class="metricas-container">
        <div class="metrica-card">
            <div class="metrica-valor">${metricasPedidos.total}</div>
            <div class="metrica-label">📦 Pedidos Totales</div>
        </div>
        <div class="metrica-card success">
            <div class="metrica-valor">${metricasPedidos.entregados}</div>
            <div class="metrica-label">✅ Entregados</div>
        </div>
        <div class="metrica-card info">
            <div class="metrica-valor">${metricasPedidos.enTransito}</div>
            <div class="metrica-label">✈️ En Tránsito</div>
        </div>
        <div class="metrica-card warning">
            <div class="metrica-valor">${metricasPedidos.pendientes}</div>
            <div class="metrica-label">⏳ Pendientes</div>
        </div>
    </div>

    <div class="metricas-container">
        <div class="metrica-card">
            <div class="metrica-valor">${metricasVuelos.total}</div>
            <div class="metrica-label">✈️ Vuelos Totales</div>
        </div>
        <div class="metrica-card success">
            <div class="metrica-valor">${metricasVuelos.completados}</div>
            <div class="metrica-label">🛬 Completados</div>
        </div>
        <div class="metrica-card info">
            <div class="metrica-valor">${metricasVuelos.cargaTotal}</div>
            <div class="metrica-label">📦 Carga Total</div>
        </div>
    </div>

    <div class="seccion">
        <h2>📦 Detalle de Pedidos (${pedidos.length})</h2>
        <table>
            <thead>
                <tr>
                    <th>#</th>
                    <th>ID Pedido</th>
                    <th>Origen</th>
                    <th>Destino</th>
                    <th>Cantidad</th>
                    <th>Vuelo Asignado</th>
                    <th>Estado</th>
                </tr>
            </thead>
            <tbody>
                ${pedidos.slice(0, 100).map((p, idx) => {
                    const status = p.status || p.estado || 'Pendiente';
                    const statusClass = status === 'Entregado' ? 'status-entregado' : 
                                        status === 'En vuelo' ? 'status-envuelo' : 
                                        status === 'En escala' ? 'status-escala' : 'status-pendiente';
                    return `
                <tr>
                    <td>${idx + 1}</td>
                    <td><strong>${p.idPedido || p.id || '-'}</strong></td>
                    <td>${p.origin || p.origen || '-'}</td>
                    <td>${p.destination || p.destino || '-'}</td>
                    <td>${p.cantidad || 1}</td>
                    <td>${p.flightId || p.vueloId || '-'}</td>
                    <td><span class="status-chip ${statusClass}">${status}</span></td>
                </tr>`;
                }).join('')}
                ${pedidos.length > 100 ? `
                <tr>
                    <td colspan="7" style="text-align: center; font-style: italic; color: #666;">
                        ... y ${pedidos.length - 100} pedidos más
                    </td>
                </tr>` : ''}
            </tbody>
        </table>
    </div>

    <div class="seccion">
        <h2>✈️ Detalle de Vuelos (${vuelos.length})</h2>
        <table>
            <thead>
                <tr>
                    <th>#</th>
                    <th>ID Vuelo</th>
                    <th>Origen</th>
                    <th>Destino</th>
                    <th>Paquetes</th>
                    <th>Progreso</th>
                    <th>Estado</th>
                </tr>
            </thead>
            <tbody>
                ${vuelos.slice(0, 50).map((v, idx) => {
                    const progreso = v.progress || 0;
                    const status = v.status === 'completed' ? 'Completado' : 
                                   v.status === 'active' ? 'En vuelo' : 'Esperando';
                    const statusClass = status === 'Completado' ? 'status-entregado' : 
                                        status === 'En vuelo' ? 'status-envuelo' : 'status-pendiente';
                    return `
                <tr>
                    <td>${idx + 1}</td>
                    <td><strong>${v.id || '-'}</strong></td>
                    <td>${v.origin?.code || '-'}</td>
                    <td>${v.destination?.code || '-'}</td>
                    <td>${v.currentPackages || v.pedidos?.length || 0}</td>
                    <td>${typeof progreso === 'number' ? progreso.toFixed(1) : progreso}%</td>
                    <td><span class="status-chip ${statusClass}">${status}</span></td>
                </tr>`;
                }).join('')}
                ${vuelos.length > 50 ? `
                <tr>
                    <td colspan="7" style="text-align: center; font-style: italic; color: #666;">
                        ... y ${vuelos.length - 50} vuelos más
                    </td>
                </tr>` : ''}
            </tbody>
        </table>
    </div>

    <div class="footer">
        <p>🚀 Sistema MoraPack - Simulación de Logística Aérea</p>
        <p>Reporte generado automáticamente</p>
    </div>
</body>
</html>`;
    };

    // Descargar como PDF
    const descargarPDF = () => {
        const contenido = generarContenidoPDF();
        
        // Crear un iframe oculto para imprimir
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
        
        iframe.contentDocument.write(contenido);
        iframe.contentDocument.close();
        
        // Esperar a que cargue y luego imprimir
        iframe.onload = () => {
            setTimeout(() => {
                iframe.contentWindow.print();
                // Remover iframe después de imprimir
                setTimeout(() => {
                    document.body.removeChild(iframe);
                }, 1000);
            }, 500);
        };
    };

    // Obtener color del chip según estado
    const getStatusColor = (status) => {
        switch (status) {
            case 'Entregado':
            case 'completed':
                return 'success';
            case 'En vuelo':
            case 'active':
                return 'primary';
            case 'En escala':
                return 'info';
            case 'Planificado':
            case 'waiting':
            default:
                return 'warning';
        }
    };

    // Obtener label del chip según estado
    const getStatusLabel = (status) => {
        switch (status) {
            case 'completed': return 'Completado';
            case 'active': return 'En vuelo';
            case 'waiting': return 'Esperando';
            case 'En escala': return 'En escala';
            default: return status || 'Pendiente';
        }
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="lg"
            fullWidth
            PaperProps={{
                sx: {
                    borderRadius: 3,
                    maxHeight: '90vh'
                }
            }}
        >
            <DialogTitle sx={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center',
                background: 'linear-gradient(135deg, #1a237e 0%, #3949ab 100%)',
                color: 'white',
                py: 2
            }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <AssessmentIcon />
                    <Typography variant="h6" fontWeight="bold">
                        📊 Reporte de Simulación
                    </Typography>
                </Box>
                <IconButton onClick={onClose} sx={{ color: 'white' }}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>

            <DialogContent sx={{ p: 0 }}>
                {/* Resumen de métricas */}
                <Box sx={{ p: 3, bgcolor: '#f5f5f5' }}>
                    <Grid container spacing={2}>
                        {/* Métricas de tiempo */}
                        <Grid item xs={12}>
                            <Card sx={{ mb: 2 }}>
                                <CardContent sx={{ py: 1.5 }}>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-around', flexWrap: 'wrap', gap: 2 }}>
                                        <Box sx={{ textAlign: 'center' }}>
                                            <Typography variant="caption" color="textSecondary">Fecha Inicio</Typography>
                                            <Typography variant="body1" fontWeight="bold">{fechaInicio || '-'}</Typography>
                                        </Box>
                                        <Box sx={{ textAlign: 'center' }}>
                                            <Typography variant="caption" color="textSecondary">Fecha Fin</Typography>
                                            <Typography variant="body1" fontWeight="bold">{fechaFin || '-'}</Typography>
                                        </Box>
                                        <Box sx={{ textAlign: 'center' }}>
                                            <Typography variant="caption" color="textSecondary">Tiempo Real</Typography>
                                            <Typography variant="body1" fontWeight="bold" color="primary">
                                                <AccessTimeIcon sx={{ fontSize: 16, mr: 0.5, verticalAlign: 'text-bottom' }} />
                                                {formatearTiempo(tiempoRealTranscurrido)}
                                            </Typography>
                                        </Box>
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* Métricas de pedidos */}
                        <Grid item xs={12} md={6}>
                            <Card sx={{ height: '100%' }}>
                                <CardContent>
                                    <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <LocalShippingIcon color="primary" /> Pedidos
                                    </Typography>
                                    <Box sx={{ mb: 2 }}>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                            <Typography variant="body2">Progreso de entregas</Typography>
                                            <Typography variant="body2" fontWeight="bold" color="success.main">
                                                {metricasPedidos.porcentajeEntregados}%
                                            </Typography>
                                        </Box>
                                        <LinearProgress 
                                            variant="determinate" 
                                            value={parseFloat(metricasPedidos.porcentajeEntregados)} 
                                            sx={{ height: 10, borderRadius: 5 }}
                                            color="success"
                                        />
                                    </Box>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center' }}>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="text.primary">{metricasPedidos.total}</Typography>
                                            <Typography variant="caption" color="textSecondary">Total</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="success.main">{metricasPedidos.entregados}</Typography>
                                            <Typography variant="caption" color="textSecondary">Entregados</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="primary.main">{metricasPedidos.enTransito}</Typography>
                                            <Typography variant="caption" color="textSecondary">En tránsito</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="warning.main">{metricasPedidos.pendientes}</Typography>
                                            <Typography variant="caption" color="textSecondary">Pendientes</Typography>
                                        </Box>
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>

                        {/* Métricas de vuelos */}
                        <Grid item xs={12} md={6}>
                            <Card sx={{ height: '100%' }}>
                                <CardContent>
                                    <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                                        <FlightTakeoffIcon color="primary" /> Vuelos
                                    </Typography>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center', mt: 3 }}>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="text.primary">{metricasVuelos.total}</Typography>
                                            <Typography variant="caption" color="textSecondary">Total</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="success.main">{metricasVuelos.completados}</Typography>
                                            <Typography variant="caption" color="textSecondary">Completados</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="primary.main">{metricasVuelos.enVuelo}</Typography>
                                            <Typography variant="caption" color="textSecondary">En vuelo</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="h5" fontWeight="bold" color="info.main">{metricasVuelos.cargaTotal}</Typography>
                                            <Typography variant="caption" color="textSecondary">Carga total</Typography>
                                        </Box>
                                    </Box>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>
                </Box>

                {/* Tabs para ver detalles */}
                <Box sx={{ borderBottom: 1, borderColor: 'divider', px: 2 }}>
                    <Tabs value={tabActual} onChange={(e, v) => setTabActual(v)}>
                        <Tab label={`📦 Pedidos (${pedidos.length})`} />
                        <Tab label={`✈️ Vuelos (${vuelos.length})`} />
                    </Tabs>
                </Box>

                {/* Contenido de tabs */}
                <Box sx={{ p: 2, maxHeight: 400, overflow: 'auto' }}>
                    {/* Tab Pedidos */}
                    {tabActual === 0 && (
                        <TableContainer component={Paper} elevation={0}>
                            <Table size="small" stickyHeader>
                                <TableHead>
                                    <TableRow>
                                        <TableCell><strong>#</strong></TableCell>
                                        <TableCell><strong>ID Pedido</strong></TableCell>
                                        <TableCell><strong>Origen</strong></TableCell>
                                        <TableCell><strong>Destino</strong></TableCell>
                                        <TableCell><strong>Cantidad</strong></TableCell>
                                        <TableCell><strong>Vuelo</strong></TableCell>
                                        <TableCell><strong>Estado</strong></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {pedidos.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={7} align="center">
                                                <Typography color="textSecondary" sx={{ py: 4 }}>
                                                    No hay pedidos registrados
                                                </Typography>
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        pedidos.map((pedido, idx) => (
                                            <TableRow key={pedido.idPedido || pedido.id || idx} hover>
                                                <TableCell>{idx + 1}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" fontWeight="bold">
                                                        {pedido.idPedido || pedido.id || '-'}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>{pedido.origin || pedido.origen || '-'}</TableCell>
                                                <TableCell>{pedido.destination || pedido.destino || '-'}</TableCell>
                                                <TableCell>{pedido.cantidad || 1}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" color="primary">
                                                        {pedido.flightId || pedido.vueloId || '-'}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        size="small"
                                                        label={getStatusLabel(pedido.status || pedido.estado)}
                                                        color={getStatusColor(pedido.status || pedido.estado)}
                                                        icon={
                                                            (pedido.status || pedido.estado) === 'Entregado' 
                                                                ? <CheckCircleIcon /> 
                                                                : (pedido.status || pedido.estado) === 'En vuelo'
                                                                    ? <FlightTakeoffIcon />
                                                                    : <WarningIcon />
                                                        }
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}

                    {/* Tab Vuelos */}
                    {tabActual === 1 && (
                        <TableContainer component={Paper} elevation={0}>
                            <Table size="small" stickyHeader>
                                <TableHead>
                                    <TableRow>
                                        <TableCell><strong>#</strong></TableCell>
                                        <TableCell><strong>ID Vuelo</strong></TableCell>
                                        <TableCell><strong>Origen</strong></TableCell>
                                        <TableCell><strong>Destino</strong></TableCell>
                                        <TableCell><strong>Paquetes</strong></TableCell>
                                        {/*<TableCell><strong>Progreso</strong></TableCell>*/}
                                        {/*<TableCell><strong>Estado</strong></TableCell>*/}
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {vuelos.length === 0 ? (
                                        <TableRow>
                                            <TableCell colSpan={7} align="center">
                                                <Typography color="textSecondary" sx={{ py: 4 }}>
                                                    No hay vuelos registrados
                                                </Typography>
                                            </TableCell>
                                        </TableRow>
                                    ) : (
                                        vuelos.map((vuelo, idx) => (
                                            <TableRow key={vuelo.id || idx} hover>
                                                <TableCell>{idx + 1}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" fontWeight="bold">
                                                        {vuelo.id || '-'}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>{vuelo.origin?.code || '-'}</TableCell>
                                                <TableCell>{vuelo.destination?.code || '-'}</TableCell>
                                                <TableCell>{vuelo.currentPackages || vuelo.pedidos?.length || 0}</TableCell>
                                                <TableCell>
                                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                        <LinearProgress
                                                            variant="determinate"
                                                            value={Math.min(100, vuelo.progress || 0)}
                                                            sx={{ width: 60, height: 6, borderRadius: 3 }}
                                                        />
                                                        <Typography variant="caption">
                                                            {typeof vuelo.progress === 'number' ? vuelo.progress.toFixed(0) : 0}%
                                                        </Typography>
                                                    </Box>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        size="small"
                                                        label={getStatusLabel(vuelo.status)}
                                                        color={getStatusColor(vuelo.status)}
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        ))
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </Box>
            </DialogContent>

            <Divider />

            <DialogActions sx={{ p: 2, justifyContent: 'space-between' }}>
                <Typography variant="caption" color="textSecondary">
                    Generado: {new Date().toLocaleString('es-ES')}
                </Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button onClick={onClose} color="inherit">
                        Cerrar
                    </Button>
                    <Button
                        variant="contained"
                        color="primary"
                        startIcon={<DownloadIcon />}
                        onClick={descargarPDF}
                        sx={{
                            background: 'linear-gradient(135deg, #1a237e 0%, #3949ab 100%)',
                            '&:hover': {
                                background: 'linear-gradient(135deg, #0d1452 0%, #283593 100%)',
                            }
                        }}
                    >
                        Descargar PDF
                    </Button>
                </Box>
            </DialogActions>
        </Dialog>
    );
};

export default ReporteSimulacionModal;
