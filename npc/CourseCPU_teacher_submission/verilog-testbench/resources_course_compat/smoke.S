  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 5
  addi x2, x0, 7
  add x3, x1, x2
  sw x3, 0(x0)
  lw x4, 0(x0)
  addi x1, x0, 1
  beq x1, x1, branch_target
  addi x5, x0, 0
branch_target:
  addi x5, x0, 9
  addi x0, x0, 0
  addi x6, x0, 1
  sb x6, 1(x0)
  lbu x8, 1(x0)
  jal x9, halt
  addi x9, x0, 0
halt:
  ebreak
