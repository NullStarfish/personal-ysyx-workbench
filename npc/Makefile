# ==================================================================
#                       变量定义 (Variables)
# ==================================================================

# --- 项目名称和路径 ---
# 项目名称，也是最终生成的可执行文件名
NAME ?= npc
# !!! 用户需要修改这里 !!!
# NEMU 项目的根目录，用于复用其源代码
NEMU_HOME ?= 
# NPC 项目的根目录 (当前目录)
NPC_HOME ?= 
# 编译输出目录
BUILD_DIR ?= $(NPC_HOME)/build
# Verilator 生成模型的输出目录
V_BUILD_DIR = $(BUILD_DIR)/VTop_obj

# --- 源代码文件 ---
# Verilog 源文件目录
VSRC_DIR = $(NPC_HOME)/vsrc
# C/C++ 源文件目录
CSRC_DIR = $(NPC_HOME)/csrc
# 自动查找所有 Verilog 文件
VSRCS = $(shell find $(VSRC_DIR) -name "*.v")
# 自动查找所有 C++ 文件
CPPSRCS = $(shell find $(CSRC_DIR) -name "*.cpp")

# --- NEMU 基础设施复用 ---
# 需要包含的 NEMU 头文件目录
# --- FIX: Added ISA and Device include paths from NEMU ---
NEMU_INCLUDES = \
  -I$(NEMU_HOME)/include \
  -I$(NEMU_HOME)/src/monitor/sdb \
  -I$(NEMU_HOME)/src/utils \
  -I$(NEMU_HOME)/src/isa/riscv32/include \
  -I$(NEMU_HOME)/src/device/include

# 需要编译的 NEMU 源文件
NEMU_SRCS = \
  $(NEMU_HOME)/src/monitor/sdb/sdb.c \
  $(NEMU_HOME)/src/monitor/sdb/expr.c \
  $(NEMU_HOME)/src/monitor/sdb/watchpoint.c \
  $(NEMU_HOME)/src/utils/log.c \
  $(NEMU_HOME)/src/utils/timer.c

# --- 编译和链接标志 ---
# Verilator 标志
VERILATOR_FLAGS = --cc --exe --top-module Top -I$(VSRC_DIR) --build -j
# C 和 C++ 编译器
CC = gcc
CXX = g++
# 编译标志 (包含 NEMU 头文件, Verilator 头文件)
INC_PATH = -I/usr/local/share/verilator/include -I/usr/local/share/verilator/include/vltstd -I$(V_BUILD_DIR) $(NEMU_INCLUDES) -I/usr/include/SDL2
# --- FIX: Removed quotes from __GUEST_ISA__ definition ---
CFLAGS = -O2 -Wall -Werror -g $(INC_PATH) -D__GUEST_ISA__=riscv32 -DNPC_SIM
CXXFLAGS = $(CFLAGS)
# 链接标志
LDFLAGS = -L/usr/lib -lSDL2 -lreadline -ldl

# --- 目标文件 ---
# 将所有 C/C++ 源文件转换为 .o 目标文件
OBJS = $(CPPSRCS:$(CSRC_DIR)/%.cpp=$(BUILD_DIR)/%.o)
OBJS += $(NEMU_SRCS:$(NEMU_HOME)/%.c=$(BUILD_DIR)/nemu/%.o)

# ==================================================================
#                       编译规则 (Rules)
# ==================================================================

# --- 默认目标 ---
all: $(BUILD_DIR)/$(NAME)

# --- Verilator 模型生成 ---
# 这个目标负责运行 Verilator 并编译其生成的 C++ 模型
# 它依赖于所有的 Verilog 和 C++ 源文件
$(V_BUILD_DIR)/VTop: $(VSRCS) $(CPPSRCS)
	@echo "[Verilator] Generating C++ model..."
	@mkdir -p $(V_BUILD_DIR)
	@verilator $(VERILATOR_FLAGS) --Mdir $(V_BUILD_DIR) $(VSRCS) $(CPPSRCS)

# --- NEMU 和 NPC 源代码编译 ---
# 创建目标文件目录的规则
$(BUILD_DIR)/%.o: $(CSRC_DIR)/%.cpp | $(V_BUILD_DIR)/VTop
	@mkdir -p $(dir $@)
	@echo "[CXX] Compiling $<"
	@$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD_DIR)/nemu/%.o: $(NEMU_HOME)/%.c | $(V_BUILD_DIR)/VTop
	@mkdir -p $(dir $@)
	@echo "[CC]  Compiling NEMU source $<"
	@$(CC) $(CFLAGS) -c $< -o $@

# --- 链接生成最终可执行文件 ---
$(BUILD_DIR)/$(NAME): $(OBJS) $(V_BUILD_DIR)/VTop
	@echo "[LD]  Linking..."
	@$(CXX) -o $@ $(filter %.o, $^) $(addprefix $(V_BUILD_DIR)/, VTop__ALL.a) $(LDFLAGS)

# ==================================================================
#                       便捷命令 (Phony Targets)
# ==================================================================
.PHONY: all run clean

# --- 运行仿真 ---
# 使用 'make run IMG=/path/to/your/program.bin' 来指定程序
IMG ?= your_program.bin
run: $(BUILD_DIR)/$(NAME)
	@echo "[RUN] Starting NPC simulation..."
	@$^ $(IMG)

# --- 清理 ---
clean:
	@echo "[CLEAN] Removing generated files..."
	@rm -rf $(BUILD_DIR)

