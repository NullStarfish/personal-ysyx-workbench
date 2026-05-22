#include "Pipeline/Cache.h"

#include <cstdio>
#include <cstdlib>

#include "Pipeline/Pipeline.h"
#include "svdpi.h"

static unsigned envUnsigned(const char *name, unsigned fallback) {
  const char *text = std::getenv(name);
  if (text == nullptr || *text == '\0') return fallback;

  char *end = nullptr;
  unsigned long value = std::strtoul(text, &end, 0);
  if (end == text || *end != '\0') return fallback;
  return static_cast<unsigned>(value);
}

static uint32_t envU32(const char *name, uint32_t fallback) {
  const char *text = std::getenv(name);
  if (text == nullptr || *text == '\0') return fallback;

  char *end = nullptr;
  unsigned long value = std::strtoul(text, &end, 0);
  if (end == text || *end != '\0') return fallback;
  return static_cast<uint32_t>(value);
}

static cachesim::CacheConfig refConfig() {
  cachesim::CacheConfig config;
  config.capacityBytes = envU32("NPC_CACHESIM_CAPACITY", 8192);
  config.ways = envUnsigned("NPC_CACHESIM_WAYS", 2);
  config.offsetBits = envUnsigned("NPC_CACHESIM_OFFSET_BITS", 5);
  config.indexBits = envUnsigned("NPC_CACHESIM_INDEX_BITS", 7);
  config.tagBits = envUnsigned("NPC_CACHESIM_TAG_BITS", 0);
  return config;
}

CacheTraceModel::CacheTraceModel()
    : icacheRefConfig_(refConfig()),
      icacheRef_(icacheRefConfig_) {}

void CacheTraceModel::traceICacheReq(uint32_t pc) {
  const cachesim::AccessResult result = icacheRef_.access(pc);
  icacheExpected_.push_back({pc, result.hit});
}

void CacheTraceModel::flushICacheRef() {
  icacheFlushDropCount += static_cast<long long>(icacheExpected_.size());
  icacheExpected_.clear();
}

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

    if (icacheExpected_.empty()) {
      icacheUnexpectedCount++;
      std::fprintf(stderr,
                   "[cachesim] unexpected icache result: rtl=%s latency=%u\n",
                   hit ? "hit" : "miss",
                   latency);
    } else {
      const RefAccess expected = icacheExpected_.front();
      icacheExpected_.pop_front();
      icacheCompareCount++;
      if (expected.hit != hit) {
        icacheMismatchCount++;
        std::fprintf(stderr,
                     "[cachesim] icache mismatch pc=0x%08x ref=%s rtl=%s latency=%u\n",
                     expected.pc,
                     expected.hit ? "hit" : "miss",
                     hit ? "hit" : "miss",
                     latency);
      }
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

  if (icacheCompareCount > 0 || icacheMismatchCount > 0 || icacheUnexpectedCount > 0 || icacheFlushDropCount > 0) {
    const cachesim::CacheStats &refStats = icacheRef_.stats();
    printf("ICache ref: access=%llu hit=%llu miss=%llu hitRate=%lf missRate=%lf compare=%lld mismatch=%lld unexpected=%lld flushed=%lld pending=%zu\n",
           static_cast<unsigned long long>(refStats.accessCount),
           static_cast<unsigned long long>(refStats.hitCount),
           static_cast<unsigned long long>(refStats.missCount),
           refStats.hitRate(),
           refStats.missRate(),
           icacheCompareCount,
           icacheMismatchCount,
           icacheUnexpectedCount,
           icacheFlushDropCount,
           icacheExpected_.size());
  }
}

extern "C" void icache_req_trace(int pc) {
  pipeline.cache.traceICacheReq(static_cast<uint32_t>(pc));
}

extern "C" void icache_ref_flush(svBit flush) {
  if (flush != 0) {
    pipeline.cache.flushICacheRef();
  }
}

extern "C" void icache_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceICache(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}

extern "C" void dcache_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceDCache(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}
