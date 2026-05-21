#include "Pipeline/Pipeline.h"

#include <cstdio>

#include "svdpi.h"

void CacheTraceModel::traceICache(bool hit, bool miss, uint32_t latency) {
  if (hit) icacheHits++;
  if (miss) {
    icacheMisses++;
    icacheMissCycles += latency;
  }
  if (hit || miss) {
    icacheAccessCycles += latency;
    if (latency > icacheMaxLatency) {
      icacheMaxLatency = latency;
    }
  }
}

void CacheTraceModel::traceDCache(bool hit, bool miss, uint32_t latency) {
  if (hit) dcacheHits++;
  if (miss) {
    dcacheMisses++;
    dcacheMissCycles += latency;
  }
  if (hit || miss) {
    dcacheAccessCycles += latency;
    if (latency > dcacheMaxLatency) {
      dcacheMaxLatency = latency;
    }
  }
}

void CacheTraceModel::printStats() const {
  const long long icacheAccesses = icacheHits + icacheMisses;
  const long long dcacheAccesses = dcacheHits + dcacheMisses;

  printf("Cache trace: ICacheHit=%lld, ICacheMiss=%lld, DCacheHit=%lld, DCacheMiss=%lld\n",
         icacheHits, icacheMisses, dcacheHits, dcacheMisses);
  if (icacheAccesses > 0) {
    printf("ICache AMAT=%lf cycles, TMT=%lld cycles, Max=%lld cycles\n",
           static_cast<double>(icacheAccessCycles) / icacheAccesses,
           icacheMissCycles,
           icacheMaxLatency);
  }
  if (dcacheAccesses > 0) {
    printf("DCache AMAT=%lf cycles, TMT=%lld cycles, Max=%lld cycles\n",
           static_cast<double>(dcacheAccessCycles) / dcacheAccesses,
           dcacheMissCycles,
           dcacheMaxLatency);
  }
}

extern "C" void icache_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceICache(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}

extern "C" void dcache_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceDCache(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}
