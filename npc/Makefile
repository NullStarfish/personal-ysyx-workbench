# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)

TARGET := npc
TOP_MODULE := Top
IMG ?= ./load-store-riscv32e-npc.bin

# Include Kconfig generated configuration and parent Makefile if they exist.
-include .config
-include ../Makefile

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := $(NPC_HOME)/vsrc
CSRC_DIR := $(NPC_HOME)/csrc
BUILD_DIR := $(NPC_HOME)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(NPC_HOME)/csrc/tools

# --- Verilog/SystemVerilog Sources ---
VSRC_PKG := $(shell find $(VSRC_DIR)/include -name "*pkg.sv")
VSRC_OPCODES := $(shell find $(VSRC_DIR)/include -name "*opcodes.sv")
VSRC_OTHERS := $(filter-out $(VSRC_PKG) $(VSRC_OPCODES), $(shell find $(VSRC_DIR) -name "*.v" -o -name "*.sv"))
VSRC := $(VSRC_PKG) $(VSRC_OTHERS) $(VSRC_OPCODES)

# --- C/C++ Sources ---
# Start with base lists of all source files found.
CXX_SRCS_BASE := $(shell find $(CSRC_DIR) -name "*.cpp")
C_SRCS_BASE   := $(shell find $(CSRC_DIR) -name "*.c")

# Explicitly remove conditionally compiled files from the base list.
C_SRCS_BASE := $(filter-out %/disasm.c, $(C_SRCS_BASE))

# Initialize final source lists. They will be appended to based on config.
CXX_SRCS := $(CXX_SRCS_BASE)
C_SRCS   := $(C_SRCS_BASE)

# =============================================================================
# Compilation and Linking Flags
# =============================================================================
# --- Include Directories ---
# Project's own include paths
INC_DIRS_BASE := $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
                 $(CSRC_DIR)/trace $(CSRC_DIR)/log

# Manually specified paths for Verilator (replaces `verilator --cflags`)
# CRITICAL FIX: Add the `vltstd` subdirectory, which often contains svdpi.h or other standard headers.
VERILATOR_INC_DIRS := /usr/local/share/verilator/include \
                      /usr/local/share/verilator/include/vltstd
VERILATOR_OBJ_DIR  := $(BUILD_DIR)/obj_dir

# Combine all include directories into the final flag list
INC_DIRS  := $(INC_DIRS_BASE) $(VERILATOR_INC_DIRS) $(VERILATOR_OBJ_DIR)
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

# --- Compiler Flags ---
COMMON_FLAGS := -g -Wall -pthread
CC := gcc
CXX := g++

# Define CFLAGS and CXXFLAGS using the combined include flags.
CFLAGS   := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17 -Wno-deprecated-declarations

# --- Linker Flags ---
LDFLAGS := -lreadline -pthread -ldl

# --- Kconfig Conditional Flags ---
# Use `strip` to be robust against whitespace in .config file.
# Append flags using `+=`.
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
.PHONY: all npc run rundiff gdb-diff clean menuconfig check_config

# --- Core Dependencies ---
# 1. Any change in configuration requires recompiling all C/C++ files.
$(ALL_OBJS): .config

# 2. All C++ files depend on Verilator's output.
$(CXX_OBJS): $(VERILATED_LIB)

# --- Main Targets ---
all: npc
npc: $(TARGET)

# 3. A phony target to check for .config's existence before building.
check_config:
	@if [ ! -f .config ]; then \
		echo "ERROR: Configuration file .config is missing."; \
		echo "Please run 'make menuconfig' first."; \
		exit 1; \
	fi

# --- Build Rules ---
# The final executable depends on all objects and the verilated library.
# It uses an order-only prerequisite `| check_config` to run the check first.
$(TARGET): $(ALL_OBJS) $(VERILATED_LIB) | check_config
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $(ALL_OBJS) $(VERILATED_LIB) \
		$(firstword $(VERILATOR_INC_DIRS))/verilated.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_dpi.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_threads.cpp \
		$(LDFLAGS)


		
# Rule to run Verilator and build its output library.
$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	verilator --cc --sv --top-module $(TOP_MODULE) +incdir+$(VSRC_DIR)/include \
		-Mdir $(OBJ_DIR) -Wno-WIDTHEXPAND $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

# Generic rule to compile a C file.
$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

# Generic rule to compile a C++ file.
$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

# Rule to create the build directory.
$(BUILD_DIR):
	@mkdir -p $@

# =============================================================================
# User-facing Targets
# =============================================================================
menuconfig:
	@echo "### Launching Kconfig menu... ###"
	kconfig-mconf Kconfig
	@echo "### Configuration saved to .config ###"

run:
	$(call git_commit, "sim RTL")
	$(MAKE) npc
	@echo "### Starting NPC Simulation (config from menuconfig) ###"
	./$(TARGET) $(IMG)

rundiff:
	$(call git_commit, "sim RTL")
	# Override .config for this target by passing flags on the command line.
	$(MAKE) npc 'CFLAGS += -DCONFIG_DIFFTEST' 'CXXFLAGS += -DCONFIG_DIFFTEST'
	@echo "### Starting NPC Simulation (WITH difftest) ###"
	./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

gdb-diff:
	$(call git_commit, "sim RTL")
	$(MAKE) npc 'CFLAGS += -DCONFIG_DIFFTEST' 'CXXFLAGS += -DCONFIG_DIFFTEST'
	@echo "### Starting NPC Simulation with GDB (WITH difftest) ###"
	gdb --args ./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR) $(TARGET) .config .config.old