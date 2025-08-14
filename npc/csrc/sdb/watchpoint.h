#ifndef __WATCHPOINT_H__
#define __WATCHPOINT_H__

#include <cstdint>

// Initializes the watchpoint pool. Should be called once at startup.
void init_wp_pool();

// Handler for the 'w' command to add a new watchpoint.
void wp_add(char* args);

// Handler for the 'd' command to remove a watchpoint by its number.
void wp_remove(int no);

// Handler for the 'info w' command to display all active watchpoints.
void display_wp();

/**
 * @brief Checks all active watchpoints to see if their values have changed.
 * This function should be called after each instruction execution.
 * @return true if a watchpoint was triggered, false otherwise.
 */
bool check_watchpoints();

#endif
