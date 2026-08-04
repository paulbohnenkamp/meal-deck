#!/usr/bin/env bash

set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
sample_dir="$repo_dir/meal-samples"
image_selection="all"
api_url="${EXPO_PUBLIC_API_URL:-http://localhost:8080}"
api_url="${api_url%/}"
backend_pid=""

usage() {
  echo "Usage: $0 [--images none|all|NNN]"
  echo "  none  Start without importing photos"
  echo "  all   Import every meal card and packing slip (default)"
  echo "  NNN   Import one front/back pair, such as 001 or 010"
}

while (( $# > 0 )); do
  case "$1" in
    --images)
      if (( $# < 2 )); then
        usage >&2
        exit 1
      fi
      image_selection="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac
done

if [[ "$image_selection" != "none"
      && "$image_selection" != "all"
      && ! "$image_selection" =~ ^[0-9]{3}$ ]]; then
  echo "--images must be none, all, or a three-digit sample such as 001." >&2
  exit 1
fi

if ! command -v xcrun >/dev/null 2>&1; then
  echo "Xcode command-line tools are required (xcrun was not found)." >&2
  exit 1
fi

stop_mealdeck_listener() {
  local port="$1"
  local expected_dir="$2"
  local listener_pid listener_dir
  while read -r listener_pid; do
    [[ -z "$listener_pid" ]] && continue
    listener_dir=$(lsof -a -p "$listener_pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p')
    if [[ "$listener_dir" != "$expected_dir" ]]; then
      echo "Port $port is owned by PID $listener_pid in $listener_dir; refusing to stop an unrelated process." >&2
      return 1
    fi
    echo "Stopping existing MealDeck process $listener_pid on port $port..."
    kill "$listener_pid"
    for _ in {1..20}; do
      kill -0 "$listener_pid" 2>/dev/null || break
      sleep 0.25
    done
  done < <(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
}

cleanup() {
  [[ -z "$backend_pid" ]] || kill "$backend_pid" 2>/dev/null || true
  stop_mealdeck_listener 8080 "$repo_dir/backend" || true
}
trap cleanup INT TERM EXIT

stop_mealdeck_listener 8081 "$repo_dir/mobile"
stop_mealdeck_listener 8080 "$repo_dir/backend"

if [[ -z "${OPENAI_API_KEY:-}" ]]; then
  echo "Warning: OPENAI_API_KEY is not set; the backend will run, but photo extraction will be unavailable." >&2
fi
echo "Starting a fresh MealDeck backend at $api_url..."
(cd "$repo_dir/backend" && ../mvnw spring-boot:run) &
backend_pid=$!

for _ in {1..90}; do
  if curl --silent --fail "$api_url/api/meals" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    wait "$backend_pid"
    exit $?
  fi
  sleep 1
done

if ! curl --silent --fail "$api_url/api/meals" >/dev/null 2>&1; then
  echo "The backend did not become ready at $api_url within 90 seconds." >&2
  exit 1
fi

if [[ "$image_selection" != "none" && ! -d "$sample_dir" ]]; then
  echo "Private samples were not found at $sample_dir" >&2
  exit 1
fi

shopt -s nullglob
if [[ "$image_selection" == "none" ]]; then
  sample_images=()
elif [[ "$image_selection" == "all" ]]; then
  meal_images=("$sample_dir"/[0-9][0-9][0-9]/*.HEIC)
  packing_slips=("$sample_dir"/packing-slips/*.HEIC)
  sample_images=("${meal_images[@]}" "${packing_slips[@]}")
elif [[ "$image_selection" =~ ^[0-9]{3}$ ]]; then
  if [[ ! -d "$sample_dir/$image_selection" ]]; then
    echo "Sample folder $image_selection was not found." >&2
    exit 1
  fi
  sample_images=(
    "$sample_dir/$image_selection/front.HEIC"
    "$sample_dir/$image_selection/back.HEIC"
  )
else
  echo "--images must be none, all, or a three-digit sample such as 001." >&2
  exit 1
fi
shopt -u nullglob

if [[ "$image_selection" != "none" ]] && (( ${#sample_images[@]} == 0 )); then
  echo "No HEIC samples were found under $sample_dir" >&2
  exit 1
fi

booted_device=""
booted_device=$(xcrun simctl list devices booted --json | \
  sed -n 's/.*"udid"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

if [[ -z "$booted_device" ]]; then
  booted_device=$(xcrun simctl list devices available | \
    sed -n 's/^[[:space:]]*iPhone 16 (\([0-9A-F-]*\))[[:space:]]*(Shutdown)[[:space:]]*$/\1/p' | head -1)
  if [[ -z "$booted_device" ]]; then
    booted_device=$(xcrun simctl list devices available | \
      sed -n 's/^[[:space:]]*iPhone.*(\([0-9A-F-]*\))[[:space:]]*(Shutdown)[[:space:]]*$/\1/p' | head -1)
  fi
  if [[ -z "$booted_device" ]]; then
    echo "No available iPhone Simulator was found." >&2
    exit 1
  fi
  echo "Booting an iPhone Simulator..."
  xcrun simctl boot "$booted_device"
fi

open -a Simulator
if ! xcrun simctl bootstatus "$booted_device" -b; then
  echo "The iOS Simulator did not finish booting." >&2
  exit 1
fi

if [[ -z "$booted_device" ]]; then
  echo "No iOS Simulator is booted." >&2
  exit 1
fi

if [[ "$image_selection" == "none" ]]; then
  echo "Skipping photo import."
elif [[ "$image_selection" == "all" ]]; then
  echo "Importing all ${#sample_images[@]} private sample images into Photos..."
else
  echo "Importing sample $image_selection front and back into Photos..."
fi
if (( ${#sample_images[@]} > 0 )); then
  xcrun simctl addmedia "$booted_device" "${sample_images[@]}"
  echo "Samples imported."
fi
echo "Starting MealDeck; press Ctrl-C to stop Expo."

cd "$repo_dir/mobile"
npm run ios
