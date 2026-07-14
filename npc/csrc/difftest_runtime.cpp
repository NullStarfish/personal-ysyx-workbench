#include "difftest_runtime.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <limits.h>

#include "mem.h"
#include "runtime/runtime.h"
#include "sim.h"


#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

#ifndef IMAGE_BASE_ADDR
#define IMAGE_BASE_ADDR 0xa0000000u
#endif

bool difftestis_enabled = false;
Difftest *activeDifftest = nullptr;

namespace {
constexpr uint32_t kImageBase = IMAGE_BASE_ADDR;
constexpr uint32_t kDifftestMemSize = 0x02000000u;

uint32_t sign_extend(uint32_t value, int bits) {
  uint32_t mask = 1u << (bits - 1);
  return (value ^ mask) - mask;
}

bool in_difftestmem(uint32_t addr, uint32_t len) {
  return len <= kDifftestMemSize &&
         addr >= kImageBase &&
         addr - kImageBase <= kDifftestMemSize - len;
}
}

void difftestskip_ref_if_enabled() {
#ifdef CONFIG_DIFFTEST
  if (activeDifftest != nullptr) {
    activeDifftest->skipRef();
  }
#endif
}

Difftest::Difftest() {
  activeDifftest = this;
}

Difftest::~Difftest() {
  shutdown();
  if (activeDifftest == this) activeDifftest = nullptr;
}

void Difftest::setRefSoFile(const char *path) { refSoFile = path == nullptr ? "" : path; }

void Difftest::skipRef() { isSkipRef = true; }

void Difftest::init(long imgSize) {
#ifdef CONFIG_DIFFTEST
  shutdown();
  if (refSoFile.empty()) return;

  char *nemuHome = getenv("NEMU_HOME");
  if (nemuHome == nullptr) {
    printf("\n[NPC ERROR] NEMU_HOME environment variable is not set.\n");
    exit(1);
  }

  char soFullPath[PATH_MAX];
  if (refSoFile[0] == '/') snprintf(soFullPath, sizeof(soFullPath), "%s", refSoFile.c_str());
  else snprintf(soFullPath, sizeof(soFullPath), "%s/%s", nemuHome, refSoFile.c_str());

  printf("Attempting to open reference simulator: %s\n", soFullPath);
  refHandle = dlopen(soFullPath, RTLD_LAZY);
  if (refHandle == nullptr) {
    printf("\n[NPC ERROR] Cannot open reference simulator '%s'\n", soFullPath);
    printf("dlerror: %s\n\n", dlerror());
    exit(1);
  }

  refMemcpy = reinterpret_cast<RefMemcpy>(dlsym(refHandle, "difftest_memcpy"));
  refRegcpy = reinterpret_cast<RefRegcpy>(dlsym(refHandle, "difftest_regcpy"));
  refExec = reinterpret_cast<RefExec>(dlsym(refHandle, "difftest_exec"));
  refInit = reinterpret_cast<RefInit>(dlsym(refHandle, "difftest_init"));
  if (refMemcpy == nullptr || refRegcpy == nullptr || refExec == nullptr || refInit == nullptr) {
    printf("Error: API symbols not found in reference simulator.\n");
    exit(1);
  }

  printf("Differential testing: armed, waiting for SDRAM PC\n");
  difftestis_enabled = false;
  refReady = true;
  imageSize = imgSize;
  refInit(0);
#else
  (void)imgSize;
#endif
}

void Difftest::shutdown() {
  difftestis_enabled = false;
  refReady = false;
  isSkipRef = false;
  hasLastDutState = false;
  refMemcpy = nullptr;
  refRegcpy = nullptr;
  refExec = nullptr;
  refInit = nullptr;
  if (refHandle != nullptr) {
    dlclose(refHandle);
    refHandle = nullptr;
  }
}

void Difftest::step() {
#ifdef CONFIG_DIFFTEST
  if (refReady && !difftestis_enabled) {
    if (!armAtSdram()) return;
    return;
  }
  if (!difftestis_enabled) return;

  riscv32_CPU_state dut = cpu.dutState();
  uint32_t inst = cpu.inst();

  if (isSkipRef) {
    refRegcpy(&dut, DIFFTEST_TO_REF);
    remember(dut);
    isSkipRef = false;
    return;
  }

  if (shouldSkipRefForInst(inst)) {
    refRegcpy(&dut, DIFFTEST_TO_REF);
    remember(dut);
    return;
  }

  refExec(1);

  riscv32_CPU_state ref;
  refRegcpy(&ref, DIFFTEST_TO_DUT);
  checkregs(dut, ref);
  refRegcpy(&ref, DIFFTEST_TO_REF);
  remember(dut);
#endif
}

