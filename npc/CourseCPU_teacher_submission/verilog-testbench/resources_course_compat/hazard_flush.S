  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 1
  beq x1, x1, branch_target
  addi x2, x0, 0xaa
branch_target:
  addi x2, x0, 9
  jal x3, jump_target
  addi x4, x0, 0x55
jump_target:
  addi x4, x0, 7
  ebreak
