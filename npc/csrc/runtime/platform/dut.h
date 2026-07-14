#ifndef NPC_RUNTIME_DUT_H
#define NPC_RUNTIME_DUT_H

#include <cstdint>
#include <memory>

class Dut {
public:
  Dut();
  ~Dut();

  Dut(const Dut &) = delete;
  Dut &operator=(const Dut &) = delete;

  void init();
  void shutdown();
  void reset(int cycles);
  void stepCycle();

  bool startVcdWatch(const char *filename = nullptr);
  bool endVcdWatch();
  bool isVcdWatching() const;
  const char *vcdPath() const;

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

#endif
