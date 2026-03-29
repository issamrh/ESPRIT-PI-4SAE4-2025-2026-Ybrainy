import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CourseDetail, CheckoutSessionRequest } from '../../models/course.models';
import { CourseStoreService } from '../../services/course-store.service';
import { CourseApiService } from '../../services/course-api.service';
import { AuthMockService } from '../../services/auth-mock.service';

@Component({
  selector: 'app-course-detail',
  standalone: false,
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.css'],
  host: { style: 'display:block' },
})
export class CourseDetailComponent implements OnInit, OnDestroy {
  course: CourseDetail | null = null;
  courseId = '';
  loading = true;

  enrolled = false;
  enrolling = false;
  enrollError = '';

  private readonly sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private store: CourseStoreService,
    private api: CourseApiService,
    private authMock: AuthMockService
  ) {}

  get studentId(): number { return this.authMock.getUserId(); }
  private get isStudent(): boolean { return this.authMock.getRole() === 'STUDENT'; }

  ngOnInit(): void {
    this.sub.add(
      this.route.paramMap.subscribe((pm) => {
        this.courseId = pm.get('courseId') ?? '';
        const id = this.courseId ? +this.courseId : NaN;
        this.enrolled = false;
        this.enrollError = '';
        if (!isNaN(id)) {
          this.checkEnrollment(id);
          this.sub.add(
            this.store.getCourseById(id).subscribe({
              next: (detail) => { this.course = detail; this.loading = false; },
              error: () => { this.course = null; this.loading = false; },
            })
          );
        } else {
          this.course = null;
        }
      })
    );

    this.sub.add(
      this.store.selectedCourse$.subscribe((selected) => {
        if (selected && this.courseId && String(selected.id) === this.courseId) {
          this.course = selected;
        }
      })
    );

    // Handle redirect back from Stripe checkout
    this.sub.add(
      this.route.queryParams.subscribe((params) => {
        if (params['payment'] === 'success' && this.courseId && !this.enrolled) {
          const id = +this.courseId;
          if (!isNaN(id)) {
            // Enroll via API in case webhook was slow
            this.api.enroll(this.studentId, id).subscribe({
              next: () => { this.enrolled = true; },
              error: () => { this.checkEnrollment(id); },
            });
          }
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.store.clearSelectedCourse();
    this.sub.unsubscribe();
  }

  checkEnrollment(courseId: number): void {
    this.api.getStudentEnrollments(this.studentId).subscribe({
      next: (enrollments) => {
        this.enrolled = enrollments.some(e => e.courseId === courseId);
      },
      error: () => { this.enrolled = false; },
    });
  }

  enroll(): void {
    if (this.enrolling || !this.courseId) return;
    this.enrolling = true;
    this.enrollError = '';
    this.api.enroll(this.studentId, +this.courseId).subscribe({
      next: () => { this.enrolled = true; this.enrolling = false; },
      error: () => {
        this.enrollError = 'Enrollment failed. Please try again.';
        this.enrolling = false;
      },
    });
  }

  enrollOrPay(): void {
    if (this.enrolling || !this.courseId || !this.course) return;
    if (!this.course.price || this.course.price === 0) {
      this.enroll();
    } else {
      this.enrolling = true;
      this.enrollError = '';
      const request: CheckoutSessionRequest = {
        courseId: +this.courseId,
        studentId: this.studentId,
        courseTitle: this.course.title,
        price: this.course.price,
      };
      this.api.createCheckoutSession(request).subscribe({
        next: (response) => { window.location.href = response.checkoutUrl; },
        error: () => {
          this.enrollError = 'Payment setup failed. Please try again.';
          this.enrolling = false;
        },
      });
    }
  }

  backToCourses(): void {
    this.router.navigateByUrl('/courses');
  }

  viewLessons(): void {
    if (!this.courseId) return;
    this.router.navigate(['/courses', this.courseId, 'lessons']);
  }

  goToProgress(): void {
    if (!this.courseId) return;
    this.router.navigate(['/courses', this.courseId, 'progress']);
  }

  goToQuiz(): void {
    if (!this.courseId) return;
    this.router.navigate(['/courses', this.courseId, 'quiz']);
  }

  goToReviews(): void {
    if (!this.courseId) return;
    this.router.navigate(['/courses', this.courseId, 'reviews']);
  }

  formatDate(value: string | undefined): string {
    if (!value) return '--';
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  formatPrice(value: number | undefined | null): string {
    const n = Number(value);
    if (!isFinite(n)) return '--';
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(n);
    } catch {
      return `${n.toFixed(2)} $`;
    }
  }
}
