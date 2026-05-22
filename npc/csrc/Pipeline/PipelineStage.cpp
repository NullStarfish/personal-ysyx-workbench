#include "Pipeline/PipelineStage.h"

#include <cstdio>

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
