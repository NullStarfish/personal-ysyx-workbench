#include <ftrace.h>
#include <elf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

// 根据NEMU编译的目标ISA选择32位或64位ELF结构
#ifdef CONFIG_ISA64
#define Elf_Ehdr Elf64_Ehdr
#define Elf_Shdr Elf64_Shdr
#define Elf_Sym  Elf64_Sym
#define ELF_ST_TYPE ELF64_ST_TYPE
#else
#define Elf_Ehdr Elf32_Ehdr
#define Elf_Shdr Elf32_Shdr
#define Elf_Sym  Elf32_Sym
#define ELF_ST_TYPE ELF32_ST_TYPE
#endif

// 用于存储从ELF文件中解析出的函数信息
typedef struct {
    char name[128]; // 函数名
    paddr_t start;  // 函数起始地址
    paddr_t end;    // 函数结束地址
} FuncInfo;

static FuncInfo *func_table = NULL; // 函数信息表
static int func_count = 0;          // 函数数量
static bool ftrace_is_enabled = false; // 运行时标志

// --- 调用栈 ---
#define CALL_STACK_DEPTH 4096
typedef struct {
    paddr_t pc;
    paddr_t target_func_addr;
    char target_func_name[128];
} CallStackEntry;

static CallStackEntry call_stack[CALL_STACK_DEPTH];
static int stack_top = 0;
static int indent_level = 0;

bool is_ftrace_enabled() {
    return ftrace_is_enabled;
}

static const char* find_func_name(paddr_t addr, paddr_t* start_addr) {
    if (!func_table) return "???";
    for (int i = 0; i < func_count; i++) {
        if (addr >= func_table[i].start && addr < func_table[i].end) {
            if(start_addr) *start_addr = func_table[i].start;
            return func_table[i].name;
        }
    }
    return "???";
}

void init_ftrace(const char *elf_file) {
    if (!elf_file) {
        return;
    }

    FILE *fp = fopen(elf_file, "rb");
    Assert(fp, "Failed to open ELF file for ftrace: %s", elf_file);
    
    ftrace_is_enabled = true;
    Log("Ftrace enabled. Reading symbols from '%s'.", elf_file);

    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    uint8_t *elf_data = malloc(file_size);
    Assert(elf_data, "Failed to allocate memory for ELF file");
    int ret = fread(elf_data, 1, file_size, fp);
    Assert(ret == file_size, "Failed to read ELF file");
    fclose(fp);

    Elf_Ehdr *ehdr = (Elf_Ehdr *)elf_data;
    Assert(memcmp(ehdr->e_ident, ELFMAG, SELFMAG) == 0, "Invalid ELF magic number");

    Elf_Shdr *shdr_table = (Elf_Shdr *)(elf_data + ehdr->e_shoff);
    int shnum = ehdr->e_shnum;

    Elf_Shdr *symtab_shdr = NULL;
    Elf_Shdr *strtab_shdr = NULL;

    for (int i = 0; i < shnum; i++) {
        if (shdr_table[i].sh_type == SHT_SYMTAB) {
            symtab_shdr = &shdr_table[i];
            strtab_shdr = &shdr_table[shdr_table[i].sh_link];
        }
    }
    Assert(symtab_shdr && strtab_shdr, "Symbol table or string table not found in ELF file");

    Elf_Sym *sym_table = (Elf_Sym *)(elf_data + symtab_shdr->sh_offset);
    char *str_table = (char *)(elf_data + strtab_shdr->sh_offset);
    int sym_count = symtab_shdr->sh_size / symtab_shdr->sh_entsize;

    for (int i = 0; i < sym_count; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) {
            func_count++;
        }
    }

    func_table = malloc(sizeof(FuncInfo) * func_count);
    Assert(func_table, "Failed to allocate memory for function table");

    int f_idx = 0;
    for (int i = 0; i < sym_count; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) {
            snprintf(func_table[f_idx].name, sizeof(func_table[f_idx].name), "%s", str_table + sym_table[i].st_name);
            func_table[f_idx].start = sym_table[i].st_value;
            func_table[f_idx].end = sym_table[i].st_value + sym_table[i].st_size;
            f_idx++;
        }
    }
    
    free(elf_data);
}

void log_func_call(vaddr_t pc, vaddr_t target) {
    if (!ftrace_is_enabled) return;
    paddr_t func_start = 0;
    const char *func_name = find_func_name(target, &func_start);

    // 打印调用日志，使用当前的缩进等级
    printf("0x%08x: %*scall [%s@0x%08x]\n", (uint32_t)pc, indent_level * 2, "", func_name, (uint32_t)func_start);

    // 将函数信息压入调用栈，然后增加缩进等级
    if (stack_top < CALL_STACK_DEPTH) {
        call_stack[stack_top].pc = pc;
        call_stack[stack_top].target_func_addr = func_start;
        snprintf(call_stack[stack_top].target_func_name, sizeof(call_stack[0].target_func_name), "%s", func_name);
        stack_top++;
        indent_level++;
    } else {
        printf("Ftrace: Call stack overflow!\n");
    }
}

void log_func_ret(vaddr_t pc) {
    if (!ftrace_is_enabled || stack_top == 0) return;

    // --- 修改开始 ---
    // 返回时，我们先将缩进等级减1，回到调用者的层级
    indent_level--;
    // 然后从栈顶弹出函数信息
    stack_top--;
    const char *func_name = call_stack[stack_top].target_func_name;

    // 使用新的、正确的缩进等级来打印返回日志
    printf("0x%08x: %*sret [%s]\n", (uint32_t)pc, indent_level * 2, "", func_name);
    // --- 修改结束 ---
}
