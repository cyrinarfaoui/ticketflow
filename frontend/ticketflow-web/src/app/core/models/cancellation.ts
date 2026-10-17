// Réponses de POST /events/{id}/cancel et GET /events/{id}/cancellation (records Java CancellationJob / CancellationReport)
export interface CancellationReport {
  cancelledReservations: number;
  notified: number;       // clients prévenus par e-mail
  failed: number;         // e-mails en échec
  withoutEmail: number;   // anciennes réservations sans e-mail
}

export interface CancellationJob {
  status: 'RUNNING' | 'DONE' | 'FAILED';
  report: CancellationReport | null;
  error: string | null;
}