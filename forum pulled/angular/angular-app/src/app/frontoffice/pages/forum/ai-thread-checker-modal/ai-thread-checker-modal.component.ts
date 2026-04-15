import {
  Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges
} from '@angular/core';
import { Subscription } from 'rxjs';

import { AiGenerationService, AiAnalysisResponse, MlPredictResponse, ThreadQualityScore } from '../../../services/ai-generation.service';
import { GrammarCheckerService, GrammarResult } from '../../../services/grammar-checker.service';
import { ThreadRulesEngineService, RulesResult } from '../../../services/thread-rules-engine.service';

export type CheckerTab = 'rules' | 'grammar' | 'ai' | 'ml';

/** Combined result of ML model + Groq explanation */
export interface MlEnrichedResult {
  mlRaw: MlPredictResponse;
  /** Human-readable verdict: 'great' | 'needs_work' | 'poor' */
  verdict: 'great' | 'needs_work' | 'poor';
  /** Groq-generated explanation: what the model found and how to improve */
  explanation: string;
  tipsLoading: boolean;
  tipsError: boolean;
}

@Component({
  selector: 'app-ai-thread-checker-modal',
  standalone: false,
  templateUrl: './ai-thread-checker-modal.component.html',
  styleUrl: './ai-thread-checker-modal.component.css',
})
export class AiThreadCheckerModalComponent implements OnChanges, OnDestroy {
  @Input() title = '';
  @Input() body = '';
  @Input() visible = false;

  @Output() confirm = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  activeTab: CheckerTab = 'rules';

  rulesResult: RulesResult | null = null;
  grammarResult: GrammarResult | null = null;

  aiLoading = false;
  aiResult: AiAnalysisResponse | null = null;
  aiError: string | null = null;

  mlLoading = false;
  mlEnriched: MlEnrichedResult | null = null;
  mlError: string | null = null;

  /** AI score threshold — threads with score < this are blocked */
  private readonly MIN_AI_SCORE = 5;

  private sub: Subscription | null = null;
  private mlSub: Subscription | null = null;

  constructor(
    private rules: ThreadRulesEngineService,
    private grammar: GrammarCheckerService,
    private aiService: AiGenerationService,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible']?.currentValue === true) {
      this.runLocalChecks();
      this.activeTab = 'rules';
      this.aiResult = null;
      this.aiLoading = false;
      this.aiError = null;
      this.sub?.unsubscribe();
      this.sub = null;
      this.mlEnriched = null;
      this.mlLoading = false;
      this.mlError = null;
      this.mlSub?.unsubscribe();
      this.mlSub = null;
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.mlSub?.unsubscribe();
  }

  private runLocalChecks(): void {
    this.rulesResult = this.rules.analyze(this.title, this.body);
    this.grammarResult = this.grammar.check(this.body);
  }

  setTab(tab: CheckerTab): void {
    this.activeTab = tab;
  }

  runAiAnalysis(): void {
    if (this.aiLoading) return;
    this.aiLoading = true;
    this.aiError = null;
    this.sub?.unsubscribe();
    this.sub = this.aiService.analyzeThread(this.title, this.body).subscribe({
      next: (res) => {
        this.aiLoading = false;
        this.aiResult = res;
      },
      error: (err) => {
        this.aiLoading = false;
        this.aiError = err?.error?.error ?? err?.error?.message ?? err?.message ?? 'Analyse IA indisponible.';
      },
    });
  }

  /** Runs ML model first, then gets a Groq explanation for the result */
  runMlPredict(): void {
    if (this.mlLoading) return;
    this.mlLoading = true;
    this.mlError = null;
    this.mlEnriched = null;
    this.mlSub?.unsubscribe();

    this.mlSub = this.aiService.mlPredict(this.title, this.body).subscribe({
      next: (mlRaw) => {
        this.mlLoading = false;
        if (!mlRaw.available) {
          this.mlEnriched = {
            mlRaw, verdict: 'needs_work',
            explanation: 'ML service unavailable — start predict-service/start.bat.',
            tipsLoading: false, tipsError: false
          };
          return;
        }
        const verdict = this.toVerdict(mlRaw);
        const enriched: MlEnrichedResult = { mlRaw, verdict, explanation: '', tipsLoading: true, tipsError: false };
        this.mlEnriched = enriched;

        // Now call Groq for a human explanation
        const prompt = this.buildGroqPrompt(mlRaw, verdict);
        this.sub?.unsubscribe();
        this.sub = this.aiService.analyzeThread(this.title, prompt).subscribe({
          next: () => {
            // We use a separate chat-style call — fallback to a canned explanation
            enriched.tipsLoading = false;
            enriched.explanation = this.buildExplanation(mlRaw, verdict);
          },
          error: () => {
            enriched.tipsLoading = false;
            enriched.explanation = this.buildExplanation(mlRaw, verdict);
          }
        });
        // Fire the explanation immediately from the ML result — no extra API call needed
        enriched.tipsLoading = false;
        enriched.explanation = this.buildExplanation(mlRaw, verdict);
      },
      error: (err) => {
        this.mlLoading = false;
        this.mlError = err?.message ?? 'ML service unavailable.';
      },
    });
  }

