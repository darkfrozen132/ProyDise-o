import React, { useEffect, useMemo, useState } from 'react';
import './Pedidos.css';
import PedidoDiarioService from '../../services/PedidoDiarioService';
import Alert from '@mui/material/Alert';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Button from '@mui/material/Button';
import Tooltip from '@mui/material/Tooltip';

const PRODUCTOS = [
  { id: 'ELEC001', name: 'Laptop', categoria: 'Electrónica', peso: 2.5 },
  { id: 'ELEC002', name: 'Smartphone', categoria: 'Electrónica', peso: 0.3 },
  { id: 'ELEC003', name: 'Tablet', categoria: 'Electrónica', peso: 0.5 },
  { id: 'ROPA001', name: 'Camiseta', categoria: 'Ropa', peso: 0.2 },
  { id: 'ROPA002', name: 'Pantalón', categoria: 'Ropa', peso: 0.4 },
  { id: 'ALIM001', name: 'Conservas', categoria: 'Alimentos', peso: 1.0 },
  { id: 'ALIM002', name: 'Café', categoria: 'Alimentos', peso: 0.5 },
  { id: 'DOC001', name: 'Documentos', categoria: 'Documentos', peso: 0.1 },
  { id: 'MED001', name: 'Medicamentos', categoria: 'Medicina', peso: 0.3 },
  { id: 'OTR001', name: 'Otros', categoria: 'Otros', peso: 1.0 },
];

const AIRPORTS = [
  // América del Sur
  { code: 'SKBO', name: 'Bogotá (Colombia) - SKBO' },
  { code: 'SEQM', name: 'Quito (Ecuador) - SEQM' },
  { code: 'SVMI', name: 'Caracas (Venezuela) - SVMI' },
  { code: 'SBBR', name: 'Brasilia (Brasil) - SBBR' },
  // { code: 'SPIM', name: 'Lima (Perú) - SPIM' }, // Sede - No seleccionable
  { code: 'SLLP', name: 'La Paz (Bolivia) - SLLP' },
  { code: 'SCEL', name: 'Santiago de Chile (Chile) - SCEL' },
  { code: 'SABE', name: 'Buenos Aires (Argentina) - SABE' },
  { code: 'SGAS', name: 'Asunción (Paraguay) - SGAS' },
  { code: 'SUAA', name: 'Montevideo (Uruguay) - SUAA' },
  // Europa
  { code: 'LATI', name: 'Tirana (Albania) - LATI' },
  { code: 'EDDI', name: 'Berlín (Alemania) - EDDI' },
  { code: 'LOWW', name: 'Viena (Austria) - LOWW' },
  // { code: 'EBCI', name: 'Bruselas (Bélgica) - EBCI' }, // Sede - No seleccionable
  { code: 'UMMS', name: 'Minsk (Bielorrusia) - UMMS' },
  { code: 'LBSF', name: 'Sofía (Bulgaria) - LBSF' },
  { code: 'LKPR', name: 'Praga (Chequia) - LKPR' },
  { code: 'LDZA', name: 'Zagreb (Croacia) - LDZA' },
  { code: 'EKCH', name: 'Copenhague (Dinamarca) - EKCH' },
  { code: 'EHAM', name: 'Ámsterdam (Países Bajos) - EHAM' },
  // Asia
  { code: 'VIDP', name: 'Delhi (India) - VIDP' },
  { code: 'OSDI', name: 'Damasco (Siria) - OSDI' },
  { code: 'OERK', name: 'Riad (Arabia Saudita) - OERK' },
  { code: 'OMDB', name: 'Dubái (EAU) - OMDB' },
  { code: 'OAKB', name: 'Kabul (Afganistán) - OAKB' },
  { code: 'OOMS', name: 'Mascate (Omán) - OOMS' },
  { code: 'OYSN', name: 'Saná (Yemen) - OYSN' },
  { code: 'OPKC', name: 'Karachi (Pakistán) - OPKC' },
  // { code: 'UBBB', name: 'Bakú (Azerbaiyán) - UBBB' }, // Sede - No seleccionable
  { code: 'OJAI', name: 'Amán (Jordania) - OJAI' },
];

const initialForm = {
  cliente: '',
  email: '',
  telefono: '',
  origen: 'SPIM',
  destino: 'SKBO',
  productos: [], // Array de productos con sus cantidades
  prioridad: 'Normal',
  notas: ''
};

