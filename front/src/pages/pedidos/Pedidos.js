import React, { useEffect, useMemo, useState } from 'react';
import './Pedidos.css';
import PedidoDiarioService from '../../services/PedidoDiarioService';

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
  { code: 'SPIM', name: 'Lima (Perú) - SPIM' },
  { code: 'SLLP', name: 'La Paz (Bolivia) - SLLP' },
  { code: 'SCEL', name: 'Santiago de Chile (Chile) - SCEL' },
  { code: 'SABE', name: 'Buenos Aires (Argentina) - SABE' },
  { code: 'SGAS', name: 'Asunción (Paraguay) - SGAS' },
  { code: 'SUAA', name: 'Montevideo (Uruguay) - SUAA' },
  // Europa
  { code: 'LATI', name: 'Tirana (Albania) - LATI' },
  { code: 'EDDI', name: 'Berlín (Alemania) - EDDI' },
  { code: 'LOWW', name: 'Viena (Austria) - LOWW' },
  { code: 'EBCI', name: 'Bruselas (Bélgica) - EBCI' },
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
  { code: 'UBBB', name: 'Bakú (Azerbaiyán) - UBBB' },
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
        status: p.status || 'Pendiente'
      }));
      setOrders(pedidosConStatus);
      console.log('✅ Pedidos cargados:', pedidosConStatus.length);
    } catch (error) {
      console.error('❌ Error al cargar pedidos:', error);
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

  const handleImportFile = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const importedOrders = JSON.parse(event.target.result);
        if (Array.isArray(importedOrders)) {
          setOrders(prev => [...prev, ...importedOrders]);
        }
      } catch (error) {
        console.error('Error al importar pedidos:', error);
      }
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
        cliente: newOrder.clienteId,
        origen: 'SPIM', // Lima es el origen fijo
        destino: newOrder.aeropuertoDestinoId,
        cantidadTotal: newOrder.cantidadProductos,
        prioridad: 'Normal',
        status: 'Pendiente',
        fechaUTC: `${utcDia}/${utcMes}/${utcAnio} ${utcHora}:${String(utcMinuto).padStart(2, '0')} UTC`
      };
      setOrders(prev => [...prev, pedidoCreado]);
      resetForm();
      alert('✅ Pedido creado exitosamente');
    } catch (error) {
      console.error('❌ Error al crear pedido:', error);
      alert('Error al crear el pedido. Por favor, intente nuevamente.');
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
      <div className="pedidos-layout">
        <section className="form-section">
          <div className="form-header">
            <h2><i className="fas fa-clipboard-list"></i> Ingresar Pedido</h2>
          </div>
          
          <form className="pedido-form" onSubmit={submit}>
            <div className="grid two">
              <div className="field">
                <label>Cliente (DNI) *</label>
                <input name="cliente" value={form.cliente} onChange={handleChange} placeholder="Nombre o razón social" />
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
              <button type="submit" className="btn primary"><i className="fas fa-check"></i> Aceptar Pedido</button>
              <button type="button" className="btn" onClick={resetForm}><i className="fas fa-eraser"></i> Limpiar</button>
            </div>
          </form>
        </section>

        <section className="list-section">
          <div className="list-header">
            <h2><i className="fas fa-boxes"></i> Pedidos ({totalFiltrado.length})</h2>
            <div className="list-actions">
              <input className="search" placeholder="Buscar por ID, cliente, destino..." value={filter} onChange={e=>setFilter(e.target.value)} />
              <button className="btn" onClick={cargarPedidos} disabled={loading}>
                <i className={`fas fa-sync-alt ${loading ? 'fa-spin' : ''}`}></i>
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
                ) : totalFiltrado.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="empty">Sin pedidos registrados</td>
                  </tr>
                ) : (
                  totalFiltrado.map(o => {
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
        </section>
      </div>

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
