#include <monitor/ftrace.h>
#include <elf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// 根据NEMU编译的目标ISA选择32位或64位ELF结构
// 这使得代码更具通用性
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

// --- 调用栈 ---
#define CALL_STACK_DEPTH 64
// 调用栈条目，记录了被调用函数的信息
typedef struct {
    paddr_t pc;                 // 调用指令的地址
    paddr_t target_func_addr;   // 被调用函数的起始地址
    char target_func_name[128]; // 被调用函数的名字
} CallStackEntry;

static CallStackEntry call_stack[CALL_STACK_DEPTH];
static int stack_top = 0;
static int indent_level = 0; // 用于控制打印输出的缩进

// 辅助函数：根据地址查找函数名
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

// --- ELF 解析 ---
void init_ftrace(const char *elf_file) {
    if (!elf_file) {
        Log("Ftrace is not enabled (no ELF file provided).");
        return;
    }

    // 1. 打开并读取整个ELF文件到内存
    FILE *fp = fopen(elf_file, "rb");
    Assert(fp, "Failed to open ELF file: %s", elf_file);

    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    uint8_t *elf_data = malloc(file_size);
    Assert(elf_data, "Failed to allocate memory for ELF file");
    int ret = fread(elf_data, 1, file_size, fp);
    Assert(ret == file_size, "Failed to read ELF file");
    fclose(fp);

    // 2. 解析ELF头
    Elf_Ehdr *ehdr = (Elf_Ehdr *)elf_data;
    Assert(memcmp(ehdr->e_ident, ELFMAG, SELFMAG) == 0, "Invalid ELF magic number");

    // 3. 找到节头表 (Section Header Table)
    Elf_Shdr *shdr_table = (Elf_Shdr *)(elf_data + ehdr->e_shoff);
    int shnum = ehdr->e_shnum;

    // 4. 找到符号表 (.symtab) 和 字符串表 (.strtab)
    Elf_Shdr *symtab_shdr = NULL;
    Elf_Shdr *strtab_shdr = NULL;
    // 首先找到节头字符串表，用于获取节的名字
    Elf_Shdr *shstrtab_shdr = &shdr_table[ehdr->e_shstrndx];
    char *shstrtab = (char *)(elf_data + shstrtab_shdr->sh_offset);

    for (int i = 0; i < shnum; i++) {
        char *sh_name = shstrtab + shdr_table[i].sh_name;
        if (shdr_table[i].sh_type == SHT_SYMTAB) {
            symtab_shdr = &shdr_table[i];
        }
        // 符号表对应的字符串表通常紧随其后，或者通过sh_link字段指定
        if (symtab_shdr && symtab_shdr->sh_link == i) {
             strtab_shdr = &shdr_table[i];
        }
    }
    Assert(symtab_shdr && strtab_shdr, "Symbol table or string table not found in ELF file");

    // 5. 读取符号表，并筛选出函数类型的符号
    Elf_Sym *sym_table = (Elf_Sym *)(elf_data + symtab_shdr->sh_offset);
    char *str_table = (char *)(elf_data + strtab_shdr->sh_offset);
    int sym_count = symtab_shdr->sh_size / symtab_shdr->sh_entsize;

    // 第一次遍历：统计函数数量，用于分配内存
    for (int i = 0; i < sym_count; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) {
            func_count++;
        }
    }

    // 分配函数信息表内存
    func_table = malloc(sizeof(FuncInfo) * func_count);
    Assert(func_table, "Failed to allocate memory for function table");

    // 第二次遍历：填充函数信息表
    int f_idx = 0;
    for (int i = 0; i < sym_count; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) {
            strncpy(func_table[f_idx].name, str_table + sym_table[i].st_name, sizeof(func_table[f_idx].name) - 1);
            func_table[f_idx].name[sizeof(func_table[f_idx].name) - 1] = '\0'; // 保证字符串结束
            func_table[f_idx].start = sym_table[i].st_value;
            func_table[f_idx].end = sym_table[i].st_value + sym_table[i].st_size;
            f_idx++;
        }
    }

    Log("Ftrace enabled. Found %d functions in '%s'.", func_count, elf_file);
    free(elf_data); // 释放ELF文件数据所占内存
}

// --- 日志打印 ---
void log_func_call(vaddr_t pc, vaddr_t target) {
    if (!func_table) return;

    paddr_t func_start;
    const char *func_name = find_func_name(target, &func_start);

    // 打印调用日志，使用indent_level控制缩进
    printf("0x%08x: %*scall [%s@0x%08x]\n", (uint32_t)pc, indent_level * 2, "", func_name, (uint32_t)func_start);

    // 将函数信息压入调用栈
    if (stack_top < CALL_STACK_DEPTH) {
        call_stack[stack_top].pc = pc;
        call_stack[stack_top].target_func_addr = func_start;
        strncpy(call_stack[stack_top].target_func_name, func_name, sizeof(call_stack[0].target_func_name)-1);
        stack_top++;
        indent_level++;
    } else {
        printf("Ftrace: Call stack overflow!\n");
    }
}

void log_func_ret(vaddr_t pc) {
    if (!func_table || stack_top == 0) return;

    // 从调用栈弹出
    stack_top--;
    indent_level--;
    const char *func_name = call_stack[stack_top].target_func_name;

    // 打印返回日志
    printf("0x%08x: %*sret  [%s]\n", (uint32_t)pc, indent_level * 2, "", func_name);
}
