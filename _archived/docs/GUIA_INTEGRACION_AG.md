# 🎯 Guía de Integración del Algoritmo Genético

## 📋 Resumen

Este documento describe cómo integrar el algoritmo genético real con el sistema de simulación por WebSocket.

---

## 🏗️ Paso 1: Definir Interfaz del Algoritmo Genético

Crear la interfaz que debe implementar el AG:

```java
package com.proyecto.backend.simulation.algorithm;

import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.Builder;
import java.util.List;

/**
 * Interfaz para el Algoritmo Genético de planificación de rutas
 */
public interface GeneticAlgorithm {

    /**
     * Ejecuta el algoritmo genético
     * 
     * @param context Contexto con datos de entrada
     * @return Resultado con rutas planificadas
     */
    GAResult execute(GAContext context);

    /**
     * Contexto de ejecución del AG
     */
    @Builder
    record GAContext(
        List<Pedido> pendingOrders,      // Pedidos pendientes
        List<PlanDeVuelo> availableFlights, // Vuelos disponibles
        GAConfiguration configuration     // Configuración del AG
    ) {}

    /**
     * Configuración del Algoritmo Genético
     */
    @Builder
    record GAConfiguration(
        int populationSize,              // Tamaño de población
        int maxGenerations,              // Máximo de generaciones
        int stagnationLimit,             // Generaciones sin mejora
        double mutationRate,             // Tasa de mutación
        double crossoverRate             // Tasa de crossover
    ) {
        public static GAConfiguration fromRequest(
                Integer populationSize,
                Integer maxGenerations,
                Integer stagnationLimit) {
            return GAConfiguration.builder()
                    .populationSize(populationSize != null ? populationSize : 20)
                    .maxGenerations(maxGenerations != null ? maxGenerations : 20)
                    .stagnationLimit(stagnationLimit != null ? stagnationLimit : 10)
                    .mutationRate(0.1)
                    .crossoverRate(0.8)
                    .build();
        }
    }

    /**
     * Resultado del algoritmo genético
     */
    @Builder
    record GAResult(
        List<PlannedRoute> routes,       // Rutas planificadas
        int processedOrders,             // Pedidos procesados
        double fitness,                  // Fitness de la solución
        GAMetrics metrics                // Métricas del AG
    ) {}

    /**
     * Ruta planificada por el AG
     */
    @Builder
    record PlannedRoute(
        PlanDeVuelo flightPlan,          // Plan de vuelo base
        List<Pedido> assignedOrders,     // Pedidos asignados
        String flightCode,               // Código único del vuelo
        int totalLoad,                   // Carga total
        double utilization               // % de utilización
    ) {}

    /**
     * Métricas del AG
     */
    @Builder
    record GAMetrics(
        int generationsExecuted,         // Generaciones ejecutadas
        int evaluationsCount,            // Evaluaciones totales
        double convergenceRate,          // Tasa de convergencia
        long executionTimeMs             // Tiempo de ejecución
    ) {}
}
```

---

## 🔧 Paso 2: Implementar el Algoritmo Genético

Crear implementación real del AG:

