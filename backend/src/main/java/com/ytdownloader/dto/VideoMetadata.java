package com.ytdownloader.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadatos del video obtenidos desde yt-dlp --dump-json.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoMetadata {

    private String id;
    private String title;
    private Long duration;          // duración en segundos
    private String thumbnail;
    private String uploader;
    private Long viewCount;
    private String description;

    @JsonProperty("duration_string")
    private String durationString;

    // Formatos disponibles simplificados
    private String[] availableQualities;

    // Constructors
    public VideoMetadata() {}

    public VideoMetadata(String id, String title, Long duration, String thumbnail,
                         String uploader, Long viewCount, String durationString) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.thumbnail = thumbnail;
        this.uploader = uploader;
        this.viewCount = viewCount;
        this.durationString = durationString;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }

    @JsonProperty("view_count")
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDurationString() { return durationString; }
    public void setDurationString(String durationString) { this.durationString = durationString; }

    public String[] getAvailableQualities() { return availableQualities; }
    public void setAvailableQualities(String[] availableQualities) {
        this.availableQualities = availableQualities;
    }
}
