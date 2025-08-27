#include <stdio.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <limits.h> // For PATH_MAX
#include "dut.h"
#include "state.h"
#include "reg.h"

// --- From C++ bridge ---
void get_dut_regstate_cpp(riscv32_CPU_state *dut);
void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n);

// --- Reference Simulator API ---
void (*ref_difftest_memcpy)(uint32_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_init)(int port) = NULL;

// --- Difftest State ---
bool difftest_is_enabled = false;
static bool is_skip_ref = false;

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

  uint8_t *guest_mem = (uint8_t*)malloc(img_size);
  pmem_read_chunk(0x80000000, guest_mem, img_size);
  ref_difftest_memcpy(0x80000000, guest_mem, img_size, DIFFTEST_TO_REF);
  free(guest_mem);

  riscv32_CPU_state dut_regs;
  get_dut_regstate_cpp(&dut_regs);
  ref_difftest_regcpy(&dut_regs, DIFFTEST_TO_REF);
}

static void checkregs(riscv32_CPU_state *ref) {
  riscv32_CPU_state dut;
  get_dut_regstate_cpp(&dut);
  bool mismatch = false;

  for (int i = 0; i < 32; i++) {
    if (dut.gpr[i] != ref->gpr[i]) {
      printf("Difftest mismatch at GPR[%d]: DUT=0x%08x, REF=0x%08x\n", i, dut.gpr[i], ref->gpr[i]);
      mismatch = true;
    }
  }
  if (dut.pc != ref->pc) {
    printf("Difftest mismatch at PC: DUT=0x%08x, REF=0x%08x\n", dut.pc, ref->pc);
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
  }
}

void difftest_step() {
  if (is_skip_ref) {
    riscv32_CPU_state dut;
    get_dut_regstate_cpp(&dut);
    ref_difftest_regcpy(&dut, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }

  ref_difftest_exec(1);

  riscv32_CPU_state ref_r;
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

  checkregs(&ref_r);
}
