#ifndef __WATCHPOINT_H__
#define __WATCHPOINT_H__

#include <stdint.h>
#include <stdbool.h>

// 初始化监视点池
void init_wp_pool();
// 添加监视点
void wp_add(char *args);
// 移除监视点
void wp_remove(int no);
// 显示所有监视点
void display_wp();
// 检查监视点是否被触发
bool check_watchpoints();

#endif
