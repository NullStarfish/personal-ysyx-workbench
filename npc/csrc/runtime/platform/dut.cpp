#include "runtime/platform/dut.h"

#include <cstdio>
#include <cstdlib>
#include <string>

#include <verilated.h>
#include <verilated_vcd_c.h>

#ifdef CONFIG_NPC_VIRTUAL_SOC
#include "VNpcTop.h"
using VerilatedDut = VNpcTop;
#else
#include "VysyxSoCFull.h"
using VerilatedDut = VysyxSoCFull;
#endif

#ifdef CONFIG_BOARD
#include <nvboard.h>
void nvboard_bind_all_pins(VerilatedDut *top);
#endif

class Dut::Impl {
public:
  VerilatedDut *top = nullptr;
  VerilatedVcdC *vcd = nullptr;
  bool nvboardInitialized = false;
  bool vcdWatching = false;
  uint64_t timestamp = 0;
  uint64_t cycles = 0;
  std::string vcdPath;
};

Dut::Dut() : impl(std::make_unique<Impl>()) {}
Dut::~Dut() { shutdown(); }

void Dut::init() {
  if (impl->top != nullptr) return;
  Verilated::traceEverOn(true);
  impl->top = new VerilatedDut;
  impl->vcd = new VerilatedVcdC;
  impl->top->trace(impl->vcd, 99);

#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  printf("nvboard initing...\n");
#ifdef NVBOARD_RESOURCE_HOME
  if (getenv("NVBOARD_HOME") == nullptr) setenv("NVBOARD_HOME", NVBOARD_RESOURCE_HOME, 0);
#endif
  nvboard_bind_all_pins(impl->top);
  nvboard_init();
  impl->nvboardInitialized = true;
#endif
}

void Dut::shutdown() {
#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  if (impl->nvboardInitialized) {
    nvboard_quit();
    impl->nvboardInitialized = false;
  }
#endif
  if (impl->top != nullptr) impl->top->final();
  if (impl->vcd != nullptr) {
    if (impl->vcd->isOpen()) {
      impl->vcd->close();
      printf("VCD waveform written to %s\n", impl->vcdPath.c_str());
    }
    delete impl->vcd;
    impl->vcd = nullptr;
  }
  delete impl->top;
  impl->top = nullptr;
  impl->vcdWatching = false;
}

void Dut::stepCycle() {
  impl->top->clock = 0;
  impl->top->eval();
  if (impl->vcdWatching) impl->vcd->dump(impl->timestamp);
  ++impl->timestamp;
  Verilated::timeInc(1);
  impl->top->clock = 1;
  impl->top->eval();
  if (impl->vcdWatching) impl->vcd->dump(impl->timestamp);
  ++impl->timestamp;
  Verilated::timeInc(1);

#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  nvboard_update();
  static int traceUart = -1;
  static uint8_t lastUartRx = 0xffu;
  static uint8_t lastUartTx = 0xffu;
  if (traceUart < 0) traceUart = getenv("NPC_TRACE_UART") != nullptr;
  if (traceUart) {
    const uint8_t uartRx = impl->top->externalPins_uart_rx;
    const uint8_t uartTx = impl->top->externalPins_uart_tx;
    if (uartRx != lastUartRx || uartTx != lastUartTx) {
      printf("[nvboard-uart] cycle=%llu rx=%u tx=%u\n",
             static_cast<unsigned long long>(impl->cycles), uartRx & 1u, uartTx & 1u);
      lastUartRx = uartRx;
      lastUartTx = uartTx;
    }
  }
#endif
  ++impl->cycles;
}

void Dut::reset(int cycles) {
  impl->top->reset = 1;
  for (int i = 0; i < cycles; ++i) stepCycle();
  impl->top->reset = 0;
  impl->top->eval();
}

bool Dut::startVcdWatch(const char *filename) {
  if (impl->top == nullptr || impl->vcd == nullptr) return false;
  const bool hasFilename = filename != nullptr && filename[0] != '\0';
  if (impl->vcd->isOpen()) {
    if (hasFilename && impl->vcdPath != filename) {
      printf("VCD output is already bound to '%s'\n", impl->vcdPath.c_str());
      return false;
    }
  } else {
    impl->vcdPath = hasFilename ? filename : "npc-wave.vcd";
    impl->vcd->open(impl->vcdPath.c_str());
    if (!impl->vcd->isOpen()) {
      printf("Failed to open VCD output '%s'\n", impl->vcdPath.c_str());
      impl->vcdPath.clear();
      return false;
    }
  }
  impl->vcdWatching = true;
  return true;
}

bool Dut::endVcdWatch() {
  if (!impl->vcdWatching) return false;
  impl->vcdWatching = false;
  impl->vcd->flush();
  return true;
}

bool Dut::isVcdWatching() const { return impl->vcdWatching; }
const char *Dut::vcdPath() const { return impl->vcdPath.empty() ? nullptr : impl->vcdPath.c_str(); }
