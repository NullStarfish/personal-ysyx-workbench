#include "Pipeline/Pipeline.h"

#include <cstdio>

#include "svdpi.h"

void CacheTraceModel::traceICache(bool hit, bool miss) {
  if (hit) icacheHits++;
  if (miss) icacheMisses++;
}

void CacheTraceModel::traceDCache(bool hit, bool miss) {
  if (hit) dcacheHits++;
  if (miss) dcacheMisses++;
}

void CacheTraceModel::printStats() const {
  printf("Cache trace: ICacheHit=%lld, ICacheMiss=%lld, DCacheHit=%lld, DCacheMiss=%lld\n",
         icacheHits, icacheMisses, dcacheHits, dcacheMisses);
}

extern "C" void icache_trace(svBit hit, svBit miss) {
  pipeline.cache.traceICache(hit != 0, miss != 0);
}

extern "C" void dcache_trace(svBit hit, svBit miss) {
  pipeline.cache.traceDCache(hit != 0, miss != 0);
}

