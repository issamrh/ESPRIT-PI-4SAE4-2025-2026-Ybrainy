export type ThreadStatus = 'OPEN' | 'CLOSED' | 'LOCKED';
export type VoteType = 'UPVOTE' | 'DOWNVOTE';
export type ReactionType = 'LIKE' | 'DISLIKE';

export interface ThreadInteractionStatus {
  userVote: VoteType | null;
  userReaction: ReactionType | null;
  wishlisted: boolean;
  upvoteCount: number;
  downvoteCount: number;
  voteScore: number;
  likeCount: number;
  dislikeCount: number;
}

export interface AuthorResponse {
  id: number;
  username: string;
  email?: string;
  avatarUrl?: string | null;
  level?: number;
  levelTitle?: string;
  role?: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description: string;
}

export interface ThreadResponse {
  id: number;
  title: string;
  body: string;
  imageUrl?: string | null;
  fileUrl?: string | null;
  fileType?: string | null;
  mediaUrl?: string | null;
  mediaType?: string | null;
  status: ThreadStatus;
  author: AuthorResponse | null;
  authorId?: number;
  category: CategoryResponse | null;
  categoryId?: number | null;
  createdAt: string;
  updatedAt?: string;
  upvoteCount: number;
  downvoteCount: number;
  voteScore: number;
  likeCount: number;
  dislikeCount: number;
  savedByCurrentUser?: boolean;
  aiAnalyzed?: boolean;
  aiOverallScore?: number | null;
  aiLabel?: string | null;
  aiLabelColor?: string | null;
  aiSummary?: string | null;
}

export interface ThreadRequest {
  title: string;
  body: string;
  authorId: number;
  categoryId?: number | null;
  mediaUrl?: string | null;
  mediaType?: string | null;
}

export interface PostResponse {
  id: number;
  body: string;
  imageUrl?: string | null;
  fileUrl?: string | null;
  fileType?: string | null;
  mediaUrl?: string | null;
  mediaType?: string | null;
  author: AuthorResponse | null;
  authorId?: number;
  threadId: number;
  threadTitle?: string;
  bestAnswer?: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface PostRequest {
  body: string;
  threadId: number;
  authorId: number;
  mediaUrl?: string | null;
  mediaType?: string | null;
}

export interface CommentResponse {
  id: number;
  body: string;
  author: AuthorResponse | null;
  authorId?: number;
  postId: number;
  threadId?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface CommentRequest {
  body: string;
  postId: number;
  authorId: number;
  threadId?: number;
}

export interface UserProfileResponse {
  id: number;
  username: string;
  email: string;
  role: string;
  bio?: string;
  avatarUrl?: string;
  streakDays: number;
  xp: number;
  level: number;
  levelTitle: string;
  xpCurrentLevelMin: number;
  xpNextLevelMin: number;
  xpProgress: number;
  xpNeeded: number;
  xpToNextLevel: number;
  progressPercent: number;
  xpProgressPercent: number;
  isMaxLevel: boolean;
}

export interface NotificationResponse {
  id: number;
  type: string;
  message: string;
  threadId: number;
  read: boolean;
  createdAt: string;
}

export type XpSource =
  | 'THREAD_CREATED'
  | 'POST_CREATED'
  | 'POST_DETAILED'
  | 'COMMENT_CREATED'
  | 'UPVOTE_RECEIVED'
  | 'BEST_ANSWER';

export interface XpEventResponse {
  id: number;
  sourceType: XpSource;
  amount: number;
  newTotal: number;
  newLevel: number;
  description?: string;
  levelUp: boolean;
  createdAt: string;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface PostSummary {
  id: number;
  title: string;
  upvotes: number;
  totalReactions: number;
}

export interface XpDataPoint {
  date: string;
  xpGained: number;
  newTotal: number;
  source: string;
}

export interface DayActivity {
  weekLabel: string;
  postsCount: number;
  threadsCount: number;
  commentsCount: number;
}

export interface PerformanceInsight {
  type: 'POSITIVE' | 'NEGATIVE' | 'SUGGESTION';
  message: string;
  icon: string;
}

export type ReactionStats = Record<string, number>;

export interface LeaderboardEntry {
  userId: number;
  username: string;
  level: number;
  levelTitle: string;
  xp: number;
  threadCount: number;
  hqRate: number;
  mlTotalAnalyzed: number;
  rankPosition: number;
}

export interface UserDashboard {
  totalThreadsCreated: number;
  totalPostsCreated: number;
  totalCommentsCreated: number;
  totalUpvotesReceived: number;
  totalDownvotesReceived: number;
  totalPositiveReactions: number;
  totalNegativeReactions: number;
  positiveToNegativeRatio: number;
  totalSavesReceived: number;
  avgUpvotesPerPost: number;
  avgDownvotesPerPost: number;
  avgReactionsPerPost: number;
  bestPerformingPost?: PostSummary | null;
  bestThreadTitle?: string;
  bestThreadId?: number;
  bestThreadUpvotes?: number;
  engagementRate: number;
  reactionStats: ReactionStats;
  xpTimeline: XpDataPoint[];
  weeklyActivity: DayActivity[];
  communityAvgUpvotesPerPost: number;
  communityAvgReactionsPerPost: number;
  communityAvgEngagementRate: number;
  userPercentile: number;
  rankPosition: number;
  performanceInsights: PerformanceInsight[];
  mlHqRate?: number;
  mlHqCount?: number;
  mlLqEditCount?: number;
  mlLqCloseCount?: number;
  mlTotalAnalyzed?: number;
  mlAvailable?: boolean;
  predictedNextWeekPosts?: number;
  topLeaderboard?: LeaderboardEntry[];
}
