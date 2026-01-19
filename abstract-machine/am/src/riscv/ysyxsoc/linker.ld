ENTRY(_start)

/* 定义物理内存区域 */
MEMORY {
  flash (rx) : ORIGIN = 0x30000000, LENGTH = 64K/* 只读，存放代码和只读数据 */
  sram (rwx): ORIGIN = 0x0f000000, LENGTH = 8K   /* 读写，存放 data, bss, 堆栈 */
}

SECTIONS {
  /* 
   * .text 和 .rodata 直接放在 flash 中 
   * 运行时地址(VMA) = 加载地址(LMA)
   */
  .text : {
    *(entry)
    *(.text*)
  } > flash

  .rodata : {
    *(.rodata*)
  } > flash

  /* 
   * .data 段是关键：
   * > sram    表示运行时它应该在 SRAM (VMA)
   * AT> flash  表示烧录时它应该在 flash (LMA)
   */
  .data : {
    _data_vma = .;    /* 记录 SRAM 中的开始地址 */
    *(.data*)
    *(.sdata*)
    . = ALIGN(4);
    _data_vma_end = .;
  } > sram AT> flash

  /* 
   * 获取 .data 段在 flash 中的起始地址 
   * 我们将在 C 代码中使用这个符号来复制数据
   */
  _data_lma = LOADADDR(.data);

  /* .bss 段只占用 SRAM，不需要在 flash 中占空间 */
  .bss : {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(4);
    _bss_end = .;
  } > sram

  /* 
   * 堆区起始地址 
   * 紧接在 .bss 后面
   */
  _heap_start = ALIGN(4);

  /* 
   * 栈顶定义
   * 直接设为 SRAM 的末尾 (0x0f000000 + 8K = 0x0f002000)
   * 栈向下生长
   */
  _stack_pointer = ORIGIN(sram) + LENGTH(sram);
  
  /* 定义 _end 符号，防止 AM 报错 */
  _end = .;
}