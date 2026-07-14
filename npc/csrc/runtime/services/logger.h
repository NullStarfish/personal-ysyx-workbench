#ifndef NPC_LOG_H
#define NPC_LOG_H

#include <cstdio>
#include <string>

class Logger {
public:
  Logger() = default;
  ~Logger();

  Logger(const Logger &) = delete;
  Logger &operator=(const Logger &) = delete;

  void setLogFile(const char *path);
  void setPcTraceFile(const char *path);
  void init();
  void shutdown();
  void writeLog(const char *fmt, ...);
  void writePcTrace(const char *fmt, ...);

private:
  static FILE *openFile(const std::string &path);

  std::string logFilePath;
  std::string pcTraceFilePath;
  FILE *logFile = nullptr;
  FILE *pcTraceFile = nullptr;
};
#endif
