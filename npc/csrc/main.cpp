//#define CONFIG_DIFFTEST


#include <ctime>
#include <sys/types.h>
#include <verilated.h>
#include "VTop.h"
#include "VTop___024root.h"
// MODIFIED: Include the full definitions for the hierarchy
#include "VTop_Top.h"
#include "VTop_RegFile.h"
#include "VTop_CSR.h"
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
#include "VTop_IFU__IBz5.h"
#include "VTop_IDU__Iz5_IBz6.h"
#include "VTop_EXU__Ez6_EBz7.h"
#include "VTop_CSR.h"
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
    uint32_t a0_val = top_ptr->rootp->Top->u_idu->u_regfile->reg_file[10];
    npc_state.state = (a0_val == 0) ? NPC_END : NPC_ABORT;
    npc_state.halt_ret = a0_val;

    printf("ebreak: state: %d\n", npc_state.state);
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

extern "C" {

void assert_fail_msg() {
    isa_reg_display();
    print_iring_buffer();
    print_ftrace_stack();
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

// This signal is now at the top level
uint32_t get_pc_cpp() { return top_ptr->rootp->Top->u_ifu->pc_reg; }

// Correct hierarchical path
uint32_t get_inst_cpp() { return top_ptr->rootp->Top->u_ifu->inst_reg; }

void set_dpi_scope() {
    // No longer needed.
}
void step_one_clk() {
    top_ptr->clk = 0; top_ptr->eval();
    top_ptr->clk = 1; top_ptr->eval();
    cycle_count++; // Increment cycle count for each clock cycle
}

void exec_one_cycle_cpp() {
    last_pc = get_pc_cpp();
    while (get_pc_cpp() == last_pc && npc_state.state == NPC_RUNNING) {
        step_one_clk();
        cycle_count++;
    }
    
}

void reset_cpu(int n) {
    top_ptr->rst = 1; // Assert reset
    for (int i = 0; i < n; ++i) {
        step_one_clk(); // Step n clock cycles while reset is high
    }
    top_ptr->rst = 0; // De-assert reset
    top_ptr->eval();    // Evaluate once with rst=0 to propagate the change
}

void init_cpu() {
    while (!top_ptr->rootp->Top->u_ifu->blank)
        step_one_clk();
}

// ======================= FINAL VERSION =======================
// The loader now simply copies the program data into the C++ memory model.
// No Verilog interaction is needed.
void load_data_to_rom(const uint8_t* data, size_t size) {
    assert(size <= PMEM_SIZE);
    memcpy(pmem, data, size);
}
// ===========================================================


// Correct hierarchical path
uint32_t isa_reg_read_cpp(int reg_num) {
    if (reg_num >= 0 && reg_num < 32) {
        return top_ptr->rootp->Top->u_idu->u_regfile->reg_file[reg_num];
    }
    return 0;
}


uint32_t isa_get_csrs(int csr_num) {
    switch (csr_num) {
        case 0x300: return top_ptr->rootp->Top->u_exu->u_csr->mstatus;
        case 0x305: return top_ptr->rootp->Top->u_exu->u_csr->mtvec;
        case 0x341: return top_ptr->rootp->Top->u_exu->u_csr->mepc;
        case 0x342: return top_ptr->rootp->Top->u_exu->u_csr->mcause;
        default: return 0; // 未实现其他 CSR
    }
}



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
        dut->gpr[i] = isa_reg_read_cpp(i);
    }
    // This signal is now at the top level
    dut->pc = get_pc_cpp();
    
    dut->csrs.mtvec = top_ptr->rootp->Top->u_exu->u_csr->mtvec;
    dut->csrs.mepc = top_ptr->rootp->Top->u_exu->u_csr->mepc;
    dut->csrs.mstatus = top_ptr->rootp->Top->u_exu->u_csr->mstatus;
    dut->csrs.mcause = top_ptr->rootp->Top->u_exu->u_csr->mcause;
    
}
void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n) {
    long offset = (unsigned int)addr - PMEM_BASE;
    if (offset < 0 || offset + n > PMEM_SIZE || !buf) return;
    memcpy(buf, pmem + offset, n);
}
}


static int pmem_read_commit_flag = 0;

