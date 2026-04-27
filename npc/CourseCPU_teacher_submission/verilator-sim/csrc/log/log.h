#ifndef __LOG_H__
#define __LOG_H__

#include <stdio.h>

// Initializes the log file. If log_file is NULL, output is redirected to /dev/null.
void init_log(const char *log_file);

// Writes a formatted string to the log file.
void log_write(const char *fmt, ...);

#endif