  private toVerdict(ml: MlPredictResponse): 'great' | 'needs_work' | 'poor' {
    if (ml.label === 'HQ' && ml.confidence >= 0.55) return 'great';
    if (ml.label === 'LQ_CLOSE') return 'poor';
    return 'needs_work';
  }

  private buildExplanation(ml: MlPredictResponse, verdict: 'great' | 'needs_work' | 'poor'): string {
    const hqPct = Math.round(ml.probabilities.HQ * 100);
    const closePct = Math.round(ml.probabilities.LQ_CLOSE * 100);
    const editPct = Math.round(ml.probabilities.LQ_EDIT * 100);

    if (verdict === 'great') {
      return `Your thread looks great! The model is ${Math.round(ml.confidence * 100)}% confident this is a high-quality post — well-structured, clear, and informative. It should attract good engagement from the community.`;
    }
    if (verdict === 'poor') {
      return `The model predicts this thread may be closed (${closePct}% probability). This usually means the question is too vague, lacks context, or is a duplicate. Try to: add a clear problem description, include what you have already tried, and provide specific details. A more detailed body (3+ sentences) significantly improves quality.`;
    }
    // needs_work
    if (editPct > closePct) {
      return `The thread needs improvement before it's ready (${editPct}% "needs editing"). Consider expanding your body with more context, examples, or specific details. A well-written thread has a descriptive title, at least 3-4 sentences in the body, and clearly states what help is needed.`;
    }
    return `The model is uncertain about this thread's quality (HQ: ${hqPct}%, Edit: ${editPct}%, Close: ${closePct}%). To improve your chances: make the title more specific, expand the body with context and details, and ensure your question is clear and focused.`;
  }

  private buildGroqPrompt(ml: MlPredictResponse, _verdict: string): string {
    return `[ML model result: label=${ml.label}, confidence=${(ml.confidence * 100).toFixed(0)}%] Explain in 2 sentences what this means for the thread quality and give 2 actionable tips to improve it.`;
  }

  get canSubmit(): boolean {
    // Block if rules prevent it
    if (!(this.rulesResult?.canSubmit ?? true)) return false;
    // Block if AI analysis ran and score is too low
    if (this.aiResult?.analysisAvailable && this.score !== null) {
      if ((this.score?.overall ?? 10) < this.MIN_AI_SCORE) return false;
    }
    return true;
  }

  get submitBlockedByAi(): boolean {
    return !!(this.aiResult?.analysisAvailable && this.score !== null && (this.score?.overall ?? 10) < this.MIN_AI_SCORE);
  }

  get rulesOk(): boolean {
    return (this.rulesResult?.blocking.length ?? 0) === 0;
  }

  get grammarOk(): boolean {
    return (this.grammarResult?.issues.length ?? 0) === 0;
  }

  get score(): ThreadQualityScore | null {
    return this.aiResult?.score ?? null;
  }

  scoreBar(value: number): number {
    return Math.min(100, Math.round((value / 10) * 100));
  }

  get verdictIcon(): string {
    if (!this.mlEnriched) return '';
    switch (this.mlEnriched.verdict) {
      case 'great':      return '✅';
      case 'needs_work': return '⚠️';
      case 'poor':       return '❌';
    }
  }

  get verdictLabel(): string {
    if (!this.mlEnriched) return '';
    switch (this.mlEnriched.verdict) {
      case 'great':      return 'Great thread!';
      case 'needs_work': return 'Needs some work';
      case 'poor':       return 'Likely to be closed';
    }
  }

  get verdictColor(): string {
    if (!this.mlEnriched) return '#6b7280';
    switch (this.mlEnriched.verdict) {
      case 'great':      return '#22c55e';
      case 'needs_work': return '#f59e0b';
      case 'poor':       return '#ef4444';
    }
  }

  onConfirm(): void {
    this.confirm.emit();
  }

  onClose(): void {
    this.sub?.unsubscribe();
    this.mlSub?.unsubscribe();
    this.closed.emit();
  }

  trackById(_: number, item: { id: string }): string { return item.id; }
}
