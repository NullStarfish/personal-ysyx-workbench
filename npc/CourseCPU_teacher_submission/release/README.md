# CourseCPU Release Package

This directory contains a self-contained CourseCPU simulation release.

## Host Requirements

The prebuilt simulator is a Linux x86-64 executable. It was built and smoke
tested on Debian with:

- glibc: `Debian GLIBC 2.41-12+deb13u2`
- RISC-V GCC: `riscv64-linux-gnu-gcc (Debian 14.2.0-19) 14.2.0`
- GNU binutils: `2.44`

Recommended host environment:

- Linux x86-64
- glibc 2.41 or newer, or a compatible Debian/Ubuntu environment
- `bash`
- `libreadline.so.8`
- `libtinfo.so.6`
- `libstdc++.so.6`
- `libgcc_s.so.1`
- `libm.so.6`
- `libc.so.6`
- RISC-V cross toolchain if you want to build new C programs with `am-sdk`

On Debian/Ubuntu, install the runtime dependencies and RISC-V toolchain with:

```bash
sudo apt update
sudo apt install -y \
  bash \
  libc6 \
  libstdc++6 \
  libgcc-s1 \
  libreadline8 \
  libtinfo6 \
  gcc-riscv64-linux-gnu \
  binutils-riscv64-linux-gnu
```

If your distribution does not provide `libreadline8` or `libtinfo6` under these
exact package names, install the equivalent readline 8 and ncurses/tinfo runtime
packages. If your glibc is older than the build environment, prefer running this
release inside a newer Debian/Ubuntu container or rebuild `bin/npc-course` on
your machine.

You can inspect the simulator's dynamic library requirements with:

```bash
ldd bin/npc-course
```

The expected dependency list includes:

```text
libreadline.so.8
libstdc++.so.6
libm.so.6
libgcc_s.so.1
libc.so.6
libtinfo.so.6
```

## Contents

- `bin/npc-course`: prebuilt Verilator simulator for CourseCPU.
- `ref/riscv32-nemu-interpreter-so`: NEMU reference model for difftest.
- `tools/capstone/repo/libcapstone.so.5`: Capstone runtime library used by the packaged NEMU reference model.
- `benchmarks/*/*.bin`: prebuilt benchmark images.
- `benchmarks/*/*.c`: benchmark C sources.
- `benchmarks/*/*-riscv32e-npc.txt`: benchmark disassembly dumps.
- `am-sdk/lib/am-riscv32e-npc.a`: prebuilt Abstract-Machine static runtime library.
- `am-sdk/include`: exported AM and klib headers.
- `am-sdk/scripts`: exported linker/build fragments used by the AM NPC target.
- `docs/performance_summary.txt`: recorded performance summary.

## Run

Run quick-sort without difftest:

```bash
./run.sh
```

Run a specific benchmark without difftest:

```bash
./run.sh benchmarks/select-sort/select-sort-riscv32e-npc.bin
```

Run with NEMU difftest:

```bash
./run-diff.sh benchmarks/quick-sort/quick-sort-riscv32e-npc.bin
```

Both scripts enable batch mode (`-b`), so they run to completion without entering
the interactive SDB prompt.

## Build A New C Program With The Exported AM SDK

The `sdk-smoke/` directory contains a minimal C program that is built only from
the exported AM SDK files in this release package.

Install the RISC-V toolchain first:

```bash
sudo apt update
sudo apt install -y gcc-riscv64-linux-gnu binutils-riscv64-linux-gnu
```

Check the toolchain:

```bash
riscv64-linux-gnu-gcc --version
riscv64-linux-gnu-objcopy --version
```

Build it:

```bash
./build-sdk-smoke.sh
```

Run it:

```bash
./run.sh sdk-smoke/smoke-riscv32e-npc.bin
```

Expected output includes:

```text
OK
ebreak: state: 2, a0: 0
```

The build script compiles with:

```bash
riscv64-linux-gnu-gcc \
  -fno-pic -march=rv32e_zicsr -mabi=ilp32e -mcmodel=medany -mstrict-align \
  -ffreestanding -fno-builtin -O2 \
  -DARCH_H='"arch/riscv.h"' -DMAINARGS_MAX_LEN=64 -DMAINARGS_PLACEHOLDER='""' \
  -I am-sdk/include/am -I am-sdk/include/klib \
  -c sdk-smoke/smoke.c -o sdk-smoke/smoke.o
```

Then it links with the exported AM runtime:

```bash
riscv64-linux-gnu-gcc \
  -nostdlib -static -march=rv32e_zicsr -mabi=ilp32e -Wl,-melf32lriscv \
  -Wl,-T,am-sdk/scripts/linker.ld \
  -Wl,--defsym=_pmem_start=0x80000000 \
  -Wl,--defsym=_entry_offset=0 \
  -Wl,-u,_start \
  sdk-smoke/smoke.o am-sdk/lib/am-riscv32e-npc.a \
  -o sdk-smoke/smoke.elf
```

Finally it converts the ELF into a raw image that CourseCPU can load:

```bash
riscv64-linux-gnu-objcopy -O binary \
  sdk-smoke/smoke.elf \
  sdk-smoke/smoke-riscv32e-npc.bin
```

## Notes

The simulator is a Linux x86-64 executable. It dynamically links common host
libraries such as `libreadline`, `libstdc++`, `libm`, `libgcc_s`, `libc`, and
`libtinfo`.

The AM SDK is exported as a static runtime archive plus headers and linker
scripts. This matches the bare-metal RISC-V build flow better than exporting a
host `.so`.

The difftest script sets `NEMU_HOME` to this release directory by default, so
the packaged NEMU `.so` can find its bundled Capstone dependency.
