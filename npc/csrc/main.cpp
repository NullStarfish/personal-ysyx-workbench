//#define CONFIG_DIFFTEST

#include <ctime>
#include <sys/types.h>
#include <verilated.h>
#include "VysyxSoCFull.h"
#include "svdpi.h" 
//#include "VTop__Dpi.h" // 确保包含 Verilator 生成的 DPI 头文件

#include <cassert>
#include <cstdio>
#include <map>
#include <string>
#include <cstdint>
#include <cstdlib>
#include <cstring> 
#include "difftest/dut.h"
#include "device.h"
#include <sys/time.h>
#include <csignal>

#include <unistd.h> // for usleep



const long long TARGET_SIM_FREQ = 1000000; 




extern "C" {
    #include "monitor.h"
    #include "state.h"
    #include "sdb/sdb.h"
    #include "trace/itrace.h"
    #include "readline/readline.h"
    #include "reg.h"
    #include "ftrace.h"

#ifdef CONFIG_DIFFTEST
    #include "difftest/dut.h"
#endif
}

/*temp loader*/
#define MEM_SIZE (1024 * 1024) 
//const char *filename = "char-test.bin";


uint8_t *ram = (uint8_t *)malloc(MEM_SIZE);




/////////////////




VysyxSoCFull* top_ptr = NULL;
long long cycle_count = 0;
long long instr_count = 0;

// =========================================================================
// [关键修改] 全局 CPU 状态副本 (由 DPI 自动更新)
// =========================================================================
struct CpuState {
    uint32_t pc;
    uint32_t dnpc; // [新增] 下一个 PC
    uint32_t inst; // [新增]
    uint32_t gpr[32];
    struct {
        uint32_t mtvec;
        uint32_t mepc;
        uint32_t mstatus;
        uint32_t mcause;
    } csrs;
} g_cpu_state;

bool g_has_committed = false;

extern "C" void dpi_update_state(int pc, int dnpc, const svBitVecVal* gprs, 
                                 int mtvec, int mepc, int mstatus, int mcause,
                                 int inst) {
    g_cpu_state.pc = (uint32_t)pc;     // Current PC
    g_cpu_state.dnpc = (uint32_t)dnpc; // [新增] Next PC
    g_cpu_state.inst = (uint32_t)inst;
    
    for(int i = 0; i < 32; i++) {
        g_cpu_state.gpr[i] = gprs[i];
    }
    
    g_cpu_state.csrs.mtvec   = (uint32_t)mtvec;
    g_cpu_state.csrs.mepc    = (uint32_t)mepc;
    g_cpu_state.csrs.mstatus = (uint32_t)mstatus;
    g_cpu_state.csrs.mcause  = (uint32_t)mcause;

    // [新增] 标记本周期有有效指令提交
    g_has_committed = true;
}
// =========================================================================


extern "C" void difftest_skip_ref_cpp(){
#ifdef CONFIG_DIFFTEST
    difftest_skip_ref();
#endif
    return;
} // 声明 Difftest 跳过函数


// [修复] ebreak 现在读取全局状态中的 a0 (gpr[10])
extern "C" void ebreak() {
    uint32_t a0_val = g_cpu_state.gpr[10];
    npc_state.state = (a0_val == 0) ? NPC_END : NPC_ABORT;
    npc_state.halt_ret = a0_val;
    printf("ebreak: state: %d, a0: %d\n", npc_state.state, a0_val);
}

// --- Main Memory Model ---
static uint8_t* pmem = NULL;
static const long PMEM_SIZE = 0x70000000; // 128MB
static const long PMEM_BASE = 0x20000000L;

static long last_pc = -1;



