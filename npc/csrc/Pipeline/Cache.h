#ifndef NPC_PIPELINE_CACHE_H
#define NPC_PIPELINE_CACHE_H

#include <cstddef>
#include <cstdint>
#include <deque>

#include "cache_sim.h"

class CacheTraceModel {
public:
  CacheTraceModel();

  void traceICacheReq(uint32_t pc);
  void flushICacheRef();
  void traceICache(bool hit, bool miss, uint32_t latency);
  void traceDCache(bool hit, bool miss, uint32_t latency);
  void printStats() const;

private:
  struct RefAccess {
    uint32_t pc = 0;
    bool hit = false;
  };

  cachesim::CacheConfig icacheRefConfig_;
  cachesim::Cache icacheRef_;
  std::deque<RefAccess> icacheExpected_;

  long long icacheHits = 0;
  long long icacheMisses = 0;
  long long icacheAccessCycles = 0;
  long long icacheMissCycles = 0;
  long long icacheMaxLatency = 0;
  long long icacheCompareCount = 0;
  long long icacheMismatchCount = 0;
  long long icacheUnexpectedCount = 0;
  long long icacheFlushDropCount = 0;

  long long dcacheHits = 0;
  long long dcacheMisses = 0;
  long long dcacheAccessCycles = 0;
  long long dcacheMissCycles = 0;
  long long dcacheMaxLatency = 0;
};

#endif
