export interface PackPricingMetrics {
  algorithm: string | null;
  randomSeed: number | null;
  userCount: number | null;
  discountGridPct: number[];
  trainingRows: number | null;
  testRows: number | null;
  positiveRate: number | null;
  accuracy: number | null;
  rocAuc: number | null;
}

export interface PackPricingPortfolioSummary {
  currentExpectedRevenueTotal: number | null;
  recommendedExpectedRevenueTotal: number | null;
  revenueUpliftPct: number | null;
  packsToIncreasePrice: number | null;
  packsToDecreasePrice: number | null;
  packsToHoldPrice: number | null;
}

export interface PackPricingRecommendation {
  packId: number | null;
  title: string | null;
  categoryId: number | null;
  categoryName: string | null;
  level: string | null;
  primarySkill: string | null;
  originalPrice: number | null;
  currentSalePrice: number | null;
  currentDiscountPct: number | null;
  recommendedSalePrice: number | null;
  recommendedDiscountPct: number | null;
  discountRangeMinPct: number | null;
  discountRangeMaxPct: number | null;
  recommendedBand: string | null;
  priceAction: string | null;
  baselineConversionRate: number | null;
  recommendedConversionRate: number | null;
  baselineExpectedRevenue: number | null;
  recommendedExpectedRevenue: number | null;
  revenueLiftPct: number | null;
  conversionLiftPct: number | null;
  pricingConfidence: number | null;
  marketDemandScore: number | null;
  marketAvgRoi: number | null;
  marketCourseCount: number | null;
  scenarioCount: number | null;
  pricingRank: number | null;
}

export interface PackPricingSummary {
  sourceFile: string;
  generatedAt: string;
  assumption: string | null;
  coursesFile: string | null;
  skillTrendsFile: string | null;
  modelMetrics: PackPricingMetrics | null;
  portfolioSummary: PackPricingPortfolioSummary | null;
  champion: PackPricingRecommendation | null;
  topPricingRecommendations: PackPricingRecommendation[];
}
