#ifndef NPC_RUNTIME_H
#define NPC_RUNTIME_H

#include <cstddef>
#include <cstdint>
#include <string>

#include "difftest_runtime.h"
#include "log/log.h"
#include "runtime/sdb/sdb.h"
#include "runtime/traces/ftrace.h"
#include "runtime/traces/itrace.h"

#ifdef __cplusplus
#ifdef CONFIG_NPC_VIRTUAL_SOC
class VNpcTop;
using VerilatedDut = VNpcTop;
#else
class VysyxSoCFull;
using VerilatedDut = VysyxSoCFull;
#endif
class CPU;
class VerilatedVcdC;
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  int state;
  uint64_t halt_ret;
} NpcState;

enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
class Runtime {
public:
  Runtime();
  ~Runtime();

  void init(int argc, char *argv[]);
  void shutdown();
  void stepOneClk();
  void resetCpu(int n);
  void assertFailMsg() const;
  bool startVcdWatch(const char *filename = nullptr);
  bool endVcdWatch();
  bool isVcdWatching() const;
  const char *vcdPath() const;
  const char *imagePath() const;
  Sdb &sdb() { return sdbValue; }
  ITrace &itrace() { return itraceValue; }
  FTrace &ftrace() { return ftraceValue; }
  Difftest &difftest() { return difftestValue; }

  void setRunning();
  void setStop();
  void setEnd(uint64_t haltRet);
  void setAbort(uint64_t haltRet = 1);
  void setQuit();
  bool isRunning() const;
  bool hasEnded() const;
  int isExitStatusBad() const;
  const NpcState &state() const;

private:
  void parseArgs(int argc, char *argv[]);
  long loadProgram() const;
  void welcome() const;
  uint64_t getTimeInternal() const;
  uint64_t getTime();

  VerilatedDut *top = nullptr;
  VerilatedVcdC *vcd = nullptr;
  Logger loggerValue;
  Sdb sdbValue;
  ITrace itraceValue;
  FTrace ftraceValue;
  Difftest difftestValue;
  bool nvboardInitialized = false;
  bool vcdWatching = false;
  uint64_t simTime = 0;
  std::string vcdPathValue;
  char *imagePathValue = nullptr;
  uint64_t bootTime = 0;
  NpcState npcState{NPC_STOP, 0};
};

#endif

#endif
