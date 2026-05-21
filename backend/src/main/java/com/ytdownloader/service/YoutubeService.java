package com.ytdownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytdownloader.dto.DownloadRequest;
import com.ytdownloader.dto.VideoMetadata;
import com.ytdownloader.exception.InvalidRequestException;
import com.ytdownloader.exception.ProcessExecutionException;
import com.ytdownloader.exception.VideoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Servicio principal que orquesta yt-dlp y ffmpeg.
 *
 * Seguridad: todos los comandos usan ProcessBuilder con lista de args (no shell string)
 * para evitar inyección de comandos.
 */
@Service
public class YoutubeService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Patrón para validar formato de tiempo MM:SS o HH:MM:SS
    private static final Pattern TIME_PATTERN =
            Pattern.compile("^(\\d{1,2}:)?\\d{1,2}:\\d{2}$");

    @Value("${app.download.dir:./downloads}")
    private String downloadDir;

    @Value("${app.process.timeout:600}")
    private int processTimeoutSeconds;

    // ═══════════════════════════════════════════════════════════════════════
    //  METADATOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene metadatos del video usando: yt-dlp --dump-json <url>
     * No descarga nada, solo lee la información del video.
     */
    public VideoMetadata getVideoMetadata(String url) {
        validateUrl(url);
        ensureDownloadDir();

        List<String> cmd = List.of("yt-dlp", "--dump-json", "--no-playlist", url);
        log.debug("Ejecutando metadatos: {}", cmd);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ProcessExecutionException("Timeout al obtener metadatos del video");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                handleYtDlpError(stderr, url);
            }

            return parseMetadata(stdout);

        } catch (IOException e) {
            throw new ProcessExecutionException(
                "No se pudo ejecutar yt-dlp. ¿Está instalado y en el PATH? Error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessExecutionException("Proceso interrumpido");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DESCARGA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Descarga el video/audio/fragmento y devuelve el archivo como stream.
     * El archivo temporal se elimina después de enviarse al cliente.
     */
    public Map<String, Object> downloadVideo(DownloadRequest request) {
        validateUrl(request.getUrl());
        ensureDownloadDir();

        if ("fragment".equals(request.getType())) {
            validateTimeRange(request.getStartTime(), request.getEndTime());
        }

        // Nombre único para evitar colisiones entre descargas simultáneas
        String fileId = UUID.randomUUID().toString();
        String outputTemplate = downloadDir + "/" + fileId + ".%(ext)s";

        List<String> cmd = buildDownloadCommand(request, outputTemplate);
        log.info("Iniciando descarga: type={}, url={}", request.getType(), request.getUrl());
        log.debug("Comando: {}", cmd);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Leer stderr en hilo separado para evitar bloqueo de buffer
            StringBuilder stderrBuilder = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try { stderrBuilder.append(readStream(process.getErrorStream())); }
                catch (IOException ignored) {}
            });
            stderrThread.start();

            boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            stderrThread.join(5000);

            if (!finished) {
                process.destroyForcibly();
                throw new ProcessExecutionException("Timeout: la descarga tardó demasiado");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                handleYtDlpError(stderrBuilder.toString(), request.getUrl());
            }

            // Encontrar el archivo generado (yt-dlp determina la extensión)
            File downloadedFile = findDownloadedFile(fileId);

            Map<String, Object> result = new HashMap<>();
            result.put("file", downloadedFile);
            result.put("contentType", resolveContentType(downloadedFile.getName()));
            result.put("filename", buildOutputFilename(request, downloadedFile.getName()));

            return result;

        } catch (IOException e) {
            throw new ProcessExecutionException(
                "Error ejecutando yt-dlp/ffmpeg: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessExecutionException("Proceso de descarga interrumpido");
        }
    }

    /**
     * Construye la respuesta de streaming para enviar el archivo al cliente.
     * El archivo temporal se elimina después de enviarse.
     */
    public StreamingResponseBody streamFile(File file) {
        return outputStream -> {
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } finally {
                // Eliminar el archivo temporal después de enviarlo
                try {
                    Files.deleteIfExists(file.toPath());
                    log.debug("Archivo temporal eliminado: {}", file.getName());
                } catch (IOException e) {
                    log.warn("No se pudo eliminar el archivo temporal: {}", file.getName());
                }
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LIMPIEZA PROGRAMADA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Elimina archivos huérfanos en ./downloads/ que tienen más de 1 hora.
     * Se ejecuta cada 30 minutos.
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void cleanOrphanFiles() {
        Path dir = Path.of(downloadDir);
        if (!Files.exists(dir)) return;

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> {
                      try {
                          return Files.getLastModifiedTime(path).toInstant().isBefore(oneHourAgo);
                      } catch (IOException e) { return false; }
                  })
                  .forEach(path -> {
                      try {
                          Files.delete(path);
                          log.info("Archivo huérfano eliminado: {}", path.getFileName());
                      } catch (IOException e) {
                          log.warn("No se pudo eliminar huérfano: {}", path);
                      }
                  });
        } catch (IOException e) {
            log.error("Error limpiando archivos temporales", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Construye la lista de argumentos para yt-dlp según el tipo de descarga.
     *
     * Para fragmentos se usa --download-sections con el formato "*START-END".
     * El asterisco (*) es necesario porque yt-dlp usa el prefijo * para indicar
     * que el rango de tiempo aplica a cualquier capítulo/sección del video,
     * no solo a los capítulos definidos por el creador del video.
     */
    private List<String> buildDownloadCommand(DownloadRequest req, String outputTemplate) {
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("--no-playlist");
        cmd.add("-o");
        cmd.add(outputTemplate);

        switch (req.getType()) {
            case "audio" -> {
                // Descargar solo audio y convertir a MP3
                cmd.addAll(List.of(
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "0"  // mejor calidad
                ));
            }
            case "fragment" -> {
                // Fragmento: usa --download-sections con rango de tiempo
                // Formato: "*HH:MM:SS-HH:MM:SS" (el * hace que aplique a todo el video)
                String section = "*" + req.getStartTime() + "-" + req.getEndTime();
                cmd.addAll(List.of(
                    "--download-sections", section,
                    "--force-keyframes-at-cuts",   // keyframes exactos en los cortes
                    "-f", resolveVideoFormat(req.getQuality()),
                    "--merge-output-format", "mp4"
                ));
            }
            default -> { // "video"
                cmd.addAll(List.of(
                    "-f", resolveVideoFormat(req.getQuality()),
                    "--merge-output-format", "mp4"
                ));
            }
        }

        cmd.add(req.getUrl());
        return cmd;
    }

    /**
     * Mapea la calidad seleccionada por el usuario a un format selector de yt-dlp.
     */
    private String resolveVideoFormat(String quality) {
        if (quality == null || quality.isBlank() || "best".equalsIgnoreCase(quality)) {
            return "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best";
        }
        // Ejemplo: "1080p" → "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best"
        String height = quality.toLowerCase().replace("p", "");
        return "bestvideo[height<=" + height + "][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best";
    }

    private VideoMetadata parseMetadata(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            VideoMetadata meta = new VideoMetadata();
            meta.setId(node.path("id").asText(null));
            meta.setTitle(node.path("title").asText("Sin título"));
            meta.setDuration(node.path("duration").asLong(0));
            meta.setThumbnail(node.path("thumbnail").asText(null));
            meta.setUploader(node.path("uploader").asText("Desconocido"));
            meta.setViewCount(node.path("view_count").asLong(0));
            meta.setDurationString(node.path("duration_string").asText(null));

            // Extraer resoluciones únicas disponibles
            JsonNode formats = node.path("formats");
            Set<String> qualities = new LinkedHashSet<>();
            qualities.add("best");
            if (formats.isArray()) {
                for (JsonNode fmt : formats) {
                    int height = fmt.path("height").asInt(0);
                    if (height > 0) {
                        qualities.add(height + "p");
                    }
                }
            }
            meta.setAvailableQualities(qualities.toArray(new String[0]));

            return meta;
        } catch (Exception e) {
            throw new ProcessExecutionException("Error parseando metadatos: " + e.getMessage(), e);
        }
    }

    private File findDownloadedFile(String fileId) {
        File dir = new File(downloadDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith(fileId));
        if (files == null || files.length == 0) {
            throw new ProcessExecutionException(
                "No se encontró el archivo descargado. " +
                "Verifica que yt-dlp y ffmpeg estén instalados correctamente.");
        }
        return files[0];
    }

    private String resolveContentType(String filename) {
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        if (filename.endsWith(".m4a")) return "audio/mp4";
        if (filename.endsWith(".webm")) return "video/webm";
        return "video/mp4";
    }

    private String buildOutputFilename(DownloadRequest req, String originalName) {
        String ext = originalName.substring(originalName.lastIndexOf('.'));
        return switch (req.getType()) {
            case "audio" -> "audio" + ext;
            case "fragment" -> "fragment_" + req.getStartTime().replace(":", "-") +
                               "_to_" + req.getEndTime().replace(":", "-") + ext;
            default -> "video" + ext;
        };
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidRequestException("La URL no puede estar vacía");
        }
        if (!url.contains("youtube.com/") && !url.contains("youtu.be/")) {
            throw new InvalidRequestException("URL inválida. Solo se admiten URLs de YouTube");
        }
    }

    private void validateTimeRange(String start, String end) {
        if (start == null || start.isBlank()) {
            throw new InvalidRequestException("El tiempo de inicio es obligatorio para fragmentos");
        }
        if (end == null || end.isBlank()) {
            throw new InvalidRequestException("El tiempo de fin es obligatorio para fragmentos");
        }
        if (!TIME_PATTERN.matcher(start).matches()) {
            throw new InvalidRequestException(
                "Formato de tiempo inválido para inicio: '" + start + "'. Use MM:SS o HH:MM:SS");
        }
        if (!TIME_PATTERN.matcher(end).matches()) {
            throw new InvalidRequestException(
                "Formato de tiempo inválido para fin: '" + end + "'. Use MM:SS o HH:MM:SS");
        }
    }

    private void handleYtDlpError(String stderr, String url) {
        if (stderr.contains("Video unavailable") || stderr.contains("Private video")) {
            throw new VideoNotFoundException("Video no disponible o privado: " + url);
        }
        if (stderr.contains("is not a valid URL")) {
            throw new VideoNotFoundException("URL inválida: " + url);
        }
        if (stderr.contains("Sign in to confirm your age")) {
            throw new VideoNotFoundException("El video requiere verificación de edad");
        }
        if (stderr.contains("This video is not available")) {
            throw new VideoNotFoundException("El video no está disponible en tu región o fue eliminado");
        }
        throw new ProcessExecutionException("Error de yt-dlp: " + stderr);
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void ensureDownloadDir() {
        File dir = new File(downloadDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("Directorio de descargas creado: {}", dir.getAbsolutePath());
        }
    }
}
