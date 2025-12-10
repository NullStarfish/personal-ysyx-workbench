# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)
export SOC_HOME ?= $(shell realpath $(NPC_HOME)/../ysyxSoC)




TARGET := npc
TOP_MODULE := ysyxSoCFull
IMG ?= ./dummy-riscv32e-ysyxsoc.bin

# Include Kconfig generated configuration and parent Makefile if they exist.
-include .config
-include ../Makefile

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := $(NPC_HOME)/src/main/verilog
VPERIP_DIR := $(SOC_HOME)/perip
VSOC_DIR := $(SOC_HOME)/build

# [新增] 定义生成目录路径
GEN_DIR  := $(VSRC_DIR)/Core
CSRC_DIR := $(NPC_HOME)/csrc
BUILD_DIR := $(NPC_HOME)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(NPC_HOME)/csrc/tools

# --- Verilog/SystemVerilog Sources ---
DEBUG=1
# 定义一个存储 DEBUG 状态的文件
DEBUG_FLAG_FILE := $(BUILD_DIR)/.debug_flag

$(DEBUG_FLAG_FILE): FORCE
	@mkdir -p $(dir $@)
	@# 将当前的 DEBUG 变量写入临时比较
	@echo "$(DEBUG)" | cmp -s - $@ || echo "$(DEBUG)" > $@



# 4. Generated Verification Sources & Layers
#    (Must cover gen/verification, gen/verification/assert, etc.)
VSRC_VERI := $(shell find $(GEN_DIR)/verification -name "*.sv")

# 5. Main Source Files (Top.sv, generated modules, handwritten files)
#    We use find on VSRC_DIR but filter out what we already found to avoid duplicates.
#    Also explicitly look in GEN_DIR to ensure Top.sv is found.
VSRC_ALL_RAW := $(shell find $(VSRC_DIR) $(VPERIP_DIR) $(VSOC_DIR) -name "*.v" -o -name "*.sv")
VSRC_OTHERS  := $(filter-out  $(VSRC_VERI), $(VSRC_ALL_RAW))

# 6. Final Source List (Order matters: Packages -> Interfaces -> Others)
VSRC := $(VSRC_VERI) $(VSRC_OTHERS)

# --- Verilog Include Paths (Critical Fix) ---
# Add generated directories to include paths so Verilator can resolve `include "layers-..."`
VERILOG_INC_DIRS := $(VSRC_DIR)/include \
                    $(GEN_DIR) \
                    $(GEN_DIR)/verification \
                    $(GEN_DIR)/verification/assert \
                    $(GEN_DIR)/verification/assume \
                    $(GEN_DIR)/verification/cover \
					$(SOC_HOME)/perip/uart16550/rtl \
					$(SOC_HOME)/perip/spi/rtl

# Format them for Verilator (+incdir+path)
VERILATOR_INC_FLAGS := $(addprefix +incdir+, $(VERILOG_INC_DIRS))

# --- C/C++ Sources ---
CXX_SRCS_BASE := $(shell find $(CSRC_DIR) -name "*.cpp")
C_SRCS_BASE   := $(shell find $(CSRC_DIR) -name "*.c")
C_SRCS_BASE   := $(filter-out %/disasm.c, $(C_SRCS_BASE))

CXX_SRCS := $(CXX_SRCS_BASE)
C_SRCS   := $(C_SRCS_BASE)

# =============================================================================
# Compilation and Linking Flags
# =============================================================================
# --- Include Directories (C++) ---
INC_DIRS_BASE := $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
                 $(CSRC_DIR)/trace $(CSRC_DIR)/log

VERILATOR_INC_DIRS := /usr/local/share/verilator/include \
                      /usr/local/share/verilator/include/vltstd
VERILATOR_OBJ_DIR  := $(BUILD_DIR)/obj_dir

INC_DIRS  := $(VERILATOR_OBJ_DIR) $(INC_DIRS_BASE) $(VERILATOR_INC_DIRS)
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

# --- Compiler Flags ---
COMMON_FLAGS := -g -Wall -pthread
CC := gcc
CXX := g++

CFLAGS   := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17 -Wno-deprecated-declarations

# --- Linker Flags ---
LDFLAGS := -lreadline -pthread -ldl

