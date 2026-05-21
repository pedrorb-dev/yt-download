package com.ytdownloader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración del pool de hilos para operaciones asíncronas.
 * Las descargas largas se ejecutan en hilos separados para no bloquear el servidor.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "downloadExecutor")
    public Executor downloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Hilos base siempre activos
        executor.setCorePoolSize(3);
        // Máximo de descargas simultáneas
        executor.setMaxPoolSize(10);
        // Cola de espera si todos los hilos están ocupados
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("yt-download-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return downloadExecutor();
    }
}
