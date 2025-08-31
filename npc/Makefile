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

# MODIFIED: Find both .v and .sv files, ensuring the package is listed first.
PKG_FILE := $(VSRC_DIR)/include/riscv_pkg.sv
OTHER_VSRCS := $(filter-out $(PKG_FILE), $(shell find $(VSRC_DIR) -name "*.v" -o -name "*.sv"))
VSRC := $(PKG_FILE) $(OTHER_VSRCS)

CXX_SRCS := $(shell find $(CSRC_DIR) -name "*.cpp")
C_SRCS := $(shell find $(CSRC_DIR) -name "*.c")

# =============================================================================
# Compilation and Linking Flags
# =============================================================================
INC_DIRS := $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
            $(CSRC_DIR)/trace $(CSRC_DIR)/log $(BUILD_DIR)/obj_dir \
            /usr/local/share/verilator/include /usr/local/share/verilator/include/vltstd
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

LDFLAGS := -L$(NEMU_HOME)/build/am-libs -L$(NEMU_HOME)/build -lriscv32-nemu-interpreter -ldl
CXXFLAGS_EXTRA := -Wno-deprecated-declarations
CFLAGS_EXTRA :=

VERILATOR_FLAGS := --cc --sv --top-module $(TOP_MODULE) \
                   +incdir+$(VSRC_DIR)/include \
                   -Mdir $(OBJ_DIR)

# MODIFIED: Add flag to suppress the benign WIDTHEXPAND warning.
VERILATOR_FLAGS += -Wno-WIDTHEXPAND

# =============================================================================
# Main Compilation Logic (Generally no need to edit below this line)
# =============================================================================
OBJS := $(patsubst $(CSRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(C_SRCS)) \
        $(patsubst $(CSRC_DIR)/%.cpp, $(BUILD_DIR)/%.o, $(CXX_SRCS))
VERILATED_LIB := $(OBJ_DIR)/libV$(TOP_MODULE).a

CFLAGS := -g -O2 -MMD -Wall $(CFLAGS_EXTRA) $(INC_FLAGS) -D__GUEST_ISA__=\"riscv32\"
CXXFLAGS := -g -O2 -MMD -Wall $(CXXFLAGS_EXTRA) $(INC_FLAGS) -D__GUEST_ISA__=\"riscv32\"

ifeq ($(CONFIG_ITRACE), y)
  CFLAGS += -DCONFIG_ITRACE
  CXXFLAGS += -DCONFIG_ITRACE
endif

ifeq ($(CONFIG_FTRACE), y)
  CFLAGS += -DCONFIG_FTRACE
  CXXFLAGS += -DCONFIG_FTRACE
endif

ifeq ($(CONFIG_DIFFTEST), y)
  CFLAGS += -DCONFIG_DIFFTEST
  CXXFLAGS += -DCONFIG_DIFFTEST
endif

all: $(TARGET)

$(TARGET): $(OBJS) $(VERILATED_LIB)
	@echo "### Linking: $@ ###"
	$(CXX) -o $@ $(OBJS) $(VERILATED_LIB) \
		/usr/local/share/verilator/include/verilated.cpp \
		/usr/local/share/verilator/include/verilated_dpi.cpp \
		/usr/local/share/verilator/include/verilated_threads.cpp \
		$(LDFLAGS)

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator... ###"
	verilator $(VERILATOR_FLAGS) $(VSRC)
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
	$(call git_commit, "sim RTL")
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=
	@echo "### Starting NPC Simulation (no difftest) ###"
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/my-difftest:$(NEMU_HOME)/build/am-libs:$(NEMU_HOME)/build:$$LD_LIBRARY_PATH \
	./$(TARGET) -i $(IMG)

# Target to run WITH difftest
rundiff:
	$(call git_commit, "sim RTL")
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation (with difftest) ###"
	LD_LIBRARY_PATH=$(NEMU_HOME)/tools/my-difftest:$(NEMU_HOME)/build/am-libs:$(NEMU_HOME)/build:$$LD_LIBRARY_PATH \
	./$(TARGET) -d -i $(IMG)

gdb:
	$(call git_commit, "sim RTL")
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=
	@echo "### Starting NPC Simulation with GDB ###"
	gdb -s ./$(TARGET) --args ./$(TARGET) -i $(IMG)

gdb-diff:
	$(call git_commit, "sim RTL")
	$(MAKE) $(TARGET) CONFIG_DIFFTEST=y
	@echo "### Starting NPC Simulation with GDB (and difftest) ###"
	gdb -s ./$(TARGET) --args ./$(TARGET) -d -i $(IMG)

# --- Cleanup ---
clean:
	rm -rf $(BUILD_DIR) $(TARGET)

.PHONY: all clean run rundiff gdb gdb-diff

-include $(OBJS:.o=.d)

