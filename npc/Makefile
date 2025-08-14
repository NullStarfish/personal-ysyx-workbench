# =============================================================================
# Project-specific settings
# =============================================================================
# Set the shell to bash for robust scripting.
SHELL := /bin/bash
# The name of the final executable.
TARGET := npc
# The top-level Verilog module for Verilator.
TOP_MODULE := Top
# Default image file, can be overridden from the command line.
# Example: make run IMG=path/to/my.bin
IMG ?= ./dummy-riscv32e-npc.bin
# Allows passing extra arguments to the simulation.
# Example for batch mode: make run ARGS=-b
ARGS ?=

# =============================================================================
# Directories and Source Files
# =============================================================================
VSRC_DIR := ./vsrc
CSRC_DIR := ./csrc
BUILD_DIR := ./build
OBJ_DIR := $(BUILD_DIR)/obj_dir

# VPATH tells 'make' where to find source files.
# This path includes the root C source directory and the sdb subdirectory.
VPATH := $(CSRC_DIR):$(CSRC_DIR)/sdb

# Find all Verilog and C++ source files automatically.
VSRC := $(shell find $(VSRC_DIR) -name "*.v")
CSRC := $(shell find $(CSRC_DIR) -name "*.cpp")

# Create a list of all C++ object file names to be placed in the build directory.
# $(notdir ...) extracts just the filename (e.g., main.cpp from csrc/main.cpp).
# The result is a list like: build/main.o build/monitor.o build/sdb.o ...
C_OBJS := $(patsubst %.cpp,$(BUILD_DIR)/%.o,$(notdir $(CSRC)))

# Define the path to the static library that Verilator will compile.
VERILATED_LIB := $(OBJ_DIR)/V$(TOP_MODULE)__ALL.a

# =============================================================================
# Toolchain and Flags
# =============================================================================
VERILATOR := verilator
CXX := g++

# Flags for Verilator.
VERILATOR_FLAGS := \
	--cc \
	--top-module $(TOP_MODULE) \
	+incdir+$(VSRC_DIR) \
	-Mdir $(OBJ_DIR)

# Flags for C++ compilation.
# -I$(CSRC_DIR) allows includes like #include "sdb/sdb.h" to work correctly.
# -pthread is required for Verilator's multithreading support.
CXXFLAGS := \
    -I$(CSRC_DIR) \
    -I$(OBJ_DIR) \
    -I/usr/local/share/verilator/include \
    -I/usr/local/share/verilator/include/vltstd \
    -std=c++17 -g -Wall -pthread

# Flags for the final linking stage.
# -lreadline enables the readline library for the interactive debugger.
# -pthread is required for linking Verilator's threading support.
LDFLAGS := -lreadline -pthread

# =============================================================================
# Makefile Rules
# =============================================================================
.PHONY: all sim run clean

# Default target
all: $(TARGET)

# --- Final Executable Linking Rule ---
# This rule links our C++ object files with the Verilator-generated library
# and the necessary Verilator runtime source files.
$(TARGET): $(C_OBJS) $(VERILATED_LIB)
	@echo "### Linking executable: $(TARGET) ###"
	$(CXX) $(CXXFLAGS) -o $@ $(C_OBJS) $(VERILATED_LIB) \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

# --- C++ Object File Compilation Rule ---
# This generic rule compiles our C++ source files into object files.
# It depends on the Verilator library to ensure Verilator-generated headers are
# available before our code is compiled.
$(BUILD_DIR)/%.o: %.cpp $(VERILATED_LIB) | $(BUILD_DIR)
	@echo "### Compiling C++: $< ###"
	$(CXX) $(CXXFLAGS) -c -o $@ $<

# --- Verilated Library Generation Rule ---
# This rule creates the Verilator library. It first runs Verilator to generate
# the C++ source code from your Verilog files, and then it runs 'make' on the
# Makefile that Verilator generated to compile the library.
$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator to generate C++... ###"
	$(VERILATOR) $(VERILATOR_FLAGS) $(VSRC)
	@echo "### Compiling Verilated library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

# --- Directory Creation Rule ---
# This ensures the build directory exists before we try to put files in it.
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

# --- Simulation Run Rule ---
# The run rule now uses the ARGS variable to pass command-line options
# to the simulator executable.
sim: run
run: all
	@echo "### Starting NPC Simulation ###"
	@if [ -f "$(TARGET)" ]; then \
		./$(TARGET) $(ARGS) $(IMG); \
	else \
		echo "Error: Executable '$(TARGET)' not found. Please run 'make' first."; \
		exit 1; \
	fi

# --- Cleanup Rule ---
clean:
	@echo "### Cleaning up generated files... ###"
	@rm -rf $(BUILD_DIR) $(TARGET) *.vcd
