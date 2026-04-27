# =============================================================================
# Makefile Fragment for Disassembler Integration
# =============================================================================
# This file should be included by the main Makefile when CONFIG_ITRACE is enabled.

# 1. Include the dedicated Makefile for the Capstone tool.
# This will handle the automatic cloning and building of the library.
include $(TOOLS_DIR)/capstone.mk

# 2. Add Capstone flags to the main build variables.
# These variables (e.g., LIBCAPSTONE) are now defined in capstone.mk.
CFLAGS += -I$(REPO_PATH)/include
LDFLAGS += -L$(REPO_PATH) -lcapstone -Wl,-rpath=$(abspath $(REPO_PATH))

# 3. Make all C object files depend on the Capstone library being built first.
$(C_OBJS): $(LIBCAPSTONE)

# 4. Hook into the main 'clean' rule.
CLEAN_HOOKS += clean-capstone
