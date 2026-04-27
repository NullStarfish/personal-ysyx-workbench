SHELL := /bin/bash

# Standalone Verilator build for the submission package.
# This Makefile intentionally does not invoke Chisel/SBT. It builds directly
# from the generated SystemVerilog files shipped in ../generated-verilog.

SUBMISSION_ROOT := $(shell realpath ..)

TARGET := npc-course
TOP_MODULE := CourseTop

VSRC_DIR := $(SUBMISSION_ROOT)/generated-verilog/simulation-course-dpi
CSRC_DIR := $(CURDIR)/csrc
LOCAL_CSRC_DIR := $(CURDIR)
BUILD_DIR := $(CURDIR)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(CSRC_DIR)/tools

RELEASE_DIR := $(SUBMISSION_ROOT)/release
NEMU_HOME ?= $(RELEASE_DIR)
DIFF_SO ?= $(RELEASE_DIR)/ref/riscv32-nemu-interpreter-so
CAPSTONE_LIB_DIR ?= $(RELEASE_DIR)/tools/capstone/repo

VERILATOR_ROOT ?= $(shell verilator --getenv VERILATOR_ROOT 2>/dev/null || echo /usr/share/verilator)
VERILATOR_INC_DIRS := $(VERILATOR_ROOT)/include $(VERILATOR_ROOT)/include/vltstd

VSRC := $(shell find "$(VSRC_DIR)" \( -path "$(VSRC_DIR)/verification" -o -path "$(VSRC_DIR)/verification/*" \) -prune -o \( -name "*.v" -o -name "*.sv" \) -print | sort)

CXX_SRCS_BASE := $(shell find "$(CSRC_DIR)" -name "*.cpp")
CXX_SRCS_BASE := $(filter-out $(CSRC_DIR)/main.cpp $(CSRC_DIR)/dpi/dpi_tracer_smoke_main.cpp,$(CXX_SRCS_BASE))
CXX_SRCS := $(CXX_SRCS_BASE) $(LOCAL_CSRC_DIR)/main.cpp

C_SRCS_BASE := $(shell find "$(CSRC_DIR)" -name "*.c")
C_SRCS_BASE := $(filter-out %/disasm.c,$(C_SRCS_BASE))
C_SRCS := $(C_SRCS_BASE)

INC_DIRS := $(OBJ_DIR) $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
            $(CSRC_DIR)/trace $(CSRC_DIR)/log $(VERILATOR_INC_DIRS)
INC_FLAGS := $(addprefix -I,$(INC_DIRS))

COMMON_FLAGS := -g -Wall -pthread
CC := gcc
CXX := g++
CFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17 -Wno-deprecated-declarations
COURSE_CPPFLAGS := -DIMAGE_BASE_ADDR=0x80000000u
CFLAGS += $(COURSE_CPPFLAGS)
CXXFLAGS += $(COURSE_CPPFLAGS)
LDFLAGS := -lreadline -pthread -ldl

# Optional tracing features can be enabled from the command line:
#   make CONFIG_ITRACE=y
#   make CONFIG_FTRACE=y
#   make CONFIG_WATCHPOINT=y
#   make CONFIG_DIFFTEST=y
ifeq ($(strip $(CONFIG_TRACE)),y)
  CFLAGS += -DCONFIG_TRACE
  CXXFLAGS += -DCONFIG_TRACE
endif
ifeq ($(strip $(CONFIG_ITRACE)),y)
  CFLAGS += -DCONFIG_ITRACE
  CXXFLAGS += -DCONFIG_ITRACE
  include $(TOOLS_DIR)/disasm.mk
  C_SRCS += $(CSRC_DIR)/tools/disasm.c
endif
ifeq ($(strip $(CONFIG_FTRACE)),y)
  CFLAGS += -DCONFIG_FTRACE
  CXXFLAGS += -DCONFIG_FTRACE
  LDFLAGS += -lelf
endif
ifeq ($(strip $(CONFIG_WATCHPOINT)),y)
  CFLAGS += -DCONFIG_WATCHPOINT
  CXXFLAGS += -DCONFIG_WATCHPOINT
