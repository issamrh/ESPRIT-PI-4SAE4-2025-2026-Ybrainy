import { motion } from 'framer-motion';

interface JobDescriptionCardProps {
  value: string;
  onChange: (value: string) => void;
}

export function JobDescriptionCard({ value, onChange }: JobDescriptionCardProps) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay: 0.08 }}
      className="glass-card rounded-2xl p-6 shadow-card transition duration-300 hover:scale-[1.01] hover:shadow-xl"
    >
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-lg font-semibold text-brand-ink">2. Job Description</h2>
        <span className="rounded-full bg-brand-soft px-3 py-1 text-xs font-semibold text-brand-primary">
          Required
        </span>
      </div>

      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Paste the full job description here..."
        className="scrollbar-fancy h-72 w-full resize-none rounded-2xl border border-brand-primary/20 bg-white/80 p-4 text-sm text-slate-700 outline-none transition focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20"
      />

      <div className="mt-3 text-right text-xs font-semibold text-brand-ink/75">{value.length} characters</div>
    </motion.section>
  );
}
