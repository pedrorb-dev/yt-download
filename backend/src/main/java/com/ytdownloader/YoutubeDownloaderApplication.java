package com.ytdownloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal del YouTube Downloader.
 * Usa yt-dlp y ffmpeg para descargar videos de YouTube.
 *
 * IMPORTANTE: Solo para uso educativo/personal.
 * Respetar los Términos de Servicio de YouTube.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class YoutubeDownloaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeDownloaderApplication.class, args);
    }
}