```java
package com.proyecto.backend.simulation.algorithm;

import com.proyecto.backend.model.Pedido;
import com.proyecto.backend.model.PlanDeVuelo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementación del Algoritmo Genético para planificación de rutas
 */
@Slf4j
@Component
public class GeneticAlgorithmImpl implements GeneticAlgorithm {

    @Override
    public GAResult execute(GAContext context) {
        long startTime = System.currentTimeMillis();
        
        log.debug("🧬 Ejecutando AG con {} pedidos y {} vuelos",
                context.pendingOrders().size(),
                context.availableFlights().size());

        // 1. Inicializar población
        List<Solution> population = initializePopulation(context);

        // 2. Evolucionar
        Solution bestSolution = evolve(population, context);

        // 3. Construir resultado
        List<PlannedRoute> routes = buildRoutes(bestSolution, context);
        
        long executionTime = System.currentTimeMillis() - startTime;

        return GAResult.builder()
                .routes(routes)
                .processedOrders(countProcessedOrders(bestSolution))
                .fitness(bestSolution.getFitness())
                .metrics(GAMetrics.builder()
                        .generationsExecuted(context.configuration().maxGenerations())
                        .evaluationsCount(population.size() * context.configuration().maxGenerations())
                        .convergenceRate(0.95)
                        .executionTimeMs(executionTime)
                        .build())
                .build();
    }

    /**
     * Inicializa la población inicial
     */
    private List<Solution> initializePopulation(GAContext context) {
        List<Solution> population = new ArrayList<>();
        
        for (int i = 0; i < context.configuration().populationSize(); i++) {
            Solution solution = createRandomSolution(context);
            population.add(solution);
        }
        
        return population;
    }

    /**
     * Proceso evolutivo principal
     */
    private Solution evolve(List<Solution> population, GAContext context) {
        Solution bestSolution = null;
        int generationsWithoutImprovement = 0;

        for (int gen = 0; gen < context.configuration().maxGenerations(); gen++) {
            // Evaluar fitness
            population.forEach(this::evaluateFitness);

            // Ordenar por fitness
            population.sort(Comparator.comparingDouble(Solution::getFitness).reversed());

            // Mejor solución
            Solution currentBest = population.get(0);
            
            if (bestSolution == null || currentBest.getFitness() > bestSolution.getFitness()) {
                bestSolution = currentBest;
                generationsWithoutImprovement = 0;
            } else {
                generationsWithoutImprovement++;
            }

            // Criterio de parada
            if (generationsWithoutImprovement >= context.configuration().stagnationLimit()) {
                log.debug("🛑 Convergencia alcanzada en generación {}", gen);
                break;
            }

            // Selección
            List<Solution> selected = selection(population);

            // Crossover
            List<Solution> offspring = crossover(selected, context);

            // Mutación
            mutation(offspring, context);

            // Nueva población
            population = offspring;
        }

        return bestSolution;
    }

    /**
     * Crea una solución aleatoria
     */
    private Solution createRandomSolution(GAContext context) {
        // TODO: Implementar generación de solución aleatoria
        return new Solution();
    }

    /**
     * Evalúa el fitness de una solución
     */
    private void evaluateFitness(Solution solution) {
        // TODO: Implementar cálculo de fitness real
        // Criterios: minimizar retrasos, maximizar utilización, etc.
        double fitness = Math.random() * 1000;
        solution.setFitness(fitness);
    }

    /**
     * Selección de individuos
     */
    private List<Solution> selection(List<Solution> population) {
        // TODO: Implementar selección por torneo o ruleta
        return new ArrayList<>(population.subList(0, population.size() / 2));
    }

    /**
     * Operador de crossover
     */
    private List<Solution> crossover(List<Solution> parents, GAContext context) {
        // TODO: Implementar crossover (OX, PMX, etc.)
        return new ArrayList<>(parents);
    }

    /**
     * Operador de mutación
     */
    private void mutation(List<Solution> population, GAContext context) {
        // TODO: Implementar mutación (swap, insert, etc.)
    }

    /**
     * Construye las rutas planificadas desde la solución
     */
    private List<PlannedRoute> buildRoutes(Solution solution, GAContext context) {
        // TODO: Convertir Solution a PlannedRoute
        return Collections.emptyList();
    }

    /**
     * Cuenta pedidos procesados
     */
    private int countProcessedOrders(Solution solution) {
        // TODO: Contar pedidos en la solución
        return 0;
    }

    /**
     * Clase interna para representar una solución
     */
    private static class Solution {
        private double fitness;
        private Map<PlanDeVuelo, List<Pedido>> assignments = new HashMap<>();

        public double getFitness() {
            return fitness;
        }

        public void setFitness(double fitness) {
            this.fitness = fitness;
        }

        public Map<PlanDeVuelo, List<Pedido>> getAssignments() {
            return assignments;
        }
    }
}
```

