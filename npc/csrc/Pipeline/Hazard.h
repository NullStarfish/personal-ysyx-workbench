#ifndef NPC_PIPELINE_HAZARD_H
#define NPC_PIPELINE_HAZARD_H

class HazardTraceModel {
public:
  void trace(bool loadUseStall, bool redirectFlush);
  void printStats(long long totalCycles) const;

private:
  long long loadUseCycles = 0;
  long long redirectFlushCycles = 0;
};

#endif
