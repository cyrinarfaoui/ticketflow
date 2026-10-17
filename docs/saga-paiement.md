# Saga de paiement (ticket #2)

## Le problème

Une réservation touche **deux microservices, deux bases** : event-service (places) et payment-service (paiements).
Une transaction SQL ne peut pas couvrir les deux bases. On utilise donc une **saga** : une suite de transactions
locales, chacune avec son **action de compensation** si une étape suivante échoue.

## Le déroulé (saga *chorégraphiée* : pas de chef d'orchestre, chacun réagit aux événements Kafka)

```
Utilisateur ──POST /reservations {seats, paymentMethod: "pm_…"}──▶ event-service
   1. verrouille l'événement (SELECT … FOR UPDATE), retire les places, réservation PENDING
   2. publie reservation.created (v2 : montant = places × prix, devise, jeton)
                                     │
                                     ▼
                              payment-service
   3. idempotence : déjà un paiement pour cette réservation ? → on republie le résultat, c'est tout
   4. règles marchand (montant 0, plafond 2 000 TND) puis "banque" (FakeStripeProvider)
   5. enregistre le paiement (UNIQUE reservation_id) et publie payment.completed OU payment.failed
                                     │
                                     ▼
                              event-service
   6a. payment.completed → réservation CONFIRMED
   6b. payment.failed    → COMPENSATION : réservation CANCELLED + motif, places rendues à l'événement
```

## Choix techniques

| Choix | Pourquoi |
|---|---|
| Chorégraphie (et pas orchestration) | 2 services seulement : simple, faible couplage. Au-delà de 4–5 étapes, un orchestrateur devient plus lisible. |
| Jeton `pm_…`, jamais de numéro de carte | Comme Stripe/PayPal : la carte va directement chez le prestataire. Notre backend reste hors du périmètre PCI-DSS. `@Pattern` refuse tout le reste. |
| Montants en `long` centimes | `0.1 + 0.2 != 0.3` en `double`. Jamais de float pour de l'argent. |
| Codes de refus Stripe (`card_declined`, `insufficient_funds`…) | Vocabulaire standard, affichable à l'utilisateur, prêt pour un vrai prestataire. |
| Jeton inconnu → refusé (`invalid_payment_method`) | *Fail closed* : en cas de doute, on n'encaisse pas. |
| Port `PaymentProvider` + adaptateur `FakeStripeProvider` | Architecture hexagonale : un vrai client HTTP (WebClient, ticket #5) remplacera le faux sans toucher au métier. |
| Clé Kafka = `reservationId` | Tous les messages d'une réservation vont dans la même partition → traités dans l'ordre. |
| Une base par service (paymentdb) | Autonomie : payment-service ne lit jamais la base d'event-service. |

## Idempotence : pourquoi et comment

Kafka garantit *at-least-once* : après un crash ou un rééquilibrage, un message peut être **relivré**.
Sans protection, on débiterait deux fois ou on rendrait les places deux fois.

- payment-service : contrainte `UNIQUE (reservation_id)` + lecture préalable → un rejeu republie le résultat existant.
- event-service : `confirmPayment` / `rejectPayment` ne font rien si la réservation n'est plus `PENDING`.

Vérifié de bout en bout : un rejeu des mêmes messages laisse les places et le nombre de paiements inchangés.

## Limite connue : le pattern Outbox (à étudier / implémenter)

Aujourd'hui, on publie sur Kafka **dans** la transaction, avant le commit SQL. Deux pannes possibles :

1. Kafka reçoit le message, puis le commit SQL échoue → message "fantôme" (paiement d'une réservation qui n'existe pas).
2. Le commit réussit, puis l'envoi Kafka échoue → la saga reste bloquée en `PENDING`.

**Outbox** : dans la MÊME transaction que la réservation, on insère le message dans une table `outbox`.
Un processus séparé (poller planifié, ou Debezium qui lit le journal de Postgres) publie ensuite les lignes
de `outbox` vers Kafka et les marque envoyées. Base et message deviennent atomiques ; la livraison reste
*at-least-once*, d'où l'importance de l'idempotence côté consommateurs (déjà en place).

Filet de sécurité complémentaire : l'expiration des réservations `PENDING` trop anciennes
libère les places si un message se perd (voir ci-dessous).

## Expiration des réservations sans réponse de paiement

Une réservation reste `PENDING` une ou deux secondes : le temps de l'aller-retour Kafka.
Si la réponse n'arrive jamais (payment-service en panne, message perdu), un batch Spring planifié
(toutes les 5 min) fait expirer les réservations `PENDING` de plus de 5 minutes :

```
Reader  : réservations PENDING créées avant (maintenant - 5 min)
Writer  : ReservationService.expireReservation(id), pour chacune, par paquets de 20 (une transaction par paquet)
          → réservation EXPIRED + places rendues à l'événement (verrou SELECT … FOR UPDATE)
```

- **Même règle que la compensation** : expirer, c'est défaire l'étape 1 de la saga, donc rendre les places.
  (Bug corrigé : la première version expirait la réservation sans rendre les places ; la migration Liquibase
  `013` a recalculé les places des événements touchés.)
- **Course avec la banque** : si la réponse arrive entre la lecture du batch et le traitement, la réservation
  n'est plus `PENDING` → `expireReservation` ne fait rien (idempotence), sans faire échouer le paquet.

### Limite connue : paiement accepté APRÈS l'expiration

Si payment-service débite le client puis que sa réponse arrive après l'expiration, `confirmPayment` trouve
une réservation `EXPIRED` et ne fait rien : **le client a payé mais n'a pas de place** (elles ont pu être revendues).

Ce qu'il faut faire (non implémenté) : déclencher un **remboursement automatique**. event-service publierait
`reservation.expired` ; payment-service, s'il trouve un paiement `SUCCEEDED` pour cette réservation, appellerait
le remboursement du prestataire (`refunds` chez Stripe) et passerait le paiement à `REFUNDED`, en restant
idempotent (un seul remboursement par paiement). Le client serait prévenu par e-mail.
En attendant, ce cas est rare (il faut une panne de plus de 5 minutes) et se repère dans les logs d'event-service
(« Paiement confirmé pour la réservation … déjà EXPIRED »).
