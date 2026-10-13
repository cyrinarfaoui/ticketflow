# WebFlux dans notification-service (ticket #5)

## Ce qui a été construit

```
reservation.created (Kafka)
   │  listener : thread Kafka classique → .block() est permis ici (2 s max)
   ▼
NotificationService.handle()  ── Mono<Notification> ──────────────────────────────┐
   │  port EventCatalog → WebClientEventCatalog → GET event-service /events/{id}    │
   │  .timeout(2s)  .map(enrichie)  .switchIfEmpty(simple)  .onErrorResume(simple)  │
   ▼                                                                                │
NotificationBroadcaster (Sinks.many().multicast().directBestEffort())  ◀──publish──┘
   │  Flux<Notification> partagé par tous les abonnés
   ▼
GET /notifications/stream  (text/event-stream, battement de cœur toutes les 15 s)
   │  via la gateway : GET /api/notifications/stream (public, CORS)
   ▼
Navigateurs (EventSource, ticket #8)
```

Exemple réellement reçu :

```
event:reservation
data:{"eventId":"a0aa…","eventTitle":"test","seats":3,"availableSeats":98,"message":"3 place(s) réservée(s) pour « test » — il en reste 98"}
```

event-service arrêté → après 2 s : `"eventTitle":null, "message":"1 place(s) viennent d'être réservée(s)"` (dégradation gracieuse).

## Mesure (Docker, poste local)

| | Threads JVM | Mémoire |
|---|---|---|
| Au repos | 59 | ~280 Mo |
| **1 000 navigateurs connectés au flux** | **59** | ~330 Mo |
| Après diffusion d'une notification à tous | 59 | — |

1 000 connexions établies, 1 000 / 1 000 ont reçu la notification. Le nombre de threads ne bouge pas :
une connexion ouverte n'occupe **aucun thread** pendant qu'elle attend, seulement un peu de mémoire.

## Comparaison avec Spring MVC (à savoir dire honnêtement)

| | Spring MVC (Tomcat) | WebFlux (Netty) |
|---|---|---|
| Modèle | 1 thread par requête (pool ~200) | Boucle d'événements, ~1 thread par cœur |
| Appel HTTP sortant | `RestClient` : le thread attend | `WebClient` : le thread est libéré |
| Flux SSE | `SseEmitter` (servlet asynchrone : le thread est rendu, mais tout le code autour reste impératif) | `Flux` natif : composition, timeout, backpressure intégrés |
| Code bloquant (JPA/JDBC) | Normal | **Interdit** sur les threads Netty (gèle le serveur) |
| Débogage, piles d'appels | Simple | Plus difficile (chaînes d'opérateurs) |
| Courbe d'apprentissage | Faible | Forte |

**Quand WebFlux vaut le coup** : beaucoup de connexions longues ou d'attentes réseau (streaming, SSE, gateway,
agrégation d'appels HTTP), et une chaîne entièrement non bloquante. C'est le cas ici : pas de base de données.

**Quand non** : un CRUD classique sur JPA (event-service, payment-service) — on y perdrait en simplicité sans rien gagner.

**Le nouvel élément du débat : les threads virtuels (Java 21+).** Avec `spring.threads.virtual.enabled=true`,
Spring MVC donne à chaque requête un thread virtuel quasi gratuit : un code bloquant simple tient alors une forte
charge. WebFlux garde l'avantage pour le *streaming* et la composition d'appels (opérateurs, backpressure). À tester dans le ticket #17.

## Points d'attention retenus

- **Rien ne s'exécute sans abonnement** : `handle()` renvoie une recette ; le listener l'exécute avec `.block()`.
- **`.block()` uniquement hors des threads Netty** (ici, le thread du listener Kafka).
- **404 ≠ panne** : traduit en `Mono.empty()` par l'adaptateur ; un 5xx reste une erreur, rattrapée par le service.
- **Spring Boot 4** : `WebClient.Builder` nécessite `spring-boot-starter-webclient` (module séparé de `starter-webflux`).
- **WebFlux n'envoie les en-têtes HTTP qu'avec le premier élément** du flux (vu en test et en mesure de charge).
- **`directBestEffort`** : un abonné lent perd des notifications au lieu de ralentir les autres (backpressure par abonné).
- **Flux public sans données personnelles** : `EventSource` ne sait pas envoyer d'en-tête `Authorization`.
  Pour des notifications personnelles, il faudrait un cookie de session, un jeton à usage unique, ou `fetch()` en streaming.
