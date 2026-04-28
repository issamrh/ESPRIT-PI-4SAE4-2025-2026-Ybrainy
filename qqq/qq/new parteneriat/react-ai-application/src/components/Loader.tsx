import { motion } from 'framer-motion';
import { Sparkles } from 'lucide-react';

export function Loader() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-card rounded-2xl p-6 shadow-card"
    >
      <div className="mb-4 flex items-center gap-3 text-brand-primary">
        <Sparkles className="animate-pulse" size={20} />
        <p className="font-display text-sm font-semibold">Optimizing your application...</p>
      </div>

      <div className="h-2 w-full overflow-hidden rounded-full bg-brand-primary/15">
        <motion.div
          className="h-full rounded-full bg-gradient-to-r from-brand-primary to-brand-secondary"
          initial={{ x: '-100%' }}
          animate={{ x: ['-100%', '100%'] }}
          transition={{ duration: 1.2, repeat: Infinity, ease: 'easeInOut' }}
        />
      </div>
    </motion.div>
  );
}
