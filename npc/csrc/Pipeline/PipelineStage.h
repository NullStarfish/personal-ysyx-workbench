#ifndef NPC_PIPELINE_STAGE_H
#define NPC_PIPELINE_STAGE_H

#include "Pipeline/Inst.h"

class PipelineStage {
public:
  const char *name = "";
  bool valid = false;
  Inst inst{};
  long long validCycles = 0;
  long long acceptCount = 0;

  PipelineStage() = default;
  explicit PipelineStage(const char *stageName);

  void accept(const Inst &next);
  Inst take();
  void clear();
  void tick();
  void print(long long totalCycles) const;
};

#endif
