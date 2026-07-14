# Simulation runtime source ownership.

CSRC_INCLUDE_DIRS := \
	$(CSRC_DIR) \
	$(CSRC_DIR)/tools \
	$(CSRC_DIR)/sdb \
	$(CSRC_DIR)/difftest \
	$(CSRC_DIR)/trace \
	$(CSRC_DIR)/log

CSRC_CPP_SRCS := \
	$(CSRC_DIR)/log/log.cpp \
	$(CSRC_DIR)/main.cpp \
	$(CSRC_DIR)/runtime.cpp \
	$(CSRC_DIR)/sim_counter.cpp \
	$(CSRC_DIR)/trace/itrace.cpp \
	$(CSRC_DIR)/trace/ftrace.cpp \
	$(CSRC_DIR)/monitor.cpp \
	$(CSRC_DIR)/difftest_runtime.cpp \
	$(CSRC_DIR)/cpu.cpp \
	$(CSRC_DIR)/mem.cpp

ifeq ($(strip $(CONFIG_INTERACTIVE_SDB)),y)
CSRC_CPP_SRCS += \
	$(CSRC_DIR)/sdb/watchpoint.cpp \
	$(CSRC_DIR)/sdb/expr.cpp \
	$(CSRC_DIR)/sdb/sdb.cpp
else
CSRC_CPP_SRCS += \
	$(CSRC_DIR)/sdb/sdb_stub.cpp
endif

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

# runtime.h selects the Verilated top class and therefore needs its header.
$(CSRC_BUILD_DIR)/runtime.o: $(VERILATOR_MODEL_LIB)

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
