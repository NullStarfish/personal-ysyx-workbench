ENTRY(_start)

MEMORY {
  sdram (rwx) : ORIGIN = 0xa0000000, LENGTH = 32M
  sram  (rwx) : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  .text : {
    *(entry)
    *(.text*)
  } > sdram

  .rodata : {
    *(.rodata*)
  } > sdram

  .data : {
    _data_vma = .;
    *(.data*)
    *(.sdata*)
    . = ALIGN(4);
    _data_vma_end = .;
  } > sdram

  _data_lma = LOADADDR(.data);

  .bss : {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    . = ALIGN(4);
    _bss_end = .;
  } > sdram

  _heap_start = ALIGN(4);

  _stack_pointer = ORIGIN(sram) + LENGTH(sram);

  _end = .;
}
