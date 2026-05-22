#ifndef NPC_PIPELINE_INST_H
#define NPC_PIPELINE_INST_H

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

#endif
