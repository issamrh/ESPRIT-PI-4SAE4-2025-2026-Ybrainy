import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CertificateVerification } from '../../models/course.models';
import { CourseApiService } from '../../services/course-api.service';

@Component({
  selector: 'app-certificate-verify',
  standalone: false,
  templateUrl: './certificate-verify.component.html',
  styleUrls: ['./certificate-verify.component.css']
})
export class CertificateVerifyComponent implements OnInit {
  certificateId: string = '';
  result: CertificateVerification | null = null;
  loading: boolean = false;
  error: string = '';

  constructor(
    private route: ActivatedRoute,
    private api: CourseApiService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.certificateId = id;
        this.verify(id);
      }
    });
  }

  verify(id: string): void {
    this.loading = true;
    this.error = '';
    this.api.verifyCertificate(id).subscribe({
      next: (data) => {
        this.result = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Certificate not found or invalid.';
        this.loading = false;
      }
    });
  }
}
