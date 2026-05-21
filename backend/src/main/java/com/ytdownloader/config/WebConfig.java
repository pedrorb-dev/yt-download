package com.ytdownloader.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS para permitir peticiones desde el frontend React.
 * En producción, reemplazar "*" con el dominio real del frontend.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:5173",   // Vite dev server
                    "http://localhost:3000",   // alternativa
                    "http://127.0.0.1:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                    "Content-Disposition",     // Necesario para que el browser lea el nombre del archivo
                    "Content-Type",
                    "Content-Length"
                )
                .allowCredentials(false)
                .maxAge(3600);
    }
}