extern "C" int pmem_read(int raddr) {
    // --- Execution Guard ---
    // On the first call by Verilator (flag=0), do nothing but toggle the flag.
    // On the second call (flag=1), execute the full logic.
    if (pmem_read_commit_flag == 0) {
        pmem_read_commit_flag = 1;
        // Read from memory without side effects for the "probe" call.
        // This ensures Verilator gets a consistent value during its evaluation.
        long offset = (unsigned int)raddr - PMEM_BASE;
        long align_offset = offset & ~0x3u;
        if (align_offset < 0 || align_offset + 4 > PMEM_SIZE) return 0;
        return (*(uint32_t*)(pmem + align_offset));
    }
    // This is the "commit" call. Reset the flag for the next logical access.
    pmem_read_commit_flag = 0;

    // --- Main Logic (Executes only ONCE per DUT access) ---

    // Address calculation
    long offset = (unsigned int)raddr - PMEM_BASE;
    long align_offset = offset & ~0x3u;

    // Boundary check
    if (align_offset < 0 || align_offset + 4 > PMEM_SIZE) return 0;

    // Debug Printing
    //printf("pmem_read at 0x%08x, aligned_addr = 0x%08lx, result = 0x%08x\n",
    //       raddr, align_offset + PMEM_BASE, *(uint32_t*)(pmem + align_offset));

    // Device Access Logic
    if (align_offset + PMEM_BASE == RTC_ADDR || align_offset + PMEM_BASE == RTC_UP_ADDR) {
        //printf("  (RTC device access)\n");
        if (align_offset + PMEM_BASE == RTC_ADDR) {
            time_t timep;
            struct tm *p;
            time(&timep);
            p = localtime(&timep);
            uint32_t *rtc_low_addr = (uint32_t*)(pmem + (RTC_ADDR - PMEM_BASE));
            uint32_t *rtc_high_addr = (uint32_t*)(pmem + (RTC_ADDR - PMEM_BASE + 4));
            *rtc_low_addr = (p->tm_sec) | (p->tm_min << 8) | (p->tm_hour << 16);
            *rtc_high_addr = (p->tm_year + 1900) | ((p->tm_mon + 1) << 16) | (p->tm_mday << 24);
        } else if (align_offset + PMEM_BASE == RTC_UP_ADDR) {
            uint64_t us = get_time();
            uint32_t *rtc_us_low_addr = (uint32_t*)(pmem + (RTC_UP_ADDR - PMEM_BASE));
            uint32_t *rtc_us_high_addr = (uint32_t*)(pmem + (RTC_UP_ADDR - PMEM_BASE + 4));
            *rtc_us_low_addr = (uint32_t)(us & 0xFFFFFFFF);
            *rtc_us_high_addr = (uint32_t)(us >> 32);
        }

        // Difftest Interaction
        #ifdef CONFIG_DIFFTEST
        //printf("  Difftest: Skipping REF execution for device read.\n");
        difftest_skip_ref();
        #endif
    }
    if (align_offset + PMEM_BASE == RTC_ADDR + 4 || align_offset + PMEM_BASE == RTC_UP_ADDR + 4) {
        #ifdef CONFIG_DIFFTEST
        //printf("  Difftest: Skipping REF execution for device read.\n");
        difftest_skip_ref();
        #endif
    }



    // Return the memory content
    return (*(uint32_t*)(pmem + align_offset));
}

// Static flag for pmem_write.
static int pmem_write_commit_flag = 0;

extern "C" void pmem_write(int waddr, int wdata, char wmask) {
    // --- Execution Guard ---
    if (pmem_write_commit_flag == 0) {
        pmem_write_commit_flag = 1;
        return; // For the "probe" write call, do nothing.
    }
    // This is the "commit" call.
    pmem_write_commit_flag = 0;

    // --- Main Logic (Executes only ONCE per DUT access) ---

    // Address and mask calculation
    long offset = (unsigned int)waddr - PMEM_BASE;
    long align_offset = offset & ~0x3u;
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

    // Debug Printing
    //printf("pc = 0x%08x, cycle = %lld\n", get_pc_cpp(), cycle_count);
    //printf("pmem_write at 0x%08x, aligned_addr = 0x%08lx, data = 0x%08x, wmask = 0b%04b, written_data = 0x%08x\n",
    //       waddr, (long)align_offset + PMEM_BASE, wdata, (unsigned char)wmask, new_data);

    // Device Access Logic
    if (align_offset + PMEM_BASE == SERIAL_PORT) {
        //printf("  (Serial port access, char: '%c')\n", (char)new_data);
        putchar((char)new_data);
        fflush(stdout);

        // Difftest Interaction
        #ifdef CONFIG_DIFFTEST
        //printf("  Difftest: Skipping REF execution for device write.\n");
        difftest_skip_ref();
        #endif
    }

    // Perform the memory write
    *paddr = new_data;
}




int main(int argc, char** argv) {

    init_monitor(argc, argv);
    reset_cpu(100);
    init_cpu();
    sdb_mainloop();
    delete top_ptr;
    free(pmem);
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return is_exit_status_bad();
}

