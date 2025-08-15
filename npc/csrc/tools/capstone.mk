# =============================================================================
# Makefile for Capstone Engine Tool (Reusing from NEMU)
# =============================================================================
# This Makefile finds and builds the Capstone library located within an
# existing NEMU project directory.

# --- Prerequisite Check ---
# Check if the NEMU_HOME environment variable is set.
ifndef NEMU_HOME
  $(error NEMU_HOME is not set. Please set it to the root of your NEMU project, e.g., export NEMU_HOME=/path/to/nemu)
endif

# --- Paths ---
# Use the NEMU_HOME variable to define the path to Capstone.
CAPSTONE_HOME := $(NEMU_HOME)/tools/capstone
REPO_PATH := $(CAPSTONE_HOME)/repo

# --- Library Target ---
# Define the final shared library file as the main target.
LIBCAPSTONE := $(REPO_PATH)/libcapstone.so.5

# --- Build Rule ---
# This rule builds the library inside the NEMU project if it doesn't exist.
# It assumes the repository has already been cloned by the NEMU build system.
$(LIBCAPSTONE):
	@echo "### Building Capstone from NEMU directory: $(CAPSTONE_HOME) ###"
	@if [ ! -d "$(REPO_PATH)" ]; then \
		echo "Error: Capstone repository not found in NEMU project."; \
		echo "Please run 'make' inside '$(CAPSTONE_HOME)' first."; \
		exit 1; \
	fi
	$(MAKE) -C $(CAPSTONE_HOME)

# --- Phony Targets ---
.PHONY: all clean-capstone

all: $(LIBCAPSTONE)
.DEFAULT_GOAL = all

clean-capstone:
	@if [ -d "$(CAPSTONE_HOME)" ]; then \
		$(MAKE) -C $(CAPSTONE_HOME) clean; \
	fi
