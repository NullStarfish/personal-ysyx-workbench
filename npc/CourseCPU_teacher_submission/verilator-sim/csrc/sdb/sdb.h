#ifndef __SDB_H__
#define __SDB_H__

#include <stdint.h>
#include <stdbool.h>

// SDB 初始化
void init_sdb();
// SDB 主循环
void sdb_mainloop();
// 设置批处理模式
void sdb_set_batch_mode();

// 表达式求值接口
void init_regex(); // 新增：初始化正则表达式
uint32_t expr(char *e, bool *success);

// 监视点接口
void init_wp_pool();
void wp_add(char *args);
void wp_remove(int no);
void display_wp();
bool check_watchpoints();

// CPU 执行接口 (在 main.cpp 中定义)
void cpu_exec(uint64_t n);

// 物理地址读取 (在 main.cpp 中定义)
uint32_t paddr_read(uint32_t addr);


extern char *img_file;

#endif
