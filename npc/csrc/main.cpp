//#define CONFIG_DIFFTEST


#include <ctime>
#include <sys/types.h>
#include <verilated.h>
#include "VTop.h"
#include "VTop___024root.h"
// MODIFIED: Include the full definitions for the hierarchy
#include "VTop_Top.h"
#include "VTop_datapath.h"
#include "VTop_RegFile.h"
#include "VTop_CSRs.h"
#include "svdpi.h"
#include <cassert>
#include <cstdio>
#include <map>
#include <string>
#include <cstdint>
#include <cstdlib>
#include <cstring> // For memcpy
#include "difftest/dut.h"
#include "device.h"
#include <sys/time.h>
extern "C" {
    #include "monitor.h"
    #include "state.h"
    #include "sdb/sdb.h"
    #include "trace/itrace.h"
    #include "reg.h"
    #include "ftrace.h"

#ifdef CONFIG_DIFFTEST
    #include "difftest/dut.h"
#endif
}





VTop* top_ptr = NULL;
long long cycle_count = 0;

extern "C" void ebreak() {
    // Correct hierarchical path
    uint32_t a0_val = top_ptr->rootp->Top->datapath_unit->reg_file_unit->reg_file[10];
    npc_state.state = (a0_val == 0) ? NPC_END : NPC_ABORT;
    npc_state.halt_ret = a0_val;
}







// --- Main Memory Model ---
static uint8_t* pmem = NULL;
static const long PMEM_SIZE = 0x70000000; // 128MB
static const long PMEM_BASE = 0x80000000;

static long last_pc = -1;




static uint64_t boot_time = 0;

static uint64_t get_time_internal() {

  struct timeval now;
  gettimeofday(&now, NULL);
  uint64_t us = now.tv_sec * 1000000 + now.tv_usec;
  return us;
}

uint64_t get_time() {
  if (boot_time == 0) boot_time = get_time_internal();
  uint64_t now = get_time_internal();
  return now - boot_time;
}


// --- DPI-C Interface for Memory ---
extern "C" int pmem_read(int raddr) {
    long offset = (unsigned int)raddr - PMEM_BASE;
    long align_offset = offset & ~0x3u; // Align to 4 bytes
    if (align_offset < 0 || align_offset + 4 > PMEM_SIZE) return 0;
    

    if (last_pc != top_ptr->rootp->Top->pc_out) { 

        if (align_offset + PMEM_BASE == RTC_ADDR) {
            time_t timep;
            struct tm *p;
            time(&timep);
            p = localtime(&timep);
            *(uint32_t*)(pmem + align_offset) = (p->tm_sec) | (p->tm_min << 6) | (p->tm_hour << 12);
            *(uint32_t*)(pmem + align_offset + 4) = (p->tm_year + 1900) | ((p->tm_mon + 1) << 12) | (p->tm_mday << 16);
        } else if (align_offset + PMEM_BASE == RTC_UP_ADDR) {//这意味着需要先访问低位来进行更新
            uint64_t us = get_time();
            *(uint32_t*)(pmem + align_offset) = (uint32_t)(us & 0xFFFFFFFF);
            *(uint32_t*)(pmem + align_offset + 4) = (uint32_t)(us >> 32);
            //printf("RTC_UP read: %x\n", *(uint32_t*)(pmem + align_offset));
        }

        //printf("pmem_read at %x, aligned addr = %lx, result = %x\n", raddr, align_offset + PMEM_BASE, *(uint32_t*)(pmem + align_offset));
    }
    
    return (*(uint32_t*)(pmem + align_offset));
}


