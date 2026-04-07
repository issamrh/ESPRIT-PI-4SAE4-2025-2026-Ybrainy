package tn.esprit.tpfoyer.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.tpfoyer.Dto.LessonResponseDTO;
import tn.esprit.tpfoyer.Services.ILessonService;

@RestController
@RequestMapping("/api/courses/lessons")
@RequiredArgsConstructor
public class LessonsQueryController {

    private final ILessonService lessonService;

    @GetMapping
    public ResponseEntity<Page<LessonResponseDTO>> getAllLessons(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(lessonService.getAllLessons(courseId, pageable));
    }
}
