import React, { useState } from 'react';
import './Usuarios.css';

const Usuarios = () => {
  const [users, setUsers] = useState([
    {
      id: 1,
      nombre: 'Juan Pérez',
      correo: 'juan.perez@morapack.com',
      rol: 'admin',
      estado: 'active'
    },
    {
      id: 2,
      nombre: 'María García',
      correo: 'maria.garcia@morapack.com',
      rol: 'operator',
      estado: 'active'
    },
    {
      id: 3,
      nombre: 'Carlos López',
      correo: 'carlos.lopez@morapack.com',
      rol: 'operator',
      estado: 'inactive'
    }
  ]);

  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [newUser, setNewUser] = useState({
    nombre: '',
    correo: '',
    rol: 'operator',
    estado: 'active'
  });

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.correo.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = roleFilter === 'all' || user.rol === roleFilter;
    return matchesSearch && matchesRole;
  });

  const handleAddUser = () => {
    if (newUser.nombre && newUser.correo) {
      setUsers([...users, {
        ...newUser,
        id: Date.now()
      }]);
      setNewUser({ nombre: '', correo: '', rol: 'operator', estado: 'active' });
      setShowAddModal(false);
    }
  };

  const handleDeleteUser = (id) => {
    if (window.confirm('¿Está seguro de eliminar este usuario?')) {
      setUsers(users.filter(user => user.id !== id));
    }
  };

  return (
    <div className="usuarios-container">
      <div className="control-panel">
        <h2>Gestión de Usuarios</h2>
        
        <div className="user-actions">
          <button className="user-btn add-user" onClick={() => setShowAddModal(true)}>
            <i className="fas fa-user-plus"></i> Agregar Usuario
          </button>
          <div className="search-box">
            <input 
              type="text" 
              placeholder="Buscar por nombre, correo o rol..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <i className="fas fa-search"></i>
          </div>
        </div>

        <div className="user-filters">
          <label>Filtrar por rol:</label>
          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="all">Todos los roles</option>
            <option value="admin">Administradores</option>
            <option value="operator">Operarios</option>
          </select>
        </div>

        <div className="user-table">
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Correo</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map(user => (
                <tr key={user.id}>
                  <td>{user.nombre}</td>
                  <td>{user.correo}</td>
                  <td>
                    <span className={`role-badge ${user.rol}`}>
                      {user.rol === 'admin' ? 'Administrador' : 'Operario'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${user.estado}`}>
                      {user.estado === 'active' ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td>
                    <button className="action-btn edit">
                      <i className="fas fa-edit"></i>
                    </button>
                    <button className="action-btn delete" onClick={() => handleDeleteUser(user.id)}>
                      <i className="fas fa-trash"></i>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Sección de estadísticas para probar scroll */}
      <div className="users-stats">
        <h2>
          <i className="fas fa-chart-bar"></i>
          Estadísticas de Usuarios
        </h2>
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-users"></i>
            </div>
            <div className="stat-info">
              <h3>127</h3>
              <p>Usuarios Activos</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-user-plus"></i>
            </div>
            <div className="stat-info">
              <h3>24</h3>
              <p>Nuevos Esta Semana</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-user-shield"></i>
            </div>
            <div className="stat-info">
              <h3>8</h3>
              <p>Administradores</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon">
              <i className="fas fa-user-clock"></i>
            </div>
            <div className="stat-info">
              <h3>3.2h</h3>
              <p>Tiempo Promedio Sesión</p>
            </div>
          </div>
        </div>
      </div>

      <div className="users-activity">
        <h2>
          <i className="fas fa-history"></i>
          Actividad Reciente
        </h2>
        <div className="activity-list">
          <div className="activity-item">
            <div className="activity-avatar">
              <i className="fas fa-user"></i>
            </div>
            <div className="activity-details">
              <h4>María García</h4>
              <p>Inició sesión desde Madrid</p>
              <span className="activity-time">Hace 5 minutos</span>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-avatar">
              <i className="fas fa-user"></i>
            </div>
            <div className="activity-details">
              <h4>Carlos Rodríguez</h4>
              <p>Actualizó perfil de usuario</p>
              <span className="activity-time">Hace 15 minutos</span>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-avatar">
              <i className="fas fa-user"></i>
            </div>
            <div className="activity-details">
              <h4>Ana López</h4>
              <p>Cambió permisos de acceso</p>
              <span className="activity-time">Hace 30 minutos</span>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-avatar">
              <i className="fas fa-user"></i>
            </div>
            <div className="activity-details">
              <h4>Miguel Torres</h4>
              <p>Creó nuevo usuario operativo</p>
              <span className="activity-time">Hace 1 hora</span>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-avatar">
              <i className="fas fa-user"></i>
            </div>
            <div className="activity-details">
              <h4>Laura Martín</h4>
              <p>Desactivó cuenta temporal</p>
              <span className="activity-time">Hace 2 horas</span>
            </div>
          </div>
        </div>
      </div>

      {/* Modal para agregar usuario */}
      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Agregar Nuevo Usuario</h3>
            <div className="form-group">
              <label>Nombre:</label>
              <input
                type="text"
                value={newUser.nombre}
                onChange={(e) => setNewUser({...newUser, nombre: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>Correo:</label>
              <input
                type="email"
                value={newUser.correo}
                onChange={(e) => setNewUser({...newUser, correo: e.target.value})}
              />
            </div>
            <div className="form-group">
              <label>Rol:</label>
              <select 
                value={newUser.rol}
                onChange={(e) => setNewUser({...newUser, rol: e.target.value})}
              >
                <option value="operator">Operario</option>
                <option value="admin">Administrador</option>
              </select>
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setShowAddModal(false)}>
                Cancelar
              </button>
              <button className="btn-save" onClick={handleAddUser}>
                Guardar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Usuarios;
