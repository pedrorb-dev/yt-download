import { DownloadType, VideoMetadata } from '../types';

interface DownloadOptionsProps {
  downloadType: DownloadType;
  quality: string;
  startTime: string;
  endTime: string;
  metadata: VideoMetadata;
  isDownloading: boolean;
  downloadProgress: number | null;
  onTypeChange: (type: DownloadType) => void;
  onQualityChange: (quality: string) => void;
  onStartTimeChange: (time: string) => void;
  onEndTimeChange: (time: string) => void;
  onDownload: () => void;
}

/**
 * Panel de opciones de descarga: tipo, calidad y rango de tiempo.
 */
export function DownloadOptions({
  downloadType, quality, startTime, endTime,
  metadata, isDownloading, downloadProgress,
  onTypeChange, onQualityChange, onStartTimeChange, onEndTimeChange,
  onDownload,
}: DownloadOptionsProps) {

  const types: { value: DownloadType; label: string; }[] = [
    { value: 'video', label: 'Video completo' },
    { value: 'audio', label: 'Solo audio (MP3)' },
    { value: 'fragment', label: 'Fragmento' },
  ];

  return (
    <div className="download-options">
      {/* Tipo de descarga */}
      <fieldset className="type-selector">
        <legend>Tipo de descarga</legend>
        <div className="radio-group">
          {types.map(({ value, label }) => (
            <label key={value} className={`radio-card ${downloadType === value ? 'selected' : ''}`}>
              <input
                type="radio"
                name="downloadType"
                value={value}
                checked={downloadType === value}
                onChange={() => onTypeChange(value)}
                disabled={isDownloading}
              />
              <span className="radio-label">{label}</span>
            </label>
          ))}
        </div>
      </fieldset>

      {/* Selector de calidad (solo para video y fragmento) */}
      {(downloadType === 'video' || downloadType === 'fragment') && (
        <div className="quality-selector">
          <label htmlFor="quality">Calidad</label>
          <select
            id="quality"
            value={quality}
            onChange={(e) => onQualityChange(e.target.value)}
            disabled={isDownloading}
            className="select-input"
          >
            {metadata.availableQualities.map((q) => (
              <option key={q} value={q}>
                {q === 'best' ? ' Mejor disponible' : q}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Rango de tiempo (solo para fragmento) */}
      {downloadType === 'fragment' && (
        <div className="time-range">
          <div className="time-field">
            <label htmlFor="startTime">Inicio</label>
            <input
              id="startTime"
              type="text"
              placeholder="0:00"
              value={startTime}
              onChange={(e) => onStartTimeChange(e.target.value)}
              disabled={isDownloading}
              className="time-input"
              pattern="(\d{1,2}:)?\d{1,2}:\d{2}"
              title="Formato: MM:SS o HH:MM:SS"
            />
          </div>
          <span className="time-separator">→</span>
          <div className="time-field">
            <label htmlFor="endTime">Fin</label>
            <input
              id="endTime"
              type="text"
              placeholder="1:30"
              value={endTime}
              onChange={(e) => onEndTimeChange(e.target.value)}
              disabled={isDownloading}
              className="time-input"
              pattern="(\d{1,2}:)?\d{1,2}:\d{2}"
              title="Formato: MM:SS o HH:MM:SS"
            />
          </div>
          <span className="time-hint">Duración total: {metadata.durationString}</span>
        </div>
      )}

      {/* Botón de descarga */}
      <button
        className="btn-download"
        onClick={onDownload}
        disabled={isDownloading}
      >
        {isDownloading ? (
          <span>
            {downloadProgress !== null
              ? `Descargando ${downloadProgress}%`
              : 'Procesando... (puede tardar varios minutos)'}
          </span>
        ) : (
          '⬇ Descargar'
        )}
      </button>

      {isDownloading && downloadProgress !== null && (
        <div className="progress-bar-wrapper">
          <div className="progress-bar" style={{ width: `${downloadProgress}%` }} />
        </div>
      )}
    </div>
  );
}
