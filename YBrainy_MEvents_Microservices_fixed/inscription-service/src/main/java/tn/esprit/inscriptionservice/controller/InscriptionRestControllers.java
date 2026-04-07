package tn.esprit.inscriptionservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.inscriptionservice.client.EventClient;
import tn.esprit.inscriptionservice.client.UserClient;
import tn.esprit.inscriptionservice.dto.EventDto;
import tn.esprit.inscriptionservice.dto.InscriptionCreateDto;
import tn.esprit.inscriptionservice.entity.Inscription;
import tn.esprit.inscriptionservice.entity.InscriptionStatut;
import tn.esprit.inscriptionservice.service.IInscriptionServices;

import java.util.List;

@RestController
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowCredentials = "true"
)
@AllArgsConstructor
@RequestMapping("/Inscription")
public class InscriptionRestControllers {

    private final IInscriptionServices inscriptionServices;
    private final UserClient userClient;
    private final EventClient eventClient;

    // ── Endpoints consumed by event-service (Feign) ─────────────────────────

    /** Called by event-service to create an inscription. */
    @PostMapping
    public Inscription createInscription(@RequestBody InscriptionCreateDto dto) {
        return inscriptionServices.createInscription(dto.eventId(), dto.studentId());
    }

    /** Called by event-service to guard against duplicate registrations. */
    @GetMapping("/exists")
    public boolean existsByStudentIdAndEventId(
            @RequestParam("studentId") long studentId,
            @RequestParam("eventId") long eventId) {
        return inscriptionServices.existsByStudentAndEvent(studentId, eventId);
    }

    /** Called by event-service to attach inscription count to an event. */
    @GetMapping("/count")
    public long countConfirmedByEventId(@RequestParam("eventId") long eventId) {
        return inscriptionServices.countConfirmedByEvent(eventId);
    }

    // ── Public endpoints ─────────────────────────────────────────────────────

    /**
     * Returns student IDs — proxies to user-service.
     * Falls back to all user IDs if no STUDENT-role users exist.
     */
    @GetMapping("/students-ids")
    public List<Long> getStudentIds() {
        List<Long> students = userClient.findIdsByRole("STUDENT");
        if (students != null && !students.isEmpty()) {
            return students;
        }
        return userClient.findAllIds();
    }

    /** Event IDs the given student is registered for. */
    @GetMapping("/student/{idStudent}/event-ids")
    public List<Long> getRegisteredEventIdsByStudent(
            @PathVariable("idStudent") long idStudent) {
        return inscriptionServices.getDistinctEventIdsByStudent(idStudent);
    }

    /** Statuses of each event the student is registered for. */
    @GetMapping("/student/{idStudent}/event-statuses")
    public List<StudentEventStatusDto> getEventStatusesByStudent(
            @PathVariable("idStudent") long idStudent) {
        return inscriptionServices.getEventStatusesByStudent(idStudent).stream()
                .map(i -> new StudentEventStatusDto(
                        i.getEventId(),
                        i.getStatut() != null ? i.getStatut().name() : ""
                ))
                .filter(dto -> dto.idEvent() > 0)
                .toList();
    }

    /** All inscriptions in EN_ATTENTE status, enriched with event name via Feign. */
    @GetMapping("/pending")
    public List<PendingInscriptionDto> getPendingInscriptions() {
        return inscriptionServices.getPendingInscriptions().stream()
                .map(i -> {
                    String eventName = eventClient.findById(i.getEventId())
                            .map(EventDto::name)
                            .orElse("");
                    return new PendingInscriptionDto(
                            i.getIdInscription(),
                            i.getStudentId(),
                            i.getEventId(),
                            eventName,
                            i.getDateInscription() != null ? i.getDateInscription().toString() : ""
                    );
                })
                .filter(dto -> dto.idInscription() > 0 && dto.idEvent() > 0 && dto.idStudent() > 0)
                .toList();
    }

    /** Update inscription status to CONFIRMEE or ANNULEE. */
    @PutMapping("/{idInscription}/status/{status}")
    public void updateInscriptionStatus(
            @PathVariable("idInscription") long idInscription,
            @PathVariable("status") String status) {
        InscriptionStatut targetStatus;
        try {
            targetStatus = InscriptionStatut.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid inscription status.");
        }
        inscriptionServices.updateStatus(idInscription, targetStatus);
    }

    // ── Response DTOs ────────────────────────────────────────────────────────

    public record PendingInscriptionDto(
            long idInscription,
            long idStudent,
            long idEvent,
            String eventName,
            String dateInscription
    ) {}

    public record StudentEventStatusDto(
            long idEvent,
            String statut
    ) {}
}
