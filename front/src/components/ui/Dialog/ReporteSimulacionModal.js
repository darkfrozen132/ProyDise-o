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
    Grid,
    Collapse,
    List,
    ListItem,
    ListItemIcon,
    ListItemText
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import CloseIcon from '@mui/icons-material/Close';
import DownloadIcon from '@mui/icons-material/Download';
import FlightTakeoffIcon from '@mui/icons-material/FlightTakeoff';
import FlightLandIcon from '@mui/icons-material/FlightLand';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ScheduleIcon from '@mui/icons-material/Schedule';

/**
 * Modal de Reporte de Simulación - Formato Profesional
 */
const ReporteSimulacionModal = ({
    open,
    onClose,
    pedidos = [],
    vuelos = [],
    metricas = {},
    fechaInicio = '',
    fechaFin = '',
    tiempoRealTranscurrido = 0
}) => {
    const [tabActual, setTabActual] = useState(0);
    const [expandedRows, setExpandedRows] = useState({});

    const toggleRowExpanded = (pedidoId) => {
        setExpandedRows(prev => ({ ...prev, [pedidoId]: !prev[pedidoId] }));
    };

    const getVuelosDelPedido = (pedido) => {
        const idPedido = pedido.idPedido || pedido.id;
        if (!idPedido) return [];
        
        const vuelosDelPedido = vuelos.filter(v => {
            if (!v) return false;
            if (v.pedidos && Array.isArray(v.pedidos)) {
                return v.pedidos.some(p => 
                    String(p.idPedido || p.id) === String(idPedido) || 
                    Number(p.idPedido || p.id) === Number(idPedido)
                );
            }
            return String(v.pedidoId) === String(idPedido);
        });
        
        return vuelosDelPedido.sort((a, b) => {
            const fechaA = new Date(a.fechaInicial || a.departureTime || 0).getTime();
            const fechaB = new Date(b.fechaInicial || b.departureTime || 0).getTime();
            return fechaA - fechaB;
        });
    };

    const metricasPedidos = useMemo(() => {
        const entregados = pedidos.filter(p => p.status === 'Entregado' || p.estado === 'Entregado').length;
        const enTransito = pedidos.filter(p => 
            p.status === 'En vuelo' || p.estado === 'En vuelo' ||
            p.status === 'En escala' || p.estado === 'En escala'
        ).length;
        const pendientes = pedidos.filter(p => p.status === 'Planificado' || p.estado === 'Planificado' || !p.status).length;
        const total = pedidos.length;
        const porcentajeEntregados = total > 0 ? ((entregados / total) * 100).toFixed(1) : 0;
        return { total, entregados, enTransito, pendientes, porcentajeEntregados };
    }, [pedidos]);

    const metricasVuelos = useMemo(() => {
        const completados = vuelos.filter(v => v.status === 'completed' || v.progress >= 100).length;
        const enVuelo = vuelos.filter(v => v.status === 'active' && v.progress > 0 && v.progress < 100).length;
        const pendientes = vuelos.filter(v => v.status === 'waiting' || v.progress === 0).length;
        const total = vuelos.length;
        return { total, completados, enVuelo, pendientes };
    }, [vuelos]);

    const formatearTiempo = (segundos) => {
        const horas = Math.floor(segundos / 3600);
        const minutos = Math.floor((segundos % 3600) / 60);
        const segs = Math.floor(segundos % 60);
        if (horas > 0) return `${horas}h ${minutos}m ${segs}s`;
        if (minutos > 0) return `${minutos}m ${segs}s`;
        return `${segs}s`;
    };

    const formatearFechaCorta = (fecha) => {
        if (!fecha) return '-';
        try {
            return new Date(fecha).toLocaleString('es-PE', {
                day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
            });
        } catch { return '-'; }
    };

    const generarContenidoPDF = () => {
        const fechaGeneracion = new Date().toLocaleString('es-PE', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });
        
        return `<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Reporte de Simulación Logística</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Arial', sans-serif; padding: 30px 40px; color: #2c4a6b; line-height: 1.5; font-size: 11px; }
        .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 25px; padding-bottom: 15px; border-bottom: 2px solid #00d4ff; }
        .header-left h1 { color: #2c4a6b; font-size: 22px; font-weight: 700; margin-bottom: 3px; }
        .header-left .subtitle { color: #7f8c8d; font-size: 11px; }
        .header-right { text-align: right; font-size: 10px; color: #7f8c8d; }
        .header-right .company { font-size: 12px; font-weight: 600; color: #2c4a6b; }
        .info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 25px; }
        .info-box { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 6px; padding: 12px; text-align: center; }
        .info-box .label { font-size: 9px; color: #6c757d; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
        .info-box .value { font-size: 14px; font-weight: 600; color: #2c4a6b; }
        .info-box .value.success { color: #27ae60; }
        .info-box .value.warning { color: #f39c12; }
        .info-box .value.info { color: #0070f0ff; }
        .section { margin-bottom: 25px; }
        .section-title { font-size: 14px; font-weight: 600; color: #2c4a6b; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 1px solid #dee2e6; }
        table { width: 100%; border-collapse: collapse; font-size: 10px; }
        th { background: #2c4a6b; color: white; padding: 8px 6px; text-align: left; font-weight: 600; font-size: 9px; text-transform: uppercase; }
        td { padding: 7px 6px; border-bottom: 1px solid #e9ecef; vertical-align: top; }
        tr:nth-child(even) { background: #f8f9fa; }
        .status { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 9px; font-weight: 600; }
        .status-entregado { background: #d4edda; color: #155724; }
        .status-envuelo { background: #cce5ff; color: #004085; }
        .status-escala { background: #e2d5f1; color: #5a2d82; }
        .status-pendiente { background: #fff3cd; color: #856404; }
        .status-completado { background: #d4edda; color: #155724; }
        .pedido-card { border: 1px solid #dee2e6; border-radius: 6px; margin-bottom: 12px; overflow: hidden; }
        .pedido-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #f8f9fa; border-bottom: 1px solid #e9ecef; }
        .pedido-header .id { font-weight: 600; color: #2c4a6b; }
        .pedido-header .ruta { color: #6c757d; font-size: 10px; }
        .pedido-vuelos { padding: 10px 12px; }
        .vuelo-item { display: flex; align-items: center; padding: 6px 0; border-bottom: 1px dashed #e9ecef; font-size: 10px; }
        .vuelo-item:last-child { border-bottom: none; }
        .vuelo-num { width: 20px; height: 20px; background: #00d4ff; color: #2c4a6b; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 9px; font-weight: 600; margin-right: 10px; }
        .vuelo-ruta { flex: 1; font-weight: 500; }
        .vuelo-fechas { color: #6c757d; font-size: 9px; }
        .footer { margin-top: 30px; padding-top: 15px; border-top: 1px solid #dee2e6; display: flex; justify-content: space-between; font-size: 9px; color: #6c757d; }
        @media print { body { padding: 15px; } .pedido-card { break-inside: avoid; } }
    </style>
</head>
<body>
    <div class="header">
        <div class="header-left">
            <h1>Reporte de Simulación Logística</h1>
            <div class="subtitle">Resumen de operaciones y estado de envíos</div>
        </div>
        <div class="header-right">
            <div class="company">Sistema de Gestión Logística de Morapack</div>
            <div>Generado: ${fechaGeneracion}</div>
        </div>
    </div>
    <div class="info-grid">
        <div class="info-box"><div class="label">Fecha Inicio</div><div class="value">${fechaInicio || '-'}</div></div>
        <div class="info-box"><div class="label">Fecha Fin</div><div class="value">${fechaFin || '-'}</div></div>
        <div class="info-box"><div class="label">Duración Real</div><div class="value">${formatearTiempo(tiempoRealTranscurrido)}</div></div>
        <div class="info-box"><div class="label">Eficiencia</div><div class="value success">${metricasPedidos.porcentajeEntregados}%</div></div>
    </div>
    <div class="section">
        <div class="section-title">Resumen de Operaciones</div>
        <div class="info-grid">
            <div class="info-box"><div class="label">Total Pedidos</div><div class="value">${metricasPedidos.total}</div></div>
            <div class="info-box"><div class="label">Entregados</div><div class="value success">${metricasPedidos.entregados}</div></div>
            <div class="info-box"><div class="label">En Tránsito</div><div class="value info">${metricasPedidos.enTransito}</div></div>
            <div class="info-box"><div class="label">Pendientes</div><div class="value warning">${metricasPedidos.pendientes}</div></div>
        </div>
        <div class="info-grid">
            <div class="info-box"><div class="label">Total Vuelos</div><div class="value">${metricasVuelos.total}</div></div>
            <div class="info-box"><div class="label">Completados</div><div class="value success">${metricasVuelos.completados}</div></div>
            <div class="info-box"><div class="label">En Vuelo</div><div class="value info">${metricasVuelos.enVuelo}</div></div>
            <div class="info-box"><div class="label">Programados</div><div class="value warning">${metricasVuelos.pendientes}</div></div>
        </div>
    </div>
    <div class="section">
        <div class="section-title">Detalle de Pedidos</div>
        ${pedidos.slice(0, 50).map((p, idx) => {
            const status = p.status || p.estado || 'Pendiente';
            const statusClass = status === 'Entregado' ? 'status-entregado' : status === 'En vuelo' ? 'status-envuelo' : status === 'En escala' ? 'status-escala' : 'status-pendiente';
            const vuelosDelPedido = getVuelosDelPedido(p);
            return `<div class="pedido-card">
            <div class="pedido-header">
                <div><span class="id">Pedido #${p.idPedido || p.id || (idx + 1)}</span><span class="ruta"> | ${p.origin || p.origen || '-'} → ${p.destination || p.destino || '-'} | Cant: ${p.cantidad || 1}</span></div>
                <span class="status ${statusClass}">${status}</span>
            </div>
            ${vuelosDelPedido.length > 0 ? `<div class="pedido-vuelos">
                ${vuelosDelPedido.map((v, vIdx) => {
                    const vStatus = v.status === 'completed' ? 'Completado' : v.status === 'active' ? 'En vuelo' : 'Programado';
                    const vStatusClass = v.status === 'completed' ? 'status-completado' : v.status === 'active' ? 'status-envuelo' : 'status-pendiente';
                    return `<div class="vuelo-item">
                    <div class="vuelo-num">${vIdx + 1}</div>
                    <div class="vuelo-ruta">${v.origin?.code || '-'} → ${v.destination?.code || '-'}</div>
                    <div class="vuelo-fechas">Salida: ${formatearFechaCorta(v.fechaInicial || v.departureTime)} | Llegada: ${formatearFechaCorta(v.fechaFinal || v.arrivalTime)}</div>
                    <span class="status ${vStatusClass}" style="margin-left: 10px;">${vStatus}</span>
                </div>`;}).join('')}
            </div>` : `<div class="pedido-vuelos" style="color: #6c757d; font-style: italic;">Sin vuelos asignados</div>`}
        </div>`;}).join('')}
        ${pedidos.length > 50 ? `<p style="text-align: center; color: #6c757d; font-style: italic; margin-top: 15px;">... y ${pedidos.length - 50} pedidos adicionales</p>` : ''}
    </div>
    <div class="footer">
        <div>Documento generado automáticamente por el Sistema de Gestión Logística de Morapack</div>
        <div>Página 1 de 1</div>
    </div>
</body>
</html>`;
    };

    const descargarPDF = () => {
        const contenido = generarContenidoPDF();
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        document.body.appendChild(iframe);
        iframe.contentDocument.write(contenido);
        iframe.contentDocument.close();
        iframe.onload = () => {
            setTimeout(() => {
                iframe.contentWindow.print();
                setTimeout(() => document.body.removeChild(iframe), 1000);
            }, 500);
        };
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'Entregado': case 'completed': return 'success';
            case 'En vuelo': case 'active': return 'primary';
            case 'En escala': return 'secondary';
            default: return 'warning';
        }
    };

    const getStatusLabel = (status) => {
        switch (status) {
            case 'completed': return 'Completado';
            case 'active': return 'En vuelo';
            case 'waiting': return 'Programado';
            default: return status || 'Pendiente';
        }
    };

    const getStatusIcon = (status) => {
        switch (status) {
            case 'Entregado': case 'completed': return <CheckCircleIcon fontSize="small" />;
            case 'En vuelo': case 'active': return <FlightTakeoffIcon fontSize="small" />;
            case 'En escala': return <FlightLandIcon fontSize="small" />;
            default: return <ScheduleIcon fontSize="small" />;
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth PaperProps={{ sx: { borderRadius: 2, maxHeight: '90vh' } }}>
            <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: '#2c4a6b', color: 'white', py: 2, px: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <AssessmentIcon />
                    <Box>
                        <Typography variant="h6" fontWeight="600" sx={{ lineHeight: 1.2 }}>Reporte de Simulación</Typography>
                        <Typography variant="caption" sx={{ opacity: 0.8 }}>Resumen de operaciones logísticas</Typography>
                    </Box>
                </Box>
                <IconButton onClick={onClose} sx={{ color: 'white' }}><CloseIcon /></IconButton>
            </DialogTitle>

            <DialogContent sx={{ p: 0 }}>
                {/* Información General */}
                <Box sx={{ p: 3, bgcolor: '#f8f9fa', borderBottom: '1px solid #e9ecef' }}>
                    <Grid container spacing={2}>
                        <Grid item xs={6} sm={3}>
                            <Paper elevation={0} sx={{ p: 2, textAlign: 'center', bgcolor: 'white', border: '1px solid #e9ecef' }}>
                                <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontSize: 10 }}>Fecha Inicio</Typography>
                                <Typography variant="body1" fontWeight="600">{fechaInicio || '-'}</Typography>
                            </Paper>
                        </Grid>
                        <Grid item xs={6} sm={3}>
                            <Paper elevation={0} sx={{ p: 2, textAlign: 'center', bgcolor: 'white', border: '1px solid #e9ecef' }}>
                                <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontSize: 10 }}>Fecha Fin</Typography>
                                <Typography variant="body1" fontWeight="600">{fechaFin || '-'}</Typography>
                            </Paper>
                        </Grid>
                        <Grid item xs={6} sm={3}>
                            <Paper elevation={0} sx={{ p: 2, textAlign: 'center', bgcolor: 'white', border: '1px solid #e9ecef' }}>
                                <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontSize: 10 }}>Duración Real</Typography>
                                <Typography variant="body1" fontWeight="600" color="primary.main">{formatearTiempo(tiempoRealTranscurrido)}</Typography>
                            </Paper>
                        </Grid>
                        <Grid item xs={6} sm={3}>
                            <Paper elevation={0} sx={{ p: 2, textAlign: 'center', bgcolor: 'white', border: '1px solid #e9ecef' }}>
                                <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', fontSize: 10 }}>Eficiencia</Typography>
                                <Typography variant="body1" fontWeight="600" color="success.main">{metricasPedidos.porcentajeEntregados}%</Typography>
                            </Paper>
                        </Grid>
                    </Grid>
                </Box>

                {/* Métricas */}
                <Box sx={{ p: 3 }}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} md={6}>
                            <Card variant="outlined">
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                                        <LocalShippingIcon color="primary" />
                                        <Typography variant="subtitle1" fontWeight="600">Pedidos</Typography>
                                    </Box>
                                    <Box sx={{ mb: 2 }}>
                                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                            <Typography variant="body2" color="text.secondary">Progreso de entregas</Typography>
                                            <Typography variant="body2" fontWeight="600" color="success.main">{metricasPedidos.porcentajeEntregados}%</Typography>
                                        </Box>
                                        <LinearProgress variant="determinate" value={parseFloat(metricasPedidos.porcentajeEntregados)} sx={{ height: 8, borderRadius: 4 }} color="success" />
                                    </Box>
                                    <Grid container spacing={1} sx={{ textAlign: 'center' }}>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700">{metricasPedidos.total}</Typography><Typography variant="caption" color="text.secondary">Total</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="success.main">{metricasPedidos.entregados}</Typography><Typography variant="caption" color="text.secondary">Entregados</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="primary.main">{metricasPedidos.enTransito}</Typography><Typography variant="caption" color="text.secondary">En tránsito</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="warning.main">{metricasPedidos.pendientes}</Typography><Typography variant="caption" color="text.secondary">Pendientes</Typography></Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                        <Grid item xs={12} md={6}>
                            <Card variant="outlined">
                                <CardContent>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                                        <FlightTakeoffIcon color="primary" />
                                        <Typography variant="subtitle1" fontWeight="600">Vuelos</Typography>
                                    </Box>
                                    <Grid container spacing={1} sx={{ textAlign: 'center', mt: 2 }}>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700">{metricasVuelos.total}</Typography><Typography variant="caption" color="text.secondary">Total</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="success.main">{metricasVuelos.completados}</Typography><Typography variant="caption" color="text.secondary">Completados</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="primary.main">{metricasVuelos.enVuelo}</Typography><Typography variant="caption" color="text.secondary">En vuelo</Typography></Grid>
                                        <Grid item xs={3}><Typography variant="h5" fontWeight="700" color="warning.main">{metricasVuelos.pendientes}</Typography><Typography variant="caption" color="text.secondary">Programados</Typography></Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Grid>
                    </Grid>
                </Box>

                {/* Tabs */}
                <Box sx={{ borderBottom: 1, borderColor: 'divider', px: 3 }}>
                    <Tabs value={tabActual} onChange={(e, v) => setTabActual(v)}>
                        <Tab label={`Pedidos (${pedidos.length})`} />
                        <Tab label={`Vuelos (${vuelos.length})`} />
                    </Tabs>
                </Box>

                {/* Contenido de tabs */}
                <Box sx={{ p: 2, maxHeight: 400, overflow: 'auto' }}>
                    {tabActual === 0 && (
                        <TableContainer component={Paper} variant="outlined">
                            <Table size="small" stickyHeader>
                                <TableHead>
                                    <TableRow sx={{ '& th': { bgcolor: '#2c4a6b', color: 'white', fontWeight: 600, fontSize: 11 } }}>
                                        <TableCell sx={{ width: 50 }}></TableCell>
                                        <TableCell>N°</TableCell>
                                        <TableCell>ID Pedido</TableCell>
                                        <TableCell>Origen</TableCell>
                                        <TableCell>Destino</TableCell>
                                        <TableCell>Cantidad</TableCell>
                                        <TableCell>Escalas</TableCell>
                                        <TableCell>Estado</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {pedidos.length === 0 ? (
                                        <TableRow><TableCell colSpan={8} align="center"><Typography color="text.secondary" sx={{ py: 4 }}>No hay pedidos registrados</Typography></TableCell></TableRow>
                                    ) : (
                                        pedidos.map((pedido, idx) => {
                                            const pedidoId = pedido.idPedido || pedido.id || idx;
                                            const vuelosDelPedido = getVuelosDelPedido(pedido);
                                            const isExpanded = expandedRows[pedidoId];
                                            const tieneVuelos = vuelosDelPedido.length > 0;
                                            
                                            return (
                                                <React.Fragment key={pedidoId}>
                                                    <TableRow hover sx={{ '& > *': { borderBottom: 'unset' } }}>
                                                        <TableCell>
                                                            {tieneVuelos && (
                                                                <IconButton size="small" onClick={() => toggleRowExpanded(pedidoId)}>
                                                                    {isExpanded ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
                                                                </IconButton>
                                                            )}
                                                        </TableCell>
                                                        <TableCell>{idx + 1}</TableCell>
                                                        <TableCell><Typography variant="body2" fontWeight="600">{pedido.idPedido || pedido.id || '-'}</Typography></TableCell>
                                                        <TableCell>{pedido.origin || pedido.origen || '-'}</TableCell>
                                                        <TableCell>{pedido.destination || pedido.destino || '-'}</TableCell>
                                                        <TableCell>{pedido.cantidad || 1}</TableCell>
                                                        <TableCell><Chip size="small" label={vuelosDelPedido.length} variant="outlined" sx={{ minWidth: 32 }} /></TableCell>
                                                        <TableCell>
                                                            <Chip size="small" label={getStatusLabel(pedido.status || pedido.estado)} color={getStatusColor(pedido.status || pedido.estado)} icon={getStatusIcon(pedido.status || pedido.estado)} />
                                                        </TableCell>
                                                    </TableRow>
                                                    {tieneVuelos && (
                                                        <TableRow>
                                                            <TableCell colSpan={8} sx={{ py: 0, px: 0 }}>
                                                                <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                                                                    <Box sx={{ m: 2, ml: 7, bgcolor: '#f8f9fa', borderRadius: 1, p: 2, border: '1px solid #e9ecef' }}>
                                                                        <Typography variant="subtitle2" sx={{ mb: 1.5, fontWeight: 600, color: '#2c4a6b' }}>Itinerario de vuelos:</Typography>
                                                                        <List dense sx={{ py: 0 }}>
                                                                            {vuelosDelPedido.map((vuelo, vIdx) => (
                                                                                <ListItem key={vuelo.id || vIdx} sx={{ py: 0.5, px: 0 }}>
                                                                                    <ListItemIcon sx={{ minWidth: 32 }}>
                                                                                        <Box sx={{ width: 22, height: 22, bgcolor: '#00d4ff', color: '#2c4a6b', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 600 }}>{vIdx + 1}</Box>
                                                                                    </ListItemIcon>
                                                                                    <ListItemText
                                                                                        primary={
                                                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                                                                <Typography variant="body2" fontWeight="600">{vuelo.origin?.code || '-'}</Typography>
                                                                                                <Typography variant="body2" color="text.secondary">→</Typography>
                                                                                                <Typography variant="body2" fontWeight="600">{vuelo.destination?.code || '-'}</Typography>
                                                                                                <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>ID: {vuelo.id}</Typography>
                                                                                            </Box>
                                                                                        }
                                                                                        secondary={<Typography variant="caption" color="text.secondary">Salida: {formatearFechaCorta(vuelo.fechaInicial || vuelo.departureTime)} | Llegada: {formatearFechaCorta(vuelo.fechaFinal || vuelo.arrivalTime)}</Typography>}
                                                                                    />
                                                                                    <Chip size="small" label={getStatusLabel(vuelo.status)} color={getStatusColor(vuelo.status)} sx={{ ml: 1 }} />
                                                                                </ListItem>
                                                                            ))}
                                                                        </List>
                                                                    </Box>
                                                                </Collapse>
                                                            </TableCell>
                                                        </TableRow>
                                                    )}
                                                </React.Fragment>
                                            );
                                        })
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}

                    {tabActual === 1 && (
                        <TableContainer component={Paper} variant="outlined">
                            <Table size="small" stickyHeader>
                                <TableHead>
                                    <TableRow sx={{ '& th': { bgcolor: '#2c4a6b', color: 'white', fontWeight: 600, fontSize: 11 } }}>
                                        <TableCell>N°</TableCell>
                                        <TableCell>ID Vuelo</TableCell>
                                        <TableCell>Origen</TableCell>
                                        <TableCell>Destino</TableCell>
                                        <TableCell>Salida</TableCell>
                                        <TableCell>Llegada</TableCell>
                                        <TableCell>Estado</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {vuelos.length === 0 ? (
                                        <TableRow><TableCell colSpan={7} align="center"><Typography color="text.secondary" sx={{ py: 4 }}>No hay vuelos registrados</Typography></TableCell></TableRow>
                                    ) : (
                                        vuelos.map((vuelo, idx) => (
                                            <TableRow key={vuelo.id || idx} hover>
                                                <TableCell>{idx + 1}</TableCell>
                                                <TableCell><Typography variant="body2" fontWeight="600">{vuelo.id || '-'}</Typography></TableCell>
                                                <TableCell>{vuelo.origin?.code || '-'}</TableCell>
                                                <TableCell>{vuelo.destination?.code || '-'}</TableCell>
                                                <TableCell><Typography variant="caption">{formatearFechaCorta(vuelo.fechaInicial || vuelo.departureTime)}</Typography></TableCell>
                                                <TableCell><Typography variant="caption">{formatearFechaCorta(vuelo.fechaFinal || vuelo.arrivalTime)}</Typography></TableCell>
                                                <TableCell><Chip size="small" label={getStatusLabel(vuelo.status)} color={getStatusColor(vuelo.status)} /></TableCell>
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

            <DialogActions sx={{ p: 2, justifyContent: 'space-between', bgcolor: '#f8f9fa' }}>
                <Typography variant="caption" color="text.secondary">Generado: {new Date().toLocaleString('es-PE')}</Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button onClick={onClose} color="inherit" variant="outlined">Cerrar</Button>
                    <Button variant="contained" startIcon={<DownloadIcon />} onClick={descargarPDF} sx={{ bgcolor: '#2c4a6b', '&:hover': { bgcolor: '#1e3a57' } }}>Descargar PDF</Button>
                </Box>
            </DialogActions>
        </Dialog>
    );
};

export default ReporteSimulacionModal;
