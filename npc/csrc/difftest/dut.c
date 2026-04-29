#include <stdio.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <limits.h> // For PATH_MAX
#include <string.h>
#include "dut.h"
#include "state.h"
#include "reg.h"

#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

// --- From C++ bridge ---
void get_dut_regstate_cpp(riscv32_CPU_state *dut);
void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n);
uint32_t get_inst_cpp();

// --- Reference Simulator API ---
void (*ref_difftest_memcpy)(uint32_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_init)(int port) = NULL;

// --- Difftest State ---
bool difftest_is_enabled = false;
static bool is_skip_ref = false;
#ifndef IMAGE_BASE_ADDR
#define IMAGE_BASE_ADDR 0xa0000000u
#endif
static const uint32_t IMAGE_BASE = IMAGE_BASE_ADDR;

typedef struct {
  uint32_t dut_base;
  uint32_t ref_base;
  uint32_t mem_size;
  uint32_t value_size;
} DifftestWindow;

static const DifftestWindow difftest_windows[] = {
  {0xa0000000u, 0x80000000u, 0x02000000u, 0x02000000u}, // ysyxSoC SDRAM, 32 MiB
  {0x0f000000u, 0x82000000u, 0x00002000u, 0x00002008u}, // ysyxSoC AXI SRAM, 8 KiB plus one-past stack values
};

static riscv32_CPU_state last_dut_state;
static bool has_last_dut_state = false;

static bool map_addr(uint32_t addr, uint32_t *mapped) {
  for (size_t i = 0; i < sizeof(difftest_windows) / sizeof(difftest_windows[0]); i++) {
    const DifftestWindow *w = &difftest_windows[i];
    if (addr >= w->dut_base && addr - w->dut_base < w->value_size) {
      *mapped = w->ref_base + (addr - w->dut_base);
      return true;
    }
  }
  *mapped = addr;
  return false;
}

static bool addr_range_in_window(uint32_t addr, uint32_t len) {
  for (size_t i = 0; i < sizeof(difftest_windows) / sizeof(difftest_windows[0]); i++) {
    const DifftestWindow *w = &difftest_windows[i];
    if (len <= w->mem_size && addr >= w->dut_base && addr - w->dut_base <= w->mem_size - len) {
      return true;
    }
  }
  return false;
}

static void map_cpu_state_to_ref(riscv32_CPU_state *s) {
  map_addr(s->pc, &s->pc);
  for (int i = 0; i < 32; i++) {
    map_addr(s->gpr[i], &s->gpr[i]);
  }
  map_addr(s->csrs.mepc, &s->csrs.mepc);
  map_addr(s->csrs.mtvec, &s->csrs.mtvec);
}

static uint32_t sign_extend(uint32_t value, int bits) {
  uint32_t mask = 1u << (bits - 1);
  return (value ^ mask) - mask;
}

