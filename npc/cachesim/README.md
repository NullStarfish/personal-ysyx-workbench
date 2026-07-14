# CacheSim

CacheSim 是一个轻量 Cache 参考模型，可按地址序列计算每次访问的 hit/miss、映射的
tag/index/offset、替换 way 和最终命中率。当前替换策略是基于 `lastUsed` 的 LRU。

它有两种用途：

1. `cachesim` 命令行工具，用于离线分析 PC 或访存地址序列。
2. `cache_sim.cpp` 静态库，通过 `sources.mk` 链接进 NPC 仿真器，供 C++ 侧复用。

## 构建

```bash
cd $NPC_HOME/cachesim
make
make test
```

生成的独立工具是 `./cachesim`。清理构建产物使用 `make clean`。

## 基本用法

```text
./cachesim [OPTIONS] [ADDR_FILE]
```

省略 `ADDR_FILE` 时从标准输入读取地址。支持的输入形式包括：

```text
0xa0000000
pc 0xa0000004
fetch,0xa0000008
addr=0xa000000c
# comment
```

每行提取遇到的第一个合法整数作为 32-bit 地址，空行和 `#` 注释会被忽略。

## 配置参数

| 参数 | 默认值 | 作用 |
| --- | ---: | --- |
| `--capacity BYTES` | 4096 | Cache 总容量，单位为 byte。`--capacity-bytes` 是等价写法。 |
| `--ways N` | 1 | 组相联 way 数。 |
| `--index-bits N` | 8 | Cache set index 位数。 |
| `--offset-bits N` | 4 | line 内 offset 位数；默认 line 大小为 16 B。 |
| `--tag-bits N` | 0 | tag 位数；0 表示由 32-bit 地址宽度自动推导。 |
| `--trace` | 关闭 | 逐项打印 hit/miss 和地址分解。 |
| `-h`, `--help` |  | 显示帮助。 |

参数必须满足：容量、way 数和 set 数是 2 的幂，且
`capacity = sets * ways * lineBytes`。因此显式指定 `index-bits` 时，需要与容量、
way 和 offset 保持一致。

## 分析一个地址序列

```bash
./cachesim --capacity 4096 --ways 2 --index-bits 7 \
  --offset-bits 4 --trace pc.txt
```

逐访问输出示例：

```text
addr=0xa0017260 tag=0x14002e index=0x26 offset=0x0 way=0 miss
addr=0xa0017260 tag=0x14002e index=0x26 offset=0x0 way=0 hit
```

最后会打印实际配置及总体统计：

```text
Cache stats: access=2 hit=1 miss=1 hitRate=0.500000 missRate=0.500000
```

查找某个 PC 的命中情况可以直接过滤 trace：

```bash
./cachesim --capacity 4096 --ways 2 --index-bits 7 \
  --offset-bits 4 --trace pc.txt | grep 'addr=0xa0017260'
```

## 从 NPC 导出 PC 序列

先在 `make menuconfig` 中启用 `TRACE -> PC fetch Trace`，重新构建后运行：

```bash
cd $NPC_HOME
make npc
./npc -b --pc-trace=pc.txt program.bin
./cachesim/cachesim --capacity 4096 --ways 2 --index-bits 7 \
  --offset-bits 4 --trace pc.txt > cache-trace.txt
```

当前 NPC 的 `--pc-trace` 在 retire DPI 到达时写入，因此这是提交 PC 序列，不包含
被 flush 的错误路径取指，也不等同于 ICache 的实际 lookup 序列。它适合分析程序
指令流的理论 Cache 行为；若要和 RTL ICache 的访问次数逐项对齐，应从 ICache 请求
握手处导出地址。

## 从 NEMU ITrace 转换

NEMU 启用 ITrace 后可用 `-l` 输出指令日志。其每条指令通常以 `PC:` 开头，可以将
第一列转换成 CacheSim 接受的地址文件：

```bash
$NEMU_HOME/build/riscv32-nemu-interpreter -b -l nemu-itrace.log program.bin
awk -F: '/^[[:space:]]*[0-9a-fA-F]+:/ { gsub(/[[:space:]]/, "", $1); print "0x" $1 }' \
  nemu-itrace.log > nemu-pc.txt
./cachesim --trace nemu-pc.txt
```

如果 NEMU 配置未启用 ITrace，日志中不会产生这类逐指令记录，需要先在 NEMU 中
执行 `make menuconfig` 并重新构建。

## 通过标准输入使用

```bash
printf '0x80000000\n0x80000004\n0x80000000\n' | \
  ./cachesim --capacity 4096 --ways 1 --trace
```

不加 `--trace` 时只输出总体配置和统计，适合处理很长的地址序列。
