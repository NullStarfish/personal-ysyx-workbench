#ifndef NPC_PIPELINE_LSU_H
#define NPC_PIPELINE_LSU_H

#include <cstdint>

#include "Pipeline/PipelineStage.h"

class LSUStage : public PipelineStage {
public:
  LSUStage();

  void trace(uint32_t latency, bool write);
  void traceBackpressure(bool blocked);
  void printUnitStats() const;
  void printStats(long long totalCycles) const;

private:
  long long gotDataCnt = 0;
  long long writeDataCnt = 0;
  long long loadLatencyCnt = 0;
  long long maxLoadLatency = 0;
  long long storeLatencyCnt = 0;
  long long maxStoreLatency = 0;
  long long backpressureCycles = 0;
};

#endif
