#include "Pipeline/Pipeline.h"

#include <cstdio>

#include "svdpi.h"

LSUStage::LSUStage() : PipelineStage("LSU") {}

void LSUStage::trace(uint32_t latency, bool write) {
  if (write) {
    writeDataCnt++;
    storeLatencyCnt += latency;
    if (latency > maxStoreLatency) {
      maxStoreLatency = latency;
    }
  } else {
    gotDataCnt++;
    loadLatencyCnt += latency;
    if (latency > maxLoadLatency) {
      maxLoadLatency = latency;
    }
  }
}

void LSUStage::traceBackpressure(bool blocked) {
  if (blocked) {
    backpressureCycles++;
  }
}

void LSUStage::printUnitStats() const {
  printf(", LsuLoadData=%lld\t, LsuStoreAck=%lld\n", gotDataCnt, writeDataCnt);
}

void LSUStage::printStats(long long totalCycles) const {
  if (totalCycles > 0) {
    printf("LSU backpressure: %lld (%lf%%)\n",
           backpressureCycles, 100.0 * backpressureCycles / totalCycles);
  }
  if (gotDataCnt > 0) {
    printf("LSU load latency: Avg=%lf cycles\t, Max=%lld cycles\n",
           static_cast<double>(loadLatencyCnt) / gotDataCnt, maxLoadLatency);
  }
  if (writeDataCnt > 0) {
    printf("LSU store latency: Avg=%lf cycles\t, Max=%lld cycles\n",
           static_cast<double>(storeLatencyCnt) / writeDataCnt, maxStoreLatency);
  }
}

extern "C" void lsu_trace(int latency, svBit write) {
  pipeline.lsu.trace(static_cast<uint32_t>(latency), write != 0);
}

extern "C" void lsu_backpressure_trace(svBit blocked) {
  pipeline.lsu.traceBackpressure(blocked != 0);
}
