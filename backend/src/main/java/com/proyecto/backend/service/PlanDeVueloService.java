package com.proyecto.backend.service;

import com.proyecto.backend.model.PlanDeVuelo;
import com.proyecto.backend.repository.PlanDeVueloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanDeVueloService {

    private final PlanDeVueloRepository planDeVueloRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Obtiene todos los planes de vuelo
     */
    @Transactional(readOnly = true)
    public List<PlanDeVuelo> obtenerTodos() {
        return planDeVueloRepository.findAll();
    }

    /**
     * Busca un plan de vuelo por ID
     */
    @Transactional(readOnly = true)
    public PlanDeVuelo buscarPorId(Long id) {
        return planDeVueloRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan de vuelo no encontrado: " + id));
    }

    /**
     * Busca planes de vuelo por aeropuerto de origen
     */
    @Transactional(readOnly = true)
    public List<PlanDeVuelo> buscarPorOrigen(String aeropuertoOrigen) {
        return planDeVueloRepository.findByAeropuertoOrigen(aeropuertoOrigen);
    }

    /**
     * Busca planes de vuelo por aeropuerto de destino
     */
    @Transactional(readOnly = true)
    public List<PlanDeVuelo> buscarPorDestino(String aeropuertoDestino) {
        return planDeVueloRepository.findByAeropuertoDestino(aeropuertoDestino);
    }

    /**
     * Busca planes de vuelo entre dos aeropuertos
     */
    @Transactional(readOnly = true)
    public List<PlanDeVuelo> buscarPorRuta(String aeropuertoOrigen, String aeropuertoDestino) {
        return planDeVueloRepository.findByAeropuertoOrigenAndAeropuertoDestino(aeropuertoOrigen, aeropuertoDestino);
    }

    /**
     * Crea un nuevo plan de vuelo
     */
    @Transactional
    public PlanDeVuelo crear(PlanDeVuelo planDeVuelo) {
        log.info("Creando plan de vuelo: {} -> {}",
            planDeVuelo.getAeropuertoOrigen(),
            planDeVuelo.getAeropuertoDestino());
        return planDeVueloRepository.save(planDeVuelo);
    }

    /**
     * Actualiza un plan de vuelo existente
     */
    @Transactional
    public PlanDeVuelo actualizar(Long id, PlanDeVuelo planDeVuelo) {
        PlanDeVuelo existente = buscarPorId(id);

        existente.setAeropuertoOrigen(planDeVuelo.getAeropuertoOrigen());
        existente.setAeropuertoDestino(planDeVuelo.getAeropuertoDestino());
        existente.setHoraSalida(planDeVuelo.getHoraSalida());
        existente.setHoraLlegada(planDeVuelo.getHoraLlegada());
        existente.setCapacidadMaxima(planDeVuelo.getCapacidadMaxima());

        log.info("Actualizando plan de vuelo ID: {}", id);
        return planDeVueloRepository.save(existente);
    }

    /**
     * Elimina un plan de vuelo
     */
    @Transactional
    public void eliminar(Long id) {
        PlanDeVuelo planDeVuelo = buscarPorId(id);
        planDeVueloRepository.delete(planDeVuelo);
        log.info("Plan de vuelo eliminado: ID {}", id);
    }

    /**
     * Limpia todos los planes de vuelo de la base de datos con DELETE nativo optimizado
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void limpiarPlanesDeVuelo() {
        long count = planDeVueloRepository.count();
        planDeVueloRepository.deleteAllNative(); // DELETE masivo con SQL nativo
        log.info("Se eliminaron {} planes de vuelo de la base de datos (DELETE nativo)", count);
    }

    /**
     * Carga planes de vuelo desde el archivo de texto
     * IMPORTANTE: Limpia la BD antes de cargar para evitar duplicados
     * Formato: ORIGEN-DESTINO-HORA_SALIDA-HORA_LLEGADA-CAPACIDAD
     * Ejemplo: SKBO-SEQM-03:34-05:21-0300
     * @return Lista de planes de vuelo cargados
     */
    @Transactional
    public List<PlanDeVuelo> cargarDesdeArchivo() {
        // Limpiar la base de datos antes de cargar
        log.info("Limpiando planes de vuelo existentes...");
        limpiarPlanesDeVuelo();
        
        List<PlanDeVuelo> planesParaGuardar = new ArrayList<>();

        try {
            log.info("Iniciando lectura de archivo de planes de vuelo...");
            ClassPathResource resource = new ClassPathResource("datos/PlanesDeVuelo.txt");

            // PASO 1: Leer TODO el archivo primero (solo I/O, sin BD)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-8"))) {

                String linea;
                int lineaNumero = 0;

                while ((linea = reader.readLine()) != null) {
                    lineaNumero++;

                    // Saltar lineas vacias
                    if (linea.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        PlanDeVuelo planDeVuelo = parsearLineaPlanDeVuelo(linea);
                        if (planDeVuelo != null) {
                            planesParaGuardar.add(planDeVuelo);
                        }
                    } catch (Exception e) {
                        log.error("Error procesando linea {}: {} - {}", lineaNumero, linea, e.getMessage());
                    }
                }

                log.info("✓ Lectura completada: {} planes parseados de {} líneas", planesParaGuardar.size(), lineaNumero);
            }

            // PASO 2: Guardar con JDBC batch insert DIRECTO (mucho más rápido que JPA)
            if (!planesParaGuardar.isEmpty()) {
                log.info("Guardando {} planes de vuelo con JDBC batch insert...", planesParaGuardar.size());
                
                long inicio = System.currentTimeMillis();
                guardarConJdbcBatch(planesParaGuardar);
                long fin = System.currentTimeMillis();
                
                log.info("✓ {} planes de vuelo guardados en {} ms", planesParaGuardar.size(), (fin - inicio));
                return planesParaGuardar;
            } else {
                log.info("No hay planes de vuelo para guardar");
                return new ArrayList<>();
            }

        } catch (IOException e) {
            log.error("Error leyendo archivo de planes de vuelo: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar el archivo de planes de vuelo", e);
        }
    }

    /**
     * Guarda un batch de planes de vuelo en una transacción separada
     * Esto evita timeouts en transacciones muy largas
     */
    @Transactional
    private void guardarBatch(List<PlanDeVuelo> batch) {
        planDeVueloRepository.saveAll(batch);
    }

    /**
     * Guarda planes de vuelo usando JDBC batch insert directo (más rápido que JPA)
     * MySQL con rewriteBatchedStatements=true convierte múltiples INSERT en uno solo
     */
    @Transactional
    private void guardarConJdbcBatch(List<PlanDeVuelo> planes) {
        String sql = "INSERT INTO planesdevuelo (aeropuerto_origen, aeropuerto_destino, hora_salida, hora_llegada, capacidad_maxima) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, planes, 500, (PreparedStatement ps, PlanDeVuelo plan) -> {
            ps.setString(1, plan.getAeropuertoOrigen());
            ps.setString(2, plan.getAeropuertoDestino());
            ps.setObject(3, plan.getHoraSalida());
            ps.setObject(4, plan.getHoraLlegada());
            ps.setInt(5, plan.getCapacidadMaxima());
        });
    }

    /**
     * Parsea una linea del archivo y crea un objeto PlanDeVuelo
     * Formato: ORIGEN-DESTINO-HORA_SALIDA-HORA_LLEGADA-CAPACIDAD
     * Ejemplo: SKBO-SEQM-03:34-05:21-0300
     */
    private PlanDeVuelo parsearLineaPlanDeVuelo(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }

        try {
            // Dividir la linea por guiones
            String[] partes = linea.trim().split("-");

            if (partes.length != 5) {
                log.warn("Linea con formato invalido (esperado 5 partes): {}", linea);
                return null;
            }

            String aeropuertoOrigen = partes[0].trim();
            String aeropuertoDestino = partes[1].trim();
            LocalTime horaSalida = LocalTime.parse(partes[2].trim());
            LocalTime horaLlegada = LocalTime.parse(partes[3].trim());
            int capacidadMaxima = Integer.parseInt(partes[4].trim());

            return new PlanDeVuelo(aeropuertoOrigen, aeropuertoDestino,
                                  horaSalida, horaLlegada, capacidadMaxima);

        } catch (Exception e) {
            log.error("Error parseando linea: {} - {}", linea, e.getMessage());
            return null;
        }
    }

    /**
     * Recarga planes de vuelo: limpia la BD y carga desde el archivo
     */
    @Transactional
    public List<PlanDeVuelo> recargarDesdeArchivo() {
        log.info("Iniciando recarga de planes de vuelo...");
        limpiarPlanesDeVuelo();
        return cargarDesdeArchivo();
    }

}