---

## 🔌 Paso 3: Integrar con SimulationService

Modificar `SimulationService.java`:

```java
package com.proyecto.backend.simulation.service;

import com.proyecto.backend.simulation.algorithm.GeneticAlgorithm;
import com.proyecto.backend.simulation.algorithm.GeneticAlgorithm.*;
// ... otros imports

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    // Inyectar el AG
    private final GeneticAlgorithm geneticAlgorithm;
    
    // ... otros campos

    /**
     * Ejecuta el algoritmo genético (VERSIÓN REAL)
     */
    private SimulationResult runGeneticAlgorithm(
            List<Pedido> orders,
            List<PlanDeVuelo> flights,
            SimulationRequest config) {
        
        // 1. Construir contexto
        GAContext context = GAContext.builder()
                .pendingOrders(orders)
                .availableFlights(flights)
                .configuration(GAConfiguration.fromRequest(
                        config.getPopulationSize(),
                        config.getMaxGenerations(),
                        config.getStagnationLimit()
                ))
                .build();

        // 2. Ejecutar AG
        GAResult result = geneticAlgorithm.execute(context);

        // 3. Convertir resultado
        return new SimulationResult(
                result.processedOrders(),
                result.fitness(),
                convertToRoutes(result.routes())
        );
    }

    /**
     * Convierte PlannedRoute a formato de salida
     */
    private List<SimulationRoute> convertToRoutes(List<PlannedRoute> routes) {
        return routes.stream()
                .map(route -> SimulationSnapshot.SimulationRoute.builder()
                        .id(route.flightPlan().getId())
                        .flightCode(route.flightCode())
                        .origin(route.flightPlan().getAeropuertoOrigen())
                        .destination(route.flightPlan().getAeropuertoDestino())
                        .departureTime(route.flightPlan().getHoraSalida().toString())
                        .arrivalTime(route.flightPlan().getHoraLlegada().toString())
                        .orders(convertOrders(route.assignedOrders()))
                        .totalLoad(route.totalLoad())
                        .maxCapacity(route.flightPlan().getCapacidadMaxima())
                        .utilization(route.utilization())
                        .build())
                .toList();
    }

    private List<SimulationSnapshot.OrderInfo> convertOrders(List<Pedido> orders) {
        return orders.stream()
                .map(order -> SimulationSnapshot.OrderInfo.builder()
                        .orderId(order.getId())
                        .clientId(order.getClienteId())
                        .destinationAirport(order.getAeropuertoDestinoId())
                        .quantity(order.getCantidadProductos())
                        .deadline(LocalDateTime.of(
                                order.getAnio(),
                                order.getMes(),
                                order.getDia(),
                                order.getHora(),
                                order.getMinuto()
                        ))
                        .build())
                .toList();
    }
}
```

---

## ✅ Paso 4: Verificar Integración

### Test de Integración

