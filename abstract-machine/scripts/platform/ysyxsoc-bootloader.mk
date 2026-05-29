AM_SRCS := riscv/ysyxsoc/bootloader/start.S \
           riscv/ysyxsoc/bootloader.c \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/input.c \
           riscv/ysyxsoc/gpu.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
CFLAGS    += -I$(AM_HOME)/am/src/riscv/ysyxsoc
LDSCRIPTS += $(AM_HOME)/am/src/riscv/ysyxsoc/bootloader/linker.ld

YSYXSOC_FLASH_BASE ?= 0x30000000
YSYXSOC_FLASH_SIZE ?= 0x10000000
YSYXSOC_SRAM_BASE  ?= 0x0f000000
YSYXSOC_SRAM_SIZE  ?= 0x2000
YSYXSOC_SDRAM_BASE ?= 0xa0000000
YSYXSOC_SDRAM_SIZE ?= 0x2000000

LDFLAGS   += --defsym=_flash_start=$(YSYXSOC_FLASH_BASE)
LDFLAGS   += --defsym=_flash_size=$(YSYXSOC_FLASH_SIZE)
LDFLAGS   += --defsym=_sram_start=$(YSYXSOC_SRAM_BASE)
LDFLAGS   += --defsym=_sram_size=$(YSYXSOC_SRAM_SIZE)
LDFLAGS   += --defsym=_sdram_start=$(YSYXSOC_SDRAM_BASE)
LDFLAGS   += --defsym=_sdram_size=$(YSYXSOC_SDRAM_SIZE)
LDFLAGS   += --defsym=_pmem_start=$(YSYXSOC_SDRAM_BASE) --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start

MAINARGS_MAX_LEN = 64
MAINARGS_PLACEHOLDER = The insert-arg rule in Makefile will insert mainargs here.
CFLAGS += -DMAINARGS_MAX_LEN=$(MAINARGS_MAX_LEN) -DMAINARGS_PLACEHOLDER=\""$(MAINARGS_PLACEHOLDER)"\"

NPC_HOME=$(NEMU_HOME)/../npc
YSYXSOC_BIN = $(NPC_HOME)/npc

CARGS ?=
ifeq ($(BOARD),1)
CARGS += "BOARD=1"
endif

ysyxsoc-bootloader:
	$(MAKE) -C $(NPC_HOME) SIM_TARGET=ysyxsoc-bootloader $(CARGS)

ARGS =

ifeq ($(BATCH),1)
ARGS += -b
endif

ifeq ($(LOG),1)
ARGS += -l log.txt
endif

insert-arg: image
	@python3 $(AM_HOME)/tools/insert-arg.py $(IMAGE).bin $(MAINARGS_MAX_LEN) "$(MAINARGS_PLACEHOLDER)" "$(mainargs)"

image: image-dep
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

run: insert-arg ysyxsoc-bootloader
	@echo + $(YSYXSOC_BIN) $(ARGS) $(IMAGE)
	$(YSYXSOC_BIN) $(ARGS) $(IMAGE).bin

gdb: insert-arg ysyxsoc-bootloader
	gdb --args $(YSYXSOC_BIN) $(ARGS) $(IMAGE).bin

.PHONY: insert-arg ysyxsoc-bootloader
