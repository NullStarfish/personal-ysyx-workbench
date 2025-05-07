/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "sdb.h"
#include <stdint.h>

#define NR_WP 32


#ifndef DEBUG_WP
#define DEBUG_PRINT(fmt, ...) 
#else
#define DEBUG_PRINT(fmt, ...) printf(fmt, ##__VA_ARGS__)
#endif

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  struct watchpoint *last;
  
  /* TODO: Add more members if necessary */
  char *expr;
  uint32_t old_val;

} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;


static uint32_t last_index = 0;


void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
    wp_pool[i].last = (i == 0 ? NULL : &wp_pool[i - 1]);
    wp_pool[i].expr = NULL;
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

void wp_setup(WP* wp, char *e, bool *success) {
  int old_value = expr(e, success);
  if (!(*success)) {
    printf("Invalid EXPRESSION !\n");
    return;
  }
  // 使用 strdup 做深拷贝
  wp->expr = strdup(e);
  wp->old_val = old_value;
}




WP* new_wp(char *e) {
  DEBUG_PRINT("enter new_wp\n");
  if (free_ == NULL) {
    printf("No free watchpoint\n");
    return NULL;
  }
  bool success = false;
  WP* retexpr = free_;
  wp_setup(retexpr, e, &success);
  if (!success) {
    return NULL;
  }
  free_ = free_ ->next; // setup 不影响next



  retexpr->next = head;
  retexpr->last = NULL;
  if (head)
    head->last = retexpr;

  retexpr->NO = last_index ++;
  head = retexpr;

  return retexpr;

}

void free_wp(WP *wp) {
  if (wp == NULL) {
    printf("You re freeing an EMPTY watchpoint!\n");
    return;
  }
  bool find = false;
  for (WP *i = head; i ; i = i->next) {
    if (i == wp) find = true;
  }
  if (!find) {
    printf("Not included in wp list!\n");
    return;
  }
  
  DEBUG_PRINT("start remove\n");
  if (!wp->last)
    head = wp->next;

  if (wp->last && wp->next)
    wp->last->next = wp->next;
  

  wp->next = free_;
  wp->last = NULL;
  free(wp->expr);
  DEBUG_PRINT("success\n");
}

void wp_remove(int NO) {
  bool find = false;
  for (WP*i = head; i != NULL; i = i->next) {
    DEBUG_PRINT("i->expr: %s, i->NO: %u, i->old_val: %u\n", i->expr, i->NO, i->old_val);
    if (i->NO == NO) {
      printf("Delete watchpoint %d: %s\n", i->NO, i->expr);
      free_wp(i);
      find = true;
      break;
    }
  }
  if (!find)
    printf("Invalid NO!\n");
}

void wp_add(char *str) {
  WP* wp = new_wp(str);
  if (wp == NULL) {
    return;
  }
  printf("Hardware watchpoint %d: %s\n", wp->NO, wp->expr);
}


void wp_difftest() {
  for (WP* i = head; i; i = i -> next) {
    bool success;
    uint32_t cur_val = expr(i->expr, &success);
    if (!success) {
      printf("BAD EXPRESSION!\n");
      return;
    }

    if (i->old_val != cur_val) {
      printf("Watchpoint %d: %s\nOld value = %u\nNew value = %u\n", i->NO, i->expr, i->old_val, cur_val);
      i->old_val = cur_val;
    }
  }
}


void display_wp() {
  for (WP* i = head; i != NULL; i = i->next) {
    printf("Watchpoint %d: %s, val %d\n", i->NO, i->expr, i->old_val);
  }
}