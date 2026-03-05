export type PackLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type PackStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export interface Pack {
  id: number;
  title: string;
  description: string;
  originalPrice: number;
  salePrice: number;
  level: PackLevel;
  durationHours: number;
  certificateName: string | null;
  status: PackStatus;
  categoryId: number;
  categoryName: string;
  image?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePack {
  title: string;
  description: string;
  originalPrice: number;
  salePrice: number;
  level: PackLevel;
  durationHours: number;
  certificateName?: string;
  categoryId: number;
}

export interface UpdatePack {
  title: string;
  description: string;
  originalPrice: number;
  salePrice: number;
  level: PackLevel;
  durationHours: number;
  certificateName?: string;
  categoryId: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface GeneratePackContentRequest {
  title?: string;
  description?: string;
  categoryName?: string;
  level?: PackLevel;
  durationHours?: number;
  certificateName?: string;
}

export interface GeneratePackContentResponse {
  generatedTitle: string | null;
  generatedDescription: string;
  providerMessage: string;
}

