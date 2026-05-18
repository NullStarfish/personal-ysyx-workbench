#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <test-name> [wave-start-cycle] [wave-end-cycle]" >&2
  exit 1
fi

TB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUBMISSION_ROOT="$(cd "$TB_DIR/../.." && pwd)"
BUILD_DIR="$SUBMISSION_ROOT/build/course_compat"
OBJ_DIR="$BUILD_DIR/obj_dir"
MEM_DIR="$SUBMISSION_ROOT/verilog-testbench/resources_course_compat"
VSRC_DIR="$SUBMISSION_ROOT/generated-verilog/readmem-compatible-course"
WAVE_DIR="$BUILD_DIR/waves"

test_name="$1"
wave_start="${2:-0}"
wave_end="${3:-80}"

cd "$SUBMISSION_ROOT"

mkdir -p "$BUILD_DIR" "$WAVE_DIR"

verilator \
  --binary \
  --timing \
  --trace \
  --top-module tb_CourseCompatibleTop \
  -Mdir "$OBJ_DIR" \
  -Wno-DECLFILENAME \
  -Wno-WIDTHEXPAND \
  -Wno-WIDTHTRUNC \
  "$VSRC_DIR"/*.sv \
  "$TB_DIR"/CompatibleIMem.sv \
  "$TB_DIR"/CompatibleDMem.sv \
  "$TB_DIR"/tb_CourseCompatibleTop.sv

"$OBJ_DIR/Vtb_CourseCompatibleTop" \
  "+TEST=$test_name" \
  "+IMEM=$MEM_DIR/$test_name.mem" \
  "+MAX_CYCLES=300" \
  "+DUMPFILE=$WAVE_DIR/$test_name.vcd" \
  "+WAVE_START=$wave_start" \
  "+WAVE_END=$wave_end"

echo "$WAVE_DIR/$test_name.vcd"