extern "C" void flash_read(int32_t addr, int32_t *data) { assert(0); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { 
    uint32_t pc_offset = (addr - 0x20000000L) & 0xfffffffc;
    uint32_t inst = 
    (uint32_t)pmem[pc_offset + 0] |
    (uint32_t)pmem[pc_offset + 1] << 8  |
    (uint32_t)pmem[pc_offset + 2] << 16 |
    (uint32_t)pmem[pc_offset + 3] << 24;
    *data = inst;
}

void print_stats() {
    printf("\nExecution Statistics:\n");
    printf("  Total Cycles:       %lld\n", cycle_count);
    printf("  Total Instructions: %lld\n", instr_count);
    if (cycle_count > 0) {
        printf("  Average IPC:        %f\n", (double)instr_count / cycle_count);
    } else {
        printf("  Average IPC:        N/A (cycles = 0)\n");
    }
}

void handle_sigint(int sig) {
    printf("\n\nCaught Ctrl+C (SIGINT). Terminating simulation...\n");
    print_stats();
    if (top_ptr) delete top_ptr;
    if (pmem) free(pmem);
    exit(0);
}

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

void sync_after_load() { }

long long get_cycle_count() { return cycle_count; }

void init_verilator(int argc, char *argv[]) {
    pmem = (uint8_t*)malloc(PMEM_SIZE);
    assert(pmem);
    Verilated::commandArgs(argc, argv);
    top_ptr = new VysyxSoCFull;
}

// [修复] 从全局状态读取 PC
uint32_t get_pc_cpp() { 
    return g_cpu_state.pc; 
}

uint32_t get_inst_cpp() { 
    return g_cpu_state.inst; // [修改]
}

void set_dpi_scope() {}

void step_one_clk() {
    // 1. 执行硬件逻辑
    top_ptr->clock = 0; top_ptr->eval();
    top_ptr->clock = 1; top_ptr->eval();

    // 2. 速度控制逻辑 (复用你的 get_time)
    // 使用 static 变量记录该函数被调用的总次数
    static uint64_t total_sim_cycles = 0;
    total_sim_cycles++;

    // 每 1000 个周期 (1ms) 同步一次，避免频繁调用 get_time 拖慢速度
    if (total_sim_cycles % 1000 == 0) {
        
        // 获取从仿真启动开始经过的真实时间 (us)
        uint64_t real_time_us = get_time();
        
        // 计算理论上应该经过的仿真时间 (us)
        // 因为我们设定频率是 1MHz，所以 1 cycle = 1 us
        uint64_t expected_sim_us = total_sim_cycles; 

        // 如果仿真跑得比真实时间快 (Expected > Real)，就睡一会
        if (expected_sim_us > real_time_us) {
            usleep(expected_sim_us - real_time_us);
        }
        // 如果仿真跑得慢 (Expected < Real)，不 sleep，全速追赶
    }
}


void exec_one_cycle_cpp() {
    // 1. 重置提交标志
    g_has_committed = false;

    // 2. 循环单步执行，直到硬件报告“指令已提交”
    // 注意：npc_state.state 的判断是为了防止仿真中途出错（如 ebreak）死循环
    while (!g_has_committed && npc_state.state == NPC_RUNNING) {
        step_one_clk();
        cycle_count++;
    }
    
    if (npc_state.state == NPC_RUNNING) {
        instr_count++;
    }
}


void reset_cpu(int n) {
    top_ptr->reset = 1; 
    for (int i = 0; i < n; ++i) {
        step_one_clk(); 
    }
    top_ptr->reset = 0; 
    top_ptr->eval();    
}

// 这里的路径 u_ifu->blank 可能也需要调整，建议直接根据 PC 变化判断启动
void init_cpu() {
    // 简单起见，reset后直接认为启动，或者检查 PC 是否重置到 START_ADDR
    // while (get_pc_cpp() != 0x80000000) step_one_clk();
    g_cpu_state.pc = 0x20000000;
    g_cpu_state.dnpc = 0x20000000; // [新增]
    g_cpu_state.csrs.mstatus = 0x1800; // Reset value
}

void load_data_to_rom(const uint8_t* data, size_t size) {
    assert(size <= PMEM_SIZE);
    memcpy(pmem, data, size);
}

// [修复] 从全局状态读取 GPR
uint32_t isa_reg_read_cpp(int reg_num) {
    if (reg_num >= 0 && reg_num < 32) {
        return g_cpu_state.gpr[reg_num];
    }
    return 0;
}

// [修复] 从全局状态读取 CSR
uint32_t isa_get_csrs(int csr_num) {
    switch (csr_num) {
        case 0x300: return g_cpu_state.csrs.mstatus;
        case 0x305: return g_cpu_state.csrs.mtvec;
        case 0x341: return g_cpu_state.csrs.mepc;
        case 0x342: return g_cpu_state.csrs.mcause;
        default: return 0;
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
extern "C" void get_dut_regstate_cpp(riscv32_CPU_state *dut) {
    if (!dut) return;

    // [关键修改] Difftest 比较的是执行后的状态，即 Next PC
    dut->pc = g_cpu_state.dnpc; 

    memcpy(dut->gpr, g_cpu_state.gpr, sizeof(dut->gpr));
    dut->csrs.mtvec   = g_cpu_state.csrs.mtvec;
    dut->csrs.mepc    = g_cpu_state.csrs.mepc;
    dut->csrs.mstatus = g_cpu_state.csrs.mstatus;
    dut->csrs.mcause  = g_cpu_state.csrs.mcause;
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
    // if (pmem_read_commit_flag == 0) {
    //     pmem_read_commit_flag = 1;
    //     // Read from memory without side effects for the "probe" call.
    //     // This ensures Verilator gets a consistent value during its evaluation.
    //     long offset = (unsigned int)raddr - PMEM_BASE;
    //     long align_offset = offset & ~0x3u;
    //     if (align_offset < 0 || align_offset + 4 > PMEM_SIZE) return 0;
    //     return (*(uint32_t*)(pmem + align_offset));
    // }
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
    //      raddr, align_offset + PMEM_BASE, *(uint32_t*)(pmem + align_offset));

    // Device Access Logic
    if (align_offset + PMEM_BASE == RTC_ADDR || align_offset + PMEM_BASE == RTC_UP_ADDR) {
        printf("  (RTC device access)\n");
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
    //      waddr, (long)align_offset + PMEM_BASE, wdata, (unsigned char)wmask, new_data);

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



    Verilated::commandArgs(argc, argv);
    init_monitor(argc, argv);
    rl_catch_signals = 0;             
    signal(SIGINT, handle_sigint);  
    sdb_mainloop();
    print_stats();
    delete top_ptr;
    free(pmem);
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return is_exit_status_bad();
}