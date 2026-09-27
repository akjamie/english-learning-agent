#!/usr/bin/env bash
# Submit a change as a PR: create branch, commit, push (triggers the asdlc review
# via the pre-push hook), then open the PR.
#   usage: scripts/submit-pr.sh <CHANGE_ID> [spec|impl] [commit message]
set -euo pipefail
CHANGE_ID="${1:?usage: submit-pr.sh <CHANGE_ID> [spec|impl] [message]}"
MODE="${2:-spec}"
MSG="${3:-$MODE($CHANGE_ID): submit change for review}"
slug="$(printf '%s' "$CHANGE_ID" | tr '[:upper:]' '[:lower:]')"
if [ "$MODE" = "impl" ]; then branch="asdlc/impl/$slug"; else branch="asdlc/$slug"; fi

git checkout -B "$branch"
git add -A
git commit -m "$MSG" || echo "[asdlc] nothing to commit"
git push -u origin "$branch"
gh pr create --base main --head "$branch" \
  --title "$MODE($CHANGE_ID): proposal" \
  --body "Submitted for AI review via asdlc." || true
