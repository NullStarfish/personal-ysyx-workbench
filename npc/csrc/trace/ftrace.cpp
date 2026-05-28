#include "trace/ftrace.h"

#include <elf.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "sim.h"

namespace {
using ElfEhdr = Elf32_Ehdr;
using ElfShdr = Elf32_Shdr;
using ElfSym = Elf32_Sym;

struct FuncInfo {
  char name[128];
  uint32_t start;
  uint32_t end;
};

struct CallStackEntry {
  uint32_t pc;
  uint32_t targetFuncAddr;
  char targetFuncName[128];
};

constexpr int kCallStackDepth = 128;

FuncInfo *funcTable = nullptr;
int funcCount = 0;
bool ftraceEnabled = false;
CallStackEntry callStack[kCallStackDepth];
int stackTop = 0;
int indentLevel = 0;

const char *find_func_name(uint32_t addr, uint32_t *startAddr) {
  if (funcTable == nullptr) {
    return "???";
  }
  for (int i = 0; i < funcCount; i++) {
    if (addr >= funcTable[i].start && addr < funcTable[i].end) {
      if (startAddr != nullptr) {
        *startAddr = funcTable[i].start;
      }
      return funcTable[i].name;
    }
  }
  return "???";
}

void log_func_call(uint32_t pc, uint32_t target) {
  uint32_t funcStart = 0;
  const char *funcName = find_func_name(target, &funcStart);
  printf("0x%08x: %*scall [%s@0x%08x]\n", pc, indentLevel * 2, "", funcName, funcStart);
  if (stackTop < kCallStackDepth) {
    callStack[stackTop].pc = pc;
    callStack[stackTop].targetFuncAddr = funcStart;
    strncpy(callStack[stackTop].targetFuncName, funcName, sizeof(callStack[stackTop].targetFuncName) - 1);
    callStack[stackTop].targetFuncName[sizeof(callStack[stackTop].targetFuncName) - 1] = '\0';
    stackTop++;
    indentLevel++;
  }
}

void log_func_ret(uint32_t pc) {
  if (stackTop == 0) {
    return;
  }
  indentLevel--;
  stackTop--;
  printf("0x%08x: %*sret [%s]\n", pc, indentLevel * 2, "", callStack[stackTop].targetFuncName);
}
}

void init_ftrace(const char *elfFile) {
  ftraceEnabled = false;
  stackTop = 0;
  indentLevel = 0;

  if (elfFile == nullptr) {
    return;
  }

  FILE *fp = fopen(elfFile, "rb");
  if (fp == nullptr) {
    printf("Error: Cannot open ELF file '%s' for ftrace.\n", elfFile);
    return;
  }

  printf("Loading ftrace symbols from '%s'.\n", elfFile);

  fseek(fp, 0, SEEK_END);
  long fileSize = ftell(fp);
  fseek(fp, 0, SEEK_SET);

  auto *elfData = static_cast<uint8_t *>(malloc(fileSize));
  if (fread(elfData, 1, fileSize, fp) != (size_t)fileSize) {
    printf("Error: Failed to read ELF file.\n");
    free(elfData);
    fclose(fp);
    return;
  }
  fclose(fp);

  auto *ehdr = reinterpret_cast<ElfEhdr *>(elfData);
  if (memcmp(ehdr->e_ident, ELFMAG, SELFMAG) != 0) {
    printf("Error: Invalid ELF magic number.\n");
    free(elfData);
    return;
  }

  auto *shdrTable = reinterpret_cast<ElfShdr *>(elfData + ehdr->e_shoff);
  ElfShdr *symtabShdr = nullptr;
  ElfShdr *strtabShdr = nullptr;
  for (int i = 0; i < ehdr->e_shnum; i++) {
    if (shdrTable[i].sh_type == SHT_SYMTAB) {
      symtabShdr = &shdrTable[i];
      strtabShdr = &shdrTable[shdrTable[i].sh_link];
      break;
    }
  }
  if (symtabShdr == nullptr || strtabShdr == nullptr) {
    printf("Warning: Symbol table or string table not found in ELF file. Ftrace will be disabled.\n");
    free(elfData);
    ftraceEnabled = false;
    return;
  }

  auto *symTable = reinterpret_cast<ElfSym *>(elfData + symtabShdr->sh_offset);
  auto *strTable = reinterpret_cast<char *>(elfData + strtabShdr->sh_offset);
  int symbolCount = symtabShdr->sh_size / symtabShdr->sh_entsize;

  funcCount = 0;
  for (int i = 0; i < symbolCount; i++) {
    if (ELF32_ST_TYPE(symTable[i].st_info) == STT_FUNC) {
      funcCount++;
    }
  }

  printf("Found %d symbols in total, %d of which are functions.\n", symbolCount, funcCount);
  if (funcCount == 0) {
    printf("Warning: No function symbols found. Ftrace will be disabled.\n");
    free(elfData);
    ftraceEnabled = false;
    return;
  }

  funcTable = static_cast<FuncInfo *>(malloc(sizeof(FuncInfo) * funcCount));
  int funcIndex = 0;
  for (int i = 0; i < symbolCount; i++) {
    if (ELF32_ST_TYPE(symTable[i].st_info) == STT_FUNC) {
      strncpy(funcTable[funcIndex].name, strTable + symTable[i].st_name, sizeof(funcTable[funcIndex].name) - 1);
      funcTable[funcIndex].name[sizeof(funcTable[funcIndex].name) - 1] = '\0';
      funcTable[funcIndex].start = symTable[i].st_value;
      funcTable[funcIndex].end = symTable[i].st_value + symTable[i].st_size;
      funcIndex++;
    }
  }
  ftraceEnabled = true;
  printf("Ftrace enabled.\n");
  free(elfData);
}

void trace_func_call(uint32_t pc, uint32_t inst) {
  if (!ftraceEnabled) {
    return;
  }

  uint32_t opcode = inst & 0x7f;
  uint32_t rd = (inst >> 7) & 0x1f;
  uint32_t rs1 = (inst >> 15) & 0x1f;
  bool rdIsLink = rd == 1 || rd == 5;
  bool rs1IsLink = rs1 == 1 || rs1 == 5;

  if (opcode == 0b1101111 && rdIsLink) {
    uint32_t imm20 = (inst >> 31) & 0x1;
    uint32_t imm10_1 = (inst >> 21) & 0x3ff;
    uint32_t imm11 = (inst >> 20) & 0x1;
    uint32_t imm19_12 = (inst >> 12) & 0xff;
    int32_t imm = (imm20 << 20) | (imm19_12 << 12) | (imm11 << 11) | (imm10_1 << 1);
    imm = (imm << 11) >> 11;
    log_func_call(pc, pc + imm);
  } else if (opcode == 0b1100111) {
    int32_t immI = (int32_t)inst >> 20;
    uint32_t target = (cpu.regRead(rs1) + immI) & ~1u;
    if (!rdIsLink && rs1IsLink) {
      log_func_ret(pc);
    } else if (rdIsLink) {
      if (rs1IsLink && rd != rs1) {
        log_func_ret(pc);
      }
      log_func_call(pc, target);
    }
  }
}

bool ftrace_ready() {
  return ftraceEnabled;
}

void print_ftrace_stack() {
  if (!ftraceEnabled) {
    return;
  }
  printf("\nFunction Call Stack Trace:\n");
  if (stackTop == 0) {
    printf("  <empty>\n");
    return;
  }
  for (int i = 0; i < stackTop; i++) {
    printf("  at 0x%08x: called %s@0x%08x\n", callStack[i].pc, callStack[i].targetFuncName,
           callStack[i].targetFuncAddr);
  }
}
