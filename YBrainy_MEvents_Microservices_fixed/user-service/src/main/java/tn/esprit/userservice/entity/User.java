package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user")
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


    /*
     * NOTE — microservice design:
     * The back-references @OneToMany eventsCrees and @OneToMany inscriptions
     * have been removed. event-service and inscription-service each store
     * the userId as a plain FK column — they own their relationship side.
     */
}
