package tn.esprit.ybrainy_mevents.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.ybrainy_mevents.Entities.Inscription;
import tn.esprit.ybrainy_mevents.Entities.InscriptionStatut;

import java.util.List;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    List<Inscription> findByStudent_IdUser(long idStudent);

    boolean existsByStudent_IdUserAndEvent_IdEvent(long idStudent, long idEvent);

    long countByEvent_IdEventAndStatut(long idEvent, InscriptionStatut statut);

    List<Inscription> findByStatutOrderByDateInscriptionDesc(InscriptionStatut statut);

    @Query("select distinct i.event.idEvent from Inscription i where i.student.idUser = :idStudent order by i.event.idEvent asc")
    List<Long> findDistinctEventIdsByStudentId(@Param("idStudent") long idStudent);

    List<Inscription> findByStudent_IdUserOrderByDateInscriptionDesc(long idStudent);
}
