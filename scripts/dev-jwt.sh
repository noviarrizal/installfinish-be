#!/usr/bin/env bash
# Local development only: creates a throwaway RS256 key pair and mints bearer tokens the "local" profile accepts.
# Keys are written to .local/jwt/ (git-ignored). Production validates tokens from the real identity provider instead.
#
#   scripts/dev-jwt.sh keys                              # create the key pair (once)
#   scripts/dev-jwt.sh token <subject> [ROLE ...]        # print a token valid for 8 hours
#
# Staff roles: SYSTEM_ADMIN, OPERATOR, QUALITY_REVIEWER. A client user's access comes from the client_user table,
# so mint their token with no roles: scripts/dev-jwt.sh token "<identity subject>"
set -euo pipefail

KEY_DIR="$(cd "$(dirname "$0")/.." && pwd)/.local/jwt"
PRIVATE_KEY="$KEY_DIR/dev-private.pem"
PUBLIC_KEY="$KEY_DIR/dev-public.pem"
AUDIENCE="${JWT_AUDIENCE:-installfinish-api}"
ROLES_CLAIM="${JWT_STAFF_ROLES_CLAIM:-installfinish_roles}"

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

create_keys() {
  if [[ -f "$PRIVATE_KEY" ]]; then
    echo "Key pair already exists in $KEY_DIR" >&2
    return
  fi
  mkdir -p "$KEY_DIR"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIVATE_KEY" 2>/dev/null
  openssl pkey -in "$PRIVATE_KEY" -pubout -out "$PUBLIC_KEY"
  echo "Created $PUBLIC_KEY" >&2
}

mint_token() {
  local subject="$1"; shift
  [[ -f "$PRIVATE_KEY" ]] || { echo "Run '$0 keys' first" >&2; exit 1; }

  local roles="" role
  for role in "$@"; do roles+="${roles:+,}\"$role\""; done
  local now; now=$(date +%s)
  local header='{"alg":"RS256","typ":"JWT"}'
  local payload
  payload=$(printf '{"iss":"installfinish-local-dev","sub":"%s","aud":"%s","iat":%d,"exp":%d,"%s":[%s]}' \
    "$subject" "$AUDIENCE" "$now" "$((now + 8 * 3600))" "$ROLES_CLAIM" "$roles")

  local signing_input
  signing_input="$(printf '%s' "$header" | b64url).$(printf '%s' "$payload" | b64url)"
  local signature
  signature=$(printf '%s' "$signing_input" | openssl dgst -sha256 -sign "$PRIVATE_KEY" -binary | b64url)
  printf '%s.%s\n' "$signing_input" "$signature"
}

case "${1:-}" in
  keys) create_keys ;;
  token) shift; [[ $# -ge 1 ]] || { echo "usage: $0 token <subject> [ROLE ...]" >&2; exit 1; }; mint_token "$@" ;;
  *) echo "usage: $0 keys | token <subject> [ROLE ...]" >&2; exit 1 ;;
esac
