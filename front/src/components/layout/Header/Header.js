import React from 'react';
import './Header.css';

const Header = () => {
  return (
    <div className="app-header">
      <div className="brand-logo">
        <i className="fas fa-dove"></i>
        <div className="brand-text">
          <span className="brand-name">MoraPack</span>
          <span className="brand-subtitle">Global Distribution</span>
        </div>
      </div>
      <div className="header-title">Panel de Monitoreo</div>
    </div>
  );
};

export default Header;
