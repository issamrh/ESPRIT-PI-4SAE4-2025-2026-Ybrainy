import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseApiService, ApiCourse } from '../../services/course-api.service';
import { ApiCourseProgress, ApiLessonProgress } from '../../models/course.models';
import { AuthMockService } from '../../services/auth-mock.service';

@Component({
  selector: 'app-course-progress',
  standalone: false,
  templateUrl: './course-progress.component.html',
  styleUrls: ['./course-progress.component.css'],
})
export class CourseProgressComponent implements OnInit {
  courseId = 0;

  get studentId(): number { return this.authMock.getUserId(); }

  progress: ApiCourseProgress | null = null;
  course: ApiCourse | null = null;
  loading = true;
  error = '';
  certDownloading = false;
  certError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: CourseApiService,
    private authMock: AuthMockService
  ) {}

  ngOnInit(): void {
    this.courseId = +(this.route.snapshot.paramMap.get('courseId') ?? '0');
    this.fetchCourse();
    this.load();
  }

  fetchCourse(): void {
    this.api.getById(this.courseId).subscribe({
      next: (c) => { this.course = c; },
      error: () => {},
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.getCourseProgress(this.courseId, this.studentId).subscribe({
      next: (data) => {
        this.progress = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Not enrolled or unable to load progress.';
        this.loading = false;
      },
    });
  }

  get completedCount(): number {
    return (this.progress?.lessonProgresses ?? []).filter(l => l.status === 'COMPLETED').length;
  }

  get totalCount(): number {
    return this.progress?.lessonProgresses?.length ?? 0;
  }

  getLessonTitle(lessonId: number): string {
    const lesson = this.course?.lessons?.find(l => l.id === lessonId);
    return lesson?.title ?? `Lesson ${lessonId}`;
  }

  markComplete(lessonId: number): void {
    this.api.markLessonComplete(this.courseId, lessonId, this.studentId).subscribe({
      next: () => this.load(),
      error: () => { this.error = 'Could not mark lesson complete.'; },
    });
  }

  downloadCertificate(): void {
    this.certDownloading = true;
    this.certError = '';
    this.api.downloadCertificate(this.courseId, this.studentId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `certificate-course-${this.courseId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.certDownloading = false;
      },
      error: () => {
        this.certError = 'Certificate download failed. Please try again.';
        this.certDownloading = false;
      },
    });
  }

  goToLesson(lessonId: number): void {
    this.router.navigate(['/courses', this.courseId, 'lessons'],
      { queryParams: { lesson: lessonId } });
  }

  formatTime(seconds: number): string {
    if (seconds < 60) return seconds + 's';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return s > 0 ? m + 'm ' + s + 's' : m + 'm';
  }

  back(): void {
    this.router.navigate(['/courses', this.courseId]);
  }
}
