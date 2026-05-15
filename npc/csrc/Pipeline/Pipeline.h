#ifndef NPC_PIPELINE_H
#define NPC_PIPELINE_H

#include <cstdint>

enum InstType {
  InstArith = 0,
  InstMem = 1,
  InstRedirect = 2,
  InstSys = 3,
  InstTypeCount = 4,
};

class Inst {
public:
  uint32_t pc = 0;
  uint32_t instVal = 0;
  uint32_t instType = InstArith;
  uint32_t lifeCycleCount = 0;
};

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

class FetchStage : public PipelineStage {
public:
  FetchStage();

  void trace(bool gotInst, uint32_t pc, uint32_t instVal, uint32_t memLatency, uint32_t waitLatency);
  void traceUnit(bool reqBlocked, bool outBlocked, bool flush);
  void printUnitStats() const;
  void printStats(long long totalCycles) const;

private:
  long long gotInstCnt = 0;
  long long memLatencyCnt = 0;
  long long maxMemLatency = 0;
  long long waitLatencyCnt = 0;
  long long maxWaitLatency = 0;
  long long reqBlockedCycles = 0;
  long long outBlockedCycles = 0;
  long long flushCycles = 0;
};

class DecodeStage : public PipelineStage {
public:
  DecodeStage();
};

class ExecuteStage : public PipelineStage {
public:
  ExecuteStage();

  void trace(bool finished);
  void printUnitStats() const;

private:
  long long finishedCnt = 0;
};

class LSUStage : public PipelineStage {
public:
  LSUStage();

  void trace(uint32_t latency, bool write);
  void traceBackpressure(bool blocked);
  void printUnitStats() const;
  void printStats(long long totalCycles) const;

private:
  long long gotDataCnt = 0;
  long long writeDataCnt = 0;
  long long loadLatencyCnt = 0;
  long long maxLoadLatency = 0;
  long long storeLatencyCnt = 0;
  long long maxStoreLatency = 0;
  long long backpressureCycles = 0;
};

class WriteBackStage : public PipelineStage {
public:
  WriteBackStage();

  void traceRetire(const Inst &inst, uint32_t instType);
  long long retiredCount() const;
  void printStats() const;

private:
  long long instTypeCnt[InstTypeCount] = {};
  long long instLifeCycleCnt[InstTypeCount] = {};
  long long totalInstLifeCycles = 0;
  long long maxInstLifeCycles = 0;
};

class HazardTraceModel {
public:
  void trace(bool loadUseStall, bool redirectFlush);
  void printStats(long long totalCycles) const;

private:
  long long loadUseCycles = 0;
  long long redirectFlushCycles = 0;
};

class PipelineModel {
public:
  enum StageId { Fetch = 0, Decode, Execute, LSU, WriteBack, NumStages };

  FetchStage fetch;
  DecodeStage decode;
  ExecuteStage execute;
  LSUStage lsu;
  WriteBackStage writeBack;
  HazardTraceModel hazard;

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
