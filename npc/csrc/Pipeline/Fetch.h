#ifndef NPC_PIPELINE_FETCH_H
#define NPC_PIPELINE_FETCH_H

#include <cstdint>

#include "Pipeline/PipelineStage.h"

class FetchStage : public PipelineStage {
public:
  FetchStage();

  void trace(bool gotInst, uint32_t pc, uint32_t instVal, uint32_t memLatency, uint32_t waitLatency);
  void traceUnit(bool reqBlocked, bool outBlocked, bool flush);
  void printUnitStats() const;
  void printStats(long long totalCycles) const;

private:
  long long gotInstCnt = 0;
  long long memLatencyCnt = 0;
  long long maxMemLatency = 0;
  long long waitLatencyCnt = 0;
  long long maxWaitLatency = 0;
  long long reqBlockedCycles = 0;
  long long outBlockedCycles = 0;
  long long flushCycles = 0;
};

#endif
