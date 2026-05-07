#include "sdb/watchpoint.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#include "cpu.h"
#include "mem.h"
#include "sdb/sdb.h"
#include "sim.h"

namespace {
constexpr int kNrWp = 32;
constexpr int kExprMaxCapacity = 64;

struct Watchpoint {
  int number = 0;
  Watchpoint *next = nullptr;
  Watchpoint *prev = nullptr;
  char *expr = nullptr;
  uint32_t oldValue = 0;
};

Watchpoint watchpointPool[kNrWp];
Watchpoint *head = nullptr;
Watchpoint *freeList = nullptr;
int nextWatchpointNo = 0;

Watchpoint *new_watchpoint(char *exprString) {
  if (freeList == nullptr) {
    printf("No free watchpoint available.\n");
    return nullptr;
  }
  if (strlen(exprString) >= kExprMaxCapacity) {
    printf("Error: Watchpoint expression is too long (max %d characters).\n", kExprMaxCapacity - 1);
    return nullptr;
  }

  Watchpoint *wp = freeList;
  freeList = freeList->next;

  bool success = false;
  uint32_t initialValue = expr(exprString, &success);
  if (!success) {
    printf("Invalid expression for watchpoint: %s\n", exprString);
    wp->next = freeList;
    freeList = wp;
    return nullptr;
  }

  wp->expr = static_cast<char *>(malloc(kExprMaxCapacity));
  if (wp->expr == nullptr) {
    printf("Error: Failed to allocate memory for watchpoint expression.\n");
    wp->next = freeList;
    freeList = wp;
    return nullptr;
  }

  strncpy(wp->expr, exprString, kExprMaxCapacity - 1);
  wp->expr[kExprMaxCapacity - 1] = '\0';
  wp->oldValue = initialValue;
  wp->number = nextWatchpointNo++;
  wp->next = head;
  wp->prev = nullptr;
  if (head != nullptr) {
    head->prev = wp;
  }
  head = wp;
  return wp;
}

void free_watchpoint(Watchpoint *wp) {
  if (wp == nullptr) {
    return;
  }
  if (wp->prev != nullptr) {
    wp->prev->next = wp->next;
  } else {
    head = wp->next;
  }
  if (wp->next != nullptr) {
    wp->next->prev = wp->prev;
  }
  free(wp->expr);
  wp->expr = nullptr;
  wp->next = freeList;
  wp->prev = nullptr;
  freeList = wp;
}
}

void init_wp_pool() {
  for (int i = 0; i < kNrWp; i++) {
    watchpointPool[i].number = i;
    watchpointPool[i].next = (i == kNrWp - 1) ? nullptr : &watchpointPool[i + 1];
    watchpointPool[i].prev = (i == 0) ? nullptr : &watchpointPool[i - 1];
    watchpointPool[i].expr = nullptr;
    watchpointPool[i].oldValue = 0;
  }
  head = nullptr;
  freeList = watchpointPool;
  nextWatchpointNo = 0;
}

void wp_add(char *args) {
  if (args == nullptr) {
    printf("Usage: w EXPR\n");
    return;
  }
  Watchpoint *wp = new_watchpoint(args);
  if (wp != nullptr) {
    printf("Watchpoint %d: %s\n", wp->number, wp->expr);
  }
}

void wp_remove(int no) {
  Watchpoint *target = nullptr;
  for (Watchpoint *p = head; p != nullptr; p = p->next) {
    if (p->number == no) {
      target = p;
      break;
    }
  }
  if (target != nullptr) {
    printf("Deleted watchpoint %d: %s\n", target->number, target->expr);
    free_watchpoint(target);
  } else {
    printf("Watchpoint %d not found.\n", no);
  }
}

void display_wp() {
  if (head == nullptr) {
    printf("No watchpoints.\n");
    return;
  }
  printf("Num\tWhat\t\tValue\n");
  printf("---\t----\t\t-----\n");
  for (Watchpoint *p = head; p != nullptr; p = p->next) {
    printf("%-d\t%-16s\t0x%08x (%u)\n", p->number, p->expr, p->oldValue, p->oldValue);
  }
}

bool check_watchpoints() {
  bool triggered = false;
  for (Watchpoint *p = head; p != nullptr; p = p->next) {
    bool success = false;
    uint32_t newValue = expr(p->expr, &success);
    if (success && newValue != p->oldValue) {
      printf("\nWatchpoint %d: %s\n", p->number, p->expr);
      printf("Old value = 0x%08x (%u)\n", p->oldValue, p->oldValue);
      printf("New value = 0x%08x (%u)\n", newValue, newValue);
      p->oldValue = newValue;
      triggered = true;
    }
  }
  return triggered;
}
