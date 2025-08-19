#include <stdio.h>
#include <string.h>
#include "itrace.h"
#include "log.h"

// ======================= CRITICAL FIX =======================
// Only include disasm.h if difftest is NOT on. This is the key to
// preventing compilation and linking issues in difftest mode.
#ifndef DIFFTEST_ON
  #include "tools/disasm.h"
#endif
// ============================================================

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

  // Step 1: Always log the PC and the hexadecimal instruction code.
  p += snprintf(p, end - p, "0x%08x: %02x %02x %02x %02x", pc,
    (uint8_t)(inst & 0xff), (uint8_t)((inst >> 8) & 0xff),
    (uint8_t)((inst >> 16) & 0xff), (uint8_t)((inst >> 24) & 0xff));

  // ======================= CRITICAL FIX =======================
  // Step 2: Only perform disassembly if difftest is OFF.
  // In difftest mode, NEMU is responsible for disassembly logging.
  // NPC should NOT attempt to call any disassembly function.
  #ifndef DIFFTEST_ON
    int space_len = 28 - (p - logbuf);
    if (space_len < 0) space_len = 0;
    memset(p, ' ', space_len);
    p += space_len;
    // This function will only be linked when DIFFTEST_ON is not defined.
    disassemble(p, end - p, pc, (uint8_t *)&inst, 4);
  #endif
  // ============================================================

  // Step 3: Write the final formatted string to the log file.
  log_write("%s\n", logbuf);

  // Step 4: Update the instruction ring buffer for debugging purposes.
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
