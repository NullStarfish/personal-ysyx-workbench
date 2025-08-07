// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP_IMEM_H_
#define VERILATED_VTOP_IMEM_H_  // guard

#include "verilated.h"

class VTop__Syms;
class VTop_rom;


class VTop_IMEM final : public VerilatedModule {
  public:
    // CELLS
    VTop_rom* rom0;

    // DESIGN SPECIFIC STATE
    VL_OUT8(__PVT__rd,4,0);
    VL_OUT8(__PVT__rs1,4,0);
    VL_OUT8(__PVT__rs2,4,0);
    VL_IN(__PVT__addr,31,0);
    VL_OUT(__PVT__inst,31,0);

    // INTERNAL VARIABLES
    VTop__Syms* const vlSymsp;

    // CONSTRUCTORS
    VTop_IMEM(VTop__Syms* symsp, const char* v__name);
    ~VTop_IMEM();
    VL_UNCOPYABLE(VTop_IMEM);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
