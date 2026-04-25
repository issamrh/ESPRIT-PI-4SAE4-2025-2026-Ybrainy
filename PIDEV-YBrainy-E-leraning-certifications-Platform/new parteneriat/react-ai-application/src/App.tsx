import { AnimatePresence, motion } from 'framer-motion';
import { RefreshCw, Sparkles } from 'lucide-react';
import { useMemo, useState } from 'react';
import { AnimatedButton } from './components/AnimatedButton';
import { CVUploadCard } from './components/CVUploadCard';
import { JobDescriptionCard } from './components/JobDescriptionCard';
import { Loader } from './components/Loader';
import { ProgressSteps } from './components/ProgressSteps';
import { ResultCard } from './components/ResultCard';
import { ResultTabs } from './components/ResultTabs';
import { SuccessBadge } from './components/SuccessBadge';
import { computeAtsScore } from './lib/ats';
import { generateApplication } from './lib/api';
import { downloadAsPdf } from './lib/pdf';
import { AtsResult, GenerateApplicationResponse, ResultTab } from './types';

function App() {
  const [cvText, setCvText] = useState('');
  const [cvFileName, setCvFileName] = useState<string | null>(null);
  const [jobDescription, setJobDescription] = useState('');
  const [result, setResult] = useState<GenerateApplicationResponse | null>(null);
  const [activeTab, setActiveTab] = useState<ResultTab>('optimizedCV');
  const [atsResult, setAtsResult] = useState<AtsResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);

  const currentStep: 1 | 2 | 3 = useMemo(() => {
    if (result) return 3;
    if (cvText.trim() && jobDescription.trim()) return 2;
    return 1;
  }, [cvText, jobDescription, result]);

  const activeResultText = result ? (activeTab === 'optimizedCV' ? result.optimizedCV : result.coverLetter) : '';

  const runGeneration = async () => {
    if (!cvText.trim() || !jobDescription.trim()) {
      setError('Please provide both CV content and job description before generating.');
      return;
    }

    setIsLoading(true);
    setError('');
    setShowSuccess(false);

    try {
      const generated = await generateApplication({
        cv: cvText,
        jobDescription,
      });

      setResult(generated);
      setActiveTab('optimizedCV');
      setAtsResult(computeAtsScore(jobDescription, generated.optimizedCV));
      setShowSuccess(true);
      window.setTimeout(() => setShowSuccess(false), 2500);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Generation failed. Please try again.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const updateResultText = (tab: ResultTab, next: string) => {
    setResult((prev) => {
      if (!prev) return prev;
      if (tab === 'optimizedCV') {
        setAtsResult(computeAtsScore(jobDescription, next));
        return { ...prev, optimizedCV: next };
      }
      return { ...prev, coverLetter: next };
    });
  };

  const handleCopy = async () => {
    await navigator.clipboard.writeText(activeResultText);
  };

  const handleDownload = () => {
    const fileName = activeTab === 'optimizedCV' ? 'optimized-cv.pdf' : 'cover-letter.pdf';
    const title = activeTab === 'optimizedCV' ? 'Optimized CV' : 'Cover Letter';
    downloadAsPdf(title, activeResultText, fileName);
  };

  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-8 md:px-8">
      <div className="pointer-events-none absolute inset-0 textured-bg opacity-35" />
      <div className="pointer-events-none absolute -left-24 top-14 h-64 w-64 rounded-full bg-brand-primary/15 blur-3xl" />
      <div className="pointer-events-none absolute -right-24 bottom-10 h-72 w-72 rounded-full bg-brand-secondary/20 blur-3xl" />

      <div className="relative mx-auto max-w-7xl space-y-6">
        <motion.header
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass-card rounded-2xl p-6 shadow-card"
        >
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="mb-2 inline-flex items-center gap-2 rounded-full bg-brand-primary/10 px-3 py-1 text-xs font-display font-semibold text-brand-primary">
                <Sparkles size={14} />
                AI Career Toolkit
              </p>
              <h1 className="font-display text-2xl font-bold text-brand-deep md:text-3xl">
                CV Optimizer + Cover Letter Generator
              </h1>
              <p className="mt-2 max-w-2xl text-sm text-slate-600">
                Upload a CV, paste a job description, and generate a polished ATS-friendly profile with a ready-to-send
                cover letter.
              </p>
            </div>

            <AnimatedButton
              label={result ? 'Regenerate' : 'Generate Application'}
              onClick={runGeneration}
              loading={isLoading}
              icon={<RefreshCw size={16} />}
            />
          </div>
        </motion.header>

        <ProgressSteps currentStep={currentStep} />

        <section className="grid grid-cols-1 gap-6 xl:grid-cols-2">
          <CVUploadCard
            cvText={cvText}
            fileName={cvFileName}
            onCvChange={(text, fileName) => {
              setCvText(text);
              setCvFileName(fileName ?? null);
            }}
          />
          <JobDescriptionCard value={jobDescription} onChange={setJobDescription} />
        </section>

        <AnimatePresence>
          {isLoading ? (
            <motion.div key="loader" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <Loader />
            </motion.div>
          ) : null}
        </AnimatePresence>

        {error ? (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {error}
          </motion.div>
        ) : null}

        <AnimatePresence>
          {result ? (
            <motion.section
              key="results"
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 10 }}
              className="space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <ResultTabs
                  activeTab={activeTab}
                  onChange={setActiveTab}
                  atsScore={atsResult ? atsResult.score : null}
                />
                {showSuccess ? <SuccessBadge /> : null}
              </div>

              <section className="grid grid-cols-1 gap-4 xl:grid-cols-[2fr,1fr]">
                <ResultCard
                  title={activeTab === 'optimizedCV' ? 'Optimized CV' : 'Cover Letter'}
                  value={activeResultText}
                  onChange={(text) => updateResultText(activeTab, text)}
                  onCopy={handleCopy}
                  onDownload={handleDownload}
                />

                <motion.aside
                  initial={{ opacity: 0, y: 18 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="glass-card rounded-2xl p-5 shadow-card"
                >
                  <h3 className="font-display text-lg font-semibold text-brand-ink">Keyword Match Insight</h3>
                  <p className="mt-1 text-sm text-slate-600">
                    Basic ATS estimate based on overlap between job description terms and optimized CV text.
                  </p>

                  <div className="mt-4 rounded-xl bg-brand-soft p-3">
                    <div className="mb-2 text-sm font-semibold text-brand-primary">
                      Match score: {atsResult ? atsResult.score : 0}%
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-brand-primary/15">
                      <motion.div
                        className="h-full rounded-full bg-gradient-to-r from-brand-primary to-brand-secondary"
                        initial={{ width: 0 }}
                        animate={{ width: `${atsResult ? atsResult.score : 0}%` }}
                      />
                    </div>
                  </div>

                  <div className="mt-4 space-y-3 text-sm">
                    <div>
                      <h4 className="mb-2 font-display font-semibold text-brand-ink">Matched keywords</h4>
                      <div className="flex flex-wrap gap-2">
                        {(atsResult?.matchedKeywords ?? []).map((keyword) => (
                          <span
                            key={`match-${keyword}`}
                            className="rounded-full bg-emerald-100 px-2 py-1 text-xs font-semibold text-emerald-700"
                          >
                            {keyword}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div>
                      <h4 className="mb-2 font-display font-semibold text-brand-ink">Missing keywords</h4>
                      <div className="flex flex-wrap gap-2">
                        {(atsResult?.missingKeywords ?? []).map((keyword) => (
                          <span
                            key={`missing-${keyword}`}
                            className="rounded-full bg-amber-100 px-2 py-1 text-xs font-semibold text-amber-700"
                          >
                            {keyword}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </motion.aside>
              </section>
            </motion.section>
          ) : null}
        </AnimatePresence>
      </div>
    </main>
  );
}

export default App;
