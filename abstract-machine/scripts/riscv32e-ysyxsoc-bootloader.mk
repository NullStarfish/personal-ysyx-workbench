include $(AM_HOME)/scripts/isa/riscv.mk
include $(AM_HOME)/scripts/platform/ysyxsoc-bootloader.mk
COMMON_CFLAGS += -march=rv32e_zicsr -mabi=ilp32e
LDFLAGS       += -melf32lriscv --print-map

AM_SRCS += riscv/ysyxsoc/libgcc/div.S \
           riscv/ysyxsoc/libgcc/muldi3.S \
           riscv/ysyxsoc/libgcc/multi3.c \
           riscv/ysyxsoc/libgcc/ashldi3.c \
           riscv/ysyxsoc/libgcc/unused.c
