package tn.esprit.inscriptionservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.inscriptionservice.entity.Inscription;
import tn.esprit.inscriptionservice.client.EventClient;
import tn.esprit.inscriptionservice.client.UserClient;
import tn.esprit.inscriptionservice.dto.EventDto;
import tn.esprit.inscriptionservice.dto.UserDto;
import tn.esprit.inscriptionservice.entity.InscriptionStatut;
import tn.esprit.inscriptionservice.repository.InscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class InscriptionServicesImp implements IInscriptionServices {

    private final InscriptionRepository inscriptionRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final InscriptionNotificationEmailService notificationEmailService;

    @Override
    public Inscription createInscription(long eventId, long studentId) {
        if (inscriptionRepository.existsByStudentIdAndEventId(studentId, eventId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student already registered for this event.");
        }
        Inscription inscription = new Inscription();
        inscription.setEventId(eventId);
        inscription.setStudentId(studentId);
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setStatut(InscriptionStatut.EN_ATTENTE);
        return inscriptionRepository.save(inscription);
    }

    @Override
    public void updateStatus(long idInscription, InscriptionStatut status) {
        Inscription inscription = inscriptionRepository.findById(idInscription)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inscription not found."));

        if (status != InscriptionStatut.CONFIRMEE && status != InscriptionStatut.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be CONFIRMEE or ANNULEE.");
        }
        inscription.setStatut(status);
        inscriptionRepository.save(inscription);

        trySendDecisionEmail(inscription, status);
    }

    @Override
    public List<Inscription> getPendingInscriptions() {
        return inscriptionRepository.findByStatutOrderByDateInscriptionDesc(
                InscriptionStatut.EN_ATTENTE);
    }

    @Override
    public List<Long> getDistinctEventIdsByStudent(long studentId) {
        return inscriptionRepository.findDistinctEventIdsByStudentId(studentId);
    }

    @Override
    public List<Inscription> getEventStatusesByStudent(long studentId) {
        return inscriptionRepository.findByStudentIdOrderByDateInscriptionDesc(studentId);
    }

    @Override
    public boolean existsByStudentAndEvent(long studentId, long eventId) {
        return inscriptionRepository.existsByStudentIdAndEventId(studentId, eventId);
    }

    @Override
    public long countConfirmedByEvent(long eventId) {
        return inscriptionRepository.countByEventIdAndStatut(eventId, InscriptionStatut.CONFIRMEE);
    }

    private void trySendDecisionEmail(Inscription inscription, InscriptionStatut status) {
        UserDto student = userClient.findById(inscription.getStudentId()).orElse(null);
        EventDto event = eventClient.findById(inscription.getEventId()).orElse(null);

        if (student == null) {
            log.warn("Skipping inscription decision email because student {} was not found.", inscription.getStudentId());
            return;
        }
        if (event == null) {
            log.warn("Skipping inscription decision email because event {} was not found.", inscription.getEventId());
            return;
        }

        notificationEmailService.sendDecisionEmail(student, event, inscription, status);
    }
}
