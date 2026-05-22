#ifndef NPC_PIPELINE_WRITEBACK_H
#define NPC_PIPELINE_WRITEBACK_H

#include <cstdint>

#include "Pipeline/PipelineStage.h"

class WriteBackStage : public PipelineStage {
public:
  WriteBackStage();

  void traceRetire(const Inst &inst, uint32_t instType);
  long long retiredCount() const;
  void printStats() const;

private:
  long long instTypeCnt[InstTypeCount] = {};
  long long instLifeCycleCnt[InstTypeCount] = {};
  long long totalInstLifeCycles = 0;
  long long maxInstLifeCycles = 0;
};

#endif
