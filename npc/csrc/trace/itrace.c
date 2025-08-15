#include <stdio.h>
#include <string.h>
#include "itrace.h"
#include "disasm.h" // FIX: 路径已由 Makefile 管理
#include "itrace.h"
#include "log.h"    // FIX: 路径已由 Makefile 管理

#define IRING_CAPACITY 16
#define LOGBUF_SIZE 128

typedef struct {
  char inst_log[IRING_CAPACITY][LOGBUF_SIZE];
  int wrIdx;
  int size;
} IRingBuffer;

static IRingBuffer iring_buffer = { .wrIdx = 0, .size = 0 };

void log_and_trace(uint32_t pc, uint32_t inst) {
  char logbuf[LOGBUF_SIZE];
  char *p = logbuf;
  char *end = p + sizeof(logbuf);

  p += snprintf(p, end - p, "0x%08x: %02x %02x %02x %02x", pc,
    (uint8_t)(inst & 0xff), (uint8_t)((inst >> 8) & 0xff),
    (uint8_t)((inst >> 16) & 0xff), (uint8_t)((inst >> 24) & 0xff));

  int space_len = 28 - (p - logbuf);
  if (space_len < 0) space_len = 0;
  memset(p, ' ', space_len);
  p += space_len;

  disassemble(p, end - p, pc, (uint8_t *)&inst, 4);

  log_write("%s\n", logbuf);

  strncpy(iring_buffer.inst_log[iring_buffer.wrIdx], logbuf, LOGBUF_SIZE - 1);
  iring_buffer.inst_log[iring_buffer.wrIdx][LOGBUF_SIZE - 1] = '\0';

  iring_buffer.wrIdx = (iring_buffer.wrIdx + 1) % IRING_CAPACITY;
  if (iring_buffer.size < IRING_CAPACITY) {
    iring_buffer.size++;
  }
}

void print_iring_buffer() {
  printf("\nInstruction Ring Buffer (most recent first):\n");
  printf("--------------------------------------------\n");
  
  int idx = iring_buffer.wrIdx;
  for (int i = 0; i < iring_buffer.size; i++) {
    idx = (idx - 1 + IRING_CAPACITY) % IRING_CAPACITY;
    printf("  %s\n", iring_buffer.inst_log[idx]);
  }
  printf("--------------------------------------------\n");
}
