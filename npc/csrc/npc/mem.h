#ifndef NPC_VIRTUAL_MEM_H
#define NPC_VIRTUAL_MEM_H

#include <cstddef>
#include <cstdint>

class Mem {
public:
  static constexpr uint32_t kPmemBase = 0xa0000000u;
  static constexpr uint32_t kProgramBase = kPmemBase;
  static constexpr uint32_t kPmemSize = 128u * 1024u * 1024u;

  static constexpr uint32_t kSerialPort = 0x10000000u;
  static constexpr uint32_t kSerialLsr = kSerialPort + 5u;
  static constexpr uint32_t kRtcUptime = 0x02000000u;
  static constexpr uint32_t kRtcDate = 0x02000008u;

  Mem();
  ~Mem();

  void loadDataToRom(const uint8_t *data, size_t size);
  void pmemReadChunk(uint32_t addr, uint8_t *buf, size_t n) const;
  int pmemRead(int raddr) const;
  void pmemWrite(int waddr, int wdata, char wmask);

private:
  bool isPmem(uint32_t addr, size_t n = 1) const;
  uint32_t readPmemWord(uint32_t addr) const;
  void writePmem(uint32_t addr, uint32_t data, uint8_t mask);
  uint32_t rtcWord(uint32_t addr) const;

  uint8_t *pmem = nullptr;
};

#endif
