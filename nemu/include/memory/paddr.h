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

#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>
// MROM: 0x20000000 ~ 0x20000fff (4KB)
#define SOC_MROM_BASE 0x20000000
#define SOC_MROM_SIZE 0x00001000

// SRAM: 0x0f000000 ~ 0x0fffffff (16MB)
#define SOC_SRAM_BASE 0x0f000000
#define SOC_SRAM_SIZE 0x01000000



#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)


#ifdef CONFIG_ENGINE_INTERPRETER
#define RESET_VECTOR (SOC_MROM_BASE + CONFIG_PC_RESET_OFFSET)
#else
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)
#endif

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);


static inline bool in_soc_mrom(paddr_t addr) {
  return (addr >= SOC_MROM_BASE && addr < SOC_MROM_BASE + SOC_MROM_SIZE);
}

static inline bool in_soc_sram(paddr_t addr) {
  return (addr >= SOC_SRAM_BASE && addr < SOC_SRAM_BASE + SOC_SRAM_SIZE);
}



static inline bool in_pmem(paddr_t addr) {
#ifdef CONFIG_ENGINE_INTERPRETER
  return in_soc_mrom(addr) || in_soc_sram(addr);
#else
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
#endif
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

#endif
