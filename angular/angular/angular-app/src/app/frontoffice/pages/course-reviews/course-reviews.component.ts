import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseApiService, SpringPage } from '../../services/course-api.service';
import { ApiReview, RatingDistribution } from '../../models/course.models';
import { AuthMockService } from '../../services/auth-mock.service';

@Component({
  selector: 'app-course-reviews',
  standalone: false,
  templateUrl: './course-reviews.component.html',
  styleUrls: ['./course-reviews.component.css'],
})
export class CourseReviewsComponent implements OnInit {
  courseId = 0;

  get studentId(): number { return this.authMock.getUserId(); }
  get isStudent(): boolean { return this.authMock.getRole() === 'STUDENT'; }
  get alreadyReviewed(): boolean {
    return this.page.content.some(r => r.studentId === this.studentId);
  }

  isEnrolled = false;
  enrollmentChecked = false;

  page: SpringPage<ApiReview> = { content: [] };
  ratingStats: RatingDistribution | null = null;
  newRating = 0;
  newComment = '';
  error = '';
  success = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: CourseApiService,
    private authMock: AuthMockService
  ) {}

  ngOnInit(): void {
    this.courseId = +(this.route.snapshot.paramMap.get('courseId') ?? '0');
    if (!this.isStudent) {
      this.isEnrolled = true;
      this.enrollmentChecked = true;
      this.loadReviews();
      return;
    }
    this.api.getStudentEnrollments(this.studentId).subscribe({
      next: (enrollments) => {
        this.isEnrolled = enrollments.some(e => e.courseId === this.courseId);
        this.enrollmentChecked = true;
        if (this.isEnrolled) this.loadReviews();
      },
      error: () => { this.isEnrolled = false; this.enrollmentChecked = true; },
    });
  }

  loadReviews(): void {
    this.api.getReviews(this.courseId).subscribe({
      next: (p) => { this.page = p; },
      error: () => {},
    });
    this.api.getRatingStats(this.courseId).subscribe({
      next: (s) => { this.ratingStats = s; },
      error: () => {},
    });
  }

  getBarWidth(count: number): string {
    if (!this.ratingStats || this.ratingStats.totalReviews === 0) return '0%';
    return Math.round((count / this.ratingStats.totalReviews) * 100) + '%';
  }

  setRating(n: number): void {
    this.newRating = n;
  }

  onCommentChange(event: Event): void {
    this.newComment = (event.target as HTMLTextAreaElement).value;
  }

  submitReview(): void {
    if (this.newRating < 1) { this.error = 'Please select a rating.'; return; }
    this.error = '';
    this.success = '';
    this.api.addReview(this.courseId, this.studentId, this.newRating, this.newComment).subscribe({
      next: () => {
        this.success = 'Review submitted!';
        this.newRating = 0;
        this.newComment = '';
        this.loadReviews();
      },
      error: (err: any) => {
        this.error = err.status === 409
          ? 'You have already reviewed this course.'
          : 'Could not submit review.';
      },
    });
  }

  stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  back(): void {
    this.router.navigate(['/courses', this.courseId]);
  }
}
