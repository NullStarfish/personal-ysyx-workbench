#ifndef NPC_RUNTIME_RETIRE_PIPELINE_H
#define NPC_RUNTIME_RETIRE_PIPELINE_H

#include "runtime/base/retire_event.h"

class Difftest;
class FTrace;
class ITrace;
class RunControl;
class WatchpointManager;

class RetirePipeline {
public:
  RetirePipeline(ITrace &itrace, FTrace &ftrace, Difftest &difftest,
                 WatchpointManager &watchpoints, RunControl &runControl);

  void process(const RetireEvent &event);

private:
  ITrace &itrace;
  FTrace &ftrace;
  Difftest &difftest;
  WatchpointManager &watchpoints;
  RunControl &runControl;
};

#endif
