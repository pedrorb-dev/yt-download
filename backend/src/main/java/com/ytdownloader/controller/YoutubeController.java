package com.ytdownloader.controller;

import com.ytdownloader.dto.DownloadRequest;
import com.ytdownloader.dto.VideoMetadata;
import com.ytdownloader.service.YoutubeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.util.Map;

/**
 * Controlador REST para la API de descarga de YouTube.
 *
 * Endpoints:
 *   GET  /api/metadata?url=...  → Devuelve metadatos del video
 *   POST /api/download          → Descarga y envía el archivo al cliente
 *   GET  /api/health            → Health check
 */
@RestController
@RequestMapping("/api")
public class YoutubeController {

    private static final Logger log = LoggerFactory.getLogger(YoutubeController.class);
    private final YoutubeService youtubeService;

    public YoutubeController(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    /**
     * Health check básico.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "yt-downloader"));
    }

    /**
     * Obtiene metadatos del video.
     *
     * GET /api/metadata?url=https://www.youtube.com/watch?v=...
     */
    @GetMapping("/metadata")
    public ResponseEntity<VideoMetadata> getMetadata(
            @RequestParam String url) {

        log.info("Solicitando metadatos para URL: {}", url);
        VideoMetadata metadata = youtubeService.getVideoMetadata(url);
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadVideo(
            @Valid @RequestBody DownloadRequest request) {

        log.info("Solicitud de descarga: {}", request);

        // Ejecutar la descarga (puede tardar varios minutos)
        Map<String, Object> result = youtubeService.downloadVideo(request);

        File file         = (File) result.get("file");
        String contentType = (String) result.get("contentType");
        String filename    = (String) result.get("filename");

        log.info("Archivo listo para enviar: {} ({} bytes)", file.getName(), file.length());

        // StreamingResponseBody: envía el archivo en chunks sin cargarlo en RAM
        StreamingResponseBody body = youtubeService.streamFile(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
