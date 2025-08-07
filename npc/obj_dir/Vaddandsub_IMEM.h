// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vaddandsub.h for the primary calling header

#ifndef VERILATED_VADDANDSUB_IMEM_H_
#define VERILATED_VADDANDSUB_IMEM_H_  // guard

#include "verilated.h"

class Vaddandsub__Syms;
class Vaddandsub_rom;


class Vaddandsub_IMEM final : public VerilatedModule {
  public:
    // CELLS
    Vaddandsub_rom* rom0;

    // DESIGN SPECIFIC STATE
    VL_OUT8(__PVT__rd,4,0);
    VL_OUT8(__PVT__rs1,4,0);
    VL_OUT8(__PVT__rs2,4,0);
    VL_IN8(__PVT____pinNumber6,0,0);
    VL_IN(__PVT__addr,31,0);
    VL_OUT(__PVT__inst,31,0);

    // INTERNAL VARIABLES
    Vaddandsub__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vaddandsub_IMEM(Vaddandsub__Syms* symsp, const char* v__name);
    ~Vaddandsub_IMEM();
    VL_UNCOPYABLE(Vaddandsub_IMEM);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
