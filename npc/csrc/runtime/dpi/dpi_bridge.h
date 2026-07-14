#ifndef NPC_RUNTIME_DPI_BRIDGE_H
#define NPC_RUNTIME_DPI_BRIDGE_H

class Difftest;
class Memory;
class SimCounterBank;
class Simulation;

class DpiBridge {
public:
  DpiBridge(Simulation &simulation, Memory &memory, Difftest &difftest, SimCounterBank &counters);
  ~DpiBridge();

  DpiBridge(const DpiBridge &) = delete;
  DpiBridge &operator=(const DpiBridge &) = delete;

  void bind();
  void unbind();

  Simulation &simulation;
  Memory &memory;
  Difftest &difftest;
  SimCounterBank &counters;
};

#endif
