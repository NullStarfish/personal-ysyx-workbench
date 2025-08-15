#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include "log.h"

static FILE *log_fp = NULL;

void init_log(const char *log_file) {
  if (log_file == NULL) {
    // Redirect to /dev/null if no log file is specified
    log_fp = fopen("/dev/null", "w");
  } else {
    log_fp = fopen(log_file, "w");
  }
  if (log_fp == NULL) {
    printf("Error: Cannot open log file '%s'.\n", log_file ? log_file : "/dev/null");
    exit(1);
  }
}

void log_write(const char *fmt, ...) {
  if (log_fp) {
    va_list args;
    va_start(args, fmt);
    vfprintf(log_fp, fmt, args);
    va_end(args);
    fflush(log_fp);
  }
}
