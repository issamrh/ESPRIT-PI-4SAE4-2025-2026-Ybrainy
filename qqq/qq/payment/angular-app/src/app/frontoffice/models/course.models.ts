export interface Lesson {
  id: string;
  title: string;
  description: string;
  videoUrl: string;
  durationMinutes?: number;
}

export interface Course {
  id: string;
  title: string;
  /** Aligns with backend: /api/courses/category/{category} */
  category?: string;
  description: string;
  /** Image shown in list cards (frontoffice visual). */
  imageUrl: string;
  /** Static demo fields (to match admin-template style cards). */
  rating?: number; // 0..5
  studentsCount?: number;
  /**
   * Aligns with backend: Course.thumbnailVideoPath (a video used as a thumbnail preview).
   * Examples:
   * - "/uploads/thumbnails/xxxx.mp4" (backend)
   * - "assets/.../file.mp4" (static demo)
   */
  thumbnailVideoPath?: string;
  /** Backwards-compat with earlier UI field name. Prefer thumbnailVideoPath. */
  promoVideoUrl?: string;
  lessons: Lesson[];
  createdAt: string; // ISO string for easy localStorage persistence
}


