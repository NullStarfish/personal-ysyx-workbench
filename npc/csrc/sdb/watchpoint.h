#ifndef __WATCHPOINT_H__
#define __WATCHPOINT_H__

#include <stdbool.h>
#include <stdint.h>

class CPU;
class Mem;

void init_wp_pool();
void wp_add(char *args);
void wp_remove(int no);
void display_wp();
bool check_watchpoints();

#endif
