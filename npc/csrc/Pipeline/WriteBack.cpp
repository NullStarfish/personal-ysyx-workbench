#include "Pipeline/WriteBack.h"

#include <cstdio>

WriteBackStage::WriteBackStage() : PipelineStage("WriteBack") {}

void WriteBackStage::traceRetire(const Inst &inst, uint32_t instType) {
  if (instType < InstTypeCount) {
    instTypeCnt[instType]++;
  }

  if (inst.lifeCycleCount > 0) {
    totalInstLifeCycles += inst.lifeCycleCount;
    if (inst.lifeCycleCount > maxInstLifeCycles) {
      maxInstLifeCycles = inst.lifeCycleCount;
    }
    if (instType < InstTypeCount) {
      instLifeCycleCnt[instType] += inst.lifeCycleCount;
    }
  }
}

long long WriteBackStage::retiredCount() const {
  long long total = 0;
  for (int i = 0; i < InstTypeCount; i++) {
    total += instTypeCnt[i];
  }
  return total;
}

void WriteBackStage::printStats() const {
  const long long total = retiredCount();
  printf("Inst statistics: \n");
  printf("Arith: %lld\t, Mem: %lld\t, Redirect: %lld\t, Sys: %lld\n",
         instTypeCnt[InstArith], instTypeCnt[InstMem], instTypeCnt[InstRedirect], instTypeCnt[InstSys]);
  if (total > 0) {
    printf("Arith: %lf%%, Mem: %lf%%, Redirect: %lf%%, Sys: %lf%%\n",
           100.0 * instTypeCnt[InstArith] / total,
           100.0 * instTypeCnt[InstMem] / total,
           100.0 * instTypeCnt[InstRedirect] / total,
           100.0 * instTypeCnt[InstSys] / total);
    printf("Inst lifecycle: Avg=%lf cycles\t, Max=%lld cycles\n",
           static_cast<double>(totalInstLifeCycles) / total, maxInstLifeCycles);
    printf("Inst lifecycle by type: Arith=%lf\t, Mem=%lf\t, Redirect=%lf\t, Sys=%lf\n",
           instTypeCnt[InstArith] == 0 ? 0.0 : static_cast<double>(instLifeCycleCnt[InstArith]) / instTypeCnt[InstArith],
           instTypeCnt[InstMem] == 0 ? 0.0 : static_cast<double>(instLifeCycleCnt[InstMem]) / instTypeCnt[InstMem],
           instTypeCnt[InstRedirect] == 0 ? 0.0 : static_cast<double>(instLifeCycleCnt[InstRedirect]) / instTypeCnt[InstRedirect],
           instTypeCnt[InstSys] == 0 ? 0.0 : static_cast<double>(instLifeCycleCnt[InstSys]) / instTypeCnt[InstSys]);
  }
}
