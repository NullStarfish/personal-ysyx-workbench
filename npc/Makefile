SHELL := /bin/bash
.DEFAULT_GOAL := all

# Project locations and build selection.
export NPC_HOME ?= $(abspath $(CURDIR))
export NEMU_HOME ?= $(abspath $(NPC_HOME)/../nemu)
export SOC_HOME ?= $(abspath $(NPC_HOME)/../ysyxSoC)
export NVBOARD_HOME ?= $(abspath $(NPC_HOME)/../../nvboard)

TARGET := $(NPC_HOME)/npc
BUILD_DIR := $(NPC_HOME)/build
CSRC_DIR := $(NPC_HOME)/csrc
CACHESIM_DIR := $(NPC_HOME)/cachesim
TOOLS_DIR := $(CSRC_DIR)/tools

SIM_TARGET ?= npc
BOARD ?= 0
DEBUG ?= 1
IMG ?= ./dummy-riscv32e-ysyxsoc.bin
SIM_BUILD_TAG := $(SIM_TARGET)$(if $(filter 1,$(BOARD)),-board,)
SIM_BUILD_DIR := $(BUILD_DIR)/sim/$(SIM_BUILD_TAG)
SIM_EXEC := $(SIM_BUILD_DIR)/bin/npc
VERILATOR_OBJ_DIR := $(SIM_BUILD_DIR)/obj_dir

# Components publish their linker inputs through these variables.
CSRC_LIBS :=
CACHESIM_LIBS :=
NVBOARD_LIBS :=
VERILATOR_LIBS :=

ifeq ($(SIM_TARGET),npc)
TOP_MODULE := NpcTop
else ifneq ($(filter ysyxsoc ysyxsoc-bootloader,$(SIM_TARGET)),)
TOP_MODULE := ysyxSoCFull
else
$(error Unsupported SIM_TARGET '$(SIM_TARGET)'; use npc, ysyxsoc, or ysyxsoc-bootloader)
endif

-include .config
-include ../Makefile

include $(NPC_HOME)/mk/config.mk
include $(NPC_HOME)/mk/toolchain.mk
include $(NPC_HOME)/mk/chisel.mk
include $(NPC_HOME)/mk/verilog.mk
include $(NPC_HOME)/csrc/sources.mk
include $(NPC_HOME)/cachesim/sources.mk
include $(NPC_HOME)/mk/nvboard.mk

.PHONY: all npc menuconfig run rundiff gdb-diff clean compdb perf

all: npc
npc: $(SIM_EXEC)
	$(call git_commit, "sim RTL")
	@ln -sfn $(SIM_EXEC) $(TARGET)

# Compatibility entry points for explicit Chisel RTL generation.
scala local_scala: $(CHISEL_RTL_STAMP)

$(SIM_EXEC): $(CSRC_LIBS) $(CACHESIM_LIBS) $(NVBOARD_LIBS) $(VERILATOR_LIBS) $(BUILD_MODE_FILE) | check_config
	$(call build_banner,Linking executable: $@)
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -o $@ $(CSRC_LIBS) $(CACHESIM_LIBS) $(NVBOARD_LIBS) $(VERILATOR_LIBS) $(LDLIBS)

$(BUILD_DIR):
	@mkdir -p $@

$(SIM_BUILD_DIR):
	@mkdir -p $@

menuconfig:
	@echo "### Launching Kconfig menu... ###"
	kconfig-mconf Kconfig
	@echo "### Configuration saved to .config ###"

# Generate the Verilated model first, outside Bear, then capture only the
# simulation runtime compilation database.
compdb: $(VERILATOR_LIBS)
	$(MAKE) --no-print-directory compdb-clean SIM_TARGET=$(SIM_TARGET) BOARD=$(BOARD)
	bear --output $(NPC_HOME)/compile_commands.json -- $(MAKE) --no-print-directory npc SIM_TARGET=$(SIM_TARGET) BOARD=$(BOARD)

.PHONY: compdb-clean
compdb-clean::
	$(RM) $(SIM_EXEC) $(TARGET)

run: npc
	@echo "### Starting NPC Simulation (config from menuconfig) ###"
	./npc -l log.txt $(IMG)

rundiff: CFLAGS += -DCONFIG_DIFFTEST
rundiff: CXXFLAGS += -DCONFIG_DIFFTEST
rundiff: npc
	@echo "### Starting NPC Simulation (WITH difftest) ###"
	./npc --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

gdb-diff: CFLAGS += -DCONFIG_DIFFTEST
gdb-diff: CXXFLAGS += -DCONFIG_DIFFTEST
gdb-diff: npc
	@echo "### Starting NPC Simulation with GDB (WITH difftest) ###"
	gdb --args ./npc --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR) $(TARGET)

perf:
	./yosys-sta.sh
	$(MAKE) -C $(NPC_HOME)/../am-kernels/benchmarks/microbench/ ARCH=riscv32e-ysyxsoc ARGS=-b run mainargs=train
