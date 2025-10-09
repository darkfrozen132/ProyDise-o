package com.proyecto.backend.service;

import com.proyecto.backend.model.Aeropuerto;
import com.proyecto.backend.repository.AeropuertoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository;

    // Patron regex para parsear lineas de aeropuerto
    private static final Pattern AEROPUERTO_PATTERN = Pattern.compile(
        "^\\s*\\d+\\s+(?<icao>[A-Z]{4})\\s+(?<ciudad>.+?)\\s{2,}(?<pais>[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\.]+)\\s+\\S+\\s+(?<gmt>[+\\-]?\\d+)\\s+(?<cap>\\d+)\\s+Latitude:\\s+(?<lat>[^L]+?)\\s+Longitude:\\s+(?<lon>.+)$"
    );

    // Patron para extraer grados, minutos, segundos de latitud
    private static final Pattern LAT_PATTERN = Pattern.compile(
        "(?<d>\\d{1,2})[°º]\\s*(?<m>\\d{1,2})['']\\s*(?<s>\\d{1,2})[\"\"]\\s*(?<hem>[NS])"
    );

    // Patron para extraer grados, minutos, segundos de longitud
    private static final Pattern LON_PATTERN = Pattern.compile(
        "(?<d>\\d{1,3})[°º]\\s*(?<m>\\d{1,2})['']\\s*(?<s>\\d{1,2})[\"\"]\\s*(?<hem>[EW])"
    );

    /**
     * Carga aeropuertos desde el archivo de texto
     * @return Lista de aeropuertos cargados
     */
    @Transactional
    public List<Aeropuerto> cargarDesdeArchivo() {
        List<Aeropuerto> aeropuertosCargados = new ArrayList<>();
        List<Aeropuerto> batch = new ArrayList<>();
        final int BATCH_SIZE = 100; // Guardar en lotes de 100
        String continenteActual = "Desconocido";

        try {
            ClassPathResource resource = new ClassPathResource("datos/Aeropuertos.txt");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-16"))) {

                String linea;
                int lineaNumero = 0;

                while ((linea = reader.readLine()) != null) {
                    lineaNumero++;
                    String lineaTrimmed = linea.trim();

                    // Saltar lineas vacias o de asteriscos
                    if (lineaTrimmed.isEmpty() || lineaTrimmed.contains("***") || lineaTrimmed.startsWith("PDDS")) {
                        continue;
                    }

                    // Detectar cambio de continente
                    String nuevoContinente = detectarContinente(lineaTrimmed);
                    if (nuevoContinente != null) {
                        continenteActual = nuevoContinente;
                        log.info("Cambiando a continente: {}", continenteActual);
                        continue;
                    }

                    // Intentar parsear como linea de aeropuerto
                    try {
                        Aeropuerto aeropuerto = parsearLineaAeropuerto(linea, continenteActual);

                        if (aeropuerto != null) {
                            // Verificar si ya existe antes de agregar al batch
                            if (!aeropuertoRepository.existsByCodigoICAO(aeropuerto.getCodigoICAO())) {
                                batch.add(aeropuerto);
                                
                                // Guardar en batch cada BATCH_SIZE registros
                                if (batch.size() >= BATCH_SIZE) {
                                    List<Aeropuerto> guardados = aeropuertoRepository.saveAll(batch);
                                    aeropuertosCargados.addAll(guardados);
                                    log.info("Guardados {} aeropuertos (total: {})", batch.size(), aeropuertosCargados.size());
                                    batch.clear();
                                }
                            } else {
                                log.debug("Aeropuerto {} ya existe, omitiendo", aeropuerto.getCodigoICAO());
                            }
                        }

                    } catch (Exception e) {
                        log.debug("Linea {} no es aeropuerto (probablemente encabezado): {}", lineaNumero, lineaTrimmed);
                    }
                }

                // Guardar el ultimo lote (los que quedaron)
                if (!batch.isEmpty()) {
                    List<Aeropuerto> guardados = aeropuertoRepository.saveAll(batch);
                    aeropuertosCargados.addAll(guardados);
                    log.info("Guardados {} aeropuertos finales (total: {})", batch.size(), aeropuertosCargados.size());
                    batch.clear();
                }

                log.info("Total de aeropuertos cargados: {}", aeropuertosCargados.size());

            }

        } catch (IOException e) {
            log.error("Error leyendo archivo de aeropuertos: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar el archivo de aeropuertos", e);
        }

        return aeropuertosCargados;
    }

    /**
     * Detecta si una linea contiene un encabezado de continente
     * @param linea Linea a analizar
     * @return Nombre del continente o null si no es encabezado
     */
    private String detectarContinente(String linea) {
        if (linea.contains("America del Sur")) {
            return "America del Sur";
        } else if (linea.equals("Europa")) {
            return "Europa";
        } else if (linea.equals("Asia")) {
            return "Asia";
        }
        return null;
    }

    /**
     * Parsea una linea del archivo y crea un objeto Aeropuerto usando regex
     * @param linea Linea del archivo
     * @param continente Continente actual
     * @return Objeto Aeropuerto o null si no se pudo parsear
     */
    private Aeropuerto parsearLineaAeropuerto(String linea, String continente) {
        if (linea == null || linea.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = AEROPUERTO_PATTERN.matcher(linea);

        if (!matcher.matches()) {
            return null;
        }

        try {
            String codigoICAO = matcher.group("icao").trim();
            String ciudad = matcher.group("ciudad").trim();
            String pais = matcher.group("pais").trim();
            int husoHorario = Integer.parseInt(matcher.group("gmt"));
            int capacidadAlmacen = Integer.parseInt(matcher.group("cap"));

            // Normalizar y extraer coordenadas
            String latStr = normalizarCoordenada(matcher.group("lat"));
            String lonStr = normalizarCoordenada(matcher.group("lon"));

            double latitud = parsearDMS(latStr, LAT_PATTERN);
            double longitud = parsearDMS(lonStr, LON_PATTERN);

            return new Aeropuerto(codigoICAO, ciudad, pais, husoHorario,
                                 capacidadAlmacen, latitud, longitud, continente);

        } catch (Exception e) {
            log.error("Error parseando linea: {} - {}", linea, e.getMessage());
            return null;
        }
    }

    /**
     * Normaliza una coordenada reemplazando variantes de simbolos
     * @param coordenada Coordenada en formato original
     * @return Coordenada normalizada
     */
    private String normalizarCoordenada(String coordenada) {
        if (coordenada == null) {
            return "";
        }

        // Reemplazar variantes de simbolos de grados, minutos y segundos
        return coordenada
            .replace("º", "°")
            .replace("'", "'")
            .replace("'", "'")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Parsea coordenadas en formato DMS (grados, minutos, segundos) a decimal
     * @param coordStr Coordenada en formato DMS
     * @param pattern Patron regex para extraer componentes
     * @return Coordenada en formato decimal
     */
    private double parsearDMS(String coordStr, Pattern pattern) {
        Matcher matcher = pattern.matcher(coordStr);

        if (!matcher.find()) {
            log.warn("No se pudo parsear coordenada: {}", coordStr);
            return 0.0;
        }

        try {
            int grados = Integer.parseInt(matcher.group("d"));
            int minutos = Integer.parseInt(matcher.group("m"));
            int segundos = Integer.parseInt(matcher.group("s"));
            String hemisferio = matcher.group("hem");

            // Convertir a decimal
            double decimal = grados + (minutos / 60.0) + (segundos / 3600.0);

            // Si es Sur o Oeste, multiplicar por -1
            if (hemisferio.equals("S") || hemisferio.equals("W")) {
                decimal = -decimal;
            }

            return decimal;

        } catch (Exception e) {
            log.error("Error convirtiendo DMS a decimal: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Obtiene todos los aeropuertos
     */
    @Transactional(readOnly = true)
    public List<Aeropuerto> obtenerTodos() {
        return aeropuertoRepository.findAll();
    }

    /**
     * Busca aeropuerto por codigo ICAO
     */
    @Transactional(readOnly = true)
    public Aeropuerto buscarPorCodigo(String codigoICAO) {
        return aeropuertoRepository.findByCodigoICAO(codigoICAO)
                .orElseThrow(() -> new RuntimeException("Aeropuerto no encontrado: " + codigoICAO));
    }

    /**
     * Limpia todos los aeropuertos de la base de datos
     */
    @Transactional
    public void limpiarAeropuertos() {
        long count = aeropuertoRepository.count();
        aeropuertoRepository.deleteAll();
        log.info("Se eliminaron {} aeropuertos de la base de datos", count);
    }

    /**
     * Recarga aeropuertos: limpia la BD y carga desde el archivo
     */
    @Transactional
    public List<Aeropuerto> recargarDesdeArchivo() {
        log.info("Iniciando recarga de aeropuertos...");
        limpiarAeropuertos();
        return cargarDesdeArchivo();
    }

}
