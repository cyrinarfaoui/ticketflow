# TicketFlow

Plateforme de réservation et billetterie d'événements — projet de montée en compétences.

## Architecture
Microservices en architecture hexagonale (voir docs/), orchestrés via saga pattern, communication événementielle par Kafka.

## Structure du repo
- `backend/` — microservices Spring Boot (event-service, booking-service, payment-service, notification-service)
- `frontend/` — application Angular
- `infra/` — docker-compose, manifests Kubernetes, ArgoCD
- `docs/` — ADR (Architecture Decision Records), schémas d'archi
