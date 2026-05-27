# Chisel source ownership and Core RTL generation.

CHISEL_RTL_DIR := $(BUILD_DIR)/rtl/Core
CHISEL_RTL_STAMP := $(CHISEL_RTL_DIR)/.generated.stamp

CHISEL_SRCS := $(shell find $(NPC_HOME)/src/main/scala $(NPC_HOME)/HwOS/src/main/scala -name '*.scala')
CHISEL_DEPS := $(CHISEL_SRCS) $(NPC_HOME)/build.mill

$(CHISEL_RTL_STAMP): $(CHISEL_DEPS)
	@mkdir -p $(CHISEL_RTL_DIR)
	$(call build_banner,Generating Core RTL with Mill...)
	mill coreverilog.run
	@touch $@

.PHONY: chisel rtl
chisel rtl: $(CHISEL_RTL_STAMP)