static bool is_memory_instruction(uint32_t inst, uint32_t *addr, uint32_t *len) {
  if (!has_last_dut_state) return false;

  uint32_t opcode = inst & 0x7fu;
  uint32_t funct3 = (inst >> 12) & 0x7u;
  int rs1 = (inst >> 15) & 0x1f;
  uint32_t imm = 0;

  if (opcode == 0x03u) { // load
    imm = sign_extend(inst >> 20, 12);
  } else if (opcode == 0x23u) { // store
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
  *addr = last_dut_state.gpr[rs1] + imm;
  return true;
}

static bool should_skip_ref_for_inst(uint32_t inst) {
  uint32_t opcode = inst & 0x7fu;
  uint32_t funct3 = (inst >> 12) & 0x7u;

  if (opcode == 0x73u && funct3 != 0) {
    uint32_t csr = inst >> 20;
    switch (csr) {
      case 0x300u: // mstatus
      case 0x305u: // mtvec
      case 0x341u: // mepc
      case 0x342u: // mcause
        break;
      default:
        return true;
    }
  }

  uint32_t addr = 0;
  uint32_t len = 0;
  if (!is_memory_instruction(inst, &addr, &len)) return false;

  return !addr_range_in_window(addr, len);
}

static int inst_rd(uint32_t inst) {
  uint32_t opcode = inst & 0x7fu;
  switch (opcode) {
    case 0x03u: // load
    case 0x0fu: // fence
    case 0x13u: // op-imm
    case 0x17u: // auipc
    case 0x1bu: // op-imm-32
    case 0x33u: // op
    case 0x37u: // lui
    case 0x3bu: // op-32
    case 0x67u: // jalr
    case 0x6fu: // jal
    case 0x73u: // system/csr
      return (inst >> 7) & 0x1f;
    default:
      return 0;
  }
}

static void normalize_ref_written_addr(uint32_t inst, const riscv32_CPU_state *dut, riscv32_CPU_state *ref) {
  int rd = inst_rd(inst);
  uint32_t mapped = 0;
  if (rd != 0 && map_addr(dut->gpr[rd], &mapped)) {
    ref->gpr[rd] = mapped;
  }
}

static void zero_window_to_ref(uint32_t ref_base, uint32_t size) {
  const uint32_t chunk_size = 4096;
  uint8_t *buf = (uint8_t*)calloc(chunk_size, 1);
  if (!buf) {
    printf("Error: failed to allocate difftest zero sync buffer.\n");
    exit(1);
  }

  for (uint32_t off = 0; off < size; off += chunk_size) {
    uint32_t n = size - off < chunk_size ? size - off : chunk_size;
    ref_difftest_memcpy(ref_base + off, buf, n, DIFFTEST_TO_REF);
  }

  free(buf);
}


void difftest_skip_ref() {
  is_skip_ref = true;
}

void init_difftest(char *ref_so_file, long img_size) {
  if (!ref_so_file) return;

  // ============================ Robust Path Construction ============================
  char *nemu_home = getenv("NEMU_HOME");
  if (nemu_home == NULL) {
      printf("\n[NPC ERROR] NEMU_HOME environment variable is not set.\n");
      exit(1);
  }

  char so_full_path[PATH_MAX];
  // This logic now correctly handles both absolute paths (just in case)
  // and the relative paths we now pass from the Makefile and launch.json.
  if (ref_so_file[0] == '/') {
      snprintf(so_full_path, sizeof(so_full_path), "%s", ref_so_file);
  } else {
      snprintf(so_full_path, sizeof(so_full_path), "%s/%s", nemu_home, ref_so_file);
  }
  // =================================================================================

  printf("Attempting to open reference simulator: %s\n", so_full_path);
  void *handle = dlopen(so_full_path, RTLD_LAZY);
  if (!handle) {
    printf("\n[NPC ERROR] Cannot open reference simulator '%s'\n", so_full_path);
    printf("dlerror: %s\n\n", dlerror());
    exit(1);
  }

  ref_difftest_memcpy = dlsym(handle, "difftest_memcpy");
  ref_difftest_regcpy = dlsym(handle, "difftest_regcpy");
  ref_difftest_exec = dlsym(handle, "difftest_exec");
  ref_difftest_init = dlsym(handle, "difftest_init");

  if (!ref_difftest_memcpy || !ref_difftest_regcpy || !ref_difftest_exec || !ref_difftest_init) {
    printf("Error: API symbols not found in reference simulator.\n");
    exit(1);
  }

  printf("Differential testing: ON\n");
  difftest_is_enabled = true;

  ref_difftest_init(0);

  uint32_t image_ref_base = 0;
  if (!map_addr(IMAGE_BASE, &image_ref_base)) {
    printf("Error: image base 0x%08x is outside the difftest memory windows.\n", IMAGE_BASE);
    exit(1);
  }

  uint8_t *guest_mem = (uint8_t*)malloc(img_size);
  pmem_read_chunk(IMAGE_BASE, guest_mem, img_size);
  ref_difftest_memcpy(image_ref_base, guest_mem, img_size, DIFFTEST_TO_REF);
  free(guest_mem);

  // SRAM starts zeroed in the SoC model; make the reference window match.
  zero_window_to_ref(0x82000000u, 0x00002000u);

  riscv32_CPU_state dut_regs;
  get_dut_regstate_cpp(&dut_regs);
  last_dut_state = dut_regs;
  has_last_dut_state = true;
  map_cpu_state_to_ref(&dut_regs);
  ref_difftest_regcpy(&dut_regs, DIFFTEST_TO_REF);
}

static void checkregs(riscv32_CPU_state *dut, riscv32_CPU_state *ref) {
  bool mismatch = false;

  for (int i = 0; i < 32; i++) {
    if (dut->gpr[i] != ref->gpr[i]) {
      printf("Difftest mismatch at GPR[%d]: DUT=0x%08x, REF=0x%08x\n", i, dut->gpr[i], ref->gpr[i]);
      mismatch = true;
    }
  }
  if (dut->pc != ref->pc) {
    printf("Difftest mismatch at PC: DUT=0x%08x, REF=0x%08x\n", dut->pc, ref->pc);
    mismatch = true;
  }
  if (dut->csrs.mcause != ref->csrs.mcause) {
    printf("Difftest mismatch at mcause: DUT=0x%08x, REF=0x%08x\n", dut->csrs.mcause, ref->csrs.mcause);
    mismatch = true;
  }
  if (dut->csrs.mepc != ref->csrs.mepc) {
    printf("Difftest mismatch at mepc: DUT=0x%08x, REF=0x%08x\n", dut->csrs.mepc, ref->csrs.mepc);
    mismatch = true;
  }
  if (dut->csrs.mstatus != ref->csrs.mstatus) {
    printf("Difftest mismatch at mstatus: DUT=0x%08x, REF=0x%08x\n", dut->csrs.mstatus, ref->csrs.mstatus);
    mismatch = true;
  }
  if (dut->csrs.mtvec != ref->csrs.mtvec) {
    printf("Difftest mismatch at mtvec: DUT=0x%08x, REF=0x%08x\n", dut->csrs.mtvec, ref->csrs.mtvec); 
    mismatch = true;
  }


  if (mismatch) {
    npc_state.state = NPC_ABORT;
    printf("dut's regs:\n");
    isa_reg_display();
    printf("============================\n");
    printf("ref's regs:\n");
    for (int i = 0; i < 32; i ++) {
      printf("x%d:  %x\n", i, ref->gpr[i]);
    }
    printf("pc:  %x\n", ref->pc);
    printf("mstatus:  %x\n", ref->csrs.mstatus);
    printf("mtvec:  %x\n", ref->csrs.mtvec);
    printf("mepc:  %x\n", ref->csrs.mepc);
    printf("mcause:  %x\n", ref->csrs.mcause);  
}
}

void difftest_step() {
  riscv32_CPU_state dut;
  get_dut_regstate_cpp(&dut);
  uint32_t inst = get_inst_cpp();

  if (is_skip_ref) {
    riscv32_CPU_state mapped = dut;
    map_cpu_state_to_ref(&mapped);
    ref_difftest_regcpy(&mapped, DIFFTEST_TO_REF);
    last_dut_state = dut;
    has_last_dut_state = true;
    is_skip_ref = false;
    return;
  }

  if (should_skip_ref_for_inst(inst)) {
    riscv32_CPU_state mapped = dut;
    map_cpu_state_to_ref(&mapped);
    ref_difftest_regcpy(&mapped, DIFFTEST_TO_REF);
    last_dut_state = dut;
    has_last_dut_state = true;
    return;
  }

  ref_difftest_exec(1);

  riscv32_CPU_state ref_r;
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);
  normalize_ref_written_addr(inst, &dut, &ref_r);
  map_cpu_state_to_ref(&ref_r);

  riscv32_CPU_state mapped_dut = dut;
  map_cpu_state_to_ref(&mapped_dut);
  checkregs(&mapped_dut, &ref_r);
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_REF);

  last_dut_state = dut;
  has_last_dut_state = true;
}
