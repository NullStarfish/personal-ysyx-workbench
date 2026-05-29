ENTRY(_start)

MEMORY {
  flash (rx)  : ORIGIN = _flash_start, LENGTH = _flash_size
  sram  (rwx) : ORIGIN = _sram_start,  LENGTH = _sram_size
  sdram (rwx) : ORIGIN = _sdram_start, LENGTH = _sdram_size
}

SECTIONS {
  .text.stage0 : {
    KEEP(*(entry))
    KEEP(*(.text.stage0*))
    . = ALIGN(4);
  } > flash

  .text.stage1 : {
    _stage1_vma = .;
    KEEP(*(.text.stage1*))
    *(.rodata.stage1*)
    *(.srodata.stage1*)
    . = ALIGN(4);
  } > sram AT> flash

  .data.stage1 : {
    *(.data.stage1*)
    *(.sdata.stage1*)
    . = ALIGN(4);
    _stage1_vma_end = .;
  } > sram AT> flash
  _stage1_lma = LOADADDR(.text.stage1);

  .bss.stage1 (NOLOAD) : {
    _stage1_bss_start = .;
    *(.bss.stage1*)
    *(.sbss.stage1*)
    . = ALIGN(4);
    _stage1_bss_end = .;
  } > sram

  _sram_stack_pointer = ORIGIN(sram) + LENGTH(sram);

  .text : {
    _sdram_vma = .;
    KEEP(*(.text.sdram_entry*))
    *(.text*)
    . = ALIGN(8);
  } > sdram AT> flash
  _sdram_lma = LOADADDR(.text);

  .rodata : {
    *(.rodata*)
    *(.srodata*)
    *(.eh_frame*)
    . = ALIGN(4);
  } > sdram AT> flash

  .data : {
    _data_vma = .;
    *(.data*)
    *(.sdata*)
    *(.sdata2*)
    *(.init_array*)
    *(.fini_array*)
    . = ALIGN(4);
    _data_vma_end = .;
  } > sdram AT> flash

  .data.extra : {
    __fsymtab_start = .;
    KEEP(*(FSymTab))
    __fsymtab_end = .;
    . = ALIGN(8);

    __vsymtab_start = .;
    KEEP(*(VSymTab))
    __vsymtab_end = .;
    . = ALIGN(8);

    . = ALIGN(8);
    __rt_init_start = .;
    KEEP(*(SORT(.rti_fn*)))
    __rt_init_end = .;
    . = ALIGN(8);

    __rt_utest_tc_tab_start = .;
    KEEP(*(UtestTcTab))
    __rt_utest_tc_tab_end = .;

    . = ALIGN(8);
    __am_apps_data_start = .;
    *(__am_apps.data*)
    *(__am_apps.sdata*)
    __am_apps_data_end = .;
    . = ALIGN(8);
  } > sdram AT> flash
  _sdram_vma_end = .;

  _data_lma = LOADADDR(.data);

  .bss : {
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    *(COMMON)
    . = ALIGN(4);
    _bss_end = .;
  } > sdram

  .bss.extra (NOLOAD) : {
    . = ALIGN(8);
    __am_apps_bss_start = .;
    *(__am_apps.bss*)
    *(__am_apps.sbss*)
    *(__am_apps.scommon*)
    __am_apps_bss_end = .;
    . = ALIGN(8);
    _bss_end = .;
  } > sdram

  _heap_start = ALIGN(4);
  _stack_pointer = ORIGIN(sdram) + LENGTH(sdram);
  _end = .;
}
