  .option norvc
  .text
  .globl _start
_start:
  lui x1, 0x12345
  addi x2, x2, 16
  or x3, x1, x2
  sub x4, x3, x2
  sll x5, x1, x2
  srli x6, x5, 1
  srai x7, x6, 1
  sw x4, 0(x0)
  lw x8, 0(x0)
  ebreak
