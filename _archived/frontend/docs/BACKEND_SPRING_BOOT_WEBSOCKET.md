# 🔧 CONFIGURACIÓN DEL BACKEND - Spring Boot + WebSocket STOMP

## 📋 Índice

1. [Dependencias Maven](#dependencias-maven)
2. [Configuración WebSocket](#configuración-websocket)
3. [DTOs](#dtos)
4. [Controller REST](#controller-rest)
5. [Service](#service)
6. [Envío de Mensajes](#envío-de-mensajes)
7. [Ejemplo Completo](#ejemplo-completo)

---

## 📦 Dependencias Maven

Añade estas dependencias a tu `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Spring Boot Web (para REST API) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- SockJS (opcional pero recomendado para compatibilidad) -->
    <!-- Ya incluido en spring-boot-starter-websocket -->
    
    <!-- Jackson para JSON (usualmente ya incluido) -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

## ⚙️ Configuración WebSocket

### 1. WebSocketConfig.java

```java
package com.tuempresa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un broker simple en memoria para enviar mensajes a clientes
        config.enableSimpleBroker("/topic");
        
        // Prefijo para mensajes destinados a métodos @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra el endpoint /ws que usará SockJS
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000") // ⚠️ IMPORTANTE: CORS
                .withSockJS(); // Habilita fallback de SockJS
    }
}
```

**Explicación:**
- `/topic` → Prefijo para topics de broadcast (uno-a-muchos)
- `/app` → Prefijo para mensajes enviados por clientes
- `/ws` → URL del endpoint WebSocket
- `setAllowedOrigins()` → **MUY IMPORTANTE** para CORS
- `withSockJS()` → Habilita fallback para navegadores sin WebSocket nativo

---

## 📝 DTOs

### 1. SimulationRequest.java (Request del cliente)

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SimulationRequest {
    
    @JsonProperty("fecha")
    private String fecha; // Formato: "2025-01-02"
    
    @JsonProperty("factorK")
    private int factorK; // Ejemplo: 5
}
```

### 2. SimulationStartResponse.java (Response al cliente)

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimulationStartResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("mensaje")
    private String mensaje;
    
    @JsonProperty("topicUrl")
    private String topicUrl;
}
```

### 3. ProgresoAGDTO.java (Progreso del Algoritmo Genético)

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoAGDTO {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("tipo")
    private String tipo = "PROGRESO_AG"; // Constante
    
    @JsonProperty("generacion")
    private int generacion;
    
    @JsonProperty("maxGeneraciones")
    private int maxGeneraciones;
    
    @JsonProperty("progreso")
    private double progreso; // 0.0 a 100.0
    
    @JsonProperty("mejorFitness")
    private double mejorFitness;
    
    @JsonProperty("fitnessPromedio")
    private double fitnessPromedio;
    
    @JsonProperty("solucion")
    private SolucionDTO solucion;
    
    @JsonProperty("fechaSimulada")
    private String fechaSimulada; // ISO format
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("pedidosProcesados")
    private int pedidosProcesados;
    
    @JsonProperty("pedidosTotales")
    private int pedidosTotales;
    
    @JsonProperty("mensaje")
    private String mensaje;
}
```

### 4. SolucionDTO.java (Solución del AG)

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SolucionDTO {
    
    @JsonProperty("rutas")
    private List<RutaDTO> rutas;
    
    @JsonProperty("metricas")
    private MetricasDTO metricas;
}
```

### 5. RutaDTO.java

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RutaDTO {
    
    @JsonProperty("pedidoId")
    private Long pedidoId;
    
    @JsonProperty("origen")
    private String origen; // Código ICAO
    
    @JsonProperty("destino")
    private String destino; // Código ICAO
    
    @JsonProperty("subRutas")
    private List<SubRutaDTO> subRutas;
}
```

### 6. SubRutaDTO.java

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubRutaDTO {
    
    @JsonProperty("origen")
    private String origen; // Código ICAO
    
    @JsonProperty("destino")
    private String destino; // Código ICAO
    
    @JsonProperty("vuelo")
    private String vuelo; // Número de vuelo
    
    @JsonProperty("horaSalida")
    private String horaSalida; // ISO format: "2025-01-02T10:00:00"
    
    @JsonProperty("horaLlegada")
    private String horaLlegada; // ISO format: "2025-01-02T22:00:00"
}
```

### 7. MetricasDTO.java

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MetricasDTO {
    
    @JsonProperty("totalRutas")
    private int totalRutas;
    
    @JsonProperty("totalVuelos")
    private int totalVuelos;
    
    @JsonProperty("costoTotal")
    private double costoTotal;
    
    @JsonProperty("tiempoPromedioEntrega")
    private double tiempoPromedioEntrega; // En horas
}
```

### 8. SimulationSnapshot.java (Snapshot general)

```java
package com.tuempresa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimulationSnapshot {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("status")
    private String status; // "RUNNING", "COMPLETED", "CANCELLED"
    
    @JsonProperty("iteration")
    private int iteration;
    
    @JsonProperty("simulatedTime")
    private String simulatedTime;
    
    @JsonProperty("processedOrders")
    private int processedOrders;
    
    @JsonProperty("totalOrders")
    private int totalOrders;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("solution")
    private Object solution; // Puede ser SolucionDTO u otro formato
}
```

---

## 🎮 Controller REST

### SimulationController.java

```java
package com.tuempresa.controller;

import com.tuempresa.dto.SimulationRequest;
import com.tuempresa.dto.SimulationStartResponse;
import com.tuempresa.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "http://localhost:3000") // ⚠️ IMPORTANTE: CORS
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * Inicia una nueva simulación
     */
    @PostMapping("/start")
    public ResponseEntity<SimulationStartResponse> startSimulation(
            @RequestBody SimulationRequest request) {
        
        try {
            log.info("📡 Iniciando simulación - Fecha: {}, FactorK: {}", 
                     request.getFecha(), request.getFactorK());
            
            // Generar ID único de sesión
            String sessionId = UUID.randomUUID().toString();
            String topicUrl = "/topic/simulations/" + sessionId;
            
            // Iniciar simulación en hilo aparte (no bloqueante)
            simulationService.startSimulation(
                sessionId, 
                request.getFecha(), 
                request.getFactorK()
            );
            
            // Responder inmediatamente al cliente
            SimulationStartResponse response = new SimulationStartResponse(
                sessionId,
                "Simulación iniciada exitosamente",
                topicUrl
            );
            
            log.info("✅ Simulación iniciada - SessionId: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error iniciando simulación", e);
            return ResponseEntity.status(500).body(
                new SimulationStartResponse(
                    null,
                    "Error: " + e.getMessage(),
                    null
                )
            );
        }
    }

    /**
     * Cancela una simulación en curso
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<?> cancelSimulation(@PathVariable String sessionId) {
        try {
            log.info("🛑 Cancelando simulación: {}", sessionId);
            
            simulationService.cancelSimulation(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Simulación cancelada exitosamente",
                "sessionId", sessionId,
                "subscriptionTopic", "/topic/simulations/" + sessionId
            ));
            
        } catch (Exception e) {
            log.error("❌ Error cancelando simulación", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage(),
                "sessionId", sessionId
            ));
        }
    }

    /**
     * Pausa una simulación (opcional)
     */
    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<?> pauseSimulation(@PathVariable String sessionId) {
        try {
            log.info("⏸️ Pausando simulación: {}", sessionId);
            simulationService.pauseSimulation(sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Simulación pausada"));
        } catch (Exception e) {
            log.error("❌ Error pausando simulación", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Reanuda una simulación pausada (opcional)
     */
    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<?> resumeSimulation(@PathVariable String sessionId) {
        try {
            log.info("▶️ Reanudando simulación: {}", sessionId);
            simulationService.resumeSimulation(sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Simulación reanudada"));
        } catch (Exception e) {
            log.error("❌ Error reanudando simulación", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
```

---

## 🔧 Service

### SimulationService.java

```java
package com.tuempresa.service;

import com.tuempresa.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Map para controlar simulaciones activas
    private final Map<String, Boolean> activeSimulations = new ConcurrentHashMap<>();

    /**
     * Inicia una simulación en un hilo separado
     */
    public void startSimulation(String sessionId, String fecha, int factorK) {
        activeSimulations.put(sessionId, true);
        
        executorService.submit(() -> {
            try {
                ejecutarSimulacion(sessionId, fecha, factorK);
            } catch (Exception e) {
                log.error("❌ Error en simulación: {}", sessionId, e);
                enviarError(sessionId, e.getMessage());
            }
        });
    }

    /**
     * Lógica principal de la simulación
     */
    private void ejecutarSimulacion(String sessionId, String fecha, int factorK) {
        log.info("🚀 Ejecutando simulación: {}", sessionId);
        
        int maxGeneraciones = 20;
        int pedidosTotales = 4440;
        
        for (int gen = 1; gen <= maxGeneraciones; gen++) {
            // Verificar si fue cancelada
            if (!activeSimulations.getOrDefault(sessionId, false)) {
                log.info("🛑 Simulación cancelada: {}", sessionId);
                break;
            }
            
            // ========== AQUÍ VA TU LÓGICA DEL ALGORITMO GENÉTICO ==========
            
            // Simular procesamiento de una generación
            try {
                Thread.sleep(2000); // Simula tiempo de procesamiento
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            // Calcular progreso
            double progreso = (gen * 100.0) / maxGeneraciones;
            double mejorFitness = 1000 + (gen * 50.5); // Ejemplo: mejora con cada generación
            double fitnessPromedio = mejorFitness * 0.7;
            int pedidosProcesados = (int) ((gen * 1.0 / maxGeneraciones) * pedidosTotales);
            
            // Crear solución de ejemplo
            SolucionDTO solucion = crearSolucionEjemplo(gen);
            
            // Enviar progreso por WebSocket
            ProgresoAGDTO progreso = ProgresoAGDTO.builder()
                .sessionId(sessionId)
                .tipo("PROGRESO_AG")
                .generacion(gen)
                .maxGeneraciones(maxGeneraciones)
                .progreso(progreso)
                .mejorFitness(mejorFitness)
                .fitnessPromedio(fitnessPromedio)
                .solucion(solucion)
                .fechaSimulada(fecha + "T" + String.format("%02d:00:00", gen))
                .timestamp(LocalDateTime.now())
                .pedidosProcesados(pedidosProcesados)
                .pedidosTotales(pedidosTotales)
                .mensaje("Generación " + gen + " completada")
                .build();
            
            enviarProgreso(sessionId, progreso);
            
            log.info("📊 Generación {}/{} - Fitness: {}", gen, maxGeneraciones, mejorFitness);
        }
        
        // Enviar mensaje de completado
        if (activeSimulations.getOrDefault(sessionId, false)) {
            enviarCompletado(sessionId, pedidosTotales);
            log.info("✅ Simulación completada: {}", sessionId);
        }
        
        // Limpiar
        activeSimulations.remove(sessionId);
    }

    /**
     * Crear solución de ejemplo (reemplaza con tu lógica real)
     */
    private SolucionDTO crearSolucionEjemplo(int generacion) {
        SolucionDTO solucion = new SolucionDTO();
        
        List<RutaDTO> rutas = new ArrayList<>();
        
        // Ejemplo: crear 3 rutas
        for (int i = 0; i < 3; i++) {
            RutaDTO ruta = new RutaDTO();
            ruta.setPedidoId((long) (generacion * 100 + i));
            ruta.setOrigen("KJFK");
            ruta.setDestino("EGLL");
            
            List<SubRutaDTO> subRutas = new ArrayList<>();
            
            // SubRuta 1: JFK → París
            SubRutaDTO subRuta1 = new SubRutaDTO();
            subRuta1.setOrigen("KJFK");
            subRuta1.setDestino("LFPG");
            subRuta1.setVuelo("AF00" + (i + 1));
            subRuta1.setHoraSalida("2025-01-02T10:00:00");
            subRuta1.setHoraLlegada("2025-01-02T22:00:00");
            subRutas.add(subRuta1);
            
            // SubRuta 2: París → Londres
            SubRutaDTO subRuta2 = new SubRutaDTO();
            subRuta2.setOrigen("LFPG");
            subRuta2.setDestino("EGLL");
            subRuta2.setVuelo("BA20" + (i + 1));
            subRuta2.setHoraSalida("2025-01-03T08:00:00");
            subRuta2.setHoraLlegada("2025-01-03T09:30:00");
            subRutas.add(subRuta2);
            
            ruta.setSubRutas(subRutas);
            rutas.add(ruta);
        }
        
        solucion.setRutas(rutas);
        
        // Métricas
        MetricasDTO metricas = new MetricasDTO();
        metricas.setTotalRutas(rutas.size());
        metricas.setTotalVuelos(rutas.size() * 2); // 2 sub-rutas por ruta
        metricas.setCostoTotal(45000.0);
        metricas.setTiempoPromedioEntrega(48.5);
        solucion.setMetricas(metricas);
        
        return solucion;
    }

    /**
     * Enviar mensaje de progreso por WebSocket
     */
    private void enviarProgreso(String sessionId, ProgresoAGDTO progreso) {
        String topic = "/topic/simulations/" + sessionId;
        messagingTemplate.convertAndSend(topic, progreso);
        log.debug("📤 Enviando progreso a topic: {}", topic);
    }

    /**
     * Enviar mensaje de completado
     */
    private void enviarCompletado(String sessionId, int pedidosTotales) {
        String topic = "/topic/simulations/" + sessionId;
        
        SimulationSnapshot snapshot = SimulationSnapshot.builder()
            .sessionId(sessionId)
            .status("COMPLETED")
            .iteration(888)
            .processedOrders(pedidosTotales)
            .totalOrders(pedidosTotales)
            .timestamp(LocalDateTime.now())
            .message("Simulación completada exitosamente")
            .build();
        
        messagingTemplate.convertAndSend(topic, snapshot);
        log.info("📤 Enviando mensaje de completado a topic: {}", topic);
    }

    /**
     * Enviar mensaje de error
     */
    private void enviarError(String sessionId, String errorMessage) {
        String topic = "/topic/simulations/" + sessionId;
        
        Map<String, Object> errorMsg = Map.of(
            "sessionId", sessionId,
            "tipo", "ERROR",
            "mensaje", errorMessage,
            "timestamp", LocalDateTime.now()
        );
        
        messagingTemplate.convertAndSend(topic, errorMsg);
        log.error("📤 Enviando mensaje de error a topic: {}", topic);
    }

    /**
     * Cancelar simulación
     */
    public void cancelSimulation(String sessionId) {
        activeSimulations.put(sessionId, false);
        log.info("🛑 Simulación marcada para cancelación: {}", sessionId);
    }

    /**
     * Pausar simulación (implementar según necesidad)
     */
    public void pauseSimulation(String sessionId) {
        // Implementar lógica de pausa
        log.info("⏸️ Simulación pausada: {}", sessionId);
    }

    /**
     * Reanudar simulación (implementar según necesidad)
     */
    public void resumeSimulation(String sessionId) {
        // Implementar lógica de reanudación
        log.info("▶️ Simulación reanudada: {}", sessionId);
    }
}
```

---

## 📨 Envío de Mensajes

### Desde cualquier parte del código:

```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

// Enviar mensaje a un topic específico
public void enviarMensaje(String sessionId, Object mensaje) {
    String topic = "/topic/simulations/" + sessionId;
    messagingTemplate.convertAndSend(topic, mensaje);
}
```

---

## 🎯 Ejemplo Completo de Uso

### Flujo Completo:

1. Cliente envía `POST /api/simulations/start`
2. Backend responde con `sessionId`
3. Backend inicia hilo con Algoritmo Genético
4. Cada generación, backend envía `ProgresoAGDTO` por WebSocket
5. Cliente recibe y actualiza UI
6. Al terminar, backend envía `SimulationSnapshot` con `status: "COMPLETED"`
7. Cliente puede cancelar en cualquier momento con `POST /api/simulations/{sessionId}/cancel`

---

## 🔍 Testing

### Prueba del endpoint REST:

```bash
curl -X POST http://localhost:8000/api/simulations/start \
  -H "Content-Type: application/json" \
  -d '{"fecha":"2025-01-02","factorK":5}'
```

**Respuesta esperada:**
```json
{
  "sessionId": "5c79a813-2e16-4d30-9c9e-639e445a5619",
  "mensaje": "Simulación iniciada exitosamente",
  "topicUrl": "/topic/simulations/5c79a813-2e16-4d30-9c9e-639e445a5619"
}
```

---

## ⚠️ NOTAS IMPORTANTES

1. **CORS**: Asegúrate de configurar CORS correctamente:
   ```java
   .setAllowedOrigins("http://localhost:3000")
   ```

2. **Thread Safety**: Usa `ConcurrentHashMap` para estado compartido entre hilos

3. **Limpieza**: Siempre limpia recursos al finalizar o cancelar

4. **Logging**: Usa logs detallados para debugging

5. **Error Handling**: Captura y envía errores por WebSocket

---

## 🚀 Resultado Final

Con esta configuración tendrás:

- ✅ WebSocket STOMP funcional
- ✅ REST API para control
- ✅ Envío de mensajes en tiempo real
- ✅ Manejo de múltiples sesiones
- ✅ Cancelación de simulaciones
- ✅ CORS configurado correctamente

---

**🎉 ¡Backend listo para integrarse con el frontend!**

**Fecha**: 26 de noviembre de 2025  
**Versión**: 1.0 - Backend Spring Boot + WebSocket STOMP
