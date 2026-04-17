import { motion } from 'framer-motion';
import { CheckCircle2 } from 'lucide-react';

export function SuccessBadge() {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.85 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 230, damping: 18 }}
      className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-4 py-2 text-sm font-semibold text-emerald-700"
    >
      <CheckCircle2 size={17} />
      Generation complete
    </motion.div>
  );
}
