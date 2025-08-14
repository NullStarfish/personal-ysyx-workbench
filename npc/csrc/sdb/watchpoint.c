#include <stdio.h>
#include <stdlib.h>
#include <string.h> // 新增：包含 string.h 以声明 strdup
#include "watchpoint.h"
#include "sdb.h"

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  struct watchpoint *last;
  char *expr;
  uint32_t old_val;
} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;
static int next_wp_no = 0;

void init_wp_pool() {
  for (int i = 0; i < NR_WP; i++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
    wp_pool[i].last = (i == 0 ? NULL : &wp_pool[i - 1]);
    wp_pool[i].expr = NULL;
  }
  head = NULL;
  free_ = wp_pool;
  next_wp_no = 0;
}

static WP* new_wp_internal(char *e) {
  if (free_ == NULL) {
    printf("No free watchpoint available.\n");
    return NULL;
  }
  WP* wp = free_;
  free_ = free_->next;

  bool success;
  uint32_t initial_val = expr(e, &success);
  if (!success) {
    printf("Invalid expression for watchpoint: %s\n", e);
    wp->next = free_;
    free_ = wp;
    return NULL;
  }

  wp->expr = strdup(e);
  wp->old_val = initial_val;
  wp->NO = next_wp_no++;
  wp->next = head;
  wp->last = NULL;
  if (head != NULL) { head->last = wp; }
  head = wp;
  return wp;
}

static void free_wp_internal(WP *wp) {
  if (wp == NULL) return;
  if (wp->last) { wp->last->next = wp->next; }
  else { head = wp->next; }
  if (wp->next) { wp->next->last = wp->last; }

  free(wp->expr);
  wp->expr = NULL;
  wp->next = free_;
  wp->last = NULL;
  free_ = wp;
}

void wp_add(char *args) {
  if (args == NULL) { printf("Usage: w EXPR\n"); return; }
  WP* wp = new_wp_internal(args);
  if (wp != NULL) { printf("Watchpoint %d: %s\n", wp->NO, wp->expr); }
}

void wp_remove(int no) {
  WP* wp_to_free = NULL;
  for (WP* p = head; p != NULL; p = p->next) {
    if (p->NO == no) { wp_to_free = p; break; }
  }
  if (wp_to_free) {
    printf("Deleted watchpoint %d: %s\n", wp_to_free->NO, wp_to_free->expr);
    free_wp_internal(wp_to_free);
  } else {
    printf("Watchpoint %d not found.\n", no);
  }
}

void display_wp() {
  if (head == NULL) { printf("No watchpoints.\n"); return; }
  printf("Num\tWhat\t\tValue\n");
  printf("---\t----\t\t-----\n");
  for (WP* p = head; p != NULL; p = p->next) {
    printf("%-d\t%-16s\t0x%08x (%u)\n", p->NO, p->expr, p->old_val, p->old_val);
  }
}

bool check_watchpoints() {
  bool triggered = false;
  for (WP* p = head; p != NULL; p = p->next) {
    bool success;
    uint32_t new_val = expr(p->expr, &success);
    if (success && new_val != p->old_val) {
      printf("\nWatchpoint %d: %s\n", p->NO, p->expr);
      printf("Old value = 0x%08x (%u)\n", p->old_val, p->old_val);
      printf("New value = 0x%08x (%u)\n", new_val, new_val);
      p->old_val = new_val;
      triggered = true;
    }
  }
  return triggered;
}
