import React, { useState } from 'react';
import './Clientes.css';

const Clientes = () => {
  const [clients, setClients] = useState([
    {
      id: 1,
      nombre: 'Empresa Logística Global S.A.',
      contacto: 'Ana Martínez',
      email: 'ana.martinez@elglobal.com',
      telefono: '+51 999 123 456',
      pais: 'Perú',
      paquetesEnviados: 1247,
      estado: 'active'
    },
    {
      id: 2,
      nombre: 'European Trade Corp',
      contacto: 'Jean Dupont',
      email: 'j.dupont@europetrade.com',
      telefono: '+32 2 123 4567',
      pais: 'Bélgica',
      paquetesEnviados: 892,
      estado: 'active'
    },
    {
      id: 3,
      nombre: 'Asia Pacific Shipping',
      contacto: 'Kemal Aliyev',
      email: 'k.aliyev@apshipping.az',
      telefono: '+994 12 345 6789',
      pais: 'Azerbaiyán',
      paquetesEnviados: 634,
      estado: 'inactive'
    }
  ]);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedClient, setSelectedClient] = useState(null);
  const [newClient, setNewClient] = useState({
    nombre: '',
    contacto: '',
    email: '',
    telefono: '',
    pais: '',
    estado: 'active'
  });

  const filteredClients = clients.filter(client => {
    const matchesSearch = client.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         client.contacto.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         client.pais.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || client.estado === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const handleAddClient = () => {
    if (newClient.nombre && newClient.contacto && newClient.email) {
      setClients([...clients, {
        ...newClient,
        id: Date.now(),
        paquetesEnviados: 0
      }]);
      setNewClient({ nombre: '', contacto: '', email: '', telefono: '', pais: '', estado: 'active' });
      setShowAddModal(false);
    }
  };

  const handleDeleteClient = (id) => {
    if (window.confirm('¿Está seguro de eliminar este cliente?')) {
      setClients(clients.filter(client => client.id !== id));
    }
  };

  const handleViewDetails = (client) => {
    setSelectedClient(client);
    setShowDetailModal(true);
  };

  const getStatusBadge = (status) => {
    return status === 'active' ? 'Activo' : 'Inactivo';
  };

  return (
    <div className="clientes-container">
      <div className="control-panel">
        <h2>Gestión de Clientes</h2>
        
        <div className="client-actions">
          <button className="client-btn add-client" onClick={() => setShowAddModal(true)}>
            <i className="fas fa-plus"></i> Agregar Cliente
          </button>
          <div className="search-box">
            <input 
              type="text" 
              placeholder="Buscar por nombre, contacto o país..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <i className="fas fa-search"></i>
          </div>
        </div>

        <div className="client-filters">
          <label>Filtrar por estado:</label>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="all">Todos los estados</option>
            <option value="active">Activos</option>
            <option value="inactive">Inactivos</option>
          </select>
        </div>

        <div className="client-stats">
          <div className="stat-card">
            <div className="stat-number">{clients.filter(c => c.estado === 'active').length}</div>
            <div className="stat-label">Clientes Activos</div>
          </div>
          <div className="stat-card">
            <div className="stat-number">{clients.reduce((sum, c) => sum + c.paquetesEnviados, 0)}</div>
            <div className="stat-label">Paquetes Totales</div>
          </div>
          <div className="stat-card">
            <div className="stat-number">{new Set(clients.map(c => c.pais)).size}</div>
            <div className="stat-label">Países</div>
          </div>
        </div>

        <div className="client-table">
          <table>
            <thead>
              <tr>
                <th>Empresa</th>
                <th>Contacto</th>
                <th>País</th>
                <th>Paquetes Enviados</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filteredClients.map(client => (
                <tr key={client.id}>
                  <td>
                    <div className="company-info">
                      <strong>{client.nombre}</strong>
                      <small>{client.email}</small>
                    </div>
                  </td>
                  <td>
                    <div className="contact-info">
                      <strong>{client.contacto}</strong>
                      <small>{client.telefono}</small>
                    </div>
                  </td>
                  <td>{client.pais}</td>
                  <td>
                    <span className="package-count">{client.paquetesEnviados.toLocaleString()}</span>
                  </td>
                  <td>
                    <span className={`status-badge ${client.estado}`}>
                      {getStatusBadge(client.estado)}
                    </span>
                  </td>
                  <td>
                    <button className="action-btn view" onClick={() => handleViewDetails(client)}>
                      <i className="fas fa-eye"></i>
                    </button>
                    <button className="action-btn edit">
                      <i className="fas fa-edit"></i>
                    </button>
                    <button className="action-btn delete" onClick={() => handleDeleteClient(client.id)}>
                      <i className="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Sección de estadísticas de clientes para probar scroll */}
      <div className="clients-stats">
        <h2>
          <i className="fas fa-chart-pie"></i>
          Estadísticas de Clientes
        </h2>
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-users"></i>
            </div>
            <div className="stat-info">
              <h3>347</h3>
              <p>Clientes Activos</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-globe"></i>
            </div>
            <div className="stat-info">
              <h3>15</h3>
              <p>Países Cubiertos</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-handshake"></i>
            </div>
            <div className="stat-info">
              <h3>98.5%</h3>
              <p>Satisfacción Cliente</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-truck"></i>
            </div>
            <div className="stat-info">
              <h3>12,456</h3>
              <p>Envíos Este Mes</p>
            </div>
          </div>
        </div>
      </div>

      <div className="clients-distribution">
        <h2>
          <i className="fas fa-map-marked-alt"></i>
          Distribución Geográfica
        </h2>
        <div className="distribution-grid">
          <div className="distribution-item">
            <div className="country-flag">🇪🇸</div>
            <div className="country-info">
              <h4>España</h4>
              <p>142 clientes activos</p>
              <div className="progress-bar">
                <div className="progress-fill" style={{width: '85%'}}></div>
              </div>
            </div>
          </div>
          <div className="distribution-item">
            <div className="country-flag">🇲🇽</div>
            <div className="country-info">
              <h4>México</h4>
              <p>89 clientes activos</p>
              <div className="progress-bar">
                <div className="progress-fill" style={{width: '60%'}}></div>
              </div>
            </div>
          </div>
          <div className="distribution-item">
            <div className="country-flag">🇦🇷</div>
            <div className="country-info">
              <h4>Argentina</h4>
              <p>67 clientes activos</p>
              <div className="progress-bar">
                <div className="progress-fill" style={{width: '45%'}}></div>
              </div>
            </div>
          </div>
          <div className="distribution-item">
            <div className="country-flag">🇨🇴</div>
            <div className="country-info">
              <h4>Colombia</h4>
              <p>49 clientes activos</p>
              <div className="progress-bar">
                <div className="progress-fill" style={{width: '35%'}}></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="recent-clients">
        <h2>
          <i className="fas fa-clock"></i>
          Clientes Recientes
        </h2>
        <div className="recent-list">
          <div className="recent-item">
            <div className="client-avatar">
              <i className="fas fa-building"></i>
            </div>
            <div className="client-details">
              <h4>Tech Solutions SA</h4>
              <p>Madrid, España • Registrado hace 2 días</p>
              <span className="client-tag active">Activo</span>
            </div>
          </div>
          <div className="recent-item">
            <div className="client-avatar">
              <i className="fas fa-building"></i>
            </div>
            <div className="client-details">
              <h4>Logistics Corp</h4>
              <p>Guadalajara, México • Registrado hace 5 días</p>
              <span className="client-tag active">Activo</span>
            </div>
          </div>
          <div className="recent-item">
            <div className="client-avatar">
              <i className="fas fa-building"></i>
            </div>
            <div className="client-details">
              <h4>Global Shipping</h4>
              <p>Buenos Aires, Argentina • Registrado hace 1 semana</p>
              <span className="client-tag active">Activo</span>
            </div>
          </div>
        </div>
      </div>

      {/* Modal para agregar cliente */}
      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Agregar Nuevo Cliente</h3>
            <div className="form-group">
              <label>Nombre de la Empresa:</label>
              <input
                type="text"
                value={newClient.nombre}
                onChange={(e) => setNewClient({...newClient, nombre: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>Persona de Contacto:</label>
              <input
                type="text"
                value={newClient.contacto}
                onChange={(e) => setNewClient({...newClient, contacto: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>Email:</label>
              <input
                type="email"
                value={newClient.email}
                onChange={(e) => setNewClient({...newClient, email: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>Teléfono:</label>
              <input
                type="tel"
                value={newClient.telefono}
                onChange={(e) => setNewClient({...newClient, telefono: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>País:</label>
              <input
                type="text"
                value={newClient.pais}
                onChange={(e) => setNewClient({...newClient, pais: e.target.value})}
              />
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setShowAddModal(false)}>
                Cancelar
              </button>
              <button className="btn-save" onClick={handleAddClient}>
                Guardar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de detalles del cliente */}
      {showDetailModal && selectedClient && (
        <div className="modal-overlay">
          <div className="modal-content large">
            <h3>Detalles del Cliente</h3>
            <div className="client-details">
              <div className="detail-section">
                <h4>Información General</h4>
                <div className="detail-grid">
                  <div>
                    <label>Empresa:</label>
                    <span>{selectedClient.nombre}</span>
                  </div>
                  <div>
                    <label>Contacto:</label>
                    <span>{selectedClient.contacto}</span>
                  </div>
                  <div>
                    <label>Email:</label>
                    <span>{selectedClient.email}</span>
                  </div>
                  <div>
                    <label>Teléfono:</label>
                    <span>{selectedClient.telefono}</span>
                  </div>
                  <div>
                    <label>País:</label>
                    <span>{selectedClient.pais}</span>
                  </div>
                  <div>
                    <label>Estado:</label>
                    <span className={`status-badge ${selectedClient.estado}`}>
                      {getStatusBadge(selectedClient.estado)}
                    </span>
                  </div>
                </div>
              </div>
              
              <div className="detail-section">
                <h4>Estadísticas de Envío</h4>
                <div className="stats-grid">
                  <div className="stat-item">
                    <span className="stat-value">{selectedClient.paquetesEnviados}</span>
                    <span className="stat-label">Paquetes Enviados</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-value">89.5%</span>
                    <span className="stat-label">Tasa de Entrega</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-value">2.1</span>
                    <span className="stat-label">Días Promedio</span>
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setShowDetailModal(false)}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Clientes;
