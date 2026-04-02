package tn.esprit.ybrainy_mevents.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long idEvent;
    String name;
    String description;
    String location;
    int capacite;
    LocalDateTime dateDebut;
    LocalDateTime dateFin;
    LocalDateTime dateCreation;
    @Enumerated(EnumType.STRING)
    EventType type;
    @Enumerated(EnumType.STRING)
    EventStatut statut;
    @Transient
    long inscriptionsCount;

    @ManyToOne(cascade = CascadeType.ALL)
    User admin;
    @JsonIgnore
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    List<Inscription> inscriptions;

}
