export interface Event {
    idEvent: number;
    name: string;
    description: string;
    location: string;
    capacite: number;
    dateDebut: string; // ISO String format
    dateFin: string; // ISO String format
    dateCreation: string; // ISO String format
    type: string; // EventType enum string value
    statut: string; // EventStatut enum string value
}

export interface Inscription {
    idInscription: number;
    dateInscription: string;
    statut: string; // InscriptionStatut enum string value
    student: any; // Or User interface if available
    event: Event;
}

export enum EventType {
    WEBINAIRE = 'WEBINAIRE',
    FORMATION = 'FORMATION',
    ATELIER = 'ATELIER',
    HACKATHON = 'HACKATHON'
}

export enum EventStatut {
    UPCOMING = 'UPCOMING',
    ONGOING = 'ONGOING',
    COMPLETED = 'COMPLETED',
    CANCELLED = 'CANCELLED'
    // Note: Values should match the Java enum EventStatut
}

export enum InscriptionStatut {
    PENDING = 'PENDING',
    CONFIRMED = 'CONFIRMED',
    CANCELLED = 'CANCELLED'
    // Note: Values should match the Java enum InscriptionStatut
}
