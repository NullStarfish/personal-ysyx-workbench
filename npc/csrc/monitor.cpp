#include <getopt.h>
#include <VTop.h>
#include <verilated.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdint>
#include "sdb/sdb.h"
#include "monitor.h"

// Forward declarations for functions defined in main.cpp
void single_cycle(bool trace);
void set_dpi_scope();

// Global pointer to the Verilated model, defined in main.cpp
extern VTop* top_ptr;

// --- Static variables for command-line arguments ---
static char *img_file = NULL;

/**
 * @brief Loads a binary program file into the simulated ROM.
 * @param filename Path to the binary file.
 */
static void load_program(const char* filename) {
    std::ifstream file(filename, std::ios::binary);
    if (!file) {
        std::cerr << "Error: Cannot open file '" << filename << "'" << std::endl;
        exit(1);
    }
    file.seekg(0, std::ios::end);
    size_t size = file.tellg();
    file.seekg(0, std::ios::beg);
    std::vector<uint32_t> program_data(size / sizeof(uint32_t));
    file.read(reinterpret_cast<char*>(program_data.data()), size);
    file.close();

    std::cout << "Starting program loading into ROM..." << std::endl;
    top_ptr->rst = 0;
    top_ptr->load_en = 1;
    for (size_t i = 0; i < program_data.size(); ++i) {
        top_ptr->load_addr = 0x80000000 + (i * 4);
        top_ptr->load_data = program_data[i];
        single_cycle(false); // Use single_cycle to clock the loading process
    }
    top_ptr->load_en = 0;
    std::cout << "Program loading complete." << std::endl;
}

/**
 * @brief Parses command-line arguments.
 */
static int parse_args(int argc, char *argv[]) {
    const struct option table[] = {
        {"batch", no_argument, NULL, 'b'},
        {"help",  no_argument, NULL, 'h'},
        {0,       0,           NULL,  0 },
    };
    int o;
    // The leading '-' in the optstring allows handling of non-option arguments
    while ((o = getopt_long(argc, argv, "-bh", table, NULL)) != -1) {
        switch (o) {
            case 'b': sdb_set_batch_mode(); break;
            case 1: // This case handles non-option arguments (the image file)
                if (img_file == NULL) {
                    img_file = optarg;
                }
                break;
            case 'h': // Fall-through for help
            default:
                printf("Usage: %s [OPTION...] IMAGE_FILE\n\n", argv[0]);
                printf("\t-b, --batch              Run in batch mode (non-interactive)\n");
                printf("\t-h, --help               Show this help message\n");
                printf("\n");
                exit(0);
        }
    }
    // If the image file was not caught as a non-option argument, it might be the last argument.
    if (img_file == NULL && optind < argc) {
        img_file = argv[optind];
    }
    return 0;
}

/**
 * @brief Displays a welcome message to the user.
 */
static void welcome() {
  printf("Welcome to the RISC-V NPC (Nanjing University Processor Core) simulator!\n");
  printf("For help, type \"help\"\n");
}

// Public interface function
void init_monitor(int argc, char *argv[]) {
    /* Parse arguments first to handle flags like --batch */
    parse_args(argc, argv);

    /* Instantiate the Verilated model */
    Verilated::commandArgs(argc, argv);
    top_ptr = new VTop;

    /* Set up DPI-C scope */
    set_dpi_scope();

    /* Load the program image */
    if (img_file == NULL) {
        std::cerr << "Error: No image file specified." << std::endl;
        std::cerr << "Usage: " << argv[0] << " [OPTIONS] <path_to_binary_file>" << std::endl;
        exit(1);
    }
    load_program(img_file);

    /* Reset the CPU to its initial state */
    top_ptr->rst = 1;
    for (int i = 0; i < 5; ++i) {
        single_cycle(false);
    }
    top_ptr->rst = 0;
    std::cout << "CPU reset complete." << std::endl;

    /* Initialize the simple debugger and its components */
    init_sdb();

    /* Display welcome message */
    welcome();
}
