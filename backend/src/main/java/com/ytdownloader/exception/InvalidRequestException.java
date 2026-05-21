package com.ytdownloader.exception;

/** Se lanza cuando el request tiene parámetros inválidos (tiempos fuera de rango, etc.). */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}
