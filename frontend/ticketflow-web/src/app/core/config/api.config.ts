// Point d'entrée unique : l'API Gateway aiguille vers le bon microservice
// (/api/events et /api/reservations → event-service, /api/auth → auth-service)
export const API_BASE_URL = 'http://localhost:8000/api';