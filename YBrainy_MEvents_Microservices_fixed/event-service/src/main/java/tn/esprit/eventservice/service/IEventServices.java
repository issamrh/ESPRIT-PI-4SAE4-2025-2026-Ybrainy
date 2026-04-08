package tn.esprit.eventservice.service;

import tn.esprit.eventservice.entity.Event;

import java.util.List;

public interface IEventServices {
    Event addEvent(Event event);
    Event updateEvent(Event event);
    String generateDescription(String name, String type);
    Event getEventById(long idEvent);
    List<Event> getAllEvents();
    void deleteEvent(long idEvent);
    void assignStudentToEvent(long idEvent, long idStudent);
}
