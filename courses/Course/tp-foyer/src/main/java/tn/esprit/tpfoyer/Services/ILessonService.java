package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.LessonMetaRequestDTO;
import tn.esprit.tpfoyer.Dto.LessonResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonSequenceItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ILessonService {

    LessonResponseDTO createLesson(
            Long courseId,
            LessonMetaRequestDTO meta,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls,
            List<LessonSequenceItemDTO> sequence);

    List<LessonResponseDTO> getLessonsByCourse(Long courseId);

    Page<LessonResponseDTO> getAllLessons(Long courseId, Pageable pageable);

    LessonResponseDTO getLessonById(Long courseId, Long lessonId);

    LessonResponseDTO updateLesson(
            Long courseId,
            Long lessonId,
            LessonMetaRequestDTO meta,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls,
            List<LessonSequenceItemDTO> sequence);

    void deleteLesson(Long courseId, Long lessonId);
}