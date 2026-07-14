#include "runtime/platform/memory.h"

#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>

#include "runtime/base/host_clock.h"
#include "runtime/platform/memory_map.h"

#ifndef CONFIG_RESET_PC
#define CONFIG_RESET_PC 0x30000000u
#endif

Memory::Memory(HostClock &clock) : clock(clock) {
  pmem = static_cast<uint8_t *>(malloc(kPmemSize));
  psramMem = static_cast<uint8_t *>(malloc(kPsramSize));
#ifndef CONFIG_NPC_VIRTUAL_SOC
  for (int i = 0; i < 4; ++i) {
    sdramMem[i] = static_cast<uint16_t *>(malloc(sizeof(uint16_t) * kSdramHalfwords));
  }
#endif

  assert(pmem != nullptr);
  assert(psramMem != nullptr);
#ifndef CONFIG_NPC_VIRTUAL_SOC
  for (int i = 0; i < 4; ++i) {
    assert(sdramMem[i] != nullptr);
  }
#endif

  memset(pmem, 0, kPmemSize);
  memset(psramMem, 0, kPsramSize);
#ifndef CONFIG_NPC_VIRTUAL_SOC
  for (int i = 0; i < 4; ++i) {
    memset(sdramMem[i], 0, sizeof(uint16_t) * kSdramHalfwords);
  }
#endif
}

Memory::~Memory() {
  if (pmem != nullptr) {
    free(pmem);
  }
  if (psramMem != nullptr) {
    free(psramMem);
  }
#ifndef CONFIG_NPC_VIRTUAL_SOC
  for (int i = 0; i < 4; ++i) {
    if (sdramMem[i] != nullptr) {
      free(sdramMem[i]);
    }
  }
#endif
}

void Memory::flashRead(int32_t addr, int32_t *data) const {
  uint32_t pcOffset = static_cast<uint32_t>(addr) & 0xfffffffcu;
  uint32_t inst = static_cast<uint32_t>(pmem[pcOffset + 3]) |
                  static_cast<uint32_t>(pmem[pcOffset + 2]) << 8 |
                  static_cast<uint32_t>(pmem[pcOffset + 1]) << 16 |
                  static_cast<uint32_t>(pmem[pcOffset + 0]) << 24;
  *data = static_cast<int32_t>(inst);
}

void Memory::mromRead(int32_t addr, int32_t *data) const {
  uint32_t pcOffset = (static_cast<uint32_t>(addr) - 0x20000000u) & 0xfffffffcu;
  uint32_t inst = static_cast<uint32_t>(pmem[pcOffset + 0]) |
                  static_cast<uint32_t>(pmem[pcOffset + 1]) << 8 |
                  static_cast<uint32_t>(pmem[pcOffset + 2]) << 16 |
                  static_cast<uint32_t>(pmem[pcOffset + 3]) << 24;
  *data = static_cast<int32_t>(inst);
}

void Memory::psramReadByte(int32_t addr, uint8_t *data) const {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  *data = psramMem[uaddr];
}

void Memory::psramWriteByte(int32_t addr, uint8_t data) {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  psramMem[uaddr] = data;
}

void Memory::sdramReadHalfwordChip(int chip, int32_t addr, uint16_t *data) const {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  assert(chip >= 0 && chip < 4);
  assert(uaddr < kSdramHalfwords);
  *data = sdramMem[chip][uaddr];
}

void Memory::sdramWriteHalfwordChip(int chip, int32_t addr, uint16_t data, uint8_t mask) {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  assert(chip >= 0 && chip < 4);
  assert(uaddr < kSdramHalfwords);
  uint16_t old = sdramMem[chip][uaddr];
  uint16_t next = old;
  if (mask & 0x1) next = (next & 0xff00u) | (data & 0x00ffu);
  if (mask & 0x2) next = (next & 0x00ffu) | (data & 0xff00u);
  sdramMem[chip][uaddr] = next;
}

uint32_t Memory::sdramLinearHalfaddrFromBus(uint32_t addr) {
  uint32_t offset = addr - kSdramBase;
  uint32_t col = (offset >> 2) & 0x1ffu;
  uint32_t row = (offset >> 13) & 0x7ffu;
  uint32_t bank = (offset >> 11) & 0x3u;
  return (bank << 22) | (row << 9) | col;
}

uint8_t Memory::readSdramByte(uint32_t addr) const {
  uint32_t offset = addr - kSdramBase;
  uint32_t rank = offset >> 24;
  uint32_t halfaddr = sdramLinearHalfaddrFromBus(addr);
  uint32_t chip = rank * 2 + ((addr >> 1) & 0x1u);
  uint32_t shift = (addr & 0x1u) * 8u;
  assert(chip < 4);
  return static_cast<uint8_t>((sdramMem[chip][halfaddr] >> shift) & 0xffu);
}

void Memory::writeSdramByte(uint32_t addr, uint8_t data) {
  uint32_t offset = addr - kSdramBase;
  uint32_t rank = offset >> 24;
  uint32_t halfaddr = sdramLinearHalfaddrFromBus(addr);
  uint32_t chip = rank * 2 + ((addr >> 1) & 0x1u);
  uint32_t shift = (addr & 0x1u) * 8u;
  assert(chip < 4);
  uint16_t old = sdramMem[chip][halfaddr];
  uint16_t next = (old & ~(0xffu << shift)) | (static_cast<uint16_t>(data) << shift);
  sdramMem[chip][halfaddr] = next;
}

