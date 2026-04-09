package tn.esprit.tpfoyer.Controllers;

import tn.esprit.tpfoyer.Dto.LessonMetaRequestDTO;
import tn.esprit.tpfoyer.Dto.LessonResponseDTO;
import tn.esprit.tpfoyer.Dto.LessonSequenceItemDTO;
import tn.esprit.tpfoyer.Services.ILessonService;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final ILessonService lessonService;
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

        String[] youtubeUrlsArr = (youtubeUrls == null || youtubeUrls.isEmpty()) ? null : youtubeUrls.toArray(new String[0]);
		List<LessonSequenceItemDTO> sequence = parseSequence(readSequenceJson(sequencePart));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(courseId, meta, videos, pdfs, images, youtubeUrlsArr, sequence));
    }

    @GetMapping
    public ResponseEntity<List<LessonResponseDTO>> getLessons(@PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.getLessonsByCourse(courseId));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDTO> getLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {

        return ResponseEntity.ok(lessonService.getLessonById(courseId, lessonId));
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

        String[] youtubeUrlsArr = (youtubeUrls == null || youtubeUrls.isEmpty()) ? null : youtubeUrls.toArray(new String[0]);
		List<LessonSequenceItemDTO> sequence = parseSequence(readSequenceJson(sequencePart));

        return ResponseEntity.ok(lessonService.updateLesson(courseId, lessonId, meta, videos, pdfs, images, youtubeUrlsArr, sequence));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {

        lessonService.deleteLesson(courseId, lessonId);
        return ResponseEntity.noContent().build();
    }

	private LessonMetaRequestDTO parseAndValidateMeta(MultipartFile lessonPart) {
		if (lessonPart == null || lessonPart.isEmpty()) {
			throw new IllegalArgumentException("Missing required part: lesson");
		}
		try {
			LessonMetaRequestDTO meta = objectMapper.readValue(lessonPart.getBytes(), LessonMetaRequestDTO.class);
			var violations = validator.validate(meta);
			if (!violations.isEmpty()) {
				throw new ConstraintViolationException(violations);
			}
			return meta;
		} catch (ConstraintViolationException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid lesson meta JSON", e);
		}
	}

	private List<LessonSequenceItemDTO> parseSequence(String sequenceJson) {
		if (sequenceJson == null || sequenceJson.isBlank()) return null;
		try {
			return objectMapper.readValue(sequenceJson, new TypeReference<List<LessonSequenceItemDTO>>() {});
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid sequence JSON", e);
		}
	}

	private String readSequenceJson(MultipartFile sequencePart) {
		try {
			if (sequencePart == null || sequencePart.isEmpty()) {
				return null;
			}
			return new String(sequencePart.getBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid sequence part", e);
		}
	}
}
