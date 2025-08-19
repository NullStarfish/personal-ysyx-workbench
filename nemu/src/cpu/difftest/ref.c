/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "isa-def.h"
#include <isa.h>
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>


typedef struct {
  uint32_t gpr[32];
  uint32_t pc;
 } dut_cpu_state;

// In your NEMU project's difftest_ref.c
__EXPORT void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction) {
  if (direction == DIFFTEST_TO_REF) {
    // Copy memory from DUT (NPC) to REF (NEMU)
    for (size_t i = 0; i < n; i++) {
      // Correctly write one byte at a time to the correct address
      paddr_write(addr + i, 1, ((uint8_t*)buf)[i]);
    }
  } else {
    assert(0);
  }
}


__EXPORT void difftest_regcpy(void *dut, bool direction) {
  dut_cpu_state* dut_regs = (dut_cpu_state*)dut;

  if (direction == DIFFTEST_TO_REF) {
    // Copy registers from DUT (NPC) to REF (NEMU)
    for (int i = 0; i < 32; i++) {
      cpu.gpr[i] = dut_regs->gpr[i];
    }
    cpu.pc = dut_regs->pc;
  } else {
    // Copy registers from REF (NEMU) to DUT (NPC)
    for (int i = 0; i < 32; i++) {
      dut_regs->gpr[i] = cpu.gpr[i];
    }
    dut_regs->pc = cpu.pc;
  }
}

__EXPORT void difftest_exec(uint64_t n) {
  cpu_exec(n);
  //assert(0);
}

__EXPORT void difftest_raise_intr(word_t NO) {
  assert(0);
}

__EXPORT void difftest_init(int port) {
  void init_mem();
  init_mem();
  /* Perform ISA dependent initialization. */
  init_isa();
}
