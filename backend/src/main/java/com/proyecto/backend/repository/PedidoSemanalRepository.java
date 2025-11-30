package com.proyecto.backend.repository;

import com.proyecto.backend.model.PedidoSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoSemanalRepository extends JpaRepository<PedidoSemanal, Long> {

    // Buscar pedidos por aeropuerto destino
    List<PedidoSemanal> findByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Buscar pedidos por cliente
    List<PedidoSemanal> findByClienteId(String clienteId);

    // Buscar pedidos por día
    List<PedidoSemanal> findByDia(int dia);

    // Contar pedidos por aeropuerto destino
    long countByAeropuertoDestinoId(String aeropuertoDestinoId);

    // Eliminar todos los pedidos con un solo DELETE nativo
    @Modifying
    @Query(value = "DELETE FROM pedidos_semanal", nativeQuery = true)
    void deleteAllNative();

    /**
     * 🆕 Busca pedidos en un rango de fecha/hora específico, EXCLUYENDO destinos que son hubs/sedes.
     * Esta query evita cargar millones de registros en memoria.
     * 
     * @param anioInicio Año de inicio
     * @param mesInicio Mes de inicio (1-12)
     * @param diaInicio Día de inicio (1-31)
     * @param horaInicio Hora de inicio (0-23)
     * @param minutoInicio Minuto de inicio (0-59)
     * @param anioFin Año de fin
     * @param mesFin Mes de fin (1-12)
     * @param diaFin Día de fin (1-31)
     * @param horaFin Hora de fin (0-23)
     * @param minutoFin Minuto de fin (0-59)
     * @param destinosExcluidos Lista de códigos de aeropuertos a excluir (hubs/sedes)
     * @return Lista de pedidos en el rango que NO van a destinos excluidos
     */
    @Query("SELECT p FROM PedidoSemanal p WHERE " +
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
    List<PedidoSemanal> findByRangoFechaExcluyendoDestinos(
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

    /**
     * 🆕 Cuenta pedidos totales excluyendo destinos hubs/sedes (para estadísticas)
     */
    @Query("SELECT COUNT(p) FROM PedidoSemanal p WHERE p.aeropuertoDestinoId NOT IN :destinosExcluidos")
    long countExcluyendoDestinos(@Param("destinosExcluidos") List<String> destinosExcluidos);

    /**
     * 🆕 SIMULACIÓN SEMANAL: Carga pedidos de exactamente 7 días desde la fecha de inicio.
     * Excluye pedidos con destino a HUBS/SEDES.
     * 
     * Ejemplo: Si inicio es 2025-01-02, carga pedidos del 2 al 8 de enero inclusive.
     * 
     * @param anioInicio Año de inicio
     * @param mesInicio Mes de inicio (1-12)
     * @param diaInicio Día de inicio (1-31)
     * @param anioFin Año de fin (7 días después)
     * @param mesFin Mes de fin
     * @param diaFin Día de fin
     * @param destinosExcluidos Lista de códigos de aeropuertos a excluir (SPIM, EBCI, UBBB)
     * @return Lista de pedidos de esa semana que NO van a hubs/sedes
     */
    @Query("SELECT p FROM PedidoSemanal p WHERE " +
           "p.aeropuertoDestinoId NOT IN :destinosExcluidos AND " +
           "(" +
           "  (p.anio > :anioInicio OR (p.anio = :anioInicio AND p.mes > :mesInicio) OR (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia >= :diaInicio))" +
           ") AND (" +
           "  (p.anio < :anioFin OR (p.anio = :anioFin AND p.mes < :mesFin) OR (p.anio = :anioFin AND p.mes = :mesFin AND p.dia <= :diaFin))" +
           ")" +
           " ORDER BY p.anio, p.mes, p.dia, p.hora, p.minuto")
    List<PedidoSemanal> findPedidosSemana(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("diaInicio") int diaInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("diaFin") int diaFin,
            @Param("destinosExcluidos") List<String> destinosExcluidos
    );

    /**
     * 🆕 Cuenta pedidos de una semana específica (para estadísticas sin cargar datos)
     */
    @Query("SELECT COUNT(p) FROM PedidoSemanal p WHERE " +
           "p.aeropuertoDestinoId NOT IN :destinosExcluidos AND " +
           "(" +
           "  (p.anio > :anioInicio OR (p.anio = :anioInicio AND p.mes > :mesInicio) OR (p.anio = :anioInicio AND p.mes = :mesInicio AND p.dia >= :diaInicio))" +
           ") AND (" +
           "  (p.anio < :anioFin OR (p.anio = :anioFin AND p.mes < :mesFin) OR (p.anio = :anioFin AND p.mes = :mesFin AND p.dia <= :diaFin))" +
           ")")
    long countPedidosSemana(
            @Param("anioInicio") int anioInicio,
            @Param("mesInicio") int mesInicio,
            @Param("diaInicio") int diaInicio,
            @Param("anioFin") int anioFin,
            @Param("mesFin") int mesFin,
            @Param("diaFin") int diaFin,
            @Param("destinosExcluidos") List<String> destinosExcluidos
    );

}
