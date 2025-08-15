# =============================================================================
# Project-specific settings
# =============================================================================
SHELL := /bin/bash
TARGET := npc
TOP_MODULE := Top
IMG ?= ./dummy-riscv32e-npc.bin
ARGS ?=

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := ./vsrc
CSRC_DIR := ./csrc
BUILD_DIR := ./build
OBJ_DIR := $(BUILD_DIR)/obj_dir

VPATH := $(shell find $(CSRC_DIR) -type d | tr '\n' ':')

VSRC := $(shell find $(VSRC_DIR) -name "*.v")
C_SRCS := $(shell find $(CSRC_DIR) -name "*.c")
CXX_SRCS := $(shell find $(CSRC_DIR) -name "*.cpp")

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

VERILATOR_FLAGS := --cc --top-module $(TOP_MODULE) +incdir+$(VSRC_DIR) -Mdir $(OBJ_DIR)

COMMON_FLAGS := \
    -I$(CSRC_DIR) \
    -I$(OBJ_DIR) \
    -I/usr/local/share/verilator/include \
    -I/usr/local/share/verilator/include/vltstd \
    -g -Wall -pthread

CXXFLAGS := $(COMMON_FLAGS) -std=c++17
# BUG FIX: 添加 -D_POSIX_C_SOURCE=200809L 来启用 strdup 等 POSIX 函数
CFLAGS := $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L

LDFLAGS := -lreadline -pthread

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

$(BUILD_DIR)/%.o: %.c $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C: $< ###"
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: %.cpp $(VERILATED_LIB) | $(BUILD_DIR)
	@mkdir -p $(@D)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator to generate C++... ###"
	$(VERILATOR) $(VERILATOR_FLAGS) $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

sim: run
run: all
	@echo "### Starting NPC Simulation ###"
	@if [ -f "$(TARGET)" ]; then \
		./$(TARGET) $(ARGS) $(IMG); \
	else \
		echo "Error: Executable '$(TARGET)' not found. Please run 'make' first."; \
		exit 1; \
	fi

clean:
	@echo "### Cleaning up generated files... ###"
	@rm -rf $(BUILD_DIR) $(TARGET) *.vcd
