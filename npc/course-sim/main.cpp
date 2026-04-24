#include <cassert>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cstdint>
#include <map>
#include <string>

#include "verilated.h"
#include "VCourseTop.h"
#include "svdpi.h"

#include "difftest/dut.h"

extern "C" {
#include "monitor.h"
#include "state.h"
#include "sdb/sdb.h"
#include "trace/itrace.h"
#include "trace/ftrace.h"
#include "readline/readline.h"
#include "reg.h"
}

struct CpuState {
  uint32_t pc;
  uint32_t dnpc;
  uint32_t inst;
  uint32_t gpr[32];
  struct {
    uint32_t mtvec;
    uint32_t mepc;
    uint32_t mstatus;
    uint32_t mcause;
  } csrs;
} g_cpu_state;

static VCourseTop* top_ptr = nullptr;
static uint8_t* sram = nullptr;

static constexpr uint32_t PROGRAM_BASE = 0x80000000u;
static constexpr uint32_t SRAM_SIZE = 0x01000000u;
static constexpr uint32_t MMIO_BASE = 0xa0000000u;
static constexpr uint32_t SERIAL_PORT = MMIO_BASE + 0x000003f8u;

static uint32_t g_last_retire_pc = 0;
static uint32_t g_last_retire_inst = 0;
static bool g_has_committed = false;
static long long cycle_count = 0;
static long long instr_count = 0;
static long long flush_count = 0;

extern "C" void dpi_update_state(int pc, int dnpc, int reg_wen, int reg_addr, int reg_data, const svBitVecVal* gprs,
                                 int mtvec, int mepc, int mstatus, int mcause, int inst) {
  g_last_retire_pc = static_cast<uint32_t>(pc);
  g_last_retire_inst = static_cast<uint32_t>(inst);

  g_cpu_state.pc = static_cast<uint32_t>(dnpc);
  g_cpu_state.dnpc = static_cast<uint32_t>(dnpc);
  g_cpu_state.inst = static_cast<uint32_t>(inst);

  for (int i = 0; i < 32; ++i) {
    g_cpu_state.gpr[i] = gprs[i];
  }
  if (reg_wen && reg_addr != 0) {
    g_cpu_state.gpr[reg_addr & 0x1f] = static_cast<uint32_t>(reg_data);
  }

  g_cpu_state.csrs.mtvec = static_cast<uint32_t>(mtvec);
  g_cpu_state.csrs.mepc = static_cast<uint32_t>(mepc);
  g_cpu_state.csrs.mstatus = static_cast<uint32_t>(mstatus);
  g_cpu_state.csrs.mcause = static_cast<uint32_t>(mcause);

  g_has_committed = true;
}

extern "C" void difftest_skip_ref_cpp() {
#ifdef CONFIG_DIFFTEST
  difftest_skip_ref();
#endif
}

extern "C" void dpi_record_flush() {
  flush_count++;
}

extern "C" void ebreak() {
  uint32_t a0_val = g_cpu_state.gpr[10];
  npc_state.state = (a0_val == 0) ? NPC_END : NPC_ABORT;
  npc_state.halt_ret = a0_val;
  std::printf("ebreak: state: %d, a0: %d\n", npc_state.state, a0_val);
}

static inline bool sram_in_range(uint32_t addr, uint32_t bytes = 1) {
  return addr >= PROGRAM_BASE && static_cast<uint64_t>(addr - PROGRAM_BASE) + bytes <= SRAM_SIZE;
}

static inline uint32_t load_u32(uint32_t addr) {
  if (!sram_in_range(addr, 4)) return 0;
  uint32_t off = addr - PROGRAM_BASE;
  return static_cast<uint32_t>(sram[off + 0]) |
         (static_cast<uint32_t>(sram[off + 1]) << 8) |
         (static_cast<uint32_t>(sram[off + 2]) << 16) |
         (static_cast<uint32_t>(sram[off + 3]) << 24);
}

extern "C" int course_sram_ifetch(int addr) {
  return static_cast<int>(load_u32(static_cast<uint32_t>(addr)));
}

