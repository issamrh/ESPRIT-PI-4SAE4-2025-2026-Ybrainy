package tn.esprit.ybrainy_mevents.Controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.ybrainy_mevents.Entities.Inscription;
import tn.esprit.ybrainy_mevents.Entities.InscriptionStatut;
import tn.esprit.ybrainy_mevents.Entities.Role;
import tn.esprit.ybrainy_mevents.Repositories.InscriptionRepository;
import tn.esprit.ybrainy_mevents.Repositories.UserRepository;

import java.util.List;

@RestController
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowCredentials = "true"
)
@AllArgsConstructor
@RequestMapping("/Inscription")
public class InscriptionRestControllers {

    private final UserRepository userRepository;
    private final InscriptionRepository inscriptionRepository;

    @GetMapping("/students-ids")
    public List<Long> getStudentIds() {
        List<Long> students = userRepository.findIdsByRole(Role.STUDENT);
        if (students != null && !students.isEmpty()) {
            return students;
        }
        return userRepository.findAllIds();
    }

    @GetMapping("/student/{idStudent}/event-ids")
    public List<Long> getRegisteredEventIdsByStudent(@PathVariable("idStudent") long idStudent) {
        return inscriptionRepository.findDistinctEventIdsByStudentId(idStudent);
    }

    @GetMapping("/student/{idStudent}/event-statuses")
    public List<StudentEventStatusDto> getEventStatusesByStudent(@PathVariable("idStudent") long idStudent) {
        return inscriptionRepository.findByStudent_IdUserOrderByDateInscriptionDesc(idStudent).stream()
                .map(inscription -> new StudentEventStatusDto(
                        inscription.getEvent() != null ? inscription.getEvent().getIdEvent() : 0L,
                        inscription.getStatut() != null ? inscription.getStatut().name() : ""
                ))
                .filter(dto -> dto.idEvent() > 0)
                .toList();
    }

    @GetMapping("/pending")
    public List<PendingInscriptionDto> getPendingInscriptions() {
        return inscriptionRepository.findByStatutOrderByDateInscriptionDesc(InscriptionStatut.EN_ATTENTE).stream()
                .map(inscription -> new PendingInscriptionDto(
                        inscription.getIdInscription(),
                        inscription.getStudent() != null ? inscription.getStudent().getIdUser() : 0L,
                        inscription.getEvent() != null ? inscription.getEvent().getIdEvent() : 0L,
                        inscription.getEvent() != null ? inscription.getEvent().getName() : "",
                        inscription.getDateInscription() != null ? inscription.getDateInscription().toString() : ""
                ))
                .filter(dto -> dto.idInscription() > 0 && dto.idEvent() > 0 && dto.idStudent() > 0)
                .toList();
    }

    @PutMapping("/{idInscription}/status/{status}")
    public void updateInscriptionStatus(
            @PathVariable("idInscription") long idInscription,
            @PathVariable("status") String status
    ) {
        Inscription inscription = inscriptionRepository.findById(idInscription).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription not found.")
        );

        InscriptionStatut targetStatus;
        try {
            targetStatus = InscriptionStatut.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid inscription status.");
        }

        if (targetStatus != InscriptionStatut.CONFIRMEE && targetStatus != InscriptionStatut.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be CONFIRMEE or ANNULEE.");
        }

        inscription.setStatut(targetStatus);
        inscriptionRepository.save(inscription);
    }

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
