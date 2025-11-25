# 🚀 Simulador Semanal Simple - Versión REST

## 📌 Descripción

Nueva implementación **completamente simplificada** de la planificación semanal usando **REST** en lugar de WebSocket.

## ✨ Características

- ✅ **Una sola llamada HTTP** - sin WebSocket complejo
- ✅ **Interfaz minimalista** - fecha + botón
- ✅ **Resultados inmediatos** - todos los vuelos de una vez
- ✅ **Fácil de probar** - curl/Postman/script
- ✅ **Código limpio** - 200 líneas vs 500+

## 🎯 Inicio Rápido

### 1. Backend
```bash
cd backend
mvn spring-boot:run
```

### 2. Frontend
```bash
cd front
npm start
```

### 3. Navegador
```
http://localhost:3001/operaciones/simulador-simple
```

## 🧪 Prueba Rápida

```bash
./test_planificacion_simple.sh
```

O con curl:
```bash
curl -X POST "http://localhost:8000/api/planificacion/ejecutar-simple?fecha=2025-01-15&factorK=5"
```

## 📂 Archivos

### Backend
- `PlanificacionSimpleController.java` - REST endpoint

### Frontend
- `SimuladorSimple.js` - Componente React
- `SimuladorSimple.css` - Estilos
- `App.js` - Ruta agregada

### Documentación
- `INSTRUCCIONES_RAPIDAS.md` ⭐ **LEER PRIMERO**
- `SOLUCION_SIMPLE_REST.md` - Documentación completa
- `test_planificacion_simple.sh` - Script de prueba

## 🎨 Uso

1. Seleccionar fecha (ej: 2025-01-15)
2. Click en "🚀 Iniciar Simulación"
3. Esperar (loading)
4. Ver vuelos en el mapa
5. Limpiar para reiniciar

## 📊 Respuesta API

```json
{
  "vuelos": [
    {
      "fechaInicial": "2025-01-15 08:00",
      "fechaFinal": "2025-01-15 14:00",
      "origenCodigoICAO": "SPIM",
      "destinoCodigoICAO": "KJFK",
      "pedidos": [
        {"idPedido": 123, "cantidad": 50}
      ]
    }
  ]
}
```

## 🆚 Comparación

| | WebSocket | REST |
|---|---|---|
| Complejidad | Alta | Baja |
| Líneas código | 500+ | 200 |
| Testing | Difícil | Fácil |
| Estado | Stateful | Stateless |
| Debugging | Complejo | Simple |

## ✅ Ventajas

- **Simple de usar** - Click y listo
- **Simple de entender** - Código claro
- **Simple de mantener** - Sin magia
- **Rápido** - Resultados inmediatos
- **Efectivo** - Hace lo que necesitas

## 📖 Documentación

Lee `INSTRUCCIONES_RAPIDAS.md` para más detalles.

---

**¡A probarlo!** 🎉
