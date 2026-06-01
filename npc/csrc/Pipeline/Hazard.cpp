#include "Pipeline/Hazard.h"

#include <cstdio>

#include "Pipeline/Pipeline.h"
#include "svdpi.h"

void HazardTraceModel::trace(bool loadUseStall, bool redirectFlush) {
  if (loadUseStall) loadUseCycles++;
  if (redirectFlush) redirectFlushCycles++;
}

void HazardTraceModel::printStats(long long totalCycles) const {
  if (totalCycles <= 0) return;
  printf("Hazard trace: LoadUse=%lld (%lf%%), RedirectFlush=%lld (%lf%%)\n",
         loadUseCycles, 100.0 * loadUseCycles / totalCycles,
         redirectFlushCycles, 100.0 * redirectFlushCycles / totalCycles);
}

extern "C" void hazard_trace(svBit loadUseStall, svBit redirectFlush) {
#ifdef CONFIG_HAZARD_TRACE
  pipeline.hazard.trace(loadUseStall != 0, redirectFlush != 0);
#else
  (void)loadUseStall;
  (void)redirectFlush;
#endif
}
