package com.ytdownloader.exception;

/** Se lanza cuando yt-dlp no puede encontrar/acceder al video. */
public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException(String message) { super(message); }
}
