# TicketFlow

Plateforme de réservation de billets d'événements, en microservices.

[![CI](https://github.com/cyrinarfaoui/ticketflow/actions/workflows/ci.yml/badge.svg)](https://github.com/cyrinarfaoui/ticketflow/actions/workflows/ci.yml)

## Stack

- **Backend** : Java 25, Spring Boot 4, PostgreSQL, Kafka
- **Frontend** : Angular 22
- **Sécurité** : Keycloak (OpenID Connect)
- **Outillage** : Docker, GitHub Actions, SonarCloud

## Démarrer

Prérequis : Docker Desktop.

```bash
cd infra
docker compose up -d --build
```

| URL | |
|---|---|
| http://localhost:4200 | Application web |
| http://localhost:8000/api | API (gateway) |
| http://localhost:8081 | Keycloak |

## Tests

```bash
# Backend, dans backend/<service> (conteneurs PostgreSQL démarrés)
SPRING_DATASOURCE_PASSWORD=<mot de passe> ./mvnw verify

# Frontend, dans frontend/ticketflow-web
npm ci
npx ng test --watch=false --browsers=ChromeHeadless
```

## Documentation

- [Saga de paiement](docs/saga-paiement.md)
- [WebFlux](docs/webflux.md)
