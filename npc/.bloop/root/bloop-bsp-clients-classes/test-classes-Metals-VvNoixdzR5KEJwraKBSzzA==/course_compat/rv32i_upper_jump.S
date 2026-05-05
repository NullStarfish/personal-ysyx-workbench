  .option norvc
  .text
  .globl _start
_start:
  lui x1, 0x12345
  auipc x2, 0x1
  addi x3, x0, -1
  slti x4, x3, 0
  sltiu x5, x0, 1
  xori x6, x3, 0xff
  ori x7, x0, 0x55
  andi x8, x7, 0x0f
  slli x9, x7, 4
  srli x10, x9, 4
  srai x11, x3, 4
  jal x12, direct_jump_target
  addi x13, x0, 1
  addi x13, x0, 2
direct_jump_target:
  addi x14, x0, 0x44
  addi x15, x0, indirect_jump_target
  jalr x16, 0(x15)
  addi x17, x0, 0x99
indirect_jump_target:
  ebreak
