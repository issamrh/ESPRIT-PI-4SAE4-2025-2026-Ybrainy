package tn.esprit.eventservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.eventservice.dto.DescriptionGenerationRequest;
import tn.esprit.eventservice.dto.EventAnalyticsResponseDto;
import tn.esprit.eventservice.entity.Event;
import tn.esprit.eventservice.entity.EventStatut;
import tn.esprit.eventservice.entity.EventType;
import tn.esprit.eventservice.service.EventDescriptionGenerationService;
import tn.esprit.eventservice.service.IEventServices;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRestControllers.class)
class EventRestControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IEventServices eventServices;

    @Test
    void addEvent_shouldReturnSavedEventPayload() throws Exception {
        Event event = new Event();
        event.setIdEvent(15L);
        event.setName("Backend Testing Workshop");
        event.setDescription("Mockito and MockMvc practice.");
        event.setType(EventType.ATELIER);
        event.setStatut(EventStatut.PUBLIE);
        event.setDateDebut(LocalDateTime.now().plusDays(1));
        event.setDateFin(LocalDateTime.now().plusDays(2));

        when(eventServices.addEvent(org.mockito.ArgumentMatchers.any(Event.class))).thenReturn(event);

        mockMvc.perform(post("/Event/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvent").value(15L))
                .andExpect(jsonPath("$.name").value("Backend Testing Workshop"))
                .andExpect(jsonPath("$.type").value("ATELIER"));
    }

    @Test
    void generateDescription_shouldExposeServiceResponse() throws Exception {
        when(eventServices.generateDescription(eq("AI Hackathon"), eq("HACKATHON")))
                .thenReturn(new EventDescriptionGenerationService.GeneratedDescriptionResult(
                        "A sharp AI event description.",
                        true
                ));

        DescriptionGenerationRequest request = new DescriptionGenerationRequest("AI Hackathon", "HACKATHON");

        mockMvc.perform(post("/Event/generate-description")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("A sharp AI event description."))
                .andExpect(jsonPath("$.generatedByAi").value(true));
    }

    @Test
    void getAnalytics_shouldReturnOverviewForRequestedRange() throws Exception {
        EventAnalyticsResponseDto analytics = new EventAnalyticsResponseDto(
                new EventAnalyticsResponseDto.TrendSeries(
                        "April",
                        4,
                        "March",
                        2,
                        List.of("Week 01", "Week 02"),
                        List.of(2, 2),
                        List.of(1, 1)
                ),
                new EventAnalyticsResponseDto.OverviewSeries(
                        "month",
                        List.of("Week 1", "Week 2"),
                        List.of(1, 2),
                        List.of(5, 6),
                        List.of(1, 1)
                )
        );

        when(eventServices.getAnalytics("month")).thenReturn(analytics);

        mockMvc.perform(get("/Event/analytics").param("range", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.range").value("month"))
                .andExpect(jsonPath("$.trend.currentTotal").value(4))
                .andExpect(jsonPath("$.overview.eventCounts[1]").value(2));

        verify(eventServices).getAnalytics("month");
    }
}
