import { motion } from 'framer-motion';
import { ResultTab } from '../types';

interface ResultTabsProps {
  activeTab: ResultTab;
  onChange: (tab: ResultTab) => void;
  atsScore: number | null;
}

export function ResultTabs({ activeTab, onChange, atsScore }: ResultTabsProps) {
  const tabs: Array<{ key: ResultTab; label: string }> = [
    { key: 'optimizedCV', label: 'Optimized CV' },
    { key: 'coverLetter', label: 'Cover Letter' },
  ];

  return (
    <div className="glass-card rounded-2xl p-4 shadow-card">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => onChange(tab.key)}
              className={`relative rounded-xl px-4 py-2 text-sm font-display font-semibold transition ${
                isActive ? 'text-white' : 'text-brand-ink hover:text-brand-primary'
              }`}
            >
              {isActive ? (
                <motion.span
                  layoutId="activeTab"
                  className="absolute inset-0 rounded-xl bg-gradient-to-r from-brand-primary to-brand-secondary"
                  transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                />
              ) : null}
              <span className="relative z-10">{tab.label}</span>
            </button>
          );
        })}
      </div>

      {atsScore !== null ? (
        <div className="rounded-xl bg-brand-soft px-3 py-2 text-sm text-brand-ink">
          ATS Match Score: <span className="font-display font-bold text-brand-primary">{atsScore}%</span>
        </div>
      ) : null}
    </div>
  );
}
