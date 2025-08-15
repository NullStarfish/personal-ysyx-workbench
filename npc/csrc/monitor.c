#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include "monitor.h"
#include "sdb/sdb.h"
#include "disasm.h"
#include "log.h"

// --- From C++ bridge ---
void init_verilator(int argc, char *argv[]);
void set_dpi_scope();
void reset_cpu(int n);
void load_data_to_rom(const uint8_t* data, size_t size);

// Command-line arguments
static char *img_file = NULL;
static char *log_file = NULL;

static void load_program(const char* filename) {
    if (filename == NULL) { printf("No image is given.\n"); return; }
    FILE *fp = fopen(filename, "rb");
    if (fp == NULL) { printf("Can not open '%s'\n", filename); exit(1); }
    fseek(fp, 0, SEEK_END); long f_size = ftell(fp); fseek(fp, 0, SEEK_SET);
    printf("The image is %s, size = %ld\n", filename, f_size);
    uint8_t *program_data = (uint8_t*)malloc(f_size);
    if(fread(program_data, f_size, 1, fp) != 1) { printf("Failed to read program data.\n"); free(program_data); fclose(fp); exit(1); }
    load_data_to_rom(program_data, f_size);
    free(program_data); fclose(fp);
}

static int parse_args(int argc, char *argv[]) {
    const struct option table[] = {
        {"batch", no_argument,       NULL, 'b'},
        {"log",   required_argument, NULL, 'l'},
        {"help",  no_argument,       NULL, 'h'},
        {0,       0,                 NULL,  0 },
    };
    int o;
    while ((o = getopt_long(argc, argv, "-bl:h", table, NULL)) != -1) {
        switch (o) {
            case 'b': sdb_set_batch_mode(); break;
            case 'l': log_file = optarg; break;
            case 1: if (img_file == NULL) { img_file = optarg; } break;
            case 'h': default:
                printf("Usage: %s [OPTION...] IMAGE_FILE\n\n", argv[0]);
                printf("\t-b, --batch\tRun in batch mode\n");
                printf("\t-l, --log=FILE\tOutput log to FILE\n");
                printf("\t-h, --help\tShow this help message\n\n");
                exit(0);
        }
    }
    if (img_file == NULL && optind < argc) { img_file = argv[optind]; }
    return 0;
}

static void welcome() { printf("Welcome to the RISC-V NPC simulator!\nFor help, type \"help\"\n"); }

void init_monitor(int argc, char *argv[]) {
    parse_args(argc, argv);
    init_log(log_file);
    init_verilator(argc, argv);
    init_disasm();
    set_dpi_scope();
    load_program(img_file);
    reset_cpu(5);
    printf("CPU reset complete.\n");
    init_sdb();
    welcome();
}
