#include "verilated.h"
#include "svdpi.h"
#include "VDpiTracerSmokeTop.h"

#include <cstdio>
#include <cstdint>

static bool g_ebreak_seen = false;
static int g_skip_count = 0;
static int g_commit_count = 0;

extern "C" void difftest_skip_ref_cpp() {
  g_skip_count++;
  std::printf("[dpi-smoke] difftest_skip_ref_cpp() count=%d\n", g_skip_count);
}

extern "C" void dpi_update_state(
    int pc,
    int dnpc,
    const svBitVecVal* gprs,
    int mtvec,
    int mepc,
    int mstatus,
    int mcause,
    int inst) {
  g_commit_count++;
  std::printf(
      "[dpi-smoke] commit=%d pc=0x%08x dnpc=0x%08x inst=0x%08x x1=0x%08x mtvec=0x%08x mepc=0x%08x mstatus=0x%08x mcause=0x%08x\n",
      g_commit_count,
      static_cast<uint32_t>(pc),
      static_cast<uint32_t>(dnpc),
      static_cast<uint32_t>(inst),
      static_cast<uint32_t>(gprs[1]),
      static_cast<uint32_t>(mtvec),
      static_cast<uint32_t>(mepc),
      static_cast<uint32_t>(mstatus),
      static_cast<uint32_t>(mcause));
}

extern "C" void ebreak() {
  g_ebreak_seen = true;
  std::printf("[dpi-smoke] ebreak()\n");
}

static void step_cycle(VDpiTracerSmokeTop* top) {
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  auto* top = new VDpiTracerSmokeTop;

  top->reset = 1;
  top->io_start = 0;
  for (int i = 0; i < 4; ++i) {
    step_cycle(top);
  }

  top->reset = 0;
  top->io_start = 1;
  step_cycle(top);
  top->io_start = 0;

  int cycles = 0;
  while (!Verilated::gotFinish() && cycles < 200) {
    step_cycle(top);
    cycles++;
    if (top->io_done && g_skip_count >= 1 && g_commit_count >= 2 && g_ebreak_seen) {
      break;
    }
  }

  std::printf(
      "[dpi-smoke] summary skip=%d commits=%d ebreak=%d cycles=%d done=%d\n",
      g_skip_count,
      g_commit_count,
      g_ebreak_seen ? 1 : 0,
      cycles,
      top->io_done ? 1 : 0);

  delete top;
  return (top->io_done && g_skip_count >= 1 && g_commit_count >= 2 && g_ebreak_seen) ? 0 : 1;
}
