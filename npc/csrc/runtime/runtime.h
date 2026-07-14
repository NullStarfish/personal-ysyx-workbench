#ifndef NPC_RUNTIME_H
#define NPC_RUNTIME_H

#include <memory>
#include <string>

struct RuntimeOptions {
  bool batchMode = false;
  std::string imageFile;
  std::string logFile;
  std::string pcTraceFile;
  std::string elfFile;
  std::string diffSoFile;

  static RuntimeOptions parse(int argc, char *argv[]);
};

class Runtime {
public:
  explicit Runtime(RuntimeOptions options);
  ~Runtime();

  Runtime(const Runtime &) = delete;
  Runtime &operator=(const Runtime &) = delete;

  void init();
  int run();
  void shutdown();

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

#endif
