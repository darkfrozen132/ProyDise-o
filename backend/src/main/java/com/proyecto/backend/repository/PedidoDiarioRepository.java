package com.proyecto.backend.repository;

import com.proyecto.backend.model.PedidoDiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoDiarioRepository extends JpaRepository<PedidoDiario, Long> {

    // NOTA: El campo 'estado' ya no existe en la entidad.
    // El estado se maneja en RAM mediante SessionStateManager.

    // Buscar pedidos por aeropuerto destino
    List<PedidoDiario> findByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Buscar pedidos por cliente
    List<PedidoDiario> findByClienteId(String clienteId);

    // Buscar pedidos por día
    List<PedidoDiario> findByDia(int dia);

    // Contar pedidos por aeropuerto destino
    long countByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Eliminar todos los pedidos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM pedidos_diario", nativeQuery = true)
    void deleteAllNative();

    /**
     * Carga pedidos diarios de un rango de fechas.
     * Excluye pedidos con destino a HUBS/SEDES.
     * 
     * @param anioInicio Anio de inicio
     * @param mesInicio Mes de inicio (1-12)
     * @param diaInicio Dia de inicio (1-31)
     * @param anioFin Anio de fin
     * @param mesFin Mes de fin
     * @param diaFin Dia de fin
     * @param destinosExcluidos Lista de codigos de aeropuertos a excluir (SPIM, EBCI, UBBB)
     * @return Lista de pedidos que NO van a hubs/sedes
     */
    @Query("SELECT p FROM PedidoDiario p WHERE " +
           "p.aeropuertoDestinoId NOT IN :destinosExcluidos AND " +
           "(" +
           "  (p.anio > :anioInicio OR (p.anio = :anioInicio AND p.mes > :mesInicio) OR (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia >= :diaInicio))" +
           ") AND (" +
           "  (p.anio < :anioFin OR (p.anio = :anioFin AND p.mes < :mesFin) OR (p.anio = :anioFin AND p.mes = :mesFin AND p.dia <= :diaFin))" +
           ")" +
           " ORDER BY p.anio, p.mes, p.dia, p.hora, p.minuto")
    List<PedidoDiario> findPedidosDiarios(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("diaInicio") int diaInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("diaFin") int diaFin,
            @Param("destinosExcluidos") List<String> destinosExcluidos
    );

    /**
     * Cuenta pedidos diarios de un rango (para estadisticas sin cargar datos)
     */
    @Query("SELECT COUNT(p) FROM PedidoDiario p WHERE " +
           "p.aeropuertoDestinoId NOT IN :destinosExcluidos AND " +
           "(" +
           "  (p.anio > :anioInicio OR (p.anio = :anioInicio AND p.mes > :mesInicio) OR (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia >= :diaInicio))" +
           ") AND (" +
           "  (p.anio < :anioFin OR (p.anio = :anioFin AND p.mes < :mesFin) OR (p.anio = :anioFin AND p.mes = :mesFin AND p.dia <= :diaFin))" +
           ")")
    long countPedidosDiarios(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("diaInicio") int diaInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("diaFin") int diaFin,
            @Param("destinosExcluidos") List<String> destinosExcluidos
    );

    /**
     * Busca pedidos diarios en un rango de fecha/hora, excluyendo destinos hubs.
     * Usado por AlgoritmoGeneticoService para cargar pedidos por ventana de tiempo.
     */
    @Query("SELECT p FROM PedidoDiario p WHERE " +
           "p.aeropuertoDestinoId NOT IN :destinosExcluidos AND " +
           "(" +
           "  (p.anio > :anioInicio) OR " +
           "  (p.anio = :anioInicio AND p.mes > :mesInicio) OR " +
           "  (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia > :diaInicio) OR " +
           "  (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia = :diaInicio AND p.hora > :horaInicio) OR " +
           "  (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia = :diaInicio AND p.hora = :horaInicio AND p.minuto >= :minutoInicio)" +
           ") AND (" +
           "  (p.anio < :anioFin) OR " +
           "  (p.anio = :anioFin AND p.mes < :mesFin) OR " +
           "  (p.anio = :anioFin AND p.mes = :mesFin AND p.dia < :diaFin) OR " +
           "  (p.anio = :anioFin AND p.mes = :mesFin AND p.dia = :diaFin AND p.hora < :horaFin) OR " +
           "  (p.anio = :anioFin AND p.mes = :mesFin AND p.dia = :diaFin AND p.hora = :horaFin AND p.minuto <= :minutoFin)" +
           ")")
    List<PedidoDiario> findByRangoFechaExcluyendoDestinos(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("diaInicio") int diaInicio,
            @Param("horaInicio") int horaInicio,
            @Param("minutoInicio") int minutoInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("diaFin") int diaFin,
            @Param("horaFin") int horaFin,
            @Param("minutoFin") int minutoFin,
            @Param("destinosExcluidos") List<String> destinosExcluidos
    );

}
