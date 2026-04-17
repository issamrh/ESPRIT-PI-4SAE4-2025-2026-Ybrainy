export type JobContractType = 'CDI' | 'CDD' | 'Stage' | 'Alternance' | 'Freelance' | 'Autre';

export interface JobOffer {
  id: string;
  partnershipId: string; // company / partner
  title: string;
  location?: string;
  contractType: JobContractType;
  description?: string;
  skills?: string[];
  salaryRange?: string;
  imageDataUrl?: string; // base64 data URL (demo / localStorage)
  deadline?: string; // ISO date string
  createdAt: string; // ISO string
  isActive: boolean;
}


