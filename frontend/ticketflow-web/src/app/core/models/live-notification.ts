// Ce que le serveur envoie sur GET /api/notifications/stream (record Notification.java de notification-service).
// Flux PUBLIC : aucune donnée personnelle.
export interface LiveNotification {
  eventId: string;
  eventTitle: string | null;        // null si event-service n'a pas répondu (notification "simple")
  seats: number;                    // places qui viennent d'être réservées
  availableSeats: number | null;    // places restantes après la réservation (null = inconnu)
  message: string;                  // texte prêt à afficher
}

// État de la connexion au flux, affiché discrètement dans l'en-tête
export type StreamStatus = 'connecting' | 'open' | 'reconnecting';