const initialProducto = {
  id: 'ELEC001',
  cantidad: 1
};

const Pedidos = () => {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [filter, setFilter] = useState('');
  const [currentProduct, setCurrentProduct] = useState(initialProducto);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showOrderDetail, setShowOrderDetail] = useState(false);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // Estado para alertas
  const [alert, setAlert] = useState({ show: false, severity: 'success', message: '' });
  
  // Estado para paginación
  const [currentPage, setCurrentPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  
  // Estado para modal de confirmación de eliminación
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Función para mostrar alerta
  const showAlert = (severity, message) => {
    setAlert({ show: true, severity, message });
    // Auto-ocultar después de 5 segundos
    setTimeout(() => {
      setAlert(prev => ({ ...prev, show: false }));
    }, 5000);
  };

  // Cargar pedidos al iniciar
  useEffect(() => {
    cargarPedidos();
  }, []);

  const cargarPedidos = async () => {
    setLoading(true);
    try {
      const pedidos = await PedidoDiarioService.obtenerTodos();
      // Mapear los pedidos para agregar status si no existe
      const pedidosConStatus = pedidos.map(p => ({
        ...p,
        status: p.status || 'PLANIFICADO'
      }));
      setOrders(pedidosConStatus);
      console.log('Pedidos cargados:', pedidosConStatus.length);
    } catch (error) {
      console.error('Error al cargar pedidos:', error);
    } finally {
      setLoading(false);
    }
  };

  // Filtrar pedidos según búsqueda
  const totalFiltrado = useMemo(() => {
    if (!filter.trim()) return orders;
    const f = filter.toLowerCase();
    return orders.filter(o =>
      String(o.id || '').toLowerCase().includes(f) ||
      String(o.clienteId || '').toLowerCase().includes(f) ||
      String(o.aeropuertoDestinoId || '').toLowerCase().includes(f) ||
      String(o.status || '').toLowerCase().includes(f)
    );
  }, [orders, filter]);

  // Calcular datos paginados
  const totalPages = Math.ceil(totalFiltrado.length / rowsPerPage);
  const paginatedData = useMemo(() => {
    const start = (currentPage - 1) * rowsPerPage;
    const end = start + rowsPerPage;
    return totalFiltrado.slice(start, end);
  }, [totalFiltrado, currentPage, rowsPerPage]);

  // Reset página cuando cambia el filtro
  useEffect(() => {
    setCurrentPage(1);
  }, [filter]);

  // Función para eliminar todos los pedidos
  const handleDeleteAllPedidos = async () => {
    setDeleting(true);
    try {
      await PedidoDiarioService.limpiarPedidos();
      setOrders([]);
      setShowDeleteModal(false);
      showAlert('success', '¡Todos los pedidos han sido eliminados exitosamente!');
    } catch (error) {
      console.error('Error al eliminar pedidos:', error);
      showAlert('error', 'Error al eliminar los pedidos. Por favor, intente nuevamente.');
    } finally {
      setDeleting(false);
    }
  };

  const resetForm = () => { 
    setForm(initialForm); 
    setCurrentProduct(initialProducto);
    setErrors({}); 
  };

  const addProduct = () => {
    if (currentProduct.cantidad >= 1) {
      const existingIndex = form.productos.findIndex(p => p.id === currentProduct.id);
      if (existingIndex >= 0) {
        // Si el producto ya existe, actualizar la cantidad
        const updatedProducts = [...form.productos];
        updatedProducts[existingIndex].cantidad += parseInt(currentProduct.cantidad);
        setForm(prev => ({ ...prev, productos: updatedProducts }));
      } else {
        // Agregar nuevo producto
        setForm(prev => ({ 
          ...prev, 
          productos: [...prev.productos, { ...currentProduct, cantidad: parseInt(currentProduct.cantidad) }]
        }));
      }
      setCurrentProduct(initialProducto);
    }
  };

  const removeProduct = (productId) => {
    setForm(prev => ({
      ...prev,
      productos: prev.productos.filter(p => p.id !== productId)
    }));
  };

  const updateProductQuantity = (productId, newQuantity) => {
    if (newQuantity >= 1) {
      setForm(prev => ({
        ...prev,
        productos: prev.productos.map(p => 
          p.id === productId ? { ...p, cantidad: parseInt(newQuantity) } : p
        )
      }));
    }
  };

  const generateId = () => {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    const n = String(Math.floor(Math.random() * 10000)).padStart(4, '0');
    return `ORD-${y}${m}${d}-${n}`;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const exportOrders = () => {
    const dataStr = JSON.stringify(orders, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'pedidos_morapack.json';
    link.click();
    URL.revokeObjectURL(url);
  };

  // Estado para importación
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);

  /**
   * Importar archivo CSV o TXT con pedidos
   * Formato: id-fechaUTC-hh-mm-codedestino-cantidadSolicitada-id_cliente
   * Ejemplo: 100000021-202512##-hh-mm-SVMI-990-0007729
   * 
   * Solo se usa: codedestino, cantidadSolicitada, id_cliente
   * La fecha es la actual UTC al momento de subir
   */
  const handleImportFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validar extensión
    const extension = file.name.split('.').pop().toLowerCase();
    if (!['csv', 'txt'].includes(extension)) {
      alert('❌ Solo se permiten archivos CSV o TXT');
      e.target.value = '';
      return;
    }

    setImporting(true);
    setImportResult(null);

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const content = event.target.result;
        const lines = content.split('\n').filter(line => line.trim() !== '');
        
        // Obtener fecha actual UTC
        const now = new Date();
        const utcDia = now.getUTCDate();
        const utcMes = now.getUTCMonth() + 1;
        const utcAnio = now.getUTCFullYear();
        const utcHora = now.getUTCHours();
        const utcMinuto = now.getUTCMinutes();

        let exitosos = 0;
        let fallidos = 0;
        const errores = [];

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim();
          if (!line) continue;

          try {
            // Parsear línea: id-fechaUTC-hh-mm-codedestino-cantidadSolicitada-id_cliente
            const parts = line.split('-');
            
            if (parts.length < 7) {
              throw new Error(`Formato inválido. Se esperan 7 campos separados por '-'`);
            }

            // Extraer campos (ignorando id, fecha, hh, mm del archivo)
            // parts[0] = id (ignorado - se genera automáticamente en BD)
            // parts[1] = fechaUTC (ignorado - usamos fecha actual)
            // parts[2] = hh (ignorado)
            // parts[3] = mm (ignorado)
            // parts[4] = codedestino
            // parts[5] = cantidadSolicitada
            // parts[6] = id_cliente
            const codigoDestino = parts[4];
            const cantidadSolicitada = parseInt(parts[5]);
            const idCliente = parts[6];

            // Validaciones
            if (!codigoDestino || codigoDestino.length !== 4) {
              throw new Error(`Código destino inválido: ${codigoDestino}`);
            }
            if (isNaN(cantidadSolicitada) || cantidadSolicitada <= 0) {
              throw new Error(`Cantidad inválida: ${parts[5]}`);
            }
            if (!idCliente || idCliente.trim() === '') {
              throw new Error(`ID cliente vacío`);
            }

            // Crear pedido (ID se genera automáticamente en la BD)
            const pedido = {
              clienteId: idCliente.trim(),
              aeropuertoDestinoId: codigoDestino.toUpperCase(),
              cantidadProductos: cantidadSolicitada,
              dia: utcDia,
              mes: utcMes,
              anio: utcAnio,
              hora: utcHora,
              minuto: utcMinuto
            };

            // Enviar al backend
            await PedidoDiarioService.crearPedido(pedido);
            exitosos++;

          } catch (lineError) {
            fallidos++;
            errores.push(`Línea ${i + 1}: ${lineError.message}`);
            console.error(`Error en línea ${i + 1}:`, lineError);
          }
        }

        // Mostrar resultado
        setImportResult({
          total: lines.length,
          exitosos,
          fallidos,
          errores: errores.slice(0, 5) // Mostrar solo los primeros 5 errores
        });

        // Recargar la lista de pedidos
        await cargarPedidos();

        if (exitosos > 0 && fallidos === 0) {
          showAlert('success', `!Importación completada! Se agregaron ${exitosos} pedidos exitosamente.`);
        } else if (exitosos > 0 && fallidos > 0) {
          showAlert('warning', `Importación parcial: ${exitosos} pedidos agregados, ${fallidos} errores.`);
        } else {
          showAlert('error', `No se pudo importar ningún pedido. Se encontraron ${fallidos} errores.`);
        }

      } catch (error) {
        console.error('Error al procesar archivo:', error);
        showAlert('error', `Error al procesar el archivo: ${error.message}`);
      } finally {
        setImporting(false);
        e.target.value = ''; // Limpiar input para permitir reimportar mismo archivo
      }
    };

    reader.onerror = () => {
      showAlert('error', 'Error al leer el archivo');
      setImporting(false);
      e.target.value = '';
    };

    reader.readAsText(file);
  };

  const validate = () => {
    const v = {};
    if (!form.cliente.trim()) v.cliente = 'DNI del cliente es requerido';
    if (!form.destino) v.destino = 'Destino es requerido';
    if (currentProduct.cantidad < 1) v.cantidad = 'Cantidad debe ser al menos 1';
    setErrors(v);
    return Object.keys(v).length === 0;
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    // Obtener fecha actual en UTC
    const now = new Date();
    const utcDia = now.getUTCDate();
    const utcMes = now.getUTCMonth() + 1; // getUTCMonth() es 0-indexed
    const utcAnio = now.getUTCFullYear();
    const utcHora = now.getUTCHours();
    const utcMinuto = now.getUTCMinutes();

    // Crear el nuevo pedido con formato del backend
    const newOrder = {
      clienteId: form.cliente,
      aeropuertoDestinoId: form.destino,
      cantidadProductos: parseInt(currentProduct.cantidad),
      dia: utcDia,
      mes: utcMes,
      anio: utcAnio,
      hora: utcHora,
      minuto: utcMinuto
    };

    // Enviar al backend
    console.log('📤 Enviando pedido al backend:', JSON.stringify(newOrder, null, 2));
    try {
      const response = await PedidoDiarioService.crearPedido(newOrder);
      console.log('✅ Pedido creado:', response);
      
      // Agregar a la lista local con el ID del backend
      const pedidoCreado = {
        id: response.pedido?.id,
        clienteId: newOrder.clienteId,
        aeropuertoDestinoId: newOrder.aeropuertoDestinoId,
        cantidadProductos: newOrder.cantidadProductos,
        dia: utcDia,
        mes: utcMes,
        anio: utcAnio,
        hora: utcHora,
        minuto: utcMinuto,
        status: 'PLANIFICADO'
      };
      setOrders(prev => [...prev, pedidoCreado]);
      resetForm();
      showAlert('success', 'Pedido creado exitosamente');
    } catch (error) {
      console.error('Error al crear pedido:', error);
      showAlert('error', 'Error al crear el pedido. Por favor, intente nuevamente.');
    }
  };

  const showOrderDetails = (order) => {
    setSelectedOrder(order);
    setShowOrderDetail(true);
  };

  const closeOrderDetail = () => {
    setSelectedOrder(null);
    setShowOrderDetail(false);
  };

  return (
    <div className="pedidos-page">
      {/* Alerta global */}
      {alert.show && (
        <div className="alert-container">
          <Alert 
            severity={alert.severity} 
            onClose={() => setAlert(prev => ({ ...prev, show: false }))}
          >
            {alert.message}
          </Alert>
        </div>
      )}

      <div className="pedidos-layout">
        <section className="form-section">
          <div className="form-header">
            <h2><i className="fas fa-clipboard-list"></i> Ingresar Pedido</h2>
            <div className="import-actions">
              <label className={`btn secondary ${importing ? 'disabled' : ''}`}>
                <i className={`fas ${importing ? 'fa-spinner fa-spin' : 'fa-file-import'}`}></i>
                {importing ? ' Importando...' : ' Importar'}
                <input 
                  type="file" 
                  accept=".csv,.txt" 
                  onChange={handleImportFile}
                  disabled={importing}
                  style={{ display: 'none' }}
                />
              </label>
            </div>
          </div>
          
          <form className="pedido-form" onSubmit={submit}>
            <div className="grid two">
              <div className="field">
                <label>
                  Id Cliente *
                  <Tooltip title="Ingrese solo 7 dígitos numéricos" arrow placement="top">
                    <i className="fas fa-info-circle info-icon"></i>
                  </Tooltip>
                </label>
                <input 
                  name="cliente" 
                  value={form.cliente} 
                  onChange={handleChange} 
                  placeholder="Ej: 1234567" 
                  maxLength={7}
                />
                {errors.cliente && <small className="error">{errors.cliente}</small>}
              </div>
            </div>

            <div className="grid three">
              <div className="field">
                <label>Destino *</label>
                <select name="destino" value={form.destino} onChange={handleChange}>
                  {AIRPORTS.map(a => <option key={a.code} value={a.code}>{a.name}</option>)}
                </select>
                {errors.destino && <small className="error">{errors.destino}</small>}
              </div>
            </div>

            <div className="grid three">
              <div className="field">
                <label>Cantidad *</label>
                <input 
                  type="number" 
                  min="1" 
                  value={currentProduct.cantidad} 
                  onChange={(e) => setCurrentProduct(prev => ({ ...prev, cantidad: e.target.value }))}
                />
              </div>
            </div>

            {/* Lista de productos agregados */}
            {form.productos.length > 0 && (
              <div className="productos-agregados">
                <h4>Productos en el pedido:</h4>
                <div className="productos-list">
                  {form.productos.map((p, index) => {
                    const producto = PRODUCTOS.find(prod => prod.id === p.id);
                    return (
                      <div key={index} className="producto-item">
                        <div className="producto-info">
                          <span className="producto-nombre">{producto.name}</span>
                          <span className="producto-detalles">
                            Cantidad: 
                            <input 
                              type="number" 
                              min="1" 
                              value={p.cantidad}
                              onChange={(e) => updateProductQuantity(p.id, e.target.value)}
                              className="cantidad-input"
                            />
                            - Peso: {(producto.peso * p.cantidad).toFixed(2)}kg
                          </span>
                        </div>
                        <button 
                          type="button" 
                          className="btn-remove" 
                          onClick={() => removeProduct(p.id)}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      </div>
                    );
                  })}
                </div>
                <div className="productos-resumen">
                  <strong>
                    Total: {form.productos.reduce((sum, p) => sum + p.cantidad, 0)} productos - 
                    {form.productos.reduce((sum, p) => {
                      const producto = PRODUCTOS.find(prod => prod.id === p.id);
                      return sum + (producto.peso * p.cantidad);
                    }, 0).toFixed(2)}kg
                  </strong>
                </div>
                {errors.productos && <small className="error">{errors.productos}</small>}
              </div>
            )}

            <div className="actions">
              <button type="submit" className="btn primary"><i className="fas fa-check"></i> Registrar</button>
              <button type="button" className="btn" onClick={resetForm}><i className="fas fa-eraser"></i> Limpiar</button>
            </div>
          </form>
        </section>

        <section className="list-section">
          <div className="list-header">
            <h2><i className="fas fa-boxes"></i> Pedidos ({totalFiltrado.length})</h2>
            <div className="list-actions">
              <input className="search" placeholder="Buscar por ID, cliente, destino..." value={filter} onChange={e=>setFilter(e.target.value)} />
              <button className="btn" onClick={cargarPedidos} disabled={loading} title="Recargar">
                <i className={`fas fa-sync-alt ${loading ? 'fa-spin' : ''}`}></i>
              </button>
              <button 
                className="btn danger" 
                onClick={() => setShowDeleteModal(true)} 
                disabled={orders.length === 0}
                title="Eliminar todos los pedidos"
              >
                <i className="fas fa-trash-alt"></i>
              </button>
            </div>
          </div>
          <div className="orders-table datatable">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Cliente (DNI)</th>
                  <th>Destino</th>
                  <th>Cantidad</th>
                  <th>Fecha (UTC)</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan="6" className="empty">
                      <i className="fas fa-spinner fa-spin"></i> Cargando pedidos...
                    </td>
                  </tr>
                ) : paginatedData.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty">Sin pedidos registrados</td>
                  </tr>
                ) : (
                  paginatedData.map(o => {
                    const status = o.status || 'Pendiente';
                    // Formatear fecha
                    const fecha = o.fechaUTC || `${o.dia || '-'}/${o.mes || '-'}/${o.anio || '-'} ${o.hora || '00'}:${String(o.minuto || '00').padStart(2, '0')}`;
                    return (
                      <tr key={o.id} className="clickable-row" onClick={() => showOrderDetails(o)}>
                        <td className="mono">{o.id}</td>
                        <td>{o.clienteId || '-'}</td>
                        <td>{o.aeropuertoDestinoId || '-'}</td>
                        <td className="center">{o.cantidadProductos || 0}</td>
                        <td>{fecha}</td>
                        <td>
                          <span className={`badge status-badge ${status.toLowerCase().replace(' ', '-')}`}>
                            {status}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          
          {/* Controles de paginación */}
          {totalFiltrado.length > 0 && (
            <div className="pagination-controls">
              <div className="pagination-info">
                Mostrando {((currentPage - 1) * rowsPerPage) + 1} - {Math.min(currentPage * rowsPerPage, totalFiltrado.length)} de {totalFiltrado.length}
              </div>
              <div className="pagination-actions">
                <select 
                  value={rowsPerPage} 
                  onChange={(e) => {
                    setRowsPerPage(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                  className="rows-select"
                >
                  <option value={5}>5 por página</option>
                  <option value={10}>10 por página</option>
                  <option value={25}>25 por página</option>
                  <option value={50}>50 por página</option>
                </select>
                <div className="pagination-buttons">
                  <button 
                    className="btn pagination-btn" 
                    onClick={() => setCurrentPage(1)}
                    disabled={currentPage === 1}
                  >
                    <i className="fas fa-angle-double-left"></i>
                  </button>
                  <button 
                    className="btn pagination-btn" 
                    onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                    disabled={currentPage === 1}
                  >
                    <i className="fas fa-angle-left"></i>
                  </button>
                  <span className="page-indicator">
                    Página {currentPage} de {totalPages || 1}
                  </span>
                  <button 
                    className="btn pagination-btn" 
                    onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                    disabled={currentPage === totalPages || totalPages === 0}
                  >
                    <i className="fas fa-angle-right"></i>
                  </button>
                  <button 
                    className="btn pagination-btn" 
                    onClick={() => setCurrentPage(totalPages)}
                    disabled={currentPage === totalPages || totalPages === 0}
                  >
                    <i className="fas fa-angle-double-right"></i>
                  </button>
                </div>
              </div>
            </div>
          )}
        </section>
      </div>

      {/* Modal de confirmación para eliminar todos los pedidos */}
      <Dialog
        open={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">
          <i className="fas fa-exclamation-triangle" style={{ color: '#f59e0b', marginRight: '8px' }}></i>
          Confirmar eliminación
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            ¿Está seguro de que desea eliminar <strong>todos los pedidos</strong>? 
            Esta acción no se puede deshacer y se eliminarán {orders.length} pedido(s) del sistema.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowDeleteModal(false)} disabled={deleting}>
            Cancelar
          </Button>
          <Button 
            onClick={handleDeleteAllPedidos} 
            color="error" 
            variant="contained"
            disabled={deleting}
          >
            {deleting ? 'Eliminando...' : 'Sí, eliminar todos'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Modal de detalles del pedido */}
      {showOrderDetail && selectedOrder && (
        <div className="modal-overlay" onClick={closeOrderDetail}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Detalles del Pedido #{selectedOrder.id}</h3>
              <button className="modal-close" onClick={closeOrderDetail}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            
            <div className="modal-body">
              <div className="order-info">
                <div className="info-grid">
                  <div className="info-item">
                    <label>ID Pedido:</label>
                    <span className="mono">{selectedOrder.id}</span>
                  </div>
                  <div className="info-item">
                    <label>Cliente (DNI):</label>
                    <span>{selectedOrder.clienteId || '-'}</span>
                  </div>
                  <div className="info-item">
                    <label>Aeropuerto Destino:</label>
                    <span>{selectedOrder.aeropuertoDestinoId || '-'}</span>
                  </div>
                  <div className="info-item">
                    <label>Cantidad Productos:</label>
                    <span>{selectedOrder.cantidadProductos || 0}</span>
                  </div>
                  <div className="info-item">
                    <label>Fecha (UTC):</label>
                    <span>
                      {selectedOrder.fechaUTC || 
                       `${selectedOrder.dia || '-'}/${selectedOrder.mes || '-'}/${selectedOrder.anio || '-'} ${selectedOrder.hora || '00'}:${String(selectedOrder.minuto || '00').padStart(2, '0')}`}
                    </span>
                  </div>
                  <div className="info-item">
                    <label>Estado:</label>
                    <span className={`badge status-badge ${(selectedOrder.status || 'pendiente').toLowerCase().replace(' ', '-')}`}>
                      {selectedOrder.status || 'Pendiente'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Pedidos;
