#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$ROOT/sdk-smoke"
SRC="$OUT/smoke.c"

mkdir -p "$OUT"

riscv64-linux-gnu-gcc \
  -fno-pic -march=rv32e_zicsr -mabi=ilp32e -mcmodel=medany -mstrict-align \
  -ffreestanding -fno-builtin -O2 \
  -DARCH_H='"arch/riscv.h"' -DMAINARGS_MAX_LEN=64 -DMAINARGS_PLACEHOLDER='""' \
  -I "$ROOT/am-sdk/include/am" -I "$ROOT/am-sdk/include/klib" \
  -c "$SRC" -o "$OUT/smoke.o"

riscv64-linux-gnu-gcc \
  -nostdlib -static -march=rv32e_zicsr -mabi=ilp32e -Wl,-melf32lriscv \
  -Wl,-T,"$ROOT/am-sdk/scripts/linker.ld" \
  -Wl,--defsym=_pmem_start=0x80000000 \
  -Wl,--defsym=_entry_offset=0 \
  -Wl,-u,_start \
  "$OUT/smoke.o" "$ROOT/am-sdk/lib/am-riscv32e-npc.a" \
  -o "$OUT/smoke.elf"

riscv64-linux-gnu-objcopy -O binary "$OUT/smoke.elf" "$OUT/smoke-riscv32e-npc.bin"
riscv64-linux-gnu-objdump -d "$OUT/smoke.elf" > "$OUT/smoke-riscv32e-npc.txt"

echo "built $OUT/smoke-riscv32e-npc.bin"
