package com.proyecto.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuración de Hibernate y JPA
 *
 * - @EnableJpaRepositories: Habilita los repositorios JPA
 * - @EnableTransactionManagement: Habilita el manejo de transacciones
 * - @EnableJpaAuditing: Habilita auditoría automática (createdDate, lastModifiedDate, etc.)
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.proyecto.backend.repository")
@EnableTransactionManagement
@EnableJpaAuditing
public class HibernateConfig {

    // Aquí se pueden agregar configuraciones adicionales de Hibernate
    // como EntityManagerFactory personalizado, interceptores, etc.

}
