#ifndef NPC_CPU_H
#define NPC_CPU_H

#include <cstddef>
#include <cstdint>

#include "runtime.h"
#include "svdpi.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  uint32_t mtvec;
  uint32_t mepc;
  uint32_t mstatus;
  uint32_t mcause;
} CSRS;

typedef struct {
  uint32_t gpr[32];
  uint32_t pc;
  CSRS csrs;
} riscv32_CPU_state;

extern bool difftestis_enabled;

#define DIFFTEST_TO_REF 1
#define DIFFTEST_TO_DUT 0

void diffteststep();
void difftestskip_ref();

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
class CPU;
class Runtime;
class Difftest;
class Mem;
class Inst;



class Inst {
public:
  uint32_t pc = 0;
  uint32_t instVal = 0;
  uint32_t instType = 0;
  uint32_t lifeCycleCount = 0;
};

class InstQueue {
  public:
    Inst queue[5];
    int tail = 0;
    int head = 0;
    int cnt = 0;

    void run(Inst inst) {
      if (cnt >= 5) return;
      queue[tail++] = inst;
      if (tail >= 5) {
        tail = 0;
      }
      cnt ++;
    }
    Inst pop() {
      Inst inst = {};
      if (!cnt) return inst;
      inst = queue[head++];
      if (head >= 5) {
        head = 0;
      }
      cnt --;
      return inst;
    }
    Inst retire(uint32_t pc, uint32_t instVal) {
      int matchDistance = -1;
      int index = head;
      for (int i = 0; i < cnt; i++) {
        if (queue[index].pc == pc && queue[index].instVal == instVal) {
          matchDistance = i;
          break;
        }
        index++;
        if (index >= 5) {
          index = 0;
        }
      }
      if (matchDistance < 0) return {};

      for (int i = 0; i <= matchDistance; i++) {
        Inst inst = pop();
        if (inst.pc == pc && inst.instVal == instVal) {
          return inst;
        }
      }
      return {};
    }
    void discardAfter(uint32_t pc, uint32_t instVal) {
      int index = head;
      for (int i = 0; i < cnt; i++) {
        if (queue[index].pc == pc && queue[index].instVal == instVal) {
          tail = index + 1;
          if (tail >= 5) {
            tail = 0;
          }
          cnt = i + 1;
          return;
        }
        index++;
        if (index >= 5) {
          index = 0;
        }
      }
    }

    bool inQueue(int index) {
      if (!cnt) return false;
      
      if (tail > head) return (index >= head) && (index < tail);
      else return (index >= head && index < 5) || (index < tail && index >= 0);
    }

    void updateCycles() {
      for (int i = 0; i < 5; i ++) {
        if (inQueue(i)) {
          queue[i].lifeCycleCount ++;
        }
      }
    }
};



class CPU {
public:
  enum instType {arith, mem, redirect, sys};
  struct RetireSnapshot {
    uint32_t pc = 0;
    uint32_t dnpc = 0;
    uint32_t inst = 0;
    uint32_t instType = arith;
    uint32_t gpr[32] = {};
    bool regWen = false;
    uint32_t regAddr = 0;
    uint32_t regData = 0;
    CSRS csrs = {};
  };

  CPU() = default;

  void init();
  void exec(uint64_t n);
  void execOnce();
  void commitRetire(const RetireSnapshot &snapshot);
  void traceFetch(bool gotInst, uint32_t pc, uint32_t instVal, uint32_t latency);
  void traceExecute(bool finished);
  void traceLsu(uint32_t latency, bool write);
  void traceFlush(bool flush, uint32_t pc, uint32_t instVal);

  uint32_t pc() const;
  uint32_t retirePc() const;
  uint32_t inst() const;
  uint32_t regRead(int regNum) const;
  uint32_t csrRead(int csrNum) const;
  uint32_t regStr2Val(const char *s, bool *success) const;
  void isa_reg_display() const;
  uint32_t isa_reg_str2val(const char *s, bool *success) const;
  riscv32_CPU_state dutState() const;
  void copyDutState(riscv32_CPU_state *dut) const;

  long long cycleCount() const;
  void printStats() const;
  void handleSigint();

private:
  void traceAndDifftest();

  riscv32_CPU_state archState{};
  uint32_t retirePcValue = 0;
  uint32_t retireInstValue = 0;
  bool hasCommitted = false;
  long long cycleCountValue = 0;
  long long instrCountValue = 0;
  long long instArithCnt = 0;
  long long instMemCnt = 0;
  long long instRdrctCnt = 0;
  long long instSysCnt =0 ;
  long long instLifeCycleCnt[4] = {};
  long long instTypeCnt[4] = {};
  long long fetchGotInstCnt = 0;
  long long fetchLatencyCnt = 0;
  long long fetchMaxLatency = 0;
  long long executeFinishedCnt = 0;
  long long lsuGotDataCnt = 0;
  long long lsuWriteDataCnt = 0;
  long long lsuLoadLatencyCnt = 0;
  long long lsuMaxLoadLatency = 0;
  long long lsuStoreLatencyCnt = 0;
  long long lsuMaxStoreLatency = 0;
  long long totalInstLifeCycles = 0;
  long long maxInstLifeCycles = 0;
  InstQueue instQueue;
};
#endif

#endif
