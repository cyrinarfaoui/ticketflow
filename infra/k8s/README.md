# Déploiement Kubernetes

Toute l'application TicketFlow (5 microservices, frontend, 2 PostgreSQL, Kafka, Keycloak, Mailpit,
Prometheus, Grafana) dans le namespace `ticketflow`, avec les mêmes adresses qu'avec Docker Compose.
Cluster local : Kubernetes de Docker Desktop (type kubeadm, 1 nœud).

## Prérequis

1. Docker Desktop → Settings → Kubernetes → Enable Kubernetes (kubeadm).
2. Docker Compose **arrêté** : `docker compose -f infra/docker-compose.yml stop` (mêmes ports).
3. Images construites :
   ```bash
   docker compose -f infra/docker-compose.yml build              # les 5 services
   docker build -t ticketflow-web:prod frontend/ticketflow-web     # frontend de production (nginx)
   ```
4. Export du realm Keycloak dans `infra/k8s/keycloak/ticketflow-realm.json` (**ignoré par Git** :
   mots de passe hachés et secrets des clients), depuis le volume de Docker Compose :
   ```bash
   MSYS_NO_PATHCONV=1 docker run --rm -v infra_keycloak_data:/opt/keycloak/data \
     -v "$(pwd -W)/infra/k8s/keycloak:/tmp/export" quay.io/keycloak/keycloak:26.0 \
     export --dir /tmp/export --realm ticketflow --users realm_file
   ```

## Déployer

```bash
bash infra/k8s/deploy.sh             # demande les mots de passe la 1re fois, puis applique tout
kubectl get pods -n ticketflow -w    # attendre 1/1 partout
```

| URL | |
|---|---|
| http://ticketflow.localhost | Application (port 80 ; `*.localhost` est résolu par le navigateur) |
| http://localhost:8000/api | API (gateway) |
| http://localhost:8081 | Keycloak |
| http://localhost:8025 | Mailpit |
| http://localhost:9090 · http://localhost:3000 | Prometheus · Grafana |

## Objets utilisés

| Objet | Pour |
|---|---|
| Deployment | les composants sans état : microservices, frontend, Kafka, Keycloak… |
| StatefulSet + PersistentVolumeClaim | les 2 PostgreSQL : nom stable, disque conservé |
| Service (ClusterIP / LoadBalancer) | adresse stable interne / publication sur localhost |
| ConfigMap | configuration de chaque service ; fichiers Prometheus et Grafana |
| Secret | mots de passe et realm Keycloak, créés par commande, jamais dans Git |
| Sondes startup / liveness / readiness | Actuator : démarrage, redémarrage automatique, trafic seulement si prêt |

## Démonstrations

```bash
kubectl delete pod -n ticketflow -l app=event-service                 # auto-réparation
kubectl scale deployment event-service -n ticketflow --replicas=3     # mise à l'échelle
kubectl rollout restart deployment event-service -n ticketflow        # mise à jour sans coupure
kubectl logs -n ticketflow deploy/event-service -f                    # journaux
```

`kubectl exec` sur Docker Desktop : `export KUBECTL_REMOTE_COMMAND_WEBSOCKETS=false` si l'erreur
« server gave HTTP response to HTTPS client » apparaît.

## Limites assumées (environnement local)

- 1 nœud, Kafka sans réplication, Keycloak en `start-dev`, Secrets seulement encodés (base64).
- Relais Outbox et batch d'expiration prévus pour **une** copie d'event-service (à plusieurs :
  verrou ShedLock ou `SELECT … FOR UPDATE SKIP LOCKED`).
- En production : images dans un registre (ghcr.io), Ingress + TLS au lieu des LoadBalancer, coffre à secrets
  (Vault, Sealed Secrets), et ArgoCD pour synchroniser le cluster sur Git.
