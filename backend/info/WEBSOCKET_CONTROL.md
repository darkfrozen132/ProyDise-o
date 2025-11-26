# WebSocket con Control ON/OFF

## 📋 Descripción

WebSocket simple con un booleano para activar/desactivar el envío de mensajes.

## 🔧 Clase Principal: WebSocketService

### Métodos:

```java
// Activar el WebSocket
webSocketService.activar();

// Desactivar el WebSocket
webSocketService.desactivar();

// Verificar estado
boolean activo = webSocketService.estaHabilitado();

// Enviar mensaje (solo si está habilitado)
webSocketService.enviarMensaje("/topic/estado", mensaje);
```

## 🌐 Endpoints REST

### 1. Activar WebSocket
```bash
curl http://localhost:8000/api/websocket/activar
```
**Respuesta:**
```json
{
  "accion": "activar",
  "habilitado": true,
  "mensaje": "WebSocket activado correctamente"
}
```

### 2. Desactivar WebSocket
```bash
curl http://localhost:8000/api/websocket/desactivar
```
**Respuesta:**
```json
{
  "accion": "desactivar",
  "habilitado": false,
  "mensaje": "WebSocket desactivado"
}
```

### 3. Consultar Estado
```bash
curl http://localhost:8000/api/websocket/estado
```
**Respuesta:**
```json
{
  "habilitado": true,
  "timestamp": "2025-11-04T07:53:26.123"
}
```

### 4. Enviar Mensaje de Prueba
```bash
curl "http://localhost:8000/api/websocket/test?mensaje=Hola"
```
**Respuesta:**
```json
{
  "enviado": true,
  "mensaje": "Hola",
  "canal": "/topic/estado",
  "habilitado": true
}
```

## 📝 Comportamiento

- **Habilitado (true)**: Los mensajes se envían normalmente por WebSocket
- **Deshabilitado (false)**: Los mensajes NO se envían, se ignoran silenciosamente

## 💡 Ejemplo de Uso

```java
@Service
public class MiServicio {
    
    @Autowired
    private WebSocketService webSocketService;
    
    public void procesarAlgo() {
        // Enviar notificación (solo si está habilitado)
        webSocketService.enviarEstado("Proceso completado");
        
        // O con más control
        if (webSocketService.estaHabilitado()) {
            Map<String, Object> data = Map.of(
                "tipo", "notificacion",
                "mensaje", "Datos actualizados"
            );
            webSocketService.enviarMensaje("/topic/estado", data);
        }
    }
}
```

## 🎯 Estado por Defecto

El WebSocket está **ACTIVADO** por defecto (`habilitado = true`).

## 📊 Logs

Cuando activas/desactivas verás en el log:
```
✅ WebSocket ACTIVADO
❌ WebSocket DESACTIVADO
```

Cuando envías mensajes:
```
📤 Mensaje enviado a /topic/estado: {...}
⏭️ WebSocket deshabilitado. Mensaje NO enviado a /topic/estado
```
