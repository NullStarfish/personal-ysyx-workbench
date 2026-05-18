#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMG="${1:-$ROOT/benchmarks/quick-sort/quick-sort-riscv32e-npc.bin}"
LOG="${2:-$ROOT/run-diff.log}"
REF="${3:-$ROOT/ref/riscv32-nemu-interpreter-so}"

if [[ ! -x "$ROOT/bin/npc-course" ]]; then
  echo "error: missing executable: $ROOT/bin/npc-course" >&2
  exit 1
fi

if [[ ! -f "$IMG" ]]; then
  echo "error: image not found: $IMG" >&2
  exit 1
fi

if [[ ! -f "$REF" ]]; then
  echo "error: difftest reference not found: $REF" >&2
  exit 1
fi

export NEMU_HOME="${NEMU_HOME:-$ROOT}"
export LD_LIBRARY_PATH="$ROOT/tools/capstone/repo${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

"$ROOT/bin/npc-course" -b --diff="$REF" -l "$LOG" "$IMG"
