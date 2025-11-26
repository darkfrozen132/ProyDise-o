package com.proyecto.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configuración del Scheduler para tareas programadas
 * 
 * Tareas programadas:
 * - Limpieza de sesiones antiguas (cada hora)
 * - Monitoreo de recursos (futuro)
 * 
 * @author Sistema Package Planner
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * Configura el pool de threads para tareas programadas
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        
        taskRegistrar.setTaskScheduler(scheduler);
        
        log.info("✅ Scheduler configurado con pool de 5 threads");
    }
}
