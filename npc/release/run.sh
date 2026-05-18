#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMG="${1:-$ROOT/benchmarks/quick-sort/quick-sort-riscv32e-npc.bin}"
LOG="${2:-$ROOT/run.log}"

if [[ ! -x "$ROOT/bin/npc-course" ]]; then
  echo "error: missing executable: $ROOT/bin/npc-course" >&2
  exit 1
fi

if [[ ! -f "$IMG" ]]; then
  echo "error: image not found: $IMG" >&2
  exit 1
fi

"$ROOT/bin/npc-course" -b -l "$LOG" "$IMG"
