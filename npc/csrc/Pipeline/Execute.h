#ifndef NPC_PIPELINE_EXECUTE_H
#define NPC_PIPELINE_EXECUTE_H

#include "Pipeline/PipelineStage.h"

class ExecuteStage : public PipelineStage {
public:
  ExecuteStage();

  void trace(bool finished);
  void printUnitStats() const;

private:
  long long finishedCnt = 0;
};

#endif
