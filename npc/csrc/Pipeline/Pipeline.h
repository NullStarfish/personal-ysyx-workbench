#ifndef NPC_PIPELINE_H
#define NPC_PIPELINE_H

#include <cstdint>

#include "Pipeline/Cache.h"
#include "Pipeline/Decode.h"
#include "Pipeline/Execute.h"
#include "Pipeline/Fetch.h"
#include "Pipeline/Hazard.h"
#include "Pipeline/LSU.h"
#include "Pipeline/WriteBack.h"

class PipelineModel {
public:
  enum StageId { Fetch = 0, Decode, Execute, LSU, WriteBack, NumStages };

  FetchStage fetch;
  DecodeStage decode;
  ExecuteStage execute;
  LSUStage lsu;
  WriteBackStage writeBack;
  HazardTraceModel hazard;
  CacheTraceModel cache;

  void reset();
  void tick();
  void fetchOut(uint32_t pc, uint32_t instVal);
  void decodeOut(uint32_t pc, uint32_t instVal);
  void executeOut(uint32_t pc, uint32_t instVal);
  void lsuOut(uint32_t pc, uint32_t instVal);
  Inst retire(uint32_t pc, uint32_t instVal, uint32_t instType);
  void flush(uint32_t pc, uint32_t instVal);
  void printStats(long long totalCycles) const;
  long long retiredInstructions() const;

private:
  PipelineStage *stage(StageId id);
  const PipelineStage *stage(StageId id) const;
  void move(StageId from, StageId to, uint32_t pc, uint32_t instVal);
  Inst takeFrom(StageId stage, uint32_t pc, uint32_t instVal);
  void accept(StageId stage, const Inst &inst);
};

extern PipelineModel pipeline;

#endif
