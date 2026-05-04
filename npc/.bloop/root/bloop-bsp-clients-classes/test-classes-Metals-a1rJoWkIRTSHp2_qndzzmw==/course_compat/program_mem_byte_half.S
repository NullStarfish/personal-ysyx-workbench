  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 0xab
  sb x1, 1(x0)
  lbu x2, 1(x0)
  addi x3, x0, 0x234
  sh x3, 2(x0)
  lhu x3, 2(x0)
  ebreak
