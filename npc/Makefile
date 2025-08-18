# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)
# Add NEMU_HOME for convenience, assuming it's a sibling directory
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)

TARGET := npc
TOP_MODULE := Top
IMG ?= ./dummy-riscv32e-npc.bin

# --- Set default arguments for 'make run' ---
# Default arguments now point to the NEMU difftest shared object.
ARGS ?= --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so

# --- Feature Flags (can be overridden from command line, e.g., make ITRACE=y) ---
CONFIG_ITRACE ?= y
CONFIG_FTRACE ?= y
CONFIG_DIFFTEST ?= y

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := $(NPC_HOME)/vsrc
CSRC_DIR := $(NPC_HOME)/csrc
BUILD_DIR := $(NPC_HOME)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(NPC_HOME)/csrc/tools

# --- Discover Source Files ---
VSRC := $(shell find $(VSRC_DIR) -name "*.v")
CXX_SRCS := $(shell find $(CSRC_DIR) -name "*.cpp")
C_SRCS := $(shell find $(CSRC_DIR) -name "*.c")

# Conditionally exclude files if itrace is disabled
ifeq ($(CONFIG_ITRACE),)
C_SRCS := $(filter-out %/tools/disasm.c %/trace/itrace.c, $(C_SRCS))
endif

# --- Map source files to object files ---
CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp, $(BUILD_DIR)/%.o, $(CXX_SRCS))
C_OBJS := $(patsubst $(CSRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(C_SRCS))
ALL_OBJS := $(CXX_OBJS) $(C_OBJS)
VERILATED_LIB := $(BUILD_DIR)/obj_dir/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Compilation and Linking Flags
# =============================================================================
# --- Include Paths ---
INC_DIRS := $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
            $(CSRC_DIR)/trace $(CSRC_DIR)/log $(BUILD_DIR)/obj_dir \
            /usr/local/share/verilator/include /usr/local/share/verilator/include/vltstd
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

# --- Common Flags ---
COMMON_FLAGS := -g -Wall -pthread

# --- C and C++ Specific Flags ---
CC := gcc
CXX := g++
CFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17

# --- Linker Flags ---
LDFLAGS := -lreadline -pthread -ldl

ifeq ($(CONFIG_FTRACE),y)
LDFLAGS += -lelf
endif
ifeq ($(CONFIG_ITRACE),y)
include $(TOOLS_DIR)/disasm.mk
endif

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all sim run gdb clean

all: $(TARGET)

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB)
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $^ \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

# --- Compilation Rules ---
$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

# --- Verilation Rule ---
$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	verilator --cc --top-module $(TOP_MODULE) $(addprefix +incdir+, $(VSRC_DIR)) \
		-Mdir $(OBJ_DIR) $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR):
	@mkdir -p $@

# --- Run and Debug Rules ---
run: $(TARGET)
	@echo "### Starting NPC Simulation ###"
	@echo -n "Git commit: " && git log -1 --format="%H"
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/capstone/repo ./$(TARGET) $(ARGS) $(IMG)

gdb: $(TARGET)
	@echo "### Starting NPC Simulation with GDB ###"
	@echo "Hint: In GDB, type 'run' to start, and 'bt' after it crashes."
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/capstone/repo gdb --args ./$(TARGET) $(ARGS) $(IMG)

# --- Clean Rule ---
clean:
	rm -rf $(BUILD_DIR)

