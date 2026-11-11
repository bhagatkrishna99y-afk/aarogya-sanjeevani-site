#!/usr/bin/env bash
# Concurrency simulation: proves that when 20 patients race to book the
# same doctor slot and the doctor then races to accept more than one of
# the resulting requests, exactly one acceptance wins and every other
# one fails safely with AS019_SLOT_ALREADY_BOOKED -- it never leaves two
# accepted/confirmed requests overlapping for the same doctor.
#
# Requires `psql` and `pgbench` (both ship with a standard PostgreSQL
# client install, e.g. `apt install postgresql-client` /
# `brew install libpq postgresql`) and a DATABASE_URL pointing at a real
# Postgres connection. Point this at a local `supabase start` stack, not
# the shared hosted project -- this script creates and deletes real rows
# (via its own fixture, cleaned up at the end) and opens up to 20
# concurrent connections, neither of which you want to do against shared
# infrastructure just to run a test.
#
# Usage:
#   DATABASE_URL="postgresql://postgres:postgres@localhost:54322/postgres" \
#     ./scripts/run_concurrency_test.sh
#
# Optional: ACCEPT_CLIENTS (default 2) sets how many of the 20 requests
# the doctor races to accept concurrently -- 2 reproduces the literal
# "doctor accepts two of them" scenario; set it as high as 20 to exercise
# the same EXCLUDE constraint under full load.

set -euo pipefail

: "${DATABASE_URL:?Set DATABASE_URL to a real Postgres connection string, e.g. your local supabase start stack.}"
ACCEPT_CLIENTS="${ACCEPT_CLIENTS:-2}"

if ! command -v psql >/dev/null 2>&1 || ! command -v pgbench >/dev/null 2>&1; then
  echo "This script needs both 'psql' and 'pgbench' on PATH. Install the PostgreSQL client tools and retry." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIXTURE_DIR="$SCRIPT_DIR/../supabase/tests/concurrency"

cleanup() {
  echo "--- Tearing down fixture ---"
  psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$FIXTURE_DIR/fixture_teardown.sql" || true
}
trap cleanup EXIT

echo "--- Setting up fixture: 1 doctor, 20 patients, 1 shared slot ---"
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$FIXTURE_DIR/fixture_setup.sql"

echo "--- Phase 1: 20 concurrent patients create a request for the same slot ---"
pgbench -n -c 20 -j 20 -t 1 -f "$FIXTURE_DIR/01_create_requests.pgbench.sql" "$DATABASE_URL"

CREATED_COUNT=$(psql "$DATABASE_URL" -t -A -c "
  select count(*) from public.requests r, public._concurrency_fixture f
  where r.doctor_id = f.doctor_id and r.scheduled_start = f.slot_start and r.status = 'requested';
")
echo "Requests created for the shared slot: $CREATED_COUNT (expect 20)"

echo "--- Phase 2: $ACCEPT_CLIENTS concurrent accept attempts on different requests for that slot ---"
set +e
pgbench -n -c "$ACCEPT_CLIENTS" -j "$ACCEPT_CLIENTS" -t 1 -f "$FIXTURE_DIR/02_accept_race.pgbench.sql" "$DATABASE_URL"
set -e

echo "--- Verifying: exactly one request for the slot ended up accepted ---"
ACCEPTED_COUNT=$(psql "$DATABASE_URL" -t -A -c "
  select count(*) from public.requests r, public._concurrency_fixture f
  where r.doctor_id = f.doctor_id and r.scheduled_start = f.slot_start and r.status = 'accepted';
")
OVERLAP_COUNT=$(psql "$DATABASE_URL" -t -A -c "
  select count(*)
  from public.requests
  where status in ('accepted', 'confirmed')
  group by doctor_id, tstzrange(scheduled_start, scheduled_end)
  having count(*) > 1;
")

echo "Accepted requests for the shared slot: $ACCEPTED_COUNT (expect exactly 1)"
echo "Doctors with overlapping accepted/confirmed requests, anywhere in the table: ${OVERLAP_COUNT:-0} (expect 0)"

if [ "$ACCEPTED_COUNT" = "1" ] && [ -z "$OVERLAP_COUNT" ]; then
  echo "PASS: exactly one acceptance won the race; every other concurrent attempt failed safely."
  exit 0
else
  echo "FAIL: either more than one request was accepted for the same slot, or an overlap was found elsewhere." >&2
  exit 1
fi
