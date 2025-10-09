package com.proyecto.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionChecker implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        log.info("=".repeat(60));
        log.info("VERIFICANDO CONEXIÓN A LA BASE DE DATOS");
        log.info("=".repeat(60));

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            log.info("✓ CONEXIÓN EXITOSA");
            log.info("  - URL: {}", metaData.getURL());
            log.info("  - Driver: {}", metaData.getDriverName());
            log.info("  - Versión del Driver: {}", metaData.getDriverVersion());
            log.info("  - Base de Datos: {}", metaData.getDatabaseProductName());
            log.info("  - Versión de BD: {}", metaData.getDatabaseProductVersion());
            log.info("  - Usuario: {}", metaData.getUserName());
            log.info("=".repeat(60));

        } catch (Exception e) {
            log.error("=".repeat(60));
            log.error("✗ ERROR DE CONEXIÓN A LA BASE DE DATOS");
            log.error("  Mensaje: {}", e.getMessage());
            log.error("  Tipo: {}", e.getClass().getSimpleName());
            log.error("=".repeat(60));
            log.error("Verifica tu configuración en application.properties:");
            log.error("  - spring.datasource.url");
            log.error("  - spring.datasource.username");
            log.error("  - spring.datasource.password");
            log.error("  - Dependencia de driver en pom.xml");
            log.error("=".repeat(60));
        }
    }

}
