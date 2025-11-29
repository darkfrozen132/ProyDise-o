import { Play, Pause, RotateCcw } from 'lucide-react';

interface ControlesSimulacionProps {
  estaActivo: boolean;
  onIniciar: () => void;
  onPausar: () => void;
  onReiniciar: () => void;
}

export function ControlesSimulacion({ estaActivo, onIniciar, onPausar, onReiniciar }: ControlesSimulacionProps) {
  return (
    <div className="absolute top-4 left-1/2 -translate-x-1/2 z-[1000] p-2 bg-base-100/80 backdrop-blur-sm rounded-lg shadow-lg flex gap-2">
      {!estaActivo ? (
        <button className="btn btn-success btn-sm" onClick={onIniciar}>
          <Play size={16} /> Iniciar
        </button>
      ) : (
        <button className="btn btn-warning btn-sm" onClick={onPausar}>
          <Pause size={16} /> Pausar
        </button>
      )}
      <button className="btn btn-ghost btn-sm" onClick={onReiniciar}>
        <RotateCcw size={16} /> Reiniciar
      </button>
    </div>
  );
}
