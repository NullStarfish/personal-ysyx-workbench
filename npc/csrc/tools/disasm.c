#include <stdio.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <capstone/capstone.h>
#include "disasm.h"

// Pointers to hold the dynamically loaded Capstone functions
static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code, size_t code_size, uint64_t address, size_t count, cs_insn **insn);
static void (*cs_free_dl)(cs_insn *insn, size_t count);
cs_err (*cs_open_dl)(cs_arch arch, cs_mode mode, csh *handle);

static csh handle; // Capstone handle

void init_disasm() {
  // BUG FIX: 只提供库名，让动态链接器通过 rpath (由 Makefile 设置) 寻找库
  void *dl_handle = dlopen("libcapstone.so.5", RTLD_LAZY);
  if (!dl_handle) {
    // 提供更详细的错误信息
    printf("Error: Cannot open libcapstone.so. \n");
    printf("1. Make sure NEMU_HOME is set correctly.\n");
    printf("2. Make sure Capstone is compiled in your NEMU project.\n");
    printf("Dynamic linker error: %s\n", dlerror());
    exit(1);
  }

  // Load the necessary functions from the library
  cs_open_dl = dlsym(dl_handle, "cs_open");
  cs_disasm_dl = dlsym(dl_handle, "cs_disasm");
  cs_free_dl = dlsym(dl_handle, "cs_free");
  if (!cs_open_dl || !cs_disasm_dl || !cs_free_dl) {
      printf("Error: Failed to load Capstone functions.\n");
      exit(1);
  }

  // Initialize Capstone for RISC-V 32-bit architecture
  cs_arch arch = CS_ARCH_RISCV;
  cs_mode mode = CS_MODE_RISCV32;
  int ret = cs_open_dl(arch, mode, &handle);
  if (ret != CS_ERR_OK) {
      printf("Error: Failed to initialize Capstone engine.\n");
      exit(1);
  }
}

void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte) {
  cs_insn *insn;
  size_t count = cs_disasm_dl(handle, code, nbyte, pc, 1, &insn); // Disassemble 1 instruction

  if (count > 0) {
    int ret = snprintf(str, size, "%s", insn->mnemonic);
    if (insn->op_str[0] != '\0') {
      snprintf(str + ret, size - ret, "\t%s", insn->op_str);
    }
    cs_free_dl(insn, count);
  } else {
    snprintf(str, size, "invalid instruction");
  }
}
