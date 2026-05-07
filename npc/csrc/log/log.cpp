#include "log.h"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>

namespace {
FILE *logFile = nullptr;
}

void init_log(const char *path) {
  if (path == nullptr) {
    logFile = fopen("/dev/null", "w");
  } else {
    logFile = fopen(path, "w");
  }
  if (logFile == nullptr) {
    printf("Error: Cannot open log file '%s'.\n", path ? path : "/dev/null");
    exit(1);
  }
}

void log_write(const char *fmt, ...) {
  if (logFile == nullptr) {
    return;
  }
  va_list args;
  va_start(args, fmt);
  vfprintf(logFile, fmt, args);
  va_end(args);
  fflush(logFile);
}
