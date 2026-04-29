package tn.esprit.lessonservice.controllers;

import tn.esprit.lessonservice.dto.*;
import tn.esprit.lessonservice.entities.*;
import tn.esprit.lessonservice.services.IFileStorageService;
import tn.esprit.lessonservice.services.ILessonService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons")
@RequiredArgsConstructor
public class LessonFileController {

    private final ILessonService lessonService;
    private final IFileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponseDTO> createLesson(
            @PathVariable Long courseId,
            @RequestPart("lesson") MultipartFile lessonPart,
            @RequestPart(value = "videos", required = false) MultipartFile[] videos,
            @RequestPart(value = "pdfs", required = false) MultipartFile[] pdfs,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "youtubeUrls", required = false) List<String> youtubeUrls,
            @RequestPart(value = "sequence", required = false) MultipartFile sequencePart) {

        LessonMetaRequestDTO meta = parseAndValidateMeta(lessonPart);
        String[] youtubeArr = toArray(youtubeUrls);
        List<LessonSequenceItemDTO> sequence = parseSequence(readSequenceJson(sequencePart));

        // Build lesson entity
        Lesson lesson = buildLesson(meta, videos, pdfs, images, youtubeArr, sequence, null);
        Lesson created = lessonService.createLesson(courseId, lesson);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(created));
    }

    @GetMapping
    public ResponseEntity<List<LessonResponseDTO>> getLessons(@PathVariable Long courseId) {
        return ResponseEntity.ok(
            lessonService.getLessonsByCourse(courseId).stream()
                .map(this::toResponseDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDTO> getLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(
            toResponseDTO(lessonService.getLessonByIdAndCourse(lessonId, courseId)));
    }

    @PutMapping(value = "/{lessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponseDTO> updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestPart("lesson") MultipartFile lessonPart,
            @RequestPart(value = "videos", required = false) MultipartFile[] videos,
            @RequestPart(value = "pdfs", required = false) MultipartFile[] pdfs,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "youtubeUrls", required = false) List<String> youtubeUrls,
            @RequestPart(value = "sequence", required = false) MultipartFile sequencePart) {

        LessonMetaRequestDTO meta = parseAndValidateMeta(lessonPart);
        String[] youtubeArr = toArray(youtubeUrls);
        List<LessonSequenceItemDTO> sequence = parseSequence(readSequenceJson(sequencePart));

        Lesson existing = lessonService.getLessonByIdAndCourse(lessonId, courseId);

        // Delete removed content files
        List<LessonContent> existingContents =
            Optional.ofNullable(existing.getContents()).orElse(Collections.emptyList());
        Set<Long> keptIds = new HashSet<>();
        if (sequence != null) {
            sequence.stream()
                .filter(s -> s.getExistingContentId() != null)
                .forEach(s -> keptIds.add(s.getExistingContentId()));
        }
        for (LessonContent c : existingContents) {
            if (!keptIds.contains(c.getId()) && c.getType() != LessonType.YOUTUBE_EMBED) {
                fileStorageService.deleteFile(c.getContentUrl());
            }
        }

        Lesson updated = buildLesson(meta, videos, pdfs, images, youtubeArr, sequence, existing);
        Lesson saved = lessonService.updateLesson(courseId, lessonId, updated);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {
        try {
            Lesson lesson = lessonService.getLessonByIdAndCourse(lessonId, courseId);
            Optional.ofNullable(lesson.getContents()).orElse(Collections.emptyList())
                .stream()
                .filter(c -> c.getType() != LessonType.YOUTUBE_EMBED)
                .forEach(c -> fileStorageService.deleteFile(c.getContentUrl()));
        } catch (Exception ignored) {}
        lessonService.deleteLesson(courseId, lessonId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Lesson buildLesson(LessonMetaRequestDTO meta,
                                MultipartFile[] videos, MultipartFile[] pdfs,
                                MultipartFile[] images, String[] youtubeUrls,
                                List<LessonSequenceItemDTO> sequence,
                                Lesson existing) {
        List<LessonContent> contents = new ArrayList<>();

        if (sequence != null && !sequence.isEmpty()) {
            Map<Long, LessonContent> existingById = new HashMap<>();
            if (existing != null && existing.getContents() != null) {
                existing.getContents().forEach(c -> existingById.put(c.getId(), c));
            }
            for (int i = 0; i < sequence.size(); i++) {
                LessonSequenceItemDTO step = sequence.get(i);
                if (step.getExistingContentId() != null) {
                    LessonContent found = existingById.get(step.getExistingContentId());
                    if (found != null) {
                        found.setOrderIndex(i);
                        contents.add(found);
                    }
                } else {
                    contents.add(buildContentFromStep(step, i, videos, pdfs, images, youtubeUrls));
                }
            }
        } else {
            int idx = 0;
            if (videos != null) for (MultipartFile f : videos)
                if (f != null && !f.isEmpty())
                    contents.add(buildContent(LessonType.VIDEO_UPLOAD,
                        fileStorageService.storeLessonContent(f, LessonType.VIDEO_UPLOAD), idx++));
            if (pdfs != null) for (MultipartFile f : pdfs)
                if (f != null && !f.isEmpty())
                    contents.add(buildContent(LessonType.PDF,
                        fileStorageService.storeLessonContent(f, LessonType.PDF), idx++));
            if (images != null) for (MultipartFile f : images)
                if (f != null && !f.isEmpty())
                    contents.add(buildContent(LessonType.IMAGE,
                        fileStorageService.storeLessonContent(f, LessonType.IMAGE), idx++));
            if (youtubeUrls != null) for (String url : youtubeUrls)
                if (url != null && !url.isBlank())
                    contents.add(buildContent(LessonType.YOUTUBE_EMBED, url, idx++));
        }

        if (contents.isEmpty()) {
            if (existing != null && existing.getContents() != null && !existing.getContents().isEmpty()) {
                contents.addAll(existing.getContents());
            } else {
                throw new IllegalArgumentException("At least one content is required");
            }
        }

        LessonContent primary = contents.get(0);
        Lesson lesson = existing != null ? existing : new Lesson();
        lesson.setTitle(meta.getTitle());
        lesson.setDescription(meta.getDescription());
        lesson.setOrderIndex(meta.getOrderIndex());
        lesson.setDurationMinutes(meta.getDurationMinutes());
        lesson.setType(primary.getType());
        lesson.setContentUrl(primary.getContentUrl());
        if (existing != null) {
            lesson.getContents().clear();
            lesson.getContents().addAll(contents);
        } else {
            lesson.setContents(contents);
        }
        return lesson;
    }

    private LessonContent buildContentFromStep(LessonSequenceItemDTO step, int i,
            MultipartFile[] videos, MultipartFile[] pdfs,
            MultipartFile[] images, String[] youtubeUrls) {
        LessonType type = step.getType();
        if (type == LessonType.YOUTUBE_EMBED) {
            Integer yIdx = step.getYoutubeIndex();
            if (yIdx == null || youtubeUrls == null || yIdx >= youtubeUrls.length)
                throw new IllegalArgumentException("Invalid youtubeIndex at step " + i);
            return buildContent(type, youtubeUrls[yIdx], i);
        }
        Integer fIdx = step.getFileIndex();
        if (fIdx == null) throw new IllegalArgumentException("Missing fileIndex at step " + i);
        MultipartFile f = switch (type) {
            case VIDEO_UPLOAD -> videos != null && fIdx < videos.length ? videos[fIdx] : null;
            case PDF -> pdfs != null && fIdx < pdfs.length ? pdfs[fIdx] : null;
            case IMAGE -> images != null && fIdx < images.length ? images[fIdx] : null;
            default -> throw new IllegalArgumentException("Unsupported type " + type);
        };
        if (f == null || f.isEmpty())
            throw new IllegalArgumentException("Missing file for step " + i);
        return buildContent(type, fileStorageService.storeLessonContent(f, type), i);
    }

    private LessonContent buildContent(LessonType type, String url, int orderIndex) {
        LessonContent c = new LessonContent();
        c.setType(type);
        c.setContentUrl(url);
        c.setOrderIndex(orderIndex);
        return c;
    }

    private LessonResponseDTO toResponseDTO(Lesson l) {
        List<LessonContentResponseDTO> contents =
            Optional.ofNullable(l.getContents()).orElse(Collections.emptyList())
                .stream()
                .map(c -> LessonContentResponseDTO.builder()
                    .id(c.getId())
                    .type(c.getType())
                    .contentUrl(c.getContentUrl())
                    .orderIndex(c.getOrderIndex())
                    .createdAt(c.getCreatedAt())
                    .build())
                .collect(Collectors.toList());
        return LessonResponseDTO.builder()
            .id(l.getId())
            .title(l.getTitle())
            .description(l.getDescription())
            .type(l.getType())
            .contentUrl(l.getContentUrl())
            .contents(contents)
            .orderIndex(l.getOrderIndex())
            .durationMinutes(l.getDurationMinutes())
            .courseId(l.getCourseId())
            .createdAt(l.getCreatedAt())
            .updatedAt(l.getUpdatedAt())
            .build();
    }

    private LessonMetaRequestDTO parseAndValidateMeta(MultipartFile lessonPart) {
        if (lessonPart == null || lessonPart.isEmpty())
            throw new IllegalArgumentException("Missing required part: lesson");
        try {
            LessonMetaRequestDTO meta = objectMapper.readValue(
                lessonPart.getBytes(), LessonMetaRequestDTO.class);
            var violations = validator.validate(meta);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
            return meta;
        } catch (ConstraintViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid lesson meta JSON", e);
        }
    }

    private List<LessonSequenceItemDTO> parseSequence(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json,
                new TypeReference<List<LessonSequenceItemDTO>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid sequence JSON", e);
        }
    }

    private String readSequenceJson(MultipartFile part) {
        try {
            if (part == null || part.isEmpty()) return null;
            return new String(part.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid sequence part", e);
        }
    }

    private String[] toArray(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.toArray(new String[0]);
    }
}
