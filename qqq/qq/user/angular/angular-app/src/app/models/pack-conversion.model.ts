export interface PackConversionMetrics {
  algorithm: string | null;
  randomSeed: number | null;
  userCount: number | null;
  trainingRows: number | null;
  testRows: number | null;
  positiveRate: number | null;
  accuracy: number | null;
  rocAuc: number | null;
}

export interface PackConversionScore {
  packId: number | null;
  title: string | null;
  categoryId: number | null;
  categoryName: string | null;
  level: string | null;
  primarySkill: string | null;
  salePrice: number | null;
  originalPrice: number | null;
  discountPct: number | null;
  durationHours: number | null;
  marketDemandScore: number | null;
  marketAvgRoi: number | null;
  marketCourseCount: number | null;
  avgViews: number | null;
  avgCartAdds: number | null;
  observedPurchaseRate: number | null;
  predictedConversionRate: number | null;
  expectedBuyers: number | null;
  trainingSupport: number | null;
  expectedRevenue: number | null;
  conversionRank: number | null;
}

export interface PackConversionSummary {
  sourceFile: string;
  generatedAt: string;
  assumption: string | null;
  coursesFile: string | null;
  skillTrendsFile: string | null;
  modelMetrics: PackConversionMetrics | null;
  champion: PackConversionScore | null;
  topConversionPacks: PackConversionScore[];
}
