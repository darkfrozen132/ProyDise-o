import React from 'react';
import { NavLink } from 'react-router-dom';
import './Navigation.css';

const Navigation = () => {
  return (
    <nav className="nav-tabs">
      <NavLink to="/configuracion" className="nav-tab">
        <i className="fas fa-cog"></i>
        Configuración Inicial
      </NavLink>
      <NavLink to="/usuarios" className="nav-tab">
        <i className="fas fa-users"></i>
        Usuarios
      </NavLink>
      <NavLink to="/simulador" className="nav-tab">
        <i className="fas fa-plane"></i>
        Simulación
      </NavLink>
      <NavLink to="/clientes" className="nav-tab">
        <i className="fas fa-box"></i>
        Clientes
      </NavLink>
      <NavLink to="/pedidos" className="nav-tab">
        <i className="fas fa-clipboard-list"></i>
        Pedidos
      </NavLink>
    </nav>
  );
};

export default Navigation;