bool Difftest::armAtSdram() {
  uint32_t retirePc = cpu.retirePc();
  if (retirePc < kImageBase || retirePc >= kImageBase + kDifftestMemSize) {
    return false;
  }

  uint8_t *guestMem = static_cast<uint8_t *>(malloc(imageSize));
  mem.pmemReadChunk(kImageBase, guestMem, imageSize);
  refMemcpy(kImageBase, guestMem, imageSize, DIFFTEST_TO_REF);
  free(guestMem);

  lastDutState = cpu.dutState();
  hasLastDutState = true;
  refRegcpy(&lastDutState, DIFFTEST_TO_REF);
  difftestis_enabled = true;
  printf("Differential testing: ON from SDRAM PC 0x%08x\n", retirePc);
  return true;
}

void Difftest::remember(const riscv32_CPU_state &dut) {
  lastDutState = dut;
  hasLastDutState = true;
}

bool Difftest::isMemoryInstruction(uint32_t inst, uint32_t *addr, uint32_t *len) const {
  if (!hasLastDutState) return false;

  uint32_t opcode = inst & 0x7fu;
  uint32_t funct3 = (inst >> 12) & 0x7u;
  int rs1 = (inst >> 15) & 0x1f;
  uint32_t imm = 0;

  if (opcode == 0x03u) imm = sign_extend(inst >> 20, 12);
  else if (opcode == 0x23u) {
    imm = ((inst >> 7) & 0x1fu) | (((inst >> 25) & 0x7fu) << 5);
    imm = sign_extend(imm, 12);
  } else {
    return false;
  }

  switch (funct3 & 0x3u) {
    case 0: *len = 1; break;
    case 1: *len = 2; break;
    default: *len = 4; break;
  }
  *addr = lastDutState.gpr[rs1] + imm;
  return true;
}

bool Difftest::shouldSkipRefForInst(uint32_t inst) const {
  uint32_t opcode = inst & 0x7fu;
  uint32_t funct3 = (inst >> 12) & 0x7u;

  if (opcode == 0x73u && funct3 != 0) {
    uint32_t csr = inst >> 20;
    switch (csr) {
      case 0x300u:
      case 0x305u:
      case 0x341u:
      case 0x342u:
        break;
      default:
        return true;
    }
  }

  uint32_t addr = 0;
  uint32_t len = 0;
  if (!isMemoryInstruction(inst, &addr, &len)) return false;
  return !in_difftestmem(addr, len);
}

void Difftest::checkregs(const riscv32_CPU_state &dut, const riscv32_CPU_state &ref) {
  bool mismatch = false;

  for (int i = 0; i < 32; i++) {
    if (dut.gpr[i] != ref.gpr[i]) {
      printf("Difftest mismatch at GPR[%d]: DUT=0x%08x, REF=0x%08x\n", i, dut.gpr[i], ref.gpr[i]);
      mismatch = true;
    }
  }
  if (dut.pc != ref.pc) {
    printf("Difftest mismatch at PC: DUT=0x%08x, REF=0x%08x\n", dut.pc, ref.pc);
    mismatch = true;
  }
  if (dut.csrs.mcause != ref.csrs.mcause) {
    printf("Difftest mismatch at mcause: DUT=0x%08x, REF=0x%08x\n", dut.csrs.mcause, ref.csrs.mcause);
    mismatch = true;
  }
  if (dut.csrs.mepc != ref.csrs.mepc) {
    printf("Difftest mismatch at mepc: DUT=0x%08x, REF=0x%08x\n", dut.csrs.mepc, ref.csrs.mepc);
    mismatch = true;
  }
  if (dut.csrs.mstatus != ref.csrs.mstatus) {
    printf("Difftest mismatch at mstatus: DUT=0x%08x, REF=0x%08x\n", dut.csrs.mstatus, ref.csrs.mstatus);
    mismatch = true;
  }
  if (dut.csrs.mtvec != ref.csrs.mtvec) {
    printf("Difftest mismatch at mtvec: DUT=0x%08x, REF=0x%08x\n", dut.csrs.mtvec, ref.csrs.mtvec);
    mismatch = true;
  }

  if (mismatch) {
    runtime.setAbort();
    printf("dut's regs:\n");
    cpu.isa_reg_display();
    printf("============================\n");
    printf("ref's regs:\n");
    for (int i = 0; i < 32; i++) {
      printf("x%d:  %x\n", i, ref.gpr[i]);
    }
    printf("pc:  %x\n", ref.pc);
    printf("mstatus:  %x\n", ref.csrs.mstatus);
    printf("mtvec:  %x\n", ref.csrs.mtvec);
    printf("mepc:  %x\n", ref.csrs.mepc);
    printf("mcause:  %x\n", ref.csrs.mcause);
  }
}

extern "C" void difftest_skip_ref_cpp() {
  difftestskip_ref_if_enabled();
}

void difftestskip_ref() {
  difftestskip_ref_if_enabled();
}
