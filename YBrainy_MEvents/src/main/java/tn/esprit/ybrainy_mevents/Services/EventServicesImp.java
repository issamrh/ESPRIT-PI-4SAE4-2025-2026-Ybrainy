package tn.esprit.ybrainy_mevents.Services;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.ybrainy_mevents.Entities.Event;
import tn.esprit.ybrainy_mevents.Entities.EventStatut;
import tn.esprit.ybrainy_mevents.Entities.Inscription;
import tn.esprit.ybrainy_mevents.Entities.InscriptionStatut;
import tn.esprit.ybrainy_mevents.Entities.Role;
import tn.esprit.ybrainy_mevents.Entities.User;
import tn.esprit.ybrainy_mevents.Repositories.EventRepository;
import tn.esprit.ybrainy_mevents.Repositories.InscriptionRepository;
import tn.esprit.ybrainy_mevents.Repositories.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class EventServicesImp implements IEventServices {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final InscriptionRepository inscriptionRepository;

    @Override
    public Event addEvent(Event event) {
        validateEventDates(event);
        event.setDateCreation(LocalDateTime.now());
        return eventRepository.save(event);
    }

    @Override
    public Event updateEvent(Event event) {
        Event existing = eventRepository.findById(event.getIdEvent()).orElse(null);
        if (existing != null) {
            applyAutomaticTermination(existing);
            if (EventStatut.TERMINE.equals(existing.getStatut())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Terminated events cannot be modified.");
            }
        }
        validateEventDates(event);
        return eventRepository.save(event);
    }

    @Override
    public Event getEventById(long idEvent) {
        Event event = eventRepository.findById(idEvent).orElse(null);
        if (event != null) {
            applyAutomaticTermination(event);
            attachInscriptionCount(event);
        }
        return event;
    }

    @Override
    public List<Event> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            applyAutomaticTermination(event);
            attachInscriptionCount(event);
        }
        return events;
    }

    @Override
    public void deleteEvent(long idEvent) {
        eventRepository.deleteById(idEvent);
    }

    @Override
    public void assignStudentToEvent(long idEvent, long idStudent) {
        Event event = eventRepository.findById(idEvent).orElse(null);
        User student = userRepository.findById(idStudent)
                .orElseGet(() -> userRepository.findFirstByRole(Role.STUDENT).orElse(null));

        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.");
        }
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found.");
        }
        if (inscriptionRepository.existsByStudent_IdUserAndEvent_IdEvent(student.getIdUser(), event.getIdEvent())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already registered for this event.");
        }

        Inscription inscription = new Inscription();
        inscription.setEvent(event);
        inscription.setStudent(student);
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setStatut(InscriptionStatut.EN_ATTENTE);

        inscriptionRepository.save(inscription);
        attachInscriptionCount(event);
    }

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
        if (event.getDateFin().isBefore(LocalDateTime.now()) && !EventStatut.TERMINE.equals(event.getStatut())) {
            event.setStatut(EventStatut.TERMINE);
            eventRepository.save(event);
        }
    }

    private void attachInscriptionCount(Event event) {
        if (event == null || event.getIdEvent() <= 0) return;
        event.setInscriptionsCount(
                inscriptionRepository.countByEvent_IdEventAndStatut(event.getIdEvent(), InscriptionStatut.CONFIRMEE)
        );
    }
}
