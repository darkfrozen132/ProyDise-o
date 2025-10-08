import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Header from './components/Header/Header';
import Navigation from './components/Navigation/Navigation';
import Login from './pages/login/Login';
import Configuracion from './pages/confi/Configuracion';
import Usuarios from './pages/usuarios/Usuarios';
import Simulador from './pages/simulador/Simulador';
import SimuladorSemanal from './pages/simulador/SimuladorSemanal';
import SimuladorColapso from './pages/simulador/SimuladorColapso';
import Clientes from './pages/clientes/Clientes';
import Pedidos from './pages/pedidos/Pedidos';
import './App.css';

function AppContent() {
  const location = useLocation();
  const isSimulador = location.pathname.startsWith('/simulador');

  return (
    <div className="app-container">
      <Header />
      <Navigation />
      <main className={`main-content ${isSimulador ? 'simulador-mode' : ''}`}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/configuracion" element={<Configuracion />} />
          <Route path="/usuarios" element={<Usuarios />} />
          <Route path="/simulador" element={<Simulador />} />
          <Route path="/clientes" element={<Clientes />} />
          <Route path="/simulador-semanal" element={<SimuladorSemanal />} />
          <Route path="/simulador-colapso" element={<SimuladorColapso />} />
          <Route path="/pedidos" element={<Pedidos />} />
          <Route path="/" element={<Navigate to="/simulador" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
