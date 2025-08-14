#ifndef __MONITOR_H__
#define __MONITOR_H__

/**
 * @brief Initializes the entire simulation monitor.
 * * This function performs the following steps:
 * 1. Parses command-line arguments.
 * 2. Instantiates the Verilated model.
 * 3. Sets up the DPI-C interface scope.
 * 4. Loads the program binary into the simulated ROM.
 * 5. Resets the CPU.
 * 6. Initializes the Simple Debugger (SDB).
 * 7. Displays a welcome message.
 * * @param argc The argument count from main().
 * @param argv The argument vector from main().
 */
void init_monitor(int argc, char *argv[]);

#endif
