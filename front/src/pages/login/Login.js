import React, { useState } from 'react';
import './Login.css';

const Login = () => {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCredentials(prevCredentials => ({
      ...prevCredentials,
      [name]: value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // Lógica de autenticación: aquí se simularía una llamada a un backend
    console.log('Intento de inicio de sesión:', credentials);
    // En una aplicación real, aquí se manejaría la respuesta y la redirección
  };

  return (
    <div className="login-container">
      <div className="login-form">
        <div className="login-header">
          <i className="fas fa-dove"></i>
          <h2>Acceso MoraPack</h2>
          <p>Panel de Monitoreo Global</p>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              type="text"
              id="username"
              name="username"
              value={credentials.username}
              onChange={handleChange}
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              type="password"
              id="password"
              name="password"
              value={credentials.password}
              onChange={handleChange}
              required
            />
          </div>
          
          <button type="submit" className="login-button">
            <i className="fas fa-sign-in-alt"></i>
            Iniciar Sesión
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;