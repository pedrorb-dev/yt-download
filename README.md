# YT Downloader — Spring Boot + React

Aplicación full stack para descargar videos de YouTube: completos, solo audio o fragmentos por rango de tiempo.

> ⚠️ **Solo para uso educativo y personal.** Respetar los [Términos de Servicio de YouTube](https://www.youtube.com/static?template=terms).

---

## Arquitectura

```
yt-downloader/
├── backend/                    # Spring Boot 3.2, Java 21, Maven
│   ├── pom.xml
│   └── src/main/java/com/ytdownloader/
│       ├── YoutubeDownloaderApplication.java
│       ├── controller/YoutubeController.java
│       ├── service/YoutubeService.java
│       ├── dto/VideoMetadata.java
│       ├── dto/DownloadRequest.java
│       ├── config/AsyncConfig.java
│       ├── config/WebConfig.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           ├── VideoNotFoundException.java
│           ├── ProcessExecutionException.java
│           └── InvalidRequestException.java
└── frontend/                   # React 18 + TypeScript + Vite
    ├── index.html
    ├── vite.config.ts
    └── src/
        ├── App.tsx
        ├── App.css
        ├── main.tsx
        ├── types/index.ts
        ├── services/api.ts
        └── components/
            ├── VideoForm.tsx
            ├── VideoInfo.tsx
            └── DownloadOptions.tsx
```

---

## Requisitos previos

### 1. Instalar yt-dlp

**Windows:**
```powershell
# Con winget
winget install yt-dlp

# O descargar el ejecutable directamente:
# https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe
# Copiar a C:\Windows\System32\ o agregar al PATH
```

**macOS:**
```bash
brew install yt-dlp
```

**Linux (Debian/Ubuntu):**
```bash
sudo apt install python3-pip
pip3 install yt-dlp
# O con el binario:
sudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
sudo chmod a+rx /usr/local/bin/yt-dlp
```

Verificar: `yt-dlp --version`

---

### 2. Instalar ffmpeg

**Windows:**
```powershell
winget install ffmpeg
# O descargar de https://ffmpeg.org/download.html y agregar al PATH
```

**macOS:**
```bash
brew install ffmpeg
```

**Linux (Debian/Ubuntu):**
```bash
sudo apt update && sudo apt install ffmpeg
```

Verificar: `ffmpeg -version`

---

### 3. Java 21 y Maven

```bash
# Verificar Java
java -version  # Necesita Java 21+

# Verificar Maven
mvn -version
```

Si no tienes Java 21: [Adoptium Temurin 21](https://adoptium.net/)

### 4. Node.js 18+

```bash
node --version  # Necesita v18+
npm --version
```

---

## Ejecución

### Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

El servidor arranca en `http://localhost:8080`.
Los archivos temporales se guardan en `./downloads/` y se limpian automáticamente.

**Variables de entorno opcionales:**
```bash
# Cambiar puerto
SERVER_PORT=9090 mvn spring-boot:run

# Cambiar directorio de descargas
APP_DOWNLOAD_DIR=/tmp/yt-downloads mvn spring-boot:run
```

**Windows (PowerShell):**
```powershell
$env:SERVER_PORT="9090"; mvn spring-boot:run
```

---

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

La app estará en `http://localhost:5173`.
El proxy de Vite redirige `/api/*` → `http://localhost:8080/api/*` (sin problemas de CORS en desarrollo).

---

## API REST

### `GET /api/health`
Health check.
```json
{ "status": "ok", "service": "yt-downloader" }
```

### `GET /api/metadata?url=<youtube-url>`
Obtiene metadatos sin descargar.
```json
{
  "id": "dQw4w9WgXcQ",
  "title": "Rick Astley - Never Gonna Give You Up",
  "duration": 212,
  "durationString": "3:32",
  "thumbnail": "https://...",
  "uploader": "Rick Astley",
  "viewCount": 1400000000,
  "availableQualities": ["best", "1080p", "720p", "480p", "360p"]
}
```

### `POST /api/download`
Descarga y retorna el archivo binario.
```json
// Body
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "type": "video",       // "video" | "audio" | "fragment"
  "quality": "720p",     // "best" | "1080p" | "720p" | ...
  "startTime": "0:30",   // Solo para type="fragment"
  "endTime": "1:15"      // Solo para type="fragment"
}
```
Responde con `Content-Disposition: attachment; filename="video.mp4"`.

---

## Por qué `--download-sections "*MM:SS-MM:SS"`

El asterisco `*` en `--download-sections` es parte de la sintaxis de yt-dlp para especificar un rango de tiempo **absoluto** en el video, independientemente de los capítulos definidos por el creador. Sin el `*`, yt-dlp interpreta el argumento como un nombre de capítulo (expresión regular). Con `*`, el formato es:

```
*INICIO-FIN
```

Ejemplo: `--download-sections "*0:30-1:15"` descarga desde el segundo 30 hasta el minuto 1:15.

La flag `--force-keyframes-at-cuts` asegura que ffmpeg inserte un keyframe exacto en los puntos de corte, evitando fotogramas borrosos al inicio/fin del fragmento.

---

## Decisiones técnicas

| Decisión | Razón |
|---|---|
| `ProcessBuilder` con lista de args | Evita shell injection. Nunca concatenar strings con la URL del usuario |
| `StreamingResponseBody` | Archivos de video pueden pesar varios GB. No cargar en RAM |
| `@Scheduled` cada 30 min | Limpiar archivos huérfanos si el cliente cierra la conexión |
| `CompletableFuture` / `@Async` | El proceso de yt-dlp puede durar minutos; no bloquear hilos de Tomcat |
| Vite proxy en dev | Evita CORS sin modificar el backend en desarrollo |
| `responseType: 'blob'` en Axios | Necesario para recibir el binario del archivo correctamente |

---

## Posibles mejoras

1. **WebSocket para progreso real**: Parsear la salida de yt-dlp línea a línea y enviar el porcentaje vía `SockJS` / `STOMP`.
2. **Cola de descargas**: Usar `BlockingQueue` o `Redis` para limitar descargas simultáneas.
3. **Soporte de playlists**: Agregar `--yes-playlist` y un endpoint que devuelva múltiples archivos (ZIP).
4. **Caché de metadatos**: Guardar en Redis o H2 para no rellamar a yt-dlp con la misma URL.
5. **Autenticación**: Añadir Spring Security con JWT para limitar el uso.
6. **Docker**: `docker-compose.yml` con los servicios de backend, frontend y una imagen con yt-dlp+ffmpeg pre-instalados.

---

## Solución de problemas

**`yt-dlp: command not found`**  
→ Asegúrate de que yt-dlp está en el PATH del sistema y que el proceso de Java lo hereda.

**Timeout en descargas largas**  
→ Aumenta `app.process.timeout` en `application.properties` (en segundos).

**Error "Sign in to confirm your age"**  
→ Algunos videos requieren cuenta. Puedes pasar cookies con `--cookies-from-browser chrome` pero esto requiere modificaciones al servicio.

**Puerto 8080 ocupado**  
→ Cambiar con `server.port=8081` en `application.properties`.
