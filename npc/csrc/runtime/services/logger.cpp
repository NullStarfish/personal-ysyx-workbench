#include "runtime/services/logger.h"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>

Logger::~Logger() { shutdown(); }

void Logger::setLogFile(const char *path) { logFilePath = path == nullptr ? "" : path; }

void Logger::setPcTraceFile(const char *path) { pcTraceFilePath = path == nullptr ? "" : path; }

FILE *Logger::openFile(const std::string &path) {
  const char *filePath = path.empty() ? "/dev/null" : path.c_str();
  FILE *file = fopen(filePath, "w");
  if (file == nullptr) {
    printf("Error: Cannot open log file '%s'.\n", filePath);
    exit(1);
  }
  return file;
}

void Logger::init() {
  shutdown();
  logFile = openFile(logFilePath);
  pcTraceFile = openFile(pcTraceFilePath);
}

void Logger::shutdown() {
  if (logFile != nullptr) fclose(logFile);
  if (pcTraceFile != nullptr) fclose(pcTraceFile);
  logFile = nullptr;
  pcTraceFile = nullptr;
}

void Logger::writeLog(const char *fmt, ...) {
  if (logFile == nullptr) return;
  va_list args;
  va_start(args, fmt);
  vfprintf(logFile, fmt, args);
  va_end(args);
#ifdef CONFIG_LOG_FLUSH_EACH_WRITE
  fflush(logFile);
#endif
}

void Logger::writePcTrace(const char *fmt, ...) {
  if (pcTraceFile == nullptr) return;
  va_list args;
  va_start(args, fmt);
  vfprintf(pcTraceFile, fmt, args);
  va_end(args);
#ifdef CONFIG_LOG_FLUSH_EACH_WRITE
  fflush(pcTraceFile);
#endif
}
