#include "runtime/execution/retire_pipeline.h"

#include "runtime/base/run_control.h"
#include "runtime/sdb/watchpoint.h"
#include "runtime/services/difftest.h"
#include "runtime/traces/ftrace.h"
#include "runtime/traces/itrace.h"

RetirePipeline::RetirePipeline(ITrace &itrace, FTrace &ftrace, Difftest &difftest,
                               WatchpointManager &watchpoints, RunControl &runControl)
    : itrace(itrace), ftrace(ftrace), difftest(difftest),
      watchpoints(watchpoints), runControl(runControl) {}

void RetirePipeline::process(const RetireEvent &event) {
#ifdef CONFIG_ITRACE
  itrace.record(event);
#endif
#ifdef CONFIG_FTRACE
  ftrace.record(event);
#endif
#ifdef CONFIG_DIFFTEST
  if (difftest.step(event) == DifftestResult::Mismatch) {
    runControl.abort();
    return;
  }
#endif
#ifdef CONFIG_WATCHPOINT
  if (watchpoints.check()) runControl.stop();
#endif
}
