import React, { useEffect, useMemo, useState } from 'react';
import './Pedidos.css';

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

const PRODUCTOS = [
  { id: 'ELEC001', name: 'Laptop Lenovo ThinkPad', peso: 2.5, categoria: 'Electrónica' },
  { id: 'ELEC002', name: 'iPhone 15 Pro', peso: 0.2, categoria: 'Electrónica' },
  { id: 'ELEC003', name: 'Monitor Samsung 27"', peso: 4.8, categoria: 'Electrónica' },
  { id: 'ROPA001', name: 'Camisa Polo Ralph Lauren', peso: 0.3, categoria: 'Ropa' },
  { id: 'ROPA002', name: 'Jeans Levi\'s 501', peso: 0.7, categoria: 'Ropa' },
  { id: 'LIBR001', name: 'Enciclopedia Britannica', peso: 3.2, categoria: 'Libros' },
  { id: 'DECO001', name: 'Jarrón de Cerámica', peso: 1.8, categoria: 'Decoración' },
  { id: 'DECO002', name: 'Cuadro Enmarcado', peso: 2.1, categoria: 'Decoración' },
  { id: 'MED001', name: 'Medicamentos Especiales', peso: 0.5, categoria: 'Medicinas' },
  { id: 'DOC001', name: 'Documentos Legales', peso: 0.1, categoria: 'Documentos' }
];

