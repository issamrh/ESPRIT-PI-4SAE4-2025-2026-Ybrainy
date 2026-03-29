import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EnrolledCourse, StudentDashboard } from '../../models/course.models';
import { CourseApiService } from '../../services/course-api.service';
import { AuthMockService } from '../../services/auth-mock.service';

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

  constructor(
    private api: CourseApiService,
    private authMock: AuthMockService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.api.getStudentDashboard(this.authMock.getUserId()).subscribe({
      next: (d) => { this.dashboard = d; this.loading = false; },
      error: () => { this.loading = false; }
    });
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
    this.api.downloadCertificate(course.courseId, this.authMock.getUserId()).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'YBrainy-Certificate.pdf';
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  getThumbnailUrl(course: EnrolledCourse): string {
    if (!course.thumbnailUrl) return '';
    if (course.thumbnailUrl.startsWith('http')) return course.thumbnailUrl;
    return `/api/courses/files/${course.thumbnailUrl}`;
  }
}
