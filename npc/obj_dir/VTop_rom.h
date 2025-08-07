// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP_ROM_H_
#define VERILATED_VTOP_ROM_H_  // guard

#include "verilated.h"

class VTop__Syms;

class VTop_rom final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN(__PVT__addr,31,0);
    VL_OUT(__PVT__data,31,0);
    VlUnpacked<IData/*31:0*/, 32768> rom_data;

    // INTERNAL VARIABLES
    VTop__Syms* const vlSymsp;

    // CONSTRUCTORS
    VTop_rom(VTop__Syms* symsp, const char* v__name);
    ~VTop_rom();
    VL_UNCOPYABLE(VTop_rom);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
