#include "cache_sim.h"

#include <limits>
#include <stdexcept>

namespace cachesim {

double CacheStats::missRate() const {
  if (accessCount == 0) return 0.0;
  return static_cast<double>(missCount) / static_cast<double>(accessCount);
}

double CacheStats::hitRate() const {
  if (accessCount == 0) return 0.0;
  return static_cast<double>(hitCount) / static_cast<double>(accessCount);
}

Cache::Cache(CacheConfig config) : config_(config) {
  if (config_.offsetBits >= 31) {
    throw std::invalid_argument("offsetBits must be less than 31");
  }
  if (config_.ways == 0) {
    throw std::invalid_argument("ways must be greater than 0");
  }
  if (!isPowerOfTwo(config_.ways)) {
    throw std::invalid_argument("ways must be a power of two");
  }
  if (config_.capacityBytes == 0) {
    throw std::invalid_argument("capacityBytes must be greater than 0");
  }
  if (!isPowerOfTwo(config_.capacityBytes)) {
    throw std::invalid_argument("capacityBytes must be a power of two");
  }

  const uint32_t bytesPerLine = uint32_t{1} << config_.offsetBits;
  if (config_.capacityBytes < bytesPerLine * config_.ways) {
    throw std::invalid_argument("capacityBytes must be at least lineBytes * ways");
  }
  if (config_.capacityBytes % (bytesPerLine * config_.ways) != 0) {
    throw std::invalid_argument("capacityBytes must be divisible by lineBytes * ways");
  }

  const uint32_t sets = config_.capacityBytes / (bytesPerLine * config_.ways);
  if (!isPowerOfTwo(sets)) {
    throw std::invalid_argument("derived set count must be a power of two");
  }

  const unsigned derivedIndexBits = log2Exact(sets);
  if (config_.indexBits != derivedIndexBits) {
    throw std::invalid_argument("indexBits does not match capacityBytes / (lineBytes * ways)");
  }
  if (config_.tagBits == 0) {
    config_.tagBits = 32 - config_.indexBits - config_.offsetBits;
  }
  if (config_.indexBits >= 31) {
    throw std::invalid_argument("indexBits must be less than 31");
  }
  if (config_.tagBits + config_.indexBits + config_.offsetBits > 32) {
    throw std::invalid_argument("tagBits + indexBits + offsetBits must be <= 32");
  }

  sets_.resize(sets);
  for (std::vector<Line> &set : sets_) {
    set.resize(config_.ways);
  }
}

AccessResult Cache::access(uint32_t addr) {
  AccessResult result;
  result.tag = tagOf(addr);
  result.index = indexOf(addr);
  result.offset = offsetOf(addr);

  stats_.accessCount++;
  time_++;

  std::vector<Line> &set = sets_.at(result.index);
  for (unsigned way = 0; way < set.size(); way++) {
    Line &line = set[way];
    if (line.valid && line.tag == result.tag) {
      line.lastUsed = time_;
      result.hit = true;
      result.way = way;
      stats_.hitCount++;
      return result;
    }
  }

  stats_.missCount++;

  unsigned victimWay = 0;
  uint64_t oldestUse = std::numeric_limits<uint64_t>::max();
  for (unsigned way = 0; way < set.size(); way++) {
    const Line &line = set[way];
    if (!line.valid) {
      victimWay = way;
      oldestUse = 0;
      break;
    }
    if (line.lastUsed < oldestUse) {
      victimWay = way;
      oldestUse = line.lastUsed;
    }
  }

  Line &victim = set[victimWay];
  result.way = victimWay;
  result.evicted = victim.valid;
  result.evictedTag = victim.tag;
  victim.valid = true;
  victim.tag = result.tag;
  victim.lastUsed = time_;

  return result;
}

void Cache::reset() {
  stats_ = {};
  time_ = 0;
  for (std::vector<Line> &set : sets_) {
    for (Line &line : set) {
      line = {};
    }
  }
}

const CacheConfig &Cache::config() const {
  return config_;
}

const CacheStats &Cache::stats() const {
  return stats_;
}

std::size_t Cache::lineCount() const {
  return setCount() * config_.ways;
}

std::size_t Cache::setCount() const {
  return sets_.size();
}

uint32_t Cache::lineBytes() const {
  return uint32_t{1} << config_.offsetBits;
}

uint32_t Cache::tagOf(uint32_t addr) const {
  return (addr >> (config_.indexBits + config_.offsetBits)) & mask(config_.tagBits);
}

uint32_t Cache::indexOf(uint32_t addr) const {
  return (addr >> config_.offsetBits) & mask(config_.indexBits);
}

uint32_t Cache::offsetOf(uint32_t addr) const {
  return addr & mask(config_.offsetBits);
}

uint32_t Cache::mask(unsigned bits) {
  if (bits == 0) return 0;
  if (bits >= 32) return 0xffffffffu;
  return (uint32_t{1} << bits) - 1u;
}

bool Cache::isPowerOfTwo(uint32_t value) {
  return value != 0 && (value & (value - 1)) == 0;
}

unsigned Cache::log2Exact(uint32_t value) {
  if (!isPowerOfTwo(value)) {
    throw std::invalid_argument("log2Exact requires a power of two");
  }

  unsigned log = 0;
  while (value > 1) {
    value >>= 1;
    log++;
  }
  return log;
}

} // namespace cachesim
