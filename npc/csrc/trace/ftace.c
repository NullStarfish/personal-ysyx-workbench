#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <elf.h>
#include "ftrace.h"
#include "../reg.h"

#define Elf_Ehdr Elf32_Ehdr
#define Elf_Shdr Elf32_Shdr
#define Elf_Sym  Elf32_Sym
#define ELF_ST_TYPE ELF32_ST_TYPE

// --- From C++ bridge ---
uint32_t isa_reg_read_cpp(int reg_num);

typedef struct {
    char name[128];
    uint32_t start;
    uint32_t end;
} FuncInfo;

static FuncInfo *func_table = NULL;
static int func_count = 0;
static bool ftrace_enabled = false;

// --- Call Stack ---
#define CALL_STACK_DEPTH 128
typedef struct {
    uint32_t pc;
    uint32_t target_func_addr;
    char target_func_name[128];
} CallStackEntry;

static CallStackEntry call_stack[CALL_STACK_DEPTH];
static int stack_top = 0;
static int indent_level = 0;

static const char* find_func_name(uint32_t addr, uint32_t* start_addr) {
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
    if (!elf_file) return;

    FILE *fp = fopen(elf_file, "rb");
    if (!fp) { printf("Error: Cannot open ELF file '%s' for ftrace.\n", elf_file); return; }
    
    ftrace_enabled = true;
    printf("Ftrace enabled. Reading symbols from '%s'.\n", elf_file);

    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    uint8_t *elf_data = (uint8_t*)malloc(file_size);
    if (fread(elf_data, 1, file_size, fp) != file_size) {
        printf("Error: Failed to read ELF file.\n");
        free(elf_data);
        fclose(fp);
        return;
    }
    fclose(fp);

    Elf_Ehdr *ehdr = (Elf_Ehdr *)elf_data;
    if (memcmp(ehdr->e_ident, ELFMAG, SELFMAG) != 0) {
        printf("Error: Invalid ELF magic number.\n");
        free(elf_data);
        return;
    }

    Elf_Shdr *shdr_table = (Elf_Shdr *)(elf_data + ehdr->e_shoff);
    Elf_Shdr *symtab_shdr = NULL, *strtab_shdr = NULL;

    for (int i = 0; i < ehdr->e_shnum; i++) {
        if (shdr_table[i].sh_type == SHT_SYMTAB) {
            symtab_shdr = &shdr_table[i];
            strtab_shdr = &shdr_table[shdr_table[i].sh_link];
            break;
        }
    }
    if (!symtab_shdr || !strtab_shdr) {
        printf("Warning: Symbol table or string table not found in ELF file. Ftrace will be disabled.\n");
        free(elf_data);
        ftrace_enabled = false;
        return;
    }

    Elf_Sym *sym_table = (Elf_Sym *)(elf_data + symtab_shdr->sh_offset);
    char *str_table = (char *)(elf_data + strtab_shdr->sh_offset);
    int sym_count_total = symtab_shdr->sh_size / symtab_shdr->sh_entsize;

    for (int i = 0; i < sym_count_total; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) func_count++;
    }

    // --- 增强的诊断信息 ---
    printf("Found %d symbols in total, %d of which are functions.\n", sym_count_total, func_count);
    if (func_count == 0) {
        printf("Warning: No function symbols found. Ftrace will be disabled.\n");
        free(elf_data);
        ftrace_enabled = false;
        return;
    }
    // --- 诊断信息结束 ---

    func_table = (FuncInfo*)malloc(sizeof(FuncInfo) * func_count);
    int f_idx = 0;
    for (int i = 0; i < sym_count_total; i++) {
        if (ELF_ST_TYPE(sym_table[i].st_info) == STT_FUNC) {
            strncpy(func_table[f_idx].name, str_table + sym_table[i].st_name, sizeof(func_table[0].name)-1);
            func_table[f_idx].name[sizeof(func_table[0].name)-1] = '\0';
            func_table[f_idx].start = sym_table[i].st_value;
            func_table[f_idx].end = sym_table[i].st_value + sym_table[i].st_size;
            f_idx++;
        }
    }
    free(elf_data);
}

static void log_func_call(uint32_t pc, uint32_t target) {
    uint32_t func_start = 0;
    const char *func_name = find_func_name(target, &func_start);
    printf("0x%08x: %*scall [%s@0x%08x]\n", pc, indent_level * 2, "", func_name, func_start);
    if (stack_top < CALL_STACK_DEPTH) {
        call_stack[stack_top].pc = pc;
        call_stack[stack_top].target_func_addr = func_start;
        strncpy(call_stack[stack_top].target_func_name, func_name, sizeof(call_stack[0].target_func_name)-1);
        call_stack[stack_top].target_func_name[sizeof(call_stack[0].target_func_name)-1] = '\0';
        stack_top++;
        indent_level++;
    }
}

static void log_func_ret(uint32_t pc) {
    if (stack_top == 0) return;
    indent_level--;
    stack_top--;
    const char *func_name = call_stack[stack_top].target_func_name;
    printf("0x%08x: %*sret [%s]\n", pc, indent_level * 2, "", func_name);
}

void trace_func_call(uint32_t pc, uint32_t inst) {
    if (!ftrace_enabled) return;
    uint32_t opcode = inst & 0x7f;
    uint32_t rd = (inst >> 7) & 0x1f;
    uint32_t rs1 = (inst >> 15) & 0x1f;

    if (opcode == 0b1101111) { // JAL
        // --- BUG FIX: Correct J-type immediate decoding ---
        uint32_t imm20   = (inst >> 31) & 0x1;
        uint32_t imm10_1 = (inst >> 21) & 0x3ff;
        uint32_t imm11   = (inst >> 20) & 0x1;
        uint32_t imm19_12= (inst >> 12) & 0xff;

        // Reconstruct the 21-bit signed immediate, left-shifted by 1
        int32_t imm = (imm20 << 20) | (imm19_12 << 12) | (imm11 << 11) | (imm10_1 << 1);
        // Sign extend from 21 bits
        imm = (imm << 11) >> 11;

        uint32_t target = pc + imm;
        log_func_call(pc, target);
    }
    else if (opcode == 0b1100111) { // JALR
        int32_t imm_i = (int32_t)inst >> 20;
        uint32_t target = (isa_reg_read_cpp(rs1) + imm_i) & ~1;
        if (rd == 0 && rs1 == 1 && imm_i == 0) {
            log_func_ret(pc);
        } else {
            log_func_call(pc, target);
        }
    }
}

void print_ftrace_stack() {
    if (!ftrace_enabled) return;
    printf("\nFunction Call Stack Trace:\n");
    for (int i = 0; i < stack_top; i++) {
        printf("  at 0x%08x: called %s@0x%08x\n",
            call_stack[i].pc, call_stack[i].target_func_name, call_stack[i].target_func_addr);
    }
}
