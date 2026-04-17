export interface ForecastHorizon {
  from: string | null;
  to: string | null;
  monthsAhead: number | null;
  incomeTotal: number | null;
  expensesTotal: number | null;
  profitTotal: number | null;
}

export interface ForecastContext {
  marginPct: number | null;
  marginTrendPct: number | null;
  topRevenueSource: string | null;
  watchCategory: string | null;
  watchGrowthPct: number | null;
  forecastHorizon: ForecastHorizon | null;
}

export interface RecommendationItem {
  recommendationRank: number | null;
  title: string | null;
  platform: string | null;
  skillCategory: string | null;
  hybridFinalScore: number | null;
  confidenceLevel: string | null;
  impactBand: string | null;
  recommendationReason: string | null;
  url: string | null;
  month?: string | null;
  marginBefore?: number | null;
  marginAfter?: number | null;
  profitBefore?: number | null;
  profitAfter?: number | null;
  profitLowBefore?: number | null;
  profitLowAfter?: number | null;
  actionKey?: string | null;
  targetMetric?: string | null;
  priority?: string | null;
  how?: string | null;
  expectedOutcome?: string | null;
  successSignal?: string | null;
}

export interface ImprovementAction {
  priority: string | null;
  action: string | null;
  rationale: string | null;
  targetMetric: string | null;
}

export interface ExecutiveMetric {
  metric: string | null;
  value: string | null;
  notes: string | null;
  status: string | null;
}

export interface UserPlaybookStep {
  step: number | null;
  window: string | null;
  priority: string | null;
  action: string | null;
  why: string | null;
  how: string | null;
  expectedImpact: string | null;
  successSignal: string | null;
  targetMetric: string | null;
}

export interface RecommendationSummary {
  sourceFile: string;
  generatedAt: string;
  forecastContext: ForecastContext | null;
  executiveMetrics: ExecutiveMetric[];
  topRecommendations: RecommendationItem[];
  improvementActions: ImprovementAction[];
  userPlaybook: UserPlaybookStep[];
}
