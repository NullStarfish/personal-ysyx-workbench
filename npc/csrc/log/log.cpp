#include "log.h"

#include <cstdarg>
#include <cstdio>
#include <cstdlib>

namespace {
Logger *activeLogger = nullptr;
}

Logger::Logger() { activeLogger = this; }

Logger::~Logger() {
  shutdown();
  if (activeLogger == this) activeLogger = nullptr;
}

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

void Logger::writeLog(const char *fmt, va_list args) {
  if (logFile == nullptr) return;
  vfprintf(logFile, fmt, args);
#ifdef CONFIG_LOG_FLUSH_EACH_WRITE
  fflush(logFile);
#endif
}

void Logger::writePcTrace(const char *fmt, va_list args) {
  if (pcTraceFile == nullptr) return;
  vfprintf(pcTraceFile, fmt, args);
#ifdef CONFIG_LOG_FLUSH_EACH_WRITE
  fflush(pcTraceFile);
#endif
}

void log_write(const char *fmt, ...) {
  if (activeLogger == nullptr) return;
  va_list args;
  va_start(args, fmt);
  activeLogger->writeLog(fmt, args);
  va_end(args);
}

void pcTraceWrite(const char *fmt, ...) {
  if (activeLogger == nullptr) return;
  va_list args;
  va_start(args, fmt);
  activeLogger->writePcTrace(fmt, args);
  va_end(args);
}
