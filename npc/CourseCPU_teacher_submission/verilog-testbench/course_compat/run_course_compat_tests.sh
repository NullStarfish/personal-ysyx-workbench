#!/usr/bin/env bash
set -euo pipefail

TB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUBMISSION_ROOT="$(cd "$TB_DIR/../.." && pwd)"
BUILD_DIR="$SUBMISSION_ROOT/build/course_compat"
OBJ_DIR="$BUILD_DIR/obj_dir"
MEM_DIR="$SUBMISSION_ROOT/verilog-testbench/resources_course_compat"
VSRC_DIR="$SUBMISSION_ROOT/generated-verilog/readmem-compatible-course"
WAVE_DIR="$BUILD_DIR/waves"
TRACE="${TRACE:-0}"

cd "$SUBMISSION_ROOT"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$WAVE_DIR"

trace_args=()
if [[ "$TRACE" == "1" ]]; then
  trace_args+=(--trace)
fi

verilator \
  --binary \
  --timing \
  --top-module tb_CourseCompatibleTop \
  -Mdir "$OBJ_DIR" \
  -Wno-DECLFILENAME \
  -Wno-WIDTHEXPAND \
  -Wno-WIDTHTRUNC \
  "${trace_args[@]}" \
  "$VSRC_DIR"/*.sv \
  "$TB_DIR"/CompatibleIMem.sv \
  "$TB_DIR"/CompatibleDMem.sv \
  "$TB_DIR"/tb_CourseCompatibleTop.sv

tests=(
  smoke
  hazard_raw
  hazard_load_use
  hazard_flush
  program_arith_upper
  program_mem_byte_half
  rv32i_upper_jump
  rv32i_alu_branch
  rv32i_load_store
)

for test_name in "${tests[@]}"; do
  run_args=(
    "+TEST=$test_name"
    "+IMEM=$MEM_DIR/$test_name.mem"
    "+MAX_CYCLES=300"
  )
  if [[ "$TRACE" == "1" ]]; then
    run_args+=(
      "+DUMPFILE=$WAVE_DIR/$test_name.vcd"
      "+WAVE_START=0"
      "+WAVE_END=80"
    )
  fi

  "$OBJ_DIR/Vtb_CourseCompatibleTop" \
    "${run_args[@]}"
done

echo "All CourseCompatibleTop RTL readmem tests passed."
if [[ "$TRACE" == "1" ]]; then
  echo "VCD waves are under $WAVE_DIR"
fi
