#include "runtime.h"

#include <cstdlib>
#include <cstdio>
#include <sys/time.h>
#include <unistd.h>

#ifdef CONFIG_NPC_VIRTUAL_SOC
#include "VNpcTop.h"
#else
#include "VysyxSoCFull.h"
#endif
#include "cpu.h"
#include "sim.h"
#include "trace/ftrace.h"
#include "trace/itrace.h"

#ifdef CONFIG_BOARD
#include <nvboard.h>
void nvboard_bind_all_pins(VerilatedDut *top);
#endif

namespace {
const Runtime *activeRuntime = nullptr;
}

Runtime::Runtime() {
  activeRuntime = this;
}

Runtime::~Runtime() {
  if (activeRuntime == this) {
    activeRuntime = nullptr;
  }
}

void Runtime::initVerilator(int argc, char *argv[]) {
  (void)argc;
  (void)argv;
  top = new VerilatedDut;

#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  printf("nvboard initing...\n");
#ifdef NVBOARD_RESOURCE_HOME
  if (getenv("NVBOARD_HOME") == nullptr) {
    setenv("NVBOARD_HOME", NVBOARD_RESOURCE_HOME, 0);
  }
#endif
  nvboard_bind_all_pins(top);
  nvboard_init();
#endif
}

void Runtime::shutdown() {
#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  nvboard_quit();
#endif
  if (top != nullptr) {
    top->final();
    delete top;
    top = nullptr;
  }
}

uint64_t Runtime::getTimeInternal() const {
  struct timeval now;
  gettimeofday(&now, nullptr);
  return static_cast<uint64_t>(now.tv_sec) * 1000000ull + static_cast<uint64_t>(now.tv_usec);
}

uint64_t Runtime::getTime() {
  if (bootTime == 0) bootTime = getTimeInternal();
  return getTimeInternal() - bootTime;
}

void Runtime::stepOneClk() {
  static uint64_t totalSimCycles = 0;

  top->clock = 0;
  top->eval();
  top->clock = 1;
  top->eval();

#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  nvboard_update();
  static int traceUart = -1;
  static uint8_t lastUartRx = 0xffu;
  static uint8_t lastUartTx = 0xffu;
  if (traceUart < 0) {
    traceUart = getenv("NPC_TRACE_UART") != nullptr;
  }
  if (traceUart) {
    uint8_t uartRx = top->externalPins_uart_rx;
    uint8_t uartTx = top->externalPins_uart_tx;
    if (uartRx != lastUartRx || uartTx != lastUartTx) {
      printf("[nvboard-uart] cycle=%lld rx=%u tx=%u\n",
             static_cast<long long>(totalSimCycles), uartRx & 1u, uartTx & 1u);
      lastUartRx = uartRx;
      lastUartTx = uartTx;
    }
  }
#endif

  totalSimCycles++;
  // if (totalSimCycles % 1000 == 0) {
  //   uint64_t realTimeUs = getTime();
  //   uint64_t expectedSimUs = totalSimCycles;
  //   if (expectedSimUs > realTimeUs) {
  //     usleep(expectedSimUs - realTimeUs);
  //   }
  // }
}

void Runtime::resetCpu(int n) {
  top->reset = 1;
  for (int i = 0; i < n; ++i) {
    stepOneClk();
  }
  top->reset = 0;
  top->eval();
}

void Runtime::syncAfterLoad() {}
void Runtime::setDpiScope() {}

void Runtime::assertFailMsg() const {
  cpu.isa_reg_display();
  print_iring_buffer();
  print_ftrace_stack();
}

void Runtime::setRunning() { npcState.state = NPC_RUNNING; }
void Runtime::setStop() { npcState.state = NPC_STOP; }
void Runtime::setEnd(uint64_t haltRet) {
  npcState.state = NPC_END;
  npcState.halt_ret = haltRet;
}
void Runtime::setAbort(uint64_t haltRet) {
  npcState.state = NPC_ABORT;
  npcState.halt_ret = haltRet;
}
void Runtime::setQuit() { npcState.state = NPC_QUIT; }
bool Runtime::isRunning() const { return npcState.state == NPC_RUNNING; }
bool Runtime::hasEnded() const { return npcState.state == NPC_END || npcState.state == NPC_ABORT; }

int Runtime::isExitStatusBad() const {
  int good = (npcState.state == NPC_END && npcState.halt_ret == 0) ||
             (npcState.state == NPC_QUIT);
  return !good;
}

const NpcState &Runtime::state() const { return npcState; }

extern "C" void assert_fail_msg() {
  if (activeRuntime == nullptr) {
    fprintf(stderr, "Runtime is not initialized\n");
    abort();
  }
  activeRuntime->assertFailMsg();
}
