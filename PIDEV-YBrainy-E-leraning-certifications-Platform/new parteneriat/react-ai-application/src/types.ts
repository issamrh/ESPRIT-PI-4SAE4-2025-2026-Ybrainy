export interface GenerateApplicationRequest {
  cv: string;
  jobDescription: string;
  cvSkeleton?: string | null;
  profileImageBase64?: string | null;
  profileImageMimeType?: string | null;
}

export interface GenerateApplicationResponse {
  optimizedCV: string;
  coverLetter: string;
  professionalProfilePhotoDataUrl?: string | null;
  profilePhotoAssistantMessage?: string | null;
}

export type ResultTab = 'optimizedCV' | 'coverLetter';

export interface AtsResult {
  score: number;
  matchedKeywords: string[];
  missingKeywords: string[];
}
