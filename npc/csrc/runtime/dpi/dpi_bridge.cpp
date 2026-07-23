#include "runtime/dpi/dpi_bridge.h"

#include <cstdio>
#include <cstdlib>
#include <cstdint>

#include "runtime/base/retire_event.h"
#include "runtime/execution/simulation.h"
#include "runtime/platform/memory.h"
#include "runtime/platform/memory_map.h"
#include "runtime/services/difftest.h"
#include "runtime/services/ila_engine.h"
#include "runtime/services/sim_counter.h"
#include "svdpi.h"
#include <verilated.h>

namespace {
DpiBridge *activeBridge = nullptr;

DpiBridge &bridge() {
  if (activeBridge == nullptr) {
    fprintf(stderr, "DPI bridge is not initialized\n");
    abort();
  }
  return *activeBridge;
}
}

DpiBridge::DpiBridge(Simulation &simulation, Memory &memory, Difftest &difftest, SimCounterBank &counters, IlaEngine &ila)
    : simulation(simulation), memory(memory), difftest(difftest), counters(counters), ila(ila) {}
DpiBridge::~DpiBridge() { unbind(); }
void DpiBridge::bind() { activeBridge = this; }
void DpiBridge::unbind() {
  if (activeBridge == this) activeBridge = nullptr;
}

extern "C" void dpi_update_state(int pc, int dnpc, int reg_wen, int reg_addr, int reg_data,
                                 const svBitVecVal *gprs, int mtvec, int mepc, int mstatus,
                                 int mcause, int inst, int instType) {
#ifdef CONFIG_RETIRE_TRACE
  RetireEvent event;
  event.pc = static_cast<uint32_t>(pc);
  event.dnpc = static_cast<uint32_t>(dnpc);
  event.inst = static_cast<uint32_t>(inst);
  event.instType = static_cast<uint32_t>(instType);
  event.regWen = reg_wen != 0;
  event.regAddr = static_cast<uint32_t>(reg_addr);
  event.regData = static_cast<uint32_t>(reg_data);
  for (int i = 0; i < 32; ++i) event.gpr[i] = gprs[i];
  event.csrs.mtvec = static_cast<uint32_t>(mtvec);
  event.csrs.mepc = static_cast<uint32_t>(mepc);
  event.csrs.mstatus = static_cast<uint32_t>(mstatus);
  event.csrs.mcause = static_cast<uint32_t>(mcause);
  bridge().simulation.acceptRetire(event);
#else
  (void)pc; (void)dnpc; (void)reg_wen; (void)reg_addr; (void)reg_data; (void)gprs;
  (void)mtvec; (void)mepc; (void)mstatus; (void)mcause; (void)inst; (void)instType;
#endif
}

extern "C" void ebreak() { bridge().simulation.handleEbreak(); }
extern "C" void assert_fail_msg() { bridge().simulation.printFailureContext(); }
extern "C" void difftest_skip_ref_cpp() { bridge().difftest.skipRef(); }

extern "C" void pmemread_chunk(uint32_t addr, uint8_t *buf, size_t n) {
  bridge().memory.pmemReadChunk(addr, buf, n);
}

extern "C" int pmemread(int raddr) {
#ifndef CONFIG_NPC_VIRTUAL_SOC
  const unsigned int aligned = static_cast<unsigned int>(raddr) & ~0x3u;
  if (aligned == RTC_ADDR || aligned == RTC_UP_ADDR || aligned == RTC_ADDR + 4 || aligned == RTC_UP_ADDR + 4) {
    bridge().difftest.skipRef();
  }
#endif
  return bridge().memory.pmemRead(raddr);
}

extern "C" void pmemwrite(int waddr, int wdata, char wmask) {
#ifndef CONFIG_NPC_VIRTUAL_SOC
  if ((static_cast<unsigned int>(waddr) & ~0x3u) == SERIAL_PORT) {
    putchar(static_cast<char>(wdata));
    fflush(stdout);
    bridge().difftest.skipRef();
  }
#endif
  bridge().memory.pmemWrite(waddr, wdata, wmask);
}

extern "C" void flash_read(int32_t addr, int32_t *data) { bridge().memory.flashRead(addr, data); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { bridge().memory.mromRead(addr, data); }
extern "C" void psram_read_byte(int32_t addr, uint8_t *data) { bridge().memory.psramReadByte(addr, data); }
extern "C" void psram_write_byte(int32_t addr, uint8_t data) { bridge().memory.psramWriteByte(addr, data); }
extern "C" void clint_mtime_read(uint64_t *mtime) {
  if (mtime != nullptr) *mtime = bridge().memory.elapsedMicros();
}
extern "C" void sdram_read_halfword_chip(int chip, int32_t addr, uint16_t *data) {
  bridge().memory.sdramReadHalfwordChip(chip, addr, data);
}
extern "C" void sdram_write_halfword_chip(int chip, int32_t addr, uint16_t data, uint8_t mask) {
  bridge().memory.sdramWriteHalfwordChip(chip, addr, data, mask);
}

extern "C" int sim_counter_alloc(const char *tag, const char *name) {
  return bridge().counters.allocate(tag, name);
}
extern "C" void sim_counter_add(int id, uint64_t delta) { bridge().counters.add(id, delta); }
extern "C" uint64_t sim_counter_read(int id) { return bridge().counters.read(id); }
extern "C" void sim_counter_register_ratio(const char *tag, const char *name,
                                             const char *numeratorTag, const char *numeratorName,
                                             const char *denominatorTag, const char *denominatorName,
                                             int percentage) {
  bridge().counters.registerRatio(tag, name, numeratorTag, numeratorName,
                                  denominatorTag, denominatorName, percentage != 0);
}

extern "C" int ila_source_allocate(const char *name, const char *schema,
                                     int packedWidth) {
  return bridge().ila.allocateSource(name, schema, packedWidth);
}
extern "C" void ila_sample(int id, const svOpenArrayHandle sampleWords) {
  const auto *packed =
      static_cast<const svBitVecVal *>(svGetArrayPtr(sampleWords));
  if (packed == nullptr) {
    fprintf(stderr, "ILA DPI sample array is not contiguous\n");
    abort();
  }
  bridge().ila.sample(id, packed, Verilated::time());
}
