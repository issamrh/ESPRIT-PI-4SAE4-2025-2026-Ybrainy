package tn.esprit.ybrainy_mevents.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long idUser;
    String nom;
    String prenom;
    String email;

    String password;

    @Enumerated(EnumType.STRING)
    Role role;

    @OneToMany(mappedBy = "admin")
    List<Event> eventsCrees;

    @OneToMany(mappedBy = "student")
    List<Inscription> inscriptions;
}
