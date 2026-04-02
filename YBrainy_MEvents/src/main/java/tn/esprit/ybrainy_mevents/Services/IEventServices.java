package tn.esprit.ybrainy_mevents.Services;

import tn.esprit.ybrainy_mevents.Entities.Event;

import java.util.List;

public interface IEventServices {
    Event addEvent(Event event);

    Event updateEvent(Event event);

    Event getEventById(long idEvent);

    List<Event> getAllEvents();

    void deleteEvent(long idEvent);

    void assignStudentToEvent(long idEvent, long idStudent);
}
