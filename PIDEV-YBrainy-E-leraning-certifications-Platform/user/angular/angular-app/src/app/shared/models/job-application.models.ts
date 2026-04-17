export type JobApplicationStatus = 'PENDING' | 'REVIEWED' | 'SHORTLISTED' | 'ACCEPTED' | 'REJECTED';

export interface JobApplication {
  id: string;
  offerId: string;
  applicantName: string;
  applicantEmail: string;
  message?: string;
  cvDataUrl?: string; // base64 data URL (demo / localStorage)
  status: JobApplicationStatus;
  reviewerNotes?: string;
  createdAt: string; // ISO string
}


