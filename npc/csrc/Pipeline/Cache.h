#ifndef NPC_PIPELINE_CACHE_H
#define NPC_PIPELINE_CACHE_H

#include <cstdint>

#include "cache_sim.h"

class CacheAccessStats {
public:
  void record(bool hit, bool miss, uint32_t latency);

  long long hits() const { return hits_; }
  long long misses() const { return misses_; }
  void printLatency(const char *name) const;

private:
  long long hits_ = 0;
  long long misses_ = 0;
  long long accessCycles_ = 0;
  long long missCycles_ = 0;
  long long maxLatency_ = 0;
};

class CacheTraceModel {
public:
  CacheTraceModel();

  void traceICacheAccess(bool hit, bool miss, uint32_t latency);
  void compareICacheRetire(uint32_t pc, bool hit);
  void traceDCacheAccess(bool hit, bool miss, uint32_t latency);
  void printStats() const;

private:
  CacheAccessStats icacheStats_;
  CacheAccessStats dcacheStats_;
  cachesim::Cache icacheReference_;
  long long icacheMismatches_ = 0;
};

#endif
