import { motion } from 'framer-motion';

interface ProgressStepsProps {
  currentStep: 1 | 2 | 3;
}

const steps = [
  { id: 1, label: 'Upload CV' },
  { id: 2, label: 'Job Match' },
  { id: 3, label: 'Results' },
] as const;

export function ProgressSteps({ currentStep }: ProgressStepsProps) {
  return (
    <div className="glass-card rounded-2xl p-4 shadow-card">
      <div className="grid grid-cols-3 gap-2">
        {steps.map((step) => {
          const active = currentStep === step.id;
          const completed = currentStep > step.id;

          return (
            <motion.div
              key={step.id}
              layout
              className={`rounded-xl border px-3 py-3 text-center transition ${
                completed
                  ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
                  : active
                    ? 'border-brand-primary bg-brand-primary/10 text-brand-primary'
                    : 'border-slate-200 bg-white/60 text-slate-500'
              }`}
            >
              <div className="text-xs font-display font-semibold">Step {step.id}</div>
              <div className="text-sm font-semibold">{step.label}</div>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
