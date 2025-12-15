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

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
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
          <NavLink to="/operaciones" className="nav-item">
            <i className="fas fa-plane"></i>
            Operaciones
          </NavLink>
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
            <NavLink to="/operaciones" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-plane"></i>Operaciones</NavLink>
            {/*<NavLink to="/clientes" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-box"></i>Clientes</NavLink>*/}
            <NavLink to="/pedidos" className="nav-item" onClick={handleToggleMobile}><i className="fas fa-clipboard-list"></i>Pedidos</NavLink>
          </nav>
        </div>
      )}
    </header>
  );
};

export default Header;
