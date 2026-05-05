  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 17
  sw x1, 0(x0)
  lw x2, 0(x0)
  add x3, x2, x1
  ebreak
