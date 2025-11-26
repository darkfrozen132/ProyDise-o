/**
 * 🧪 PRUEBA RÁPIDA DEL SERVICIO DE SIMULACIÓN
 * 
 * Usa este archivo para probar el servicio independientemente del componente React
 * 
 * Uso:
 * 1. Asegúrate de que el backend esté corriendo
 * 2. Importa este archivo en tu App.js o cualquier componente
 * 3. Llama a testSimulacion()
 */

import { simulacionService } from './SimulacionLogisticaService';

/**
 * 🧪 TEST COMPLETO DEL SERVICIO
 */
export async function testSimulacion() {
  console.log('🧪 ========================================');
  console.log('🧪 INICIANDO TEST DEL SERVICIO');
  console.log('🧪 ========================================');

  try {
    // 1. CONFIGURAR CALLBACKS
    console.log('\n📌 Paso 1: Configurando callbacks...');
    
    simulacionService
      .onConnected(() => {
        console.log('✅ Callback: Conectado');
      })
      .onDisconnected(() => {
        console.log('⚠️ Callback: Desconectado');
      })
      .onProgresoAG((data) => {
        console.log('🧬 Callback: Progreso AG recibido');
        console.log(`   Generación: ${data.generacion}/${data.maxGeneraciones}`);
        console.log(`   Progreso: ${data.progreso.toFixed(2)}%`);
        console.log(`   Mejor Fitness: ${data.mejorFitness}`);
        console.log(`   Pedidos: ${data.pedidosProcesados}/${data.pedidosTotales}`);
      })
      .onSnapshot((data) => {
        console.log('📊 Callback: Snapshot recibido');
        console.log(`   Iteración: ${data.iteration}`);
        console.log(`   Status: ${data.status}`);
      })
      .onCompleted((data) => {
        console.log('✅ Callback: Simulación completada');
        console.log(`   Status: ${data.status}`);
        console.log('🧪 ========================================');
        console.log('🧪 TEST COMPLETADO EXITOSAMENTE');
        console.log('🧪 ========================================');
      })
      .onError((error) => {
        console.error('❌ Callback: Error recibido');
        console.error(`   Tipo: ${error.type}`);
        console.error(`   Mensaje: ${error.message}`);
      });

    // 2. CONECTAR WEBSOCKET
    console.log('\n📌 Paso 2: Conectando al WebSocket...');
    await simulacionService.connect();
    console.log('✅ WebSocket conectado');

    // Esperar un momento para asegurar conexión
    await sleep(1000);

    // 3. VERIFICAR ESTADO
    console.log('\n📌 Paso 3: Verificando estado del servicio...');
    const estado = simulacionService.getEstado();
    console.log('Estado:', estado);

    if (!estado.isConnected) {
      throw new Error('WebSocket no está conectado');
    }

    // 4. INICIAR SIMULACIÓN
    console.log('\n📌 Paso 4: Iniciando simulación...');
    const fecha = '2025-01-15';
    console.log(`Fecha: ${fecha}`);
    console.log(`Factor K: 5 (automático)`);

    const sessionId = await simulacionService.iniciarSimulacion(fecha);
    console.log(`✅ Simulación iniciada con Session ID: ${sessionId}`);

    // 5. ESPERAR ALGUNOS SEGUNDOS
    console.log('\n📌 Paso 5: Esperando mensajes...');
    console.log('Recibirás mensajes en los callbacks configurados arriba');
    console.log('Esperando 30 segundos para ver actualizaciones...');
    
    await sleep(30000);

    // 6. CANCELAR SIMULACIÓN (OPCIONAL)
    console.log('\n📌 Paso 6: Cancelando simulación...');
    await simulacionService.cancelarSimulacion();
    console.log('✅ Simulación cancelada');

    // 7. DESCONECTAR
    console.log('\n📌 Paso 7: Desconectando...');
    simulacionService.disconnect();
    console.log('✅ Desconectado');

    console.log('\n🎉 Test completado exitosamente');

  } catch (error) {
    console.error('\n❌ ========================================');
    console.error('❌ TEST FALLIDO');
    console.error('❌ ========================================');
    console.error('Error:', error);
    console.error('Stack:', error.stack);
    
    // Limpiar
    simulacionService.disconnect();
  }
}

