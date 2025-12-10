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

#include "common.h"
#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

//==================difftest===========================
#ifdef CONFIG_ENGINE_INTERPRETER



static uint8_t *soc_mrom = NULL;
static uint8_t *soc_sram = NULL;

// 辅助函数：判断地址是否在 SoC 内存范围内


// 辅助函数：将 guest 地址转换为 host 指针
static uint8_t* soc_guest_to_host(paddr_t addr) {
  if (in_soc_mrom(addr)) return soc_mrom + (addr - SOC_MROM_BASE);
  if (in_soc_sram(addr)) return soc_sram + (addr - SOC_SRAM_BASE);
  return NULL;
}

#endif
//==================difftest===========================








#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
#endif

//由于虚拟内存机制：CONFIG_MABASE不为内存的开始
//我们引入pmem，直接对pmem数组（物理内存）进行读写操作



//paddr的位数是机器的位数，例如32、64位。paddr_t是物理地址类型，paddr_t的大小和机器的位数有关。
//pmem是物理内存的起始地址，CONFIG_MSIZE是物理
//physical memory: pmem
// 原有的 guest_to_host (保留用于非 SoC 模式或未命中的情况)
uint8_t* guest_to_host(paddr_t paddr) { 
#ifdef CONFIG_ENGINE_INTERPRETER
    // 如果是 Difftest 模式，先尝试转换 SoC 地址
    uint8_t *ret = soc_guest_to_host(paddr);
    if (ret) return ret;
#endif
    return pmem + paddr - CONFIG_MBASE; 
}

//CONFIG_MBASE是物理内存的起始地址，CONFIG_MSIZE是物理内存的大小
//返回一个host虚拟地址，这个地址是pmem数组的偏移量加上物理内存的起始地址
//host也是32位，但是存放的是一个uint8_t *类型的指针
//对返回的地址取*，就可以直接得到内存的值



paddr_t host_to_guest(uint8_t *haddr) { 
#ifdef CONFIG_ENGINE_INTERPRETER
    // 简单的反向映射 check
    if (haddr >= soc_mrom && haddr < soc_mrom + SOC_MROM_SIZE) 
        return SOC_MROM_BASE + (haddr - soc_mrom);
    if (haddr >= soc_sram && haddr < soc_sram + SOC_SRAM_SIZE) 
        return SOC_SRAM_BASE + (haddr - soc_sram);
#endif
    return haddr - pmem + CONFIG_MBASE; 
}

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
}

void init_mem() {
#ifdef CONFIG_ENGINE_INTERPRETER
  // SoC 模式：申请两块独立的内存
  soc_mrom = malloc(SOC_MROM_SIZE);
  soc_sram = malloc(SOC_SRAM_SIZE);
  assert(soc_mrom && soc_sram);
  Log("SoC Mode: MROM [" FMT_PADDR ", " FMT_PADDR "]", SOC_MROM_BASE, SOC_MROM_BASE + SOC_MROM_SIZE - 1);
  Log("SoC Mode: SRAM [" FMT_PADDR ", " FMT_PADDR "]", SOC_SRAM_BASE, SOC_SRAM_BASE + SOC_SRAM_SIZE - 1);
  
  // 仍然初始化原有的 pmem 以防止某些遗留代码崩溃，但不作为主要存储
  // 如果你想完全禁用原有 pmem，可以根据需要注释掉下面
#endif

#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  IFDEF(CONFIG_MEM_RANDOM, memset(pmem, rand(), CONFIG_MSIZE));
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
}


word_t paddr_read(paddr_t addr, int len) {
  IFDEF(CONFIG_MTRACE, Log("paddr_read: addr = " FMT_PADDR ", len = %d", addr, len);)

#ifdef CONFIG_ENGINE_INTERPRETER
  // SoC 模式：自定义范围检查
  if (likely(in_soc_mrom(addr) || in_soc_sram(addr))) {
      word_t ret = pmem_read(addr, len);
      IFDEF(CONFIG_MTRACE, Log("paddr read success: " FMT_WORD, ret);)
      return ret;
  }
#else
  // 原有 NEMU 模式
  if (likely(in_pmem(addr))) {
    word_t ret = pmem_read(addr, len);
    IFDEF(CONFIG_MTRACE, Log("paddr read success: " FMT_WORD, ret);)
    return ret;
  }
#endif

  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));
  out_of_bound(addr);
  return 0;
}




void paddr_write(paddr_t addr, int len, word_t data) {
  IFDEF(CONFIG_MTRACE, Log("paddr_write: addr = " FMT_PADDR ", len = %d, data = " FMT_WORD , addr, len, data);)

#ifdef CONFIG_ENGINE_INTERPRETER
  // SoC 模式：自定义范围检查
  // 注意：MROM 通常是只读的，但加载程序时需要写。Difftest 时通常允许写以便初始化。
  // 如果想模拟真实的 MROM 只读，可以在这里把 MROM 排除，或者加一个标志位控制初始化阶段。
  // 这里为了 difftest_memcpy 能正常工作，允许写入。
  if (likely(in_soc_mrom(addr) || in_soc_sram(addr))) {
    pmem_write(addr, len, data);
    IFDEF(CONFIG_MTRACE, Log("paddr write success: " FMT_WORD, data);)
    return;
  }
#else
  // 原有 NEMU 模式
  if (likely(in_pmem(addr))) { 
    pmem_write(addr, len, data); 
    IFDEF(CONFIG_MTRACE, Log("paddr write success: " FMT_WORD, data);)
    return; 
  }
#endif

  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  out_of_bound(addr);
}
