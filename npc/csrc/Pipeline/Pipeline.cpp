#include "Pipeline/Pipeline.h"

#include <cstdio>

#include "svdpi.h"

PipelineModel pipeline;

PipelineStage::PipelineStage(const char *stageName) : name(stageName) {}

void PipelineStage::accept(const Inst &next) {
  inst = next;
  valid = true;
  acceptCount++;
}

Inst PipelineStage::take() {
  Inst old = inst;
  clear();
  return old;
}

void PipelineStage::clear() {
  valid = false;
  inst = {};
}

void PipelineStage::tick() {
  if (valid) {
    validCycles++;
    inst.lifeCycleCount++;
  }
}

void PipelineStage::print(long long totalCycles) const {
  if (totalCycles <= 0) return;
  printf("  %-9s valid=%d pc=0x%08x inst=0x%08x occupancy=%lf%% accepted=%lld\n",
         name, valid ? 1 : 0, inst.pc, inst.instVal,
         100.0 * validCycles / totalCycles, acceptCount);
}

void PipelineModel::reset() {
  *this = PipelineModel();
}

void PipelineModel::tick() {
  fetch.tick();
  decode.tick();
  execute.tick();
  lsu.tick();
  writeBack.tick();
}

PipelineStage *PipelineModel::stage(StageId id) {
  switch(id) {
    case Fetch: return &fetch;
    case Decode: return &decode;
    case Execute: return &execute;
    case LSU: return &lsu;
    case WriteBack: return &writeBack;
    default: return nullptr;
  }
}

const PipelineStage *PipelineModel::stage(StageId id) const {
  switch(id) {
    case Fetch: return &fetch;
    case Decode: return &decode;
    case Execute: return &execute;
    case LSU: return &lsu;
    case WriteBack: return &writeBack;
    default: return nullptr;
  }
}

void PipelineModel::accept(StageId stage, const Inst &inst) {
  PipelineStage *target = this->stage(stage);
  if (target != nullptr) {
    target->accept(inst);
  }
}

Inst PipelineModel::takeFrom(StageId stage, uint32_t pc, uint32_t instVal) {
  PipelineStage *preferred = this->stage(stage);
  if (preferred != nullptr && preferred->valid && preferred->inst.pc == pc && preferred->inst.instVal == instVal) {
    return preferred->take();
  }

  for (int i = 0; i < NumStages; i++) {
    PipelineStage *candidate = this->stage(static_cast<StageId>(i));
    if (candidate != nullptr && candidate->valid && candidate->inst.pc == pc && candidate->inst.instVal == instVal) {
      return candidate->take();
    }
  }

  Inst fallback = {};
  fallback.pc = pc;
  fallback.instVal = instVal;
  return fallback;
}

void PipelineModel::move(StageId from, StageId to, uint32_t pc, uint32_t instVal) {
  accept(to, takeFrom(from, pc, instVal));
}

void PipelineModel::fetchOut(uint32_t pc, uint32_t instVal) {
  Inst inst = {};
  inst.pc = pc;
  inst.instVal = instVal;
  accept(Decode, inst);
}

void PipelineModel::decodeOut(uint32_t pc, uint32_t instVal) {
  move(Decode, Execute, pc, instVal);
}

void PipelineModel::executeOut(uint32_t pc, uint32_t instVal) {
  move(Execute, LSU, pc, instVal);
}

void PipelineModel::lsuOut(uint32_t pc, uint32_t instVal) {
  move(LSU, WriteBack, pc, instVal);
}

Inst PipelineModel::retire(uint32_t pc, uint32_t instVal, uint32_t instType) {
  Inst inst = takeFrom(WriteBack, pc, instVal);
  inst.instType = instType;
  writeBack.traceRetire(inst, instType);
  return inst;
}

void PipelineModel::flush(uint32_t pc, uint32_t instVal) {
  (void)pc;
  (void)instVal;
  fetch.clear();
  decode.clear();
}

void PipelineModel::printStats(long long totalCycles) const {
  if (totalCycles <= 0) return;
  writeBack.printStats();
  fetch.printUnitStats();
  execute.printUnitStats();
  lsu.printUnitStats();
  fetch.printStats(totalCycles);
  lsu.printStats(totalCycles);
  cache.printStats();
  printf("Pipeline stages:\n");
  for (int i = 0; i < NumStages; i++) {
    const PipelineStage *s = stage(static_cast<StageId>(i));
    if (s != nullptr) {
      s->print(totalCycles);
    }
  }
  hazard.printStats(totalCycles);
}

long long PipelineModel::retiredInstructions() const {
  return writeBack.retiredCount();
}

extern "C" void pipeline_trace(svBit fetchOut, int fetchPc, int fetchInst,
                                svBit decodeOut, int decodePc, int decodeInst,
                                svBit executeOut, int executePc, int executeInst,
                                svBit lsuOut, int lsuPc, int lsuInst,
                                svBit retire, int retirePc, int retireInst) {
  (void)retire;
  (void)retirePc;
  (void)retireInst;
  if (lsuOut) pipeline.lsuOut(static_cast<uint32_t>(lsuPc), static_cast<uint32_t>(lsuInst));
  if (executeOut) pipeline.executeOut(static_cast<uint32_t>(executePc), static_cast<uint32_t>(executeInst));
  if (decodeOut) pipeline.decodeOut(static_cast<uint32_t>(decodePc), static_cast<uint32_t>(decodeInst));
  if (fetchOut) pipeline.fetchOut(static_cast<uint32_t>(fetchPc), static_cast<uint32_t>(fetchInst));
}

extern "C" void flush_trace(svBit flush, int pc, int inst) {
  if (flush) {
    pipeline.flush(static_cast<uint32_t>(pc), static_cast<uint32_t>(inst));
  }
}
