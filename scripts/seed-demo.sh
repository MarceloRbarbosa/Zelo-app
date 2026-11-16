#!/usr/bin/env bash
# Fills a local Zelo API with fictitious demo data: staff, classes with schedules and enrolled students,
# four weeks of attendance with some absence streaks, an event with registrations and assistance stock,
# so every screen has something to show. Names and phone numbers are invented.
# Run it once against an empty database.
#
# Usage: ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/seed-demo.sh
set -euo pipefail

API="${API_URL:-http://localhost:8080}/api/v1"
: "${ADMIN_EMAIL:?set ADMIN_EMAIL}"
: "${ADMIN_PASSWORD:?set ADMIN_PASSWORD}"
DEMO_PASSWORD="${DEMO_PASSWORD:-demo12345}"

call() {
  curl -sf -X "$1" "$API$2" -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" ${3:+-d "$3"}
}

TOKEN=$(curl -sf -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg e "$ADMIN_EMAIL" --arg p "$ADMIN_PASSWORD" '{email: $e, password: $p}')" | jq -r .token)

if [ "$(call GET /activities | jq length)" -gt 0 ]; then
  echo "Database already has activities; demo data was not loaded again." >&2
  exit 1
fi

echo "Staff"
staff() {
  call POST /staff-members "$(jq -n --arg n "$1" --arg e "$2" --arg p "$DEMO_PASSWORD" --arg t "$3" --arg r "$4" \
    '{fullName: $n, email: $e, password: $p, memberType: $t, role: $r}')" | jq .id
}
COORDINATOR=$(staff "Coordenação Demo" coordenacao@zelo.local COORDINATOR ROLE_COORDINATOR)
VOLUNTEER=$(staff "Voluntário Demo" voluntario@zelo.local VOLUNTEER ROLE_VOLUNTEER)

echo "Beneficiaries"
declare -a IDS=()
# Fictitious CPFs with valid check digits (the API rejects invalid ones).
CPFS=(52998224725 11144477735 93541134780 01234567890 09876543229 24681357928 13579246828 86420975310 31415926590 27182818205)
i=0
for name in "Ana Clara Souza" "Bruno Henrique Lima" "Carla Vitória Dias" "Davi Lucas Rocha" "Eduarda Martins" \
  "Felipe Augusto Reis" "Gabriela Nunes" "Heitor Carvalho" "Isabela Freitas" "João Pedro Alves"; do
  i=$((i + 1))
  body=$(jq -n --arg n "$name" --arg ph "(22) 90000-00$(printf '%02d' $i)" --arg cpf "${CPFS[$((i - 1))]}" \
    --arg bd "201$((i % 5 + 2))-0$((i % 9 + 1))-1$((i % 9))" \
    '{fullName: $n, documentNumber: $cpf, birthDate: $bd, phoneNumber: $ph}')
  IDS+=("$(call POST /beneficiaries "$body" | jq .id)")
done

echo "Classes and enrollments"
START=$(date -d "-8 weeks" +%F)
activity() {
  call POST /activities "$(jq -n --arg t "$1" --arg d "$2" --arg s "$START" --argjson sc "$3" --argjson st "$4" \
    '{title: $t, description: $d, startDate: $s, schedules: $sc, staffIds: $st}')" | jq .id
}
CAPOEIRA=$(activity "Capoeira infantil" "Turma da tarde, 7 a 12 anos" \
  '[{"dayOfWeek":"MONDAY","startTime":"14:00","endTime":"15:30"},{"dayOfWeek":"WEDNESDAY","startTime":"14:00","endTime":"15:30"}]' \
  "[$VOLUNTEER]")
REFORCO=$(activity "Reforço escolar" "Português e matemática" \
  '[{"dayOfWeek":"TUESDAY","startTime":"09:00","endTime":"11:00"},{"dayOfWeek":"THURSDAY","startTime":"09:00","endTime":"11:00"}]' \
  "[$COORDINATOR,$VOLUNTEER]")
activity "Informática" "Primeiros passos no computador" \
  '[{"dayOfWeek":"FRIDAY","startTime":"15:00","endTime":"16:30"}]' "[$COORDINATOR]" > /dev/null

for id in "${IDS[@]}"; do
  call POST "/activities/$CAPOEIRA/enrollments" "{\"beneficiaryId\": $id}" > /dev/null
  call POST "/activities/$REFORCO/enrollments" "{\"beneficiaryId\": $id}" > /dev/null
done

# The last four class dates before today for the given ISO weekdays (1 = Monday ... 7 = Sunday).
last_classes() {
  local days=$1 found=() offset=1
  while [ ${#found[@]} -lt 4 ]; do
    if [[ " $days " == *" $(date -d "-$offset days" +%u) "* ]]; then
      found=("$(date -d "-$offset days" +%F)" "${found[@]}")
    fi
    offset=$((offset + 1))
  done
  echo "${found[@]}"
}

# Four past classes per activity, oldest first; the last letters build the current streak (P = present, F = absent).
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
  call POST /attendances/batch "$(jq -n --argjson a "$activity" --arg d "$date" --argjson e "$entries" \
    '{activityId: $a, attendanceDate: $d, entries: $e}')" > /dev/null
}

echo "Attendance history"
read -r -a CAPOEIRA_DATES <<< "$(last_classes "1 3")"
read -r -a REFORCO_DATES <<< "$(last_classes "2 4")"
for index in 0 1 2 3; do
  session "$CAPOEIRA" "${CAPOEIRA_DATES[$index]}" "$index" CAPOEIRA_MARKS
  session "$REFORCO" "${REFORCO_DATES[$index]}" "$index" REFORCO_MARKS
done

echo "Event"
EVENT=$(call POST /events "$(jq -n --arg d "$(date -d "$(date -d "+6 days" +%F) 09:00" --iso-8601=seconds)" --argjson st "[$VOLUNTEER]" \
  '{title: "Feira de saúde da comunidade", description: "Vacinação e orientação com a UBS", eventDate: $d,
    location: "Quadra do Lar", staffIds: $st}')" | jq .id)
for id in "${IDS[@]:0:4}"; do
  call POST "/events/$EVENT/participants" "{\"beneficiaryId\": $id}" > /dev/null
done

echo "Assistance"
BASKET=$(call POST /assistance/items '{"name": "Cesta básica", "description": "Arroz, feijão, óleo, açúcar e café"}' | jq .id)
call POST /assistance/items '{"name": "Kit de higiene"}' > /dev/null
call POST /assistance/receipts "$(jq -n --argjson i "$BASKET" --arg d "$(date -d "-10 days" +%F)" \
  '{itemId: $i, quantity: 20, receivedAt: $d, donor: "Mercado do bairro"}')" > /dev/null
for id in "${IDS[@]:0:3}"; do
  call POST /assistance/deliveries "$(jq -n --argjson i "$BASKET" --argjson b "$id" --arg d "$(date -d "-3 days" +%F)" \
    '{itemId: $i, beneficiaryId: $b, quantity: 1, deliveredAt: $d}')" > /dev/null
done

echo "Done. Demo logins: coordenacao@zelo.local / voluntario@zelo.local (provisional password: $DEMO_PASSWORD)"
