// ── Tipos de datos que vienen del backend ──────────────────────────────────

export interface VideoMetadata {
  id: string;
  title: string;
  duration: number;          // en segundos
  durationString: string;    // formato legible "3:45"
  thumbnail: string;
  uploader: string;
  viewCount: number;
  availableQualities: string[];
}

// ── Request de descarga ────────────────────────────────────────────────────

export type DownloadType = 'video' | 'audio' | 'fragment';

export interface DownloadRequest {
  url: string;
  type: DownloadType;
  quality?: string;
  startTime?: string;  // MM:SS o HH:MM:SS
  endTime?: string;    // MM:SS o HH:MM:SS
}

// ── Estado de la UI ────────────────────────────────────────────────────────

export type AppStatus =
  | 'idle'           // inicial
  | 'loading-meta'   // cargando metadatos
  | 'ready'          // metadatos cargados, esperando configuración
  | 'downloading'    // descarga en curso
  | 'error';         // error

export interface AppState {
  status: AppStatus;
  url: string;
  metadata: VideoMetadata | null;
  downloadType: DownloadType;
  quality: string;
  startTime: string;
  endTime: string;
  error: string | null;
}
