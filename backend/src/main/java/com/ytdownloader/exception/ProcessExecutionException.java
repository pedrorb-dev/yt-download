package com.ytdownloader.exception;

/** Se lanza cuando falla un proceso externo (yt-dlp o ffmpeg). */
public class ProcessExecutionException extends RuntimeException {
    public ProcessExecutionException(String message) { super(message); }
    public ProcessExecutionException(String message, Throwable cause) { super(message, cause); }
}
