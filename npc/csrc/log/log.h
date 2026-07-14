#ifndef NPC_LOG_H
#define NPC_LOG_H

#include <cstdarg>
#include <cstdio>
#include <string>

class Logger {
public:
  Logger();
  ~Logger();

  Logger(const Logger &) = delete;
  Logger &operator=(const Logger &) = delete;

  void setLogFile(const char *path);
  void setPcTraceFile(const char *path);
  void init();
  void shutdown();
  void writeLog(const char *fmt, va_list args);
  void writePcTrace(const char *fmt, va_list args);

private:
  static FILE *openFile(const std::string &path);

  std::string logFilePath;
  std::string pcTraceFilePath;
  FILE *logFile = nullptr;
  FILE *pcTraceFile = nullptr;
};

void log_write(const char *fmt, ...);
void pcTraceWrite(const char *fmt, ...);
#endif