void Memory::loadDataToSdram(const uint8_t *data, size_t size) {
  assert(size <= static_cast<size_t>(kSdramSize));
  for (size_t i = 0; i < size; ++i) {
    writeSdramByte(kSdramBase + static_cast<uint32_t>(i), data[i]);
  }
}

void Memory::loadDataToRom(const uint8_t *data, size_t size) {
#ifdef CONFIG_NPC_VIRTUAL_SOC
  assert(size <= static_cast<size_t>(kPmemSize));
  memcpy(pmem + (kProgramBase - kPmemBase), data, size);
#else
  if (CONFIG_RESET_PC >= kSdramBase && CONFIG_RESET_PC < kSdramBase + kSdramSize) {
    loadDataToSdram(data, size);
  } else {
    assert(size <= static_cast<size_t>(kFlashSize));
    assert(static_cast<uint64_t>(kFlashBase - kPmemBase) + size <= static_cast<uint64_t>(kPmemSize));
    memcpy(pmem + (kFlashBase - kPmemBase), data, size);
  }
#endif
}

void Memory::pmemReadChunk(uint32_t addr, uint8_t *buf, size_t n) const {
  if (buf == nullptr) return;

#ifndef CONFIG_NPC_VIRTUAL_SOC
  if (addr >= kSdramBase && static_cast<uint64_t>(addr - kSdramBase) + n <= (1ull << 25)) {
    for (size_t i = 0; i < n; ++i) {
      buf[i] = readSdramByte(addr + static_cast<uint32_t>(i));
    }
    return;
  }
#endif

  if (addr >= kPsramBase && static_cast<uint64_t>(addr - kPsramBase) + n <= kPsramSize) {
    memcpy(buf, psramMem + (addr - kPsramBase), n);
    return;
  }

  long offset = static_cast<unsigned int>(addr) - kPmemBase;
  if (offset < 0 || offset + static_cast<long>(n) > kPmemSize) return;
  memcpy(buf, pmem + offset, n);
}

int Memory::pmemRead(int raddr) const {
#ifndef CONFIG_NPC_VIRTUAL_SOC
  uint32_t uaddr = static_cast<uint32_t>(raddr);
  if (uaddr >= kSdramBase && static_cast<uint64_t>(uaddr - kSdramBase) + 4 <= kSdramSize) {
    uint32_t alignAddr = uaddr & ~0x3u;
    uint32_t data = static_cast<uint32_t>(readSdramByte(alignAddr + 0)) |
                    static_cast<uint32_t>(readSdramByte(alignAddr + 1)) << 8 |
                    static_cast<uint32_t>(readSdramByte(alignAddr + 2)) << 16 |
                    static_cast<uint32_t>(readSdramByte(alignAddr + 3)) << 24;
    return static_cast<int>(data);
  }
#endif

  long offset = static_cast<unsigned int>(raddr) - kPmemBase;
  long alignOffset = offset & ~0x3u;
  if (alignOffset < 0 || alignOffset + 4 > kPmemSize) return 0;

#ifndef CONFIG_NPC_VIRTUAL_SOC
  if (alignOffset + kPmemBase == RTC_ADDR || alignOffset + kPmemBase == RTC_UP_ADDR) {
    if (alignOffset + kPmemBase == RTC_ADDR) {
      time_t timep;
      struct tm *p;
      time(&timep);
      p = localtime(&timep);
      uint32_t *rtcLowAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_ADDR - kPmemBase));
      uint32_t *rtcHighAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_ADDR - kPmemBase + 4));
      *rtcLowAddr = p->tm_sec | (p->tm_min << 8) | (p->tm_hour << 16);
      *rtcHighAddr = (p->tm_year + 1900) | ((p->tm_mon + 1) << 16) | (p->tm_mday << 24);
    } else {
      uint64_t us = clock.elapsedMicros();
      uint32_t *rtcUsLowAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_UP_ADDR - kPmemBase));
      uint32_t *rtcUsHighAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_UP_ADDR - kPmemBase + 4));
      *rtcUsLowAddr = static_cast<uint32_t>(us & 0xffffffffu);
      *rtcUsHighAddr = static_cast<uint32_t>(us >> 32);
    }
  }
#endif

  return *reinterpret_cast<uint32_t *>(pmem + alignOffset);
}

void Memory::pmemWrite(int waddr, int wdata, char wmask) {
#ifndef CONFIG_NPC_VIRTUAL_SOC
  uint32_t uaddr = static_cast<uint32_t>(waddr);
  if (uaddr >= kSdramBase && static_cast<uint64_t>(uaddr - kSdramBase) + 4 <= kSdramSize) {
    uint32_t alignAddr = uaddr & ~0x3u;
    for (int i = 0; i < 4; ++i) {
      if (wmask & (1 << i)) {
        writeSdramByte(alignAddr + static_cast<uint32_t>(i), static_cast<uint8_t>(wdata >> (i * 8)));
      }
    }
    return;
  }
#endif

  long offset = static_cast<unsigned int>(waddr) - kPmemBase;
  long alignOffset = offset & ~0x3u;
  if (alignOffset < 0 || alignOffset + 4 > kPmemSize) return;

  uint32_t *paddr = reinterpret_cast<uint32_t *>(pmem + alignOffset);
  uint32_t oldData = *paddr;
  uint32_t wmaskU32 = 0;
  for (int i = 0; i < 4; ++i) {
    if (wmask & (1 << i)) {
      wmaskU32 |= (0xffu << (i * 8));
    }
  }
  uint32_t newData = (oldData & ~wmaskU32) | (static_cast<uint32_t>(wdata) & wmaskU32);
  *paddr = newData;
}

uint64_t Memory::elapsedMicros() const { return clock.elapsedMicros(); }