endif
ifeq ($(strip $(CONFIG_DIFFTEST)),y)
  CFLAGS += -DCONFIG_DIFFTEST
  CXXFLAGS += -DCONFIG_DIFFTEST
endif

CXX_OBJS := $(patsubst $(CSRC_DIR)/%.cpp,$(BUILD_DIR)/%.o,$(filter $(CSRC_DIR)/%.cpp,$(CXX_SRCS))) \
            $(patsubst $(LOCAL_CSRC_DIR)/%.cpp,$(BUILD_DIR)/local/%.o,$(filter $(LOCAL_CSRC_DIR)/%.cpp,$(CXX_SRCS)))
C_OBJS := $(patsubst $(CSRC_DIR)/%.c,$(BUILD_DIR)/%.o,$(C_SRCS))
ALL_OBJS := $(CXX_OBJS) $(C_OBJS)
VERILATED_LIB := $(OBJ_DIR)/V$(TOP_MODULE)__ALL.a

.PHONY: all run rundiff clean force-rebuild-diff check-img check-diff

all: $(TARGET)

$(VERILATED_LIB): $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator for $(TOP_MODULE) from generated RTL... ###"
	verilator --cc --sv --timescale "1ns/1ns" --autoflush --no-timing --top-module $(TOP_MODULE) \
		+incdir+$(VSRC_DIR) \
		-Mdir $(OBJ_DIR) -Wno-WIDTHEXPAND $(VSRC)
	@echo "### Compiling Verilated CourseCPU library... ###"
	$(MAKE) -C $(OBJ_DIR) -f V$(TOP_MODULE).mk

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.c | $(BUILD_DIR)
	@mkdir -p $(@D)
	$(CC) $(CFLAGS) -c -o $@ $<

$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp | $(BUILD_DIR)
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(BUILD_DIR)/local/%.o: $(LOCAL_CSRC_DIR)/%.cpp | $(BUILD_DIR)
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

$(BUILD_DIR)/local/main.o: $(VERILATED_LIB)

$(BUILD_DIR):
	@mkdir -p $@

$(TARGET): $(ALL_OBJS) $(VERILATED_LIB)
	$(CXX) $(CXXFLAGS) -o $@ $(ALL_OBJS) $(VERILATED_LIB) \
		$(VERILATOR_ROOT)/include/verilated.cpp \
		$(VERILATOR_ROOT)/include/verilated_dpi.cpp \
		$(VERILATOR_ROOT)/include/verilated_threads.cpp \
		$(LDFLAGS)

check-img:
	@if [ -z "$(IMG)" ]; then echo "Please provide IMG=/path/to/program.bin"; exit 1; fi
	@if [ ! -f "$(IMG)" ]; then echo "Image not found: $(IMG)"; exit 1; fi

check-diff:
	@if [ ! -f "$(DIFF_SO)" ]; then echo "Difftest reference not found: $(DIFF_SO)"; exit 1; fi
	@if [ ! -f "$(CAPSTONE_LIB_DIR)/libcapstone.so.5" ]; then echo "Capstone runtime not found: $(CAPSTONE_LIB_DIR)/libcapstone.so.5"; exit 1; fi

run: $(TARGET) check-img
	./$(TARGET) -b -l log.txt $(IMG)

force-rebuild-diff:
	rm -f $(TARGET) $(ALL_OBJS)

rundiff: check-img check-diff
	$(MAKE) force-rebuild-diff
	$(MAKE) CONFIG_DIFFTEST=y $(TARGET)
	env NEMU_HOME="$(NEMU_HOME)" \
	    LD_LIBRARY_PATH="$(CAPSTONE_LIB_DIR)$${LD_LIBRARY_PATH:+:$$LD_LIBRARY_PATH}" \
	    ./$(TARGET) -b --diff="$(DIFF_SO)" -l log.txt "$(IMG)"

clean:
	rm -rf $(BUILD_DIR) $(TARGET) log.txt
