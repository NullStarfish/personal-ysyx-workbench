#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include "monitor.h"
#include "sdb/sdb.h"
#include "log/log.h"
#ifdef CONFIG_FTRACE
#include "trace/ftrace.h"
#endif

#ifdef CONFIG_ITRACE
#include "tools/disasm.h"
#endif

#ifdef CONFIG_DIFFTEST
#include "difftest/dut.h"
#endif

// --- From C++ Bridge ---
void init_verilator(int argc, char *argv[]);
void set_dpi_scope();
void reset_cpu(int n);
void load_data_to_rom(const uint8_t* data, size_t size);
// The new synchronization function
void sync_after_load();

// --- Command-line arguments ---
char *img_file = NULL;
static char *log_file = NULL;
static char *elf_file = NULL;
static char *diff_so_file = NULL;

static long load_program(const char* filename) {
    if (filename == NULL) { return 0; }
    FILE *fp = fopen(filename, "rb");
    if (fp == NULL) { printf("Can not open '%s'\n", filename); exit(1); }
    fseek(fp, 0, SEEK_END);
    long f_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    printf("The image is %s, size = %ld\n", filename, f_size);
    uint8_t *program_data = (uint8_t*)malloc(f_size);
    fread(program_data, f_size, 1, fp);
    load_data_to_rom(program_data, f_size);
    free(program_data);
    fclose(fp);
    return f_size;
}

static int parse_args(int argc, char *argv[]) {
    const struct option table[] = {
        {"batch",  no_argument, NULL, 'b'}, {"log", required_argument, NULL, 'l'},
        {"diff", required_argument, NULL, 'd'}, {"ftrace", required_argument, NULL, 'f'},
        {"help", no_argument, NULL, 'h'}, {0, 0, NULL, 0 },
    };
    int o;
    while ((o = getopt_long(argc, argv, "-bl:d:f:h", table, NULL)) != -1) {
        switch (o) {
            case 'b': sdb_set_batch_mode(); break;
            case 'l': log_file = optarg; break;
            case 'd': diff_so_file = optarg; break;
            case 'f': elf_file = optarg; break;
            case 1: if (img_file == NULL) { img_file = optarg; } break;
            default: exit(0);
        }
    }
    if (img_file == NULL && optind < argc) { img_file = argv[optind]; }
    return 0;
}

static void welcome() { printf("Welcome to the RISC-V NPC simulator!\nFor help, type \"help\"\nThe current img is %s\n", img_file); }

void init_monitor(int argc, char *argv[]) {
    parse_args(argc, argv);
    init_log(log_file);

#ifdef CONFIG_FTRACE
    printf("FTRACE is ON\n");
    init_ftrace(elf_file);
#endif

    init_verilator(argc, argv);

#ifdef CONFIG_ITRACE
    printf("Disassembler for NPC log: ON\n");
    init_disasm();
#endif

    set_dpi_scope();
    
    long img_size = load_program(img_file);
    sync_after_load();
    reset_cpu(5);
    printf("CPU reset complete.\n");

#ifdef CONFIG_DIFFTEST
    init_difftest(diff_so_file, img_size);
#endif
    
    init_sdb();
    welcome();
}