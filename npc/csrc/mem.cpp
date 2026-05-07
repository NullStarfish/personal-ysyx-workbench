#include "mem.h"

#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <sys/time.h>

#include "device.h"
#include "difftest_runtime.h"

namespace {
Mem *activeMem = nullptr;
}

static uint64_t memget_time_internal() {
  struct timeval now;
  gettimeofday(&now, nullptr);
  return static_cast<uint64_t>(now.tv_sec) * 1000000ull + static_cast<uint64_t>(now.tv_usec);
}

static uint64_t memget_time() {
  static uint64_t bootTime = 0;
  if (bootTime == 0) bootTime = memget_time_internal();
  return memget_time_internal() - bootTime;
}

Mem::Mem() {
  activeMem = this;
  pmem = static_cast<uint8_t *>(malloc(kPmemSize));
  psramMem = static_cast<uint8_t *>(malloc(kPsramSize));
  for (int i = 0; i < 4; ++i) {
    sdramMem[i] = static_cast<uint16_t *>(malloc(sizeof(uint16_t) * kSdramHalfwords));
  }

  assert(pmem != nullptr);
  assert(psramMem != nullptr);
  for (int i = 0; i < 4; ++i) {
    assert(sdramMem[i] != nullptr);
  }

  memset(pmem, 0, kPmemSize);
  memset(psramMem, 0, kPsramSize);
  for (int i = 0; i < 4; ++i) {
    memset(sdramMem[i], 0, sizeof(uint16_t) * kSdramHalfwords);
  }
}

Mem::~Mem() {
  if (activeMem == this) {
    activeMem = nullptr;
  }
  if (pmem != nullptr) {
    free(pmem);
  }
  if (psramMem != nullptr) {
    free(psramMem);
  }
  for (int i = 0; i < 4; ++i) {
    if (sdramMem[i] != nullptr) {
      free(sdramMem[i]);
    }
  }
}

void Mem::flashRead(int32_t addr, int32_t *data) const {
  uint32_t pcOffset = static_cast<uint32_t>(addr) & 0xfffffffcu;
  uint32_t inst = static_cast<uint32_t>(pmem[pcOffset + 3]) |
                  static_cast<uint32_t>(pmem[pcOffset + 2]) << 8 |
                  static_cast<uint32_t>(pmem[pcOffset + 1]) << 16 |
                  static_cast<uint32_t>(pmem[pcOffset + 0]) << 24;
  *data = static_cast<int32_t>(inst);
}

void Mem::mromRead(int32_t addr, int32_t *data) const {
  uint32_t pcOffset = (static_cast<uint32_t>(addr) - 0x20000000u) & 0xfffffffcu;
  uint32_t inst = static_cast<uint32_t>(pmem[pcOffset + 0]) |
                  static_cast<uint32_t>(pmem[pcOffset + 1]) << 8 |
                  static_cast<uint32_t>(pmem[pcOffset + 2]) << 16 |
                  static_cast<uint32_t>(pmem[pcOffset + 3]) << 24;
  *data = static_cast<int32_t>(inst);
}

void Mem::psramReadByte(int32_t addr, uint8_t *data) const {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  *data = psramMem[uaddr];
}

void Mem::psramWriteByte(int32_t addr, uint8_t data) {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  psramMem[uaddr] = data;
}

void Mem::sdramReadHalfwordChip(int chip, int32_t addr, uint16_t *data) const {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  assert(chip >= 0 && chip < 4);
  assert(uaddr < kSdramHalfwords);
  *data = sdramMem[chip][uaddr];
}

void Mem::sdramWriteHalfwordChip(int chip, int32_t addr, uint16_t data, uint8_t mask) {
  uint32_t uaddr = static_cast<uint32_t>(addr);
  assert(chip >= 0 && chip < 4);
  assert(uaddr < kSdramHalfwords);
  uint16_t old = sdramMem[chip][uaddr];
  uint16_t next = old;
  if (mask & 0x1) next = (next & 0xff00u) | (data & 0x00ffu);
  if (mask & 0x2) next = (next & 0x00ffu) | (data & 0xff00u);
  sdramMem[chip][uaddr] = next;
}

uint32_t Mem::sdramLinearHalfaddrFromBus(uint32_t addr) {
  uint32_t offset = addr - kSdramBase;
  uint32_t col = (offset >> 2) & 0x1ffu;
  uint32_t row = (offset >> 13) & 0x7ffu;
  uint32_t bank = (offset >> 11) & 0x3u;
  return (bank << 22) | (row << 9) | col;
}