static int flag = 0;
extern "C" void pmem_write(int waddr, int wdata, char wmask) {
    long offset = (unsigned int)waddr - PMEM_BASE;
    long align_offset = offset & ~0x3u; // Align to 4 bytes
    if (align_offset < 0 || align_offset + 4 > PMEM_SIZE) return;
    uint32_t *paddr = (uint32_t*)(pmem + align_offset);
    uint32_t old_data = *paddr;
    uint32_t wmask_u32  = 0;
    for (int i = 0; i < 4; i++) {
        if (wmask & (1 << i)) {
            wmask_u32 |= (0xFF << (i * 8));
        }
    }


    uint32_t new_data = (old_data & ~wmask_u32) | (wdata & wmask_u32);
    if (last_pc != top_ptr->rootp->Top->pc_out) { 
        if (align_offset + PMEM_BASE == SERIAL_PORT) {
            putchar(new_data);
            //printf("access serial\n");
            fflush(stdout);
        }
        //printf("pc = %08x, cycle = %lld\n", top_ptr->rootp->Top->pc_out, cycle_count);
        //printf("pmem write at %x, aligned addr = %lx, data = %x, wmask = %b, masked data = %x\n", waddr, (long)align_offset + PMEM_BASE, wdata, wmask, new_data);
    }

    if (align_offset + PMEM_BASE == SERIAL_PORT) {
        //printf("access serial, flag = %d\n", flag);
#ifdef CONFIG_DIFFTEST
        //printf("Difftest skip ref\n");
        if (flag)
            difftest_skip_ref();
        flag = !flag;
#endif
    }



    *paddr = new_data;


    
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
    last_pc = top_ptr->rootp->Top->pc_out;
}

void sync_after_load() {
    // This function is no longer needed but kept for compatibility.
}

long long get_cycle_count() { return cycle_count; }

void init_verilator(int argc, char *argv[]) {
    pmem = (uint8_t*)malloc(PMEM_SIZE);
    assert(pmem);
    Verilated::commandArgs(argc, argv);
    top_ptr = new VTop;
}

void set_dpi_scope() {
    // No longer needed.
}

void reset_cpu(int n) {
    top_ptr->rst = 1;
    for (int i = 0; i < n; ++i) { exec_one_cycle_cpp(); }
    top_ptr->rst = 0;
    top_ptr->eval();
}

// ======================= FINAL VERSION =======================
// The loader now simply copies the program data into the C++ memory model.
// No Verilog interaction is needed.
void load_data_to_rom(const uint8_t* data, size_t size) {
    assert(size <= PMEM_SIZE);
    memcpy(pmem, data, size);
}
// ===========================================================

uint32_t paddr_read(uint32_t addr) { return pmem_read(addr); }

// Correct hierarchical path
uint32_t isa_reg_read_cpp(int reg_num) {
    if (reg_num >= 0 && reg_num < 32) {
        return top_ptr->rootp->Top->datapath_unit->reg_file_unit->reg_file[reg_num];
    }
    return 0;
}

uint32_t isa_get_csrs(int csr_num) {
    switch (csr_num) {
        case 0x300: return top_ptr->rootp->Top->datapath_unit->csr_unit->mstatus;
        case 0x305: return top_ptr->rootp->Top->datapath_unit->csr_unit->mtvec;
        case 0x341: return top_ptr->rootp->Top->datapath_unit->csr_unit->mepc;
        case 0x342: return top_ptr->rootp->Top->datapath_unit->csr_unit->mcause;
        default: return 0; // 未实现其他 CSR
    }
}

// This signal is now at the top level
uint32_t get_pc_cpp() { return top_ptr->rootp->Top->pc_out; }

// Correct hierarchical path
uint32_t get_inst_cpp() { return top_ptr->rootp->Top->datapath_unit->inst; }

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
    top_ptr->eval();
    if (!dut) return;
    for (int i = 0; i < 32; i++) {
        // Correct hierarchical path
        dut->gpr[i] = top_ptr->rootp->Top->datapath_unit->reg_file_unit->reg_file[i];
    }
    // This signal is now at the top level
    dut->pc = top_ptr->rootp->Top->pc_out;
    
    dut->csrs.mtvec = top_ptr->rootp->Top->datapath_unit->csr_unit->mtvec;
    dut->csrs.mepc = top_ptr->rootp->Top->datapath_unit->csr_unit->mepc;
    dut->csrs.mstatus = top_ptr->rootp->Top->datapath_unit->csr_unit->mstatus;
    dut->csrs.mcause = top_ptr->rootp->Top->datapath_unit->csr_unit->mcause;
}
void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n) {
    long offset = (unsigned int)addr - PMEM_BASE;
    if (offset < 0 || offset + n > PMEM_SIZE || !buf) return;
    memcpy(buf, pmem + offset, n);
}
}

int main(int argc, char** argv) {

    init_monitor(argc, argv);
    reset_cpu(5);
    sdb_mainloop();
    delete top_ptr;
    free(pmem);
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return is_exit_status_bad();
}

