#!/usr/bin/env bash
# Test de charge "surréservation" en une commande : 500 clients au même instant sur un événement de 50 places.
# Usage (Git Bash, depuis la racine du projet) : bash perf-tests/test-de-charge.sh "Charge demo 2"
# Prérequis : JMeter installé, et JMETER_HOME défini (ou la commande jmeter dans le PATH).
# Le mot de passe de perf-user est demandé au clavier : il n'est jamais écrit dans un fichier.
set -euo pipefail

if [ $# -ne 1 ]; then
  echo 'Usage : bash perf-tests/test-de-charge.sh "Titre de l événement"' >&2
  exit 1
fi
TITRE="$1"
API="http://localhost:8000/api"
# JMeter : JMETER_HOME (dossier d'installation de JMeter) s'il est défini, sinon la commande jmeter du PATH
if [ -n "${JMETER_HOME:-}" ]; then
  JMETER="$JMETER_HOME/bin/jmeter"
  [ -f "$JMETER.bat" ] && JMETER="$JMETER.bat"   # Windows
elif command -v jmeter >/dev/null 2>&1; then
  JMETER="jmeter"
else
  echo "JMeter introuvable : définir JMETER_HOME (ex. export JMETER_HOME=\"/chemin/vers/apache-jmeter-5.6.3\")" >&2
  exit 1
fi
RESULTATS="perf-tests/resultats/test-$(date +%Y%m%d-%H%M%S).jtl"

places() {   # places restantes de l'événement
  curl -s "$API/events/$EVENT_ID" | grep -o '"availableSeats":[0-9]*' | cut -d: -f2
}

# 1. L'événement visé : le premier qui porte ce titre ET a encore des places
EVENT_ID=$(curl -s -G "$API/events" --data-urlencode "title=$TITRE" --data "availableOnly=true" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ -z "$EVENT_ID" ]; then
  echo "Aucun événement « $TITRE » avec des places libres." >&2
  exit 1
fi

# 2. Le mot de passe du compte de test (saisi, jamais affiché)
if [ -z "${PERF_PASSWORD:-}" ]; then
  read -r -s -p "Mot de passe de perf-user : " PERF_PASSWORD
  echo
  export PERF_PASSWORD
fi

AVANT=$(places)
echo
echo "Événement « $TITRE » : $AVANT places libres"
echo "Lancement : 500 clients réservent 1 place AU MÊME INSTANT…"
echo

# 3. Le test (mode ligne de commande, sans interface)
"$JMETER" -n -t perf-tests/reservation-surcharge.jmx -JeventId="$EVENT_ID" -l "$RESULTATS" \
  | grep -E "summary =|Err:" | tail -1

# 4. Le bilan, lu dans les résultats
compte() { awk -F, -v code="$1" '$3 == "Réserver 1 place" && $4 == code' "$RESULTATS" | wc -l | tr -d ' '; }
ACCEPTEES=$(compte 201)
COMPLET=$(compte 409)
TOTAL=$(awk -F, '$3 == "Réserver 1 place"' "$RESULTATS" | wc -l | tr -d ' ')
AUTRES=$((TOTAL - ACCEPTEES - COMPLET))
APRES=$(places)

echo
echo "================ BILAN ================"
echo " Réservations envoyées      : $TOTAL"
echo " Acceptées (201)            : $ACCEPTEES"
echo " Refusées, complet (409)    : $COMPLET"
echo " Autres réponses (erreurs)  : $AUTRES"
echo " Places restantes           : $APRES / $AVANT"
if [ "$ACCEPTEES" -eq "$AVANT" ] && [ "$APRES" -eq 0 ] && [ "$AUTRES" -eq 0 ]; then
  echo " ✅ Aucune surréservation : exactement $AVANT places vendues"
else
  echo " ❌ Résultat inattendu : voir $RESULTATS"
fi
echo "======================================="
