import axios from 'axios';
import { VideoMetadata, DownloadRequest } from '../types';

// Base URL: en desarrollo usa el proxy de Vite (/api → localhost:8080/api)
const api = axios.create({
  baseURL: '/api',
  timeout: 30000, // 30s para metadatos
});

/**
 * Obtiene los metadatos del video sin descargarlo.
 */
export async function fetchMetadata(url: string): Promise<VideoMetadata> {
  const response = await api.get<VideoMetadata>('/metadata', {
    params: { url },
  });
  return response.data;
}

export async function downloadVideo(
  request: DownloadRequest,
  onProgress?: (percent: number) => void
): Promise<void> {
  const response = await axios.post('/api/download', request, {
    responseType: 'blob',
    timeout: 0,
    onDownloadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100);
        onProgress(percent);
      }
    },
  });

  // Obtener nombre del archivo del header Content-Disposition
  const contentDisposition = response.headers['content-disposition'] || '';
  let filename = 'descarga';
  const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
  if (match) filename = match[1];

  // Crear enlace temporal y hacer click para iniciar descarga
  const blob = new Blob([response.data], {
    type: response.headers['content-type'] || 'application/octet-stream',
  });
  const objectUrl = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  // Liberar memoria
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}

/**
 * Extrae el mensaje de error de un error de Axios.
 */
export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data?.message) return data.message;
    if (error.code === 'ECONNABORTED') return 'Tiempo de espera agotado. El video puede ser muy largo.';
    if (!error.response) return 'No se pudo conectar con el servidor. ¿Está ejecutándose?';
    return `Error ${error.response.status}: ${error.message}`;
  }
  if (error instanceof Error) return error.message;
  return 'Error desconocido';
}
