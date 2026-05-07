#ifndef __SDB_H__
#define __SDB_H__

#include <stdbool.h>
#include <stdint.h>

void init_sdb();
void sdb_mainloop();
void sdb_set_batch_mode();

void init_regex();
uint32_t expr(char *e, bool *success);

void init_wp_pool();
void wp_add(char *args);
void wp_remove(int no);
void display_wp();
bool check_watchpoints();

extern char *img_file;

#endif
