# CourseCompatibleTop Verilog Testbench

这个目录是给传统作业检查使用的 Verilog/SystemVerilog testbench，默认面向 Vivado RTL 仿真。它不依赖 DPI，也不依赖 Chisel/SBT，直接使用提交包中已经生成好的 readmem 兼容 RTL。目录中的脚本只是可选的 Verilator 快速回归方式，不是作业必须流程。

## 文件对应关系

- CPU 顶层 RTL：`../../generated-verilog/readmem-compatible-course/CourseCompatibleTop.sv`
- 指令存储器：`CompatibleIMem.sv`
- 数据存储器：`CompatibleDMem.sv`
- Testbench：`tb_CourseCompatibleTop.sv`
- 测试程序：`../resources_course_compat/*.S` 和 `../resources_course_compat/*.mem`
- 波形截图与解释：`../resources_course_compat/*.png` 和 `../resources_course_compat/*_explain.txt`

`CompatibleIMem.sv` 通过 `+IMEM=<path>` 读取 `.mem` 文件；`CompatibleDMem.sv` 是字节寻址内存，用来验证 `LW/SW/LB/SB` 等访存指令。

## 在 Vivado 中运行

1. 新建 Vivado RTL Project。
2. 添加 `../../generated-verilog/readmem-compatible-course/*.sv`。
3. 添加 `CompatibleIMem.sv` 和 `CompatibleDMem.sv`。
4. 添加 `tb_CourseCompatibleTop.sv` 到 Simulation Sources。
5. 设置 simulation top 为 `tb_CourseCompatibleTop`。
6. 在仿真 plusargs 中指定程序，例如：

```text
+TEST=hazard_load_use +IMEM=../resources_course_compat/hazard_load_use.mem +MAX_CYCLES=300
```

如果没有传 `+IMEM=...`，`CompatibleIMem.sv` 会默认填充 NOP，因此 Vivado 仿真时需要设置 plusargs。若 GUI 不方便设置 plusargs，可以临时在 `CompatibleIMem.sv` 的 `initial` 块中固定 `$readmemh` 路径，并在 `tb_CourseCompatibleTop.sv` 中固定 `test_name`。

## 可选：用 Verilator 运行全部测试

```bash
cd CourseCPU_teacher_submission/verilog-testbench/course_compat
make test
```

脚本会编译 readmem 兼容 RTL 和本目录 testbench，然后依次运行：

- `smoke`
- `hazard_raw`
- `hazard_load_use`
- `hazard_flush`
- `program_arith_upper`
- `program_mem_byte_half`
- `rv32i_upper_jump`
- `rv32i_alu_branch`
- `rv32i_load_store`

每个测试会在 `ebreak` 退休后检查寄存器或内存结果。结果正确会打印 `[test_name] PASS`。

## 已提供的波形截图

作业要求提交关键波形截图。`../resources_course_compat/` 中已经放好了三类冒险相关图片和说明：

- `hazard_raw.png` / `hazard_raw_explain.txt`：RAW 相关和 forwarding。
- `hazard_load-use.png` / `hazard_load-use_explain.txt`：load-use stall。
- `hazard_flush.png` / `hazard_flush_explain.txt`：branch/jump flush。

这些截图可直接放入实验报告或作为提交附件。

## 可选：导出 VCD 波形

导出全部测试波形：

```bash
make wave
```

只导出一个程序的指定周期窗口：

```bash
bash dump_course_compat_wave.sh hazard_load_use 0 20
```

波形位置：

```text
../../build/course_compat/waves/
```

建议查看：

| 测试 | 建议周期 | 观察重点 |
|---|---:|---|
| `hazard_raw` | 0-12 | 连续 ALU 指令 RAW 相关，forwarding 生效 |
| `hazard_load_use` | 0-20 | load-use 冒险，流水线插入 stall |
| `hazard_flush` | 0-20 | 分支/跳转 redirect 后错误路径被 flush |
| `rv32i_alu_branch` | 10-45 | BEQ/BNE/BLT/BGE 等分支真假路径 |
| `rv32i_load_store` | 0-20 | 字节/半字/字访存和符号扩展 |

## 可选：单独手动运行一个 Verilator 测试

如果不想用脚本，也可以参考下面的 Verilator 命令：

```bash
cd CourseCPU_teacher_submission
verilator --binary --timing \
  --top-module tb_CourseCompatibleTop \
  -Wno-DECLFILENAME -Wno-WIDTHEXPAND -Wno-WIDTHTRUNC \
  generated-verilog/readmem-compatible-course/*.sv \
  verilog-testbench/course_compat/CompatibleIMem.sv \
  verilog-testbench/course_compat/CompatibleDMem.sv \
  verilog-testbench/course_compat/tb_CourseCompatibleTop.sv

./obj_dir/Vtb_CourseCompatibleTop \
  +TEST=hazard_load_use \
  +IMEM=verilog-testbench/resources_course_compat/hazard_load_use.mem \
  +MAX_CYCLES=300
```
