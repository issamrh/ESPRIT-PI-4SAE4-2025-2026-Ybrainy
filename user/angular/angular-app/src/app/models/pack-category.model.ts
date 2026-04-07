export interface PackCategory {
  id: number;
  name: string;
  description: string;
  icon: string;
  status: 'ACTIVE' | 'INACTIVE';
  packCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePackCategory {
  name: string;
  description?: string;
  icon?: string;
}

export interface UpdatePackCategory {
  name: string;
  description?: string;
  icon?: string;
}

