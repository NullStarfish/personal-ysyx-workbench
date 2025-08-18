#include <verilated.h>
#include "VTop.h"
#include "VTop___024root.h"
#include "VTop_Top.h"
#include "VTop_RegFile.h"
#include "svdpi.h"
#include <cassert>
#include <cstdio>
#include <map>
#include <string>
#include "difftest/dut.h"

extern "C" {
    #include "monitor.h"
    #include "state.h"
    #include "sdb/sdb.h"
    #include "trace/itrace.h"
    #include "reg.h"
    #include "ftrace.h"
}

VTop* top_ptr = NULL;
long long cycle_count = 0;

extern "C" int pmem_read(int addr);
extern "C" void ebreak() {
    uint32_t a0_val = top_ptr->rootp->Top->reg_file_unit->reg_file[10];
    npc_state.state = (a0_val == 0) ? NPC_END : NPC_ABORT;
    npc_state.halt_ret = a0_val;
}

extern "C" {

void assert_fail_msg() {
    isa_reg_display();
    print_iring_buffer();
    print_ftrace_stack();
}

void exec_one_cycle_cpp() {
    top_ptr->clk = 0; top_ptr->eval();
    top_ptr->clk = 1; top_ptr->eval();
    cycle_count++;
}

// Other C-exported functions remain the same...
long long get_cycle_count() { return cycle_count; }
void init_verilator(int argc, char *argv[]) { Verilated::commandArgs(argc, argv); top_ptr = new VTop; }
void set_dpi_scope() { const svScope scope = svGetScopeFromName("TOP.Top.imem_unit.rom0"); assert(scope); svSetScope(scope); }
void reset_cpu(int n) { top_ptr->rst = 1; for (int i = 0; i < n; ++i) { top_ptr->clk=0; top_ptr->eval(); top_ptr->clk=1; top_ptr->eval(); } top_ptr->rst = 0; }
void load_data_to_rom(const uint8_t* data, size_t size) {
    top_ptr->load_en = 1;
    for (size_t i = 0; i < size / 4; ++i) {
        top_ptr->load_addr = 0x80000000 + (i * 4);
        top_ptr->load_data = ((uint32_t*)data)[i];
        top_ptr->clk=0; top_ptr->eval(); top_ptr->clk=1; top_ptr->eval();
    }
    top_ptr->load_en = 0;
}
uint32_t paddr_read(uint32_t addr) { if (addr >= 0x80000000) { return pmem_read(addr); } return 0; }
uint32_t isa_reg_read_cpp(int reg_num) { if (reg_num >= 0 && reg_num < 32) { return top_ptr->rootp->Top->reg_file_unit->reg_file[reg_num]; } return 0; }
uint32_t get_pc_cpp() { return top_ptr->rootp->Top->pc_out; }
uint32_t get_inst_cpp() { return top_ptr->rootp->Top->inst; }
uint32_t isa_reg_str2val_cpp(const char *s, bool *success) {
    static const std::map<std::string, int> reg_map = {
        {"pc", -1}, {"zero", 0}, {"ra", 1}, {"sp", 2}, {"gp", 3}, {"tp", 4},
        {"t0", 5}, {"t1", 6}, {"t2", 7}, {"s0", 8}, {"s1", 9}, {"a0", 10},
        {"a1", 11}, {"a2", 12}, {"a3", 13}, {"a4", 14}, {"a5", 15}, {"a6", 16},
        {"a7", 17}, {"s2", 18}, {"s3", 19}, {"s4", 20}, {"s5", 21}, {"s6", 22},
        {"s7", 23}, {"s8", 24}, {"s9", 25}, {"s10", 26}, {"s11", 27},
        {"t3", 28}, {"t4", 29}, {"t5", 30}, {"t6", 31}
    };
    *success = true; std::string str(s);
    if (reg_map.count(str)) { int reg_num = reg_map.at(str); return (reg_num == -1) ? get_pc_cpp() : isa_reg_read_cpp(reg_num); }
    if (str.length() > 1 && str[0] == 'x') { try { int reg_num = std::stoi(str.substr(1)); if (reg_num >= 0 && reg_num < 32) return isa_reg_read_cpp(reg_num); } catch (...) { } }
    *success = false; return 0;
}


void get_dut_regstate_cpp(riscv32_CPU_state *dut) {
    if (!dut) return;
    for (int i = 0; i < 32; i++) {
        dut->gpr[i] = top_ptr->rootp->Top->reg_file_unit->reg_file[i];
    }
    dut->pc = top_ptr->rootp->Top->pc_out;
}
// In npc/csrc/main.cpp

void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n) {
    if (!buf) return;

    // Ensure the address is word-aligned for simplicity
    // This logic is now essential for correctness.
    if (addr % 4 != 0 || n % 4 != 0) {
        // This case should ideally not be hit for program loading.
        // If it is, it indicates another issue.
        printf("Warning: Unaligned memory read in pmem_read_chunk.\n");
        for (size_t i = 0; i < n; i++) {
            // A simple, slow fallback (though still potentially flawed depending on Verilog)
            uint32_t word = pmem_read(addr + i);
            buf[i] = word & 0xFF;
        }
        return;
    }

    // Optimized and CORRECT path for word-aligned access
    for (size_t i = 0; i < n; i += 4) {
        // Read a full 32-bit word from the Verilog model at a word-aligned address
        uint32_t word = pmem_read(addr + i);

        // Copy the 4 bytes of the word into the buffer.
        // memcpy handles the little-endian conversion correctly.
        memcpy(buf + i, &word, 4);
    }
}

void pmem_write_chunk(uint32_t addr, const uint8_t *buf, size_t n) {
    // This function would be needed if the reference could write back to DUT memory.
    // Not implemented for now as it's not required by the current difftest model.
}

} // end of extern "C"

int main(int argc, char** argv) {
    init_monitor(argc, argv);
    sdb_mainloop();
    delete top_ptr;
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return is_exit_status_bad();
}
