export interface FinanceScenarioControl {
  name: string;
  label: string;
  min: number;
  max: number;
  unit: string;
}

export interface FinanceScenarioBaselineSummary {
  forecastFrom: string;
  forecastTo: string;
  monthsHorizon: number;
  baselineIncomeTotal: number;
  baselineExpensesTotal: number;
  baselineProfitTotal: number;
  baselineMarginAvg: number;
  baselineHighRiskMonths: number;
}

export interface FinanceScenarioOpportunity {
  skillCategory: string;
  marketOpportunityScore: number;
  marketAvgRoi: number;
  trendTotalViews: number;
  trendTotalEngagement: number;
  marketCourseCount: number;
}

export interface FinanceScenarioMarketSummary {
  globalMarketSignal: number;
  topOpportunityScore: number;
  weightedMarketRoi: number;
  weightedSalaryBoost: number;
  topSkillCategory: string;
  topOpportunities: FinanceScenarioOpportunity[];
  trackedSkills: number;
  totalScraperViews: number;
}

export interface FinanceScenarioPricingRecommendation {
  packId: number;
  title: string;
  categoryName: string;
  priceAction: string;
  recommendedSalePrice: number;
  revenueLiftPct: number;
  pricingRank: number;
}

export interface FinanceScenarioPricingSummary {
  portfolioRevenueUpliftPct: number;
  packsToIncreasePrice: number;
  packsToDecreasePrice: number;
  packsToHoldPrice: number;
  topPricingRecommendations: FinanceScenarioPricingRecommendation[];
}

export interface FinanceScenarioRecommendationContext {
  avgMarginUpliftPts: number;
  avgProfitUplift: number;
  watchCategory: string;
  watchGrowthPct: number;
  monthsAtRiskBefore: number;
  monthsAtRiskAfter: number;
  bestGainWindow: string;
  userPlaybook: Array<{
    step: number | null;
    window: string | null;
    priority: string | null;
    action: string | null;
    why: string | null;
    how: string | null;
    expectedImpact: string | null;
    successSignal: string | null;
    targetMetric: string | null;
  }>;
}

export interface FinanceScenarioModelMeta {
  algorithm: string;
  randomSeed: number;
  scenariosPerMonth: number;
  trainingRows: number;
  testRows: number;
  incomeMae: number;
  expenseMae: number;
  incomeR2: number;
  expenseR2: number;
  riskAccuracy: number;
}

export interface FinanceScenarioRanking {
  scenarioName: string;
  scenarioSlug: string;
  description: string;
  monthsHorizon: number;
  projectedIncomeTotal: number;
  projectedExpensesTotal: number;
  projectedProfitTotal: number;
  projectedMarginPct: number;
  profitUpliftPct: number;
  marginUpliftPts: number;
  incomeUpliftPct: number;
  expenseDeltaPct: number;
  riskLevel: string;
  highRiskMonths: number;
  mediumRiskMonths: number;
  lowRiskMonths: number;
  marketingBudgetChangePct: number;
  dynamicPricingRolloutPct: number;
  newPackLaunches: number;
  costControlPct: number;
  salaryOptimizationPct: number;
  marketDemandShockPct: number;
  supportAutomationPct: number;
  focusTopMarketPct: number;
  scenarioRank: number;
}

export interface FinanceScenarioMonthPath {
  scenarioName: string;
  scenarioSlug: string;
  description: string;
  month: string;
  baselineIncome: number;
  baselineExpenses: number;
  baselineProfit: number;
  baselineMargin: number;
  projectedIncome: number;
  projectedExpenses: number;
  projectedProfit: number;
  projectedMargin: number;
  riskLevel: string;
  salaryExpense: number;
  marketingExpense: number;
  infrastructureExpense: number;
  softwareExpense: number;
  otherContentExpense: number;
  otherSupportExpense: number;
}

export interface FinanceScenarioDashboardSnapshot {
  month: string;
  income: number;
  expenses: number;
  profit: number;
  margin: number;
}

export interface FinanceScenarioSummary {
  generatedAt: string;
  assumption: string;
  baselineSummary: FinanceScenarioBaselineSummary;
  marketSummary: FinanceScenarioMarketSummary;
  pricingSummary: FinanceScenarioPricingSummary;
  recommendationContext: FinanceScenarioRecommendationContext;
  model: FinanceScenarioModelMeta;
  scenarioControls: FinanceScenarioControl[];
  scenarioRankings: FinanceScenarioRanking[];
  recommendedScenario: FinanceScenarioRanking;
  recommendedMonthlyPath: FinanceScenarioMonthPath[];
  dashboardSnapshot: FinanceScenarioDashboardSnapshot | null;
}
