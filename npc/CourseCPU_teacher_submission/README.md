# CourseCPU 提交包说明

本目录是本次作业的完整提交包。CourseCPU 是一个 RV32I 单发射 4 级流水线 CPU。按照作业默认流程，主要检查入口是 Vivado/RTL 仿真可用的 Verilog testbench、`$readmemh` 版 IMEM/DMEM、`.S/.mem` 测试程序以及综合实现报告。除此之外，我们额外提供 Verilator 自动化仿真、NEMU difftest 和 Abstract Machine C 程序开发环境，作为增强验证选项。

## 1. 先看哪里

如果只想按作业要求检查文件，请看下面这些目录：

- `generated-verilog/readmem-compatible-course/`：适合 Vivado/传统 RTL testbench 的 CPU RTL。顶层是 `CourseCompatibleTop.sv`，内部连接了 CPU Core 和调试接口，不依赖 DPI。
- `verilog-testbench/course_compat/`：Vivado/RTL 仿真用 testbench 目录。这里包含 `tb_CourseCompatibleTop.sv`、`CompatibleIMem.sv`、`CompatibleDMem.sv` 和可选 Verilator 运行脚本。
- `verilog-testbench/resources_course_compat/`：每个测试程序的汇编源码 `.S` 和对应机器码 `.mem`。`.mem` 通过 `$readmemh` 装载。
- `generated-verilog/synthesis-course/`：综合/实现用 RTL。顶层是 `CourseSynthTop.sv`。
- `generated-verilog/simulation-course-dpi/`：额外 Verilator + DPI 仿真用 RTL。顶层是 `CourseTop.sv`。
- `verilator-sim/`：额外自动化仿真框架，可以直接编译 `simulation-course-dpi` RTL，不会运行 Chisel/SBT。
- `release/`：预编译可运行发布包，包含 `npc-course`、NEMU difftest 参考模型、AM SDK、benchmark 程序和脚本。
- `report/`：实验报告 LaTeX 源文件和图片素材。
- `reports/`：Vivado timing/utilization/power 和性能数据。

## 2. 作业要求中的 IMEM/DMEM RTL 在哪里

传统作业要求通常希望看到“指令存储器、数据存储器、testbench、mem 文件”。本提交包对应如下：

- 指令存储器：`verilog-testbench/course_compat/CompatibleIMem.sv`
- 数据存储器：`verilog-testbench/course_compat/CompatibleDMem.sv`
- CPU 兼容顶层：`generated-verilog/readmem-compatible-course/CourseCompatibleTop.sv`
- Testbench：`verilog-testbench/course_compat/tb_CourseCompatibleTop.sv`
- 测试程序 `.S/.mem`：`verilog-testbench/resources_course_compat/`
- Vivado/GTKWAVE 波形截图与说明：`verilog-testbench/resources_course_compat/*.png` 和 `*_explain.txt`

`CompatibleIMem.sv` 使用 `+IMEM=xxx.mem` 指定程序镜像，并通过 `$readmemh` 读取；`CompatibleDMem.sv` 是字节寻址 RAM，支持字节/半字/字访问，适合作业要求里的 `LB/SB/LW/SW` 验证。

## 3. 怎么在 Vivado 中跑 Verilog Testbench

作业默认使用 Vivado 仿真时，可以按下面方式添加文件：

1. 新建 Vivado RTL Project。
2. 将 `generated-verilog/readmem-compatible-course/*.sv` 加入 Design Sources。
3. 将 `verilog-testbench/course_compat/CompatibleIMem.sv` 加入 Design Sources。
4. 将 `verilog-testbench/course_compat/CompatibleDMem.sv` 加入 Design Sources。
5. 将 `verilog-testbench/course_compat/tb_CourseCompatibleTop.sv` 加入 Simulation Sources。
6. 设置仿真顶层为 `tb_CourseCompatibleTop`。
7. 运行仿真时通过 plusargs 指定测试名和 `.mem` 文件，例如 `+TEST=hazard_load_use +IMEM=verilog-testbench/resources_course_compat/hazard_load_use.mem +MAX_CYCLES=300`。

如果没有传 `+IMEM=...`，`CompatibleIMem.sv` 会默认填充 NOP，因此 Vivado 仿真时需要设置 plusargs。若 GUI 不方便设置 plusargs，可以临时在 `CompatibleIMem.sv` 的 `initial` 块中固定 `$readmemh` 路径，并在 `tb_CourseCompatibleTop.sv` 中固定 `test_name`。所有 `.mem` 和同名 `.S` 都放在 `verilog-testbench/resources_course_compat/`。

## 4. 可选：用 Verilator 快速跑同一个 Verilog Testbench

下面不是作业必需流程，只是为了快速自动化回归同一套 RTL testbench。需要安装 Verilator：

```bash
sudo apt update
sudo apt install -y make g++ verilator
```

运行全部 RTL readmem 兼容测试：

```bash
cd verilog-testbench/course_compat
make test
```

这会编译：

