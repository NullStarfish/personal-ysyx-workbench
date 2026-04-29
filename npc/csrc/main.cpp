//#define CONFIG_DIFFTEST

#include <ctime>
#include <sys/types.h>
#include "verilated.h"
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
#include <iostream>


#ifdef CONFIG_BOARD
#include <nvboard.h>
void nvboard_bind_all_pins(VysyxSoCFull* top);
#endif



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

#ifdef CONFIG_BOARD
static void nvboard_probe_gpio_out() {
    static uint16_t last_gpio_out = 0xffffu;
    uint16_t gpio_out = top_ptr ? top_ptr->externalPins_gpio_out : 0;
    if (gpio_out != last_gpio_out) {
        printf("[nvboard] top externalPins_gpio_out = 0x%04x\n", gpio_out);
        fflush(stdout);
        last_gpio_out = gpio_out;
    }
}

static int nvboard_readline_event_hook() {
    nvboard_update();
    usleep(1000);
    return 0;
}
#endif

// =========================================================================
// 全局退休快照（由 DPI 在指令退休当拍更新）
//
// 语义约定：
// - pc/inst/dnpc: 当前 retiring instruction 的元信息
// - gpr/csr: 与该退休事件对齐的架构态快照
//
// Difftest 仍然使用 dnpc 作为“执行后 PC”，而 itrace/ftrace 继续使用
// retire.pc / retire.inst 作为“当前退休指令”的观测窗口。
// =========================================================================
struct CpuState {
    uint32_t pc;
    uint32_t dnpc;
    uint32_t inst;
    uint32_t gpr[32];
    struct {
        uint32_t mtvec;
        uint32_t mepc;
        uint32_t mstatus;
        uint32_t mcause;
    } csrs;
} g_cpu_state;

static uint32_t g_last_retire_pc = 0;
static uint32_t g_last_retire_inst = 0;

bool g_has_committed = false;

