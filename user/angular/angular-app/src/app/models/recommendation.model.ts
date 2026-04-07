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
}

export interface ImprovementAction {
  priority: string | null;
  action: string | null;
  rationale: string | null;
  targetMetric: string | null;
}

export interface RecommendationSummary {
  sourceFile: string;
  generatedAt: string;
  forecastContext: ForecastContext | null;
  topRecommendations: RecommendationItem[];
  improvementActions: ImprovementAction[];
}
