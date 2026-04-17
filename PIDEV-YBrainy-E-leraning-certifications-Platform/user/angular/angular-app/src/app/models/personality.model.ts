export interface Personality {
  personalityId: string;
  userId: number;
  visualLearningPct: number;
  auditoryLearningPct: number;
  kinestheticLearningPct: number;
  careerAlignmentScore: number;
  cognitiveLoadTolerance: number;
  careerGoals: string[];
  behavior?: Behavior | null;
}

export interface Behavior {
  behaviorId: string;
  userId: number;
  agitationLevelPct: number;
  focusScorePct: number;
  engagementIndexPct: number;
  learningPacePercentile: number;
  fraudProbabilityScore: number;
  lastInteraction: string;
}

export interface PersonalityRequest {
  userId: number;
  visualLearningPct: number;
  auditoryLearningPct: number;
  kinestheticLearningPct: number;
  careerAlignmentScore: number;
  cognitiveLoadTolerance: number;
  careerGoals: string[];
  behavior?: BehaviorRequest;
}

export interface BehaviorRequest {
  userId: number;
  agitationLevelPct: number;
  focusScorePct: number;
  engagementIndexPct: number;
  learningPacePercentile: number;
  fraudProbabilityScore: number;
}

export interface UserPersonalitySummary {
  userId: number;
  userName: string;
  dominantStyle: 'VISUAL' | 'AUDITORY' | 'KINESTHETIC';
  alignmentScore: number;
  lastUpdated: string;
  alertStatus: 'GOOD' | 'WARNING' | 'CRITICAL';
}
