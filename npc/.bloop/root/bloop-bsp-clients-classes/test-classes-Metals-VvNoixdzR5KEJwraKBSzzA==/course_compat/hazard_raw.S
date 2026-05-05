  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 1
  addi x2, x1, 1
  add x3, x2, x2
  add x4, x3, x3
  add x5, x4, x4
  ebreak
