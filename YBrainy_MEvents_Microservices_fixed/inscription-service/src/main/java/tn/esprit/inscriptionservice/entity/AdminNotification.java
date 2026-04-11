package tn.esprit.inscriptionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long idNotification;

    LocalDateTime createdAt;

    String type;

    @Column(length = 180)
    String title;

    @Column(length = 600)
    String message;

    long eventId;

    long studentId;
}