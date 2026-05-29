.section entry, "ax"
.globl _start
.type _start, @function

_start:
  la t0, _stage1_lma
  la t1, _stage1_vma
  la t2, _stage1_vma_end
1:
  bgeu t1, t2, 2f
  lw a0, 0(t0)
  sw a0, 0(t1)
  addi t0, t0, 4
  addi t1, t1, 4
  j 1b
2:
  la t1, _stage1_bss_start
  la t2, _stage1_bss_end
3:
  bgeu t1, t2, 4f
  sw zero, 0(t1)
  addi t1, t1, 4
  j 3b
4:
  la sp, _sram_stack_pointer
  la t0, _stage1_start
  jr t0

.section .text.stage1, "ax"
.globl _stage1_start
.type _stage1_start, @function

_stage1_start:
  la sp, _sram_stack_pointer
  call __am_bootloader_main
1:
  j 1b

.section .text.sdram_entry, "ax"
.globl _sdram_entry
.type _sdram_entry, @function

_sdram_entry:
  li ra, 0
  li gp, 0
  li tp, 0
  li t0, 0
  li t1, 0
  li t2, 0
  li s0, 0
  li s1, 0
  li a0, 0
  li a1, 0
  li a2, 0
  li a3, 0
  li a4, 0
  li a5, 0
  la sp, _stack_pointer
  call _trm_init
