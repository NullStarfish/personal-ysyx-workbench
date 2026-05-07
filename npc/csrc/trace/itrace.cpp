#include "trace/itrace.h"

#include <cstdio>
#include <cstring>

#include "log/log.h"

#ifndef DIFFTEST_ON
#include "tools/disasm.h"
#endif

namespace {
constexpr int kRingCapacity = 16;
constexpr int kLogbufSize = 128;

struct IRingBuffer {
  char instLog[kRingCapacity][kLogbufSize];
  int writeIndex = 0;
  int size = 0;
};

IRingBuffer iringBuffer;
}

void log_and_trace(uint32_t pc, uint32_t inst) {
  char logbuf[kLogbufSize];
  char *p = logbuf;
  char *end = p + sizeof(logbuf);

  p += snprintf(p, end - p, "0x%08x: %02x %02x %02x %02x", pc, (uint8_t)(inst & 0xff),
                (uint8_t)((inst >> 8) & 0xff), (uint8_t)((inst >> 16) & 0xff),
                (uint8_t)((inst >> 24) & 0xff));

#ifdef CONFIG_ITRACE
  int spaceLen = 28 - (p - logbuf);
  if (spaceLen < 0) {
    spaceLen = 0;
  }
  memset(p, ' ', spaceLen);
  p += spaceLen;
  disassemble(p, end - p, pc, (uint8_t *)&inst, 4);
#endif

  log_write("%s\n", logbuf);

  strncpy(iringBuffer.instLog[iringBuffer.writeIndex], logbuf, kLogbufSize - 1);
  iringBuffer.instLog[iringBuffer.writeIndex][kLogbufSize - 1] = '\0';
  iringBuffer.writeIndex = (iringBuffer.writeIndex + 1) % kRingCapacity;
  if (iringBuffer.size < kRingCapacity) {
    iringBuffer.size++;
  }
}

void print_iring_buffer() {
  printf("\nInstruction Ring Buffer (most recent first):\n");
  printf("--------------------------------------------\n");

  int index = iringBuffer.writeIndex;
  for (int i = 0; i < iringBuffer.size; i++) {
    index = (index - 1 + kRingCapacity) % kRingCapacity;
    printf("  %s\n", iringBuffer.instLog[index]);
  }
  printf("--------------------------------------------\n");
}