/**
 * 🧪 TEST RÁPIDO (Solo conexión y ping)
 */
export async function testConexion() {
  console.log('🧪 Testing conexión rápida...');
  
  try {
    await simulacionService.connect();
    console.log('✅ Conexión exitosa');
    
    const estado = simulacionService.getEstado();
    console.log('Estado:', estado);
    
    simulacionService.disconnect();
    console.log('✅ Test de conexión completado');
  } catch (error) {
    console.error('❌ Error en test de conexión:', error);
  }
}

/**
 * 🧪 TEST DE ENDPOINTS REST (Sin WebSocket)
 */
export async function testEndpoints() {
  console.log('🧪 Testing endpoints REST...');
  
  const baseURL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8000';
  
  try {
    // Test 1: POST /api/simulations/start
    console.log('\n📡 Testing POST /api/simulations/start...');
    const response = await fetch(`${baseURL}/api/simulations/start`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        fecha: '2025-01-15',
        factorK: 5
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('✅ Response:', data);

    if (data.sessionId) {
      console.log(`✅ Session ID recibido: ${data.sessionId}`);
      
      // Test 2: POST /api/simulations/{sessionId}/cancel
      console.log('\n📡 Testing POST /api/simulations/{sessionId}/cancel...');
      const cancelResponse = await fetch(
        `${baseURL}/api/simulations/${data.sessionId}/cancel`,
        { method: 'POST' }
      );

      if (cancelResponse.ok) {
        const cancelData = await cancelResponse.json();
        console.log('✅ Cancelación exitosa:', cancelData);
      }
    }

    console.log('\n✅ Test de endpoints completado');
  } catch (error) {
    console.error('❌ Error en test de endpoints:', error);
  }
}

/**
 * 🧪 TEST DE MÚLTIPLES SIMULACIONES
 */
export async function testMultiplesSimulaciones() {
  console.log('🧪 Testing múltiples simulaciones...');
  
  try {
    await simulacionService.connect();
    
    for (let i = 0; i < 3; i++) {
      console.log(`\n🔄 Simulación ${i + 1}/3`);
      
      const sessionId = await simulacionService.iniciarSimulacion('2025-01-15');
      console.log(`   Session: ${sessionId.substring(0, 8)}...`);
      
      await sleep(5000); // Esperar 5 segundos
      
      await simulacionService.cancelarSimulacion();
      console.log('   ✅ Cancelada');
      
      await sleep(2000); // Esperar 2 segundos antes de la siguiente
    }
    
    simulacionService.disconnect();
    console.log('\n✅ Test de múltiples simulaciones completado');
  } catch (error) {
    console.error('❌ Error:', error);
    simulacionService.disconnect();
  }
}

/**
 * Utilidad: Sleep
 */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 🧪 EXPORTAR TODAS LAS PRUEBAS
 */
export const tests = {
  completo: testSimulacion,
  conexion: testConexion,
  endpoints: testEndpoints,
  multiples: testMultiplesSimulaciones,
};

/**
 * 🎮 CONSOLA DE PRUEBAS
 * 
 * Abre la consola del navegador y ejecuta:
 * 
 * window.testSimulador.completo()      // Test completo
 * window.testSimulador.conexion()      // Solo conexión
 * window.testSimulador.endpoints()     // Solo REST API
 * window.testSimulador.multiples()     // Múltiples simulaciones
 */
if (typeof window !== 'undefined') {
  window.testSimulador = tests;
  console.log('✅ Tests disponibles en window.testSimulador');
  console.log('Ejecuta: window.testSimulador.completo()');
}

export default tests;
