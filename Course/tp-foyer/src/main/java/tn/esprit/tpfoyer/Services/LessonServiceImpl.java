package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.LessonContentResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonMetaRequestDTO;
import tn.esprit.tpfoyer.Dto.LessonResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonSequenceItemDTO;
import tn.esprit.tpfoyer.Entities.Course;
import tn.esprit.tpfoyer.Entities.LessonContent;
import tn.esprit.tpfoyer.Entities.Lesson;
import tn.esprit.tpfoyer.Entities.enums.LessonType;
import tn.esprit.tpfoyer.Exception.ResourceNotFoundException;

import tn.esprit.tpfoyer.Repositories.CourseRepository;
import tn.esprit.tpfoyer.Repositories.LessonRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonServiceImpl implements ILessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final IFileStorageService fileStorageService;

    @Override
    public LessonResponseDTO createLesson(
            Long courseId,
            LessonMetaRequestDTO meta,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls,
            List<LessonSequenceItemDTO> sequence) {

        Course course = findCourseOrThrow(courseId);

        boolean titleExists = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .anyMatch(l -> l.getTitle().equalsIgnoreCase(meta.getTitle()));
        if (titleExists) {
            throw new IllegalArgumentException(
                    "A lesson with this title already exists in the course");
        }

        int orderIndex;
        if (meta.getOrderIndex() != null) {
            orderIndex = meta.getOrderIndex();
        } else {
            Integer maxIndex = lessonRepository.findMaxOrderIndexByCourseId(courseId);
            orderIndex = (maxIndex == null) ? 0 : maxIndex + 1;
        }

        Lesson lesson = Lesson.builder()
                .title(meta.getTitle())
                .description(meta.getDescription())
                .type(LessonType.VIDEO_UPLOAD)
                .orderIndex(orderIndex)
                .durationMinutes(meta.getDurationMinutes())
                .course(course)
                .build();

        if (sequence != null && !sequence.isEmpty()) {
            applySequenceCreateOrThrow(lesson, sequence, videos, pdfs, images, youtubeUrls);
        } else {
            addContentsOrThrow(lesson, videos, pdfs, images, youtubeUrls);
            assignSequentialContentOrderIndex(lesson.getContents());
        }
        syncLegacyFieldsFromContents(lesson);

        return toResponseDTO(lessonRepository.save(lesson));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponseDTO> getLessonsByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonResponseDTO> getAllLessons(Long courseId, Pageable pageable) {
        if (courseId != null) {
            return lessonRepository.findByCourseId(courseId, pageable)
                    .map(this::toResponseDTO);
        }
        return lessonRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponseDTO getLessonById(Long courseId, Long lessonId) {
        return toResponseDTO(findLessonOrThrow(courseId, lessonId));
    }

    @Override
    public LessonResponseDTO updateLesson(
            Long courseId,
            Long lessonId,
            LessonMetaRequestDTO meta,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls,
            List<LessonSequenceItemDTO> sequence) {
        Lesson lesson = findLessonOrThrow(courseId, lessonId);

        lesson.setTitle(meta.getTitle());
        lesson.setDescription(meta.getDescription());
        lesson.setDurationMinutes(meta.getDurationMinutes());
        if (meta.getOrderIndex() != null) {
            lesson.setOrderIndex(meta.getOrderIndex());
        }

        if (sequence != null && !sequence.isEmpty()) {
            applySequenceUpdateOrThrow(lesson, sequence, videos, pdfs, images, youtubeUrls);
        } else {
            // Backward compatibility: treat update like "replace contents" when sequence is not provided.
            deleteExistingContentsFiles(lesson);
            addContentsOrThrow(lesson, videos, pdfs, images, youtubeUrls);
            assignSequentialContentOrderIndex(lesson.getContents());
        }
        syncLegacyFieldsFromContents(lesson);

        return toResponseDTO(lessonRepository.save(lesson));
    }

    @Override
    public void deleteLesson(Long courseId, Long lessonId) {
        Lesson lesson = findLessonOrThrow(courseId, lessonId);
        deleteExistingContentsFiles(lesson);
        lessonRepository.delete(lesson);
    }

    private void addContentsOrThrow(
            Lesson lesson,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls) {

        if (!hasAnyContent(videos, pdfs, images, youtubeUrls)) {
            throw new IllegalArgumentException("At least one content (video/pdf/image/youtube) is required");
        }

        if (videos != null) {
            for (MultipartFile f : videos) {
                if (f == null || f.isEmpty()) continue;
                String path = fileStorageService.storeLessonContent(f, LessonType.VIDEO_UPLOAD);
                lesson.getContents().add(LessonContent.builder()
                        .type(LessonType.VIDEO_UPLOAD)
                        .contentUrl(path)
                        .orderIndex(0)
                        .lesson(lesson)
                        .build());
            }
        }

        if (pdfs != null) {
            for (MultipartFile f : pdfs) {
                if (f == null || f.isEmpty()) continue;
                String path = fileStorageService.storeLessonContent(f, LessonType.PDF);
                lesson.getContents().add(LessonContent.builder()
                        .type(LessonType.PDF)
                        .contentUrl(path)
                        .orderIndex(0)
                        .lesson(lesson)
                        .build());
            }
        }

        if (images != null) {
            for (MultipartFile f : images) {
                if (f == null || f.isEmpty()) continue;
                String path = fileStorageService.storeLessonContent(f, LessonType.IMAGE);
                lesson.getContents().add(LessonContent.builder()
                        .type(LessonType.IMAGE)
                        .contentUrl(path)
                        .orderIndex(0)
                        .lesson(lesson)
                        .build());
            }
        }

        if (youtubeUrls != null) {
            for (String url : youtubeUrls) {
                if (url == null || url.isBlank()) continue;
                lesson.getContents().add(LessonContent.builder()
                        .type(LessonType.YOUTUBE_EMBED)
                        .contentUrl(url)
                        .orderIndex(0)
                        .lesson(lesson)
                        .build());
            }
        }

        if (lesson.getContents().isEmpty()) {
            throw new IllegalArgumentException("At least one valid content is required");
        }
    }

    private boolean hasAnyContent(
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls) {

        if (videos != null) {
            for (MultipartFile f : videos) {
                if (f != null && !f.isEmpty()) return true;
            }
        }
        if (pdfs != null) {
            for (MultipartFile f : pdfs) {
                if (f != null && !f.isEmpty()) return true;
            }
        }
        if (images != null) {
            for (MultipartFile f : images) {
                if (f != null && !f.isEmpty()) return true;
            }
        }
        if (youtubeUrls != null) {
            for (String u : youtubeUrls) {
                if (u != null && !u.isBlank()) return true;
            }
        }
        return false;
    }

    private void deleteExistingContentsFiles(Lesson lesson) {
        if (lesson.getContents() == null) return;
        for (LessonContent c : lesson.getContents()) {
            if (c == null) continue;
            if (c.getType() == LessonType.YOUTUBE_EMBED) continue;
            fileStorageService.deleteFile(c.getContentUrl());
        }
        lesson.getContents().clear();
    }

    private void assignSequentialContentOrderIndex(List<LessonContent> contents) {
        if (contents == null) return;
        int idx = 0;
        for (LessonContent c : contents) {
            if (c == null) continue;
            c.setOrderIndex(idx++);
        }
    }

    private void applySequenceCreateOrThrow(
            Lesson lesson,
            List<LessonSequenceItemDTO> sequence,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls) {

        if (sequence == null || sequence.isEmpty()) {
            throw new IllegalArgumentException("Sequence is empty");
        }
        if (lesson.getContents() == null) {
            lesson.setContents(new ArrayList<>());
        }
        lesson.getContents().clear();

        for (int i = 0; i < sequence.size(); i++) {
            LessonSequenceItemDTO step = sequence.get(i);
            if (step == null || step.getType() == null) {
                throw new IllegalArgumentException("Invalid sequence step at index " + i);
            }
            if (step.getExistingContentId() != null) {
                throw new IllegalArgumentException("existingContentId is not allowed when creating a lesson");
            }

            LessonType type = step.getType();
            LessonContent created;
            if (type == LessonType.YOUTUBE_EMBED) {
                Integer yIdx = step.getYoutubeIndex();
                if (yIdx == null || youtubeUrls == null || yIdx < 0 || yIdx >= youtubeUrls.length) {
                    throw new IllegalArgumentException("Invalid youtubeIndex at step " + i);
                }
                String url = youtubeUrls[yIdx];
                if (url == null || url.isBlank()) {
                    throw new IllegalArgumentException("YouTube URL is blank at step " + i);
                }
                created = LessonContent.builder()
                        .type(LessonType.YOUTUBE_EMBED)
                        .contentUrl(url)
                        .orderIndex(i)
                        .lesson(lesson)
                        .build();
            } else {
                Integer fIdx = step.getFileIndex();
                if (fIdx == null || fIdx < 0) {
                    throw new IllegalArgumentException("Invalid fileIndex at step " + i);
                }
                MultipartFile f;
                LessonType storageType;
                if (type == LessonType.VIDEO_UPLOAD) {
                    f = (videos == null || fIdx >= videos.length) ? null : videos[fIdx];
                    storageType = LessonType.VIDEO_UPLOAD;
                } else if (type == LessonType.PDF) {
                    f = (pdfs == null || fIdx >= pdfs.length) ? null : pdfs[fIdx];
                    storageType = LessonType.PDF;
                } else if (type == LessonType.IMAGE) {
                    f = (images == null || fIdx >= images.length) ? null : images[fIdx];
                    storageType = LessonType.IMAGE;
                } else {
                    throw new IllegalArgumentException("Unsupported step type " + type + " at index " + i);
                }
                if (f == null || f.isEmpty()) {
                    throw new IllegalArgumentException("Missing uploaded file for step " + i);
                }
                String path = fileStorageService.storeLessonContent(f, storageType);
                created = LessonContent.builder()
                        .type(storageType)
                        .contentUrl(path)
                        .orderIndex(i)
                        .lesson(lesson)
                        .build();
            }
            lesson.getContents().add(created);
        }

        if (lesson.getContents().isEmpty()) {
            throw new IllegalArgumentException("At least one valid content is required");
        }
    }

    private void applySequenceUpdateOrThrow(
            Lesson lesson,
            List<LessonSequenceItemDTO> sequence,
            MultipartFile[] videos,
            MultipartFile[] pdfs,
            MultipartFile[] images,
            String[] youtubeUrls) {

        if (sequence == null || sequence.isEmpty()) {
            throw new IllegalArgumentException("Sequence is empty");
        }

        List<LessonContent> existing = lesson.getContents() == null ? List.of() : new ArrayList<>(lesson.getContents());
        Map<Long, LessonContent> existingById = new HashMap<>();
        for (LessonContent c : existing) {
            if (c != null && c.getId() != null) existingById.put(c.getId(), c);
        }

        Set<Long> keptExistingIds = new HashSet<>();
        List<LessonContent> nextContents = new ArrayList<>();

        for (int i = 0; i < sequence.size(); i++) {
            LessonSequenceItemDTO step = sequence.get(i);
            if (step == null || step.getType() == null) {
                throw new IllegalArgumentException("Invalid sequence step at index " + i);
            }

            LessonContent content;
            if (step.getExistingContentId() != null) {
                Long existingId = step.getExistingContentId();
                LessonContent found = existingById.get(existingId);
                if (found == null) {
                    throw new IllegalArgumentException("Existing content not found: " + existingId);
                }
                keptExistingIds.add(existingId);
                found.setOrderIndex(i);
                content = found;
            } else if (step.getType() == LessonType.YOUTUBE_EMBED) {
                Integer yIdx = step.getYoutubeIndex();
                if (yIdx == null || youtubeUrls == null || yIdx < 0 || yIdx >= youtubeUrls.length) {
                    throw new IllegalArgumentException("Invalid youtubeIndex at step " + i);
                }
                String url = youtubeUrls[yIdx];
                if (url == null || url.isBlank()) {
                    throw new IllegalArgumentException("YouTube URL is blank at step " + i);
                }
                content = LessonContent.builder()
                        .type(LessonType.YOUTUBE_EMBED)
                        .contentUrl(url)
                        .orderIndex(i)
                        .lesson(lesson)
                        .build();
            } else {
                Integer fIdx = step.getFileIndex();
                if (fIdx == null || fIdx < 0) {
                    throw new IllegalArgumentException("Invalid fileIndex at step " + i);
                }
                MultipartFile f;
                LessonType storageType;
                if (step.getType() == LessonType.VIDEO_UPLOAD) {
                    f = (videos == null || fIdx >= videos.length) ? null : videos[fIdx];
                    storageType = LessonType.VIDEO_UPLOAD;
                } else if (step.getType() == LessonType.PDF) {
                    f = (pdfs == null || fIdx >= pdfs.length) ? null : pdfs[fIdx];
                    storageType = LessonType.PDF;
                } else if (step.getType() == LessonType.IMAGE) {
                    f = (images == null || fIdx >= images.length) ? null : images[fIdx];
                    storageType = LessonType.IMAGE;
                } else {
                    throw new IllegalArgumentException("Unsupported step type " + step.getType() + " at index " + i);
                }
                if (f == null || f.isEmpty()) {
                    throw new IllegalArgumentException("Missing uploaded file for step " + i);
                }
                String path = fileStorageService.storeLessonContent(f, storageType);
                content = LessonContent.builder()
                        .type(storageType)
                        .contentUrl(path)
                        .orderIndex(i)
                        .lesson(lesson)
                        .build();
            }

            nextContents.add(content);
        }

        // Delete removed existing contents' files (keep existing without reupload)
        for (LessonContent old : existing) {
            if (old == null || old.getId() == null) continue;
            if (keptExistingIds.contains(old.getId())) continue;
            if (old.getType() != LessonType.YOUTUBE_EMBED) {
                fileStorageService.deleteFile(old.getContentUrl());
            }
        }

        lesson.getContents().clear();
        lesson.getContents().addAll(nextContents);

        if (lesson.getContents().isEmpty()) {
            throw new IllegalArgumentException("At least one valid content is required");
        }
    }

    private void syncLegacyFieldsFromContents(Lesson lesson) {
        // Keep old fields for existing UI/backward compatibility.
        // Choose the first available content as primary.
        LessonContent primary = lesson.getContents().stream().findFirst().orElse(null);
        if (primary == null) {
            lesson.setType(LessonType.VIDEO_UPLOAD);
            lesson.setContentUrl(null);
            return;
        }
        lesson.setType(primary.getType());
        lesson.setContentUrl(primary.getContentUrl());
    }

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    private Lesson findLessonOrThrow(Long courseId, Long lessonId) {
        return lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lesson " + lessonId + " not found in course " + courseId));
    }

    private LessonResponseDTO toResponseDTO(Lesson lesson) {
        return LessonResponseDTO.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .type(lesson.getType())
                .contentUrl(lesson.getContentUrl())
                .contents(lesson.getContents() == null ? List.of() : lesson.getContents().stream()
                        .map(this::toContentResponseDTO)
                        .collect(Collectors.toList()))
                .orderIndex(lesson.getOrderIndex())
                .durationMinutes(lesson.getDurationMinutes())
                .courseId(lesson.getCourse().getId())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    private LessonContentResponseDTO toContentResponseDTO(LessonContent content) {
        return LessonContentResponseDTO.builder()
                .id(content.getId())
                .type(content.getType())
                .contentUrl(content.getContentUrl())
                .orderIndex(content.getOrderIndex())
                .createdAt(content.getCreatedAt())
                .build();
    }
}