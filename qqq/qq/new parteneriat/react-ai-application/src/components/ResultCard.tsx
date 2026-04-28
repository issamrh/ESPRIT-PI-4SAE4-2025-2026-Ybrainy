import { motion } from 'framer-motion';
import { Check, Copy, Download } from 'lucide-react';
import { useState } from 'react';

interface ResultCardProps {
  title: string;
  value: string;
  onChange: (next: string) => void;
  onCopy: () => Promise<void>;
  onDownload: () => void;
}

export function ResultCard({ title, value, onChange, onCopy, onDownload }: ResultCardProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await onCopy();
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1400);
  };

  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      className="glass-card rounded-2xl p-5 shadow-card"
    >
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-display text-lg font-semibold text-brand-ink">{title}</h3>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex items-center gap-1 rounded-xl border border-brand-primary/25 bg-white/70 px-3 py-2 text-xs font-semibold text-brand-primary transition hover:scale-105 hover:border-brand-primary"
          >
            {copied ? <Check size={14} /> : <Copy size={14} />}
            {copied ? 'Copied' : 'Copy'}
          </button>
          <button
            type="button"
            onClick={onDownload}
            className="inline-flex items-center gap-1 rounded-xl border border-brand-secondary/30 bg-brand-secondary/10 px-3 py-2 text-xs font-semibold text-brand-secondary transition hover:scale-105 hover:border-brand-secondary"
          >
            <Download size={14} />
            PDF
          </button>
        </div>
      </div>

      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="scrollbar-fancy h-[460px] w-full resize-none rounded-2xl border border-brand-primary/20 bg-white/80 p-4 text-sm leading-6 text-slate-700 outline-none transition focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20"
      />
    </motion.section>
  );
}
