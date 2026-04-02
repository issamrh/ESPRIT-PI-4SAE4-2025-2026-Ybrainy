package tn.esprit.ybrainy_mevents.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Inscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long idInscription;
    LocalDateTime dateInscription;
    @Enumerated(EnumType.STRING)
    InscriptionStatut statut;

    @ManyToOne
    User student;
    @ManyToOne
    Event event;
}
