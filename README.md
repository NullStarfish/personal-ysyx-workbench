# 一生一芯工作台

作者：陈凯新

一生一芯学号：25050151

本仓库是一生一芯学习与开发工作台，包含处理器 RTL、仿真器、参考模型、
Abstract Machine 软件栈以及 SoC 集成环境。各子项目相互配合，但多数目录也可
独立构建。

## 目录结构

| 目录 | 作用 |
| --- | --- |
| `npc/` | 自研 RV32E 处理器。包含 Chisel RTL、Verilator 仿真运行时、SDB、差分测试、Cache 模型和形式验证。 |
| `nemu/` | NEMU 指令集模拟器，主要作为 NPC 的差分测试参考模型，也可独立运行 AM 程序。 |
| `abstract-machine/` | Abstract Machine 硬件抽象层，提供 TRM、IOE、CTE、VME、MPE 等接口及各 ARCH 的运行时支持。 |
| `am-kernels/` | 基于 Abstract Machine 的测试与应用，包括 microbench、coremark 和各类功能测试。 |
| `ysyxSoC/` | 一生一芯 SoC 集成工程，负责将处理器接入总线、外设和 SoC 顶层。 |
| `fceux-am/` | 移植到 Abstract Machine 的 FCEUX NES 模拟器，用于较完整的软件负载测试。 |
| `.vscode/` | 工作台级 VS Code 配置。 |
| `Makefile` | 工作台公共 Makefile，向子项目提供 git、构建和运行辅助规则。 |
| `init.sh` | 一生一芯子项目初始化脚本。 |

`.cache/`、`.metals/`、`compile_commands.json` 等是编辑器或工具生成的文件，不是
工程源码。

## 常用环境变量

各项目的 Makefile 通常依赖以下路径变量：

```bash
export YSYX_HOME=$HOME/personal-ysyx-workbench
export NPC_HOME=$YSYX_HOME/npc
export NEMU_HOME=$YSYX_HOME/nemu
export AM_HOME=$YSYX_HOME/abstract-machine
export SOC_HOME=$YSYX_HOME/ysyxSoC
```

## 快速开始

构建 NPC：

```bash
cd $NPC_HOME
make menuconfig
make npc
```

运行一个 AM 程序：

```bash
cd $YSYX_HOME/am-kernels/tests/cpu-tests
make ARCH=riscv32e-npc run ALL=add
```

使用 ysyxSoC 目标时，将 ARCH 换为 `riscv32e-ysyxsoc`。NPC 的详细构建方式、
模拟器参数和调试命令分别见 `npc/README.md` 与 `npc/csrc/README.md`。

## 参考资料

- [一生一芯实验讲义](https://ysyx.oscc.cc/docs/)
- 各子项目目录中的 README
