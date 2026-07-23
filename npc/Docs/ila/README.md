# DPI-ILA

NPC 的 DPI-ILA 分为采样源（Source）和捕获任务（Capture）两层：Chisel 在时钟沿原子采样一个 Source，C++ 的 Source Bank 负责分配稳定 ID、收集 schema 和接收样本；Capture 再按需引用一个或多个 Source，维护触发窗口并导出一个 VCD。

## Chisel Source API

```scala
DpiApi.ila(
  clock = clock,
  reset = reset.asBool,
  enabled = enableDpi,
  source = "lsu",
  probes = Seq(
    DpiApi.ilaProbe("valid", io.mem.a.valid.asUInt),
    DpiApi.ilaProbe("addr", io.mem.a.bits.addr),
  ),
)
```

一个 Source 绑定一个采样时钟。probe 名称在 Source 内唯一，支持任意位宽 `UInt`。DPI 调用 `ila_source_allocate()` 时只登记 Source；没有 Capture 监视该 Source 时，样本回调不会分配环形缓冲。

## C++ Capture 配置

默认配置位于 `csrc/profiles/ila/default.h`。Capture 可以同时监视多个已经注册的 Source：

```cpp
static inline bool ps2ReadTrigger(const IlaFrameView &frame) {
  const IlaSourceView ps2 = frame.source("ps2Chisel");

  return ps2["io_in_psel"] != 0 && ps2["io_in_penable"] != 0;
}

static inline bool configureDefaultIla(IlaEngine &ila,
                                       std::string &error) {
  return ila.configureCapture(
      "ps2_read",
      {"Core", "ps2Chisel"},
      4096,
      50,
      "ps2ReadTrigger",
      ps2ReadTrigger,
      "build/ila-ps2-read.vcd",
      true,
      error);
}
```

DPI 回调只更新 Source Bank。每次 `dut.stepCycle()` 返回后，`IlaEngine::finishCycle()` 才把同一时间戳的 Source 样本组成完整 frame，并在 frame 上判断 trigger。因此 Capture 不依赖多个 DPI 回调的执行顺序。

`frame.source("name")["probe"]` 返回 probe 的低 64 bit；更宽信号可通过 `word("probe", index)` 分段读取，`nonzero("probe")` 可判断任意位宽信号是否非零。

`trigger_position` 表示期望的触发位置，也就是最多保留多少预触发历史。ILA 从第一个完整 frame 起判断 trigger；若提前命中，会继续采样直到总数达到 `depth`，VCD 中的 `ila_trigger` 标记实际触发位置。

一个 Capture 只生成一个 VCD。每个 Source 对应其中的一个子 scope，例如：

```text
ps2_read
├── Core
│   └── AXI probes
└── ps2Chisel
    └── APB/FIFO probes
```

`ysyxsoc` 构建会自动包含默认 header，运行时无需指定配置文件：

```bash
./npc IMAGE
```

SDB 命令面向 Capture：

```text
ila status all
ila trigger off ps2_read
ila trigger on ps2_read
c
```

触发后会继续采满窗口，再覆盖 Capture 指定的 VCD 并返回 SDB。若 trigger 保持开启，下一次 `c` 或 `si` 会自动重新武装。