extern "C" int course_sram_read(int addr, unsigned char subop, unsigned char unsigned_load) {
  const uint32_t uaddr = static_cast<uint32_t>(addr);
  if (uaddr == SERIAL_PORT) return 0;
  switch (subop & 0x7u) {
    case 0x1: {
      if (!sram_in_range(uaddr, 1)) return 0;
      uint8_t value = sram[uaddr - PROGRAM_BASE];
      if (unsigned_load & 0x1u) return static_cast<int>(value);
      return static_cast<int>(static_cast<int32_t>(static_cast<int8_t>(value)));
    }
    case 0x2: {
      if (!sram_in_range(uaddr, 2)) return 0;
      uint32_t off = uaddr - PROGRAM_BASE;
      uint16_t value = static_cast<uint16_t>(sram[off + 0]) |
                       (static_cast<uint16_t>(sram[off + 1]) << 8);
      if (unsigned_load & 0x1u) return static_cast<int>(value);
      return static_cast<int>(static_cast<int32_t>(static_cast<int16_t>(value)));
    }
    default:
      return static_cast<int>(load_u32(uaddr));
  }
}

extern "C" void course_sram_write(int addr, int data, unsigned char subop) {
  const uint32_t uaddr = static_cast<uint32_t>(addr);
  if (uaddr == SERIAL_PORT) {
    std::putchar(data & 0xff);
    std::fflush(stdout);
    return;
  }
  if (!sram_in_range(uaddr, 1)) return;
  const uint32_t off = uaddr - PROGRAM_BASE;
  switch (subop & 0x7u) {
    case 0x1:
      sram[off] = static_cast<uint8_t>(data & 0xff);
      break;
    case 0x2:
      if (!sram_in_range(uaddr, 2)) return;
      sram[off + 0] = static_cast<uint8_t>(data & 0xff);
      sram[off + 1] = static_cast<uint8_t>((data >> 8) & 0xff);
      break;
    default:
      if (!sram_in_range(uaddr, 4)) return;
      sram[off + 0] = static_cast<uint8_t>(data & 0xff);
      sram[off + 1] = static_cast<uint8_t>((data >> 8) & 0xff);
      sram[off + 2] = static_cast<uint8_t>((data >> 16) & 0xff);
      sram[off + 3] = static_cast<uint8_t>((data >> 24) & 0xff);
      break;
  }
}

static void print_stats() {
  std::printf("\nExecution Statistics:\n");
  std::printf("  Total Cycles:       %lld\n", cycle_count);
  std::printf("  Total Instructions: %lld\n", instr_count);
  std::printf("  Total Flushes:      %lld\n", flush_count);
  if (cycle_count > 0) {
    std::printf("  Average IPC:        %f\n", static_cast<double>(instr_count) / cycle_count);
  }
  if (instr_count > 0) {
    std::printf("  Flush Probability:  %f\n", static_cast<double>(flush_count) / instr_count);
  }
}

static void handle_sigint(int) {
  std::printf("\n\nCaught Ctrl+C (SIGINT). Terminating simulation...\n");
  print_stats();
  delete top_ptr;
  free(sram);
  std::exit(0);
}