- `generated-verilog/readmem-compatible-course/*.sv`
- `verilog-testbench/course_compat/CompatibleIMem.sv`
- `verilog-testbench/course_compat/CompatibleDMem.sv`
- `verilog-testbench/course_compat/tb_CourseCompatibleTop.sv`

并依次运行 `resources_course_compat/` 下的多个 `.mem` 程序。testbench 会自动检查寄存器或内存最终结果，失败会 `$fatal`，成功会打印 `PASS`。

如果需要导出 VCD 波形：

```bash
cd verilog-testbench/course_compat
make wave
```

只导出某一个程序的波形，例如 load-use 冒险：

```bash
cd verilog-testbench/course_compat
bash dump_course_compat_wave.sh hazard_load_use 0 20
```

VCD 会生成在：

```text
build/course_compat/waves/
```

## 5. 测试程序和作业覆盖点

`verilog-testbench/resources_course_compat/` 中每个 `.mem` 都有同名 `.S` 汇编源码，方便检查机器码来源。

主要程序对应关系：

| 程序 | 作用 |
|---|---|
| `smoke.S/.mem` | 基础算术、访存、分支、跳转混合测试 |
| `hazard_raw.S/.mem` | RAW 数据相关，观察 forwarding |
| `hazard_load_use.S/.mem` | load-use 冒险，观察 stall |
| `hazard_flush.S/.mem` | 分支/跳转重定向，观察 flush |
| `rv32i_alu_branch.S/.mem` | ALU、比较、分支类指令覆盖 |
| `rv32i_load_store.S/.mem` | 字节/半字/字 load-store 行为 |
| `rv32i_upper_jump.S/.mem` | LUI/AUIPC/JAL/JALR 等上位立即数和跳转 |
| `program_arith_upper.S/.mem` | 算术和上位立即数程序测试 |
| `program_mem_byte_half.S/.mem` | 字节/半字访存程序测试 |

这些测试对应作业中的指令覆盖、RAW forwarding、load-use stall、branch flush、字节寻址内存等要求。

其中 `resources_course_compat/` 已经包含用于报告和提交检查的关键波形截图：

- `hazard_raw.png` / `hazard_raw_explain.txt`：展示 RAW 相关和 forwarding。
- `hazard_load-use.png` / `hazard_load-use_explain.txt`：展示 load-use stall。
- `hazard_flush.png` / `hazard_flush_explain.txt`：展示分支/跳转 flush。

这些图片可直接作为作业要求中“每个程序至少一张关键波形，展示冒险处理”的证明材料。

## 6. 可选：怎么跑完整 Verilator CPU 仿真

`verilator-sim/` 是额外提供的完整 CPU 仿真环境，不是作业默认 Vivado 流程。它使用 `generated-verilog/simulation-course-dpi/` 里的仿真顶层 RTL，支持批处理运行、日志、性能计数和 NEMU difftest。

安装依赖：

```bash
sudo apt update
sudo apt install -y make g++ gcc verilator libreadline-dev
```

编译仿真器：

```bash
cd verilator-sim
make
```

运行一个预置 benchmark：

```bash
make run IMG=../release/benchmarks/quick-sort/quick-sort-riscv32e-npc.bin
```

运行 NEMU difftest：

```bash
make rundiff IMG=../release/benchmarks/add-longlong/add-longlong-riscv32e-npc.bin
```

这里的 `make` 只调用 Verilator 编译已生成 RTL，不会调用 Chisel、Scala 或 SBT。

## 7. 可选：怎么使用 Abstract Machine 写 C 程序

`release/` 里提供了一个简化的 AM SDK 使用入口，这是超出作业基础要求的增强开发环境。它可以用真实 RISC-V GCC 编译 C 程序，再放到 CourseCPU 上运行。

先进入 release：

```bash
cd release
```

运行预置程序：

```bash
./run.sh benchmarks/quick-sort/quick-sort-riscv32e-npc.bin
```

运行带 difftest 的程序：

```bash
./run-diff.sh benchmarks/add-longlong/add-longlong-riscv32e-npc.bin
```

编译并运行一个 C smoke 程序：

```bash
./build-sdk-smoke.sh
./run.sh sdk-smoke/smoke-riscv32e-npc.bin
```

`release/README.md` 中写了更详细的依赖说明，包括 Linux glibc 建议、RISC-V GCC 安装命令、AM SDK 目录结构和脚本用法。

## 8. Vivado/PPA/报告资料

- 综合 RTL：`generated-verilog/synthesis-course/`
- Vivado 时序、资源、功耗数据：`reports/`
- 实验报告源码：`report/CourseCPU_report.tex`
- 报告图片：`report/report_assets/`
- 架构图：`report/figures/`

如果按作业传统 RTL 流程检查，请优先使用 `generated-verilog/readmem-compatible-course/` 和 `verilog-testbench/course_compat/`；如果想查看我们额外提供的工业化验证能力，再使用 `verilator-sim/` 和 `release/`。
