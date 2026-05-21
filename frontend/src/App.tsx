import { useState } from 'react';
import { VideoForm } from './components/VideoForm';
import { VideoInfo } from './components/VideoInfo';
import { DownloadOptions } from './components/DownloadOptions';
import { fetchMetadata, downloadVideo, extractErrorMessage } from './services/api';
import { AppStatus, DownloadType, VideoMetadata } from './types';
import './App.css';

export default function App() {
  // ── Estado global de la app ────────────────────────────────────────────
  const [status, setStatus] = useState<AppStatus>('idle');
  const [url, setUrl] = useState('');
  const [metadata, setMetadata] = useState<VideoMetadata | null>(null);
  const [downloadType, setDownloadType] = useState<DownloadType>('video');
  const [quality, setQuality] = useState('best');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [downloadProgress, setDownloadProgress] = useState<number | null>(null);

  // ── Handlers ───────────────────────────────────────────────────────────

  const handleFetchMetadata = async (inputUrl: string) => {
    setUrl(inputUrl);
    setError(null);
    setMetadata(null);
    setStatus('loading-meta');

    try {
      const data = await fetchMetadata(inputUrl);
      setMetadata(data);
      setQuality(data.availableQualities[0] ?? 'best');
      setStatus('ready');
    } catch (err) {
      setError(extractErrorMessage(err));
      setStatus('error');
    }
  };

  const handleDownload = async () => {
    if (!metadata) return;
    setError(null);
    setStatus('downloading');
    setDownloadProgress(null);

    try {
      await downloadVideo(
        { url, type: downloadType, quality, startTime, endTime },
        (percent) => setDownloadProgress(percent)
      );
      // Volver a estado "ready" después de la descarga exitosa
      setStatus('ready');
      setDownloadProgress(null);
    } catch (err) {
      setError(extractErrorMessage(err));
      setStatus('error');
      setDownloadProgress(null);
    }
  };

  const handleReset = () => {
    setStatus('idle');
    setMetadata(null);
    setError(null);
    setUrl('');
    setDownloadProgress(null);
  };

  // ── Render ─────────────────────────────────────────────────────────────
  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-text">Descargador</span>
          </div>
          <p className="header-sub">Descarga videos de YouTube</p>
        </div>
      </header>

      {/* Main content */}
      <main className="main">
        <div className="card">

          {/* Formulario URL */}
          <VideoForm
            onSubmit={handleFetchMetadata}
            isLoading={status === 'loading-meta'}
          />

          {/* Error */}
          {error && (
            <div className="error-box">
              <span className="error-icon">⚠</span>
              <span>{error}</span>
              <button className="btn-close-error" onClick={() => setError(null)}>×</button>
            </div>
          )}

          {/* Metadatos del video */}
          {metadata && (
            <>
              <div className="divider" />
              <VideoInfo metadata={metadata} />

              <div className="divider" />

              {/* Opciones de descarga */}
              <DownloadOptions
                downloadType={downloadType}
                quality={quality}
                startTime={startTime}
                endTime={endTime}
                metadata={metadata}
                isDownloading={status === 'downloading'}
                downloadProgress={downloadProgress}
                onTypeChange={setDownloadType}
                onQualityChange={setQuality}
                onStartTimeChange={setStartTime}
                onEndTimeChange={setEndTime}
                onDownload={handleDownload}
              />

              <button className="btn-reset" onClick={handleReset}>
                Nuevo video
              </button>
            </>
          )}

        </div>
      </main>
    </div>
  );
}
