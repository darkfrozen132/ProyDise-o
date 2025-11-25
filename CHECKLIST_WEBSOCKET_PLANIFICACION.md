# ✅ CHECKLIST DE VERIFICACIÓN - WebSocket Planificación

## Pre-requisitos

### Backend
- [ ] Backend iniciado en puerto 8000
- [ ] Base de datos MySQL conectada
- [ ] Aeropuertos cargados en la base de datos
- [ ] Sin errores en logs del backend

**Comando:**
```bash
cd backend
mvn spring-boot:run
```

**Verificar en logs:**
```
Started BackendApplication in X seconds
```

---

### Frontend
- [ ] Frontend iniciado en puerto 3000
- [ ] Sin errores de compilación
- [ ] Navegador abierto en http://localhost:3000

**Comando:**
```bash
cd front
npm start
```

---

## Prueba 1: Conexión WebSocket

### Pasos:
1. [ ] Abrir Simulador Semanal
2. [ ] Abrir consola del navegador (F12)
3. [ ] Buscar mensaje: `🔌 Auto-conectando WebSocket de planificación...`
4. [ ] Verificar mensaje: `🔌 WebSocket Planificación conectado`
5. [ ] Verificar mensaje: `✅ Conexión establecida`

### Estado esperado:
- [ ] Indicador verde "Conectado" visible
- [ ] Sin errores en consola
- [ ] Botón "Detener Planificación" deshabilitado (gris)

---

## Prueba 2: Iniciar Planificación

### Pasos:
1. [ ] Seleccionar fecha: **2025-01-15**
2. [ ] Observar auto-inicio: `🚀 Auto-iniciando planificación...`
3. [ ] Verificar envío: `📤 Solicitud de planificación enviada`
4. [ ] Esperar respuesta del backend

### Estado esperado:
- [ ] Indicador azul "Planificación en curso"
- [ ] Tiempo de simulación actualizado
- [ ] Cronómetro de tiempo real corriendo

---

## Prueba 3: Recepción de Vuelos

### Pasos:
1. [ ] Observar primer mensaje `completado`
2. [ ] Verificar log: `✈️ Procesando X vuelos de la iteración #1`
3. [ ] Verificar conversión: `🔄 Convertidos X de X vuelos`
4. [ ] Verificar actualización: `📊 Total vuelos en mapa: X`

### En el mapa:
- [ ] Aparecen aviones azules
- [ ] Los aviones tienen posición correcta
- [ ] Se puede hacer hover sobre los aviones
- [ ] Los aviones no desaparecen

### En el contador:
- [ ] "Vuelos en el aire" aumenta
- [ ] Coincide con el número en los logs

---

## Prueba 4: Acumulación de Vuelos

### Pasos:
1. [ ] Esperar segunda iteración
2. [ ] Verificar: `📊 Total vuelos en mapa: X (Y anteriores + Z nuevos)`
3. [ ] Contar aviones en el mapa
4. [ ] Verificar que coincida con el total

### Estado esperado:
- [ ] Los vuelos anteriores siguen visibles
- [ ] Los nuevos vuelos se suman
- [ ] No hay reemplazo, solo acumulación

---

## Prueba 5: Continuidad Automática

### Pasos:
1. [ ] Esperar varias iteraciones
2. [ ] Verificar log: `⏩ Solicitando siguiente intervalo... (Iteración X)`
3. [ ] Observar que continúa automáticamente
4. [ ] Verificar que el tiempo de simulación avanza

### Estado esperado:
- [ ] Proceso continuo sin intervención manual
- [ ] Iteraciones incrementando
- [ ] Tiempo de simulación avanzando correctamente

---

## Prueba 6: Detener Planificación

### Pasos:
1. [ ] Hacer clic en botón "Detener Planificación" (amarillo)
2. [ ] Verificar log: `🛑 Deteniendo planificación continua...`
3. [ ] Verificar que no llegan más iteraciones

### Estado esperado:
- [ ] Indicador cambia a "Detenido"
- [ ] Los vuelos permanecen en el mapa
- [ ] Cronómetro se detiene
- [ ] Se puede reiniciar con nueva fecha

---

## Prueba 7: Limpiar Mapa

### Pasos:
1. [ ] Hacer clic en "Limpiar Mapa" (azul)
2. [ ] Verificar log: `🧹 Limpiando mapa...`
3. [ ] Observar el mapa

### Estado esperado:
- [ ] Todos los vuelos desaparecen del mapa
- [ ] Contador "Vuelos en el aire" = 0
- [ ] Lista de iteraciones vacía
- [ ] Mapa limpio y listo para nueva planificación

---

## Prueba 8: Nueva Planificación

