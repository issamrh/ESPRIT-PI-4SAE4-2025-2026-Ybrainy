package tn.esprit.ybrainy_mevents.Controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.ybrainy_mevents.Entities.Event;
import tn.esprit.ybrainy_mevents.Entities.EventStatut;
import tn.esprit.ybrainy_mevents.Entities.EventType;
import tn.esprit.ybrainy_mevents.Services.IEventServices;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowCredentials = "true"
)
@AllArgsConstructor
@RequestMapping("/Event")
public class EventRestControllers {

    private final IEventServices eventServices;

    @PostMapping("/add")
    public Event addEvent(@RequestBody Event event) {
        return eventServices.addEvent(event);
    }

    @PutMapping("/update")
    public Event updateEvent(@RequestBody Event event) {
        return eventServices.updateEvent(event);
    }

    @GetMapping("/all")
    public List<Event> getAllEvents() {
        return eventServices.getAllEvents();
    }

    @GetMapping("/get/{idEvent}")
    public Event getEventById(@PathVariable("idEvent") long idEvent) {
        return eventServices.getEventById(idEvent);
    }

    @DeleteMapping("/delete/{idEvent}")
    public void deleteEvent(@PathVariable("idEvent") long idEvent) {
        eventServices.deleteEvent(idEvent);
    }

    @PostMapping("/{idEvent}/assign/{idStudent}")
    public void assignStudentToEvent(
            @PathVariable("idEvent") long idEvent,
            @PathVariable("idStudent") long idStudent) {
        eventServices.assignStudentToEvent(idEvent, idStudent);
    }
}
