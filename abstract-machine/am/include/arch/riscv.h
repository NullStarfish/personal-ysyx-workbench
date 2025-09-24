#ifndef ARCH_H__
#define ARCH_H__

#ifdef __riscv_e
#define NR_REGS 16
#else
#define NR_REGS 32
#endif

struct Context {
  // TODO: fix the order of these members to match trap.S
  uintptr_t gpr[NR_REGS], mcause, mstatus, mepc;
  void *pdir;
};

// Based on RISC-V ABI
#ifdef __riscv_e
#define GPR1 gpr[15] // a5, on RV32E a5-a7 are not standard
// Note: RV32E has a reduced register set, the convention might differ slightly
// but for the PA, this is the typical setup. For syscall number, a5 is often used.
#define GPR2 gpr[10] // a0
#define GPR3 gpr[11] // a1
#define GPR4 gpr[12] // a2
#define GPRx gpr[10] // a0 (return value)
#else
// Standard RV32/64 ABI
#define GPR1 gpr[17] // a7 (Syscall number)
#define GPR2 gpr[10] // a0 (Argument 1)
#define GPR3 gpr[11] // a1 (Argument 2)
#define GPR4 gpr[12] // a2 (Argument 3)
#define GPRx gpr[10] // a0 (Return value)
#endif

#endif