# =============================================================================
# Project Configuration
# =============================================================================
SHELL := /bin/bash
export NPC_HOME ?= $(shell pwd)

TARGET := npc
TOP_MODULE := Top
IMG ?= ./dummy-riscv32e-npc.bin
ARGS ?= -l log.txt

CONFIG_ITRACE ?= y

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

ifeq ($(CONFIG_ITRACE),)
C_SRCS := $(filter-out %/disasm.c %/itrace.c, $(C_SRCS))
endif

C_OBJS   := $(patsubst $(CSRC_DIR)/%.c,$(BUILD_DIR)/%.o,$(C_SRCS))
CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp,$(BUILD_DIR)/%.o,$(CXX_SRCS))
ALL_OBJS := $(C_OBJS) $(CXX_OBJS)

VERILATED_LIB := $(OBJ_DIR)/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Toolchain and Flags
# =============================================================================
VERILATOR := verilator
CC := gcc
CXX := g++

# --- Automatic Include Paths ---
# Find all subdirectories in csrc and add them to the include path.
# This allows for includes like #include "log.h" instead of #include "log/log.h".
C_INCLUDE_PATHS := $(patsubst %,-I%,$(shell find $(CSRC_DIR) -type d))

VERILATOR_FLAGS := --cc --top-module $(TOP_MODULE) +incdir+$(VSRC_DIR) -Mdir $(OBJ_DIR)

COMMON_FLAGS := \
    $(C_INCLUDE_PATHS) \
    -I$(OBJ_DIR) \
    -I/usr/local/share/verilator/include \
    -I/usr/local/share/verilator/include/vltstd \
    -g -Wall -pthread

CXXFLAGS := $(COMMON_FLAGS) -std=c++17
CFLAGS := $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L

LDFLAGS := -lreadline -pthread -ldl

ifeq ($(CONFIG_ITRACE),y)
include $(TOOLS_DIR)/disasm.mk
endif

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all sim run clean

all: $(TARGET)

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB)
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $(ALL_OBJS) $(VERILATED_LIB) \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	$(VERILATOR) $(VERILATOR_FLAGS) $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

sim: run
run: all
	@echo "### Starting NPC Simulation ###"
	./$(TARGET) $(ARGS) $(IMG)

CLEAN_HOOKS ?=
clean: $(CLEAN_HOOKS)
	@echo "### Cleaning up project... ###"
	@rm -rf $(BUILD_DIR) $(TARGET) *.vcd
