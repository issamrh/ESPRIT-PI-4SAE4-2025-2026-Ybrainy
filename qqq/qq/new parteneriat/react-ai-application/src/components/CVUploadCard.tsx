import { motion } from 'framer-motion';
import { FileText, UploadCloud } from 'lucide-react';
import { ChangeEvent, DragEvent, useRef, useState } from 'react';

interface CVUploadCardProps {
  cvText: string;
  fileName: string | null;
  onCvChange: (text: string, fileName?: string | null) => void;
}

export function CVUploadCard({ cvText, fileName, onCvChange }: CVUploadCardProps) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState('');

  const processFile = (file: File) => {
    const reader = new FileReader();
    reader.onload = () => {
      const raw = typeof reader.result === 'string' ? reader.result.trim() : '';
      if (!raw) {
        setError('Unable to read this file. Paste your CV text manually below.');
        return;
      }
      setError('');
      onCvChange(raw, file.name);
    };
    reader.onerror = () => setError('File reading failed. Try another file or paste manually.');
    reader.readAsText(file);
  };

  const onFileInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      processFile(file);
    }
  };

  const onDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragActive(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      processFile(file);
    }
  };

  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      className="glass-card rounded-2xl p-6 shadow-card transition duration-300 hover:scale-[1.01] hover:shadow-xl"
    >
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-brand-ink">1. Upload CV</h2>
        <span className="rounded-full bg-brand-soft px-3 py-1 text-xs font-semibold text-brand-primary">
          Drag and Drop
        </span>
      </div>

      <div
        onDragOver={(event) => {
          event.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        className={`mb-4 flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed p-7 text-center transition-all ${
          dragActive
            ? 'border-brand-primary bg-brand-primary/10 scale-[1.01]'
            : 'border-brand-primary/30 bg-white/70 hover:border-brand-primary'
        }`}
      >
        <UploadCloud className="mb-2 text-brand-primary" size={28} />
        <p className="font-display text-sm font-semibold text-brand-ink">Drop CV file here or click to browse</p>
        <p className="mt-1 text-xs text-slate-500">TXT, DOC, DOCX, PDF (text extraction quality may vary)</p>
      </div>

      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept=".txt,.md,.rtf,.doc,.docx,.pdf"
        onChange={onFileInputChange}
      />

      {fileName ? (
        <div className="mb-4 inline-flex items-center gap-2 rounded-xl bg-brand-soft px-3 py-2 text-sm text-brand-primary">
          <FileText size={16} />
          {fileName}
        </div>
      ) : null}

      {error ? <div className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}

      <textarea
        value={cvText}
        onChange={(event) => onCvChange(event.target.value, fileName)}
        placeholder="Or paste your raw CV text here..."
        className="scrollbar-fancy h-72 w-full resize-none rounded-2xl border border-brand-primary/20 bg-white/80 p-4 text-sm text-slate-700 outline-none transition focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20"
      />

      <div className="mt-3 text-right text-xs font-semibold text-brand-ink/75">{cvText.length} characters</div>
    </motion.section>
  );
}
