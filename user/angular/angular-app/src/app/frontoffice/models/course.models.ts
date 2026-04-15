/**
 * Frontend models aligned with Spring Boot DTOs.
 * Course = CourseResponseDTO (list); CourseDetail = Course + lessons (detail).
 * IDs are number to match Java Long.
 */

/** Matches backend LessonResponseDTO. */
export interface Lesson {
  id: number;
  title: string;
  description: string;
  type?: string;
  contentUrl?: string;
  videoUrl?: string;
  orderIndex?: number;
  durationMinutes?: number;
  contents?: Array<{ id: number; type: string; contentUrl: string; orderIndex?: number; createdAt?: string }>;
}

/** Alias for Lesson when referring to backend DTO. */
export type LessonResponseDTO = Lesson;

/** Matches backend CourseResponseDTO (list endpoint; no lessons). */
export interface Course {
  id: number;
  title: string;
  description: string;
  thumbnailUrl: string;
  price: number;
  rating: number;
  ratingCount: number;
  category: string;
  level: string;
  approximateDurationMinutes: number;
  isPublished: boolean;
  offersCertificate?: boolean;
  lessonCount: number;
  createdAt?: string;
  updatedAt?: string;
}

/** Full course for detail view: extends Course and adds lessons: Lesson[]. */
export interface CourseDetail extends Course {
  lessons: Lesson[];
}

/** List item type (same as Course from list endpoint). */
export type CourseListItem = Course;

/** Payload for POST/PUT: CourseRequestDTO (no id, no createdAt/updatedAt). */
export interface CourseRequestDTO {
  title: string;
  description: string;
  category: string;
  level: string;
  price: number;
  approximateDurationMinutes: number;
  isPublished: boolean;
  offersCertificate?: boolean;
}

// ── Progress models ────────────────────────────────────────────────────
export interface ApiEnrollment {
  id: number;
  studentId: number;
  courseId: number;
  enrollmentDate: string;
  status: 'ACTIVE' | 'COMPLETED' | 'DROPPED';
  currentLessonId?: number;
  completionPercentage: number;
  paymentIntentId?: string;
}

export interface ApiLessonProgress {
  id: number;
  enrollmentId: number;
  lessonId: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  startedAt?: string;
  completedAt?: string;
  timeSpentSeconds: number;
}

export interface ApiCourseProgress {
  courseId: number;
  enrollmentId: number;
  completionPercentage: number;
  enrollmentStatus: 'ACTIVE' | 'COMPLETED' | 'DROPPED';
  currentLessonId?: number;
  lessonProgresses: ApiLessonProgress[];
}

// ── Payment models ──────────────────────────────────────────────────────
export interface CheckoutSessionRequest {
  courseId: number;
  studentId: number;
  courseTitle: string;
  price: number;
}

export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

export interface CheckoutConfirmRequest {
  sessionId: string;
}

export interface CheckoutConfirmResponse {
  sessionId: string;
  paymentIntentId: string;
  enrollments: ApiEnrollment[];
}

export interface EnrollmentCheckResponse {
  enrolled: boolean;
}

// ── Quiz models ────────────────────────────────────────────────────────
export interface ApiAnswer {
  id: number;
  optionText: string;
  isCorrect?: boolean;
  orderIndex?: number;
}

export interface ApiQuestion {
  id: number;
  questionText: string;
  orderIndex?: number;
  options: ApiAnswer[];
}

export interface ApiQuiz {
  id: number;
  courseId?: number;
  title: string;
  description?: string;
  questionCount: number;
  passingScore: number;
  timeLimitMinutes?: number;
  maxAttempts?: number;
  orderIndex?: number;
}

export interface ApiQuizResponse {
  questionId: number;
  selectedOptionId: number | null;
}

export interface QuestionReview {
  questionId: number;
  questionText: string;
  selectedOptionId: number | null;
  selectedOptionText: string;
  selectedCorrect: boolean;
  correctOptionId: number | null;
  correctOptionText: string;
}

