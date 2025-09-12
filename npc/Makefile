# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)

TARGET := npc
TOP_MODULE := Top
IMG ?= ./load-store-riscv32e-npc.bin

# --- Feature Flags ---
CONFIG_ITRACE ?= y
CONFIG_FTRACE ?= y
CONFIG_DIFFTEST ?=

-include ../Makefile
# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := $(NPC_HOME)/vsrc
CSRC_DIR := $(NPC_HOME)/csrc
BUILD_DIR := $(NPC_HOME)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(NPC_HOME)/csrc/tools

# MODIFIED: Find both .v and .sv files, ensuring package is first
VSRC_PKG := $(shell find $(VSRC_DIR)/include -name "*pkg.sv")
VSRC_OPCODES := $(shell find $(VSRC_DIR)/include -name "*opcodes.sv")
VSRC_OTHERS := $(filter-out $(VSRC_PKG) $(VSRC_OPCODES), $(shell find $(VSRC_DIR) -name "*.v" -o -name "*.sv"))
VSRC := $(VSRC_PKG) $(VSRC_OTHERS) $(VSRC_OPCODES)

CXX_SRCS := $(shell find $(CSRC_DIR) -name "*.cpp")
C_SRCS := $(shell find $(CSRC_DIR) -name "*.c")

# =============================================================================
# Compilation and Linking Flags
# =============================================================================
INC_DIRS := $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
            $(CSRC_DIR)/trace $(CSRC_DIR)/log $(BUILD_DIR)/obj_dir \
            /usr/local/share/verilator/include /usr/local/share/verilator/include/vltstd
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

COMMON_FLAGS := -g -Wall -pthread
CC := gcc
CXX := g++
CFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L

# MODIFIED: Use `verilator --cflags` to get robust include paths
VERILATOR_CFLAGS := $(shell verilator --cflags)
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) $(VERILATOR_CFLAGS) -std=c++17 -Wno-deprecated-declarations

LDFLAGS := -lreadline -pthread -ldl

# --- Conditional Compilation Logic ---
ifeq ($(CONFIG_DIFFTEST),y)
  CFLAGS += -DDIFFTEST_ON
  CXXFLAGS += -DDIFFTEST_ON
endif

ifeq ($(CONFIG_FTRACE),y)
  LDFLAGS += -lelf
endif

# --- Restored logic for Capstone disassembler ---
ifeq ($(CONFIG_ITRACE),y)
  include $(TOOLS_DIR)/disasm.mk
endif

# Map sources to objects AFTER conditional filtering
CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp, $(BUILD_DIR)/%.o, $(CXX_SRCS))
C_OBJS := $(patsubst $(CSRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(C_SRCS))
ALL_OBJS := $(CXX_OBJS) $(C_OBJS)
VERILATED_LIB := $(BUILD_DIR)/obj_dir/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all run rundiff gdb-diff clean

# MODIFIED: Updated target name to prevent conflicts with parent Makefile
npc: $(TARGET)

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB)
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $^ \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	verilator --cc --sv --top-module $(TOP_MODULE) +incdir+$(VSRC_DIR)/include \
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

# --- Execution Targets ---
run:
	$(call git_commit, "sim RTL")
	$(MAKE) npc CONFIG_DIFFTEST=
	@echo "### Starting NPC Simulation (no difftest) ###"
	./$(TARGET) -l log.txt $(IMG)

rundiff:
	$(call git_commit, "sim RTL")
	$(MAKE) npc CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation (WITH difftest) ###"
	./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

gdb-diff:
	$(call git_commit, "sim RTL")
	$(MAKE) npc CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation with GDB (WITH difftest) ###"
	gdb --args ./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR) $(TARGET)

