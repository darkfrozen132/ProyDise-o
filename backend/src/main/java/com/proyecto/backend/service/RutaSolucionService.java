package com.proyecto.backend.service;

import com.proyecto.backend.model.RutaSolucion;
import com.proyecto.backend.model.VueloPedido;
import com.proyecto.backend.planificador.semanal.dto.response.VueloEnRutaDTO;
import com.proyecto.backend.repository.RutaSolucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para convertir y guardar datos de vuelos del SSE en la base de datos
 * 
 * Convierte los DTOs de VueloEnRutaDTO (del SSE) a entidades RutaSolucion
 * y los persiste en la base de datos
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RutaSolucionService {

    private final RutaSolucionRepository rutaSolucionRepository;

    /**
     * Guarda o actualiza una lista de vuelos desde el SSE
     * 
     * @param vuelosDTO Lista de vuelos del SSE
     * @return Lista de rutas guardadas
     */
    @Transactional
    public List<RutaSolucion> guardarVuelosDesdeSSE(List<VueloEnRutaDTO> vuelosDTO) {
        log.info("Guardando {} vuelos desde SSE", vuelosDTO.size());
        
        List<RutaSolucion> rutasGuardadas = new ArrayList<>();
        
        for (VueloEnRutaDTO vueloDTO : vuelosDTO) {
            RutaSolucion ruta = convertirYGuardar(vueloDTO);
            rutasGuardadas.add(ruta);
        }
        
        log.info("Se guardaron {} rutas exitosamente", rutasGuardadas.size());
        return rutasGuardadas;
    }

    /**
     * Convierte un VueloEnRutaDTO del SSE a RutaSolucion y lo guarda
     * 
     * @param vueloDTO DTO del vuelo desde el SSE
     * @return RutaSolucion guardada
     */
    @Transactional
    public RutaSolucion convertirYGuardar(VueloEnRutaDTO vueloDTO) {
        // Buscar si ya existe este vuelo (por ID único)
        RutaSolucion ruta = rutaSolucionRepository.findByIdVuelo(vueloDTO.getId())
            .orElse(new RutaSolucion());
        
        // Mapear datos básicos del vuelo
        ruta.setIdVuelo(vueloDTO.getId());
        ruta.setOriginCode(vueloDTO.getOriginCode());
        ruta.setDestinationCode(vueloDTO.getDestinationCode());
        ruta.setSalida(vueloDTO.getSalida());
        ruta.setLlegada(vueloDTO.getLlegada());
        ruta.setCapacidad(vueloDTO.getCapacidad());
        ruta.setAltitude(vueloDTO.getAltitude());
        ruta.setRegionOrigin(vueloDTO.getRegionOrigin());
        ruta.setRegionDestination(vueloDTO.getRegionDestination());
        
        // Mapear coordenadas de ruta
        if (vueloDTO.getRuta() != null) {
            if (vueloDTO.getRuta().getOrigin() != null) {
                ruta.setOriginLat(vueloDTO.getRuta().getOrigin().getLat());
                ruta.setOriginLng(vueloDTO.getRuta().getOrigin().getLng());
            }
            
            if (vueloDTO.getRuta().getDestination() != null) {
                ruta.setDestinationLat(vueloDTO.getRuta().getDestination().getLat());
                ruta.setDestinationLng(vueloDTO.getRuta().getDestination().getLng());
            }
        }
        
        // Inicializar coordenadas actuales en el origen
        if (ruta.getCurrentLat() == null && ruta.getOriginLat() != null) {
            ruta.setCurrentLat(ruta.getOriginLat());
            ruta.setCurrentLng(ruta.getOriginLng());
            ruta.setProgreso(0.0);
            ruta.setEnVuelo(false);
        }
        
        // Limpiar orders existentes (si es actualización)
        if (ruta.getId() != null) {
            ruta.getOrders().clear();
        }
        
        // Mapear orders (pedidos)
        if (vueloDTO.getOrders() != null) {
            for (VueloEnRutaDTO.OrdenVuelo ordenDTO : vueloDTO.getOrders()) {
                VueloPedido order = new VueloPedido(
                    ordenDTO.getOrderId(),
                    ordenDTO.getCantidad()
                );
                ruta.agregarOrder(order);
            }
        }
        
        // Guardar en base de datos
        RutaSolucion rutaGuardada = rutaSolucionRepository.save(ruta);
        
        log.debug("Ruta guardada: id={}, idVuelo={}, orders={}", 
            rutaGuardada.getId(), 
            rutaGuardada.getIdVuelo(), 
            rutaGuardada.getOrders().size());
        
        return rutaGuardada;
    }

    /**
     * Actualiza las coordenadas actuales de un vuelo en progreso
     * 
     * @param idVuelo ID del vuelo
     * @param currentLat Latitud actual
     * @param currentLng Longitud actual
     * @param progreso Progreso (0.0 a 100.0)
     */
    @Transactional
    public void actualizarPosicionVuelo(String idVuelo, Double currentLat, Double currentLng, Double progreso) {
        rutaSolucionRepository.findByIdVuelo(idVuelo).ifPresent(ruta -> {
            ruta.setCurrentLat(currentLat);
            ruta.setCurrentLng(currentLng);
            ruta.setProgreso(progreso);
            ruta.setEnVuelo(progreso > 0 && progreso < 100);
            
            rutaSolucionRepository.save(ruta);
            
            log.debug("Posición actualizada: idVuelo={}, lat={}, lng={}, progreso={:.2f}%", 
                idVuelo, currentLat, currentLng, progreso);
        });
    }

    /**
     * Marca un vuelo como completado
     * 
     * @param idVuelo ID del vuelo
     */
    @Transactional
    public void marcarVueloCompletado(String idVuelo) {
        rutaSolucionRepository.findByIdVuelo(idVuelo).ifPresent(ruta -> {
            ruta.setCurrentLat(ruta.getDestinationLat());
            ruta.setCurrentLng(ruta.getDestinationLng());
            ruta.setProgreso(100.0);
            ruta.setEnVuelo(false);
            
            rutaSolucionRepository.save(ruta);
            
            log.info("Vuelo completado: idVuelo={}", idVuelo);
        });
    }

    /**
     * Obtiene todos los vuelos actualmente en progreso
     * 
     * @return Lista de rutas en vuelo
     */
    @Transactional(readOnly = true)
    public List<RutaSolucion> obtenerVuelosEnProgreso() {
        return rutaSolucionRepository.findByEnVueloTrue();
    }

    /**
     * Obtiene un vuelo por su ID
     * 
     * @param idVuelo ID del vuelo
     * @return RutaSolucion si existe
     */
    @Transactional(readOnly = true)
    public RutaSolucion obtenerVueloPorId(String idVuelo) {
        return rutaSolucionRepository.findByIdVuelo(idVuelo)
            .orElse(null);
    }
}
