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
     * IMPORTANTE: Limpia la BD antes de cargar para evitar duplicados
     * @return Lista de aeropuertos cargados
     */
    @Transactional
    public List<Aeropuerto> cargarDesdeArchivo() {
        // Limpiar la base de datos antes de cargar
        log.info("Limpiando aeropuertos existentes...");
        limpiarAeropuertos();
        
        List<Aeropuerto> aeropuertosParaGuardar = new ArrayList<>();
        String continenteActual = "Desconocido";

        try {
            log.info("Iniciando lectura de archivo de aeropuertos...");
            ClassPathResource resource = new ClassPathResource("datos/Aeropuertos.txt");

            // PASO 1: Leer TODO el archivo primero
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-16"))) {

                String linea;
                int lineaNumero = 0;
                int aeropuertosParsados = 0;

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
                            aeropuertosParaGuardar.add(aeropuerto);
                            aeropuertosParsados++;
                        }

                    } catch (Exception e) {
                        log.debug("Linea {} no es aeropuerto (probablemente encabezado): {}", lineaNumero, lineaTrimmed);
                    }
                }

                log.info("Lectura completada. {} aeropuertos parseados de {} líneas", aeropuertosParsados, lineaNumero);
            }

            // PASO 2: Guardar todos (ya no necesitamos verificar existentes porque limpiamos primero)
            if (!aeropuertosParaGuardar.isEmpty()) {
                log.info("Guardando {} aeropuertos en la base de datos...", aeropuertosParaGuardar.size());
                List<Aeropuerto> guardados = guardarAeropuertos(aeropuertosParaGuardar);
                log.info("✓ Total de aeropuertos guardados: {}", guardados.size());
                return guardados;
            } else {
                log.info("No hay aeropuertos para guardar");
                return new ArrayList<>();
            }

        } catch (IOException e) {
            log.error("Error leyendo archivo de aeropuertos: {}", e.getMessage());
            throw new RuntimeException("No se pudo cargar el archivo de aeropuertos", e);
        }
    }

    /**
     * Guarda aeropuertos en una transacción separada
     */
    @Transactional
    private List<Aeropuerto> guardarAeropuertos(List<Aeropuerto> aeropuertos) {
        return aeropuertoRepository.saveAll(aeropuertos);
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
     * Limpia todos los aeropuertos de la base de datos con DELETE nativo optimizado
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void limpiarAeropuertos() {
        long count = aeropuertoRepository.count();
        aeropuertoRepository.deleteAllNative(); // DELETE masivo con SQL nativo
        log.info("Se eliminaron {} aeropuertos de la base de datos (DELETE nativo)", count);
    }

}