extern "C" {

void assert_fail_msg() {
  isa_reg_display();
  print_iring_buffer();
  print_ftrace_stack();
}

void sync_after_load() {}

long long get_cycle_count() { return cycle_count; }

void init_verilator(int argc, char* argv[]) {
  sram = static_cast<uint8_t*>(std::malloc(SRAM_SIZE));
  assert(sram);
  std::memset(sram, 0, SRAM_SIZE);
  Verilated::commandArgs(argc, argv);
  top_ptr = new VCourseTop;
}

uint32_t get_pc_cpp() { return g_cpu_state.pc; }
uint32_t get_retire_pc_cpp() { return g_last_retire_pc; }
uint32_t get_inst_cpp() { return g_last_retire_inst; }

void set_dpi_scope() {}

static void step_one_clk() {
  top_ptr->clock = 0;
  top_ptr->eval();
  top_ptr->clock = 1;
  top_ptr->eval();
}

void exec_one_cycle_cpp() {
  g_has_committed = false;
  while (!g_has_committed && npc_state.state == NPC_RUNNING) {
    step_one_clk();
    cycle_count++;
  }
  if (npc_state.state == NPC_RUNNING) {
    instr_count++;
  }
}

void reset_cpu(int n) {
  top_ptr->reset = 1;
  for (int i = 0; i < n; ++i) {
    step_one_clk();
  }
  top_ptr->reset = 0;
  top_ptr->eval();
}

void init_cpu() {
  g_cpu_state.pc = PROGRAM_BASE;
  g_cpu_state.dnpc = PROGRAM_BASE;
  g_cpu_state.csrs.mstatus = 0x1800;
}

void load_data_to_rom(const uint8_t* data, size_t size) {
  assert(size <= SRAM_SIZE);
  std::memcpy(sram, data, size);
}

uint32_t isa_reg_read_cpp(int reg_num) {
  if (reg_num >= 0 && reg_num < 32) return g_cpu_state.gpr[reg_num];
  return 0;
}

uint32_t isa_get_csrs(int csr_num) {
  switch (csr_num) {
    case 0x300: return g_cpu_state.csrs.mstatus;
    case 0x305: return g_cpu_state.csrs.mtvec;
    case 0x341: return g_cpu_state.csrs.mepc;
    case 0x342: return g_cpu_state.csrs.mcause;
    default: return 0;
  }
}

uint32_t isa_reg_str2val_cpp(const char* s, bool* success) {
  static const std::map<std::string, int> reg_map = {
      {"pc", -1}, {"zero", 0}, {"ra", 1}, {"sp", 2}, {"gp", 3}, {"tp", 4},
      {"t0", 5}, {"t1", 6}, {"t2", 7}, {"s0", 8}, {"s1", 9}, {"a0", 10},
      {"a1", 11}, {"a2", 12}, {"a3", 13}, {"a4", 14}, {"a5", 15}, {"a6", 16},
      {"a7", 17}, {"s2", 18}, {"s3", 19}, {"s4", 20}, {"s5", 21}, {"s6", 22},
      {"s7", 23}, {"s8", 24}, {"s9", 25}, {"s10", 26}, {"s11", 27},
      {"t3", 28}, {"t4", 29}, {"t5", 30}, {"t6", 31}
  };
  *success = true;
  std::string str(s);
  if (reg_map.count(str)) {
    int reg_num = reg_map.at(str);
    return (reg_num == -1) ? get_pc_cpp() : isa_reg_read_cpp(reg_num);
  }
  if (str.length() > 1 && str[0] == 'x') {
    try {
      int reg_num = std::stoi(str.substr(1));
      if (reg_num >= 0 && reg_num < 32) return isa_reg_read_cpp(reg_num);
    } catch (...) {}
  }
  *success = false;
  return 0;
}

void get_dut_regstate_cpp(riscv32_CPU_state* dut) {
  if (!dut) return;
  dut->pc = g_cpu_state.dnpc;
  std::memcpy(dut->gpr, g_cpu_state.gpr, sizeof(dut->gpr));
  dut->csrs.mtvec = g_cpu_state.csrs.mtvec;
  dut->csrs.mepc = g_cpu_state.csrs.mepc;
  dut->csrs.mstatus = g_cpu_state.csrs.mstatus;
  dut->csrs.mcause = g_cpu_state.csrs.mcause;
}

void pmem_read_chunk(uint32_t addr, uint8_t* buf, size_t n) {
  if (!buf) return;
  if (!sram_in_range(addr, static_cast<uint32_t>(n))) return;
  std::memcpy(buf, sram + (addr - PROGRAM_BASE), n);
}

int pmem_read(int raddr) {
  return static_cast<int>(load_u32(static_cast<uint32_t>(raddr) & ~0x3u));
}

void pmem_write(int waddr, int wdata, char wmask) {
  uint32_t addr = static_cast<uint32_t>(waddr) & ~0x3u;
  if (!sram_in_range(addr, 4)) return;
  uint32_t off = addr - PROGRAM_BASE;
  uint32_t old = load_u32(addr);
  uint32_t mask = 0;
  for (int i = 0; i < 4; ++i) {
    if (wmask & (1 << i)) mask |= 0xffu << (i * 8);
  }
  uint32_t next = (old & ~mask) | (static_cast<uint32_t>(wdata) & mask);
  sram[off + 0] = static_cast<uint8_t>(next & 0xff);
  sram[off + 1] = static_cast<uint8_t>((next >> 8) & 0xff);
  sram[off + 2] = static_cast<uint8_t>((next >> 16) & 0xff);
  sram[off + 3] = static_cast<uint8_t>((next >> 24) & 0xff);
}

}  // extern "C"

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  init_monitor(argc, argv);
  rl_catch_signals = 0;
  signal(SIGINT, handle_sigint);
  sdb_mainloop();
  print_stats();
  delete top_ptr;
  free(sram);
  std::printf("Simulation finished after %lld execution cycles.\n", cycle_count);
  return is_exit_status_bad();
}
