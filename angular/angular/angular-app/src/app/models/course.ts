/**
 * Course models aligned with Spring Boot Gateway (CourseResponseDTO).
 * - id: number (Java Long)
 * - thumbnailUrl (use this; do not use imageUrl)
 * - CourseDetail: extends Course and adds lessons: Lesson[]
 */
export type {
  Lesson,
  LessonResponseDTO,
  Course,
  CourseDetail,
  CourseListItem,
  CourseRequestDTO,
} from '../frontoffice/models/course.models';
