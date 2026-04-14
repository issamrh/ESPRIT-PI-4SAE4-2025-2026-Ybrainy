export interface GenerateApplicationRequest {
  cv: string;
  jobDescription: string;
  /** Squelette / structure imposee au CV optimise (optionnel cote API) */
  cvSkeleton?: string | null;
  /** Base64 sans prefixe data: */
  profileImageBase64?: string | null;
  profileImageMimeType?: string | null;
}

export interface GenerateApplicationResponse {
  optimizedCV: string;
  coverLetter: string;
  /** data:image/...;base64,... si le modele image a repondu */
  professionalProfilePhotoDataUrl?: string | null;
  /** Conseils ou note si pas d image generee */
  profilePhotoAssistantMessage?: string | null;
}

export interface AtsScoreResult {
  score: number;
  matchedKeywords: string[];
  missingKeywords: string[];
}