export interface ApiQuizResult {
  score: number;
  passed: boolean;
  correctAnswers: number;
  totalQuestions: number;
  passingScore: number;
  attemptsUsed: number;
  attemptsRemaining: number;
  questionReviews?: QuestionReview[];
}

// ── Leaderboard models ─────────────────────────────────────────────────
export interface ApiLeaderboardEntry {
  rank: number;
  studentId: number;
  studentName: string;
  score: number;
  rankTitle: string;
  attemptedAt: string;
}

// ── Learning Path models ───────────────────────────────────────────────
export interface LearningPathCourse {
  id: number;
  title: string;
  description: string;
  thumbnailUrl?: string;
  price: number;
  level: string;
  category: string;
  approximateDurationMinutes?: number;
  orderIndex: number;
  whyIncluded: string;
}

export interface LearningPath {
  id?: number;
  studentId: number;
  goal: string;
  generatedTitle: string;
  generatedDescription: string;
  courses: LearningPathCourse[];
  totalPrice: number;
  totalDurationMinutes: number;
  saved: boolean;
  createdAt?: string;
}

// ── ML Quality models ───────────────────────────────────────────────────
export interface MlQualityResult {
  qualityLabel: string;
  confidence: number;
  confidencePercentage: number;
  isHighQuality: boolean;
}

// ── ML Recommendation models ────────────────────────────────────────────
export interface MlRecommendation {
  courseId: string;
  title: string;
  category: string;
  level: string;
  price: number;
  rating: number;
  numLectures: number;
  isPaid: boolean;
}

export interface MlRecommendationsResponse {
  recommendations: MlRecommendation[];
  basedOn: { category: string; level: string; };
}

// ── AI Search models ───────────────────────────────────────────────────
export interface AiSearchResult {
  explanation: string;
  extractedKeywords: string;
  detectedCategory: string | null;
  detectedLevel: string | null;
  results: {
    content: AiCourseItem[];
    totalElements: number;
    totalPages: number;
    number: number;
  };
}

export interface AiCourseItem {
  id: number;
  title: string;
  description: string;
  thumbnailUrl?: string;
  price: number;
  rating: number;
  ratingCount: number;
  category: string;
  level: string;
  approximateDurationMinutes: number;
  isPublished: boolean;
  offersCertificate?: boolean;
  lessonCount: number;
  createdAt?: string;
}

// ── Review models ──────────────────────────────────────────────────────
export interface RatingDistribution {
  averageRating: number;
  totalReviews: number;
  count5: number;
  count4: number;
  count3: number;
  count2: number;
  count1: number;
}

export interface ApiReview {
  id: number;
  courseId: number;
  studentId: number;
  rating: number;
  comment?: string;
  createdAt?: string;
}

// ── Student Dashboard models ────────────────────────────────────────────
export interface EnrolledCourse {
  courseId: number;
  courseTitle: string;
  thumbnailUrl?: string;
  category?: string;
  level?: string;
  completionPercentage: number;
  enrollmentStatus: 'ACTIVE' | 'COMPLETED' | 'DROPPED';
  enrollmentDate?: string;
  completedAt?: string;
  certificateId?: string;
  bestQuizScore?: number;
  totalLessons: number;
  completedLessons: number;
}

export interface StudentDashboard {
  studentId: number;
  totalEnrolled: number;
  totalCompleted: number;
  totalInProgress: number;
  totalCertificates: number;
  averageProgress: number;
  enrolledCourses: EnrolledCourse[];
}

// ── ML Conversion Insight model ─────────────────────────────────────────
export interface ConversionInsight {
  avgCompletionRate: number;
  conversionProbability: number;
  percentage: number;
  totalEnrollments: number;
  paidEnrollments: number;
  conversionLabel: 'LOW' | 'MEDIUM' | 'HIGH';
}

// ── Certificate verification ────────────────────────────────────────────
export interface CertificateVerification {
  valid: boolean;
  certificateId: string;
  studentName: string;
  studentId: number;
  courseTitle: string;
  completionDate: string;
  quizScore: number | null;
  hoursSpent: number;
  issuedBy: string;
}
