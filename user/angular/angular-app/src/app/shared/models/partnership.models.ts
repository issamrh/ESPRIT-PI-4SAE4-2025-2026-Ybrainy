export interface Partnership {
  id: string;
  name: string;
  email?: string;
  phone?: string;
  website?: string;
  industry?: string;
  description?: string;
  logoDataUrl?: string; // base64 data URL (demo / localStorage)
  createdAt: string; // ISO string
  isActive: boolean;
}