# --- Kconfig Conditional Flags ---
ifeq ($(strip $(CONFIG_TRACE)),y)
    CFLAGS   += -DCONFIG_TRACE
    CXXFLAGS += -DCONFIG_TRACE
endif
ifeq ($(strip $(CONFIG_ITRACE)),y)
    CFLAGS   += -DCONFIG_ITRACE
    CXXFLAGS += -DCONFIG_ITRACE
    include $(TOOLS_DIR)/disasm.mk
    C_SRCS   += $(CSRC_DIR)/tools/disasm.c
endif
ifeq ($(strip $(CONFIG_FTRACE)),y)
    CFLAGS   += -DCONFIG_FTRACE
    CXXFLAGS += -DCONFIG_FTRACE
    LDFLAGS  += -lelf
endif
ifeq ($(strip $(CONFIG_MTRACE)),y)
    CFLAGS   += -DCONFIG_MTRACE
    CXXFLAGS += -DCONFIG_MTRACE
endif
ifeq ($(strip $(CONFIG_DTRACE)),y)
    CFLAGS   += -DCONFIG_DTRACE
    CXXFLAGS += -DCONFIG_DTRACE
endif
ifeq ($(strip $(CONFIG_DIFFTEST)),y)
    CFLAGS   += -DCONFIG_DIFFTEST
    CXXFLAGS += -DCONFIG_DIFFTEST
endif
ifeq ($(strip $(CONFIG_WATCHPOINT)),y)
    CFLAGS   += -DCONFIG_WATCHPOINT
    CXXFLAGS += -DCONFIG_WATCHPOINT
endif

# =============================================================================
# Object File Mapping
# =============================================================================
CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp, $(BUILD_DIR)/%.o, $(CXX_SRCS))
C_OBJS := $(patsubst $(CSRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(C_SRCS))
ALL_OBJS := $(CXX_OBJS) $(C_OBJS)
VERILATED_LIB := $(BUILD_DIR)/obj_dir/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all npc run rundiff gdb-diff clean menuconfig check_config FORCE

$(ALL_OBJS): .config
$(CXX_OBJS): $(VERILATED_LIB)

all: npc
npc: $(TARGET)

check_config:
	@if [ ! -f .config ]; then \
		echo "ERROR: Configuration file .config is missing."; \
		echo "Please run 'make menuconfig' first."; \
		exit 1; \
	fi

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB) | check_config
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $(ALL_OBJS) $(VERILATED_LIB) \
		$(firstword $(VERILATOR_INC_DIRS))/verilated.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_dpi.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_threads.cpp \
		$(LDFLAGS)

# [关键修改] Verilator 编译规则
# 添加了 $(VERILATOR_INC_FLAGS) 以包含 gen 和 verification 目录
$(VERILATED_LIB): $(DEBUG_FLAG_FILE) $(VSRC)  | $(BUILD_DIR) 
	@echo "### Running Verilator... ###"
	verilator --cc --sv --timescale "1ns/1ns" --no-timing -DPRINTF_COND=$(DEBUG) --top-module $(TOP_MODULE) \
		$(VERILATOR_INC_FLAGS) \
		-Mdir $(OBJ_DIR) -Wno-WIDTHEXPAND $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(BUILD_DIR):
	@mkdir -p $@

# =============================================================================
# User-facing Targets
# =============================================================================

rundiff: CFLAGS += -DCONFIG_DIFFTEST
rundiff: CXXFLAGS += -DCONFIG_DIFFTEST

gdb-diff: CFLAGS += -DCONFIG_DIFFTEST
gdb-diff: CXXFLAGS += -DCONFIG_DIFFTEST

menuconfig:
	@echo "### Launching Kconfig menu... ###"
	kconfig-mconf Kconfig
	@echo "### Configuration saved to .config ###"

run:
	$(call git_commit, "sim RTL")
	$(MAKE) npc
	@echo "### Starting NPC Simulation (config from menuconfig) ###"
	./$(TARGET) -l log.txt  $(IMG)

rundiff: npc
	$(call git_commit, "sim RTL")
	@echo "### Starting NPC Simulation (WITH difftest) ###"
	./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

gdb-diff: npc
	$(call git_commit, "sim RTL")
	@echo "### Starting NPC Simulation with GDB (WITH difftest) ###"
	gdb --args ./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR) $(TARGET) $(GEN_DIR)