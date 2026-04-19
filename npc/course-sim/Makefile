SHELL := /bin/bash

export NPC_HOME ?= $(shell realpath ..)
export NEMU_HOME ?= $(shell realpath $(NPC_HOME)/../nemu)

TARGET := npc-course
TOP_MODULE := CourseTop

-include $(NPC_HOME)/.config

VSRC_DIR := $(NPC_HOME)/src/main/verilog/Course
CSRC_DIR := $(NPC_HOME)/csrc
LOCAL_CSRC_DIR := $(CURDIR)
BUILD_DIR := $(CURDIR)/build
OBJ_DIR := $(BUILD_DIR)/obj_dir
TOOLS_DIR := $(CSRC_DIR)/tools

VSRC := $(shell if [ -d "$(VSRC_DIR)" ]; then find "$(VSRC_DIR)" \( -path "$(VSRC_DIR)/verification" -o -path "$(VSRC_DIR)/verification/*" \) -prune -o \( -name "*.v" -o -name "*.sv" \) -print; fi)

CXX_SRCS_BASE := $(shell find $(CSRC_DIR) -name "*.cpp")
CXX_SRCS_BASE := $(filter-out $(CSRC_DIR)/main.cpp $(CSRC_DIR)/dpi/dpi_tracer_smoke_main.cpp, $(CXX_SRCS_BASE))
CXX_SRCS := $(CXX_SRCS_BASE) $(LOCAL_CSRC_DIR)/main.cpp
C_SRCS_BASE := $(shell find $(CSRC_DIR) -name "*.c")
C_SRCS_BASE := $(filter-out %/disasm.c, $(C_SRCS_BASE))
C_SRCS := $(C_SRCS_BASE)

VERILATOR_INC_DIRS := /usr/share/verilator/include \
                      /usr/share/verilator/include/vltstd
INC_DIRS := $(OBJ_DIR) $(CSRC_DIR) $(CSRC_DIR)/tools $(CSRC_DIR)/sdb $(CSRC_DIR)/difftest \
            $(CSRC_DIR)/trace $(CSRC_DIR)/log $(VERILATOR_INC_DIRS)
INC_FLAGS := $(addprefix -I, $(INC_DIRS))

COMMON_FLAGS := -g -Wall -pthread
CC := gcc
CXX := g++
CFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS := $(INC_FLAGS) $(COMMON_FLAGS) -std=c++17 -Wno-deprecated-declarations
COURSE_CPPFLAGS := -DIMAGE_BASE_ADDR=0x80000000u
CFLAGS += $(COURSE_CPPFLAGS)
CXXFLAGS += $(COURSE_CPPFLAGS)
LDFLAGS := -lreadline -pthread -ldl

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

.PHONY: all scala run rundiff clean check-img force-rebuild-diff

all: $(TARGET)

scala:
	cd $(NPC_HOME) && sbt --batch 'runMain labcpu.top.GenCourseTop'

$(VERILATED_LIB): scala $(VSRC) | $(BUILD_DIR)
	@echo "### Running Verilator for CourseTop... ###"
	verilator --cc --sv --timescale "1ns/1ns" --autoflush --no-timing --top-module $(TOP_MODULE) \
		+incdir+$(VSRC_DIR) \
		-Mdir $(OBJ_DIR) -Wno-WIDTHEXPAND $(VSRC)
	@echo "### Compiling Verilated course library... ###"
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
		$(firstword $(VERILATOR_INC_DIRS))/verilated.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_dpi.cpp \
		$(firstword $(VERILATOR_INC_DIRS))/verilated_threads.cpp \
		$(LDFLAGS)

run: $(TARGET)
	@if [ -z "$(IMG)" ]; then echo "Please provide IMG=/path/to/program.bin"; exit 1; fi
	./$(TARGET) -l log.txt $(IMG)

force-rebuild-diff:
	rm -f $(TARGET) $(ALL_OBJS)

rundiff: CFLAGS += -DCONFIG_DIFFTEST
rundiff: CXXFLAGS += -DCONFIG_DIFFTEST
rundiff: force-rebuild-diff $(TARGET)
	@if [ -z "$(IMG)" ]; then echo "Please provide IMG=/path/to/program.bin"; exit 1; fi
	./$(TARGET) --diff=$(NEMU_HOME)/build/riscv32-nemu-interpreter-so -l log.txt $(IMG)

clean:
	rm -rf $(BUILD_DIR) $(TARGET)
