  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 0x80
  sb x1, 0(x0)
  lb x2, 0(x0)
  lbu x3, 0(x0)
  lui x4, 0x8
  addi x4, x4, 1
  sh x4, 2(x0)
  lh x5, 2(x0)
  lhu x6, 2(x0)
  lui x7, 0x12345
  addi x7, x7, 0x078
  sw x7, 4(x0)
  lw x8, 4(x0)
  ebreak
