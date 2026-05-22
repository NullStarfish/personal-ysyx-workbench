#include "Pipeline/Fetch.h"

#include <cstdio>

#include "Pipeline/Pipeline.h"
#include "svdpi.h"

FetchStage::FetchStage() : PipelineStage("Fetch") {}

void FetchStage::trace(bool gotInst, uint32_t pc, uint32_t instVal, uint32_t memLatency, uint32_t waitLatency) {
  (void)pc;
  (void)instVal;
  if (!gotInst) return;

  gotInstCnt++;
  memLatencyCnt += memLatency;
  if (memLatency > maxMemLatency) {
    maxMemLatency = memLatency;
  }
  waitLatencyCnt += waitLatency;
  if (waitLatency > maxWaitLatency) {
    maxWaitLatency = waitLatency;
  }
}

void FetchStage::traceUnit(bool reqBlocked, bool outBlocked, bool flush) {
  if (reqBlocked) reqBlockedCycles++;
  if (outBlocked) outBlockedCycles++;
  if (flush) flushCycles++;
}

void FetchStage::printUnitStats() const {
  printf("Unit trace: FetchInst=%lld", gotInstCnt);
}

void FetchStage::printStats(long long totalCycles) const {
  if (gotInstCnt > 0) {
    printf("Fetch memory latency: Avg=%lf cycles\t, Max=%lld cycles\n",
           static_cast<double>(memLatencyCnt) / gotInstCnt, maxMemLatency);
    printf("Fetch output wait: Avg=%lf cycles\t, Max=%lld cycles\n",
           static_cast<double>(waitLatencyCnt) / gotInstCnt, maxWaitLatency);
  }
  if (totalCycles > 0) {
    printf("Fetch unit stalls: ReqBlocked=%lld (%lf%%), OutBlocked=%lld (%lf%%), Flush=%lld (%lf%%)\n",
           reqBlockedCycles, 100.0 * reqBlockedCycles / totalCycles,
           outBlockedCycles, 100.0 * outBlockedCycles / totalCycles,
           flushCycles, 100.0 * flushCycles / totalCycles);
  }
}

extern "C" void fetch_trace(svBit gotInst, int pc, int inst, int memLatency, int waitLatency) {
  pipeline.fetch.trace(gotInst != 0,
                       static_cast<uint32_t>(pc),
                       static_cast<uint32_t>(inst),
                       static_cast<uint32_t>(memLatency),
                       static_cast<uint32_t>(waitLatency));
}

extern "C" void fetch_unit_trace(svBit reqBlocked, svBit outBlocked, svBit flush) {
  pipeline.fetch.traceUnit(reqBlocked != 0, outBlocked != 0, flush != 0);
}
