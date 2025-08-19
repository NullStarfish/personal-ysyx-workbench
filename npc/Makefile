# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)

TARGET := npc
TOP_MODULE := Top
IMG ?= ./dummy-riscv32e-npc.bin

# --- Feature Flags ---
CONFIG_ITRACE ?= y
CONFIG_FTRACE ?= y
CONFIG_DIFFTEST ?=

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := $(NPC_HOME)/vsrc
CSRC_DIR := $(NPC_HOME)/csrc
BUILD_DIR := $(NPC_HOME)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(NPC_HOME)/csrc/tools

VSRC := $(shell find $(VSRC_DIR) -name "*.v")
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
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17
LDFLAGS := -lreadline -pthread -ldl

# --- Conditional Compilation Logic ---
ifeq ($(CONFIG_DIFFTEST),y)
  CFLAGS += -DDIFFTEST_ON
endif

ifeq ($(CONFIG_FTRACE),y)
  LDFLAGS += -lelf
endif

ifeq ($(CONFIG_ITRACE),y)
  #ifneq ($(CONFIG_DIFFTEST),y)
    include $(TOOLS_DIR)/disasm.mk
  #else
    #//C_SRCS := $(filter-out %/tools/disasm.c, $(C_SRCS))
  #endif
endif

# Map sources to objects AFTER conditional filtering
CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp, $(BUILD_DIR)/%.o, $(CXX_SRCS))
C_OBJS := $(patsubst $(CSRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(C_SRCS))
ALL_OBJS := $(CXX_OBJS) $(C_OBJS)
VERILATED_LIB := $(BUILD_DIR)/obj_dir/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all run run-diff gdb-diff clean

all: $(TARGET)

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB)
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $^ \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	verilator --cc --top-module $(TOP_MODULE) $(addprefix +incdir+, $(VSRC_DIR)) \
		-Mdir $(OBJ_DIR) $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(BUILD_DIR):
	@mkdir -p $@

# --- Execution Targets ---
# Target to run WITHOUT difftest
run:
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=
	@echo "### Starting NPC Simulation (no difftest) ###"
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/capstone/repo ./$(TARGET) $(IMG)

# Target to run WITH difftest
run-diff:
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation (WITH difftest) ###"
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/capstone/repo ./$(TARGET) --diff=build/riscv32-nemu-interpreter-so $(IMG)

# Target to debug WITH difftest
gdb-diff:
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation with GDB (WITH difftest) ###"
	gdb --args ./$(TARGET) --diff=build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR)
