# TicketFlow

Plateforme de réservation et de billetterie d'événements, en microservices — projet de montée en compétences full-stack.

[![pipeline](https://gitlab.com/cyrine-projects/ticketflow/badges/main/pipeline.svg)](https://gitlab.com/cyrine-projects/ticketflow/-/pipelines)

## Fonctionnalités

- **Catalogue public** : recherche et filtres (titre, lieu, catégorie, dates, places disponibles), page d'accueil avec événements à la une.
- **Réservation avec paiement** : prix par place, paiement « façon Stripe » par jeton (`pm_…`, jamais de numéro de carte), refus expliqué à l'utilisateur (fonds insuffisants, carte expirée…).
- **Espace « Mes réservations »** : suivi du statut (en attente, confirmée, annulée, expirée), annulation.
- **Dashboard administrateur** : indicateurs, gestion des événements (création, clôture), liste paginée des réservations.
- **Notifications en direct** : flux Server-Sent Events alimenté par Kafka.
- **Expiration automatique** des réservations restées en attente (Spring Batch planifié).

## Architecture

```
                    Angular 22 (signals, zoneless)  ◀── SSE ──┐
                              │ HTTPS + JWT (Keycloak)         │
                              ▼                                │
                      API Gateway :8000  (Spring Cloud Gateway, WebFlux)
                 JWT · CORS · X-Request-Id │                   │
        ┌──────────────────┬───────────────┼───────────────────┤
        ▼                  ▼               ▼                   │
  auth-service       event-service   notification-service ─────┘
     :8082              :8080            :8083 (WebFlux)
                     PostgreSQL           ▲   │ WebClient (détails de l'événement)
                        │                 │   └──────────────▶ event-service
                        │  reservation.created
                        ▼                 │
                  ┌──────────── Kafka ────┴──────────┐
                  │                                  │
                  ▼  reservation.created             │ payment.completed / payment.failed
            payment-service :8084  ──────────────────┘
               PostgreSQL (sa propre base)
```

- **Architecture hexagonale** dans chaque service : `domain` (règles métier pures) · `application` (cas d'usage + ports) · `infrastructure` (adaptateurs web, JPA, Kafka, HTTP).
- **Une base de données par service** (event-service, payment-service), schémas versionnés avec **Liquibase**.
- **Saga chorégraphiée** pour la réservation payante : chaque service réagit aux événements Kafka, avec **compensation** (annulation + places rendues) si le paiement échoue.
- **Sécurité** : Keycloak (OpenID Connect), validation du JWT à la gateway **et** dans les services (défense en profondeur), rôles `ADMIN` / utilisateur.

## Services

| Service | Port | Rôle | Stack |
|---|---|---|---|
| `api-gateway` | 8000 | Point d'entrée unique : routage, vérification JWT, CORS, traçage `X-Request-Id` | Spring Cloud Gateway (WebFlux) |
| `auth-service` | 8082 | Profil de l'utilisateur connecté (`/auth/me`) | Spring MVC, Resource Server |
| `event-service` | 8080 | Événements, réservations, statistiques, expiration (batch) | Spring MVC, JPA, PostgreSQL, Kafka, Spring Batch |
| `payment-service` | 8084 | Paiement idempotent des réservations (fournisseur simulé « façon Stripe ») | Spring MVC, JPA, PostgreSQL, Kafka |
| `notification-service` | 8083 | Notifications enrichies, diffusées en direct (SSE) | **WebFlux**, WebClient, Reactor, Kafka |
| `ticketflow-web` | 4200 | Application web | Angular 22, signals, Keycloak |

## Stack technique

- **Backend** : Java 25, Spring Boot 4.1, Spring Cloud Gateway, Spring Data JPA, Spring Batch, Spring Kafka, Reactor / WebFlux, Liquibase, PostgreSQL 16.
- **Frontend** : Angular 22 (zoneless, signals, `httpResource`, formulaires réactifs), RxJS, accessibilité **RGAA**.
- **Messaging** : Apache Kafka.
- **Sécurité** : Keycloak 26, OAuth2 / OpenID Connect, JWT.
- **Tests** : JUnit 5, Mockito, AssertJ, Reactor `StepVerifier`, tests d'intégration sur PostgreSQL réel, Jasmine / Karma.
- **CI / qualité** : GitLab CI, SonarCloud (Quality Gate bloquante, 80 % de couverture sur le nouveau code), JaCoCo, images Docker publiées dans le GitLab Container Registry.

## Choix techniques marquants

| Sujet | Choix | Doc |
|---|---|---|
| Cohérence entre services | Saga chorégraphiée + compensation, consommateurs **idempotents** (Kafka = livraison *at-least-once*) | [docs/saga-paiement.md](docs/saga-paiement.md) |
| Surréservation | Verrou pessimiste (`SELECT … FOR UPDATE`), prouvé par un test de 50 threads concurrents | — |
| Paiement | Jetons `pm_…` uniquement (hors périmètre PCI-DSS), montants en centimes (`long`, jamais de `double`), refus *fail closed* | [docs/saga-paiement.md](docs/saga-paiement.md) |
| Programmation réactive | WebFlux là où il y a des connexions longues (gateway, flux SSE), MVC + JPA ailleurs ; 1 000 clients SSE sans thread supplémentaire | [docs/webflux.md](docs/webflux.md) |
| Résilience | `timeout` + plan B (`onErrorResume`) : une panne d'event-service dégrade la notification au lieu de la perdre | [docs/webflux.md](docs/webflux.md) |
| Limite connue | Publication Kafka dans la transaction → pattern **Outbox** à mettre en place | [docs/saga-paiement.md](docs/saga-paiement.md) |

## Démarrer en local

Prérequis : Docker Desktop.

```bash
cd infra
docker compose up -d --build
```

| URL | Quoi |
|---|---|
| http://localhost:4200 | Application web |
| http://localhost:8000/api | API (via la gateway) |
| http://localhost:8081 | Console Keycloak |
| http://localhost:8000/api/notifications/stream | Flux SSE des notifications |

Après une modification d'un service, reconstruire son image : `docker compose up -d --build <service>`.

Les tests d'un service qui utilise PostgreSQL attendent le mot de passe dans une variable d'environnement (jamais dans le code) :

```bash
SPRING_DATASOURCE_PASSWORD=ticketflow ./mvnw verify      # dans backend/<service>, avec les conteneurs PostgreSQL démarrés
```

Frontend : `npm ci` puis `npx ng test --watch=false --browsers=ChromeHeadless` dans `frontend/ticketflow-web`.

## Pipeline CI/CD (GitLab)

`build` → `test` → `quality` → `package`

- **build** : compilation de chaque service (matrice parallèle) et du frontend.
- **test** : tests unitaires et d'intégration (PostgreSQL en service CI), rapports JUnit et couverture.
- **quality** : analyse SonarCloud, Quality Gate **bloquante** pour la Merge Request.
- **package** : images Docker construites sur chaque MR, publiées (tag SHA + `latest`) sur `main`.

Workflow : une branche et une Merge Request par ticket (`Closes #N`), merge après pipeline verte.

## Structure du dépôt

```
backend/
  api-gateway/           Spring Cloud Gateway
  auth-service/
  event-service/         événements, réservations, batch d'expiration
  payment-service/       paiements (saga)
  notification-service/  WebFlux + SSE
frontend/
  ticketflow-web/        Angular
infra/
  docker-compose.yml     environnement local complet
docs/                    choix d'architecture (saga, WebFlux…)
.gitlab-ci.yml           pipeline CI/CD
sonar-project.properties
```

## Feuille de route

- [ ] **Notifications en direct dans l'UI** : `EventSource`, badge live, reconnexion automatique (ticket #8).
- [ ] **Threads virtuels** : annulation d'un événement et prévenance des clients en parallèle (ticket #17), comparaison avec WebFlux.
- [ ] **Pattern Outbox** : publication Kafka fiable, atomique avec la transaction SQL.
- [ ] **Kubernetes** : manifests (Deployments, Services, ConfigMaps, Secrets), sondes `liveness` / `readiness` (Actuator), Ingress, `infra/k8s/`.
- [ ] **ArgoCD** : déploiement GitOps — le cluster se synchronise automatiquement sur l'état décrit dans le dépôt.
- [ ] **Observabilité** : métriques et traces distribuées (Micrometer, OpenTelemetry), en s'appuyant sur `X-Request-Id`.
