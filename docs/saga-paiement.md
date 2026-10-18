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

## Publication fiable : le pattern Outbox

**Le problème (« dual write »)** : la réservation va dans PostgreSQL, le message dans Kafka. Aucune transaction
ne couvre les deux systèmes. Publier directement sur Kafka laissait deux pannes possibles :

1. Kafka reçoit le message, puis le commit SQL échoue → message "fantôme" (paiement d'une réservation qui n'existe pas).
2. Le commit réussit, puis l'envoi Kafka échoue → la saga reste bloquée en `PENDING`.

**La solution** :

```
createReservation (@Transactional)
   INSERT reservations     ─┐  même base, même transaction : tout ou rien
   INSERT outbox_events    ─┘  (OutboxReservationEventPublisher, propagation MANDATORY)

OutboxRelay (@Scheduled, toutes les secondes)
   SELECT 100 plus anciens messages WHERE published_at IS NULL
   pour chacun : send Kafka, ATTENDRE l'accusé de réception, puis published_at = maintenant
   Kafka en panne → on s'arrête (ordre conservé), nouvel essai 1 s plus tard
```

- **Atomicité** : réservation et message sont enregistrés ensemble, ou pas du tout.
- **Aucune perte** : si Kafka est en panne, les messages attendent dans la table et partent à son retour.
- **Doublons possibles** (*at-least-once*) : un crash entre l'envoi et le marquage renvoie le message.
  Sans conséquence, les consommateurs sont idempotents (voir ci-dessus).
- **Limites assumées** : prévu pour une seule instance d'event-service (avec plusieurs, il faudrait
  `SELECT … FOR UPDATE SKIP LOCKED` pour que deux relais ne prennent pas les mêmes messages) ; les messages
  envoyés restent dans la table (à purger périodiquement). Alternative sans polling : Debezium, qui lit le
  journal de PostgreSQL (CDC).

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

## Remboursement automatique

**Règle : une place payée qui n'est plus valable est remboursée.** Trois cas :

| Cas | Où c'est détecté |
|---|---|
| Paiement accepté APRÈS l'expiration ou l'annulation de la réservation (ex. Kafka en panne > 5 min) | `ReservationService.confirmPayment` : la réservation n'est plus `PENDING` ni `CONFIRMED` |
| Le client (ou l'admin) annule une réservation déjà payée | `ReservationService.cancelReservation` |
| L'admin annule un événement qui a des réservations payées | `EventCancellationService.cancelEvent` |

```
event-service : refund.requested écrit dans l'outbox (même transaction que l'annulation)
      │ relais → Kafka
      ▼
payment-service : paiement SUCCEEDED ? → 1. enregistre REFUNDED  2. appelle le prestataire (remboursement)
                  déjà REFUNDED, refusé ou absent → rien à faire (idempotent)
```

- **C'est event-service qui décide** : lui seul sait, au moment où le paiement arrive, que la réservation
  n'est plus valable. (Un message `reservation.expired` envoyé à payment-service pourrait arriver AVANT
  `reservation.created` : deux topics différents ne garantissent aucun ordre.)
- **Enregistrer PUIS rembourser** : si l'écriture en base échoue, rien n'a été rendu et Kafka réessaie.
  L'ordre inverse remboursait à chaque nouvel essai (bug vu en test : 10 remboursements pour un paiement).
- **Clé d'idempotence** (id du paiement) transmise au prestataire : si le commit échoue après le remboursement,
  le rejeu ne rembourse pas une 2e fois (comme l'en-tête `Idempotency-Key` de Stripe).
- Une fois le remboursement fait, payment-service publie `payment.refunded` : event-service envoie au client
  l'e-mail « Remboursement effectué ». Pas encore fait : afficher « Remboursée » dans « Mes réservations ».

## E-mails au client

| Moment | E-mail | Déclencheur |
|---|---|---|
| Paiement accepté | **Billet** (HTML) avec **un QR code par place** | `ReservationConfirmed`, écouté APRÈS le commit (`@TransactionalEventListener(AFTER_COMMIT)`) |
| Remboursement effectué | « Remboursement effectué », avec le montant | message Kafka `payment.refunded` |
| Événement annulé | « Annulation » | `EventCancellationService` (threads virtuels) |

- **Après le commit** : un billet envoyé pendant la transaction pourrait partir pour une réservation finalement
  annulée (rollback), et l'appel SMTP, lent, garderait la transaction et ses verrous ouverts.
- **QR code** : `TICKETFLOW-<id de la réservation>-<n° de place>`, dessiné en PNG avec ZXing et intégré au
  message (`cid:`). L'id est un UUID aléatoire, donc impossible à deviner ; pour aller plus loin, on signerait
  le code (HMAC) et on ajouterait un écran de contrôle à l'entrée qui marque chaque place « utilisée ».
- **Un e-mail qui échoue ne fait jamais échouer** la réservation ou le remboursement : il est journalisé.
  (Limite : il n'est pas renvoyé automatiquement.)
