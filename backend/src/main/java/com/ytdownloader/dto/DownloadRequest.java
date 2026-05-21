package com.ytdownloader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request de descarga enviado desde el frontend.
 */
public class DownloadRequest {

    @NotBlank(message = "La URL del video es obligatoria")
    private String url;

    /**
     * Tipo de descarga:
     * - "video"    → video completo
     * - "audio"    → solo audio (MP3)
     * - "fragment" → fragmento por rango de tiempo
     */
    @NotBlank(message = "El tipo de descarga es obligatorio")
    @Pattern(regexp = "video|audio|fragment", message = "Tipo inválido. Use: video, audio o fragment")
    private String type;

    /**
     * Calidad de video: "best", "720p", "1080p", "480p", "360p"
     * Solo aplica cuando type = "video"
     */
    private String quality = "best";

    /**
     * Tiempo de inicio del fragmento (formato MM:SS o HH:MM:SS)
     * Solo aplica cuando type = "fragment"
     */
    private String startTime;

    /**
     * Tiempo de fin del fragmento (formato MM:SS o HH:MM:SS)
     * Solo aplica cuando type = "fragment"
     */
    private String endTime;

    // Constructors
    public DownloadRequest() {}

    // Getters y Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return "DownloadRequest{url='" + url + "', type='" + type +
               "', quality='" + quality + "', start='" + startTime +
               "', end='" + endTime + "'}";
    }
}
