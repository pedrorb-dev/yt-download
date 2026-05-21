import React, { useState } from 'react';

interface VideoFormProps {
  onSubmit: (url: string) => void;
  isLoading: boolean;
}

/**
 * Formulario para ingresar la URL de YouTube y obtener metadatos.
 */
export function VideoForm({ onSubmit, isLoading }: VideoFormProps) {
  const [url, setUrl] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = url.trim();
    if (trimmed) onSubmit(trimmed);
  };

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setUrl(text);
    } catch {
      // por implementar, usuario no dió permiso de usar el portapapeles
    }
  };

  return (
    <form onSubmit={handleSubmit} className="url-form">
      <div className="input-row">
        <input
          type="url"
          className="url-input"
          placeholder="https://www.youtube.com/watch?v=..."
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          disabled={isLoading}
          required
        />
        <button
          type="button"
          className="btn-paste"
          onClick={handlePaste}
          disabled={isLoading}
          title="Pegar desde portapapeles"
        >
          pegar
        </button>
      </div>
      <button
        type="submit"
        className="btn-primary"
        disabled={isLoading || !url.trim()}
      >
        {isLoading ? (
          <span className="loading-dots">Analizando<span>.</span><span>.</span><span>.</span></span>
        ) : (
          'Analizar Video'
        )}
      </button>
    </form>
  );
}