void Mem::loadDataToRom(const uint8_t *data, size_t size) {
  const uint32_t endAddr = kProgramBase + static_cast<uint32_t>(size);
  const uint32_t startRank = (kProgramBase - kSdramBase) >> 24;
  const uint32_t endRank = ((endAddr - 1) - kSdramBase) >> 24;
  assert(startRank == endRank);
  assert(startRank < 2);

  for (size_t off = 0; off < size; off += 4) {
    uint32_t addr = kProgramBase + static_cast<uint32_t>(off);
    uint32_t halfaddr = sdramLinearHalfaddrFromBus(addr);
    assert(halfaddr < kSdramHalfwords);
    uint16_t lower = 0;
    uint16_t upper = 0;
    if (off + 0 < size) lower |= static_cast<uint16_t>(data[off + 0]);
    if (off + 1 < size) lower |= static_cast<uint16_t>(data[off + 1]) << 8;
    if (off + 2 < size) upper |= static_cast<uint16_t>(data[off + 2]);
    if (off + 3 < size) upper |= static_cast<uint16_t>(data[off + 3]) << 8;
    sdramMem[startRank * 2 + 0][halfaddr] = lower;
    sdramMem[startRank * 2 + 1][halfaddr] = upper;
  }
}

void Mem::pmemReadChunk(uint32_t addr, uint8_t *buf, size_t n) const {
  if (buf == nullptr) return;

  if (addr >= kSdramBase && static_cast<uint64_t>(addr - kSdramBase) + n <= (1ull << 25)) {
    for (size_t i = 0; i < n; ++i) {
      uint32_t cur = addr + static_cast<uint32_t>(i);
      uint32_t rank = (cur - kSdramBase) >> 24;
      uint32_t halfaddr = sdramLinearHalfaddrFromBus(cur);
      assert(rank < 2);
      uint16_t lower = sdramMem[rank * 2 + 0][halfaddr];
      uint16_t upper = sdramMem[rank * 2 + 1][halfaddr];
      switch (cur & 0x3u) {
        case 0: buf[i] = lower & 0xffu; break;
        case 1: buf[i] = (lower >> 8) & 0xffu; break;
        case 2: buf[i] = upper & 0xffu; break;
        default: buf[i] = (upper >> 8) & 0xffu; break;
      }
    }
    return;
  }

  if (addr >= kPsramBase && static_cast<uint64_t>(addr - kPsramBase) + n <= kPsramSize) {
    memcpy(buf, psramMem + (addr - kPsramBase), n);
    return;
  }

  long offset = static_cast<unsigned int>(addr) - kPmemBase;
  if (offset < 0 || offset + static_cast<long>(n) > kPmemSize) return;
  memcpy(buf, pmem + offset, n);
}

int Mem::pmemRead(int raddr) const {
  long offset = static_cast<unsigned int>(raddr) - kPmemBase;
  long alignOffset = offset & ~0x3u;
  if (alignOffset < 0 || alignOffset + 4 > kPmemSize) return 0;

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
      uint64_t us = memget_time();
      uint32_t *rtcUsLowAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_UP_ADDR - kPmemBase));
      uint32_t *rtcUsHighAddr = reinterpret_cast<uint32_t *>(pmem + (RTC_UP_ADDR - kPmemBase + 4));
      *rtcUsLowAddr = static_cast<uint32_t>(us & 0xffffffffu);
      *rtcUsHighAddr = static_cast<uint32_t>(us >> 32);
    }
  }

  return *reinterpret_cast<uint32_t *>(pmem + alignOffset);
}

void Mem::pmemWrite(int waddr, int wdata, char wmask) {
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

namespace {
Mem &require_mem() {
  if (activeMem == nullptr) {
    fprintf(stderr, "Mem is not initialized\n");
    abort();
  }
  return *activeMem;
}
}

extern "C" void pmemread_chunk(uint32_t addr, uint8_t *buf, size_t n) {
  require_mem().pmemReadChunk(addr, buf, n);
}

extern "C" int pmemread(int raddr) {
  unsigned int aligned = static_cast<unsigned int>(raddr) & ~0x3u;
  if (aligned == RTC_ADDR || aligned == RTC_UP_ADDR || aligned == RTC_ADDR + 4 || aligned == RTC_UP_ADDR + 4) {
    difftestskip_ref_if_enabled();
  }
  return require_mem().pmemRead(raddr);
}

extern "C" void pmemwrite(int waddr, int wdata, char wmask) {
  if ((static_cast<unsigned int>(waddr) & ~0x3u) == SERIAL_PORT) {
    putchar(static_cast<char>(wdata));
    fflush(stdout);
    difftestskip_ref_if_enabled();
  }
  require_mem().pmemWrite(waddr, wdata, wmask);
}

extern "C" void flash_read(int32_t addr, int32_t *data) { require_mem().flashRead(addr, data); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { require_mem().mromRead(addr, data); }
extern "C" void psram_read_byte(int32_t addr, uint8_t *data) { require_mem().psramReadByte(addr, data); }
extern "C" void psram_write_byte(int32_t addr, uint8_t data) { require_mem().psramWriteByte(addr, data); }
extern "C" void sdram_read_halfword_chip(int chip, int32_t addr, uint16_t *data) {
  require_mem().sdramReadHalfwordChip(chip, addr, data);
}
extern "C" void sdram_write_halfword_chip(int chip, int32_t addr, uint16_t data, uint8_t mask) {
  require_mem().sdramWriteHalfwordChip(chip, addr, data, mask);
}
