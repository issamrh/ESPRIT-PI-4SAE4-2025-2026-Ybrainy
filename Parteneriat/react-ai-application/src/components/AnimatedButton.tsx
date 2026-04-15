import { motion } from 'framer-motion';
import { Loader2 } from 'lucide-react';
import { ReactNode } from 'react';

interface AnimatedButtonProps {
  label: string;
  onClick?: () => void;
  type?: 'button' | 'submit';
  disabled?: boolean;
  loading?: boolean;
  icon?: ReactNode;
  className?: string;
}

export function AnimatedButton({
  label,
  onClick,
  type = 'button',
  disabled = false,
  loading = false,
  icon,
  className = '',
}: AnimatedButtonProps) {
  return (
    <motion.button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      whileHover={disabled || loading ? undefined : { scale: 1.03 }}
      whileTap={disabled || loading ? undefined : { scale: 0.98 }}
      transition={{ duration: 0.25 }}
      className={`group relative inline-flex items-center justify-center gap-2 overflow-hidden rounded-2xl px-6 py-3 font-display text-sm font-semibold text-white shadow-card transition-all duration-300 ${
        disabled || loading
          ? 'cursor-not-allowed bg-slate-400'
          : 'bg-gradient-to-r from-brand-primary to-brand-secondary hover:shadow-glow'
      } ${className}`}
    >
      <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-white/0 via-white/25 to-white/0 transition-transform duration-500 group-hover:translate-x-full" />
      {loading ? <Loader2 size={16} className="animate-spin" /> : icon}
      <span className="relative z-10">{loading ? 'Generating...' : label}</span>
    </motion.button>
  );
}
