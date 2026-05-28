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
  config.capacityBytes = envU32("NPC_CACHESIM_CAPACITY", 32*32*2);
  config.ways = envUnsigned("NPC_CACHESIM_WAYS", 2);
  config.offsetBits = envUnsigned("NPC_CACHESIM_OFFSET_BITS", 5);
  config.indexBits = envUnsigned("NPC_CACHESIM_INDEX_BITS", 5);
  config.tagBits = envUnsigned("NPC_CACHESIM_TAG_BITS", 0);
  return config;
}

void CacheAccessStats::record(bool hit, bool miss, uint32_t latency) {
  if (hit) hits_++;
  if (miss) {
    misses_++;
    missCycles_ += latency;
  }
  if (hit || miss) {
    accessCycles_ += latency;
    if (latency > maxLatency_) maxLatency_ = latency;
  }
}

void CacheAccessStats::printLatency(const char *name) const {
  const long long accesses = hits_ + misses_;
  if (accesses == 0) return;

  printf("%s AMAT=%lf cycles, TMT=%lld cycles, Max=%lld cycles\n",
         name,
         static_cast<double>(accessCycles_) / accesses,
         missCycles_,
         maxLatency_);
}

CacheTraceModel::CacheTraceModel()
    : icacheReference_(refConfig()) {}

void CacheTraceModel::compareICacheRetire(uint32_t pc, bool hit) {
  const cachesim::AccessResult expected = icacheReference_.access(pc);
  if (expected.hit != hit) {
    icacheMismatches_++;
    std::fprintf(stderr,
                 "[cachesim] icache mismatch pc=0x%08x ref=%s rtl=%s\n",
                 pc, expected.hit ? "hit" : "miss", hit ? "hit" : "miss");
  }
}

void CacheTraceModel::traceICacheAccess(bool hit, bool miss, uint32_t latency) {
  icacheStats_.record(hit, miss, latency);
}

void CacheTraceModel::traceDCacheAccess(bool hit, bool miss, uint32_t latency) {
  dcacheStats_.record(hit, miss, latency);
}

void CacheTraceModel::printStats() const {
  printf("Cache trace: ICacheHit=%lld, ICacheMiss=%lld, DCacheHit=%lld, DCacheMiss=%lld\n",
         icacheStats_.hits(), icacheStats_.misses(), dcacheStats_.hits(), dcacheStats_.misses());
  icacheStats_.printLatency("ICache");
  dcacheStats_.printLatency("DCache");
#ifdef CONFIG_CACHESIM_DIFFTEST
  const cachesim::CacheStats &stats = icacheReference_.stats();
  printf("ICache cachesim: access=%llu hit=%llu miss=%llu hitRate=%lf missRate=%lf mismatch=%lld\n",
         static_cast<unsigned long long>(stats.accessCount),
         static_cast<unsigned long long>(stats.hitCount),
         static_cast<unsigned long long>(stats.missCount),
         stats.hitRate(),
         stats.missRate(),
         icacheMismatches_);
#endif
}

extern "C" void icache_access_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceICacheAccess(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}

extern "C" void dcache_trace(svBit hit, svBit miss, int latency) {
  pipeline.cache.traceDCacheAccess(hit != 0, miss != 0, static_cast<uint32_t>(latency));
}
