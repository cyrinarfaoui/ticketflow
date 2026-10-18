#!/usr/bin/env bash
# Déploie (ou met à jour) TicketFlow sur le cluster Kubernetes courant.
# À lancer depuis la racine du projet : bash infra/k8s/deploy.sh
# Relançable sans risque : "kubectl apply" ne change que ce qui a changé.
set -euo pipefail

NS=ticketflow
K8S=infra/k8s

kubectl apply -f "$K8S/namespace.yaml"

# --- Secrets : créés une seule fois, par saisie (jamais écrits dans un fichier commité) ---
creer_secret_si_absent() {   # $1 = nom du Secret, $2 = libellé demandé
  if ! kubectl get secret "$1" -n "$NS" >/dev/null 2>&1; then
    read -r -s -p "$2 : " valeur; echo
    kubectl create secret generic "$1" -n "$NS" --from-literal=password="$valeur"
  fi
}
creer_secret_si_absent postgres-credentials "Mot de passe des bases PostgreSQL"
creer_secret_si_absent keycloak-admin "Mot de passe de l'administrateur Keycloak"
if ! kubectl get secret keycloak-realm -n "$NS" >/dev/null 2>&1; then
  # Export du realm (voir README) : ignoré par Git car il contient des mots de passe hachés
  kubectl create secret generic keycloak-realm -n "$NS" \
    --from-file=ticketflow-realm.json="$K8S/keycloak/ticketflow-realm.json"
fi

# --- ConfigMaps générées depuis les MÊMES fichiers que Docker Compose (aucune copie à maintenir) ---
configmap_depuis_fichier() {   # $1 = nom, $2 = fichier
  kubectl create configmap "$1" -n "$NS" --from-file="$2" --dry-run=client -o yaml | kubectl apply -f -
}
configmap_depuis_fichier prometheus-config           infra/prometheus/prometheus.yml
configmap_depuis_fichier grafana-datasources         infra/grafana/provisioning/datasources/prometheus.yml
configmap_depuis_fichier grafana-dashboard-providers infra/grafana/provisioning/dashboards/ticketflow.yml
configmap_depuis_fichier grafana-dashboards          infra/grafana/dashboards/ticketflow.json

# --- L'application : infrastructure d'abord, puis les services (Kubernetes relance ceux qui démarrent trop tôt) ---
for composant in postgres-event-service postgres-payment-service kafka keycloak mailpit \
                 event-service payment-service notification-service auth-service api-gateway web \
                 prometheus grafana; do
  kubectl apply -f "$K8S/$composant.yaml"
done

echo
echo "Déployé. Suivre le démarrage : kubectl get pods -n $NS -w"
