#include <VTop.h>
#include "VTop___024root.h" 
#include "VTop_Top.h"
#include "VTop_RegFile.h"
#include <verilated.h>
#include "svdpi.h"
#include <cassert>
#include <map>
#include <string>
#include "monitor.h"
#include "sdb/sdb.h"

// --- Global Pointers & State ---
VTop* top_ptr = NULL;
enum { SIM_RESET, SIM_RUNNING, SIM_HALTED, SIM_ABORTED, SIM_STOPPED };
int npc_state = SIM_RESET;
long long cycle_count = 0;

// --- DPI-C Interface ---
extern "C" int pmem_read(int addr);
extern "C" void ebreak();

// --- Function Prototypes ---
void cpu_exec(uint64_t n);

// --- Main Simulation Logic ---
int main(int argc, char** argv) {
    // Initialize the entire monitor and simulation environment
    init_monitor(argc, argv);

    // ================== BUG FIX ==================
    // After initialization, the simulator should be in the STOPPED state,
    // ready to receive commands from the SDB.
    npc_state = SIM_STOPPED;
    // =============================================

    // Enter the Simple Debugger main loop
    sdb_mainloop();

    // Cleanup
    delete top_ptr;
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    int exit_code = (npc_state == SIM_HALTED) ? 0 : 1;
    return exit_code;
}


// =================================================================
//          CORE SIMULATION & ISA ACCESS FUNCTIONS
// =================================================================
// These functions are fundamental utilities for the simulation
// and are kept in main.cpp for clarity.

void print_trace_info() {
    printf("[Cycle %03lld] PC=0x%08x, INST=0x%08x\n",
           cycle_count, top_ptr->rootp->Top->pc_out, top_ptr->rootp->Top->inst);
}

void single_cycle(bool trace) {
    top_ptr->clk = 0; top_ptr->eval();
    if (trace && npc_state == SIM_RUNNING) print_trace_info();
    top_ptr->clk = 1; top_ptr->eval();
    if (trace && npc_state == SIM_RUNNING) cycle_count++;
}

void cpu_exec(uint64_t n) {
    if (npc_state != SIM_STOPPED && npc_state != SIM_RUNNING) {
        if (npc_state == SIM_HALTED || npc_state == SIM_ABORTED) {
             printf("Program has finished. To restart, exit and run again.\n");
        } else {
            printf("Cannot execute. CPU is in state %d\n", npc_state);
        }
        return;
    }
    npc_state = SIM_RUNNING;

    uint64_t i = 0;
    while ((n == (uint64_t)-1 || i < n) && npc_state == SIM_RUNNING) {
        single_cycle(true);
        if (check_watchpoints()) {
            npc_state = SIM_STOPPED;
        }
        i++;
    }

    if (npc_state == SIM_RUNNING) {
        npc_state = SIM_STOPPED;
    }
}

void set_dpi_scope() {
    const svScope scope = svGetScopeFromName("TOP.Top.imem_unit.rom0");
    assert(scope);
    svSetScope(scope);
}

void ebreak() {
    uint32_t a0_val = isa_reg_read(10);
    if (a0_val == 0) {
        printf("\n--- HIT GOOD TRAP ---\n");
        npc_state = SIM_HALTED;
    } else {
        printf("\n--- HIT BAD TRAP ---\n");
        printf("--- Return Code: %d ---\n", a0_val);
        npc_state = SIM_ABORTED;
    }
}

uint32_t isa_reg_read(int reg_num) {
    if (reg_num >= 0 && reg_num < 32) {
        return top_ptr->rootp->Top->reg_file_unit->reg_file[reg_num];
    }
    return 0;
}

void isa_reg_display() {
    const char* abi_names[32] = {
      "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
      "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
      "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
      "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
    };
    for (int i = 0; i < 32; i++) {
        printf("  $%-4s (x%-2d) = 0x%08x\n", abi_names[i], i, isa_reg_read(i));
    }
    printf("  $pc       = 0x%08x\n", top_ptr->rootp->Top->pc_out);
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
    static const std::map<std::string, int> reg_map = {
        {"pc", -1}, {"zero", 0}, {"ra", 1}, {"sp", 2}, {"gp", 3}, {"tp", 4},
        {"t0", 5}, {"t1", 6}, {"t2", 7}, {"s0", 8}, {"s1", 9}, {"a0", 10},
        {"a1", 11}, {"a2", 12}, {"a3", 13}, {"a4", 14}, {"a5", 15}, {"a6", 16},
        {"a7", 17}, {"s2", 18}, {"s3", 19}, {"s4", 20}, {"s5", 21}, {"s6", 22},
        {"s7", 23}, {"s8", 24}, {"s9", 25}, {"s10", 26}, {"s11", 27},
        {"t3", 28}, {"t4", 29}, {"t5", 30}, {"t6", 31}
    };
    *success = true;
    std::string str(s);
    if (reg_map.count(str)) {
        int reg_num = reg_map.at(str);
        return (reg_num == -1) ? top_ptr->rootp->Top->pc_out : isa_reg_read(reg_num);
    }
    if (str[0] == 'x' && str.length() > 1) {
        try {
            int reg_num = std::stoi(str.substr(1));
            if (reg_num >= 0 && reg_num < 32) return isa_reg_read(reg_num);
        } catch (...) { }
    }
    *success = false;
    return 0;
}

uint32_t paddr_read(uint32_t addr) {
    if (addr >= 0x80000000) {
        return pmem_read(addr);
    }
    return 0;
}