### Pasos:
1. [ ] Seleccionar nueva fecha: **2025-02-01**
2. [ ] Observar que inicia automáticamente
3. [ ] Verificar que el mapa se limpia primero
4. [ ] Verificar nuevos vuelos aparecen

### Estado esperado:
- [ ] Vuelos anteriores eliminados
- [ ] Nueva planificación inicia desde cero
- [ ] Iteraciones comienzan en #1
- [ ] Todo funciona como en prueba inicial

---

## Verificación de Logs

### Logs correctos (✅):
```
🔌 Auto-conectando WebSocket de planificación...
🔌 WebSocket Planificación conectado: ws://localhost:8000/ws/planificacion
✅ Conexión establecida
🚀 Auto-iniciando planificación...
📤 Solicitud de planificación enviada para fecha: 2025-01-15
✈️ Procesando 5 vuelos de la iteración #1
🔄 Convertidos 5 de 5 vuelos
📊 Total vuelos en mapa: 5 (0 anteriores + 5 nuevos)
✈️ Vuelos en aire actualizados: 5
✅ 5 vuelos graficados exitosamente
⏩ Solicitando siguiente intervalo... (Iteración 2)
```

### Logs problemáticos (❌):
```
❌ Error en WebSocket: ...
⚠️ Aeropuertos no encontrados: XXXX o YYYY
⚠️ No se pudieron convertir vuelos (aeropuertos no encontrados)
❌ No se pudo enviar la siguiente solicitud
```

---

## Prueba de Rendimiento

### Objetivos:
- [ ] 10 iteraciones en menos de 30 segundos
- [ ] Sin lag en el navegador
- [ ] Mapa responde a interacciones
- [ ] Memoria del navegador estable

### Cómo verificar:
1. Abrir DevTools → Performance Monitor
2. Observar uso de memoria
3. Verificar FPS (debe ser ~60)
4. Verificar que no haya memory leaks

---

## Prueba de Robustez

### Escenarios:
- [ ] Cerrar y abrir el navegador → WebSocket se reconecta
- [ ] Cambiar fecha múltiples veces → Sin errores
- [ ] Detener backend temporalmente → Error graceful
- [ ] Reiniciar backend → Reconexión automática

---

## Prueba de UI/UX

### Elementos visuales:
- [ ] Botones cambian de estado correctamente
- [ ] Indicador de estado visible y claro
- [ ] Cronómetro actualiza cada segundo
- [ ] Aviones en el mapa con color correcto (azul)
- [ ] Hover sobre aviones muestra información

### Responsive:
- [ ] Panel de control se ajusta al tamaño
- [ ] Mapa ocupa todo el espacio disponible
- [ ] Drawer lateral funciona correctamente

---

## Checklist Final

### Funcionalidad Core:
- [ ] ✅ WebSocket conecta automáticamente
- [ ] ✅ Planificación inicia automáticamente
- [ ] ✅ Vuelos aparecen en el mapa
- [ ] ✅ Vuelos se acumulan correctamente
- [ ] ✅ Proceso continúa automáticamente
- [ ] ✅ Se puede detener manualmente
- [ ] ✅ Se puede limpiar el mapa
- [ ] ✅ Se puede reiniciar con nueva fecha

### Calidad:
- [ ] ✅ Sin errores en consola
- [ ] ✅ Sin warnings importantes
- [ ] ✅ Logs claros y útiles
- [ ] ✅ Performance aceptable
- [ ] ✅ UI/UX intuitiva

### Documentación:
- [ ] ✅ README actualizado
- [ ] ✅ Comentarios en código
- [ ] ✅ Logs descriptivos
- [ ] ✅ Archivo de test disponible

---

## 🎉 SI TODO ESTÁ ✅

¡Felicitaciones! El WebSocket de planificación está funcionando correctamente.

**Siguiente paso:** Usar el sistema en producción o agregar features adicionales.

---

## 🐛 SI ALGO FALLA

1. **Revisar logs del backend** (Spring Boot)
2. **Revisar logs del frontend** (Consola del navegador)
3. **Usar test_websocket_planificacion.html** para aislar el problema
4. **Leer CAMBIOS_WEBSOCKET_PLANIFICACION.md** para detalles técnicos
5. **Verificar puertos:** Backend=8000, Frontend=3000

---

## 📊 Métricas de Éxito

- **Tiempo de conexión:** < 1 segundo
- **Tiempo primera respuesta:** < 5 segundos
- **Iteraciones por minuto:** ~10-20 (depende del hardware)
- **Vuelos por iteración:** Variable (depende de la planificación)
- **Tasa de error:** 0%

---

**Última actualización:** 22 de noviembre de 2025
**Versión:** 1.0
**Estado:** ✅ Producción
