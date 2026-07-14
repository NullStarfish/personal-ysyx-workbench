#include "runtime/traces/ftrace.h"

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <fcntl.h>
#include <gelf.h>
#include <libelf.h>
#include <string>
#include <unistd.h>
#include <vector>

#include "sim.h"

class FTrace::Impl {
public:
  struct FuncInfo {
    std::string name;
    uint32_t start;
    uint64_t end;
  };

  struct CallStackEntry {
    uint32_t pc;
    uint32_t targetFuncAddr;
    std::string targetFuncName;
  };

  static constexpr size_t kCallStackDepth = 128;

  bool collectSymbols(Elf *elf, uint32_t sectionType, size_t &symbolCount) {
    bool foundTable = false;
    Elf_Scn *section = nullptr;

    while ((section = elf_nextscn(elf, section)) != nullptr) {
      GElf_Shdr sectionHeader;
      if (gelf_getshdr(section, &sectionHeader) == nullptr || sectionHeader.sh_type != sectionType) continue;

      foundTable = true;
      Elf_Data *data = elf_getdata(section, nullptr);
      if (data == nullptr || sectionHeader.sh_entsize == 0) continue;

      const size_t count = sectionHeader.sh_size / sectionHeader.sh_entsize;
      symbolCount += count;
      for (size_t i = 0; i < count; ++i) {
        GElf_Sym symbol;
        if (gelf_getsym(data, static_cast<int>(i), &symbol) == nullptr) continue;
        if (GELF_ST_TYPE(symbol.st_info) != STT_FUNC || symbol.st_shndx == SHN_UNDEF) continue;
        if (symbol.st_value > UINT32_MAX) continue;

        const char *name = elf_strptr(elf, sectionHeader.sh_link, symbol.st_name);
        if (name == nullptr || name[0] == '\0') continue;

        functions.push_back({name, static_cast<uint32_t>(symbol.st_value), symbol.st_value + symbol.st_size});
      }
    }
    return foundTable;
  }

  void normalizeFunctions() {
    std::sort(functions.begin(), functions.end(), [](const FuncInfo &lhs, const FuncInfo &rhs) {
      if (lhs.start != rhs.start) return lhs.start < rhs.start;
      return lhs.end > rhs.end;
    });

    functions.erase(std::unique(functions.begin(), functions.end(), [](const FuncInfo &lhs, const FuncInfo &rhs) {
                      return lhs.start == rhs.start;
                    }),
                    functions.end());

    for (size_t i = 0; i < functions.size(); ++i) {
      if (functions[i].end > functions[i].start) continue;
      functions[i].end = i + 1 < functions.size() ? functions[i + 1].start
                                                   : static_cast<uint64_t>(functions[i].start) + 1;
    }
  }

  const FuncInfo *findFunction(uint32_t addr) const {
    auto it = std::upper_bound(functions.begin(), functions.end(), addr,
                               [](uint32_t value, const FuncInfo &func) { return value < func.start; });
    if (it == functions.begin()) return nullptr;
    --it;
    return addr < it->end ? &*it : nullptr;
  }

  void logCall(uint32_t pc, uint32_t target) {
    const FuncInfo *func = findFunction(target);
    const char *name = func == nullptr ? "???" : func->name.c_str();
    const uint32_t start = func == nullptr ? target : func->start;

    printf("0x%08x: %*scall [%s@0x%08x]\n", pc, static_cast<int>(callStack.size() * 2), "", name, start);
    if (callStack.size() < kCallStackDepth) callStack.push_back({pc, start, name});
  }

  void logReturn(uint32_t pc) {
    if (callStack.empty()) return;
    const std::string name = callStack.back().targetFuncName;
    callStack.pop_back();
    printf("0x%08x: %*sret [%s]\n", pc, static_cast<int>(callStack.size() * 2), "", name.c_str());
  }

  std::vector<FuncInfo> functions;
  std::vector<CallStackEntry> callStack;
};

FTrace::FTrace() : impl(std::make_unique<Impl>()) {}
FTrace::~FTrace() = default;

void FTrace::setElfFile(const char *path) { elfFile = path == nullptr ? "" : path; }

void FTrace::init() {
  reset();
#ifdef CONFIG_FTRACE
  printf("FTRACE is ON\n");
  loadSymbols();
#endif
}

void FTrace::loadSymbols() {
  if (elfFile.empty()) return;

  if (elf_version(EV_CURRENT) == EV_NONE) {
    printf("Error: libelf initialization failed: %s\n", elf_errmsg(-1));
    return;
  }

  const int fd = open(elfFile.c_str(), O_RDONLY);
  if (fd < 0) {
    printf("Error: Cannot open ELF file '%s' for ftrace.\n", elfFile.c_str());
    return;
  }

  Elf *elf = elf_begin(fd, ELF_C_READ, nullptr);
  if (elf == nullptr || elf_kind(elf) != ELF_K_ELF) {
    printf("Error: '%s' is not a valid ELF file: %s\n", elfFile.c_str(), elf_errmsg(-1));
    if (elf != nullptr) elf_end(elf);
    close(fd);
    return;
  }

  printf("Loading ftrace symbols from '%s'.\n", elfFile.c_str());
  size_t symbolCount = 0;
  const bool hasSymtab = impl->collectSymbols(elf, SHT_SYMTAB, symbolCount);
  if (!hasSymtab) impl->collectSymbols(elf, SHT_DYNSYM, symbolCount);
  elf_end(elf);
  close(fd);

  impl->normalizeFunctions();
  printf("Found %zu symbols in total, %zu of which are functions.\n", symbolCount, impl->functions.size());
  if (impl->functions.empty()) {
    printf("Warning: No function symbols found. Ftrace will be disabled.\n");
    return;
  }

  setEnabled(true);
  printf("Ftrace enabled.\n");
}

void FTrace::reset() {
  setEnabled(false);
  impl->functions.clear();
  impl->callStack.clear();
}

void FTrace::record(uint32_t pc, uint32_t inst) {
  if (!enabled()) return;

  const uint32_t opcode = inst & 0x7f;
  const uint32_t rd = (inst >> 7) & 0x1f;
  const uint32_t rs1 = (inst >> 15) & 0x1f;
  const bool rdIsLink = rd == 1 || rd == 5;
  const bool rs1IsLink = rs1 == 1 || rs1 == 5;

  if (opcode == 0b1101111 && rdIsLink) {
    const uint32_t encoded = ((inst >> 31) << 20) | (((inst >> 12) & 0xff) << 12) |
                             (((inst >> 20) & 0x1) << 11) | (((inst >> 21) & 0x3ff) << 1);
    const int32_t imm = static_cast<int32_t>(encoded << 11) >> 11;
    impl->logCall(pc, pc + imm);
  } else if (opcode == 0b1100111) {
    const int32_t imm = static_cast<int32_t>(inst) >> 20;
    const uint32_t target = (cpu.regRead(rs1) + imm) & ~1u;
    if (!rdIsLink && rs1IsLink) {
      impl->logReturn(pc);
    } else if (rdIsLink) {
      if (rs1IsLink && rd != rs1) impl->logReturn(pc);
      impl->logCall(pc, target);
    }
  }
}

void FTrace::printStack() const {
  if (!enabled()) return;

  printf("\nFunction Call Stack Trace:\n");
  if (impl->callStack.empty()) {
    printf("  <empty>\n");
    return;
  }
  for (const Impl::CallStackEntry &entry : impl->callStack) {
    printf("  at 0x%08x: called %s@0x%08x\n", entry.pc, entry.targetFuncName.c_str(), entry.targetFuncAddr);
  }
}
