# Verilog source ownership and Verilator model generation.

VERILOG_HAND_DIR := $(NPC_HOME)/src/main/verilog/Core

VERILOG_HAND_SRCS :=
ifeq ($(SIM_TARGET),npc)
VERILOG_HAND_SRCS += $(VERILOG_HAND_DIR)/NpcTop.sv $(VERILOG_HAND_DIR)/NpcVirtualAxiRam.sv
endif

SOC_RTL_SRCS :=
ifeq ($(SIM_TARGET),ysyxsoc)
SOC_TOP_RTL := $(SOC_HOME)/build/ysyxSoCFull.v
SOC_CHISEL_SRCS := $(shell find $(SOC_HOME)/src -name '*.scala')
SOC_RTL_SRCS := $(shell find $(SOC_HOME)/perip \( -name '*.v' -o -name '*.sv' \)) $(SOC_TOP_RTL)

$(SOC_TOP_RTL): $(SOC_CHISEL_SRCS) $(SOC_HOME)/Makefile
	$(call build_banner,Generating ysyxSoC RTL...)
	$(MAKE) -C $(SOC_HOME) verilog
endif

# These are expanded in the Verilator recipe after the Chisel generation
# stamp has made the generated directory current.
VERILOG_GEN_SRCS = $(shell find $(CHISEL_RTL_DIR) \( -name '*.v' -o -name '*.sv' \) 2>/dev/null)
VERILOG_SRCS = $(VERILOG_GEN_SRCS) $(VERILOG_HAND_SRCS) $(SOC_RTL_SRCS)

VERILOG_INC_DIRS := \
	$(CHISEL_RTL_DIR)/include \
	$(CHISEL_RTL_DIR) \
	$(CHISEL_RTL_DIR)/verification \
	$(CHISEL_RTL_DIR)/verification/assert \
	$(CHISEL_RTL_DIR)/verification/assume \
	$(CHISEL_RTL_DIR)/verification/cover \
	$(SOC_HOME)/perip/uart16550/rtl \
	$(SOC_HOME)/perip/spi/rtl

VERILATOR_MODEL_LIB := $(VERILATOR_OBJ_DIR)/V$(TOP_MODULE)__ALL.a
VERILATOR_LIBS := \
	$(VERILATOR_MODEL_LIB) \
	$(VERILATOR_ROOT)/include/verilated.cpp \
	$(VERILATOR_ROOT)/include/verilated_dpi.cpp \
	$(VERILATOR_ROOT)/include/verilated_threads.cpp

VERILATOR_INCFLAGS := $(addprefix +incdir+,$(VERILOG_INC_DIRS))

$(VERILATOR_MODEL_LIB): $(CHISEL_RTL_STAMP) $(VERILOG_HAND_SRCS) $(SOC_RTL_SRCS) $(BUILD_MODE_FILE) | $(SIM_BUILD_DIR)
	$(call build_banner,Running Verilator for $(TOP_MODULE)...)
	$(RM) -r $(VERILATOR_OBJ_DIR)
	$(VERILATOR) --cc --sv --timescale "1ns/1ns" --autoflush --no-timing -DPRINTF_COND=$(DEBUG) --top-module $(TOP_MODULE) \
		$(VERILATOR_INCFLAGS) \
		-Mdir $(VERILATOR_OBJ_DIR) -Wno-WIDTHEXPAND $(VERILOG_SRCS)
	$(call build_banner,Compiling Verilated model library...)
	$(MAKE) -C $(VERILATOR_OBJ_DIR) -f V$(TOP_MODULE).mk
