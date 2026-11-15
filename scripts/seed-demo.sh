#!/usr/bin/env bash
# Fills a local Zelo API with fictitious demo data: staff, workshops, beneficiaries and
# two weeks of attendance with some absence streaks, so every screen has something to show.
# Names and phone numbers are invented. Run it once against an empty database.
#
# Usage: ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/seed-demo.sh
set -euo pipefail

API="${API_URL:-http://localhost:8080}/api/v1"
: "${ADMIN_EMAIL:?set ADMIN_EMAIL}"
: "${ADMIN_PASSWORD:?set ADMIN_PASSWORD}"
DEMO_PASSWORD="${DEMO_PASSWORD:-demo12345}"

post() {
  curl -sf -X POST "$API$1" -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" -d "$2"
}

TOKEN=$(curl -sf -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" '{email: $e, password: $p}')" | jq -r .token)

if [ "$(curl -sf "$API/activities" -H "Authorization: Bearer $TOKEN" | jq length)" -gt 0 ]; then
  echo "Database already has activities; demo data was not loaded again." >&2
  exit 1
fi

echo "Staff"
post /staff-members "$(jq -n --arg p "$DEMO_PASSWORD" '{fullName: "Coordenação Demo", email: "coordenacao@zelo.local", password: $p, memberType: "COORDINATOR", role: "ROLE_COORDINATOR"}')" > /dev/null
post /staff-members "$(jq -n --arg p "$DEMO_PASSWORD" '{fullName: "Voluntário Demo", email: "voluntario@zelo.local", password: $p, memberType: "VOLUNTEER", role: "ROLE_VOLUNTEER"}')" > /dev/null

echo "Workshops"
CAPOEIRA=$(post /activities '{"title": "Capoeira infantil", "recurrencePattern": "WEEKLY_MON_WED", "startDate": "2026-08-03"}' | jq .id)
REFORCO=$(post /activities '{"title": "Reforço escolar", "recurrencePattern": "WEEKLY_TUE_THU", "startDate": "2026-08-04"}' | jq .id)
post /activities '{"title": "Informática", "recurrencePattern": "WEEKLY_FRI", "startDate": "2026-08-07"}' > /dev/null

echo "Beneficiaries"
declare -a IDS=()
i=0
for name in "Ana Clara Souza" "Bruno Henrique Lima" "Carla Vitória Dias" "Davi Lucas Rocha" "Eduarda Martins" \
  "Felipe Augusto Reis" "Gabriela Nunes" "Heitor Carvalho" "Isabela Freitas" "João Pedro Alves"; do
  i=$((i + 1))
  body=$(jq -n --arg n "$name" --arg ph "(22) 90000-00$(printf '%02d' $i)" \
    --arg bd "201$((i % 5 + 2))-0$((i % 9 + 1))-1$((i % 9))" \
    '{fullName: $n, birthDate: $bd, phoneNumber: $ph}')
  IDS+=("$(post /beneficiaries "$body" | jq .id)")
done

# Four past sessions; the last letters build the current streak (P = present, F = absent).
# Bruno and Eduarda end with 3+ absences in Capoeira and Heitor in Reforço escolar.
declare -A CAPOEIRA_MARKS=([1]=PPPP [2]=PFFF [3]=PPFP [4]=PPPP [5]=FFFF [6]=PPPF [7]=PPPP [8]=PPPP [9]=FPPP [10]=PPPP)
declare -A REFORCO_MARKS=([1]=PPPP [2]=PPPP [3]=PPPP [4]=PFPP [5]=PPPP [6]=PPPP [7]=PPPP [8]=PFFF [9]=PPPP [10]=PPFP)

session() {
  local activity=$1 date=$2 index=$3 marks_name=$4
  local -n marks=$marks_name
  local entries="[]"
  for n in $(seq 1 10); do
    local present=true
    [ "${marks[$n]:$index:1}" = "F" ] && present=false
    entries=$(jq -c --argjson id "${IDS[$((n - 1))]}" --argjson p "$present" '. + [{beneficiaryId: $id, present: $p}]' <<< "$entries")
  done
  post /attendances/batch "$(jq -n --argjson a "$activity" --arg d "$date" --argjson e "$entries" '{activityId: $a, attendanceDate: $d, entries: $e}')" > /dev/null
}

echo "Attendance history"
for index in 0 1 2 3; do
  days_ago=$(( (4 - index) * 3 ))
  session "$CAPOEIRA" "$(date -d "-$days_ago days" +%F)" "$index" CAPOEIRA_MARKS
  session "$REFORCO" "$(date -d "-$((days_ago + 1)) days" +%F)" "$index" REFORCO_MARKS
done

echo "Done. Demo logins: coordenacao@zelo.local / voluntario@zelo.local (password: $DEMO_PASSWORD)"
