#!/usr/bin/env bash

set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
sample_number="${1:-}"
sample_dir="$repo_dir/meal-samples/$sample_number"

if [[ ! "$sample_number" =~ ^[0-9]{3}$ || ! -d "$sample_dir" ]]; then
  echo "Usage: $0 <three-digit sample folder, such as 001 or 010>" >&2
  exit 1
fi

booted_device=$(xcrun simctl list devices booted --json | \
  sed -n 's/.*"udid"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
if [[ -z "$booted_device" ]]; then
  echo "Start the iOS Simulator before importing a sample pair." >&2
  exit 1
fi

xcrun simctl addmedia "$booted_device" \
  "$sample_dir/front.HEIC" \
  "$sample_dir/back.HEIC"
echo "Imported sample $sample_number front and back as the two newest photos."
