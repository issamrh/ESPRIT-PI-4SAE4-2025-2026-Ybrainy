package tn.esprit.eventservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.eventservice.client.InscriptionClient;
import tn.esprit.eventservice.client.UserClient;
import tn.esprit.eventservice.dto.EventAssignmentResponseDto;
import tn.esprit.eventservice.dto.InscriptionAssignmentResultDto;
import tn.esprit.eventservice.dto.InscriptionCreateDto;
import tn.esprit.eventservice.dto.UserDto;
import tn.esprit.eventservice.entity.Event;
import tn.esprit.eventservice.entity.EventStatut;
import tn.esprit.eventservice.entity.EventType;
import tn.esprit.eventservice.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicesImpTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private InscriptionClient inscriptionClient;

    @Mock
    private EventDescriptionGenerationService eventDescriptionGenerationService;

    @Mock
    private EventImageGenerationService eventImageGenerationService;

    @InjectMocks
    private EventServicesImp eventServices;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setIdEvent(10L);
        event.setName("AI Webinar");
        event.setDescription("A focused event about AI.");
        event.setType(EventType.WEBINAIRE);
        event.setCapacite(30);
        event.setStatut(EventStatut.PUBLIE);
        event.setDateDebut(LocalDateTime.now().plusDays(2));
        event.setDateFin(LocalDateTime.now().plusDays(3));
    }

    @Test
    void addEvent_shouldPopulateAdminReferenceAndGeneratedImageBeforeSaving() {
        when(userClient.findFirstByRole("ADMIN"))
                .thenReturn(Optional.of(new UserDto(7L, "Admin", "Root", "admin@ybrainy.tn", "ADMIN")));
        when(eventRepository.countByReferenceEventStartingWith(anyString())).thenReturn(0L);
        when(eventRepository.existsByReferenceEvent(anyString())).thenReturn(false);
        when(eventImageGenerationService.generateImage(any(), any(), any()))
                .thenReturn(new EventImageGenerationService.GeneratedImageResult("http://image.local/generated.png", true));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = eventServices.addEvent(event);

        assertThat(saved.getAdminId()).isEqualTo(7L);
        assertThat(saved.getReferenceEvent()).startsWith("WEB-");
        assertThat(saved.getImageUrl()).isEqualTo("http://image.local/generated.png");
        assertThat(saved.getDateCreation()).isNotNull();

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceEvent()).isEqualTo(saved.getReferenceEvent());
    }

    @Test
    void updateEvent_shouldRejectChangesWhenExistingEventIsTerminated() {
        Event existing = new Event();
        existing.setIdEvent(event.getIdEvent());
        existing.setStatut(EventStatut.TERMINE);
        existing.setDateFin(LocalDateTime.now().minusDays(1));

        when(eventRepository.findById(event.getIdEvent())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventServices.updateEvent(event))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException response = (ResponseStatusException) exception;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void assignStudentToEvent_shouldUseWaitingListWhenEventIsAtCapacity() {
        event.setCapacite(1);
        when(eventRepository.findById(event.getIdEvent())).thenReturn(Optional.of(event));
        when(userClient.findById(22L))
                .thenReturn(Optional.of(new UserDto(22L, "Student", "One", "student@ybrainy.tn", "STUDENT")));
        when(inscriptionClient.existsByStudentIdAndEventId(22L, event.getIdEvent())).thenReturn(false);
        when(inscriptionClient.countConfirmedByEventId(event.getIdEvent())).thenReturn(1L);
        when(inscriptionClient.createInscription(any(InscriptionCreateDto.class)))
                .thenReturn(new InscriptionAssignmentResultDto(99L, event.getIdEvent(), 22L, "LISTE_ATTENTE"));

        EventAssignmentResponseDto response = eventServices.assignStudentToEvent(event.getIdEvent(), 22L);

        assertThat(response.eventId()).isEqualTo(event.getIdEvent());
        assertThat(response.studentId()).isEqualTo(22L);
        assertThat(response.inscriptionStatus()).isEqualTo("LISTE_ATTENTE");

        ArgumentCaptor<InscriptionCreateDto> captor = ArgumentCaptor.forClass(InscriptionCreateDto.class);
        verify(inscriptionClient).createInscription(captor.capture());
        assertThat(captor.getValue().initialStatus()).isEqualTo("LISTE_ATTENTE");
    }
}