```java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimulationIntegrationTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PlanDeVueloRepository planDeVueloRepository;

    private static UUID sessionId;

    @Test
    @Order(1)
    void testFullSimulationCycle() throws InterruptedException {
        // 1. Crear datos de prueba
        createTestData();

        // 2. Iniciar simulación
        SimulationRequest request = new SimulationRequest();
        request.setAction("iniciar");
        request.setStartDate(LocalDate.of(2025, 1, 2));
        request.setFactorK(5);
        request.setPopulationSize(20);
        request.setMaxGenerations(20);

        sessionId = simulationService.startSimulation(request);
        assertNotNull(sessionId);

        // 3. Esperar a que complete (máximo 30 segundos)
        SimulationSnapshot snapshot = null;
        for (int i = 0; i < 30; i++) {
            Thread.sleep(1000);
            snapshot = simulationService.getSimulationStatus(sessionId);
            
            if (snapshot != null && 
                snapshot.status() == SimulationSnapshot.SimulationStatus.COMPLETED) {
                break;
            }
        }

        // 4. Verificar resultado
        assertNotNull(snapshot);
        assertEquals(SimulationSnapshot.SimulationStatus.COMPLETED, snapshot.status());
        assertTrue(snapshot.bestFitness() > 0);
    }

    private void createTestData() {
        // Crear pedidos de prueba
        for (int i = 0; i < 20; i++) {
            Pedido pedido = new Pedido();
            pedido.setAnio(2025);
            pedido.setMes(1);
            pedido.setDia(2);
            pedido.setHora(10 + (i / 4));
            pedido.setMinuto((i % 4) * 15);
            pedido.setAeropuertoDestinoId("DEST" + (i % 5));
            pedido.setCantidadProductos(10);
            pedido.setClienteId("CLI" + i);
            pedido.setEstado("PENDIENTE");
            pedidoRepository.save(pedido);
        }

        // Crear planes de vuelo
        for (int i = 0; i < 10; i++) {
            PlanDeVuelo vuelo = new PlanDeVuelo();
            vuelo.setAeropuertoOrigen("ORIGEN");
            vuelo.setAeropuertoDestino("DEST" + (i % 5));
            vuelo.setHoraSalida(LocalTime.of(8 + i, 0));
            vuelo.setHoraLlegada(LocalTime.of(10 + i, 0));
            vuelo.setCapacidadMaxima(300);
            planDeVueloRepository.save(vuelo);
        }
    }
}
```

---

## 📊 Paso 5: Monitorear Desempeño

### Agregar Métricas

```java
@Service
public class SimulationMetricsService {

    private final Map<UUID, SimulationMetrics> metricsMap = new ConcurrentHashMap<>();

    public void recordIteration(UUID sessionId, long durationMs, double fitness) {
        metricsMap.computeIfAbsent(sessionId, k -> new SimulationMetrics())
                .addIteration(durationMs, fitness);
    }

    public SimulationMetrics getMetrics(UUID sessionId) {
        return metricsMap.get(sessionId);
    }

    @Data
    public static class SimulationMetrics {
        private int totalIterations = 0;
        private long totalDurationMs = 0;
        private double averageDurationMs = 0;
        private double bestFitness = 0;
        private List<Double> fitnessHistory = new ArrayList<>();

        public void addIteration(long durationMs, double fitness) {
            totalIterations++;
            totalDurationMs += durationMs;
            averageDurationMs = (double) totalDurationMs / totalIterations;
            bestFitness = Math.max(bestFitness, fitness);
            fitnessHistory.add(fitness);
        }
    }
}
```

---

## 🎯 Checklist de Integración

- [ ] Implementar interfaz `GeneticAlgorithm`
- [ ] Crear clase `GeneticAlgorithmImpl` con lógica real
- [ ] Inyectar AG en `SimulationService`
- [ ] Modificar método `runGeneticAlgorithm()` para usar AG real
- [ ] Implementar conversión de `PlannedRoute` a `SimulationRoute`
- [ ] Crear tests de integración
- [ ] Probar con datos reales
- [ ] Optimizar parámetros del AG
- [ ] Agregar logging y métricas
- [ ] Documentar funcionamiento

---

## 🚀 Siguiente Nivel: Optimizaciones

### 1. Cache de Soluciones
```java
@Service
public class SolutionCacheService {
    private final Cache<String, GAResult> cache = 
            Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build();
}
```

### 2. Paralelización del AG
```java
private List<Solution> evaluatePopulation(List<Solution> population) {
    return population.parallelStream()
            .peek(this::evaluateFitness)
            .collect(Collectors.toList());
}
```

### 3. Métricas en Tiempo Real
```java
@Component
public class GeneticAlgorithmMetrics {
    private final Counter evaluationsCounter;
    private final Timer fitnessTimer;
    
    // Spring Boot Actuator metrics
}
```

---

**Estado:** ✅ Listo para implementar AG real  
**Prioridad:** Alta  
**Dificultad:** Media-Alta
