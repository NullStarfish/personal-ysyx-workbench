# C/C++ compilation and link settings for the simulation executable.

CC ?= gcc
CXX ?= g++
AR ?= ar
OSS_CAD_VERILATOR := $(HOME)/oss-cad-suite/bin/verilator
VERILATOR ?= $(if $(wildcard $(OSS_CAD_VERILATOR)),$(OSS_CAD_VERILATOR),verilator)

VERILATOR_ROOT := $(shell $(VERILATOR) -V 2>/dev/null | sed -n 's/^[[:space:]]*VERILATOR_ROOT[[:space:]]*=[[:space:]]*//p' | tail -n 1)
VERILATOR_INC_DIRS := $(VERILATOR_ROOT)/include $(VERILATOR_ROOT)/include/vltstd

define build_banner
	@printf '\033[31m===================================\n### %s ###\n===================================\033[0m\n' "$(1)"
endef

INCLUDE_DIRS = \
	$(VERILATOR_OBJ_DIR) \
	$(CSRC_INCLUDE_DIRS) \
	$(CACHESIM_INCLUDE_DIRS) \
	$(NVBOARD_INCLUDE_DIRS) \
	$(VERILATOR_INC_DIRS)

CPPFLAGS = $(addprefix -I,$(INCLUDE_DIRS)) $(CONFIG_CPPFLAGS) $(NVBOARD_CPPFLAGS)
COMMON_FLAGS = -g -Wall -pthread
CFLAGS = $(CPPFLAGS) $(COMMON_FLAGS) -std=c11 -D_POSIX_C_SOURCE=200809L
CXXFLAGS = $(CPPFLAGS) $(COMMON_FLAGS) -std=c++17 -Wno-deprecated-declarations
LDLIBS = -lreadline -pthread -ldl $(NVBOARD_LDLIBS)

ifeq ($(strip $(CONFIG_FTRACE)),y)
LDLIBS += -lelf
endif

ifeq ($(strip $(CONFIG_ITRACE)),y)
CAPSTONE_HOME := $(NEMU_HOME)/tools/capstone
CAPSTONE_REPO := $(CAPSTONE_HOME)/repo
LIBCAPSTONE := $(CAPSTONE_REPO)/libcapstone.so.5
CFLAGS += -I$(CAPSTONE_REPO)/include
LDLIBS += -L$(CAPSTONE_REPO) -lcapstone -Wl,-rpath=$(CAPSTONE_REPO)

$(LIBCAPSTONE):
	@echo "### Building Capstone from NEMU directory: $(CAPSTONE_HOME) ###"
	$(MAKE) -C $(CAPSTONE_HOME)
endif
