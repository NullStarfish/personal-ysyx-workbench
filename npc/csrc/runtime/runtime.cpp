#include "runtime/runtime.h"

#include <cstdlib>
#include <cstdio>
#include <getopt.h>
#include <sys/time.h>
#include <unistd.h>
#include <vector>
#include <verilated.h>
#include <verilated_vcd_c.h>

#ifdef CONFIG_NPC_VIRTUAL_SOC
#include "VNpcTop.h"
#else
#include "VysyxSoCFull.h"
#endif
#include "cpu.h"
#include "mem.h"
#include "sim.h"

#ifdef CONFIG_BOARD
#include <nvboard.h>
void nvboard_bind_all_pins(VerilatedDut *top);
#endif

namespace {
const Runtime *activeRuntime = nullptr;
}

Runtime::Runtime() : sdbValue(*this) {
  activeRuntime = this;
}

Runtime::~Runtime() {
  if (activeRuntime == this) {
    activeRuntime = nullptr;
  }
}

void Runtime::init(int argc, char *argv[]) {
  Verilated::commandArgs(argc, argv);
  parseArgs(argc, argv);
  loggerValue.init();
  itraceValue.init();
  ftraceValue.init();

  Verilated::traceEverOn(true);
  top = new VerilatedDut;
  vcd = new VerilatedVcdC;
  top->trace(vcd, 99);

#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  printf("nvboard initing...\n");
#ifdef NVBOARD_RESOURCE_HOME
  if (getenv("NVBOARD_HOME") == nullptr) {
    setenv("NVBOARD_HOME", NVBOARD_RESOURCE_HOME, 0);
  }
#endif
  nvboard_bind_all_pins(top);
  nvboard_init();
  nvboardInitialized = true;
#endif

  const long imageSize = loadProgram();
  resetCpu(100);
  cpu.init();
  printf("CPU reset complete.\n");

  difftestValue.init(imageSize);

  sdbValue.init();
  welcome();
}

void Runtime::shutdown() {
  difftestValue.shutdown();
#if defined(CONFIG_BOARD) && !defined(CONFIG_NPC_VIRTUAL_SOC)
  if (nvboardInitialized) {
    nvboard_quit();
    nvboardInitialized = false;
  }
#endif
  if (top != nullptr) {
    top->final();
  }
  if (vcd != nullptr) {
    if (vcd->isOpen()) {
      vcd->close();
      printf("VCD waveform written to %s\n", vcdPathValue.c_str());
    }
    delete vcd;
    vcd = nullptr;
    vcdWatching = false;
  }
  if (top != nullptr) {
    delete top;
    top = nullptr;
  }
  loggerValue.shutdown();
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
  if (vcdWatching) vcd->dump(simTime);
  simTime++;
  top->clock = 1;
  top->eval();
  if (vcdWatching) vcd->dump(simTime);
  simTime++;

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

bool Runtime::startVcdWatch(const char *filename) {
  if (top == nullptr || vcd == nullptr) return false;

  const bool hasFilename = filename != nullptr && filename[0] != '\0';
  if (vcd->isOpen()) {
    if (hasFilename && vcdPathValue != filename) {
      printf("VCD output is already bound to '%s'\n", vcdPathValue.c_str());
      return false;
    }
  } else {
    vcdPathValue = hasFilename ? filename : "npc-wave.vcd";
    vcd->open(vcdPathValue.c_str());
    if (!vcd->isOpen()) {
      printf("Failed to open VCD output '%s'\n", vcdPathValue.c_str());
      vcdPathValue.clear();
      return false;
    }
  }

  vcdWatching = true;
  return true;
}

bool Runtime::endVcdWatch() {
  if (!vcdWatching) return false;
  vcdWatching = false;
  vcd->flush();
  return true;
}

bool Runtime::isVcdWatching() const { return vcdWatching; }

const char *Runtime::vcdPath() const {
  return vcdPathValue.empty() ? nullptr : vcdPathValue.c_str();
}

const char *Runtime::imagePath() const { return imagePathValue; }

void Runtime::parseArgs(int argc, char *argv[]) {
  const option table[] = {
      {"batch", no_argument, nullptr, 'b'},
      {"log", required_argument, nullptr, 'l'},
      {"diff", required_argument, nullptr, 'd'},
      {"ftrace", required_argument, nullptr, 'f'},
      {"help", no_argument, nullptr, 'h'},
      {"pc-trace", required_argument, nullptr, 'p'},
      {0, 0, nullptr, 0},
  };

  int opt = 0;
  while ((opt = getopt_long(argc, argv, "-bl:d:f:h:p:", table, nullptr)) != -1) {
    switch (opt) {
      case 'b': sdbValue.setBatchMode(); break;
      case 'l': loggerValue.setLogFile(optarg); break;
      case 'd': difftestValue.setRefSoFile(optarg); break;
      case 'f': ftraceValue.setElfFile(optarg); break;
      case 'h':
        printf("pass img file or opts to the executable\n");
        exit(0);
      case 'p': loggerValue.setPcTraceFile(optarg); break;
      case 1:
        if (imagePathValue == nullptr) imagePathValue = optarg;
        break;
      default: exit(0);
    }
  }
  if (imagePathValue == nullptr && optind < argc) imagePathValue = argv[optind];
}

long Runtime::loadProgram() const {
  if (imagePathValue == nullptr) return 0;

  FILE *fp = fopen(imagePathValue, "rb");
  if (fp == nullptr) {
    printf("Can not open '%s'\n", imagePathValue);
    exit(1);
  }
  fseek(fp, 0, SEEK_END);
  const long fileSize = ftell(fp);
  fseek(fp, 0, SEEK_SET);
  printf("The image is %s, size = %ld\n", imagePathValue, fileSize);

  std::vector<uint8_t> programData(static_cast<size_t>(fileSize));
  fread(programData.data(), programData.size(), 1, fp);
  fclose(fp);
  mem.loadDataToRom(programData.data(), programData.size());
  return fileSize;
}

void Runtime::welcome() const {
  printf("Welcome to the RISC-V NPC simulator!\nFor help, type \"help\"\nThe current img is %s\n",
         imagePathValue == nullptr ? "(none)" : imagePathValue);
}

void Runtime::resetCpu(int n) {
  top->reset = 1;
  for (int i = 0; i < n; ++i) {
    stepOneClk();
  }
  top->reset = 0;
  top->eval();
}

void Runtime::assertFailMsg() const {
  cpu.isa_reg_display();
#ifdef CONFIG_ITRACE
  itraceValue.printRecent();
#endif
#ifdef CONFIG_FTRACE
  ftraceValue.printStack();
#endif
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
