  .option norvc
  .text
  .globl _start
_start:
  addi x1, x0, 7
  addi x2, x0, 3
  addi x3, x0, -1
  addi x4, x0, 1
  add x5, x1, x2
  sub x6, x1, x2
  sll x7, x1, x2
  slt x8, x2, x1
  sltu x9, x3, x4
  xor x10, x1, x2
  srl x11, x3, x4
  sra x12, x3, x4
  or x13, x1, x2
  and x14, x1, x2
  beq x1, x2, beq_not_taken_target
  addi x21, x0, 1
beq_not_taken_target:
  beq x1, x1, beq_taken_target
  addi x20, x0, 1
beq_taken_target:
  bne x1, x2, bne_taken_target
  addi x22, x0, 2
bne_taken_target:
  blt x2, x1, blt_taken_target
  addi x23, x0, 3
blt_taken_target:
  bge x1, x2, bge_taken_target
  addi x24, x0, 4
bge_taken_target:
  bltu x4, x3, bltu_taken_target
  addi x25, x0, 5
bltu_taken_target:
  bgeu x3, x4, bgeu_taken_target
  addi x26, x0, 6
bgeu_taken_target:
  ebreak