extern "C" void dpi_update_state(int pc, int dnpc, int reg_wen, int reg_addr, int reg_data, const svBitVecVal* gprs, 
                                 int mtvec, int mepc, int mstatus, int mcause,
                                 int inst) {
    g_last_retire_pc = (uint32_t)pc;
    g_last_retire_inst = (uint32_t)inst;

    g_cpu_state.pc = (uint32_t)dnpc;
    g_cpu_state.dnpc = (uint32_t)dnpc;
    g_cpu_state.inst = (uint32_t)inst;
    
    for(int i = 0; i < 32; i++) {
        g_cpu_state.gpr[i] = gprs[i];
    }
    if (reg_wen && reg_addr != 0) {
        g_cpu_state.gpr[reg_addr & 0x1f] = (uint32_t)reg_data;
    }
    
    g_cpu_state.csrs.mtvec   = (uint32_t)mtvec;
    g_cpu_state.csrs.mepc    = (uint32_t)mepc;
    g_cpu_state.csrs.mstatus = (uint32_t)mstatus;
    g_cpu_state.csrs.mcause  = (uint32_t)mcause;

    // 标记本周期已经观察到一次退休事件
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
static uint8_t* psram_mem = NULL;
static uint16_t* sdram_mem[2] = {NULL, NULL};
static const long PMEM_SIZE = 0x70000000; // 128MB
static const long PMEM_BASE = 0x30000000L;
static const uint32_t PROGRAM_BASE = 0xa0000000u;
static const uint32_t PSRAM_BASE = 0x80000000u;
static const uint32_t PSRAM_SIZE = 0x01000000u;
static const uint32_t SDRAM_BASE = 0xa0000000u;
static const uint32_t SDRAM_HALFWORDS = 0x00400000u;

static long last_pc = -1;


uint8_t *flash_mem;




extern "C" void flash_read(int32_t addr, int32_t *data) { 
    uint32_t pc_offset = (addr ) & 0xfffffffc;
    uint32_t inst =     (uint32_t)pmem[pc_offset + 3] |
    (uint32_t)pmem[pc_offset + 2] << 8  |
    (uint32_t)pmem[pc_offset + 1] << 16 |
    (uint32_t)pmem[pc_offset + 0] << 24;

    *data = inst;
}
extern "C" void mrom_read(int32_t addr, int32_t *data) { 
    uint32_t pc_offset = (addr - 0x20000000L) & 0xfffffffc;
    uint32_t inst = 
    (uint32_t)pmem[pc_offset + 0] |
    (uint32_t)pmem[pc_offset + 1] << 8  |
    (uint32_t)pmem[pc_offset + 2] << 16 |
    (uint32_t)pmem[pc_offset + 3] << 24;
    *data = inst;
}

extern "C" void psram_read_byte(int32_t addr, uint8_t *data) {
    uint32_t uaddr = static_cast<uint32_t>(addr);
    uint8_t value = psram_mem[uaddr];
    *data = value;
}

extern "C" void psram_write_byte(int32_t addr, uint8_t data) {
    uint32_t uaddr = static_cast<uint32_t>(addr);
    psram_mem[uaddr] = data;
}

extern "C" void sdram_read_halfword_chip(int chip, int32_t addr, uint16_t *data) {
    uint32_t uaddr = static_cast<uint32_t>(addr);
    *data = sdram_mem[chip & 1][uaddr];
}

extern "C" void sdram_write_halfword_chip(int chip, int32_t addr, uint16_t data, uint8_t mask) {
    uint32_t uaddr = static_cast<uint32_t>(addr);
    uint16_t old = sdram_mem[chip & 1][uaddr];
    uint16_t next = old;
    if (mask & 0x1) next = (next & 0xff00u) | (data & 0x00ffu);
    if (mask & 0x2) next = (next & 0x00ffu) | (data & 0xff00u);
    sdram_mem[chip & 1][uaddr] = next;
}

static inline uint32_t sdram_linear_halfaddr_from_bus(uint32_t addr) {
    uint32_t offset = addr - SDRAM_BASE;
    uint32_t col = (offset >> 2) & 0x1ffu;
    uint32_t row = (offset >> 13) & 0x7ffu;
    uint32_t bank = (offset >> 11) & 0x3u;
    return (bank << 22) | (row << 9) | col;
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
    if (psram_mem) free(psram_mem);
    if (sdram_mem[0]) free(sdram_mem[0]);
    if (sdram_mem[1]) free(sdram_mem[1]);
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
    psram_mem = (uint8_t*)malloc(PSRAM_SIZE);
    sdram_mem[0] = (uint16_t*)malloc(sizeof(uint16_t) * SDRAM_HALFWORDS);
    sdram_mem[1] = (uint16_t*)malloc(sizeof(uint16_t) * SDRAM_HALFWORDS);
    assert(pmem);
    assert(psram_mem);
    assert(sdram_mem[0]);
    assert(sdram_mem[1]);
    memset(psram_mem, 0, PSRAM_SIZE);
    memset(sdram_mem[0], 0, sizeof(uint16_t) * SDRAM_HALFWORDS);
    memset(sdram_mem[1], 0, sizeof(uint16_t) * SDRAM_HALFWORDS);
    Verilated::commandArgs(argc, argv);
    top_ptr = new VysyxSoCFull;

#ifdef CONFIG_BOARD
    nvboard_bind_all_pins(top_ptr);
    nvboard_init();
#endif
}

uint32_t get_pc_cpp() { 
    return g_cpu_state.pc; 
}

uint32_t get_retire_pc_cpp() {
    return g_last_retire_pc;
}

uint32_t get_inst_cpp() { 
    return g_last_retire_inst;
}

void set_dpi_scope() {}

void step_one_clk() {
    // 1. 执行硬件逻辑
    top_ptr->clock = 0; top_ptr->eval();
    top_ptr->clock = 1; top_ptr->eval();
#ifdef CONFIG_BOARD
    nvboard_probe_gpio_out();
    nvboard_update();
#endif

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
    #ifdef CONFIG_BOARD
        nvboard_update();
    #endif
}

void nvboard_flush_cpp() {
#ifdef CONFIG_BOARD
    uint64_t deadline = get_time() + 25000;
    while (get_time() < deadline) {
        nvboard_update();
        usleep(1000);
    }
#endif
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
    g_cpu_state.pc = PROGRAM_BASE;
    g_cpu_state.dnpc = PROGRAM_BASE;
    g_cpu_state.csrs.mstatus = 0x1800; // Reset value
}

void load_data_to_rom(const uint8_t* data, size_t size) {
    const uint32_t end_addr = PROGRAM_BASE + static_cast<uint32_t>(size);
    const uint32_t start_rank = (PROGRAM_BASE - SDRAM_BASE) >> 24;
    const uint32_t end_rank = ((end_addr - 1) - SDRAM_BASE) >> 24;
    assert(start_rank == end_rank);
    for (size_t off = 0; off < size; off += 4) {
        uint32_t addr = PROGRAM_BASE + static_cast<uint32_t>(off);
        uint32_t halfaddr = sdram_linear_halfaddr_from_bus(addr);
        uint16_t lower = 0;
        uint16_t upper = 0;
        if (off + 0 < size) lower |= static_cast<uint16_t>(data[off + 0]);
        if (off + 1 < size) lower |= static_cast<uint16_t>(data[off + 1]) << 8;
        if (off + 2 < size) upper |= static_cast<uint16_t>(data[off + 2]);
        if (off + 3 < size) upper |= static_cast<uint16_t>(data[off + 3]) << 8;
        sdram_mem[0][halfaddr] = lower;
        sdram_mem[1][halfaddr] = upper;
    }
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

    // Difftest 比较的是执行后的架构态，因此这里使用 retiring
    // instruction 的 dnpc 作为 post-state PC。
    dut->pc = g_cpu_state.dnpc; 

    memcpy(dut->gpr, g_cpu_state.gpr, sizeof(dut->gpr));
    dut->csrs.mtvec   = g_cpu_state.csrs.mtvec;
    dut->csrs.mepc    = g_cpu_state.csrs.mepc;
    dut->csrs.mstatus = g_cpu_state.csrs.mstatus;
    dut->csrs.mcause  = g_cpu_state.csrs.mcause;
}

void pmem_read_chunk(uint32_t addr, uint8_t *buf, size_t n) {
    if (!buf) return;
    if (addr >= SDRAM_BASE && (uint64_t)(addr - SDRAM_BASE) + n <= (1ull << 25)) {
        for (size_t i = 0; i < n; ++i) {
            uint32_t cur = addr + static_cast<uint32_t>(i);
            uint32_t halfaddr = sdram_linear_halfaddr_from_bus(cur);
            uint16_t lower = sdram_mem[0][halfaddr];
            uint16_t upper = sdram_mem[1][halfaddr];
            switch (cur & 0x3u) {
                case 0: buf[i] = lower & 0xffu; break;
                case 1: buf[i] = (lower >> 8) & 0xffu; break;
                case 2: buf[i] = upper & 0xffu; break;
                default: buf[i] = (upper >> 8) & 0xffu; break;
            }
        }
        return;
    }
    if (addr >= PSRAM_BASE && (uint64_t)(addr - PSRAM_BASE) + n <= PSRAM_SIZE) {
        memcpy(buf, psram_mem + (addr - PSRAM_BASE), n);
        return;
    }
    long offset = (unsigned int)addr - PMEM_BASE;
    if (offset < 0 || offset + n > PMEM_SIZE) return;
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
    free(sdram_mem[0]);
    free(sdram_mem[1]);
    printf("Simulation finished after %lld execution cycles.\n", cycle_count);
    return is_exit_status_bad();
}
