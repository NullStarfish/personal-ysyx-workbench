#include <cstdio>
#include <cstdlib>
#include <cstring>
#include "watchpoint.h"
#include "sdb.h"

#define NR_WP 32

// The structure for a watchpoint node
typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  struct watchpoint *last;
  char *expr;
  uint32_t old_val;
} WP;

// The static pool of watchpoint nodes
static WP wp_pool[NR_WP] = {};
// Pointers to the head of the active list and the free list
static WP *head = NULL, *free_ = NULL;
// A counter for assigning unique watchpoint numbers
static int next_wp_no = 0;

/**
 * @brief Initializes the watchpoint pool, creating a linked list of free nodes.
 */
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

// Internal function to allocate a new watchpoint from the free list
static WP* new_wp_internal(char *e) {
  if (free_ == NULL) {
    printf("No free watchpoint available.\n");
    return NULL;
  }

  // Take a node from the free list
  WP* wp = free_;
  free_ = free_->next;

  // Evaluate the expression to get the initial value
  bool success;
  uint32_t initial_val = expr(e, &success);
  if (!success) {
    printf("Invalid expression for watchpoint: %s\n", e);
    // Return the node to the free list on failure
    wp->next = free_;
    free_ = wp;
    return NULL;
  }

  // Configure the new watchpoint
  wp->expr = strdup(e); // strdup allocates memory and copies the string
  wp->old_val = initial_val;
  wp->NO = next_wp_no++;

  // Add the new watchpoint to the head of the active list
  wp->next = head;
  wp->last = NULL;
  if (head != NULL) {
    head->last = wp;
  }
  head = wp;

  return wp;
}

// Internal function to return a watchpoint to the free list
static void free_wp_internal(WP *wp) {
  if (wp == NULL) return;

  // Unlink from the active list
  if (wp->last) {
    wp->last->next = wp->next;
  } else { // It was the head of the list
    head = wp->next;
  }
  if (wp->next) {
    wp->next->last = wp->last;
  }

  // Free the duplicated expression string
  free(wp->expr);
  wp->expr = NULL;

  // Add the node to the head of the free list
  wp->next = free_;
  wp->last = NULL;
  free_ = wp;
}

// Public interface called by the 'w' command
void wp_add(char *args) {
  if (args == NULL) {
    printf("Usage: w EXPR\n");
    return;
  }
  WP* wp = new_wp_internal(args);
  if (wp != NULL) {
    printf("Watchpoint %d: %s\n", wp->NO, wp->expr);
  }
}

// Public interface called by the 'd' command
void wp_remove(int no) {
  WP* wp_to_free = NULL;
  for (WP* p = head; p != NULL; p = p->next) {
    if (p->NO == no) {
      wp_to_free = p;
      break;
    }
  }

  if (wp_to_free) {
    printf("Deleted watchpoint %d: %s\n", wp_to_free->NO, wp_to_free->expr);
    free_wp_internal(wp_to_free);
  } else {
    printf("Watchpoint %d not found.\n", no);
  }
}

// Public interface called by the 'info w' command
void display_wp() {
  if (head == NULL) {
    printf("No watchpoints.\n");
    return;
  }
  printf("Num\tWhat\t\tValue\n");
  printf("---\t----\t\t-----\n");
  for (WP* p = head; p != NULL; p = p->next) {
    printf("%-d\t%-16s\t0x%08x (%u)\n", p->NO, p->expr, p->old_val, p->old_val);
  }
}

// Checks for changes in watchpoint values
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
  // If any watchpoint was triggered, it will cause the simulation to stop.
  return triggered;
}
