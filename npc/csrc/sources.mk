# Simulation runtime source ownership.

CSRC_INCLUDE_DIRS := \
	$(CSRC_DIR) \
	$(CSRC_DIR)/tools

CSRC_CPP_SRCS := \
	$(CSRC_DIR)/main.cpp \
	$(CSRC_DIR)/runtime/runtime.cpp \
	$(CSRC_DIR)/runtime/base/cpu.cpp \
	$(CSRC_DIR)/runtime/base/host_clock.cpp \
	$(CSRC_DIR)/runtime/base/run_control.cpp \
	$(CSRC_DIR)/runtime/platform/dut.cpp \
	$(CSRC_DIR)/runtime/platform/memory.cpp \
	$(CSRC_DIR)/runtime/platform/program_image.cpp \
	$(CSRC_DIR)/runtime/services/difftest.cpp \
	$(CSRC_DIR)/runtime/services/logger.cpp \
	$(CSRC_DIR)/runtime/services/ila_engine.cpp \
	$(CSRC_DIR)/runtime/services/sim_counter.cpp \
	$(CSRC_DIR)/runtime/execution/interrupt.cpp \
	$(CSRC_DIR)/runtime/execution/retire_pipeline.cpp \
	$(CSRC_DIR)/runtime/execution/simulation.cpp \
	$(CSRC_DIR)/runtime/dpi/dpi_bridge.cpp \
	$(CSRC_DIR)/runtime/sdb/sdb.cpp \
	$(CSRC_DIR)/runtime/sdb/watchpoint.cpp \
	$(CSRC_DIR)/runtime/traces/itrace.cpp \
	$(CSRC_DIR)/runtime/traces/ftrace.cpp

CSRC_C_SRCS :=
ifeq ($(strip $(CONFIG_DISASM)),y)
CSRC_C_SRCS += $(CSRC_DIR)/tools/disasm.c
endif

CSRC_BUILD_DIR := $(SIM_BUILD_DIR)/csrc
CSRC_CPP_OBJS := $(patsubst $(CSRC_DIR)/%.cpp,$(CSRC_BUILD_DIR)/%.o,$(CSRC_CPP_SRCS))
CSRC_C_OBJS := $(patsubst $(CSRC_DIR)/%.c,$(CSRC_BUILD_DIR)/%.o,$(CSRC_C_SRCS))
CSRC_OBJS := $(CSRC_CPP_OBJS) $(CSRC_C_OBJS)
CSRC_ARCHIVE := $(CSRC_BUILD_DIR)/libcsrc.a
CSRC_LIBS := $(CSRC_ARCHIVE)

$(CSRC_BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp $(BUILD_MODE_FILE) | $(SIM_BUILD_DIR)
	@mkdir -p $(@D)
	@echo "+ CXX -> csrc/$<"
	$(CXX) $(CXXFLAGS) -MMD -MP -c -o $@ $<

$(CSRC_BUILD_DIR)/%.o: $(CSRC_DIR)/%.c $(BUILD_MODE_FILE) | $(SIM_BUILD_DIR)
	@mkdir -p $(@D)
	@echo "+ CC  -> csrc/$<"
	$(CC) $(CFLAGS) -MMD -MP -c -o $@ $<

# Dut owns the selected Verilated top and therefore needs its generated header.
$(CSRC_BUILD_DIR)/runtime/platform/dut.o: $(VERILATOR_MODEL_LIB)

ifeq ($(strip $(CONFIG_DISASM)),y)
$(CSRC_C_OBJS): $(LIBCAPSTONE)
endif

$(CSRC_ARCHIVE): $(CSRC_OBJS)
	$(call build_banner,Archiving simulation runtime...)
	@mkdir -p $(@D)
	$(RM) $@
	$(AR) rcs $@ $^

-include $(CSRC_OBJS:.o=.d)

.PHONY: compdb-clean
compdb-clean::
	$(RM) $(CSRC_OBJS) $(CSRC_ARCHIVE)
