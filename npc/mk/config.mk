# Build-time feature selection from Kconfig and the selected simulation target.

CONFIG_CPPFLAGS :=

ifeq ($(SIM_TARGET),npc)
CONFIG_CPPFLAGS += -DCONFIG_NPC_VIRTUAL_SOC
endif

ifeq ($(strip $(CONFIG_TRACE)),y)
CONFIG_CPPFLAGS += -DCONFIG_TRACE
endif
ifeq ($(strip $(CONFIG_ITRACE)),y)
CONFIG_CPPFLAGS += -DCONFIG_ITRACE
endif
ifeq ($(strip $(CONFIG_FTRACE)),y)
CONFIG_CPPFLAGS += -DCONFIG_FTRACE
endif
ifeq ($(strip $(CONFIG_MTRACE)),y)
CONFIG_CPPFLAGS += -DCONFIG_MTRACE
endif
ifeq ($(strip $(CONFIG_DTRACE)),y)
CONFIG_CPPFLAGS += -DCONFIG_DTRACE
endif
ifeq ($(strip $(CONFIG_DIFFTEST)),y)
CONFIG_CPPFLAGS += -DCONFIG_DIFFTEST
endif
ifeq ($(strip $(CONFIG_WATCHPOINT)),y)
CONFIG_CPPFLAGS += -DCONFIG_WATCHPOINT
endif

BUILD_MODE_FILE := $(SIM_BUILD_DIR)/.selected_build_mode
BUILD_MODE_TEXT = SIM_TARGET=$(SIM_TARGET) BOARD=$(BOARD) VERILATOR=$(shell command -v $(VERILATOR)) VERILATOR_ROOT=$(VERILATOR_ROOT) CONFIG_TRACE=$(CONFIG_TRACE) CONFIG_ITRACE=$(CONFIG_ITRACE) CONFIG_FTRACE=$(CONFIG_FTRACE) CONFIG_MTRACE=$(CONFIG_MTRACE) CONFIG_DTRACE=$(CONFIG_DTRACE) CONFIG_DIFFTEST=$(CONFIG_DIFFTEST) CONFIG_WATCHPOINT=$(CONFIG_WATCHPOINT)

.PHONY: FORCE check_config

FORCE:

check_config:
	@if [ ! -f .config ]; then \
		echo "ERROR: Configuration file .config is missing."; \
		echo "Please run 'make menuconfig' first."; \
		exit 1; \
	fi

$(BUILD_MODE_FILE): FORCE | $(SIM_BUILD_DIR)
	@printf '%s\n' "$(BUILD_MODE_TEXT)" | cmp -s - $@ || printf '%s\n' "$(BUILD_MODE_TEXT)" > $@
