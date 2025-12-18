import React, { useState } from 'react';
import { useNavigate, NavLink } from 'react-router-dom';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import AccountCircle from '@mui/icons-material/AccountCircle';
import Logout from '@mui/icons-material/Logout';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  
  // Estado para el menú de Operaciones
  const [operacionesAnchorEl, setOperacionesAnchorEl] = useState(null);
  const operacionesOpen = Boolean(operacionesAnchorEl);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };
  
  // Handlers para el menú de Operaciones
  const handleOperacionesOpen = (event) => {
    setOperacionesAnchorEl(event.currentTarget);
  };
  
  const handleOperacionesClose = () => {
    setOperacionesAnchorEl(null);
  };
  
  const handleOperacionesNavigate = (path) => {
    handleOperacionesClose();
    navigate(path);
  };

  const handleToggleMobile = () => setMobileOpen(v => !v);

  const handleViewProfile = () => {
    handleClose();
    navigate('/perfil');
  };

  const handleLogout = () => {
    handleClose();
    try { localStorage.removeItem('authToken'); localStorage.removeItem('user'); } catch (e) {}
    navigate('/login');
  };

  const storedUser = (() => {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); } catch (e) { return null; }
  })();
  const userEmail = storedUser?.email || 'usuario@email.com';
  const userName = storedUser?.name || 'Juan Pérez';

  return (
    <header className="app-header">
      <div className="header-left">
        <IconButton
          className="mobile-menu-btn"
          onClick={handleToggleMobile}
          size="large"
          edge="start"
          color="inherit"
          aria-label="menu"
        >
          <MenuIcon />
        </IconButton>
        
        <div className="brand-logo" onClick={() => navigate('/') }>
          <i className="fas fa-dove" />
          <div className="brand-text">
            <span className="brand-name">MoraPack</span>
            <span className="brand-subtitle">Global Distribution</span>
          </div>
        </div>
        
        {/* Desktop Navigation */}
        <nav className="header-nav desktop-nav">
          {/*<NavLink to="/configuracion" className="nav-item">
            <i className="fas fa-cog"></i>
            Configuración
          </NavLink>*/}
          <NavLink to="/usuarios" className="nav-item">
            <i className="fas fa-users"></i>
            Usuarios
          </NavLink>
          
          {/* Menú desplegable de Operaciones */}
          <div 
            className="nav-item operaciones-dropdown"
            onMouseEnter={handleOperacionesOpen}
            onMouseLeave={handleOperacionesClose}
            style={{ position: 'relative', cursor: 'pointer' }}
          >
            <i className="fas fa-plane"></i>
            Operaciones
            <KeyboardArrowDownIcon sx={{ fontSize: 18, ml: 0.5 }} />
            
            {operacionesOpen && (
              <div 
                className="operaciones-menu"
                style={{
                  position: 'absolute',
                  top: '100%',
                  left: '50%',
                  transform: 'translateX(-50%)',
                  backgroundColor: 'white',
                  borderRadius: '8px',
                  boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
                  minWidth: '220px',
                  zIndex: 1000,
                  overflow: 'hidden',
                  marginTop: '4px'
                }}
              >
                <div 
                  onClick={() => handleOperacionesNavigate('/operaciones/monitoreo')}
                  style={{
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    cursor: 'pointer',
                    transition: 'background-color 0.2s',
                    color: '#333'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f5f5f5'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  <span style={{ fontSize: '18px' }}>📡</span>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '14px' }}>Monitoreo en Tiempo Real</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>Control y seguimiento activo</div>
                  </div>
                </div>
                
                <div 
                  onClick={() => handleOperacionesNavigate('/operaciones/simulador-semanal')}
                  style={{
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    cursor: 'pointer',
                    transition: 'background-color 0.2s',
                    color: '#333',
                    borderTop: '1px solid #eee'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f5f5f5'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  <span style={{ fontSize: '18px' }}>📅</span>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '14px' }}>Simulación Semanal</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>Planificación operativa de 7 días</div>
                  </div>
                </div>
                
                <div 
                  onClick={() => handleOperacionesNavigate('/operaciones/simulador-colapso')}
                  style={{
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    cursor: 'pointer',
                    transition: 'background-color 0.2s',
                    color: '#333',
                    borderTop: '1px solid #eee'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f5f5f5'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  <span style={{ fontSize: '18px' }}>⚠️</span>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '14px' }}>Simulación de Colapso</div>
                    <div style={{ fontSize: '11px', color: '#666' }}>Análisis de escenarios críticos</div>
                  </div>
                </div>
              </div>
            )}
          </div>
          
          {/*<NavLink to="/clientes" className="nav-item">
            <i className="fas fa-box"></i>
            Clientes
          </NavLink>*/}
          <NavLink to="/pedidos" className="nav-item">
            <i className="fas fa-clipboard-list"></i>
            Pedidos
          </NavLink>
        </nav>
      </div>

      <div className="header-right">
        <div className="user-profile" onClick={handleClick}>
          <div className="user-avatar">
            {userName[0].toUpperCase()}
          </div>
          <div className="user-info">
            <span className="user-name">{userName}</span>
          </div>
          <KeyboardArrowDownIcon />
        </div>

        <Menu
          anchorEl={anchorEl}
          open={open}
          onClose={handleClose}
          PaperProps={{
            sx: {
              mt: 1,
              minWidth: 180,
              borderRadius: 2,
              boxShadow: '0px 4px 12px rgba(0,0,0,0.1)',
            },
          }}
        >
          <MenuItem onClick={handleViewProfile} sx={{ gap: 1 }}>
            <AccountCircle fontSize="small" />
            Mi Perfil
          </MenuItem>
          <MenuItem onClick={handleLogout} sx={{ color: 'error.main', gap: 1 }}>
            <Logout fontSize="small" />
            Cerrar Sesión
          </MenuItem>
        </Menu>
      </div>

      {/* Mobile Navigation Overlay */}
      {mobileOpen && (
        <div className="mobile-nav-overlay">
          <nav className="mobile-nav">
            {/*<NavLink to="/configuracion" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-cog"></i>Configuración</NavLink>*/}
            <NavLink to="/usuarios" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-users"></i>Usuarios</NavLink>
            
            {/* Submenú de Operaciones en móvil */}
            <div className="nav-item" style={{ cursor: 'default', fontWeight: 600, color: '#1976d2' }}>
              <i className="fas fa-plane"></i>Operaciones
            </div>
            <NavLink to="/operaciones/monitoreo" className="nav-item" onClick={handleToggleMobile} style={{ paddingLeft: '32px', fontSize: '14px' }}>
              📡 Monitoreo en Tiempo Real
            </NavLink>
            <NavLink to="/operaciones/simulador-semanal" className="nav-item" onClick={handleToggleMobile} style={{ paddingLeft: '32px', fontSize: '14px' }}>
              📅 Simulación Semanal
            </NavLink>
            <NavLink to="/operaciones/simulador-colapso" className="nav-item" onClick={handleToggleMobile} style={{ paddingLeft: '32px', fontSize: '14px' }}>
              ⚠️ Simulación de Colapso
            </NavLink>
            
            {/*<NavLink to="/clientes" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-box"></i>Clientes</NavLink>*/}
            <NavLink to="/pedidos" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-clipboard-list"></i>Pedidos</NavLink>
          </nav>
        </div>
      )}
    </header>
  );
};

export default Header;
