package tn.esprit.eventservice.service;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.eventservice.client.InscriptionClient;
import tn.esprit.eventservice.client.UserClient;
import tn.esprit.eventservice.dto.InscriptionCreateDto;
import tn.esprit.eventservice.dto.UserDto;
import tn.esprit.eventservice.entity.Event;
import tn.esprit.eventservice.entity.EventStatut;
import tn.esprit.eventservice.entity.EventType;
import tn.esprit.eventservice.repository.EventRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class EventServicesImp implements IEventServices {

    private final EventRepository eventRepository;

    /** Cross-service: user-service */
    private final UserClient userClient;

    /** Cross-service: inscription-service */
    private final InscriptionClient inscriptionClient;

    private final EventDescriptionGenerationService eventDescriptionGenerationService;

    // ------------------------------------------------------------------ CRUD

    @Override
    public Event addEvent(Event event) {
        validateEventDates(event);
        ensureAdminId(event, null);
        event.setDateCreation(LocalDateTime.now());
        event.setReferenceEvent(generateUniqueReference(event.getType()));
        return eventRepository.save(event);
    }

    @Override
    public Event updateEvent(Event event) {
        Event existing = eventRepository.findById(event.getIdEvent()).orElse(null);
        if (existing != null) {
            applyAutomaticTermination(existing);
            if (EventStatut.TERMINE.equals(existing.getStatut())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Terminated events cannot be modified.");
            }
        }
        validateEventDates(event);
        ensureAdminId(event, existing);
        preserveOrGenerateReference(event, existing);
        return eventRepository.save(event);
    }

    @Override
    public String generateDescription(String name, String type) {
        return eventDescriptionGenerationService.generateDescription(name, type).description();
    }

    @Override
    public Event getEventById(long idEvent) {
        Event event = eventRepository.findById(idEvent).orElse(null);
        if (event != null) {
            ensureReferencePersisted(event);
            applyAutomaticTermination(event);
            attachInscriptionCount(event);
        }
        return event;
    }

    @Override
    public List<Event> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            ensureReferencePersisted(event);
            applyAutomaticTermination(event);
            attachInscriptionCount(event);
        }
        return events;
    }

    @Override
    public void deleteEvent(long idEvent) {
        eventRepository.deleteById(idEvent);
    }

    // ------------------------------------------------------- Student assignment

    @Override
    public void assignStudentToEvent(long idEvent, long idStudent) {
        // 1. Resolve event locally
        Event event = eventRepository.findById(idEvent)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));

        // 2. Resolve student via user-service (Feign)
        UserDto student = userClient.findById(idStudent)
                .or(() -> userClient.findFirstByRole("STUDENT"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found."));

        // 3. Guard against duplicate inscription via inscription-service (Feign)
        if (inscriptionClient.existsByStudentIdAndEventId(student.idUser(), event.getIdEvent())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student already registered for this event.");
        }

        // 4. Delegate inscription creation to inscription-service (Feign)
        inscriptionClient.createInscription(
                new InscriptionCreateDto(event.getIdEvent(), student.idUser())
        );

        // 5. Refresh count on the returned event object
        attachInscriptionCount(event);
    }

    // --------------------------------------------------------- Private helpers

    private void validateEventDates(Event event) {
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event payload is required.");
        }
        if (event.getDateDebut() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start Date is required.");
        }
        if (event.getDateFin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End Date is required.");
        }

        LocalDate today = LocalDate.now();
        if (event.getDateDebut().toLocalDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start Date cannot be before today.");
        }
        if (event.getDateFin().toLocalDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End Date cannot be before today.");
        }
        if (event.getDateFin().isBefore(event.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End Date must be after Start Date.");
        }
    }

    private void applyAutomaticTermination(Event event) {
        if (event == null || event.getDateFin() == null) return;
        if (event.getDateFin().isBefore(LocalDateTime.now())
                && !EventStatut.TERMINE.equals(event.getStatut())) {
            event.setStatut(EventStatut.TERMINE);
            eventRepository.save(event);
        }
    }

    private void attachInscriptionCount(Event event) {
        if (event == null || event.getIdEvent() <= 0) return;
        // Delegate count to inscription-service (Feign)
        event.setInscriptionsCount(
                inscriptionClient.countConfirmedByEventId(event.getIdEvent())
        );
    }

    private void ensureAdminId(Event event, Event existing) {
        if (event == null) return;

        if (event.getAdminId() > 0) {
            return;
        }

        if (existing != null && existing.getAdminId() > 0) {
            event.setAdminId(existing.getAdminId());
            return;
        }

        long resolvedAdminId = userClient.findFirstByRole("ADMIN")
                .map(UserDto::idUser)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No admin user is available to assign to this event."
                ));

        event.setAdminId(resolvedAdminId);
    }

    private void preserveOrGenerateReference(Event event, Event existing) {
        if (event == null) return;

        if (existing != null && existing.getReferenceEvent() != null && !existing.getReferenceEvent().isBlank()) {
            event.setReferenceEvent(existing.getReferenceEvent());
            return;
        }

        if (event.getReferenceEvent() == null || event.getReferenceEvent().isBlank()) {
            event.setReferenceEvent(generateUniqueReference(event.getType()));
        }
    }

    private void ensureReferencePersisted(Event event) {
        if (event == null) return;
        if (event.getReferenceEvent() != null && !event.getReferenceEvent().isBlank()) return;

        event.setReferenceEvent(generateUniqueReference(event.getType()));
        eventRepository.save(event);
    }

    private String generateUniqueReference(EventType type) {
        String prefix = buildTypePrefix(type);
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String basePrefix = prefix + "-" + datePart + "-";

        long nextCounter = eventRepository.countByReferenceEventStartingWith(basePrefix) + 1;
        String candidate = buildReference(basePrefix, nextCounter);

        while (eventRepository.existsByReferenceEvent(candidate)) {
            nextCounter += 1;
            candidate = buildReference(basePrefix, nextCounter);
        }

        return candidate;
    }

    private String buildReference(String basePrefix, long counter) {
        return basePrefix + String.format("%04d", counter);
    }

    private String buildTypePrefix(EventType type) {
        if (type == null) {
            return "EVT";
        }

        return switch (type) {
            case HACKATHON -> "HCK";
            case WEBINAIRE -> "WEB";
            case FORMATION -> "FOR";
            case ATELIER -> "ATL";
        };
    }
}