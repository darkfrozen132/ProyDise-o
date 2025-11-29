package com.proyecto.backend.simulation.service;

import com.proyecto.backend.model.PedidoSemanal;
import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.repository.PedidoSemanalRepository;
import com.proyecto.backend.repository.PlanDeVueloRepository;
import com.proyecto.backend.simulation.dto.SimulationRequest;
import com.proyecto.backend.simulation.dto.SimulationSnapshot;
import com.proyecto.backend.simulation.session.SimulationSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SimulationService
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private PedidoSemanalRepository pedidoSemanalRepository;

    @Mock
    private PlanDeVueloRepository planDeVueloRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SimulationService simulationService;

    private SimulationRequest mockRequest;
    private List<PedidoSemanal> mockOrders;
    private List<PlanDeVuelo> mockFlights;

    @BeforeEach
    void setUp() {
        // Configurar request de prueba
        mockRequest = new SimulationRequest();
        mockRequest.setAction("iniciar");
        mockRequest.setStartDate(LocalDate.of(2025, 1, 2));
        mockRequest.setFactorK(5);
        mockRequest.setPopulationSize(20);
        mockRequest.setMaxGenerations(20);
        mockRequest.setStagnationLimit(10);

        // Configurar pedidos de prueba
        mockOrders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PedidoSemanal pedido = new PedidoSemanal();
            pedido.setId((long) i);
            pedido.setAnio(2025);
            pedido.setMes(1);
            pedido.setDia(2);
            pedido.setHora(10);
            pedido.setMinuto(i * 5);
            pedido.setAeropuertoDestinoId("SKBO");
            pedido.setCantidadProductos(10);
            pedido.setClienteId("CLI00" + i);
            // Ya no hay campo estado - el estado se maneja en RAM
            mockOrders.add(pedido);
        }

        // Configurar vuelos de prueba
        mockFlights = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PlanDeVuelo vuelo = new PlanDeVuelo();
            vuelo.setId((long) i);
            vuelo.setAeropuertoOrigen("SKBO");
            vuelo.setAeropuertoDestino("SEQM");
            vuelo.setHoraSalida(LocalTime.of(8 + i, 0));
            vuelo.setHoraLlegada(LocalTime.of(10 + i, 0));
            vuelo.setCapacidadMaxima(300);
            mockFlights.add(vuelo);
        }
    }

    @Test
    void testStartSimulation_Success() {
        // Configurar mocks
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);

        // Ejecutar
        UUID sessionId = simulationService.startSimulation(mockRequest);

        // Verificar
        assertNotNull(sessionId, "SessionId no debe ser null");
        
        Map<UUID, SimulationSession> sessions = simulationService.getActiveSessions();
        assertTrue(sessions.containsKey(sessionId), "La sesión debe estar en el mapa");
        
        SimulationSession session = sessions.get(sessionId);
        assertEquals(sessionId, session.getSessionId());
        assertTrue(session.isRunning(), "La sesión debe estar running");
        
        // Verificar que se cargaron los datos
        verify(pedidoSemanalRepository, times(1)).findAll();
        verify(planDeVueloRepository, times(1)).findAll();
    }

    @Test
    void testCancelSimulation_Success() throws InterruptedException {
        // Iniciar simulación
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        UUID sessionId = simulationService.startSimulation(mockRequest);
        
        // Esperar un poco para que inicie
        Thread.sleep(100);
        
        // Cancelar
        boolean cancelled = simulationService.cancelSimulation(sessionId);
        
        // Verificar
        assertTrue(cancelled, "La cancelación debe ser exitosa");
        
        SimulationSession session = simulationService.getActiveSessions().get(sessionId);
        assertNotNull(session);
        assertFalse(session.isRunning(), "La sesión no debe estar running");
    }

    @Test
    void testCancelSimulation_NotFound() {
        // Intentar cancelar sesión inexistente
        UUID fakeId = UUID.randomUUID();
        boolean cancelled = simulationService.cancelSimulation(fakeId);
        
        // Verificar
        assertFalse(cancelled, "No debe cancelar una sesión inexistente");
    }

    @Test
    void testPauseSimulation_Success() throws InterruptedException {
        // Iniciar simulación
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        UUID sessionId = simulationService.startSimulation(mockRequest);
        
        // Esperar un poco
        Thread.sleep(100);
        
        // Pausar
        boolean paused = simulationService.pauseSimulation(sessionId);
        
        // Verificar
        assertTrue(paused, "La pausa debe ser exitosa");
        
        SimulationSession session = simulationService.getActiveSessions().get(sessionId);
        assertTrue(session.isPaused(), "La sesión debe estar pausada");
    }

    @Test
    void testResumeSimulation_Success() throws InterruptedException {
        // Iniciar y pausar simulación
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        UUID sessionId = simulationService.startSimulation(mockRequest);
        Thread.sleep(100);
        simulationService.pauseSimulation(sessionId);
        
        // Reanudar
        boolean resumed = simulationService.resumeSimulation(sessionId);
        
        // Verificar
        assertTrue(resumed, "La reanudación debe ser exitosa");
        
        SimulationSession session = simulationService.getActiveSessions().get(sessionId);
        assertFalse(session.isPaused(), "La sesión no debe estar pausada");
    }

    @Test
    void testGetSimulationStatus_Success() throws InterruptedException {
        // Iniciar simulación
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        UUID sessionId = simulationService.startSimulation(mockRequest);
        
        // Esperar un poco para que se genere un snapshot
        Thread.sleep(200);
        
        // Obtener estado
        SimulationSnapshot snapshot = simulationService.getSimulationStatus(sessionId);
        
        // Verificar
        assertNotNull(snapshot, "El snapshot no debe ser null");
        assertEquals(sessionId, snapshot.simulationId());
    }

    @Test
    void testGetSimulationStatus_NotFound() {
        // Obtener estado de sesión inexistente
        UUID fakeId = UUID.randomUUID();
        SimulationSnapshot snapshot = simulationService.getSimulationStatus(fakeId);
        
        // Verificar
        assertNull(snapshot, "El snapshot debe ser null para sesión inexistente");
    }

    @Test
    void testGetActiveSessions() {
        // Iniciar múltiples simulaciones
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        UUID sessionId1 = simulationService.startSimulation(mockRequest);
        UUID sessionId2 = simulationService.startSimulation(mockRequest);
        
        // Obtener sesiones activas
        Map<UUID, SimulationSession> sessions = simulationService.getActiveSessions();
        
        // Verificar
        assertEquals(2, sessions.size(), "Debe haber 2 sesiones activas");
        assertTrue(sessions.containsKey(sessionId1));
        assertTrue(sessions.containsKey(sessionId2));
    }

    @Test
    void testCleanupStaleSessions() throws InterruptedException {
        // Este test requeriría mockear LocalDateTime para simular sesiones antiguas
        // Por ahora, solo verificamos que el método no lance excepciones
        assertDoesNotThrow(() -> simulationService.cleanupStaleSessions());
    }

    @Test
    void testMultipleSimultaneousSimulations() {
        // Iniciar múltiples simulaciones simultáneas
        when(pedidoSemanalRepository.findAll()).thenReturn(mockOrders);
        when(planDeVueloRepository.findAll()).thenReturn(mockFlights);
        
        List<UUID> sessionIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID sessionId = simulationService.startSimulation(mockRequest);
            sessionIds.add(sessionId);
        }
        
        // Verificar que todas están activas
        Map<UUID, SimulationSession> sessions = simulationService.getActiveSessions();
        assertEquals(5, sessions.size());
        
        for (UUID sessionId : sessionIds) {
            assertTrue(sessions.containsKey(sessionId));
            assertTrue(sessions.get(sessionId).isRunning());
        }
    }
}
