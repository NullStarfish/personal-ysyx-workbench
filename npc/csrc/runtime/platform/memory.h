#ifndef NPC_SOC_MEM_H
#define NPC_SOC_MEM_H

#include <cstddef>
#include <cstdint>

class HostClock;

class Memory {
public:
  static constexpr long long kTargetSimFreq = 1000000;
#ifdef CONFIG_NPC_VIRTUAL_SOC
  static constexpr long kPmemSize = 0x08000000;
  static constexpr long kPmemBase = 0xa0000000L;
#else
  static constexpr long kPmemSize = 0x70000000;
  static constexpr long kPmemBase = 0x30000000L;
#endif
  static constexpr uint32_t kProgramBase = 0xa0000000u;
  static constexpr uint32_t kFlashBase = 0x30000000u;
  static constexpr uint32_t kFlashSize = 0x10000000u;
  static constexpr uint32_t kPsramBase = 0x80000000u;
  static constexpr uint32_t kPsramSize = 0x01000000u;
  static constexpr uint32_t kSdramBase = 0xa0000000u;
  static constexpr uint32_t kSdramSize = 0x02000000u;
  static constexpr uint32_t kSdramHalfwords = 0x01000000u;

  explicit Memory(HostClock &clock);
  ~Memory();

  void flashRead(int32_t addr, int32_t *data) const;
  void mromRead(int32_t addr, int32_t *data) const;
  void psramReadByte(int32_t addr, uint8_t *data) const;
  void psramWriteByte(int32_t addr, uint8_t data);
  void sdramReadHalfwordChip(int chip, int32_t addr, uint16_t *data) const;
  void sdramWriteHalfwordChip(int chip, int32_t addr, uint16_t data, uint8_t mask);
  void loadDataToRom(const uint8_t *data, size_t size);
  void pmemReadChunk(uint32_t addr, uint8_t *buf, size_t n) const;
  int pmemRead(int raddr) const;
  void pmemWrite(int waddr, int wdata, char wmask);
  uint64_t elapsedMicros() const;

private:
  static uint32_t sdramLinearHalfaddrFromBus(uint32_t addr);
  uint8_t readSdramByte(uint32_t addr) const;
  void writeSdramByte(uint32_t addr, uint8_t data);
  void loadDataToSdram(const uint8_t *data, size_t size);

  HostClock &clock;
  uint8_t *pmem = nullptr;
  uint8_t *psramMem = nullptr;
  uint16_t *sdramMem[4] = {nullptr, nullptr, nullptr, nullptr};
};

#endif
