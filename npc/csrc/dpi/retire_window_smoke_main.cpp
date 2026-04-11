#include "verilated.h"
#include "svdpi.h"
#include "VRetireWindowSmokeTop.h"

#include <cstdint>
#include <cstdio>

static int g_commit_count = 0;
static bool g_failed = false;

static void fail(const char* msg) {
  g_failed = true;
  std::printf("[retire-window] FAIL: %s\n", msg);
}

extern "C" void difftest_skip_ref_cpp() {}

extern "C" void ebreak() {}

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
  const uint32_t x1 = static_cast<uint32_t>(gprs[1]);
  std::printf(
      "[retire-window] commit=%d pc=0x%08x dnpc=0x%08x inst=0x%08x x1=0x%08x mtvec=0x%08x mepc=0x%08x mstatus=0x%08x mcause=0x%08x\n",
      g_commit_count,
      static_cast<uint32_t>(pc),
      static_cast<uint32_t>(dnpc),
      static_cast<uint32_t>(inst),
      x1,
      static_cast<uint32_t>(mtvec),
      static_cast<uint32_t>(mepc),
      static_cast<uint32_t>(mstatus),
      static_cast<uint32_t>(mcause));

  if (g_commit_count == 1) {
    if (static_cast<uint32_t>(pc) != 0x30000000u) fail("commit 1 pc mismatch");
    if (static_cast<uint32_t>(dnpc) != 0x30000004u) fail("commit 1 dnpc mismatch");
    if (x1 != 0u) fail("commit 1 should observe pre-write x1 = 0");
  } else if (g_commit_count == 2) {
    if (static_cast<uint32_t>(pc) != 0x30000004u) fail("commit 2 pc mismatch");
    if (static_cast<uint32_t>(dnpc) != 0x30000008u) fail("commit 2 dnpc mismatch");
    if (x1 != 1u) fail("commit 2 should observe x1 = 1 from previous retire");
  }
}

static void step_cycle(VRetireWindowSmokeTop* top) {
  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv);
  auto* top = new VRetireWindowSmokeTop;

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
  while (!Verilated::gotFinish() && cycles < 32) {
    step_cycle(top);
    cycles++;
    if (top->io_done && g_commit_count >= 2) {
      break;
    }
  }

  std::printf(
      "[retire-window] summary commits=%d cycles=%d done=%d failed=%d\n",
      g_commit_count,
      cycles,
      top->io_done ? 1 : 0,
      g_failed ? 1 : 0);

  const bool pass = top->io_done && g_commit_count == 2 && !g_failed;
  delete top;
  return pass ? 0 : 1;
}
