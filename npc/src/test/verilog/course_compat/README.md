# CourseCompatibleTop RTL readmem tests

This directory provides a non-DPI RTL compatibility layer for coursework-style
simulation.

## Components

- `CourseCompatibleTop`: Chisel-generated top that connects `CourseCore` to RTL
  instruction/data memories.
- `CompatibleIMem.sv`: combinational instruction memory loaded with
  `$readmemh` from `+IMEM=<path>`.
- `CompatibleDMem.sv`: byte-addressed data memory loaded with optional
  `+DMEM=<path>` and supporting byte/half/word load-store operations.
- `tb_CourseCompatibleTop.sv`: one reusable testbench. It selects expectations
  with `+TEST=<name>` and program image with `+IMEM=<path>`.

The `.mem` programs live in `src/test/resources/course_compat`. Every `.mem`
image has a same-name `.S` assembly source next to it, so the machine-code image
can be reviewed from human-readable RV32I assembly. These programs are direct
RTL-test translations of the Scala `CourseCoreSmokeSpec`,
`CourseCoreHazardStressSpec`, `CourseCoreProgramSpec`, and `CourseCoreRv32iSpec`
programs.

## Run

```bash
bash src/test/verilog/course_compat/run_course_compat_tests.sh
```

The script generates `src/main/verilog/CourseCompatible`, builds the testbench
with Verilator, and runs all listed `.mem` programs through the same RTL
testbench.

## Dump VCD

Dump all test waves:

```bash
TRACE=1 bash src/test/verilog/course_compat/run_course_compat_tests.sh
```

Dump one program and keep only a selected cycle window:

```bash
bash src/test/verilog/course_compat/dump_course_compat_wave.sh hazard_load_use 0 20
```

The generated VCD files are placed in `build/course_compat/waves`.

Suggested waveform windows:

| Test | Suggested cycle window | What to inspect |
|---|---:|---|
| `hazard_raw` | 0-12 | Dense RAW forwarding from consecutive ALU results. |
| `hazard_load_use` | 0-14 | Load-use stall plus forwarding of loaded data. |
| `hazard_flush` | 0-18 | Branch/jump redirect and wrong-path instruction flush. |
| `smoke` | 0-24 | Mixed arithmetic, memory, branch, byte load/store, and jump behavior. |
| `rv32i_alu_branch` | 10-45 | Branch decision/redirect coverage across BEQ/BNE/BLT/BGE/BLTU/BGEU. |
| `rv32i_load_store` | 0-20 | Byte/half/word signed and unsigned memory behavior. |
