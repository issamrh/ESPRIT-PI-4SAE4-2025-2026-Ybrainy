import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ConversionInsight, EnrolledCourse, StudentDashboard } from '../../models/course.models';
import { CourseApiService } from '../../services/course-api.service';
import { UserSessionService } from '../../../tracking/user-session.service';

@Component({
  selector: 'app-student-dashboard',
  standalone: false,
  templateUrl: './student-dashboard.component.html',
  styleUrls: ['./student-dashboard.component.css']
})
export class StudentDashboardComponent implements OnInit {
  dashboard: StudentDashboard | null = null;
  loading = true;
  activeTab: 'all' | 'active' | 'completed' = 'all';
  conversionInsight: ConversionInsight | null = null;
  conversionLoading = false;

  constructor(
    private api: CourseApiService,
    private userSession: UserSessionService,
    private router: Router
  ) {}

  get studentId(): number { return this.userSession.get()?.userId ?? 0; }

  ngOnInit(): void {
    try {
      const studentId = this.requireAuth();
      this.api.getStudentDashboard(studentId).subscribe({
        next: (d) => {
          this.dashboard = d;
          this.loading = false;
          this.conversionLoading = true;
          this.api.getConversionInsight(studentId).subscribe({
            next: (data) => {
              this.conversionInsight = data;
              this.conversionLoading = false;
            },
            error: () => {
              this.conversionLoading = false;
            }
          });
        },
        error: () => { this.loading = false; }
      });
    } catch {
      this.loading = false;
      return; // redirected to login
    }
  }

  get filteredCourses(): EnrolledCourse[] {
    if (!this.dashboard) return [];
    if (this.activeTab === 'active')
      return this.dashboard.enrolledCourses.filter(c => c.enrollmentStatus === 'ACTIVE');
    if (this.activeTab === 'completed')
      return this.dashboard.enrolledCourses.filter(c => c.enrollmentStatus === 'COMPLETED');
    return this.dashboard.enrolledCourses;
  }

  goToCourse(courseId: number): void {
    this.router.navigate(['/courses', courseId]);
  }

  goToProgress(courseId: number): void {
    this.router.navigate(['/courses', courseId, 'progress']);
  }

  downloadCertificate(course: EnrolledCourse): void {
    if (!course.certificateId) return;
    try {
      const studentId = this.requireAuth();
      this.api.downloadCertificate(course.courseId, studentId).subscribe(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'YBrainy-Certificate.pdf';
        a.click();
        URL.revokeObjectURL(url);
      });
    } catch {
      return; // redirected to login
    }
  }

  getThumbnailUrl(course: EnrolledCourse): string {
    if (!course.thumbnailUrl) return '';
    if (course.thumbnailUrl.startsWith('http')) return course.thumbnailUrl;
    return `/api/courses/files/${course.thumbnailUrl}`;
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
