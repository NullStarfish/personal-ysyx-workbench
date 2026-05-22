#include "Pipeline/Execute.h"

#include <cstdio>

#include "Pipeline/Pipeline.h"
#include "svdpi.h"

ExecuteStage::ExecuteStage() : PipelineStage("Execute") {}

void ExecuteStage::trace(bool finished) {
  if (finished) finishedCnt++;
}

void ExecuteStage::printUnitStats() const {
  printf(", ExecuteDone=%lld", finishedCnt);
}

extern "C" void execute_trace(svBit finished) {
  pipeline.execute.trace(finished != 0);
}
