#include "npc/mem.h"

#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <sys/time.h>

#include "difftest_runtime.h"

namespace {
Mem *activeMem = nullptr;

uint64_t host_time_us() {
  timeval now;
  gettimeofday(&now, nullptr);
  return static_cast<uint64_t>(now.tv_sec) * 1000000ull + static_cast<uint64_t>(now.tv_usec);
}

uint64_t uptime_us() {
  static uint64_t bootTime = 0;
  if (bootTime == 0) bootTime = host_time_us();
  return host_time_us() - bootTime;
}

Mem &require_mem() {
  if (activeMem == nullptr) {
    fprintf(stderr, "NPC virtual Mem is not initialized\n");
    abort();
  }
  return *activeMem;
}
}

Mem::Mem() {
  activeMem = this;
  pmem = static_cast<uint8_t *>(malloc(kPmemSize));
  assert(pmem != nullptr);
  memset(pmem, 0, kPmemSize);
}

Mem::~Mem() {
  if (activeMem == this) {
    activeMem = nullptr;
  }
  free(pmem);
}

bool Mem::isPmem(uint32_t addr, size_t n) const {
  return addr >= kPmemBase && static_cast<uint64_t>(addr - kPmemBase) + n <= kPmemSize;
}

void Mem::loadDataToRom(const uint8_t *data, size_t size) {
  assert(data != nullptr || size == 0);
  assert(size <= kPmemSize);
  memcpy(pmem, data, size);
}

void Mem::pmemReadChunk(uint32_t addr, uint8_t *buf, size_t n) const {
  if (buf == nullptr || n == 0) return;
  if (!isPmem(addr, n)) {
    memset(buf, 0, n);
    return;
  }
  memcpy(buf, pmem + (addr - kPmemBase), n);
}

uint32_t Mem::readPmemWord(uint32_t addr) const {
  uint32_t aligned = addr & ~0x3u;
  if (!isPmem(aligned, 4)) return 0;
  uint32_t offset = aligned - kPmemBase;
  return static_cast<uint32_t>(pmem[offset + 0]) |
         (static_cast<uint32_t>(pmem[offset + 1]) << 8) |
         (static_cast<uint32_t>(pmem[offset + 2]) << 16) |
         (static_cast<uint32_t>(pmem[offset + 3]) << 24);
}

uint32_t Mem::rtcWord(uint32_t addr) const {
  uint32_t aligned = addr & ~0x3u;
  if (aligned == kRtcUptime || aligned == kRtcUptime + 4) {
    uint64_t us = uptime_us();
    return aligned == kRtcUptime ? static_cast<uint32_t>(us) : static_cast<uint32_t>(us >> 32);
  }

  if (aligned == kRtcDate || aligned == kRtcDate + 4) {
    time_t timep = time(nullptr);
    tm local = {};
    localtime_r(&timep, &local);
    uint32_t lo = static_cast<uint32_t>(local.tm_sec) |
                  (static_cast<uint32_t>(local.tm_min) << 8) |
                  (static_cast<uint32_t>(local.tm_hour) << 16);
    uint32_t hi = static_cast<uint32_t>(local.tm_year + 1900) |
                  (static_cast<uint32_t>(local.tm_mon + 1) << 16) |
                  (static_cast<uint32_t>(local.tm_mday) << 24);
    return aligned == kRtcDate ? lo : hi;
  }

  if (aligned == (kSerialLsr & ~0x3u)) {
    return 0x20u << ((kSerialLsr & 0x3u) * 8);
  }

  return 0;
}

int Mem::pmemRead(int raddr) const {
  uint32_t addr = static_cast<uint32_t>(raddr);
  if (isPmem(addr & ~0x3u, 4)) {
    return static_cast<int>(readPmemWord(addr));
  }
  return static_cast<int>(rtcWord(addr));
}

void Mem::writePmem(uint32_t addr, uint32_t data, uint8_t mask) {
  uint32_t aligned = addr & ~0x3u;
  if (!isPmem(aligned, 4)) return;

  uint8_t *base = pmem + (aligned - kPmemBase);
  for (int i = 0; i < 4; ++i) {
    if ((mask & (1u << i)) != 0) {
      base[i] = static_cast<uint8_t>((data >> (i * 8)) & 0xffu);
    }
  }
}

void Mem::pmemWrite(int waddr, int wdata, char wmask) {
  uint32_t addr = static_cast<uint32_t>(waddr);
  uint8_t mask = static_cast<uint8_t>(wmask);
  if ((addr & ~0x3u) == (kSerialPort & ~0x3u)) {
    int lane = static_cast<int>(kSerialPort & 0x3u);
    if ((mask & (1u << lane)) != 0) {
      putchar(static_cast<char>((static_cast<uint32_t>(wdata) >> (lane * 8)) & 0xffu));
      fflush(stdout);
      difftestskip_ref_if_enabled();
    }
    return;
  }

  writePmem(addr, static_cast<uint32_t>(wdata), mask);
}

extern "C" void pmemread_chunk(uint32_t addr, uint8_t *buf, size_t n) {
  require_mem().pmemReadChunk(addr, buf, n);
}

extern "C" int pmemread(int raddr) {
  uint32_t aligned = static_cast<uint32_t>(raddr) & ~0x3u;
  if (aligned == Mem::kRtcUptime || aligned == Mem::kRtcUptime + 4 ||
      aligned == Mem::kRtcDate || aligned == Mem::kRtcDate + 4) {
    difftestskip_ref_if_enabled();
  }
  return require_mem().pmemRead(raddr);
}

extern "C" void pmemwrite(int waddr, int wdata, char wmask) {
  require_mem().pmemWrite(waddr, wdata, wmask);
}

extern "C" int npc_pmem_read(int raddr) {
  return pmemread(raddr);
}

extern "C" void npc_pmem_write(int waddr, int wdata, char wmask) {
  pmemwrite(waddr, wdata, wmask);
}
