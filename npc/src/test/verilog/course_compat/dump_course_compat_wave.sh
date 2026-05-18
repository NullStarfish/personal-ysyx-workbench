#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <test-name> [wave-start-cycle] [wave-end-cycle]" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/course_compat"
OBJ_DIR="$BUILD_DIR/obj_dir"
MEM_DIR="$ROOT_DIR/src/test/resources/course_compat"
WAVE_DIR="$BUILD_DIR/waves"

test_name="$1"
wave_start="${2:-0}"
wave_end="${3:-80}"

cd "$ROOT_DIR"

sbt -Dsbt.server=false --batch "runMain labcpu.top.GenCourseCompatibleTop"

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
  src/main/verilog/CourseCompatible/*.sv \
  src/test/verilog/course_compat/CompatibleIMem.sv \
  src/test/verilog/course_compat/CompatibleDMem.sv \
  src/test/verilog/course_compat/tb_CourseCompatibleTop.sv

"$OBJ_DIR/Vtb_CourseCompatibleTop" \
  "+TEST=$test_name" \
  "+IMEM=$MEM_DIR/$test_name.mem" \
  "+MAX_CYCLES=300" \
  "+DUMPFILE=$WAVE_DIR/$test_name.vcd" \
  "+WAVE_START=$wave_start" \
  "+WAVE_END=$wave_end"

echo "$WAVE_DIR/$test_name.vcd"
