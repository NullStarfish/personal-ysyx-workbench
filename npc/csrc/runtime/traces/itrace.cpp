#include "runtime/traces/itrace.h"

#include <cstdio>
#include <cstring>

#include "log/log.h"

#ifdef CONFIG_ITRACE
#include "tools/disasm.h"
#endif

namespace {
constexpr size_t kLogbufSize = 128;
}

ITrace::ITrace() { reset(); }

void ITrace::init() {
  reset();
#ifdef CONFIG_ITRACE
  setEnabled(true);
  printf("Disassembler for NPC log: ON\n");
  init_disasm();
#else
  setEnabled(false);
#endif
}

void ITrace::reset() {
  ring.fill({});
  writeIndex = 0;
  size = 0;
}

void ITrace::record(uint32_t pc, uint32_t inst) {
  char logbuf[kLogbufSize];
  char *p = logbuf;
  char *end = p + sizeof(logbuf);

  p += snprintf(p, end - p, "0x%08x: %02x %02x %02x %02x", pc, static_cast<uint8_t>(inst & 0xff),
                static_cast<uint8_t>((inst >> 8) & 0xff), static_cast<uint8_t>((inst >> 16) & 0xff),
                static_cast<uint8_t>((inst >> 24) & 0xff));

#ifdef CONFIG_ITRACE
  int spaceLen = 28 - (p - logbuf);
  if (spaceLen < 0) spaceLen = 0;
  memset(p, ' ', spaceLen);
  p += spaceLen;
  disassemble(p, end - p, pc, reinterpret_cast<uint8_t *>(&inst), 4);
#endif

  log_write("%s\n", logbuf);
  ring[writeIndex] = logbuf;
  writeIndex = (writeIndex + 1) % kRingCapacity;
  if (size < kRingCapacity) ++size;
}

void ITrace::printRecent() const {
  printf("\nInstruction Ring Buffer (most recent first):\n");
  printf("--------------------------------------------\n");

  size_t index = writeIndex;
  for (size_t i = 0; i < size; ++i) {
    index = (index + kRingCapacity - 1) % kRingCapacity;
    printf("  %s\n", ring[index].c_str());
  }
  printf("--------------------------------------------\n");
}
