#include "log.h"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>

namespace {
FILE *logFile = nullptr;
FILE *pcTraceFile = nullptr;
}

void init_log(const char *path, bool isLog) {
  if (path == nullptr) {
    if (isLog) logFile = fopen("/dev/null", "w");
    else pcTraceFile = fopen("/dev/null", "w");
  } else {
    if (isLog) logFile = fopen(path, "w");
    else pcTraceFile = fopen(path, "w");
  }
  if ((isLog ? logFile : pcTraceFile) == nullptr) {
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

void pcTraceWrite(const char *fmt, ...) {
  if (pcTraceFile == nullptr) {
    return;
  }
  va_list args;
  va_start(args, fmt);
  vfprintf(pcTraceFile, fmt, args);
  va_end(args);
  fflush(pcTraceFile);
} 