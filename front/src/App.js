import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation, Outlet } from 'react-router-dom';
import Header from './components/layout/Header/Header';
import Login from './pages/login/Login';
import Configuracion from './pages/configuracion/Configuracion';
import Usuarios from './pages/usuarios/Usuarios';
import Simulador from './pages/simulacion/Monitoreo/Simulador';
import SimuladorSemanal from './pages/simulacion/Simulador/SimuladorSemanal';
import SimuladorDiario from './pages/simulacion/Simulador/SimuladorDiario';
import SimuladorSimple from './components/SimuladorSimple';
import SimuladorColapso from './pages/simulacion/Simulador/SimuladorColapso';
import Clientes from './pages/clientes/Clientes';
import Pedidos from './pages/pedidos/Pedidos';
import './styles/global.css';
import Seleccion from './pages/simulacion/Seleccion';

// Layout que SÍ muestra Header/Nav (para rutas privadas)
function AppLayout() {
  return (
    <div className="app-container">
      <Header />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

// Componente principal con las rutas
function AppContent() {
  const location = useLocation();
  const isLoginPage = location.pathname.startsWith('/login');

  return (
    <div className="app-container">
      {/*Retirar despues de las pruebas*/}
      <Header />
      <main className={`main-content ${isLoginPage ? 'simulador-mode' : ''}`}>
        <Routes>
          {/* LOGIN sin Header/Nav */}
          <Route path="/login" element={<Login />} />

          <Route element={<AppLayout />}>
            {/* Rutas privadas - Administración */}
            <Route path="/configuracion" element={<Configuracion />} />
            <Route path="/usuarios" element={<Usuarios />} />

            {/* Rutas privadas - Clientes y Pedidos */}
            <Route path="/clientes" element={<Clientes />} />
            <Route path="/pedidos" element={<Pedidos />} />

            {/* Rutas privadas - Operaciones */}
            <Route path="/operaciones">
              <Route index element={<Seleccion />} />
              <Route path="monitoreo" element={<Simulador />} />
              <Route path="simulador-semanal" element={<SimuladorSemanal />} />
              <Route path="simulador-diario" element={<SimuladorDiario />} />
              <Route path="simulador-simple" element={<SimuladorSimple />} />
              <Route path="simulador-colapso" element={<SimuladorColapso />} />
            </Route>
          </Route>

          {/* Redirección por defecto */}
          <Route path="/" element={<Navigate to="/login" replace />} />

          {/* Ruta 404 */}
          <Route path="*" element={<Navigate to="/login" replace />} />
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