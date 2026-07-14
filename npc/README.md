# NPC

NPC 是陈凯新（25050151）实现的 RV32E 处理器工程。处理器使用 Chisel 描述，
通过 Verilator 构建仿真模型，可独立运行，也可接入 ysyxSoC。仿真环境支持 SDB、
指令与函数 trace、差分测试、区间 VCD 和性能计数器。

## 目录结构

| 目录或文件 | 作用 |
| --- | --- |
| `src/main/scala/` | Chisel RTL 源码。 |
| `src/main/scala/mycpu/core/` | 流水线 Core，包含前端、后端、流水寄存器、CSR、冒险处理和 Bundle 定义。 |
| `src/main/scala/mycpu/cache/` | ICache、CacheSet、替换策略和 Cache 参数。 |
| `src/main/scala/mycpu/memory/` | 统一访存 Bundle、MemoryController 和 NPC AXI RAM。 |
| `src/main/scala/mycpu/dpi/` | Scala/RTL 侧 DPI BlackBox 与仿真计数器 API。 |
| `src/main/scala/mycpu/utils/` | AXI4 定义、桥接器、仲裁器及通用调试工具。 |
| `src/main/verilog/Core/NpcTop.sv` | 手写 NPC 仿真顶层。其余 RTL 由 Chisel 生成到 `build/`。 |
| `src/test/` | ChiselTest/ScalaTest 测试、测试程序和兼容性 Verilog testbench。 |
| `src/formal/` | 形式验证脚本与准备后的验证输入。 |
| `csrc/` | C/C++ 仿真运行时，包括 DUT 驱动、内存、DPI、SDB、trace 和 difftest。 |
| `cachesim/` | 可独立运行、也可链接到仿真器的 Cache 参考模型。 |
| `mk/` | 分模块 Makefile：配置、工具链、Chisel、Verilator、NVBoard。 |
| `build.mill` | 当前 Chisel/Mill 构建定义与 RTL 生成入口。 |
| `build.sbt`, `project/` | Scala/SBT 测试和编辑器支持。 |
| `Kconfig`, `.config` | 仿真功能配置，包括 trace、difftest、SDB 和 watchpoint。 |
| `STA/`, `yosys-sta.sh` | Yosys 综合、静态时序分析及其结果。 |
| `build/`, `out/`, `target/` | 生成的 RTL、仿真 binary 和 Scala 构建产物。 |

## 配置与构建

```bash
cd $NPC_HOME
make menuconfig             # 配置 trace、difftest、SDB 等功能
make npc                    # 构建独立 NPC，生成 ./npc
make npc SIM_TARGET=ysyxsoc # 构建 ysyxSoC 仿真目标
```

生成的可执行文件位于：

```text
build/sim/npc/bin/npc
build/sim/ysyxsoc/bin/npc
```

根目录的 `./npc` 是当前构建目标 binary 的符号链接。Chisel RTL 默认生成到
`build/sim/<target>/rtl/Core/`，不应提交回 `src/main/verilog/`。

其他常用目标：

```bash
make rtl                    # 只生成 Chisel RTL
make rundiff IMG=program.bin
make gdb-diff IMG=program.bin
make compdb                 # 生成 compile_commands.json
make clean
```

## 运行

```bash
./npc [OPTIONS] IMAGE
```

例如：

```bash
./npc -b --log=log.txt program.bin
./npc --diff=$NEMU_HOME/build/riscv32-nemu-interpreter-so program.bin
```

完整参数和 SDB 命令见 `csrc/README.md`，Cache trace 分析见
`cachesim/README.md`。

## 测试

Scala 单元测试可通过 Mill 或 SBT 运行；具体 suite 位于 `src/test/scala/`。
端到端测试通常由 Abstract Machine 发起：

```bash
cd $YSYX_HOME/am-kernels/tests/cpu-tests
make ARCH=riscv32e-npc run ALL=add

cd $YSYX_HOME/am-kernels/benchmarks/microbench
make ARCH=riscv32e-ysyxsoc run mainargs=train
```
