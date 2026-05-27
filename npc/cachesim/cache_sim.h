#ifndef NPC_CACHESIM_CACHE_SIM_H
#define NPC_CACHESIM_CACHE_SIM_H

#include <cstdint>
#include <cstddef>
#include <vector>

namespace cachesim {

struct CacheConfig {
  unsigned tagBits = 0;
  unsigned indexBits = 8;
  unsigned offsetBits = 4;
  unsigned ways = 1;
  uint32_t capacityBytes = 4096;
};

struct AccessResult {
  bool hit = false;
  uint32_t tag = 0;
  uint32_t index = 0;
  uint32_t offset = 0;
  unsigned way = 0;
  bool evicted = false;
  uint32_t evictedTag = 0;
};

struct CacheStats {
  uint64_t accessCount = 0;
  uint64_t hitCount = 0;
  uint64_t missCount = 0;

  double missRate() const;
  double hitRate() const;
};

class Cache {
public:
  explicit Cache(CacheConfig config);

  AccessResult probe(uint32_t addr) const;
  AccessResult commit(uint32_t addr, bool hit);
  AccessResult access(uint32_t addr);
  void reset();

  const CacheConfig &config() const;
  const CacheStats &stats() const;

  std::size_t lineCount() const;
  std::size_t setCount() const;
  uint32_t lineBytes() const;

private:
  struct Line {
    bool valid = false;
    uint32_t tag = 0;
    uint64_t lastUsed = 0;
  };

  CacheConfig config_;
  CacheStats stats_;
  std::vector<std::vector<Line>> sets_;
  uint64_t time_ = 0;

  uint32_t tagOf(uint32_t addr) const;
  uint32_t indexOf(uint32_t addr) const;
  uint32_t offsetOf(uint32_t addr) const;
  static uint32_t mask(unsigned bits);
  static bool isPowerOfTwo(uint32_t value);
  static unsigned log2Exact(uint32_t value);
};

using DirectMappedCache = Cache;

} // namespace cachesim

#endif