const PEDIDOS_EJEMPLO = [
  {
    id: 'ORD-20240915-0001',
    cliente: 'Juan Pérez',
    email: 'juan.perez@email.com',
    telefono: '+51 999 123 456',
    origen: 'SPIM',
    destino: 'SKBO',
    productos: [
      { id: 'ELEC001', nombre: 'Laptop Lenovo ThinkPad', cantidad: 1, pesoUnitario: 2.5, pesoTotal: 2.5 }
    ],
    pesoTotal: '2.50',
    cantidadTotal: 1,
    prioridad: 'Normal',
    status: 'Hecho',
    createdAt: '2024-09-15T08:30:00.000Z',
    notas: 'Entrega urgente'
  },
  {
    id: 'ORD-20240915-0002',
    cliente: 'María González',
    email: 'maria.gonzalez@empresa.com',
    telefono: '+57 300 456 789',
    origen: 'EBCI',
    destino: 'SPIM',
    productos: [
      { id: 'ROPA001', nombre: 'Camisa Polo Ralph Lauren', cantidad: 3, pesoUnitario: 0.3, pesoTotal: 0.9 },
      { id: 'ROPA002', nombre: 'Jeans Levi\'s 501', cantidad: 2, pesoUnitario: 0.7, pesoTotal: 1.4 }
    ],
    pesoTotal: '2.30',
    cantidadTotal: 5,
    prioridad: 'Urgente',
    status: 'En curso',
    createdAt: '2024-09-15T10:15:00.000Z',
    notas: ''
  },
  {
    id: 'ORD-20240915-0003',
    cliente: 'Carlos Rodriguez',
    email: 'carlos.r@gmail.com',
    telefono: '+34 666 789 012',
    origen: 'EDDI',
    destino: 'UBBB',
    productos: [
      { id: 'MED001', nombre: 'Medicamentos Especiales', cantidad: 2, pesoUnitario: 0.5, pesoTotal: 1.0 }
    ],
    pesoTotal: '1.00',
    cantidadTotal: 2,
    prioridad: 'Crítico',
    status: 'Cancelado',
    createdAt: '2024-09-15T14:22:00.000Z',
    notas: 'Producto descontinuado'
  },
  {
    id: 'ORD-20240915-0004',
    cliente: 'Ana Silva',
    email: 'ana.silva@tech.com',
    telefono: '+55 11 98765 4321',
    origen: 'SBBR',
    destino: 'EHAM',
    productos: [
      { id: 'ELEC003', nombre: 'Monitor Samsung 27"', cantidad: 1, pesoUnitario: 4.8, pesoTotal: 4.8 },
      { id: 'ELEC002', nombre: 'iPhone 15 Pro', cantidad: 1, pesoUnitario: 0.2, pesoTotal: 0.2 }
    ],
    pesoTotal: '5.00',
    cantidadTotal: 2,
    prioridad: 'Normal',
    status: 'En curso',
    createdAt: '2024-09-15T16:45:00.000Z',
    notas: 'Frágil - manejar con cuidado'
  }
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
  const [orders, setOrders] = useState(PEDIDOS_EJEMPLO);
  const [filter, setFilter] = useState('');
  const [currentProduct, setCurrentProduct] = useState(initialProducto);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showOrderDetail, setShowOrderDetail] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem('morapack_orders');
    if (saved) {
      setOrders(JSON.parse(saved));
    } else {
      // Si no hay datos guardados, usar los pedidos de ejemplo
      setOrders(PEDIDOS_EJEMPLO);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('morapack_orders', JSON.stringify(orders));
  }, [orders]);

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

  useEffect(() => {
    const saved = localStorage.getItem('morapack_orders');
    if (saved) {
      setOrders(JSON.parse(saved));
    } else {
      // Si no hay datos guardados, usar los pedidos de ejemplo
      setOrders(PEDIDOS_EJEMPLO);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('morapack_orders', JSON.stringify(orders));
  }, [orders]);

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

  const handleImportFile = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const importedOrders = JSON.parse(event.target.result);
          if (Array.isArray(importedOrders)) {
            setOrders(prev => [...importedOrders, ...prev]);
            alert(`Se importaron ${importedOrders.length} pedidos correctamente`);
          } else {
            alert('El formato del archivo no es válido');
          }
        } catch (error) {
          alert('Error al leer el archivo. Asegúrate de que sea un archivo JSON válido.');
        }
      };
      reader.readAsText(file);
    }
    e.target.value = ''; // Reset input
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

  const validate = () => {
    const v = {};
    if (!form.cliente.trim()) v.cliente = 'Requerido';
    if (!form.email.match(/^\S+@\S+\.\S+$/)) v.email = 'Email inválido';
    if (form.origen === form.destino) v.destino = 'Destino no puede ser igual a origen';
    if (form.productos.length === 0) v.productos = 'Debe agregar al menos un producto';
    setErrors(v);
    return Object.keys(v).length === 0;
  };

  const submit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    const id = generateId();
    
    // Calcular peso total y crear resumen de productos
    let pesoTotal = 0;
    const productosResumen = form.productos.map(p => {
      const producto = PRODUCTOS.find(prod => prod.id === p.id);
      pesoTotal += producto.peso * p.cantidad;
      return {
        ...p,
        nombre: producto.name,
        pesoUnitario: producto.peso,
        pesoTotal: producto.peso * p.cantidad
      };
    });

    const order = { 
      id, 
      status: 'Pendiente', 
      createdAt: new Date().toISOString(),
      productos: productosResumen,
      pesoTotal: pesoTotal.toFixed(2),
      cantidadTotal: form.productos.reduce((sum, p) => sum + p.cantidad, 0),
      ...form 
    };
    setOrders(prev => [order, ...prev]);
    resetForm();
    alert('Pedido creado exitosamente');
  };

  const showOrderDetails = (order) => {
    setSelectedOrder(order);
    setShowOrderDetail(true);
  };

  const closeOrderDetail = () => {
    setSelectedOrder(null);
    setShowOrderDetail(false);
  };

  const totalFiltrado = useMemo(() => orders.filter(o =>
    [o.id, o.cliente, o.email, o.origen, o.destino, o.status].join(' ').toLowerCase().includes(filter.toLowerCase())
  ), [orders, filter]);

  return (
    <div className="pedidos-page">
      <div className="pedidos-layout">
        <section className="form-section">
          <div className="form-header">
            <h2><i className="fas fa-clipboard-list"></i> Ingresar Pedido</h2>
            <div className="import-actions">
              <label className="btn secondary">
                <i className="fas fa-upload"></i> Importar Pedidos
                <input type="file" accept=".json" onChange={handleImportFile} style={{display: 'none'}} />
              </label>
              <button className="btn secondary" onClick={exportOrders}>
                <i className="fas fa-download"></i> Exportar Pedidos
              </button>
            </div>
          </div>
          
          <form className="pedido-form" onSubmit={submit}>
            <div className="grid two">
              <div className="field">
                <label>Cliente *</label>
                <input name="cliente" value={form.cliente} onChange={handleChange} placeholder="Nombre o razón social" />
                {errors.cliente && <small className="error">{errors.cliente}</small>}
              </div>
              <div className="field">
                <label>Email *</label>
                <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="cliente@dominio.com" />
                {errors.email && <small className="error">{errors.email}</small>}
              </div>
            </div>

            <div className="grid three">
              <div className="field">
                <label>Teléfono</label>
                <input name="telefono" value={form.telefono} onChange={handleChange} placeholder="+51 999 999 999" />
              </div>
              <div className="field">
                <label>Origen *</label>
                <select name="origen" value={form.origen} onChange={handleChange}>
                  {AIRPORTS.map(a => <option key={a.code} value={a.code}>{a.name}</option>)}
                </select>
              </div>
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
                <label>Producto *</label>
                <select 
                  value={currentProduct.id} 
                  onChange={(e) => setCurrentProduct(prev => ({ ...prev, id: e.target.value }))}
                >
                  {PRODUCTOS.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.categoria}) - {p.peso}kg
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Cantidad *</label>
                <input 
                  type="number" 
                  min="1" 
                  value={currentProduct.cantidad} 
                  onChange={(e) => setCurrentProduct(prev => ({ ...prev, cantidad: e.target.value }))}
                />
              </div>
              <div className="field">
                <button type="button" className="btn secondary add-product-btn" onClick={addProduct}>
                  <i className="fas fa-plus"></i> Agregar Producto
                </button>
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

            <div className="grid two">
              <div className="field">
                <label>Prioridad</label>
                <select name="prioridad" value={form.prioridad} onChange={handleChange}>
                  <option>Normal</option>
                  <option>Urgente</option>
                  <option>Crítico</option>
                </select>
              </div>
              <div></div>
            </div>

            <div className="field">
              <label>Notas</label>
              <textarea name="notas" rows="3" value={form.notas} onChange={handleChange} placeholder="Instrucciones adicionales" />
            </div>

            <div className="actions">
              <button type="submit" className="btn primary"><i className="fas fa-check"></i> Aceptar Pedido</button>
              <button type="button" className="btn" onClick={resetForm}><i className="fas fa-eraser"></i> Limpiar</button>
            </div>
          </form>
        </section>

        <section className="list-section">
          <div className="list-header">
            <h2><i className="fas fa-boxes"></i> Pedidos ({totalFiltrado.length})</h2>
            <input className="search" placeholder="Buscar por ID, cliente, email, estado…" value={filter} onChange={e=>setFilter(e.target.value)} />
          </div>
          <div className="orders-table">
            <div className="table-head">
              <div>ID</div>
              <div>Cliente</div>
              <div>Origen</div>
              <div>Destino</div>
              <div>Cantidad</div>
              <div>Prioridad</div>
              <div>Estado</div>
            </div>
            {totalFiltrado.length === 0 && <div className="empty">Sin pedidos</div>}
            {totalFiltrado.map(o => {
              return (
                <div className="table-row clickable-row" key={o.id} onClick={() => showOrderDetails(o)}>
                  <div className="mono">{o.id}</div>
                  <div>{o.cliente}</div>
                  <div>{o.origen}</div>
                  <div>{o.destino}</div>
                  <div>{o.cantidadTotal || o.cantidad || 0}</div>
                  <div><span className={`badge ${o.prioridad.toLowerCase()}`}>{o.prioridad}</span></div>
                  <div><span className={`badge status-badge ${o.status.toLowerCase().replace(' ', '-')}`}>{o.status}</span></div>
                </div>
              );
            })}
          </div>
        </section>
      </div>

      {/* Modal de detalles del pedido */}
      {showOrderDetail && selectedOrder && (
        <div className="modal-overlay" onClick={closeOrderDetail}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Detalles del Pedido - {selectedOrder.id}</h3>
              <button className="modal-close" onClick={closeOrderDetail}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            
            <div className="modal-body">
              <div className="order-info">
                <div className="info-grid">
                  <div className="info-item">
                    <label>Cliente:</label>
                    <span>{selectedOrder.cliente}</span>
                  </div>
                  <div className="info-item">
                    <label>Email:</label>
                    <span>{selectedOrder.email}</span>
                  </div>
                  <div className="info-item">
                    <label>Teléfono:</label>
                    <span>{selectedOrder.telefono || 'No especificado'}</span>
                  </div>
                  <div className="info-item">
                    <label>Origen:</label>
                    <span>{selectedOrder.origen}</span>
                  </div>
                  <div className="info-item">
                    <label>Destino:</label>
                    <span>{selectedOrder.destino}</span>
                  </div>
                  <div className="info-item">
                    <label>Prioridad:</label>
                    <span className={`badge ${selectedOrder.prioridad.toLowerCase()}`}>
                      {selectedOrder.prioridad}
                    </span>
                  </div>
                  <div className="info-item">
                    <label>Estado:</label>
                    <span className={`badge status-badge ${selectedOrder.status.toLowerCase().replace(' ', '-')}`}>
                      {selectedOrder.status}
                    </span>
                  </div>
                  <div className="info-item">
                    <label>Fecha:</label>
                    <span>{new Date(selectedOrder.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
                
                {selectedOrder.notas && (
                  <div className="info-item full-width">
                    <label>Notas:</label>
                    <span>{selectedOrder.notas}</span>
                  </div>
                )}
              </div>

              <div className="products-section">
                <h4>Productos del Pedido</h4>
                {selectedOrder.productos && selectedOrder.productos.length > 0 ? (
                  <div className="products-list">
                    {selectedOrder.productos.map((producto, index) => (
                      <div key={index} className="product-item">
                        <div className="product-name">{producto.nombre}</div>
                        <div className="product-details">
                          <span>Cantidad: {producto.cantidad}</span>
                          <span>Peso unitario: {producto.pesoUnitario}kg</span>
                          <span>Peso total: {producto.pesoTotal}kg</span>
                        </div>
                      </div>
                    ))}
                    <div className="products-summary">
                      <strong>
                        Total del pedido: {selectedOrder.cantidadTotal} productos - {selectedOrder.pesoTotal}kg
                      </strong>
                    </div>
                  </div>
                ) : (
                  <p>No hay productos en este pedido.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Pedidos;
