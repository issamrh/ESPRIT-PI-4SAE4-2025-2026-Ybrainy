import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseApiService } from '../../services/course-api.service';
import { ApiLeaderboardEntry, ApiQuiz, ApiQuestion, ApiQuizResult, QuestionReview } from '../../models/course.models';
import { UserSessionService } from '../../../tracking/user-session.service';

@Component({
  selector: 'app-quiz-page',
  standalone: false,
  templateUrl: './quiz-page.component.html',
  styleUrls: ['./quiz-page.component.css'],
})
export class QuizPageComponent implements OnInit, OnDestroy {
  courseId = 0;

  get studentId(): number { return this.userSession.get()?.userId ?? 0; }
  get isStudent(): boolean { return (this.userSession.get()?.role ?? 'STUDENT') === 'STUDENT'; }

  isEnrolled = false;
  enrollmentChecked = false;

  quizzes: ApiQuiz[] = [];
  selectedQuiz: ApiQuiz | null = null;
  questions: ApiQuestion[] = [];
  responses = new Map<number, number | null>();
  quizResult: ApiQuizResult | null = null;
  loading = false;
  error = '';
  currentQuestionIndex = 0;

  timeLeft: number = 0;
  timerInterval: any = null;
  timerExpired: boolean = false;

  showReview: boolean = false;
  showAnswerReview: boolean = false;
  submitting: boolean = false;

  leaderboard: ApiLeaderboardEntry[] = [];
  showLeaderboard: boolean = false;
  leaderboardLoading: boolean = false;

