import { VideoMetadata } from '../types';

interface VideoInfoProps {
  metadata: VideoMetadata;
}

/**
 * Muestra la miniatura, título, duración y canal del video.
 */
export function VideoInfo({ metadata }: VideoInfoProps) {
  const formatViews = (n: number) => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(0)}K`;
    return n.toLocaleString();
  };

  return (
    <div className="video-info">
      {metadata.thumbnail && (
        <div className="thumbnail-wrapper">
          <img
            src={metadata.thumbnail}
            alt={metadata.title}
            className="thumbnail"
          />
          <span className="duration-badge">{metadata.durationString}</span>
        </div>
      )}
      <div className="video-details">
        <h2 className="video-title">{metadata.title}</h2>
        <div className="video-meta-row">
          <span className="channel"> {metadata.uploader}</span>
          {metadata.viewCount > 0 && (
            <span className="views"> {formatViews(metadata.viewCount)} vistas</span>
          )}
        </div>
      </div>
    </div>
  );
}
