package tn.esprit.tpfoyer.Services;

import tn.esprit.tpfoyer.Dto.CourseDetailResponseDTO;
import tn.esprit.tpfoyer.Dto.CourseRequestDTO;
import tn.esprit.tpfoyer.Dto.CourseResponseDTO;
import tn.esprit.tpfoyer.Entities.enums.CourseCategory;
import tn.esprit.tpfoyer.Entities.enums.CourseLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface ICourseService {

    CourseResponseDTO createCourse(CourseRequestDTO dto, MultipartFile thumbnail, MultipartFile certificate);

    Page<CourseResponseDTO> getAllCourses(
            String search,
            CourseCategory category,
            CourseLevel level,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isPublished,
            Pageable pageable);

    CourseDetailResponseDTO getCourseById(Long id);

    CourseResponseDTO updateCourse(Long id, CourseRequestDTO dto, MultipartFile thumbnail, MultipartFile certificate);


    void deleteCourse(Long id);
}
