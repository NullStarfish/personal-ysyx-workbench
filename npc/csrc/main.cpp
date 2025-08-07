#include <VTop.h>
#include "VTop___024root.h" 
#include "VTop_Top.h"
#include <verilated.h>
#include "svdpi.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdint>

// Forward declaration of the DPI-C function from Verilog
extern "C" void ebreak();

// --- Simulation State ---
enum {
    RESET = 0,
    RUNNING = 1,
    HALTED = 2
};
int npc_state = RESET;
long long cycle_count = 0;

// --- Function Prototypes ---
void single_cycle(VTop* top, bool trace);
void load_program(VTop* top, const char* filename);
void print_debug_info(VTop* top);

// --- Main Simulation Logic ---
int main(int argc, char** argv) {
    if (argc < 2) {
        std::cerr << "Usage: " << argv[0] << " <path_to_binary_file>" << std::endl;
        return 1;
    }

    Verilated::commandArgs(argc, argv);
    VTop* top = new VTop;

    // Phase 1: Load the program into ROM using the loader module
    load_program(top, argv[1]);

    // Phase 2: Reset the CPU
    top->rst = 1;
    for (int i = 0; i < 5; ++i) {
        single_cycle(top, false); // Cycle clock during reset, no tracing
    }
    top->rst = 0;
    std::cout << "CPU reset complete. Starting execution." << std::endl;
    cycle_count = 0;

    // Phase 3: Main execution loop
    npc_state = RUNNING;
    while (cycle_count < 50 && npc_state == RUNNING && !Verilated::gotFinish()) {
        single_cycle(top, true); // Cycle clock with tracing enabled
    }

    delete top;
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return 0;
}

/**
 * @brief Loads a binary program into the ROM cycle by cycle.
 */
void load_program(VTop* top, const char* filename) {
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
    top->rst = 0;
    top->load_en = 1; // Enable the loader

    for (size_t i = 0; i < program_data.size(); ++i) {
        top->load_addr = 0x80000000 + (i * 4);
        top->load_data = program_data[i];
        single_cycle(top, false); // Pulse clock once to write data
    }

    top->load_en = 0; // Disable the loader
    std::cout << "Program loading complete." << std::endl;
}

/**
 * @brief Prints the current PC and instruction.
 */
void print_debug_info(VTop* top) {
    printf("[Cycle %03lld] PC=0x%08x, INST=0x%08x\n",
           cycle_count,
           top->rootp->Top->pc_out,
           top->rootp->Top->inst
    );
}

/**
 * @brief Simulates a single clock cycle (posedge and negedge).
 */
void single_cycle(VTop* top, bool trace) {
    top->clk = 0;
    top->eval();
    top->clk = 1;
    top->eval();
    
    if (trace) {
        print_debug_info(top);
        cycle_count++;
    }
}

/**
 * @brief DPI-C implementation for the 'ebreak' instruction.
 */
extern "C" void ebreak() {
    printf("\n--- EBREAK instruction executed. Halting simulation. ---\n");
    npc_state = HALTED;
}
