# NPC 仿真运行时

`csrc` 是 NPC 的 C/C++ 仿真运行时。它负责驱动 Verilated DUT、装载程序、维护
仿真内存和架构提交状态，并提供 SDB、difftest、trace、VCD 和性能统计。

## 目录结构

| 路径 | 作用 |
| --- | --- |
| `main.cpp` | 程序入口，只负责解析参数并启动 `Runtime`。 |
| `runtime/base/` | CPU 提交状态、宿主时钟、运行状态和 `RetireEvent`。 |
| `runtime/platform/` | Verilated DUT、物理内存/MMIO 和程序镜像。 |
| `runtime/services/` | Logger、NEMU difftest 和仿真计数器。 |
| `runtime/execution/` | 执行循环、提交处理流水和 SIGINT 处理。 |
| `runtime/dpi/` | RTL 与 C++ 之间唯一的 DPI ABI 转发边界。 |
| `runtime/sdb/` | SDB 命令解释器、表达式求值和 watchpoint。 |
| `runtime/traces/` | ITrace、FTrace 及其公共 Trace 基类。 |
| `tools/` | Capstone 反汇编辅助代码。 |
| `constr/` | NVBoard 引脚约束。 |
| `sources.mk` | C/C++ 源文件和静态库构建规则。 |

## Binary 参数

```text
./npc [OPTIONS] IMAGE
```

| 参数 | 作用 |
| --- | --- |
| `IMAGE` | 待运行的裸二进制镜像，装入仿真物理内存。 |
| `-b`, `--batch` | 批处理模式，跳过交互式 SDB 并持续运行到结束或出错。 |
| `-l FILE`, `--log=FILE` | ITrace 等普通日志的输出文件。需要相应 Kconfig 功能。 |
| `-p FILE`, `--pc-trace=FILE` | 输出每条提交指令的 PC 序列。需要 `CONFIG_PCTRACE=y`。 |
| `-f ELF`, `--ftrace=ELF` | 为 FTrace 提供含符号表的 ELF。需要 `CONFIG_FTRACE=y`。 |
| `-d SO`, `--diff=SO` | 指定 NEMU reference shared object。需要 `CONFIG_DIFFTEST=y`。 |
| `-h`, `--help` | 退出并显示简短帮助。 |

常用示例：

```bash
# 交互调试
./npc program.bin

# 批处理并记录指令日志
./npc -b --log=log.txt program.bin

# 差分测试
./npc --diff=$NEMU_HOME/build/riscv32-nemu-interpreter-so program.bin

# 函数调用追踪；ELF 用于读符号，IMAGE 仍是实际装载的 binary
./npc --ftrace=program.elf program.bin

# 导出提交 PC 序列供 cachesim 使用
./npc -b --pc-trace=pc.txt program.bin
```

参数对应的功能需要在 `make menuconfig` 中启用。修改 `.config` 后重新执行
`make npc`。

## SDB 命令

未使用 `-b` 且启用了 `CONFIG_INTERACTIVE_SDB` 时，启动后进入 `(npc)` 提示符。

| 命令 | 作用 |
| --- | --- |
| `help [CMD]` | 列出全部命令，或查看一条命令。 |
| `c` | 连续执行；若当前停在 ebreak，提示使用 `fc`。 |
| `fc` | 强制越过当前 ebreak 暂停并继续执行。 |
| `fsi [N]` | 强制越过当前 ebreak 暂停并执行 N 条提交指令，默认 1 条。 |
| `si [N]` | 单步执行 N 条提交指令，默认 1 条。 |
| `q` | 退出模拟器。 |
| `info r` | 显示 GPR 和 PC。 |
| `info w` | 显示全部 watchpoint。 |
| `p EXPR` | 计算表达式并以十进制和十六进制输出。 |
| `x N EXPR` | 从表达式给出的地址起读取 N 个 32-bit word。 |
| `w EXPR` | 创建 watchpoint；表达式值改变时暂停。 |
| `d N` | 删除编号为 N 的 watchpoint。 |
| `bt` | 显示 FTrace 维护的函数调用栈。 |
| `vcd watch start [FILE]` | 从当前时刻开始记录区间 VCD；默认文件名由 DUT 决定。 |
| `vcd watch end` | 停止记录并关闭当前 VCD。 |
| `vcd watch status` | 查看区间 VCD 状态和路径。 |
| `ila status [CAPTURE|all]` | 查看采样、触发和输出状态。 |
| `ila trigger on|off [CAPTURE|all]` | 启用或关闭触发；关闭时仍维护预触发历史。 |

使用示例：

```text
(npc) p $pc
(npc) p $a0 + 4
(npc) x 8 0xa0000000
(npc) w $pc == 0xa0017260
(npc) si 20
(npc) vcd watch start miss.vcd
(npc) c
(npc) vcd watch end
```

## 表达式语法

SDB 表达式支持：

- 十进制数和 `0x` 十六进制数。
- `$pc`、`$x0` 至 `$x31`，以及 RISC-V ABI 寄存器名，如 `$ra`、`$sp`、`$a0`。
- `+ - * /`、`== !=`、`&& ||` 和括号。
- 一元负号，例如 `-$a0`。
- 一元解引用，例如 `*$sp`，读取该地址处的一个 32-bit word。

最多可创建 32 个 watchpoint，单个 watchpoint 表达式最长 63 个字符。

## VCD 调试建议

完整程序的 VCD 通常很大。已知可疑 PC 时，可以先建立 PC watchpoint，在暂停后
开启 VCD，再单步或继续运行一小段区间：

```text
(npc) w $pc == 0xa0017260
(npc) c
(npc) d 0
(npc) vcd watch start failure.vcd
(npc) si 30
(npc) vcd watch end
```

当前区间 VCD 只能从执行命令后开始记录，不能恢复开始记录之前的波形。
