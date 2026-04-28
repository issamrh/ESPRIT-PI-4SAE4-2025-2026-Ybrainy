package tn.esprit.tpfoyer.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.tpfoyer.Clients.LessonClient;
import tn.esprit.tpfoyer.Dto.AiSearchResultDTO;
import tn.esprit.tpfoyer.Dto.CourseDetailResponseDTO;
import tn.esprit.tpfoyer.Dto.CourseResponseDTO;
import tn.esprit.tpfoyer.Services.ICertificateService;
import tn.esprit.tpfoyer.Services.ICourseService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock ICourseService courseService;
    @Mock ICertificateService certificateService;
    @Mock LessonClient lessonClient;

    @InjectMocks CourseController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "uploadDir", "/tmp/uploads");
        ReflectionTestUtils.setField(controller, "talkingHeadBaseUrl", "http://localhost:8765");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/courses/{id} returns 200 with course detail")
    void getCourseById_returns200() throws Exception {
        CourseDetailResponseDTO detail = CourseDetailResponseDTO.builder()
                .id(1L).title("Spring Boot Course").build();
        when(courseService.getCourseById(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Spring Boot Course"));
    }

    @Test
    @DisplayName("GET /api/courses returns 200 with paginated courses")
    void getAllCourses_returnsPaginatedList() throws Exception {
        CourseResponseDTO dto = CourseResponseDTO.builder().id(1L).title("Test Course").build();
        when(courseService.getAllCourses(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("DELETE /api/courses/{id} returns 204")
    void deleteCourse_returns204() throws Exception {
        doNothing().when(courseService).deleteCourse(eq(1L), any(), any());

        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse(eq(1L), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/courses/{id}/publish with STUDENT role returns 403")
    void togglePublish_studentRole_returns403() throws Exception {
        mockMvc.perform(patch("/api/courses/1/publish")
                        .param("publish", "true")
                        .param("requestingRole", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("PATCH /api/courses/{id}/publish with INSTRUCTOR role returns 200")
    void togglePublish_instructorRole_returns200() throws Exception {
        when(courseService.togglePublish(eq(1L), eq(true), eq(10L), eq("INSTRUCTOR")))
                .thenReturn(Map.of("published", true));

        mockMvc.perform(patch("/api/courses/1/publish")
                        .param("publish", "true")
                        .param("requestingUserId", "10")
                        .param("requestingRole", "INSTRUCTOR"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/courses/{id}/publish returns 400 when service throws")
    void togglePublish_serviceThrows_returns400() throws Exception {
        when(courseService.togglePublish(any(), anyBoolean(), any(), any()))
                .thenThrow(new RuntimeException("Cannot publish: not authorized"));

        mockMvc.perform(patch("/api/courses/1/publish")
                        .param("publish", "true")
                        .param("requestingRole", "INSTRUCTOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot publish: not authorized"));
    }

    @Test
    @DisplayName("POST /api/courses/search/ai with blank query returns 400")
    void aiSearch_blankQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/courses/search/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("POST /api/courses/search/ai with null query returns 400")
    void aiSearch_nullQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/courses/search/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/courses/search/ai with valid query returns 200")
    void aiSearch_validQuery_returns200() throws Exception {
        AiSearchResultDTO result = new AiSearchResultDTO();
        when(courseService.aiSearch(eq("spring boot"), anyInt(), anyInt())).thenReturn(result);

        mockMvc.perform(post("/api/courses/search/ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"spring boot\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/courses/{id}/exists returns true when course found")
    void courseExists_returnsTrue_whenCourseFound() throws Exception {
        when(courseService.getCourseById(1L)).thenReturn(CourseDetailResponseDTO.builder().id(1L).build());

        mockMvc.perform(get("/api/courses/1/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/exists returns false when service throws")
    void courseExists_returnsFalse_whenCourseNotFound() throws Exception {
        when(courseService.getCourseById(99L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/courses/99/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("GET /api/courses/stats/by-category returns category count map")
    void getStatsByCategory_returnsMap() throws Exception {
        when(courseService.getCourseStatsByCategory())
                .thenReturn(Map.of("PROGRAMMING", 5L, "DESIGN", 3L));

        mockMvc.perform(get("/api/courses/stats/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PROGRAMMING").value(5));
    }

    @Test
    @DisplayName("GET /api/courses/stats/by-category returns empty map when service throws")
    void getStatsByCategory_returnsEmptyMap_onError() throws Exception {
        when(courseService.getCourseStatsByCategory()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/courses/stats/by-category"))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));
    }
}
