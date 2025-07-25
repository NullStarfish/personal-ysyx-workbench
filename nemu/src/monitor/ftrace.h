#ifndef __MONITOR_FTRACE_H__
#define __MONITOR_FTRACE_H__

#include <common.h>

/**
 * @brief 初始化函数跟踪功能
 * * @param elf_file 指向要加载的ELF文件的路径字符串。
 * 如果为NULL，则禁用ftrace。
 */
void init_ftrace(const char *elf_file);

/**
 * @brief 记录一次函数调用
 * * 在执行jal或jalr指令后调用此函数，以记录函数调用事件。
 * * @param pc 当前指令的程序计数器值
 * @param target 函数调用的目标地址
 */
void log_func_call(vaddr_t pc, vaddr_t target);

/**
 * @brief 记录一次函数返回
 * * 在执行ret(jalr x0, 0(ra))指令后调用此函数，以记录函数返回事件。
 * * @param pc 当前指令的程序计数器值
 */
void log_func_ret(vaddr_t pc);

#endif