  arenaMode: boolean = false;
  championHovered: boolean = false;
  validationError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: CourseApiService,
    private userSession: UserSessionService
  ) {}

  ngOnInit(): void {
    this.courseId = +(this.route.snapshot.paramMap.get('courseId') ?? '0');
    if (!this.isStudent) {
      this.isEnrolled = true;
      this.enrollmentChecked = true;
      this.loadQuizzes();
      return;
    }
    try {
      const studentId = this.requireAuth();
      this.api.getStudentEnrollments(studentId).subscribe({
        next: (enrollments) => {
          this.isEnrolled = enrollments.some(e => e.courseId === this.courseId);
          this.enrollmentChecked = true;
          if (this.isEnrolled) this.loadQuizzes();
        },
        error: () => { this.isEnrolled = false; this.enrollmentChecked = true; },
      });
    } catch {
      this.isEnrolled = false;
      this.enrollmentChecked = true;
    }
  }

  loadQuizzes(): void {
    this.loading = true;
    this.error = '';
    this.api.getQuizzesByCourse(this.courseId).subscribe({
      next: (data) => { this.quizzes = data; this.loading = false; },
      error: (err) => {
        console.error('Could not load quizzes', err);
        this.error = 'Could not load quizzes.';
        this.loading = false;
      },
    });
  }

  selectQuiz(quiz: ApiQuiz): void {
    this.selectedQuiz = quiz;
    this.quizResult = null;
    this.responses.clear();
    this.questions = [];
    this.currentQuestionIndex = 0;
    this.showReview = false;
    this.error = '';
    this.api.getQuizQuestions(this.courseId, quiz.id).subscribe({
      next: (qs) => {
        this.questions = qs;
        this.stopTimer();
        if (quiz.timeLimitMinutes && quiz.timeLimitMinutes > 0) {
          this.startTimer(quiz.timeLimitMinutes);
        }
      },
      error: () => { this.error = 'Questions unavailable.'; },
    });
  }

  selectAnswer(questionId: number, answerId: number): void {
    this.responses.set(questionId, answerId);
  }

  isSelected(questionId: number, answerId: number): boolean {
    return this.responses.get(questionId) === answerId;
  }

  get currentQuestion(): ApiQuestion | null {
    return this.questions[this.currentQuestionIndex] ?? null;
  }

  isLastQuestion(): boolean {
    return this.questions.length > 0 && this.currentQuestionIndex === this.questions.length - 1;
  }

  canProceed(): boolean {
    if (!this.currentQuestion) return false;
    return this.responses.has(this.currentQuestion.id);
  }

  nextQuestion(): void {
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
    }
  }

  prevQuestion(): void {
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
    }
  }

  toggleAnswerReview(): void {
    this.showAnswerReview = !this.showAnswerReview;
  }

  tryAgain(): void {
    if (!this.selectedQuiz) return;
    this.arenaMode = false;
    this.quizResult = null;
    this.responses.clear();
    this.currentQuestionIndex = 0;
    this.showReview = false;
    this.showAnswerReview = false;
    this.showLeaderboard = false;
    this.api.getQuizQuestions(this.courseId, this.selectedQuiz.id).subscribe({
      next: (qs) => { this.questions = qs; },
      error: () => { this.error = 'Questions unavailable.'; },
    });
  }

  submitQuiz(): void {
    this.stopTimer();
    if (!this.selectedQuiz) return;
    const unanswered = this.questions.filter(q => !this.responses.has(q.id));
    if (unanswered.length > 0) {
      this.validationError = `Please answer all questions. ${unanswered.length} question(s) remaining.`;
      const firstUnanswered = document.getElementById(`question-${unanswered[0].id}`);
      if (firstUnanswered) firstUnanswered.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    this.validationError = null;
    try {
      const studentId = this.requireAuth();
      this.submitting = true;
      const responsesArray = this.questions.map((q) => ({
        questionId: q.id,
        selectedOptionId: this.responses.get(q.id) ?? null,
      }));
      this.api.submitQuiz(this.courseId, this.selectedQuiz.id, studentId, responsesArray).subscribe({
      next: (r) => {
        this.quizResult = r;
        this.arenaMode = true;
        this.showReview = false;
        this.submitting = false;
        this.leaderboardLoading = true;
        this.api.getLeaderboard(this.courseId, this.selectedQuiz!.id).subscribe({
          next: (data) => {
            this.leaderboard = data;
            this.leaderboardLoading = false;
            setTimeout(() => this.showLeaderboard = true, 800);
          },
          error: () => {
            this.leaderboard = [];
            this.leaderboardLoading = false;
          },
        });
      },
      error: (err) => {
        this.error = err instanceof Error && err.message
          ? `Submission failed: ${err.message}`
          : 'Submission failed.';
        this.submitting = false;
      },
      });
    } catch {
      return; // redirected to login
    }
  }

  startTimer(minutes: number): void {
    this.timeLeft = minutes * 60;
    this.timerExpired = false;
    this.timerInterval = setInterval(() => {
      this.timeLeft--;
      if (this.timeLeft <= 0) {
        clearInterval(this.timerInterval);
        this.timerExpired = true;
        this.submitQuiz();
      }
    }, 1000);
  }

  get formattedTime(): string {
    const m = Math.floor(this.timeLeft / 60);
    const s = this.timeLeft % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  reviewAnswers(): void {
    this.showReview = true;
  }

  backToExam(): void {
    this.showReview = false;
  }

  goToQuestion(index: number): void {
    this.currentQuestionIndex = index;
    this.showReview = false;
  }

  getSelectedOptionText(questionId: number): string {
    const selectedId = this.responses.get(questionId);
    if (selectedId === null || selectedId === undefined) return 'Not answered';
    const question = this.questions.find(q => q.id === questionId);
    if (!question) return 'Not answered';
    const option = question.options.find(o => o.id === selectedId);
    return option ? option.optionText : 'Not answered';
  }

  getPlayerRankClass(rankTitle: string): string {
    const map: Record<string, string> = {
      'LEGEND': 'tk-legend',
      'CHAMPION': 'tk-champion',
      'FIGHTER': 'tk-fighter',
      'CHALLENGER': 'tk-challenger',
    };
    return map[rankTitle] || 'tk-challenger';
  }

  getRankIcon(rankTitle: string): string {
    const map: Record<string, string> = {
      'LEGEND': '🔥',
      'CHAMPION': '⚔️',
      'FIGHTER': '🥊',
      'CHALLENGER': '🎮',
    };
    return map[rankTitle] || '🎮';
  }

  onChampionHover(state: boolean): void {
    this.championHovered = state;
  }

  onCardHover(event: MouseEvent, entering: boolean): void {
    const card = event.currentTarget as HTMLElement;
    if (entering) {
      card.style.transform = 'translateY(-8px) scale(1.03)';
      card.style.zIndex = '10';
    } else {
      card.style.transform = '';
      card.style.zIndex = '';
    }
  }

  back(): void {
    this.router.navigate(['/courses', this.courseId]);
  }

  private requireAuth(): number {
    const userId = this.userSession.get()?.userId;
    if (!userId || userId <= 0) {
      this.router.navigate(['/login']);
      throw new Error('Authentication required');
    }
    return userId;
  }
}
